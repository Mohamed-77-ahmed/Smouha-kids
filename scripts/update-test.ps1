# Update test on the TEST emulator only (Pixel_API_34_test, CLAUDE.md 0b; docs/RELEASE.md).
# Proves that a newer version signed with the SAME key installs over the release with adb install -r and
# keeps every row of data, and records what happens with ANOTHER key (it fails; never use that as a path).
#   set ANDROID_SERIAL=emulator-5560
#   powershell -File scripts\update-test.ps1 [-NextVersionCode 2] [-NextVersionName 1.0.1] [-Report <file.txt>]
#   powershell -File scripts\update-test.ps1 -NextApk <new release.apk>   (a real next release: no build)
# Before: the release APK from dist\ is installed on the test AVD and has data (patients, medicines, a
# confirmed dose, points, the PIN).
# 1. Dumps every table of the app's database (adb root + sqlite3) and the installed version.
# 2. Builds the next version with the same key into app\build\update-test (scripts\build-release.ps1).
# 3. adb install -r: Success, versionCode = next, firstInstallTime unchanged.
# 4. Dumps every table again: every row must be identical.
# 5. Opens the app: the same patients on home; the reminders are booked again (dumpsys alarm).
# 6. Another key: the same next APK re-signed with the Android debug key, adb install -r: must fail with
#    INSTALL_FAILED_UPDATE_INCOMPATIBLE, and the version and every row stay. The re-signed copy is deleted.
# The PIN hash and salt are compared but never printed. Nothing is written to the device but the installs.
param([int]$NextVersionCode = 2, [string]$NextVersionName = "1.0.1", [string]$Report, [string]$NextApk)
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = (Resolve-Path "$PSScriptRoot\..").Path
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.smouha.kidsreminder"
$db = "/data/data/$pkg/databases/pill_pals.db"
if (-not $env:ANDROID_SERIAL) { throw "update-test: set ANDROID_SERIAL to the test emulator (emulator-5560)" }
$avd = "$(& $adb -s $env:ANDROID_SERIAL emu avd name 2>$null | Select-Object -First 1)".Trim()
if ($avd -ne "Pixel_API_34_test") { throw "update-test: refusing, $env:ANDROID_SERIAL is '$avd', not Pixel_API_34_test" }

$out = New-Object System.Collections.Generic.List[string]
$fails = New-Object System.Collections.Generic.List[string]
function Check([bool]$ok, [string]$what) {
    $out.Add(("  [{0}] {1}" -f $(if ($ok) { "OK" } else { "FAIL" }), $what))
    if (-not $ok) { $fails.Add($what) }
}
function Installed {
    $d = & $adb shell dumpsys package $pkg
    [pscustomobject]@{
        Code = [regex]::Match("$d", 'versionCode=(\d+)').Groups[1].Value
        Name = [regex]::Match("$d", 'versionName=(\S+)').Groups[1].Value
        First = [regex]::Match("$d", 'firstInstallTime=([0-9: -]+\d)').Groups[1].Value
        Last = [regex]::Match("$d", 'lastUpdateTime=([0-9: -]+\d)').Groups[1].Value
    }
}
function Dump {
    # Every table, every row, in a fixed order. Kept in memory only (the PIN hash and salt are in it).
    $tables = ("$(& $adb shell "sqlite3 $db .tables")" -split '\s+') | Where-Object { $_ -and $_ -notin @("android_metadata", "sqlite_sequence") } | Sort-Object
    $all = [ordered]@{}
    foreach ($t in $tables) { $all[$t] = @(& $adb shell "sqlite3 -header $db 'SELECT * FROM $t ORDER BY 1'") }
    $all
}
# Rows of a "sqlite3 -header" dump (row 0 = column names) cut down to the given columns.
function Project($rows, [string[]]$columns) {
    $rows = @($rows)
    if ($rows.Count -eq 0) { return @() }
    $header = $rows[0] -split '\|'
    $idx = @($columns | ForEach-Object { [array]::IndexOf($header, $_) })
    @($rows | ForEach-Object { $cells = $_ -split '\|'; ($idx | ForEach-Object { $cells[$_] }) -join '|' })
}

# Room's schema fingerprint (room_master_table) per exported schema version (app/schemas/.../N.json).
$schemaVersions = @{}
Get-ChildItem (Join-Path $root "app\schemas") -Recurse -Filter "*.json" | ForEach-Object {
    $j = Get-Content -Raw $_.FullName | ConvertFrom-Json
    $schemaVersions[$j.database.identityHash] = [int]$j.database.version
}

