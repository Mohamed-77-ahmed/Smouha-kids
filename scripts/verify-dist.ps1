# Checks a release APK before it is shared (CLAUDE.md 10, docs/RELEASE.md). Reads no password.
#   powershell -File scripts\verify-dist.ps1 -Apk <file.apk> [-Report <file.txt>] [-Sha256File <SHA256.txt>] [-RecordFingerprint]
# 1. apksigner verify (v2/v3) and the signing certificate (DN, SHA-256). Fails on the Android debug
#    certificate. docs/RELEASE.md keeps the release certificate's SHA-256: every later APK must have
#    the same one (an APK signed with another key can never update the installed app). -RecordFingerprint
#    writes it there the first time; after that a different certificate fails.
# 2. aapt badging and the resource table: package, versionName, versionCode, minSdk, targetSdk, the app
#    name and the launcher name in English and Arabic.
# 3. Permissions: no INTERNET. Manifest: not debuggable, allowBackup false.
# 4. File size and SHA-256 (written to -Sha256File when given).
# Exit code 1 when any check fails.
param(
    [Parameter(Mandatory = $true)][string]$Apk,
    [string]$Report,
    [string]$Sha256File,
    [switch]$RecordFingerprint
)
# Continue: the JDK prints warnings on stderr (a restricted method), which "Stop" would turn into an
# error; every check below decides on its own.
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = (Resolve-Path "$PSScriptRoot\..").Path
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$bt = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1
$apksigner = Join-Path $bt.FullName "apksigner.bat"
$aapt = Join-Path $bt.FullName "aapt.exe"
$aapt2 = Join-Path $bt.FullName "aapt2.exe"
$releaseMd = Join-Path $root "docs\RELEASE.md"
$marker = "Release certificate SHA-256:"

$original = (Resolve-Path -LiteralPath $Apk).Path
$apkName = Split-Path $original -Leaf
# aapt and apksigner cannot open a path with Arabic letters (dist\...\للأهل\...): check a copy in an
# ASCII temp folder. The hash below is taken from the original file.
$work = Join-Path $env:TEMP "verify-dist"
New-Item -ItemType Directory -Force $work | Out-Null
$apkPath = Join-Path $work $apkName
Copy-Item -LiteralPath $original -Destination $apkPath -Force
$out = New-Object System.Collections.Generic.List[string]
$fails = New-Object System.Collections.Generic.List[string]
function Check([bool]$ok, [string]$what) {
    $out.Add(("  [{0}] {1}" -f $(if ($ok) { "OK" } else { "FAIL" }), $what))
    if (-not $ok) { $fails.Add($what) }
}

$out.Add("verify-dist " + (Get-Date -Format "yyyy-MM-dd HH:mm") + " (build-tools $($bt.Name))")
$out.Add("File: $apkName")
Check ($apkName -match '^[A-Za-z0-9._-]+$') "file name is ASCII only"

# 1. Signature and certificate
$out.Add("Signature (apksigner verify --verbose --print-certs):")
$sig = & $apksigner verify --verbose --print-certs $apkPath 2>&1 | ForEach-Object { "$_" } | Where-Object { $_ -notmatch '^WARNING: ' }
$sigOk = $LASTEXITCODE -eq 0
foreach ($l in $sig) { if ($l -match '^(Verifies|Verified using|Number of signers|Signer #1 (certificate DN|certificate SHA-256|key algorithm|key size))') { $out.Add("  $l") } }
$v2 = ($sig -match 'Verified using v2 scheme \(APK Signature Scheme v2\): true').Count -gt 0
$v3 = ($sig -match 'Verified using v3 scheme \(APK Signature Scheme v3\): true').Count -gt 0
$dn = (($sig | Where-Object { $_ -match '^Signer #1 certificate DN: ' }) -replace '^Signer #1 certificate DN: ', '') | Select-Object -First 1
$hex = (($sig | Where-Object { $_ -match '^Signer #1 certificate SHA-256 digest: ' }) -replace '^Signer #1 certificate SHA-256 digest: ', '') | Select-Object -First 1
$signers = (($sig | Where-Object { $_ -match '^Number of signers: ' }) -replace '^Number of signers: ', '') | Select-Object -First 1
$colon = if ($hex) { (($hex.ToUpper() -split '(..)' | Where-Object { $_ }) -join ':') } else { "" }
$out.Add("  Certificate SHA-256 (as keytool -list shows it): $colon")
Check $sigOk "apksigner verify passes"
Check ($v2 -or $v3) "signed with APK Signature Scheme v2 or v3"
Check ($signers -eq "1") "exactly one signer"
Check ($dn -and $dn -notmatch 'Android Debug') "not the Android debug certificate ($dn)"

