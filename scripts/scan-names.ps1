# Before every push (docs/GITHUB.md): lists every text file the repository would contain and reports how
# many times a name used as real test data appears in each (counts only: never the matched text), and lists the pictures that would be
# published so a person can look at them. The names (with spelling variants and the Arabic forms) are in
# scripts\scan-names.txt, one per line; that file is ignored by git, so the list itself is never published.
#   powershell -File scripts\scan-names.ps1 [-Report <file.txt>]
# With git installed and the repository created, the file list comes from git itself (tracked plus
# not ignored); before that, from the same rules as .gitignore. Exit code 1 when a name is found.
param([string]$Report)
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $root
$listFile = Join-Path $PSScriptRoot "scan-names.txt"
if (-not (Test-Path $listFile)) { throw "scan-names: write the real test names into scripts\scan-names.txt (one per line, ignored by git)" }
$names = @(Get-Content $listFile -Encoding UTF8 | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") })
$pattern = "(?i)(" + ($names -join "|") + ")"
$textExt = @(".kt", ".kts", ".java", ".xml", ".md", ".txt", ".ps1", ".cmd", ".bat", ".sh", ".json", ".properties", ".toml", ".pro", ".html", ".css", ".svg", ".gitignore", ".csv", "")
$imageExt = @(".png", ".jpg", ".jpeg", ".webp", ".gif")

$files = $null
$git = Get-Command git -ErrorAction SilentlyContinue
if ($git -and (Test-Path (Join-Path $root ".git"))) {
    $files = @(git -c core.quotepath=off ls-files --cached --others --exclude-standard) | ForEach-Object { $_ -replace '/', '\' }
    $how = "git ls-files (tracked and not ignored)"
} else {
    # The rules of .gitignore (git is not installed or the repository does not exist yet); keep in step with it.
    $skipDir = '(^|\\)(\.git|build|\.gradle|\.kotlin|captures|\.externalNativeBuild|\.cxx)(\\|$)|^\.idea\\(caches|libraries)(\\|$)|^docs\\evidence\\(?!tools\\)'
    $skipFile = '(^|\\)(local\.properties|keystore\.properties|\.DS_Store|window_dump\.xml)$|\.(jks|keystore|iml|hprof|logcat)$|(^|\\)bugreport[^\\]*\.zip$|^\.idea\\(modules|workspace|navEditor|assetWizardSettings|deploymentTargetSelector)\.xml$|^dist\\link\.txt$|^scripts\\scan-names\.txt$|^docs\\final-plan\.md$|(^|\\)kh\.env($|\\)|\.env$|^dist\\|^[^\\]+\.(jpeg|jpg|png|mp4|mpeg|mp3)$'
    $files = Get-ChildItem -Recurse -File -Force | ForEach-Object { $_.FullName.Substring($root.Length + 1) } | Where-Object { $_ -notmatch $skipDir -and $_ -notmatch $skipFile }
    $how = "the .gitignore rules (no git repository yet)"
}

$out = New-Object System.Collections.Generic.List[string]
$out.Add("scan-names " + (Get-Date -Format "yyyy-MM-dd HH:mm") + ": $(@($files).Count) files, listed with $how")
$out.Add("Names searched (any case): $($names.Count) from scripts\scan-names.txt (not printed)")
$hits = 0
foreach ($f in $files) {
    $ext = [IO.Path]::GetExtension($f).ToLower()
    if ($textExt -notcontains $ext) { continue }
    $lines = @(Get-Content -LiteralPath $f -Encoding UTF8 -ErrorAction SilentlyContinue)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $c = [regex]::Matches($lines[$i], $pattern).Count
        if ($c) { $out.Add("  HIT $f`:$($i + 1) ($c)"); $hits += $c }
    }
}
$out.Add("Name hits: $hits")
$images = @($files | Where-Object { $imageExt -contains [IO.Path]::GetExtension($_).ToLower() })
$out.Add("Pictures that would be published ($($images.Count)); look at them before the push:")
$images | Group-Object { Split-Path $_ -Parent } | ForEach-Object { $out.Add("  $($_.Name): $($_.Count)") }
if ($Report) { [IO.File]::WriteAllLines($Report, [string[]]$out, (New-Object Text.UTF8Encoding $false)) }
$out
if ($hits -gt 0) { exit 1 }
