# Copies the project (git is not installed) to ../tracker-snapshots/<name>, CLAUDE.md section 0.
# Usage: powershell -File docs\evidence\tools\snapshot.ps1 -Name <name>
# Leaves out build folders and every signing secret: keystore.properties (it holds the passwords),
# *.jks and *.keystore. A snapshot must never spread the release key or its password.
param([Parameter(Mandatory = $true)][string]$Name)
$root = (Resolve-Path "$PSScriptRoot\..\..\..").Path
$dest = Join-Path (Split-Path $root -Parent) "tracker-snapshots\$Name"
robocopy $root $dest /E /XD build .gradle .idea .kotlin /XF keystore.properties *.jks *.keystore /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "snapshot: robocopy failed with code $LASTEXITCODE" }
$leak = @(Get-ChildItem $dest -Recurse -File -Include keystore.properties, *.jks, *.keystore -ErrorAction SilentlyContinue)
if ($leak.Count -gt 0) { throw "snapshot: signing files in the copy: $($leak.FullName -join ', ')" }
"Snapshot: $dest"
