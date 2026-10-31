# Screenshots of the PIN sheet on the TEST emulator (CLAUDE.md 14): sizes, font scales, languages,
# light/dark, landscape. Dot-sources ui.ps1 (which refuses any device but Pixel_API_34_test).
# Usage: . docs\evidence\tools\pin-matrix.ps1; Shot-PinSheet -Name n -Size 1080x2400 -Density 420 -Font 1.0 -Lang en
. "$PSScriptRoot\ui.ps1"

function Set-Display([string]$Size, [int]$Density, [string]$Font, [string]$Lang, [bool]$Dark = $false, [bool]$Landscape = $false) {
    & $adb shell wm size $Size | Out-Null
    & $adb shell wm density $Density | Out-Null
    & $adb shell settings put system font_scale $Font | Out-Null
    $tag = if ($Lang -eq "ar") { "ar-EG-u-nu-latn" } else { "en" }
    & $adb shell cmd locale set-app-locales $pkg --locales $tag | Out-Null
    & $adb shell cmd uimode night $(if ($Dark) { "yes" } else { "no" }) | Out-Null
    Start-Sleep 1
}

# Turns the display (with the app in front: the launcher itself stays portrait) and waits for it.
function Set-Rotation([bool]$Landscape) {
    $want = if ($Landscape) { '90' } else { '0' }
    for ($i = 0; $i -lt 5; $i++) {
        & $adb shell wm user-rotation lock $(if ($Landscape) { 1 } else { 0 }) | Out-Null
        Start-Sleep 2
        $now = (& $adb shell dumpsys window displays | Select-String -Pattern 'mCurrentRotation=ROTATION_(\d+)' | Select-Object -First 1).Matches.Groups[1].Value
        if ($now -eq $want) { return }
    }
    throw "rotation did not reach $want"
}

function Reset-Display {
    & $adb shell wm size reset | Out-Null
    & $adb shell wm density reset | Out-Null
    & $adb shell settings put system font_scale 1.0 | Out-Null
    & $adb shell cmd locale set-app-locales $pkg --locales en | Out-Null
    & $adb shell cmd uimode night no | Out-Null
    & $adb shell wm user-rotation lock 0 | Out-Null
    & $adb shell wm user-rotation free | Out-Null
}

# Screen size in pixels as the device reports it now (after wm size and rotation).
function Get-ScreenPx {
    $xml = Get-UiXml
    $m = [regex]::Match($xml, 'bounds="\[0,0\]\[(\d+),(\d+)\]"')
    if ($m.Success) { return @([int]$m.Groups[1].Value, [int]$m.Groups[2].Value) }
    return @(1080, 2400)
}

# Taps [match] once it is fully on screen, scrolling the page up in steps of half a screen.
function Tap-OnScreen([string]$match, [int]$tries = 6) {
    $w, $h = Get-ScreenPx
    for ($i = 0; $i -lt $tries; $i++) {
        $n = Find-Ui $match
        if ($n -and $n.Top -ge 0 -and $n.Bottom -le $h -and $n.Bottom -gt $n.Top) {
            & $adb shell input tap $n.X $n.Y; Start-Sleep -Milliseconds 900; return "tapped '$match'"
        }
        & $adb shell input swipe ([int]($w / 2)) ([int]($h * 0.75)) ([int]($w / 2)) ([int]($h * 0.35)) 300
        Start-Sleep -Milliseconds 700
    }
    throw "not reachable: $match"
}

# Opens parent settings > Change PIN on a fresh start and takes a screenshot of the sheet.
# -Step2 also enters the current PIN, so the step indicator shows step 2 of 3.
function Shot-PinSheet([string]$Name, [string]$Size, [int]$Density, [string]$Font, [string]$Lang, [bool]$Dark = $false, [bool]$Landscape = $false, [bool]$Step2 = $false) {
    Set-Display $Size $Density $Font $Lang $Dark $Landscape
    & $adb shell am force-stop $pkg
    & $adb shell input keyevent KEYCODE_WAKEUP
    & $adb shell am start -n "$pkg/.MainActivity" | Out-Null; Start-Sleep 4
    Set-Rotation $Landscape
    Tap-OnScreen "settingsButton" | Out-Null; Start-Sleep 1
    Tap-OnScreen "changePinButton" | Out-Null; Start-Sleep 1
    # Taps (not hardware keys): key events would switch to keyboard navigation and show a focus highlight.
    if ($Step2) { Tap-PinKeys "1234" }
    Shot $Name
}
