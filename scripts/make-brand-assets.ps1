# Makes the app's picture resources from the doctor's originals in design/brand/ (batch 6, item 0).
# Tooling only: crop, scale and place. Nothing is drawn or redrawn, no background is keyed, and the
# files with a baked-in checkerboard (rooster-nobg, avatar-no-streak, hospital-logo-nobg) are never used.
#   powershell -File scripts\make-brand-assets.ps1 [-Preview docs\evidence\batch6\item0]
# Writes into app/src/main/res/drawable-nodpi/:
#   ic_launcher_koko.png      432x432 (108dp at xxxhdpi), the adaptive icon foreground: the rooster from koko.jpg
#                             without the title, scaled so the rooster and its props fit the 66dp safe zone,
#                             the crop's outer 12 px faded into transparency over the background gradient
#   notification_large_koko.png  256x256, the same text-free square crop (the reminder's large icon)
#   hospital_logo.png         512x512, the hospital logo on its own white background (shown on a white tile)
#   avatar_celebration.jpg    512x512, the celebration creature with its own (blurred room) background, shown
#                             as a rounded picture
# Prints the sampled background gradient for @color/koko_icon_bg_top and @color/koko_icon_bg_bottom.
param([string]$Preview)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$root = (Resolve-Path "$PSScriptRoot\..").Path
$brand = Join-Path $root "design\brand"
$out = Join-Path $root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $out | Out-Null

function Crop-Scale([string]$src, [double]$x, [double]$y, [double]$size, [int]$px) {
    $img = [System.Drawing.Bitmap]::FromFile($src)
    $dst = New-Object System.Drawing.Bitmap $px, $px, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dst)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $attr = New-Object System.Drawing.Imaging.ImageAttributes
    $attr.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)   # no dark fringe at the crop border
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $px, $px), [single]$x, [single]$y, [single]$size, [single]$size, [System.Drawing.GraphicsUnit]::Pixel, $attr)
    $g.Dispose(); $img.Dispose()
    return $dst
}

function Save-Png($bmp, [string]$path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png) }
function Save-Jpg($bmp, [string]$path, [int]$quality) {
    $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
    $p = New-Object System.Drawing.Imaging.EncoderParameters 1
    $p.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]$quality)
    $bmp.Save($path, $codec, $p)
}

# --- The rooster (koko.jpg is 1024x559; the title's lowest pixels reach y 231, the rooster and its props
#     span x 374..652, y 280..507). The text-free square: top 238 (7 px below the title, so the scaling
#     filter cannot pick up a title pixel), bottom 558 (the last row), centered on the content horizontally.
$koko = Join-Path $brand "koko.jpg"
$side = 320.0; $sy = 238.0; $sx = 513.0 - $side / 2
$ccx = $sx + $side / 2; $ccy = $sy + $side / 2
# The content box's farthest corner from the crop center must fit the safe-zone radius 132 px (66dp at
# xxxhdpi) on the 432 px canvas.
$far = [math]::Sqrt([math]::Pow([math]::Max($ccx - 374, 652 - $ccx), 2) + [math]::Pow([math]::Max($ccy - 280, 507 - $ccy), 2))
$k = 132.0 / $far
$cropPx = [int][math]::Round($side * $k)            # about 232 px on the 432 px canvas
$tile = Crop-Scale $koko $sx $sy $side $cropPx

# The background layer: the picture's sky blue gets lighter towards the bottom, so the layer is a vertical
# gradient of the crop's own top and bottom rows (6 px each), extended to the full 432 px. Then the fade at
# the crop's border is invisible.
function Row-Avg($bmp, [int]$from, [int]$to) {
    $r = 0; $g = 0; $b = 0; $n = 0
    for ($j = $from; $j -lt $to; $j++) { for ($i = 0; $i -lt $bmp.Width; $i++) { $c = $bmp.GetPixel($i, $j); $r += $c.R; $g += $c.G; $b += $c.B; $n++ } }
    return @(($r / $n), ($g / $n), ($b / $n))
}
$top = Row-Avg $tile 0 6; $bottom = Row-Avg $tile ($cropPx - 6) $cropPx
$y0 = (432 - $cropPx) / 2 + 3; $y1 = (432 + $cropPx) / 2 - 3          # the rows where those averages sit
function At([double]$y) { $t = ($y - $y0) / ($y1 - $y0); return (0..2 | ForEach-Object { [math]::Max(0, [math]::Min(255, [int][math]::Round($top[$_] + ($bottom[$_] - $top[$_]) * $t))) }) }
$startRgb = At 0; $endRgb = At 432
$bgStart = [System.Drawing.Color]::FromArgb(255, $startRgb[0], $startRgb[1], $startRgb[2])
$bgEnd = [System.Drawing.Color]::FromArgb(255, $endRgb[0], $endRgb[1], $endRgb[2])
$bgHex = "#{0:X2}{1:X2}{2:X2} (top) to #{3:X2}{4:X2}{5:X2} (bottom)" -f $bgStart.R, $bgStart.G, $bgStart.B, $bgEnd.R, $bgEnd.G, $bgEnd.B

