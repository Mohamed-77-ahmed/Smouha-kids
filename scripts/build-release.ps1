# Builds the release APK signed with the real key (keystore.properties, docs/RELEASE.md), names it
# Koko-Dawa-<versionName>.apk (ASCII only) and checks it with scripts\verify-dist.ps1.
#   powershell -File scripts\build-release.ps1
#       -> dist\Koko-Dawa-App\للأهل\1_التطبيق\Koko-Dawa-<version>.apk, with SHA256.txt and
#          verify-dist.txt in dist\Koko-Dawa-App\للمستشفى\
#   powershell -File scripts\build-release.ps1 -Out <folder> [-NoDistFiles]
# Gradle reads the passwords itself; this script never opens keystore.properties.
# -UpdateTestVersionCode / -UpdateTestVersionName are only for scripts\update-test.ps1.
param(
    [string]$Out,
    [int]$UpdateTestVersionCode = 0,
    [string]$UpdateTestVersionName,
    [switch]$NoDistFiles,
    [switch]$RecordFingerprint
)
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = (Resolve-Path "$PSScriptRoot\..").Path
$hospital = Join-Path $root "dist\Koko-Dawa-App\للمستشفى"
if (-not $Out) { $Out = Join-Path $root "dist\Koko-Dawa-App\للأهل\1_التطبيق" }
if (-not (Test-Path (Join-Path $root "keystore.properties"))) {
    throw "build-release: keystore.properties is missing. Create the keystore first (docs/RELEASE.md)."
}
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
# CLAUDE.md 0b: ANDROID_SERIAL is set for every Gradle command. assembleRelease installs nothing.
if (-not $env:ANDROID_SERIAL) { $env:ANDROID_SERIAL = "emulator-5560" }

$gradleArgs = "-p `"$root`" assembleRelease --console=plain -q"
if ($UpdateTestVersionCode -gt 0) {
    $gradleArgs += " -PupdateTestVersionCode=$UpdateTestVersionCode -PupdateTestVersionName=$UpdateTestVersionName"
}
# Waits for Gradle itself, with its output in a log file: a Gradle daemon started by this build lives on
# and would keep an output pipe open, so a caller reading this script's output would wait forever.
$log = Join-Path $env:TEMP "build-release-gradle.log"
$p = Start-Process -FilePath "$root\gradlew.bat" -ArgumentList $gradleArgs -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
$null = $p.Handle   # without touching the handle first, Windows PowerShell reports no exit code
$p.WaitForExit()
if ($p.ExitCode -ne 0) {
    Get-Content $log, "$log.err" -ErrorAction SilentlyContinue | Select-Object -Last 30
    throw "build-release: assembleRelease failed (exit $($p.ExitCode))"
}

$built = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$bt = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1
$pkgLine = & (Join-Path $bt.FullName "aapt.exe") dump badging $built | Where-Object { $_ -match "^package: " } | Select-Object -First 1
$vName = [regex]::Match($pkgLine, "versionName='([^']*)'").Groups[1].Value
if (-not $vName) { throw "build-release: no versionName in $built" }
$name = "Koko-Dawa-$vName.apk"

New-Item -ItemType Directory -Force $Out | Out-Null
$others = @(Get-ChildItem -LiteralPath $Out -Filter *.apk | Where-Object { $_.Name -ne $name })
if ($others.Count -gt 0) { Write-Warning ("Other APKs in the folder (remove them so parents see only one): " + ($others.Name -join ", ")) }
$target = Join-Path $Out $name
Copy-Item -LiteralPath $built -Destination $target -Force
"Built $target"

$verify = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "verify-dist.ps1"), "-Apk", $target)
if (-not $NoDistFiles) {
    New-Item -ItemType Directory -Force $hospital | Out-Null
    $verify += @("-Report", (Join-Path $hospital "verify-dist.txt"), "-Sha256File", (Join-Path $hospital "SHA256.txt"))
}
if ($RecordFingerprint) { $verify += "-RecordFingerprint" }
powershell @verify
if ($LASTEXITCODE -ne 0) { throw "build-release: verify-dist failed for $name" }
