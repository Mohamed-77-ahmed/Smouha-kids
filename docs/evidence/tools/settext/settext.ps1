# Types text (Arabic too) into a field of the app on the TEST emulator, for screenshots and demo data:
# adb's "input text" only handles ASCII. Refuses any device but Pixel_API_34_test (CLAUDE.md 0b).
#   . docs\evidence\tools\settext\settext.ps1; Set-FieldText newNameInput "<text>"
# The first call builds settext.jar (UiAutomator 1.0 test, compiled with the SDK's android.jar and
# uiautomator.jar, dexed with d8) and pushes it to /data/local/tmp.
$st = @{ Dir = $PSScriptRoot; Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" }
if (-not $env:ANDROID_SERIAL) { $env:ANDROID_SERIAL = "emulator-5560" }
if ("$(& $st.Adb -s $env:ANDROID_SERIAL emu avd name 2>$null | Select-Object -First 1)".Trim() -ne "Pixel_API_34_test") { throw "settext: refusing, $env:ANDROID_SERIAL is not Pixel_API_34_test" }

function Build-SetText {
    $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    $platform = Get-ChildItem "$sdk\platforms" -Directory | Sort-Object Name | Select-Object -Last 1
    $bt = Get-ChildItem "$sdk\build-tools" -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } | Select-Object -Last 1
    $junit = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\junit\junit" -Recurse -Filter "junit-4*.jar" | Where-Object { $_.Name -notmatch 'sources' } | Select-Object -First 1
    $work = Join-Path $env:TEMP "settext"
    New-Item -ItemType Directory -Force $work | Out-Null
    Get-ChildItem $work -File | Remove-Item -Force
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    $cp = "$($platform.FullName)\android.jar;$($platform.FullName)\uiautomator.jar;$($junit.FullName)"
    & "$env:JAVA_HOME\bin\javac.exe" -nowarn -source 11 -target 11 -cp $cp -d $work (Join-Path $st.Dir "SetText.java") 2>&1 | Out-Null
    $classes = @(Get-ChildItem $work -Filter *.class | ForEach-Object { $_.FullName })
    if ($classes.Count -lt 2) { throw "settext: javac produced no classes" }
    $jar = Join-Path $work "settext.jar"
    & "$($bt.FullName)\d8.bat" --lib "$($platform.FullName)\android.jar" --classpath "$($platform.FullName)\uiautomator.jar" --output $jar @classes 2>&1 | Out-Null
    if (-not (Test-Path $jar) -or (Get-Item $jar).Length -lt 200) { throw "settext: d8 produced no dex" }
    & $st.Adb push $jar /data/local/tmp/settext.jar | Out-Null
}

function Set-FieldText([string]$id, [string]$text) {
    if (-not "$(& $st.Adb shell ls /data/local/tmp/settext.jar 2>$null)".Contains("settext.jar")) { Build-SetText }
    $cps = (($text.ToCharArray() | ForEach-Object { '{0:x}' -f [int]$_ }) -join '.')
    # android.test.base.jar: the Android 14 launcher script leaves it off the class path, and the old
    # runner then aborts (android.test.RepetitiveTest missing) while still printing "OK (1 test)".
    $r = "$(& $st.Adb shell uiautomator runtest /data/local/tmp/settext.jar /system/framework/android.test.base.jar -c SetText -e id $id -e cps $cps)"
    # Per test: code 1 = started, 0 = passed, -2 = failed (an "-1" at the very end is the run's RESULT_OK).
    if ($r -notmatch 'OK \(1 test\)' -or $r -notmatch 'INSTRUMENTATION_STATUS_CODE: 0' -or $r -match 'INSTRUMENTATION_STATUS_CODE: -2|Failed resolution|FAILURES|shortMsg') { throw "settext: could not set $id ($r)" }
}
