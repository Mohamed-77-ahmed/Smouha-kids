# Takes the documentation screenshots (docs/screenshots/) on the TEST emulator only (Pixel_API_34_test),
# from the installed build, with the FICTIONAL data of scripts/seed-demo.ps1.
#   set ANDROID_SERIAL=emulator-5560
#   powershell -File scripts\screenshots.ps1 [-Out docs\screenshots] [-Only home,about,sizes] [-Langs en,ar] [-Themes light,dark]
# (each run adds its list to last-run.txt; split by language and theme to keep a run short)
# Screens: home, add_patient, pin_sheet, reward_child, reward_teen, reward_adult, streak_broken, about,
# in English and Arabic, light and dark, font 1.0 and 1.5; then home on the 4 screen sizes (CLAUDE.md 14).
# The app language is set the way the app sets it (Arabic with Western digits). At the end the screen
# size, density, font scale, dark mode and app language are reset. Refuses any other device (ui.ps1).
param([string]$Out = "docs\screenshots", [string[]]$Only = @(), [string[]]$Langs = @("en", "ar"), [string[]]$Themes = @("light", "dark"))
$ErrorActionPreference = "Stop"
# With -File the list arrives as one string ("home,about").
$Only = @($Only | ForEach-Object { $_ -split "," } | Where-Object { $_ })
$Langs = @($Langs | ForEach-Object { $_ -split "," } | Where-Object { $_ })
$Themes = @($Themes | ForEach-Object { $_ -split "," } | Where-Object { $_ })
. "$PSScriptRoot\..\docs\evidence\tools\ui.ps1"
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Out | Out-Null
$seed = "$PSScriptRoot\seed-demo.ps1"
$main = "$pkg/com.example.tracker.MainActivity"
$log = @()

function Want([string]$screen) { $Only.Count -eq 0 -or $Only -contains $screen }
function Seed([string[]]$a) { powershell -NoProfile -ExecutionPolicy Bypass -File $seed @a | Out-Null; Start-Sleep 5 }
function Grab([string]$name) {
    & $adb shell screencap -p /sdcard/shot.png
    & $adb pull /sdcard/shot.png "$Out\$name.png" | Out-Null
    $script:log += $name
}

function Set-Look([string]$lang, [string]$theme, [string]$font) {
    $tag = if ($lang -eq "ar") { "ar-EG-u-nu-latn" } else { "en" }
    & $adb shell cmd locale set-app-locales $pkg --locales $tag
    & $adb shell cmd uimode night $(if ($theme -eq "dark") { "yes" } else { "no" }) | Out-Null
    & $adb shell settings put system font_scale $font
    Start-Sleep 2
}

# The overlay (reward, streak ended) is up for 3 s: take a burst and keep a frame where it is fully
# shown. A frame shows it when a page margin (left and right edge, lower part) differs from the plain
# page in any color channel: the light backdrop is close to the page's brightness at the left edge
# (cream on light sky) but not at the right (soft yellow), so brightness alone is not enough.
function Column-Mean([string]$file) {
    $bmp = [System.Drawing.Bitmap]::FromFile((Resolve-Path $file))
    try {
        $means = @()
        foreach ($fx in 0.02, 0.98) {
            $x = [int]($bmp.Width * $fx); $r = 0; $g = 0; $b = 0; $n = 0
            for ($y = [int]($bmp.Height * 0.82); $y -lt [int]($bmp.Height * 0.94); $y += 4) {
                $c = $bmp.GetPixel($x, $y); $r += $c.R; $g += $c.G; $b += $c.B; $n++
            }
            $means += @(($r / $n), ($g / $n), ($b / $n))
        }
        return $means
    } finally { $bmp.Dispose() }
}
function Margin-Diff($a, $b) { (0..($a.Count - 1) | ForEach-Object { [math]::Abs($a[$_] - $b[$_]) } | Measure-Object -Maximum).Maximum }
function Burst-Overlay([string]$name) {
    & $adb shell 'for i in 0 1 2 3 4 5 6 7 8 9 10 11 12 13; do screencap -p /sdcard/burst$i.png; done'
    $frames = 0..13 | ForEach-Object { $f = "$env:TEMP\burst_$_.png"; & $adb pull "/sdcard/burst$_.png" $f | Out-Null; $f }
    Start-Sleep 4
    & $adb shell screencap -p /sdcard/plain.png; & $adb pull /sdcard/plain.png "$env:TEMP\burst_plain.png" | Out-Null
    $plain = Column-Mean "$env:TEMP\burst_plain.png"
    $shown = @($frames | Where-Object { (Margin-Diff (Column-Mean $_) $plain) -gt 25 })
    if ($shown.Count -eq 0) { $script:log += "$name MISSING (overlay not caught)"; return }
    # Still up at the end of the burst: the last frame (count-up done). Otherwise the second-to-last
    # shown frame: fully in and not yet fading out.
    $pick = if ($shown[-1] -eq $frames[-1]) { $shown[-1] } elseif ($shown.Count -ge 2) { $shown[$shown.Count - 2] } else { $shown[0] }
    Copy-Item $pick "$Out\$name.png" -Force
    $script:log += $name
}

