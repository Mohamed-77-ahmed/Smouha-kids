# Helpers used to drive the TEST emulator for evidence. Dot-source: . docs\evidence\tools\ui.ps1
# Set $global:evidence to the folder where screenshots and dumps go.
# Input events, settings changes and installs only ever go to Pixel_API_34_test (CLAUDE.md 0b):
# loading this file refuses any other device (phone or the manual emulator Pixel_API_34).
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$global:adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$global:pkg = "com.smouha.kidsreminder"
if (-not $env:ANDROID_SERIAL -or $env:ANDROID_SERIAL -eq "emulator-5554") { $env:ANDROID_SERIAL = "emulator-5560" }
$uiAvd = "$(& $adb -s $env:ANDROID_SERIAL emu avd name 2>$null | Select-Object -First 1)".Trim()
if ($uiAvd -ne "Pixel_API_34_test") { throw "ui.ps1: refusing, $env:ANDROID_SERIAL is '$uiAvd', not Pixel_API_34_test" }
if (-not $global:evidence) { $global:evidence = "docs\evidence\phase-1" }

function Get-UiXml {
    # Right after a screen change uiautomator can fail with "null root node"; try again.
    for ($i = 0; $i -lt 4; $i++) {
        $r = & $adb shell uiautomator dump /sdcard/ui.xml 2>&1
        if ("$r" -notmatch 'ERROR') { break }
        Start-Sleep -Milliseconds 800
    }
    return [string](& $adb exec-out cat /sdcard/ui.xml)
}

function Get-UiNodes {
    $xml = Get-UiXml
    [regex]::Matches($xml, '<node [^>]*?text="([^"]*)" resource-id="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?checked="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') | ForEach-Object {
        [pscustomobject]@{
            Text = [System.Net.WebUtility]::HtmlDecode($_.Groups[1].Value)
            Id = ($_.Groups[2].Value -replace '.*:id/', '')
            Desc = [System.Net.WebUtility]::HtmlDecode($_.Groups[3].Value)
            Checked = $_.Groups[4].Value -eq 'true'
            X = [int](([int]$_.Groups[5].Value + [int]$_.Groups[7].Value) / 2)
            Y = [int](([int]$_.Groups[6].Value + [int]$_.Groups[8].Value) / 2)
            Left = [int]$_.Groups[5].Value; Top = [int]$_.Groups[6].Value; Right = [int]$_.Groups[7].Value; Bottom = [int]$_.Groups[8].Value
        }
    }
}

function Show-Ui { Get-UiNodes | Where-Object { $_.Text -or $_.Desc -or $_.Id } | ForEach-Object { "{0} | {1} | {2} | ({3},{4})" -f $_.Text, $_.Id, $_.Desc, $_.X, $_.Y } }

function Find-Ui([string]$match) {
    Get-UiNodes | Where-Object { $_.Text -eq $match -or $_.Id -eq $match -or $_.Desc -eq $match } | Select-Object -First 1
}

function Tap-Ui([string]$match, [int]$wait = 700) {
    $n = Find-Ui $match
    if (-not $n) { throw "UI element not found: $match" }
    & $adb shell input tap $n.X $n.Y
    Start-Sleep -Milliseconds $wait
    "tapped '$match' at ($($n.X),$($n.Y))"
}

function Type-Text([string]$text) {
    # input text only handles ASCII; spaces must be %s
    & $adb shell input text ($text -replace ' ', '%s')
}

function Shot([string]$name) {
    $path = Join-Path $global:evidence "$name.png"
    cmd /c "`"$adb`" exec-out screencap -p > `"$path`""
    $path
}

function Save-Text([string]$name, [string[]]$lines) {
    $path = Join-Path $global:evidence "$name.txt"
    [IO.File]::WriteAllLines((Resolve-Path -LiteralPath $global:evidence).Path + "\$name.txt", $lines, (New-Object System.Text.UTF8Encoding $false))
    $path
}

function Sql([string]$query) {
    # Needs `adb root` (google_apis image). Reads the live app DB, WAL included.
    # The query is wrapped in single quotes for the device shell, so use "..." for SQL strings.
    & $adb shell "sqlite3 -header -separator '|' /data/data/$pkg/databases/pill_pals.db '$query'"
}

function Enter-Pin([string]$pin = "1234") {
    # The PIN sheet has its own keypad and also takes hardware keys: the digits, then Enter.
    Start-Sleep -Milliseconds 800      # the sheet slides in
    & $adb shell input text $pin
    & $adb shell input keyevent KEYCODE_ENTER
    Start-Sleep -Milliseconds 3000     # the PBKDF2 check takes about 2 s on the emulator
}

