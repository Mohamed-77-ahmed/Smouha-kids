# Puts screenshots side by side in one overview image (each scaled to the same height, with its name).
# Usage: powershell -File docs\evidence\tools\montage.ps1 -Out overview.png -Height 640 -Columns 6 a.png b.png ...
param([string]$Out, [int]$Height = 640, [int]$Columns = 6, [Parameter(ValueFromRemainingArguments = $true)][string[]]$Files)
Add-Type -AssemblyName System.Drawing
$label = 28
$images = $Files | ForEach-Object { [System.Drawing.Bitmap]::FromFile((Resolve-Path $_).Path) }
$widths = $images | ForEach-Object { [int]($_.Width * $Height / $_.Height) }
$cellW = ($widths | Measure-Object -Maximum).Maximum + 12
$rows = [Math]::Ceiling($images.Count / $Columns)
$sheet = New-Object System.Drawing.Bitmap ([int]($cellW * [Math]::Min($Columns, $images.Count))), ([int](($Height + $label + 12) * $rows))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(40, 40, 40))
$g.InterpolationMode = 'HighQualityBicubic'
$font = New-Object System.Drawing.Font 'Arial', 13
for ($i = 0; $i -lt $images.Count; $i++) {
    $x = ($i % $Columns) * $cellW + 6
    $y = [Math]::Floor($i / $Columns) * ($Height + $label + 12) + 6
    $g.DrawString([IO.Path]::GetFileNameWithoutExtension($Files[$i]), $font, [System.Drawing.Brushes]::White, $x, $y)
    $g.DrawImage($images[$i], $x, $y + $label, $widths[$i], $Height)
}
$g.Dispose()
$sheet.Save($Out)
$images | ForEach-Object { $_.Dispose() }
$sheet.Dispose()
$Out
