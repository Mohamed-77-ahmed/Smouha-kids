# Writes docs/palette-v2.md and docs/palette-v2.png from the real color tokens (values/colors.xml and
# values-night/colors.xml): every token with its hex and role, and the contrast of every pair that
# ContrastTest checks (same formula, same pairs). Run again after any color change.
#   powershell -File scripts\make-palette-sheet.ps1
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$root = Resolve-Path "$PSScriptRoot\.."
$res = "$root\app\src\main\res"

function Read-Tokens([string]$dir) {
    [xml]$x = Get-Content "$res\$dir\colors.xml" -Encoding UTF8
    $t = [ordered]@{}
    foreach ($c in $x.resources.color) { $t[$c.name] = $c.InnerText.Trim().ToUpper() }
    $t
}
function Rgb([string]$hex) { $h = $hex.TrimStart('#'); if ($h.Length -eq 8) { $h = $h.Substring(2) }; @([Convert]::ToInt32($h.Substring(0, 2), 16), [Convert]::ToInt32($h.Substring(2, 2), 16), [Convert]::ToInt32($h.Substring(4, 2), 16)) }
function Alpha([string]$hex) { $h = $hex.TrimStart('#'); if ($h.Length -eq 8) { [Convert]::ToInt32($h.Substring(0, 2), 16) } else { 255 } }
function Lin([double]$v) { if ($v -le 0.03928) { $v / 12.92 } else { [math]::Pow(($v + 0.055) / 1.055, 2.4) } }
function Lum([string]$hex) { $c = Rgb $hex; 0.2126 * (Lin ($c[0] / 255.0)) + 0.7152 * (Lin ($c[1] / 255.0)) + 0.0722 * (Lin ($c[2] / 255.0)) }
function Ratio([string]$a, [string]$b) { $l1 = Lum $a; $l2 = Lum $b; ([math]::Max($l1, $l2) + 0.05) / ([math]::Min($l1, $l2) + 0.05) }
# [over] (with alpha) painted on the opaque [under], truncated like ContrastTest.blend.
function Blend([string]$over, [string]$under) {
    $a = (Alpha $over) / 255.0; $o = Rgb $over; $u = Rgb $under
    "#" + ((0..2 | ForEach-Object { "{0:X2}" -f [int][math]::Floor(($o[$_] / 255.0 * $a + $u[$_] / 255.0 * (1 - $a)) * 255) }) -join "")
}

