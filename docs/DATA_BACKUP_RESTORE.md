# MySQL + RustFS coordinated full backup and restore

## What is included

- Full backup of the configured MySQL database and complete RustFS bucket.
- One timestamped backup set containing a SQL dump, RustFS object files, a manifest, SHA-256 hashes, and a COMPLETE marker.
- A set is published only after both payloads and hashes are written. Incomplete staging directories do not qualify as backups.
- Restore verifies the set before changing live data, then replaces both MySQL and RustFS from that exact set.
- Retention keeps the newest N complete sets.
- Windows PowerShell scripts use mysqldump, mysql, and the MinIO-compatible mc CLI.

## Consistency assumptions

MySQL and RustFS do not support a shared distributed transaction. To avoid a mixed-time-point backup, the script stops the backend Windows service before copying either data store. This is suitable only if all application writes go through that service.

Also pause other writers during the operation: browsers holding presigned upload URLs, scripts using the RustFS API, manual SQL changes, and other backend instances. Stopping one service does not revoke a previously issued presigned URL. For stronger guarantees, block client access to RustFS during the maintenance window or otherwise ensure all upload URLs have expired.

If backup fails after the service was stopped, the script intentionally leaves the service stopped. Inspect the failure and staging directory and verify live data before manually restarting. If restore fails, the service also remains stopped because one data store may already have been replaced. Restore both sides from the same complete backup set before restarting.

This is a coordinated maintenance-window snapshot, not a distributed atomic commit. Schedule it when there are no direct or indirect writers.

## Prerequisites

1. Install MySQL client tools (mysqldump.exe and mysql.exe) and mc.exe on the backup host.
2. Configure an mc alias with read access to the source bucket and write/delete access to the destination bucket:
   **mc alias set museum-rustfs http://127.0.0.1:19000 ACCESS_KEY SECRET_KEY**
3. The backup destination must be mounted, writable, and on a separate physical disk or removable device. Example: **E:\MuseumBackups**. Do not use the same physical disk as the MySQL data directory or RustFS data directory.
4. Install the backend as a Windows service, or adapt the stop/start section to the actual process supervisor. The service name supplied to the scripts must be the real service name.
5. Supply credentials at run time or through a protected Task Scheduler secret mechanism. Never commit passwords to scripts or source control.

## Full backup

Run in an elevated PowerShell session. Replace the service name, alias, and destination as appropriate:

    $secure = Read-Host "MySQL password" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
      $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
      .\scripts\backup-museum.ps1 -DestinationRoot 'E:\MuseumBackups' -BackendServiceName 'MuseumBackend' -MysqlPassword $password -RustFsAlias 'museum-rustfs' -RustFsBucket 'museum-images' -KeepLatest 14
    } finally {
      [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }

The default database is **hml**. Override DB_HOST, DB_PORT, DB_NAME, and DB_USERNAME environment variables as needed, or pass the corresponding script parameters. The MySQL password is passed to the client through the MYSQL_PWD environment variable for the duration of the command; use a dedicated account and a protected execution context.

For unattended nightly execution, create a Windows Task Scheduler task running PowerShell with the script and correct arguments, using an account permitted to stop/start the backend service and write to the removable drive. Schedule it in the quiet period and allow sufficient time for the full object copy and hashing. Configure the task to run only when the target drive is available, and check task exit status/Windows event logs. Do not put a plaintext password in the task arguments.

The backup directory names use UTC, for example **museum-20261014T180000Z**. A successful set contains:
- mysql.sql
- rustfs-objects/
- manifest.json
- COMPLETE

## Restore

**Restore is destructive.** It drops and recreates the database named in the manifest and uses mc mirror --overwrite --remove to make the target RustFS bucket match the selected backup. Changes made after that backup may be lost on both sides.

1. Stop all other application instances and direct writers.
2. Confirm the backup set has COMPLETE and that its manifest matches the intended database/bucket.
3. Confirm the supplied mc alias points to the intended production RustFS instance.
4. If current data may be needed for forensic recovery, make a separate copy before proceeding.
5. Run the restore script after reviewing the path and target:

    .\scripts\restore-museum.ps1 -BackupDirectory 'E:\MuseumBackups\museum-20261014T180000Z' -BackendServiceName 'MuseumBackend' -MysqlPassword $password -RustFsAlias 'museum-rustfs' -ConfirmRestore

The backend starts automatically only when both restore steps succeed and the script itself stopped a previously running service. On failure it remains stopped. Do not manually restart until both data stores are repaired from the same backup set.

## When only one side is damaged

Do not restore only the damaged side. Choose a complete backup set and restore both MySQL and RustFS from it. This intentionally discards newer data from the undamaged side to avoid a mixed-time-point dataset. If the latest backup is damaged, use the newest older set whose COMPLETE marker and payload validation pass.

## Operational checks

- Test backup and restore against an isolated MySQL instance and a test RustFS bucket before production use.
- Perform regular restore drills; a successful backup command alone does not prove recoverability.
- Verify the backup media is mounted and has sufficient free space before the scheduled task runs.
- Protect backup media physically and restrict filesystem access because the SQL dump may contain sensitive information.
- Consider keeping one rotated copy disconnected/offline to reduce ransomware risk.
- Inspect incomplete .inprogress directories before deleting them.
- These scripts are operational tools and are not wired to an in-app admin UI/API.