function Compare-Dumps($before, $after, [string]$label) {
    $same = $true
    foreach ($t in ($before.Keys + $after.Keys | Select-Object -Unique)) {
        $b = @($before[$t]); $a = @($after[$t])
        if ($t -eq "room_master_table") {
            # Not family data: Room's fingerprint of the schema. It must stay the same, or move to the
            # exported fingerprint of a higher schema version when the update brings a Migration.
            $hb = (@($b | Select-Object -Skip 1) -join '') -replace '^\d+\|', ''
            $ha = (@($a | Select-Object -Skip 1) -join '') -replace '^\d+\|', ''
            $vb = $schemaVersions[$hb]; $va = $schemaVersions[$ha]
            if ($hb -eq $ha) { $out.Add("    $t : Room's schema fingerprint unchanged (schema $vb)") }
            elseif ($vb -and $va -and $va -gt $vb) { $out.Add("    $t : Room's schema fingerprint moved from schema $vb to schema $va, the exported $va.json (a Migration ran)") }
            else { $out.Add("    $t : Room's schema fingerprint DIFFERENT and not a known newer schema ($hb -> $ha)"); $same = $false }
            continue
        }
        # A newer schema may add columns (1.1.0: app_settings.celebrationSoundEnabled): compare the columns
        # both versions have, and show the new columns' values (never the PIN hash, which is not new).
        $bCols = if ($b.Count) { @($b[0] -split '\|') } else { @() }
        $aCols = if ($a.Count) { @($a[0] -split '\|') } else { @() }
        $common = @($bCols | Where-Object { $_ -in $aCols })
        $added = @($aCols | Where-Object { $_ -notin $bCols })
        $bp = Project $b $common; $ap = Project $a $common
        $bp = @($bp); $ap = @($ap)   # an empty table projects to $null, which Compare-Object refuses
        $equal = ($bp.Count -eq $ap.Count) -and ($bp.Count -eq 0 -or @(Compare-Object $bp $ap -SyncWindow 0).Count -eq 0)
        $rows = [math]::Max(0, $b.Count - 1)
        $note = if ($added.Count) { " in the $($common.Count) columns both versions have" } else { "" }
        if ($t -eq "app_settings") { $out.Add("    $t : $rows row(s), " + $(if ($equal) { "identical$note (PIN hash and salt included, not printed)" } else { "DIFFERENT (not printed: it holds the PIN hash)" })) }
        else {
            $out.Add("    $t : $rows row(s), " + $(if ($equal) { "identical$note" } else { "DIFFERENT" }))
            if (-not $equal) { Compare-Object $bp $ap | ForEach-Object { $out.Add("      " + $_.SideIndicator + " " + $_.InputObject) } }
        }
        if ($added.Count) { $out.Add("      new column(s) " + ($added -join ', ') + ": " + ((Project $a $added | Select-Object -Skip 1) -join '; ')) }
        if (-not $equal) { $same = $false }
    }
    Check $same "every table identical $label"
}

$out.Add("Update test " + (Get-Date -Format "yyyy-MM-dd HH:mm") + " on $env:ANDROID_SERIAL ($avd)")
& $adb root | Out-Null; Start-Sleep 2; & $adb wait-for-device

# 1. Before
$v1 = Installed
$out.Add("Installed before: $($v1.Name) (versionCode $($v1.Code)), first install $($v1.First)")
$before = Dump
$rows = 0; foreach ($k in $before.Keys) { $rows += [math]::Max(0, @($before[$k]).Count - 1) }
$out.Add("Data before: $($before.Keys.Count) tables, $rows rows; patients:")
@($before["patient"]) | Select-Object -Skip 1 | ForEach-Object { $out.Add("    $_") }
Check ($rows -gt 0 -and @($before["patient"]).Count -gt 1) "there is data to keep (at least one patient)"

# 2. Next version, same key
$nextDir = Join-Path $root "app\build\update-test"
if ($NextApk) {
    # A real next release: check it, then copy it to an ASCII path (adb and aapt, see verify-dist).
    # PowerShell names ignore case: $nextApk below IS the parameter, so keep the given path apart first.
    $given = $NextApk
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-dist.ps1") -Apk $given | Out-Null
    Check ($LASTEXITCODE -eq 0) "verify-dist passed for the next release (same certificate as docs/RELEASE.md)"
    New-Item -ItemType Directory -Force $nextDir | Out-Null
    $nextApk = Join-Path $nextDir (Split-Path $given -Leaf)
    Copy-Item -LiteralPath $given -Destination $nextApk -Force
    $badge = & (Join-Path (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1).FullName "aapt.exe") dump badging $nextApk | Select-Object -First 1
    $NextVersionCode = [int][regex]::Match("$badge", "versionCode='(\d+)'").Groups[1].Value
    $NextVersionName = [regex]::Match("$badge", "versionName='([^']*)'").Groups[1].Value
} else {
    # Started and awaited as a process with its output in a file, not through a pipe: a Gradle daemon
    # started by the build would keep a pipe open and this script would wait forever.
    $buildLog = Join-Path $env:TEMP "update-test-build.log"
    Get-ChildItem $nextDir -Filter *.apk -ErrorAction SilentlyContinue | Remove-Item -Force   # never test a stale APK
    $b = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$(Join-Path $PSScriptRoot 'build-release.ps1')`" -Out `"$nextDir`" -UpdateTestVersionCode $NextVersionCode -UpdateTestVersionName $NextVersionName -NoDistFiles" -NoNewWindow -PassThru -RedirectStandardOutput $buildLog -RedirectStandardError "$buildLog.err"
    $null = $b.Handle   # without touching the handle first, Windows PowerShell reports no exit code
    $b.WaitForExit()
    $nextApk = Join-Path $nextDir "Koko-Dawa-$NextVersionName.apk"
    Check ($b.ExitCode -eq 0 -and (Test-Path $nextApk)) "built $NextVersionName (versionCode $NextVersionCode) with the same key (verify-dist passed, same certificate as docs/RELEASE.md)"
}
Check ([int]$NextVersionCode -gt [int]$v1.Code) "the next versionCode ($NextVersionCode) is higher than the installed one ($($v1.Code))"