$recorded = $null
if (Test-Path $releaseMd) {
    $line = Get-Content $releaseMd -Encoding UTF8 | Where-Object { $_ -match [regex]::Escape($marker) } | Select-Object -First 1
    if ($line -match '([0-9A-F]{2}(:[0-9A-F]{2}){31})') { $recorded = $Matches[1] }
}
if ($recorded) {
    Check ($recorded -eq $colon) "same certificate as recorded in docs/RELEASE.md ($recorded)"
} elseif ($RecordFingerprint -and $colon -and $sigOk -and $dn -notmatch 'Android Debug') {
    $text = [IO.File]::ReadAllText($releaseMd, [Text.Encoding]::UTF8)
    $pattern = [regex]::Escape($marker) + '.*'
    if ($text -notmatch $pattern) { throw "verify-dist: docs/RELEASE.md has no line starting with '$marker'" }
    $text = [regex]::Replace($text, $pattern, "$marker ``$colon``", 1)
    [IO.File]::WriteAllText($releaseMd, $text, (New-Object Text.UTF8Encoding $false))
    $out.Add("  Recorded the certificate SHA-256 in docs/RELEASE.md (first release).")
} else {
    Check $false "release certificate recorded in docs/RELEASE.md (run once with -RecordFingerprint)"
}

