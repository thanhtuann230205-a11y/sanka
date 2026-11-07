param([switch]$RecoverV3, [switch]$PasswordFromClipboard)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    # Build locally without starting Spring or reading application-local.yml.
    & .\mvnw.cmd '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw 'Maven package failed.' }
    $archive = Join-Path $projectRoot 'target\sanka-lms-0.0.1-SNAPSHOT.jar'
    $toolRoot = Join-Path $projectRoot 'target\cloud-migration'
    New-Item -ItemType Directory -Force -Path $toolRoot | Out-Null
    Push-Location $toolRoot
    try {
        & jar --extract --file $archive BOOT-INF/lib
        if ($LASTEXITCODE -ne 0) { throw 'Cannot extract JDBC/Flyway libraries.' }
    } finally { Pop-Location }
    $libraryPath = Join-Path $toolRoot 'BOOT-INF\lib\*'
    $toolArgs = @()
    if ($RecoverV3) { $toolArgs += '--recover-v3' }
    $previousPassword = [Environment]::GetEnvironmentVariable('SANKA_MIGRATION_PASSWORD', 'Process')
    try {
        if ($PasswordFromClipboard) {
            Read-Host 'Copy ONLY the TiDB password now, return here, then press Enter' | Out-Null
            $clipboardPassword = Get-Clipboard -Raw
            if ([string]::IsNullOrWhiteSpace($clipboardPassword)) { throw 'Clipboard is empty.' }
            $clipboardPassword = $clipboardPassword.TrimEnd([char[]]"`r`n")
            if ($clipboardPassword.Contains("`n") -or $clipboardPassword.Contains("`r")) { throw 'Copy only one password, without other text.' }
            [Environment]::SetEnvironmentVariable('SANKA_MIGRATION_PASSWORD', $clipboardPassword, 'Process')
            $clipboardPassword = $null
        } else {
            [Environment]::SetEnvironmentVariable('SANKA_MIGRATION_PASSWORD', $null, 'Process')
        }
        & java --class-path $libraryPath scripts/MigrateCloud.java @toolArgs
        if ($LASTEXITCODE -ne 0) { throw 'Cloud migration failed. Read the error above; do not redeploy yet.' }
    } finally {
        [Environment]::SetEnvironmentVariable('SANKA_MIGRATION_PASSWORD', $previousPassword, 'Process')
        $clipboardPassword = $null
    }
} finally { Pop-Location }