function Tap-Id([string]$id) {
    $n = Get-UiNodes | Where-Object { $_.Id -eq $id } | Select-Object -First 1
    if (-not $n) { Scroll-Down; $n = Get-UiNodes | Where-Object { $_.Id -eq $id } | Select-Object -First 1 }
    if (-not $n) { throw "not found: $id" }
    & $adb shell input tap $n.X $n.Y
}
function Tap-Chip([string]$age) {
    $n = Get-UiNodes | Where-Object { $_.Text -like "*· $age" } | Select-Object -First 1
    if (-not $n) { throw "chip not found: $age" }
    & $adb shell input tap $n.X $n.Y
    Start-Sleep 1
}
# True once a view with this id is on screen (polls the UI dump).
function Wait-Id([string]$id, [int]$seconds) {
    $end = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $end) {
        if (Get-UiNodes | Where-Object { $_.Id -eq $id }) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}
function Type-Pin { & $adb shell input text 1234; & $adb shell input keyevent KEYCODE_ENTER }

$anyCombo = @("home", "add_patient", "pin_sheet", "reward_child", "reward_teen", "reward_adult", "streak_broken", "about") | Where-Object { Want $_ }
foreach ($lang in $Langs) {
    if (-not $anyCombo) { break }
    foreach ($theme in $Themes) {
        foreach ($font in "1.0", "1.5") {
            $sfx = "${lang}_${theme}_font$($font.Replace('.', ''))"
            Set-Look $lang $theme $font
            Seed @("-Lang", $lang)
            if (Want "home") { Grab "home_$sfx" }
            if ((Want "pin_sheet") -or (Want "reward_child")) {
                Tap-Chip "7"; Tap-Id "takeButton"; Start-Sleep 1.5
                if (Want "pin_sheet") { Grab "pin_sheet_$sfx" }
                Type-Pin; Burst-Overlay "reward_child_$sfx"
            }
            if (Want "reward_teen") { Tap-Chip "15"; Tap-Id "takeButton"; Start-Sleep 1.5; Type-Pin; Burst-Overlay "reward_teen_$sfx" }
            if (Want "reward_adult") { Tap-Chip "40"; Tap-Id "takeButton"; Start-Sleep 1.5; Type-Pin; Burst-Overlay "reward_adult_$sfx" }
            if (Want "about") {
                Tap-Id "settingsButton"; Start-Sleep 2
                Tap-Scrolled "aboutButton"; Start-Sleep 2
                Grab "about_$sfx"
                & $adb shell input keyevent KEYCODE_BACK; Start-Sleep 1; & $adb shell input keyevent KEYCODE_BACK
            }
            if (Want "streak_broken") { & $adb shell am force-stop $pkg; powershell -NoProfile -ExecutionPolicy Bypass -File $seed -Lang $lang -Broken child | Out-Null; Burst-Overlay "streak_broken_$sfx" }
            if (Want "add_patient") {
                Seed @("-Lang", $lang, "-Empty")
                Start-Sleep 6   # the mascot's 3 breaths end; uiautomator needs a settled screen
                # Waits for each screen instead of sleeping: the PIN sheet, then the form.
                $tries = 0
                while (-not (Wait-Id "keypad" 4) -and $tries -lt 3) { Tap-Id "addButton"; $tries++ }
                Type-Pin
                if (Wait-Id "newNameInput" 10) { Start-Sleep 1; Grab "add_patient_$sfx" } else { $script:log += "add_patient_$sfx MISSING (form not reached)" }
                & $adb shell input keyevent KEYCODE_BACK
            }
        }
    }
}

if (Want "sizes") {
    $sizes = [ordered]@{ small = @("480x800", 240); normal = @("1080x2400", 420); tablet = @("1600x2560", 320); unfolded = @("2208x1840", 420) }
    foreach ($lang in $Langs) {
        Set-Look $lang "light" "1.0"
        foreach ($k in $sizes.Keys) {
            & $adb shell wm size $sizes[$k][0]; & $adb shell wm density $sizes[$k][1]
            Seed @("-Lang", $lang)
            Grab "home_size_${k}_$lang"
        }
        & $adb shell wm size reset; & $adb shell wm density reset
    }
}

# Back to defaults (CLAUDE.md 0b).
& $adb shell wm size reset; & $adb shell wm density reset
& $adb shell settings put system font_scale 1.0
& $adb shell cmd uimode night no | Out-Null
& $adb shell cmd locale set-app-locales $pkg --locales en
$log | Add-Content -Encoding utf8 "$Out\last-run.txt"
"screenshots: " + $log.Count + " entries, " + @($log | Where-Object { $_ -like "*MISSING*" }).Count + " missing -> $Out"
