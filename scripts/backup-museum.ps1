[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$DestinationRoot,
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$BackendServiceName,
    [string]$MysqlHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }),
    [int]$MysqlPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$MysqlDatabase = $(if ($env:DB_NAME) { $env:DB_NAME } else { "hml" }),
    [string]$MysqlUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }),
    [Parameter(Mandatory = $true)][string]$MysqlPassword,
    [Parameter(Mandatory = $true)][string]$RustFsAlias,
    [string]$RustFsBucket = $(if ($env:RUSTFS_BUCKET) { $env:RUSTFS_BUCKET } else { "museum-images" }),
    [string]$MysqlDumpCommand = "mysqldump",
    [string]$McCommand = "mc",
    [int]$KeepLatest = 14
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Native {
    param([string]$FilePath, [string[]]$Arguments, [string]$Operation)
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Operation failed; exit code $LASTEXITCODE." }
}

if (-not (Test-Path -LiteralPath $DestinationRoot -PathType Container)) { throw "Backup destination does not exist or is not mounted: $DestinationRoot" }
if ($KeepLatest -lt 1) { throw "KeepLatest must be at least 1." }
if ($MysqlDatabase -notmatch '^[A-Za-z0-9_$]+$') { throw "Unsafe MySQL database name." }
if ([string]::IsNullOrWhiteSpace($MysqlPassword)) { throw "MysqlPassword is required; do not store it in this script." }

$destinationFullPath = [IO.Path]::GetFullPath($DestinationRoot)
$backupId = (Get-Date).ToUniversalTime().ToString("yyyyMMdd'T'HHmmss'Z'")
$finalDir = Join-Path $destinationFullPath "museum-$backupId"
$stageDir = Join-Path $destinationFullPath ".museum-$backupId.inprogress"
if ((Test-Path $finalDir) -or (Test-Path $stageDir)) { throw "Backup path already exists: $finalDir" }

$service = Get-Service -Name $BackendServiceName -ErrorAction Stop
$wasRunning = $service.Status -eq [System.ServiceProcess.ServiceControllerStatus]::Running
$stoppedByScript = $false
$backupCompleted = $false
$sqlPath = Join-Path $stageDir "mysql.sql"
$objectDir = Join-Path $stageDir "rustfs-objects"

try {
    New-Item -ItemType Directory -Path $stageDir | Out-Null
    New-Item -ItemType Directory -Path $objectDir | Out-Null

    # Pause all application writes before snapshotting either data store.
    if ($wasRunning) {
        Stop-Service -Name $BackendServiceName
        (Get-Service -Name $BackendServiceName).WaitForStatus(
            [System.ServiceProcess.ServiceControllerStatus]::Stopped,
            [TimeSpan]::FromMinutes(2))
        $stoppedByScript = $true
    }

    $env:MYSQL_PWD = $MysqlPassword
    try {
        Invoke-Native -FilePath $MysqlDumpCommand -Arguments @(
            "--host=$MysqlHost", "--port=$MysqlPort", "--user=$MysqlUser",
            "--single-transaction", "--routines", "--events", "--triggers",
            "--hex-blob", "--default-character-set=utf8mb4",
            "--databases", $MysqlDatabase, "--result-file=$sqlPath"
        ) -Operation "MySQL full dump"
    }
    finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

    if (-not (Test-Path $sqlPath) -or (Get-Item $sqlPath).Length -le 0) { throw "MySQL dump is missing or empty." }

    # Copy the complete bucket. Do not use --remove while creating a backup.
    Invoke-Native -FilePath $McCommand -Arguments @(
        "mirror", "--overwrite", "$RustFsAlias/$RustFsBucket", $objectDir
    ) -Operation "RustFS bucket mirror"

    $payloadFiles = @(Get-ChildItem -LiteralPath $stageDir -File -Recurse |
        Where-Object { $_.Name -ne "manifest.json" -and $_.Name -ne "COMPLETE" } |
        Sort-Object FullName)
    $fileEntries = foreach ($file in $payloadFiles) {
        [pscustomobject]@{
            path = $file.FullName.Substring($stageDir.Length).TrimStart('\','/').Replace('\','/')
            length = $file.Length
            sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    }

    $manifest = [ordered]@{
        formatVersion = 1
        backupId = $backupId
        createdAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        database = $MysqlDatabase
        rustFsAlias = $RustFsAlias
        rustFsBucket = $RustFsBucket
        backupType = "FULL"
        payloadFileCount = @($fileEntries).Count
        files = @($fileEntries)
        note = "Restore MySQL and RustFS together from this set."
    }
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $stageDir "manifest.json") -Encoding UTF8
    "Backup set complete: $backupId" | Set-Content -LiteralPath (Join-Path $stageDir "COMPLETE") -Encoding UTF8

    # Publish the set only after all payloads and hashes are present.
    Move-Item -LiteralPath $stageDir -Destination $finalDir
    $backupCompleted = $true

    $oldSets = @(Get-ChildItem -LiteralPath $destinationFullPath -Directory |
        Where-Object { $_.Name -match '^museum-\d{8}T\d{6}Z$' -and (Test-Path (Join-Path $_.FullName "COMPLETE")) } |
        Sort-Object Name -Descending | Select-Object -Skip $KeepLatest)
    foreach ($old in $oldSets) { Remove-Item -LiteralPath $old.FullName -Recurse -Force }

    Write-Host "Backup completed: $finalDir" -ForegroundColor Green
}
finally {
    if ($stoppedByScript -and $wasRunning -and $backupCompleted) {
        Start-Service -Name $BackendServiceName
    }
    elseif ($stoppedByScript -and -not $backupCompleted) {
        Write-Warning "Backup failed. Backend remains stopped because the snapshot is incomplete. Inspect the .inprogress directory and verify live data before manually starting the service."
    }
}