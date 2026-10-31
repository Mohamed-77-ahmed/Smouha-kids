# Makes the composite pictures for the NotebookLM presentation (batch 5 / batch 7): three phone screenshots
# side by side, each with rounded corners and a soft shadow, on a light background. No text and no brand art.
# Tooling only (System.Drawing in Windows PowerShell), never part of the app. docs/DECISIONS.md D43.
#   powershell -File scripts\make-composites.ps1
# Reads notebooklm\screenshots\*.png, writes notebooklm\screenshots\composites\*.png (2400x1500).
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$root = (Resolve-Path "$PSScriptRoot\..").Path
$shots = Join-Path $root "notebooklm\screenshots"
$out = Join-Path $shots "composites"
New-Item -ItemType Directory -Force $out | Out-Null

# name = the three screenshots, read from right to left like the Arabic slides (the first one stands right).
$sets = [ordered]@{
    "c04_family_starts" = @("04_01_language_picker", "04_02_pin_sheet", "04_04_add_medicine_days_times")
    "c05_reminder_to_confirm" = @("05_01_notification_shade", "05_02_confirm_screen", "07_01_reward_child")
    "c07_rewards_by_age" = @("07_01_reward_child", "07_02_reward_teen", "07_03_reward_adult")
    "c07_streak_and_course_end" = @("07_04_streak_broken_child", "07_05_course_end", "08_01_home_several_children")
    "c09_any_screen" = @("09_01_home_small_phone", "08_01_home_several_children", "09_02_home_english")
    "c10_privacy_and_settings" = @("10_01_about", "10_02_settings_sound_rows", "06_01_pin_gate")
}

function Rounded([System.Drawing.RectangleF]$r, [single]$rad) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $rad * 2
    $p.AddArc($r.X, $r.Y, $d, $d, 180, 90); $p.AddArc($r.Right - $d, $r.Y, $d, $d, 270, 90)
    $p.AddArc($r.Right - $d, $r.Bottom - $d, $d, $d, 0, 90); $p.AddArc($r.X, $r.Bottom - $d, $d, $d, 90, 90)
    $p.CloseFigure(); return $p
}

$W = 2400; $H = 1500; $phoneH = 1300; $gap = 110
foreach ($name in $sets.Keys) {
    $files = $sets[$name] | ForEach-Object { Join-Path $shots "$_.png" }
    $missing = @($files | Where-Object { -not (Test-Path $_) })
    if ($missing.Count) { Write-Warning "$name skipped, missing: $($missing -join ', ')"; continue }
    $bmp = New-Object System.Drawing.Bitmap $W, $H
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = "AntiAlias"; $g.InterpolationMode = "HighQualityBicubic"; $g.PixelOffsetMode = "HighQuality"
    # The palette's light sky to cream (#F4FAFD to #FFF6E0), top to bottom.
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Point 0, 0), (New-Object System.Drawing.Point 0, $H), ([System.Drawing.Color]::FromArgb(255, 0xF4, 0xFA, 0xFD)), ([System.Drawing.Color]::FromArgb(255, 0xFF, 0xF6, 0xE0))
    $g.FillRectangle($bg, 0, 0, $W, $H)
    $imgs = $files | ForEach-Object { [System.Drawing.Image]::FromFile($_) }
    $widths = $imgs | ForEach-Object { [single]($phoneH * $_.Width / $_.Height) }
    $total = ($widths | Measure-Object -Sum).Sum + $gap * 2
    $x = [single](($W + $total) / 2)   # right to left
    for ($i = 0; $i -lt 3; $i++) {
        $pw = $widths[$i]; $x -= $pw
        $r = New-Object System.Drawing.RectangleF $x, ([single](($H - $phoneH) / 2)), $pw, $phoneH
        foreach ($s in 1..6) {   # soft shadow
            $sr = New-Object System.Drawing.RectangleF ($r.X - $s * 2), ($r.Y + 6 - $s), ($r.Width + $s * 4), ($r.Height + $s * 4)
            $sp = Rounded $sr (46 + $s * 2)
            $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(9, 30, 42, 68))), $sp)
        }
        $path = Rounded $r 46
        $g.SetClip($path); $g.DrawImage($imgs[$i], $r); $g.ResetClip()
        $g.DrawPath((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0xD9, 0xE2, 0xE8)), 3), $path)
        $x -= $gap
    }
    $imgs | ForEach-Object { $_.Dispose() }
    $file = Join-Path $out "$name.png"
    $bmp.Save($file, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()
    "$name.png <- $($sets[$name] -join ', ')"
}