# Fade the crop's outer 12 px into transparency (alpha only; the rooster's pixels are untouched: the
# content keeps at least 15 px from the crop border).
$fade = 12
$fg = New-Object System.Drawing.Bitmap 432, 432, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$off = [int]((432 - $cropPx) / 2)
for ($i = 0; $i -lt $cropPx; $i++) { for ($j = 0; $j -lt $cropPx; $j++) {
    $d = [math]::Min([math]::Min($i, $j), [math]::Min($cropPx - 1 - $i, $cropPx - 1 - $j))
    $a = if ($d -ge $fade) { 255 } else { [int](255 * ($d + 0.5) / $fade) }
    $c = $tile.GetPixel($i, $j)
    $fg.SetPixel($i + $off, $j + $off, [System.Drawing.Color]::FromArgb($a, $c.R, $c.G, $c.B)) } }
Save-Png $fg (Join-Path $out "ic_launcher_koko.png")
$tile.Dispose()

$large = Crop-Scale $koko $sx $sy $side 256
Save-Png $large (Join-Path $out "notification_large_koko.png")

# --- The hospital logo (white background; the drawing spans x 172..852, y 186..794): a square with margin.
$logo = Crop-Scale (Join-Path $brand "hospital-logo.jpg") 132 110 760 512
Save-Png $logo (Join-Path $out "hospital_logo.png")

# --- The celebration creature (x 298..688, y 54..462 with wings, cape and shadow): a 490 px square.
$cele = Crop-Scale (Join-Path $brand "avatar-celebration.jpg") 250 20 490 512
Save-Jpg $cele (Join-Path $out "avatar_celebration.jpg") 90

"ic_launcher_koko.png (crop $([int]$side) px at $([math]::Round($sx,1)),$([math]::Round($sy,1)) -> $cropPx px on 432, fade $fade px)"
"background layer gradient: $bgHex"

# Previews: the icon on its background color in a circle and a squircle mask, on a light and a dark wallpaper.
if ($Preview) {
    New-Item -ItemType Directory -Force $Preview | Out-Null
    $sheet = New-Object System.Drawing.Bitmap 1000, 520
    $gs = [System.Drawing.Graphics]::FromImage($sheet)
    $gs.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $gs.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gs.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(245, 245, 240))), 0, 0, 500, 520)
    $gs.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(28, 28, 32))), 500, 0, 500, 520)
    # The visible 72dp of the 108dp layers (288 of 432 px), drawn at 216 px.
    $icon = New-Object System.Drawing.Bitmap 432, 432
    $gi = [System.Drawing.Graphics]::FromImage($icon)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Point 0, 0), (New-Object System.Drawing.Point 0, 432), $bgStart, $bgEnd
    $gi.FillRectangle($grad, 0, 0, 432, 432); $gi.DrawImage($fg, 0, 0, 432, 432); $gi.Dispose()
    foreach ($half in 0, 1) {
        foreach ($shape in 0, 1) {
            $x0 = 500 * $half + 30 + 240 * $shape; $y0 = 150
            $path = New-Object System.Drawing.Drawing2D.GraphicsPath
            if ($shape -eq 0) { $path.AddEllipse($x0, $y0, 216, 216) }
            else { $rr = 70; $path.AddArc($x0, $y0, $rr, $rr, 180, 90); $path.AddArc($x0 + 216 - $rr, $y0, $rr, $rr, 270, 90); $path.AddArc($x0 + 216 - $rr, $y0 + 216 - $rr, $rr, $rr, 0, 90); $path.AddArc($x0, $y0 + 216 - $rr, $rr, $rr, 90, 90); $path.CloseFigure() }
            $gs.SetClip($path)
            $gs.DrawImage($icon, (New-Object System.Drawing.Rectangle $x0, $y0, 216, 216), 72, 72, 288, 288, [System.Drawing.GraphicsUnit]::Pixel)
            $gs.ResetClip()
        }
    }
    $gs.Dispose(); $icon.Dispose()
    $sheet.Save((Join-Path $Preview "icon_masks_preview.png"), [System.Drawing.Imaging.ImageFormat]::Png); $sheet.Dispose()
    "preview: $(Join-Path $Preview 'icon_masks_preview.png')"
}
$fg.Dispose(); $large.Dispose(); $logo.Dispose(); $cele.Dispose()