$roles = [ordered]@{
    primary = "Rooster orange family (deep in light mode, light in dark mode): main buttons (I took it, PIN OK), settings section titles, PIN dots, the reward title, the day's progress line"
    on_primary = "Text and icons on primary"
    primary_container = "Apricot tone: the pill circle on dose cards, an About card"
    on_primary_container = "Text on primary_container"
    secondary = "Ochre tone of the yellow: where Material uses secondary as a text or icon color"
    on_secondary = "Text on secondary"
    secondary_container = "Sunny yellow from the pictures (light mode; deep gold in dark mode): selected chip, PIN keys, tonal buttons, an About card"
    on_secondary_container = "Text on secondary_container (dark brown on the sunny yellow)"
    tertiary = "Sky blue tone: Material's tertiary accents"
    on_tertiary = "Text on tertiary"
    tertiary_container = "Sky blue (koko_sky in light mode)"
    on_tertiary_container = "Text on tertiary_container"
    background = "Page background (light sky; dark sky-tinted navy)"
    on_background = "Text on the page"
    surface = "Surface (same as the page)"
    on_surface = "Main text"
    surface_variant = "Sky grey: soft boxes"
    on_surface_variant = "Secondary text"
    outline = "Borders: chips, outlined buttons, empty PIN dots"
    surface_lowest = "The reward card (white in light mode)"
    surface_low = "Dose cards, the PIN sheet, dialogs (warm cream in light mode)"
    surface_container = "Surface step (Material components)"
    surface_high = "Surface step (Material components)"
    surface_highest = "Surface step (switch tracks)"
    error = "Semantic red: missed, overdue, off, wrong PIN (never a brand color)"
    on_error = "Text on error"
    error_container = "Red box (home banner)"
    on_error_container = "Text in the red box"
    warning = "Semantic amber: the icon and border of a warning row"
    warning_container = "The warning row's box"
    on_warning_container = "Text in the warning row"
    success = "Semantic green: a dose marked taken (with the word)"
    points = "Points on the reward card"
    streak = "Streak on the reward card"
    gradient_start = "Gradient stop 1, sky blue (header, onboarding, Add medicine, PIN badge, status bar)"
    gradient_center = "Gradient stop 2, cream (dark: navy); keeps the middle from turning grey-green"
    gradient_end = "Gradient stop 3, soft yellow (dark: deep gold)"
    on_gradient = "Text and icons on the gradient"
    gradient_scrim_start = "Celebration backdrop, 90 percent of stop 1"
    gradient_scrim_center = "Celebration backdrop, 90 percent of stop 2"
    gradient_scrim_end = "Celebration backdrop, 90 percent of stop 3"
    glass = "See-through white boxes on the header (points, streak, level, hints)"
    progress_on_gradient = "Level bar on the header"
}
$surfaces = "surface", "surface_lowest", "surface_low", "surface_container", "surface_high", "surface_highest", "surface_variant"
$stops = "gradient_start", "gradient_center", "gradient_end"
# The pairs of ContrastTest: text 4.5:1, icons and borders 3:1.
$text = @()
foreach ($x in "primary", "secondary", "tertiary", "error", "primary_container", "secondary_container", "tertiary_container", "error_container", "warning_container") { $text += , @("on_$x", $x) }
$text += , @("on_background", "background")
foreach ($bg in $surfaces) { foreach ($fg in "on_surface", "on_surface_variant", "error", "success", "points", "streak", "primary") { $text += , @($fg, $bg) } }
foreach ($bg in "surface", "surface_lowest", "surface_low") { foreach ($fg in "secondary", "tertiary", "warning") { $text += , @($fg, $bg) } }
foreach ($s in $stops) { $text += , @("on_gradient", $s) }
$ui = @(); foreach ($bg in $surfaces) { $ui += , @("outline", $bg) }; $ui += , @("warning", "warning_container"); foreach ($s in $stops) { $ui += , @("progress_on_gradient", $s) }

$light = Read-Tokens "values"; $dark = Read-Tokens "values-night"
$skip = "koko_icon_bg_top", "koko_icon_bg_bottom", "koko_sky", "splash_name_text", "shadow_soft", "mascot_top", "mascot_bottom", "mascot_line", "mascot_cheek", "mascot_spark"

function PairRows($pairs, [double]$need) {
    foreach ($p in $pairs) {
        $rl = Ratio $light[$p[0]] $light[$p[1]]; $rd = Ratio $dark[$p[0]] $dark[$p[1]]
        [pscustomobject]@{ Fg = $p[0]; Bg = $p[1]; Light = $rl; Dark = $rd; Need = $need; Ok = ($rl -ge $need -and $rd -ge $need) }
    }
}
$textRows = @(PairRows $text 4.5)
$uiRows = @(PairRows $ui 3.0)
$glassRows = foreach ($s in $stops) {
    $bl = Blend $light.glass $light[$s]; $bd = Blend $dark.glass $dark[$s]
    [pscustomobject]@{ Fg = "on_gradient"; Bg = "glass over $s"; Light = (Ratio $light.on_gradient $bl); Dark = (Ratio $dark.on_gradient $bd); Need = 4.5; Ok = $true }
    [pscustomobject]@{ Fg = "progress_on_gradient"; Bg = "glass over $s"; Light = (Ratio $light.progress_on_gradient $bl); Dark = (Ratio $dark.progress_on_gradient $bd); Need = 3.0; Ok = $true }
}
foreach ($g in $glassRows) { $g.Ok = ($g.Light -ge $g.Need -and $g.Dark -ge $g.Need) }
$all = @($textRows) + @($uiRows) + @($glassRows)
$failed = @($all | Where-Object { -not $_.Ok })

