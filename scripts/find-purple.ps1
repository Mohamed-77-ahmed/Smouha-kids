# Lists every purple left in the project (CLAUDE.md 9): hex colors in the purple hue range and
# names with purple, violet, indigo, lavender, magenta, fuchsia, plum or mauve, in resources,
# Kotlin, the manifest and Gradle files. Build folders are skipped.
# Usage: powershell -File scripts\find-purple.ps1 [-Root .]
param([string]$Root = ".")

function Get-Hsv([string]$hex) {
    $h = $hex.TrimStart('#')
    if ($h.Length -eq 3) { $h = ($h.ToCharArray() | ForEach-Object { "$_$_" }) -join '' }
    if ($h.Length -eq 4) { $h = ($h.Substring(1).ToCharArray() | ForEach-Object { "$_$_" }) -join '' }
    if ($h.Length -eq 8) { $h = $h.Substring(2) }
    if ($h.Length -ne 6) { return $null }
    $r = [Convert]::ToInt32($h.Substring(0, 2), 16) / 255.0
    $g = [Convert]::ToInt32($h.Substring(2, 2), 16) / 255.0
    $b = [Convert]::ToInt32($h.Substring(4, 2), 16) / 255.0
    $max = [Math]::Max($r, [Math]::Max($g, $b)); $min = [Math]::Min($r, [Math]::Min($g, $b)); $d = $max - $min
    $hue = 0.0
    if ($d -gt 0) {
        if ($max -eq $r) { $hue = 60 * ((($g - $b) / $d) % 6) }
        elseif ($max -eq $g) { $hue = 60 * ((($b - $r) / $d) + 2) }
        else { $hue = 60 * ((($r - $g) / $d) + 4) }
    }
    if ($hue -lt 0) { $hue += 360 }
    $sat = if ($max -eq 0) { 0 } else { $d / $max }
    [pscustomobject]@{ Hue = [Math]::Round($hue); Sat = [Math]::Round($sat, 2); Val = [Math]::Round($max, 2) }
}

$files = Get-ChildItem $Root -Recurse -File -Include *.xml, *.kt, *.kts, *.toml, *.json, *.properties |
    Where-Object { $_.FullName -notmatch '\\(build|\.gradle|\.idea|\.kotlin|tracker-snapshots)\\' }
$hits = @()
foreach ($f in $files) {
    $n = 0
    foreach ($line in [IO.File]::ReadAllLines($f.FullName)) {
        $n++
        # "#RRGGBB" style in resources, "0xAARRGGBB" color ints in Kotlin.
        foreach ($m in [regex]::Matches($line, '#(?:[0-9A-Fa-f]{8}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{3,4})\b|0x[0-9A-Fa-f]{8}\b')) {
            $c = Get-Hsv ($m.Value -replace '^0x', '#')
            # Purple, violet and indigo: hue 235 to 330 with visible saturation. The app's blues stay
            # below (blue-500 217, blue-600 221, blue-800 226); greys and near-whites are not purple.
            if ($c -and $c.Hue -ge 235 -and $c.Hue -le 330 -and $c.Sat -ge 0.15 -and $c.Val -ge 0.15) {
                $hits += "{0}:{1}  {2}  (hue {3}, sat {4})  {5}" -f $f.FullName.Replace((Resolve-Path $Root).Path + "\", ""), $n, $m.Value, $c.Hue, $c.Sat, $line.Trim()
            }
        }
        if ($line -match '(?i)purple|violet|indigo|lavender|magenta|fuchsia|\bplum\b|mauve') {
            $hits += "{0}:{1}  name  {2}" -f $f.FullName.Replace((Resolve-Path $Root).Path + "\", ""), $n, $line.Trim()
        }
    }
}
if ($hits.Count -eq 0) { "No purple found in $($files.Count) files." } else { $hits }
