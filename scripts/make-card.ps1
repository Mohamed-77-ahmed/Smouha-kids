# Makes the printable A5 quick-start card (one page, a QR code, 4 steps) from docs\print\card-ar.html.
#   powershell -File scripts\make-card.ps1                     # link from dist\link.txt, card into dist\
#   powershell -File scripts\make-card.ps1 -Link <url> -Out <file.pdf> [-Pages <folder>]   # a test
# dist\link.txt (written by hand after the Drive upload, dist\DRIVE.md) holds one link per line:
#   DIRECT_LINK=https://drive.google.com/uc?export=download&id=FILE_ID
#   FOLDER_LINK=https://drive.google.com/drive/folders/FOLDER_ID
# (a single bare https line counts as the direct link). The QR code carries the DIRECT link, so a scan
# starts the download of the app file; the folder link (with the guides) goes into the WhatsApp message.
# Without a link the card is made as a DRAFT with a dashed box instead of the QR code.
# The QR code is drawn by ZXing (scripts\qr, a tool build, never part of the app; docs\DECISIONS.md D35).
# Check: the PDF page is rendered to PNG by Windows' PDF renderer and the QR code is read back from that
# picture with ZXing; the text must equal the link exactly. Exit code 1 if it does not.
param([string]$Link, [string]$Out, [string]$Pages = "docs\evidence\batch7\item2\pages")
$ErrorActionPreference = "Continue"   # native tools may write warnings to stderr; every step is checked
[Console]::OutputEncoding = [Text.Encoding]::UTF8
. "$PSScriptRoot\print-lib.ps1"
$root = $printLib.Root
if (-not $Out) { $Out = Join-Path $root "dist\Koko-Dawa-App\للطباعة\كارت_التثبيت_A5.pdf" }
if (-not [IO.Path]::IsPathRooted($Pages)) { $Pages = Join-Path $root $Pages }
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$qrtool = Join-Path $root "scripts\qr\build\install\qrtool\bin\qrtool.bat"

function Build-QrTool {
    # Gradle as a process with a log file (a daemon would keep a pipe open, see build-release.ps1).
    if (-not $env:ANDROID_SERIAL) { $env:ANDROID_SERIAL = "emulator-5560" }   # CLAUDE.md 0b; installs nothing
    $log = Join-Path $env:TEMP "qrtool-build.log"
    $p = Start-Process -FilePath (Join-Path $root "gradlew.bat") -ArgumentList "-p `"$(Join-Path $root 'scripts\qr')`" installDist --console=plain -q" -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $null = $p.Handle
    $p.WaitForExit()
    if ($p.ExitCode -ne 0 -or -not (Test-Path $qrtool)) { throw "make-card: could not build the QR tool (see $log)" }
}

# The link
$source = "the -Link parameter"
if (-not $Link) {
    $linkFile = Join-Path $root "dist\link.txt"
    $source = "dist\link.txt"
    if (Test-Path $linkFile) {
        $lines = Get-Content $linkFile -Encoding UTF8 | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") }
        $direct = $lines | Where-Object { $_ -match '^DIRECT_LINK\s*=\s*(\S+)$' } | ForEach-Object { $Matches[1] } | Select-Object -First 1
        $bare = $lines | Where-Object { $_ -match '^https://\S+$' } | Select-Object -First 1
        $Link = if ($direct) { $direct } else { $bare }
    }
}
if ($Link -and $Link -notmatch '^https://\S+$') { throw "make-card: the link must be one https address without spaces ($Link)" }

$html = [IO.File]::ReadAllText((Join-Path $root "docs\print\card-ar.html"), [Text.Encoding]::UTF8)
$report = @("make-card " + (Get-Date -Format "yyyy-MM-dd HH:mm"))
if ($Link) {
    if (-not (Test-Path $qrtool)) { Build-QrTool }
    $txt = Join-Path $printLib.Work "card-link.txt"
    $svg = Join-Path $printLib.Work "card-qr.svg"
    [IO.File]::WriteAllText($txt, $Link, (New-Object Text.UTF8Encoding $false))
    & $qrtool encode $txt $svg 2>&1 | Out-Null
    if (-not (Test-Path $svg)) { throw "make-card: the QR tool made no SVG" }
    $html = $html.Replace("{{QR}}", [IO.File]::ReadAllText($svg)).Replace("{{DRAFT}}", "")
    $report += "  link from $source, QR code of the direct link"
} else {
    $box = '<div class="placeholder"><b>الكود هيتطبع هنا</b><span>بعد رفع الملف على Drive وكتابة الرابط في</span><code>dist/link.txt</code></div>'
    $html = $html.Replace("{{QR}}", $box).Replace("{{DRAFT}}", '<div class="draft">مسودة: من غير رابط لسه</div>')
    $report += "  no link in ${source}: DRAFT card with a placeholder box, no QR code"
}

$tmpPdf = Print-Html "card-ar" (Resolve-Links $html) $Out
Get-ChildItem $Pages -Filter "card-ar_p*.png" -ErrorAction SilentlyContinue | Remove-Item -Force
$n = Render-Pages $tmpPdf $Pages 1748      # A5 width at about 300 dpi
$report += "  pages: $n (expected 1) -> $Out"
$ok = $n -eq 1
if ($Link) {
    $png = Join-Path $Pages "card-ar_p1.png"
    $back = Join-Path $printLib.Work "card-decoded.txt"
    Remove-Item $back -ErrorAction SilentlyContinue
    & $qrtool decode $png $back 2>&1 | Out-Null
    $decoded = if (Test-Path $back) { [IO.File]::ReadAllText($back, [Text.Encoding]::UTF8) } else { "" }
    $same = $decoded -ceq $Link
    $report += "  QR read back from the rendered PDF page (ZXing): " + $(if ($same) { "VERIFIED, equals the link" } elseif ($decoded) { "MISMATCH: $decoded" } else { "NOT FOUND" })
    $ok = $ok -and $same
} else {
    $report += "  QR: UNVERIFIED (no link yet); run again after writing dist\link.txt"
}
$report
if (-not $ok) { exit 1 }