# ---------- Markdown ----------
$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Palette v2 ""Koko""")
$md.Add("")
$md.Add("Batch 6, request R8 (2026-10-18). Generated by ``scripts/make-palette-sheet.ps1`` from ``app/src/main/res/values/colors.xml`` and ``values-night/colors.xml``; the swatch sheet is ``docs/palette-v2.png``. Do not edit by hand: change the tokens, then run the script again.")
$md.Add("")
$md.Add("## Where the colors come from")
$md.Add("")
$md.Add("Sampled from the Koko pictures (``design/brand/koko.jpg``): rooster orange #F38E32, sunny yellow #FDC440, comb red #F15958, sky blue #C3E2EB (the icon's sky #D4EEF7), and the platform's #E86C2C, #48B7D3, #A7BC33, #DA558E.")
$md.Add("")
$md.Add("- **Primary = the rooster orange family.** The picture's orange (#F38E32) is too light to carry white text (2.4:1) or to be read as text on cream, so primary is its deep tone #A84A0A (same hue family, 5.8:1 with white). The light orange lives in the containers (#FFE2C6), the confetti and the dark-mode primary (#FFB37A).")
$md.Add("- **Secondary = the sunny yellow, with dark text.** #FDC440 itself is the secondary container: the selected chip, the PIN keys, the tonal buttons, always with dark brown text (#3B2800, 8.6:1). The ``secondary`` token is its dark ochre tone (#7A5700) for the few places Material draws secondary as text.")
$md.Add("- **Tertiary = the sky blue.** Container #D4EEF7 (the icon's sky), accent #0B6A86 (the platform's #48B7D3, darkened for text).")
$md.Add("- **Surfaces:** the page is a light sky tint (#F4FAFD); cards, the PIN sheet and dialogs are warm cream (#FFFCF6); the reward card stays white.")
$md.Add("- **Header gradient:** light sky blue #BEE5F4 to soft yellow #FFDD80 (to bottom-right) through a cream center #FFF6E0. A straight sky-to-yellow blend turns grey-green in the middle; the cream stop keeps it sunny. Dark mode: deep sky #0B5577, dark navy #1C2E3D, deep gold #6B4A0E, white text.")
$md.Add("- **Not brand colors:** the comb red stays out of the palette (red only means missed, overdue, off or a wrong PIN). The platform pink #DA558E is not used (it sits close to the purple range that CLAUDE.md 9 forbids).")
$md.Add("- **Warnings:** amber lies between the brand's orange and yellow, so warning words alone would look like an orange section title. A warning row in parent settings is now an amber box with a 2dp border and a warning triangle, dark text inside (``bg_warning_row``, ``ic_warning``). Status is never color alone: the words say what is wrong.")
$md.Add("- **Dynamic color stays off**: the palette is always ours, in light and dark mode.")
$md.Add("")
$md.Add("## Tokens")
$md.Add("")
$md.Add("| Token | Light | Dark | Role |")
$md.Add("|---|---|---|---|")
foreach ($k in $roles.Keys) { $md.Add("| ``$k`` | $($light[$k]) | $($dark[$k]) | $($roles[$k]) |") }
$md.Add("")
$md.Add("Tokens outside the palette: ``koko_icon_bg_top``, ``koko_icon_bg_bottom``, ``koko_sky``, ``splash_name_text`` (launcher icon and splash, item 1, unchanged), ``mascot_*`` (the capsule mascot, unchanged, see the leftovers below), ``shadow_soft`` (the card shadow tint: now the darkest sky blue #073A4C in light mode, black in dark mode as before).")
$md.Add("")
$md.Add("## Contrast of every pair")
$md.Add("")
$md.Add(("Text needs 4.5:1, icons, borders and the level bar 3:1 (WCAG). These are exactly the pairs ``ContrastTest`` asserts on the same tokens. Lowest text pair: light {0:N2}, dark {1:N2}." -f ($textRows | Measure-Object Light -Minimum).Minimum, ($textRows | Measure-Object Dark -Minimum).Minimum))
$md.Add("")
$md.Add("| Text or icon | On | Light | Dark | Needed |")
$md.Add("|---|---|---|---|---|")
foreach ($r in $all) { $md.Add(("| ``{0}`` | ``{1}`` | {2:N2} | {3:N2} | {4:N1} |" -f $r.Fg, $r.Bg, $r.Light, $r.Dark, $r.Need)) }
$md.Add("")
$md.Add("Pairs below the needed value: " + $(if ($failed.Count -eq 0) { "none." } else { ($failed | ForEach-Object { "$($_.Fg) on $($_.Bg)" }) -join ", " }))
$md.Add("")
$md.Add("## Leftover green, blue and teal")
$md.Add("")
$md.Add("Kept on purpose (also listed in ``docs/CHANGELOG.md``): ``success`` green #166534 / #86EFAC (a semantic color: a dose marked taken, always with the word), the capsule mascot ``mascot_top`` #F0FDF4, ``mascot_bottom`` #4ADE80, ``mascot_line`` #14532D, ``mascot_cheek`` #99F6E4, ``mascot_spark`` #14B8A6 (the empty state and the CHILD streak-ended screen, which this batch keeps because the no-streak avatar is BLOCKED). The sky blues (``tertiary``, ``points``, the gradient start, ``koko_*``) are new and come from the Koko pictures. No other green, blue or teal brand token is left.")
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines("$root\docs\palette-v2.md", $md, $utf8)

# ---------- Swatch sheet ----------
function Col([string]$hex) { $c = Rgb $hex; [System.Drawing.Color]::FromArgb((Alpha $hex), $c[0], $c[1], $c[2]) }
$keys = @($roles.Keys | Where-Object { $_ -notlike "gradient_scrim_*" })
$swW = 760; $swH = 64; $pad = 40; $top = 150
$gradH = 150
$W = $pad * 3 + $swW * 2; $H = $top + $keys.Count * ($swH + 8) + $gradH + 110
$bmp = New-Object System.Drawing.Bitmap $W, $H
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = "AntiAlias"; $g.TextRenderingHint = "AntiAliasGridFit"
$g.Clear([System.Drawing.Color]::White)
$fTitle = New-Object System.Drawing.Font("Segoe UI", 28, [System.Drawing.FontStyle]::Bold)
$fHead = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Bold)
$fName = New-Object System.Drawing.Font("Segoe UI", 13, [System.Drawing.FontStyle]::Bold)
$fSmall = New-Object System.Drawing.Font("Segoe UI", 11)
$black = [System.Drawing.Brushes]::Black
$g.DrawString("Koko Dawa palette v2 (generated from colors.xml)", $fTitle, $black, $pad, 30)
$g.DrawString("Light", $fHead, $black, $pad, 100); $g.DrawString("Dark", $fHead, $black, ($pad * 2 + $swW), 100)
# The "on" color drawn on each swatch: its pair partner, or the main text color.
function OnFor([string]$k, $t) {
    if ($t.Contains("on_$k")) { return "on_$k" }
    if ($k -like "on_*") { return $null }
    if ($k -in $surfaces -or $k -eq "background") { return "on_surface" }
    return $null
}
$i = 0
foreach ($k in $keys) {
    $y = $top + $i * ($swH + 8)
    $col = 0
    foreach ($t in @($light, $dark)) {
        $x = $pad + $col * ($swW + $pad)
        $hex = $t[$k]
        $under = if ((Alpha $hex) -lt 255) { $t["gradient_center"] } else { $null }
        $paint = if ($under) { Blend $hex $under } else { $hex }
        $g.FillRectangle((New-Object System.Drawing.SolidBrush (Col $paint)), $x, $y, 300, $swH)
        $g.DrawRectangle([System.Drawing.Pens]::Gainsboro, $x, $y, 300, $swH)
        $on = OnFor $k $t
        if ($on) {
            $g.DrawString(("Aa  {0:N1}:1" -f (Ratio $t[$on] $paint)), $fName, (New-Object System.Drawing.SolidBrush (Col $t[$on])), ($x + 12), ($y + 18))
        } elseif ($k -like "on_*" -or $k -in "error", "success", "points", "streak", "warning", "outline", "progress_on_gradient") {
            $bgk = if ($k -eq "on_gradient" -or $k -eq "progress_on_gradient") { "gradient_center" } elseif ($k -like "on_*") { $k.Substring(3) } else { "surface_lowest" }
            if (-not $t.Contains($bgk)) { $bgk = "surface" }
            $g.FillRectangle((New-Object System.Drawing.SolidBrush (Col $t[$bgk])), ($x + 150), ($y + 8), 140, ($swH - 16))
            $g.DrawString(("Aa {0:N1}" -f (Ratio $hex $t[$bgk])), $fName, (New-Object System.Drawing.SolidBrush (Col $hex)), ($x + 160), ($y + 18))
        }
        $g.DrawString($k, $fName, $black, ($x + 312), ($y + 6))
        $g.DrawString($hex, $fSmall, [System.Drawing.Brushes]::DimGray, ($x + 312), ($y + 32))
        $col++
    }
    $i++
}
# Gradients with their text at each stop.
$gy = $top + $keys.Count * ($swH + 8) + 30
$col = 0
foreach ($t in @($light, $dark)) {
    $x = $pad + $col * ($swW + $pad)
    $rect = New-Object System.Drawing.Rectangle $x, $gy, $swW, $gradH
    $br = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, (Col $t.gradient_start), (Col $t.gradient_end), 45.0
    $blend = New-Object System.Drawing.Drawing2D.ColorBlend 3
    $blend.Colors = @((Col $t.gradient_start), (Col $t.gradient_center), (Col $t.gradient_end)); $blend.Positions = @(0.0, 0.5, 1.0)
    $br.InterpolationColors = $blend
    $g.FillRectangle($br, $rect)
    $onb = New-Object System.Drawing.SolidBrush (Col $t.on_gradient)
    $g.DrawString("Header text on the gradient", $fHead, $onb, ($x + 20), ($gy + 14))
    $g.DrawString(("start {0:N1}   center {1:N1}   end {2:N1}" -f (Ratio $t.on_gradient $t.gradient_start), (Ratio $t.on_gradient $t.gradient_center), (Ratio $t.on_gradient $t.gradient_end)), $fName, $onb, ($x + 20), ($gy + 56))
    $glassRect = New-Object System.Drawing.Rectangle ($x + 20), ($gy + 92), 300, 44
    $g.FillRectangle((New-Object System.Drawing.SolidBrush (Col $t.glass)), $glassRect)
    $g.DrawString(("glass box {0:N1} / {1:N1}" -f (Ratio $t.on_gradient (Blend $t.glass $t.gradient_start)), (Ratio $t.on_gradient (Blend $t.glass $t.gradient_end))), $fName, $onb, ($x + 30), ($gy + 102))
    $g.FillRectangle((New-Object System.Drawing.SolidBrush (Col $t.progress_on_gradient)), ($x + 360), ($gy + 108), 220, 12)
    $col++
}
$g.DrawString(("Pairs checked: {0} text (4.5:1), {1} icon and border (3:1), {2} on glass; below the needed value: {3}. Same pairs as ContrastTest." -f $textRows.Count, $uiRows.Count, @($glassRows).Count, $failed.Count), $fSmall, $black, $pad, ($gy + $gradH + 40))
$bmp.Save("$root\docs\palette-v2.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
"palette sheet: {0} tokens, {1} pairs, {2} below the needed value -> docs\palette-v2.md, docs\palette-v2.png" -f $keys.Count, $all.Count, $failed.Count
