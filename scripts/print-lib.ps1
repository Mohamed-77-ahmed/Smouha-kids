# Shared helpers for scripts\make-docs.ps1 and scripts\make-card.ps1 (dot-source this file).
# Print-Html: an HTML page (right-to-left Arabic or English) to PDF with headless Microsoft Edge (or Chrome),
#   which shapes Arabic correctly; no header or footer, backgrounds kept (print.css).
# Render-Pages: every page of that PDF to PNG with Windows' own PDF renderer (scripts\pdf-to-png.ps1),
#   so the check looks at the PDF itself, not at the HTML.
$printLib = @{ Root = (Resolve-Path "$PSScriptRoot\..").Path }
$printLib.Browser = @(
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $printLib.Browser) { throw "print-lib: neither Edge nor Chrome was found" }
$printLib.Work = Join-Path $env:TEMP "make-docs"
New-Item -ItemType Directory -Force $printLib.Work | Out-Null

function To-FileUrl([string]$path) { "file:///" + ($path -replace '\\', '/') }

# Rewrites the relative links of a page in docs\print\ (print.css, shots/..., img/...) to absolute file URLs, so the
# page can be printed from the temp folder.
function Resolve-Links([string]$html) {
    $print = Join-Path $printLib.Root "docs\print"
    $html = $html.Replace('href="print.css"', 'href="' + (To-FileUrl (Join-Path $print "print.css")) + '"')
    $html = $html.Replace('src="shots/', 'src="' + (To-FileUrl (Join-Path $print "shots")) + '/')
    $html = $html.Replace('src="img/', 'src="' + (To-FileUrl (Join-Path $print "img")) + '/')
    return $html
}

# Prints $html (a string) to $pdf. The PDF is made in an ASCII temp path first (the browser and the
# PDF renderer get no Arabic path), then copied to $pdf. Returns the temp PDF path.
function Print-Html([string]$name, [string]$html, [string]$pdf) {
    $page = Join-Path $printLib.Work "$name.html"
    $tmpPdf = Join-Path $printLib.Work "$name.pdf"
    [IO.File]::WriteAllText($page, $html, (New-Object Text.UTF8Encoding $false))
    Remove-Item $tmpPdf -ErrorAction SilentlyContinue
    $args = @("--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
        "--user-data-dir=`"$(Join-Path $printLib.Work 'browser-profile')`"", "--no-pdf-header-footer",
        "--print-to-pdf=`"$tmpPdf`"", "`"$(To-FileUrl $page)`"")
    $p = Start-Process -FilePath $printLib.Browser -ArgumentList $args -PassThru -WindowStyle Hidden
    $null = $p.Handle
    if (-not $p.WaitForExit(120000)) { $p.Kill(); throw "print-lib: the browser did not finish $name" }
    if (-not (Test-Path $tmpPdf)) { throw "print-lib: no PDF for $name" }
    if ($pdf) {
        New-Item -ItemType Directory -Force (Split-Path $pdf -Parent) | Out-Null
        Copy-Item -LiteralPath $tmpPdf -Destination $pdf -Force
    }
    return $tmpPdf
}

# Renders the pages of $pdf to $outDir\<name>_p<N>.png and returns the page count.
function Render-Pages([string]$pdf, [string]$outDir, [int]$width = 1240) {
    New-Item -ItemType Directory -Force $outDir | Out-Null
    # A separate process with a time limit: the Windows PDF renderer once kept its process alive.
    $log = Join-Path $printLib.Work "render.log"
    $p = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile -MTA -ExecutionPolicy Bypass -File `"$(Join-Path $printLib.Root 'scripts\pdf-to-png.ps1')`" -Pdf `"$pdf`" -Out `"$outDir`" -Width $width" -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $null = $p.Handle
    if (-not $p.WaitForExit(120000)) { $p.Kill(); throw "print-lib: rendering $pdf took more than 2 minutes" }
    $r = Get-Content $log -Raw
    return [int]([regex]::Match("$r", '(\d+) page').Groups[1].Value)
}