# 2. Identity
$badging = & $aapt dump badging $apkPath 2>&1 | ForEach-Object { "$_" }
$pkgLine = $badging | Where-Object { $_ -match "^package: " } | Select-Object -First 1
$package = [regex]::Match($pkgLine, "name='([^']*)'").Groups[1].Value
$vCode = [regex]::Match($pkgLine, "versionCode='([^']*)'").Groups[1].Value
$vName = [regex]::Match($pkgLine, "versionName='([^']*)'").Groups[1].Value
$minSdk = [regex]::Match(($badging -match "^sdkVersion:" | Select-Object -First 1), "'([^']*)'").Groups[1].Value
$targetSdk = [regex]::Match(($badging -match "^targetSdkVersion:" | Select-Object -First 1), "'([^']*)'").Groups[1].Value
$labelDefault = [regex]::Match(($badging -match "^application-label:" | Select-Object -First 1), "^application-label:'(.*)'$").Groups[1].Value
$labelAr = [regex]::Match(($badging -match "^application-label-ar:" | Select-Object -First 1), "^application-label-ar:'(.*)'$").Groups[1].Value
$launcherOwn = [regex]::Match(($badging -match "^launchable-activity:" | Select-Object -First 1), "label='([^']*)'").Groups[1].Value
# Since 1.1.0 the launcher activity has no label of its own, so the launcher shows the application label
# (as Android does; LocalizationTest checks it on the device). aapt then reports label=''.
$launcher = if ($launcherOwn) { $launcherOwn } else { $labelDefault }
$launcherInherits = -not $launcherOwn
$res = & $aapt2 dump resources $apkPath 2>&1 | ForEach-Object { "$_" }
function ResString([string]$name, [string]$config) {
    $inside = $false
    foreach ($l in $res) {
        if ($l -match "^\s+resource 0x[0-9a-f]+ string/$name\s*$") { $inside = $true; continue }
        if ($inside -and $l -match '^\s+resource ') { break }
        if ($inside -and $l -match "^\s+\($config\) `"(.*)`"\s*$") { return $Matches[1] }
    }
    return ""
}
# The expected names come from the project's strings (app_name is both the app label and the launcher label).
function Source-AppName([string]$file) {
    $x = [xml](Get-Content -Raw -Encoding UTF8 (Join-Path $root $file))
    return (($x.resources.string | Where-Object { $_.GetAttribute("name") -eq "app_name" } | Select-Object -First 1).InnerText -replace "\\'", "'")
}
$expectEn = Source-AppName "app\src\main\res\values\strings.xml"
$expectAr = Source-AppName "app\src\main\res\values-ar\strings.xml"
$launcherAr = ResString 'app_name' 'ar'
$out.Add("Identity (aapt dump badging, aapt2 dump resources; expected from strings.xml: '$expectEn' / '$expectAr'):")
$out.Add("  package=$package versionCode=$vCode versionName=$vName minSdk=$minSdk targetSdk=$targetSdk")
$out.Add("  app name EN: $labelDefault")
$out.Add("  app name AR: $labelAr")
$out.Add("  launcher name EN: $launcher" + $(if ($launcherInherits) { " (the launcher activity has no label of its own: the application label)" } else { "" }))
$out.Add("  launcher name AR: $launcherAr")
Check ($package -eq "com.smouha.kidsreminder") "package com.smouha.kidsreminder"
Check ($vCode -match '^\d+$' -and $vName) "versionCode and versionName present"
Check ($minSdk -eq "28") "minSdk 28"
Check ($labelDefault -eq $expectEn) "English app name"
Check ($labelAr -eq $expectAr) "Arabic app name"
Check ($launcher -eq $expectEn) "English launcher name"
Check ($launcherAr -eq $expectAr) "Arabic launcher name"

# 3. Permissions and manifest flags
$perms = & $aapt dump permissions $apkPath 2>&1 | ForEach-Object { "$_" } | Where-Object { $_ -match "^uses-permission" } | ForEach-Object { [regex]::Match($_, "name='([^']*)'").Groups[1].Value + $(if ($_ -match "maxSdkVersion='(\d+)'") { " (max SDK $($Matches[1]))" } else { "" }) }
$out.Add("Permissions (aapt dump permissions):")
$perms | ForEach-Object { $out.Add("  $_") }
Check (-not ($perms -match '^android\.permission\.INTERNET\b')) "no INTERNET permission"
$xml = & $aapt dump xmltree $apkPath AndroidManifest.xml 2>&1 | ForEach-Object { "$_" }
$allowBackup = ($xml | Where-Object { $_ -match 'android:allowBackup\(0x01010280\)' } | Select-Object -First 1)
$debuggable = ($xml | Where-Object { $_ -match 'android:debuggable\(0x0101000f\)' } | Select-Object -First 1)
$out.Add("Manifest (aapt dump xmltree):")
$out.Add("  allowBackup: " + $(if ($allowBackup) { $allowBackup.Trim() } else { "(not set)" }))
$out.Add("  debuggable: " + $(if ($debuggable) { $debuggable.Trim() } else { "(not set)" }))
Check ($allowBackup -and $allowBackup -match '\)0x0\s*$') "allowBackup is false"
Check ((-not $debuggable -or $debuggable -match '\)0x0\s*$') -and -not ($badging -match '^application-debuggable')) "not debuggable"

# 4. Size and hash
$len = (Get-Item -LiteralPath $original).Length
$sha = (Get-FileHash -LiteralPath $original -Algorithm SHA256).Hash.ToLower()
Check ($sha -eq (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLower()) "the checked copy is identical to the file"
Remove-Item -LiteralPath $apkPath -Force
$out.Add("File: $len bytes ($([math]::Round($len / 1MB, 2)) MB), SHA-256 $sha")
Check ($len -gt 0 -and $len -lt 100MB) "size under 100 MB (Google Drive adds an extra 'cannot scan for viruses' page for bigger files)"

if ($Sha256File) {
    $lines = @(
        "$sha *$apkName",
        "# SHA-256 of the app file above (check on Windows: Get-FileHash $apkName -Algorithm SHA256).",
        "# Signing certificate SHA-256 (the same for every update of the app):",
        "# $colon",
        "# Version $vName (versionCode $vCode), package $package."
    )
    [IO.File]::WriteAllLines($Sha256File, [string[]]$lines, (New-Object Text.UTF8Encoding $false))
    $out.Add("Wrote $(Split-Path $Sha256File -Leaf)")
}

$out.Add($(if ($fails.Count -eq 0) { "RESULT: all checks passed" } else { "RESULT: $($fails.Count) check(s) FAILED: " + ($fails -join "; ") }))
if ($Report) { [IO.File]::WriteAllLines($Report, [string[]]$out, (New-Object Text.UTF8Encoding $false)) }
$out
if ($fails.Count -gt 0) { exit 1 }
