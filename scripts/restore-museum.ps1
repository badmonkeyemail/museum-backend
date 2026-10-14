[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$BackupDirectory,
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$BackendServiceName,
    [string]$MysqlHost = $(if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }),
    [int]$MysqlPort = $(if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }),
    [string]$MysqlUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }),
    [Parameter(Mandatory = $true)][string]$MysqlPassword,
    [Parameter(Mandatory = $true)][string]$RustFsAlias,
    [string]$McCommand = "mc",
    [string]$MysqlCommand = "mysql",
    [Parameter(Mandatory = $true)][switch]$ConfirmRestore
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Native {
    param([string]$FilePath, [string[]]$Arguments, [string]$Operation)
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Operation failed; exit code $LASTEXITCODE." }
}

if (-not $ConfirmRestore) { throw "Restore is destructive. Re-run with -ConfirmRestore after verifying the backup and target." }
$backupFullPath = [IO.Path]::GetFullPath($BackupDirectory).TrimEnd('\','/')
if (-not (Test-Path (Join-Path $backupFullPath "COMPLETE") -PathType Leaf)) { throw "Backup is not marked COMPLETE." }
$manifestPath = Join-Path $backupFullPath "manifest.json"
$sqlPath = Join-Path $backupFullPath "mysql.sql"
$objectDir = Join-Path $backupFullPath "rustfs-objects"
if (-not (Test-Path $manifestPath -PathType Leaf) -or -not (Test-Path $sqlPath -PathType Leaf) -or -not (Test-Path $objectDir -PathType Container)) {
    throw "Backup payload is incomplete."
}
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ($manifest.formatVersion -ne 1 -or $manifest.backupType -ne "FULL") { throw "Unsupported backup format." }
if ([string]::IsNullOrWhiteSpace([string]$manifest.database)) { throw "Manifest database is missing." }
if ([string]$manifest.database -notmatch '^[A-Za-z0-9_$]+$') { throw "Unsafe database name in manifest." }
if ([string]$manifest.rustFsBucket -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { throw "Unsafe RustFS bucket in manifest." }
if ($manifest.rustFsAlias -ne $RustFsAlias) {
    throw "Backup alias is '$($manifest.rustFsAlias)', supplied alias is '$RustFsAlias'. Verify the alias before continuing."
}
if ([string]::IsNullOrWhiteSpace($MysqlPassword)) { throw "MysqlPassword is required; do not store it in this script." }

# Validate paths, sizes, and hashes before any destructive operation.
foreach ($entry in $manifest.files) {
    $relative = ([string]$entry.path).Replace('/', [IO.Path]::DirectorySeparatorChar)
    $path = [IO.Path]::GetFullPath((Join-Path $backupFullPath $relative))
    if (-not $path.StartsWith($backupFullPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Invalid path in manifest: $($entry.path)"
    }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing backup payload: $($entry.path)" }
    $file = Get-Item -LiteralPath $path
    if ($file.Length -ne [long]$entry.length) { throw "Length mismatch: $($entry.path)" }
    $actualHash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne ([string]$entry.sha256).ToLowerInvariant()) { throw "SHA-256 mismatch: $($entry.path)" }
}
if (-not (Test-Path -LiteralPath $sqlPath -PathType Leaf) -or (Get-Item -LiteralPath $sqlPath).Length -le 0) { throw "SQL dump is empty." }

$service = Get-Service -Name $BackendServiceName -ErrorAction Stop
$wasRunning = $service.Status -eq [System.ServiceProcess.ServiceControllerStatus]::Running
$stoppedByScript = $false
$restoreCompleted = $false

try {
    if ($wasRunning) {
        Stop-Service -Name $BackendServiceName
        (Get-Service -Name $BackendServiceName).WaitForStatus(
            [System.ServiceProcess.ServiceControllerStatus]::Stopped,
            [TimeSpan]::FromMinutes(2))
        $stoppedByScript = $true
    }

    # Replace the database, rather than importing over a newer schema/data set.
    $db = [string]$manifest.database
    $env:MYSQL_PWD = $MysqlPassword
    try {
        Invoke-Native -FilePath $MysqlCommand -Arguments @(
            "--host=$MysqlHost", "--port=$MysqlPort", "--user=$MysqlUser",
            "--execute=DROP DATABASE IF EXISTS $db"
        ) -Operation "Drop target database"

        # Stream the potentially large SQL file through cmd.exe instead of loading it into RAM.
        $mysqlArgs = "--host=$MysqlHost --port=$MysqlPort --user=$MysqlUser"
        $cmdLine = '"' + $MysqlCommand + '" ' + $mysqlArgs + ' < "' + $sqlPath + '"'
        & $env:ComSpec /d /c $cmdLine
        if ($LASTEXITCODE -ne 0) { throw "MySQL import failed; exit code $LASTEXITCODE." }
    }
    finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

    # Exact mirror restore: remove target objects that are absent from this snapshot.
    Invoke-Native -FilePath $McCommand -Arguments @(
        "mirror", "--overwrite", "--remove", $objectDir, "$RustFsAlias/$($manifest.rustFsBucket)"
    ) -Operation "Restore RustFS bucket"

    $restoreCompleted = $true
    Write-Host "MySQL and RustFS restored from backup set $($manifest.backupId)." -ForegroundColor Green
}
finally {
    if ($stoppedByScript -and $wasRunning -and $restoreCompleted) {
        Start-Service -Name $BackendServiceName
    }
    elseif ($stoppedByScript -and -not $restoreCompleted) {
        Write-Warning "Restore did not complete. Backend remains stopped. Do not start it until both MySQL and RustFS are repaired from the same backup set."
    }
}