# 3. Install over it
$r = "$(& $adb install -r $nextApk 2>&1)"
$out.Add("adb install -r $(Split-Path $nextApk -Leaf): $($r.Trim())")
Check ($r -match 'Success') "same key: install over the release succeeds"
$v2 = Installed
$out.Add("Installed after: $($v2.Name) (versionCode $($v2.Code)), first install $($v2.First), last update $($v2.Last)")
Check ($v2.Code -eq "$NextVersionCode" -and $v2.Name -eq $NextVersionName) "the new version is installed"
Check ($v2.First -eq $v1.First) "first install time unchanged (an update, not a new install)"

# 4. Data right after the update, before the app is opened
$out.Add("Data after the update (before opening the app):")
Compare-Dumps $before (Dump) "after the update"

# 5. Open the app
& $adb shell input keyevent KEYCODE_WAKEUP
& $adb shell am start -n "$pkg/com.example.tracker.MainActivity" | Out-Null
Start-Sleep 5
& $adb shell uiautomator dump /sdcard/update-ui.xml | Out-Null
$ui = [string](& $adb exec-out cat /sdcard/update-ui.xml)
$names = @($before["patient"]) | Select-Object -Skip 1 | ForEach-Object { ($_ -split '\|')[1] }
foreach ($n in $names) { Check ($ui -match [regex]::Escape($n)) "home shows the patient '$n' after the update" }
$alarms = @(& $adb shell dumpsys alarm | Select-String "$pkg\.action\.SLOT")
Check ($alarms.Count -gt 0) "reminders booked after the update ($($alarms.Count) slot alarm entries)"
& $adb shell input keyevent KEYCODE_HOME
Start-Sleep 2
$out.Add("Data after opening the updated app:")
$opened = Dump
Compare-Dumps $before $opened "after opening the updated app"

# 6. Another key
$work = Join-Path $env:TEMP "update-test"
New-Item -ItemType Directory -Force $work | Out-Null
$other = Join-Path $work "other-key.apk"
$bt = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$debugKs = Join-Path $env:USERPROFILE ".android\debug.keystore"
& (Join-Path $bt.FullName "apksigner.bat") sign --ks $debugKs --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out $other $nextApk 2>&1 | Out-Null
$otherCert = "$(& (Join-Path $bt.FullName "apksigner.bat") verify --print-certs $other 2>&1 | Where-Object { "$_" -match 'certificate DN' })"
$out.Add("Re-signed copy for the negative test: $($otherCert.Trim())")
$r2 = "$(& $adb install -r $other 2>&1)"
$out.Add("adb install -r (another key): $($r2.Trim())")
Check ($r2 -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') "another key: Android refuses the update (INSTALL_FAILED_UPDATE_INCOMPATIBLE)"
$v3 = Installed
Check ($v3.Code -eq $v2.Code -and $v3.Last -eq $v2.Last) "another key: the installed app is unchanged ($($v3.Name), versionCode $($v3.Code))"
$out.Add("Data after the refused install:")
Compare-Dumps $opened (Dump) "after the refused install"
Remove-Item -LiteralPath $other -Force -ErrorAction SilentlyContinue
Check (-not (Test-Path $other)) "the re-signed copy is deleted"
$out.Add("The only way to install an APK with another key is to uninstall first, which deletes all data. Never a path.")

$out.Add($(if ($fails.Count -eq 0) { "RESULT: all checks passed" } else { "RESULT: $($fails.Count) check(s) FAILED: " + ($fails -join "; ") }))
if ($Report) { [IO.File]::WriteAllLines($Report, [string[]]$out, (New-Object Text.UTF8Encoding $false)) }
$out
if ($fails.Count -gt 0) { exit 1 }
