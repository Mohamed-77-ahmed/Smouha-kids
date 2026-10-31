# Renders the app name (app_name in values/ and values-ar/strings.xml) as text in the color token
# splash_name_text (values/colors.xml) on a transparent background, for the splash screen's branding
# image (Android 12+ shows only an icon and an image there, no text). Plain text only: this is not a
# logo. Run again after a name change or a change of that color.
# Output: app/src/main/res/drawable-nodpi/splash_name.png and drawable-ar-nodpi/splash_name.png
# (800 x 320 px = the 200 x 80 dp branding box at 4x).
param([string]$Font = "Segoe UI")
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
$res = "app\src\main\res"

function Get-AppName([string]$file) {
    $xml = [xml](Get-Content -Raw -Encoding UTF8 $file)
    $node = $xml.resources.string | Where-Object { $_.GetAttribute("name") -eq "app_name" } | Select-Object -First 1
    return ($node.InnerText -replace "\\'", "'")
}

$colors = [xml](Get-Content -Raw -Encoding UTF8 "$res\values\colors.xml")
$hex = ($colors.resources.color | Where-Object { $_.GetAttribute("name") -eq "splash_name_text" } | Select-Object -First 1).InnerText.Trim()
$ink = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($hex))

function Render([string]$text, [bool]$rtl, [string]$out) {
    $w = 800; $h = 320
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    if ($rtl) { $format.FormatFlags = [System.Drawing.StringFormatFlags]::DirectionRightToLeft }
    $box = New-Object System.Drawing.RectangleF 0, 0, $w, $h
    # Largest size whose text fits the box in at most two lines.
    $size = 96
    do {
        $f = New-Object System.Drawing.Font $Font, $size, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
        $chars = 0; $lines = 0
        $measured = $g.MeasureString($text, $f, (New-Object System.Drawing.SizeF $w, $h), $format, [ref]$chars, [ref]$lines)
        $fits = $chars -eq $text.Length -and $lines -le 2 -and $measured.Height -le $h
        if (-not $fits) { $f.Dispose(); $size -= 4 }
    } until ($fits -or $size -lt 24)
    $g.DrawString($text, $f, $ink, $box, $format)
    $g.Dispose()
    New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    "$out : '$text' at ${size}px, $lines line(s), color $hex"
}

Render (Get-AppName "$res\values\strings.xml") $false "$res\drawable-nodpi\splash_name.png"
Render (Get-AppName "$res\values-ar\strings.xml") $true "$res\drawable-ar-nodpi\splash_name.png"