function Tap-PinKeys([string]$digits) {
    # Taps the keypad itself (key0..key9, then OK), language independent.
    foreach ($d in $digits.ToCharArray()) { Tap-Ui "key$d" 200 | Out-Null }
    Tap-Ui "keyOk" 3000 | Out-Null
}

function Hide-Keyboard {
    # Back only when the keyboard is up, otherwise Back would leave the screen.
    if ((& $adb shell dumpsys input_method) -match 'mInputShown=true') { & $adb shell input keyevent KEYCODE_BACK; Start-Sleep -Milliseconds 500 }
}

function Scroll-Down { Hide-Keyboard; & $adb shell input swipe 540 1800 540 700 300; Start-Sleep -Milliseconds 600 }
function Scroll-Up { Hide-Keyboard; & $adb shell input swipe 540 700 540 2100 200; Start-Sleep -Milliseconds 400 }

function Tap-Scrolled([string]$match, [int]$tries = 4) {
    for ($i = 0; $i -lt $tries; $i++) {
        $n = Find-Ui $match
        if ($n -and $n.Y -gt 300 -and $n.Y -lt 2290) { & $adb shell input tap $n.X $n.Y; Start-Sleep -Milliseconds 700; return "tapped '$match'" }
        Scroll-Down
    }
    throw "not reachable: $match"
}

# Picks a time in MaterialTimePicker (12 h clock face, English content descriptions).
function Pick-Time([int]$hour24, [int]$minute) {
    $h12 = $hour24 % 12; if ($h12 -eq 0) { $h12 = 12 }
    $face = Get-UiNodes | Where-Object { $_.Desc -eq "$h12 o'clock" -and $_.Id -eq '' } | Select-Object -First 1
    & $adb shell input tap $face.X $face.Y; Start-Sleep -Milliseconds 800
    $mm = "{0:D2} minutes" -f $minute
    $m = Get-UiNodes | Where-Object { $_.Desc -eq $mm -and $_.Id -eq '' } | Select-Object -First 1
    if (-not $m) { throw "minute $mm not on face" }
    & $adb shell input tap $m.X $m.Y; Start-Sleep -Milliseconds 500
    if ($hour24 -ge 12) { Tap-Ui "material_clock_period_pm_button" | Out-Null } else { Tap-Ui "material_clock_period_am_button" | Out-Null }
    Tap-Ui "material_timepicker_ok_button" 900 | Out-Null
}

# Adds a medicine through the English UI. $patientIndex = index in the dropdown (-1 = no dropdown,
# new patient fields shown directly). New patient when $newName is set.
function Add-Med([int]$patientIndex, [string]$newName, [string]$newAge, [string]$medicine, [int[]]$hours, [int[]]$minutes, [string]$interval = "10 min") {
    Tap-Ui "addButton" | Out-Null; Enter-Pin
    if ($patientIndex -ge 0) {
        Tap-Ui "patientDropdown" 1000 | Out-Null
        & $adb shell input tap 227 (637 + 126 * $patientIndex); Start-Sleep -Milliseconds 800
    }
    if ($newName) { Tap-Ui "newNameInput" | Out-Null; Type-Text $newName; Tap-Ui "newAgeInput" | Out-Null; Type-Text $newAge }
    Tap-Ui "nameInput" | Out-Null; Type-Text $medicine; Hide-Keyboard
    for ($i = 0; $i -lt $hours.Count; $i++) { Tap-Scrolled "addTimeButton" | Out-Null; Start-Sleep -Milliseconds 600; Pick-Time $hours[$i] $minutes[$i] }
    Tap-Scrolled $interval | Out-Null
    Tap-Scrolled "saveButton" | Out-Null; Start-Sleep 2
    if (Find-Ui "alertTitle") { Tap-Ui "button2" | Out-Null }   # notification rationale: decide separately
}

function Notifs {
    # Title and text of this app's posted notifications.
    $dump = & $adb shell dumpsys notification --noredact
    $out = @(); $inApp = $false
    foreach ($l in $dump) {
        if ($l -match 'NotificationRecord\(.*pkg=com\.example\.tracker') { $inApp = $true; $id = [regex]::Match($l, 'id=(-?\d+)').Groups[1].Value; continue }
        if ($l -match 'NotificationRecord\(') { $inApp = $false }
        if ($inApp -and $l -match 'android\.(title|text)=String \((.*)\)') { $out += "id=$id $($Matches[1])=$($Matches[2])" }
    }
    $out
}

function App-Alarms {
    & $adb shell dumpsys alarm | Select-String -Context 0,3 "RTC_WAKEUP #\d+: Alarm\{.*$pkg" | ForEach-Object {
        ($_.Context.PostContext | Where-Object { $_ -match 'tag=|origWhen=' }) -join ' ' -replace '\s+', ' '
    }
}
