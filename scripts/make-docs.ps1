# Builds the PDFs for parents and the hospital from the right-to-left HTML in docs\print\ with headless
# Edge, renders EVERY page back to PNG for checking, and copies the PDFs into dist\Koko-Dawa-App\.
#   powershell -File scripts\make-docs.ps1 [-Pages docs\evidence\batch7\item2\pages]
# Install guide: each screen from a REAL phone is taken from design\guide-shots\ (docs\guide-shots-needed.md);
# a missing one becomes a dashed placeholder box with its file name (never a drawn screen).
# design\guide-shots\install-video.mp4, when present, goes to the parents folder as 4_فيديو_التثبيت.mp4.
# Checks the page counts (install guide 2 to 3, user guide 4 to 5, each handoff 2); exit code 1 if one is off.
# Look at the PNG pages before sharing: letters joined, right to left, digits, nothing cut off.
param([string]$Pages = "docs\evidence\batch7\item2\pages")
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
. "$PSScriptRoot\print-lib.ps1"
$root = $printLib.Root
$print = Join-Path $root "docs\print"
$realShots = Join-Path $root "design\guide-shots"
$app = Join-Path $root "dist\Koko-Dawa-App"
$parents = Join-Path $app "للأهل"
$hospital = Join-Path $app "للمستشفى"
if (-not [IO.Path]::IsPathRooted($Pages)) { $Pages = Join-Path $root $Pages }

$docs = @(
    @{ Name = "install-guide-ar"; Pdf = (Join-Path $parents "2_طريقة_التثبيت.pdf"); Min = 2; Max = 3 },
    @{ Name = "user-guide-ar"; Pdf = (Join-Path $parents "3_طريقة_الاستخدام.pdf"); Min = 4; Max = 5 },
    @{ Name = "handoff-ar"; Pdf = (Join-Path $hospital "وثيقة_التسليم_عربي.pdf"); Min = 2; Max = 2 },
    @{ Name = "handoff-en"; Pdf = (Join-Path $hospital "Handoff-EN.pdf"); Min = 2; Max = 2 }
)

$missing = New-Object System.Collections.Generic.List[string]
$present = New-Object System.Collections.Generic.List[string]
$bad = 0
$report = @("make-docs " + (Get-Date -Format "yyyy-MM-dd HH:mm") + " with $(Split-Path $printLib.Browser -Leaf)")
foreach ($d in $docs) {
    $html = [IO.File]::ReadAllText((Join-Path $print "$($d.Name).html"), [Text.Encoding]::UTF8)
    # Real-phone screens: the file when it exists, otherwise a clearly marked placeholder box.
    $html = [regex]::Replace($html, '<div class="shot" data-real="([^"]+)" data-what="([^"]*)"></div>', {
        param($m)
        $file = $m.Groups[1].Value
        $found = @("png", "jpg", "jpeg") | ForEach-Object { Join-Path $realShots "$file.$_" } | Where-Object { Test-Path $_ } | Select-Object -First 1
        if ($found) {
            $present.Add($file)
            '<div class="shot"><img src="' + (To-FileUrl $found) + '" alt=""></div>'
        } else {
            $missing.Add($file)
            '<div class="shot"><div class="placeholder"><b>صورة ناقصة من موبايل حقيقي</b><span>' + $m.Groups[2].Value + '</span><code>design/guide-shots/<br>' + $file + '.png</code></div></div>'
        }
    })
    $tmpPdf = Print-Html $d.Name (Resolve-Links $html) $d.Pdf
    Get-ChildItem $Pages -Filter "$($d.Name)_p*.png" -ErrorAction SilentlyContinue | Remove-Item -Force
    $n = Render-Pages $tmpPdf $Pages
    $ok = $n -ge $d.Min -and $n -le $d.Max
    if (-not $ok) { $bad++ }
    $report += ("  {0}: {1} page(s) (expected {2} to {3}) {4} -> {5}" -f $d.Name, $n, $d.Min, $d.Max, $(if ($ok) { "OK" } else { "WRONG PAGE COUNT" }), $d.Pdf.Substring($root.Length + 1))
}
$report += "  real-phone screens present: " + $(if ($present.Count) { ($present | Sort-Object -Unique) -join ", " } else { "none" })
$report += "  real-phone screens missing (placeholder boxes): " + $(if ($missing.Count) { ($missing | Sort-Object -Unique) -join ", " } else { "none" })

$video = Join-Path $realShots "install-video.mp4"
if (Test-Path $video) {
    Copy-Item -LiteralPath $video -Destination (Join-Path $parents "4_فيديو_التثبيت.mp4") -Force
    $report += "  install video copied to the parents folder"
} else {
    $report += "  install video: not there (design\guide-shots\install-video.mp4), nothing copied"
}
$report += "  pages rendered to $($Pages.Substring($root.Length + 1)) (look at every one)"
$report
if ($bad -gt 0) { exit 1 }
