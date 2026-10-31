# Renders every page of a PDF to PNG with the PDF renderer built into Windows (Windows.Data.Pdf),
# so the PDF itself is checked, not the HTML it came from (letters joined, right to left, digits,
# nothing cut off). No extra tools.
#   powershell -File scripts\pdf-to-png.ps1 -Pdf <file.pdf> -Out <folder> [-Width 1240]
# Writes <Out>\<pdf name>_p1.png, _p2.png, ... and prints the page count.
param([Parameter(Mandatory = $true)][string]$Pdf, [Parameter(Mandatory = $true)][string]$Out, [int]$Width = 1240)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.StorageFolder, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Data.Pdf.PdfDocument, Windows.Data.Pdf, ContentType = WindowsRuntime] | Out-Null

$ext = [System.WindowsRuntimeSystemExtensions].GetMethods()
$asTaskOf = $ext | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
$asTaskAction = $ext | Where-Object { $_.Name -eq 'AsTask' -and -not $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncAction' } | Select-Object -First 1
function Await($op, [type]$type) { $t = $asTaskOf.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }
function AwaitAction($op) { $t = $asTaskAction.Invoke($null, @($op)); $t.Wait(-1) | Out-Null }

$pdfPath = (Resolve-Path -LiteralPath $Pdf).Path
New-Item -ItemType Directory -Force $Out | Out-Null
$outPath = (Resolve-Path -LiteralPath $Out).Path
$file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($pdfPath)) ([Windows.Storage.StorageFile])
$doc = Await ([Windows.Data.Pdf.PdfDocument]::LoadFromFileAsync($file)) ([Windows.Data.Pdf.PdfDocument])
$folder = Await ([Windows.Storage.StorageFolder]::GetFolderFromPathAsync($outPath)) ([Windows.Storage.StorageFolder])
$base = [IO.Path]::GetFileNameWithoutExtension($pdfPath)
for ($i = 0; $i -lt $doc.PageCount; $i++) {
    $page = $doc.GetPage($i)
    $png = Await ($folder.CreateFileAsync("${base}_p$($i + 1).png", [Windows.Storage.CreationCollisionOption]::ReplaceExisting)) ([Windows.Storage.StorageFile])
    $stream = Await ($png.OpenAsync([Windows.Storage.FileAccessMode]::ReadWrite)) ([Windows.Storage.Streams.IRandomAccessStream])
    $opts = New-Object Windows.Data.Pdf.PdfPageRenderOptions
    $opts.DestinationWidth = [uint32]$Width
    AwaitAction ($page.RenderToStreamAsync($stream, $opts))
    $stream.Dispose()
    $page.Dispose()
}
[Console]::Out.WriteLine("$($doc.PageCount) page(s): $base -> $outPath")
[Console]::Out.Flush()
# The Windows PDF objects can keep PowerShell from ending after the last page (seen: the pages were
# written, the process never exited, even with [Environment]::Exit). Everything is written and
# flushed at this point, so the process ends itself at once.
[System.Diagnostics.Process]::GetCurrentProcess().Kill()
