# TiDB initial V3 recovery

Run from an interactive PowerShell terminal at D:\Sanka:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-cloud.ps1 -RecoverV3
```

The tool prompts for JDBC URL, full TiDB username and a hidden password. Credentials
are not saved, placed on a command line, or read from application-local.yml.
Use the same JDBC URL as Vercel, with database sanka_lms and sslMode=VERIFY_IDENTITY.
Requires a JDK on PATH and Maven wrapper network access for any uncached dependencies.

If pasting into the hidden Java password prompt does not work, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-cloud.ps1 -RecoverV3 -PasswordFromClipboard
```

After the build, the script asks you to copy ONLY the TiDB password and press Enter.
It captures the clipboard without printing it, then asks for URL and username.
The password is passed only through the Java child process environment; the script
restores the previous process environment in a finally block. No password argument
or file is created. The clipboard itself is left unchanged.

Stop traffic and automatic deployments while running recovery. Close the failed site
tabs and Vercel previews. Do not run the command concurrently from multiple terminals.
Recovery is narrowly limited to successful V1/V2 followed by failed V3 and no later
history entries. It refuses nonempty students, exam_attempts or practice_submissions.
It checks the V3 source checksum, columns, defaults, indexes and foreign keys before
acknowledging the completed V3 history entry. No table is dropped and no migration
source is edited. A failed check leaves the V3 entry unchanged.

After recovery, normal Flyway runs pending migrations and validates all checksums.
Later migrations can fail independently; preserve their error output for diagnosis.
Only after SUCCESS should you redeploy Vercel. Do not disable Flyway validation.
For a database without failed V3, omit -RecoverV3 to run ordinary migrations only.

This is an operator recovery tool for the observed interrupted first deployment,
not an automatic startup workaround or a general Flyway repair replacement.
