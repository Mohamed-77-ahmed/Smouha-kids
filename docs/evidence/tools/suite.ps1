# Full suite on the TEST emulator only (Pixel_API_34_test), through scripts/run-tests.cmd.
# Usage: powershell -File docs\evidence\tools\suite.ps1 -Report docs\evidence\<item>\suite.txt [-Snapshot <name>] [-Serial emulator-5560]
# 1. Refuses unless the serial belongs to the test AVD (no phone, no manual emulator).
# 2. Closes a stray "isn't responding" dialog and wakes the screen (UI tests need window focus).
# 3. Runs unit tests, lint, assembleDebug and all instrumented tests (app left installed), prints counts.
# 4. Optional snapshot of the project without build folders and signing files to ../tracker-snapshots/<name>.
param([string]$Report, [string]$Snapshot, [string]$Serial = "emulator-5560")
$env:ANDROID_SERIAL = $Serial
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = @("Suite run " + (Get-Date -Format "yyyy-MM-dd HH:mm") + " on $Serial")

$avd = (& $adb -s $Serial emu avd name 2>$null | Select-Object -First 1)
if ("$avd".Trim() -ne "Pixel_API_34_test") { throw "suite.ps1: refusing, $Serial is '$avd', not Pixel_API_34_test" }
$out += "Device: AVD $avd"

& $adb root | Out-Null; Start-Sleep 2; & $adb wait-for-device
& $adb shell input keyevent KEYCODE_WAKEUP
& $adb shell wm dismiss-keyguard
& $adb shell uiautomator dump /sdcard/suite-ui.xml | Out-Null
if ((& $adb exec-out cat /sdcard/suite-ui.xml) -match 'resource-id="android:id/aerr_wait"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
    & $adb shell input tap ([int](([int]$Matches[1] + [int]$Matches[3]) / 2)) ([int](([int]$Matches[2] + [int]$Matches[4]) / 2))
    $out += "Closed an 'isn't responding' dialog (Wait) before the run"
}
& $adb shell input keyevent KEYCODE_HOME

$g = cmd /c "scripts\run-tests.cmd testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest --console=plain -q 2>&1"
$code = $LASTEXITCODE
$out += "scripts\run-tests.cmd testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest -> exit $code"
if ($code -ne 0) { $out += ($g | Select-Object -Last 40) }

$u = Get-ChildItem app\build\test-results\testDebugUnitTest -Filter *.xml -ErrorAction SilentlyContinue | ForEach-Object { ([xml](Get-Content $_.FullName)).testsuite }
$out += "Unit: tests=" + ($u | Measure-Object -Property tests -Sum).Sum + " failures=" + ($u | Measure-Object -Property failures -Sum).Sum + " errors=" + ($u | Measure-Object -Property errors -Sum).Sum
$i = Get-ChildItem app\build\outputs\androidTest-results\connected -Recurse -Filter *.xml -ErrorAction SilentlyContinue | ForEach-Object { Select-String -Path $_.FullName -Pattern '<testsuite ' } | ForEach-Object { [regex]::Match($_.Line, 'name="([^"]+)" tests="(\d+)" failures="(\d+)" errors="(\d+)"') }
$out += "Instrumented: tests=" + ($i | ForEach-Object { [int]$_.Groups[2].Value } | Measure-Object -Sum).Sum + " failures=" + ($i | ForEach-Object { [int]$_.Groups[3].Value + [int]$_.Groups[4].Value } | Measure-Object -Sum).Sum
$i | ForEach-Object { $out += "  " + $_.Groups[1].Value + " " + $_.Groups[2].Value + " (failed " + $_.Groups[3].Value + ")" }
# The result XML root can be <testsuites>, so search the whole document.
$fails = Get-ChildItem app\build\outputs\androidTest-results\connected -Recurse -Filter *.xml -ErrorAction SilentlyContinue | ForEach-Object { ([xml](Get-Content $_.FullName)).SelectNodes("//testcase[failure]") }
$fails | ForEach-Object { $out += "  FAILED " + $_.classname + "." + $_.name + ": " + (($_.SelectSingleNode("failure").InnerText -split "`n")[0]) }
$lint = [xml](Get-Content app\build\reports\lint-results-debug.xml)
$out += "Lint: errors=" + @($lint.issues.issue | Where-Object { $_.severity -match 'Error|Fatal' }).Count + " warnings=" + @($lint.issues.issue | Where-Object { $_.severity -eq 'Warning' }).Count
$lint.issues.issue | ForEach-Object { $loc = $_.location | Select-Object -First 1; $out += "  {0} {1} {2}:{3}" -f $_.severity, $_.id, $loc.file.Split('\')[-1], $loc.line }

& $adb uninstall "com.smouha.kidsreminder.test" 2>$null | Out-Null
if ($Snapshot) {
    # Without build folders and without keystore.properties / *.jks / *.keystore (snapshot.ps1).
    $out += (& "$PSScriptRoot\snapshot.ps1" -Name $Snapshot)
}
if ($Report) { [IO.File]::WriteAllLines((Join-Path (Get-Location) $Report), [string[]]$out, (New-Object System.Text.UTF8Encoding $false)) }
$out
