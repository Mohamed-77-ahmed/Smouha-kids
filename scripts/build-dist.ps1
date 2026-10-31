# Puts dist\Koko-Dawa-App\ together exactly as it goes onto Google Drive (dist\DRIVE.md), and checks it.
#   powershell -File scripts\build-dist.ps1 [-Rebuild]
#   -Rebuild: first builds and checks the release APK again (scripts\build-release.ps1, real key).
# Koko-Dawa-App\
#   للأهل\       1_التطبيق\Koko-Dawa-<v>.apk, 2_طريقة_التثبيت.pdf, 3_طريقة_الاستخدام.pdf,
#                4_فيديو_التثبيت.mp4 (only when design\guide-shots\install-video.mp4 exists)
#   للمستشفى\    the handoff in Arabic and English, SHA256.txt, the release notes, verify-dist.txt
#   للطباعة\     the A5 quick-start card
#   للمطور\      README, screenshots of the current version (docs\screenshots-v1.1, fictional demo data), the sources of the documents; nothing sensitive
# Checks: the parents folder holds only what parents need; no signing file, password file, evidence or
# device dump anywhere in dist; exactly one APK and it passes verify-dist. Exit code 1 when a check fails.
param([switch]$Rebuild)
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = (Resolve-Path "$PSScriptRoot\..").Path
$app = Join-Path $root "dist\Koko-Dawa-App"
$parents = Join-Path $app "للأهل"
$apkDir = Join-Path $parents "1_التطبيق"
$hospital = Join-Path $app "للمستشفى"
$print = Join-Path $app "للطباعة"
$dev = Join-Path $app "للمطور"
$fails = New-Object System.Collections.Generic.List[string]
function Check([bool]$ok, [string]$what) { "  [{0}] {1}" -f $(if ($ok) { "OK" } else { "FAIL" }), $what; if (-not $ok) { $fails.Add($what) } }
function Run-Script([string]$name, [string]$arguments) {
    # A process with its output in a file, so a Gradle daemon can never keep a pipe open (build-release.ps1).
    $log = Join-Path $env:TEMP "build-dist-$name.log"
    $p = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$(Join-Path $PSScriptRoot "$name.ps1")`" $arguments" -NoNewWindow -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $null = $p.Handle
    $p.WaitForExit()
    Get-Content $log -Encoding UTF8 | ForEach-Object { Write-Host "    $_" }   # shown, not returned
    return $p.ExitCode
}

"build-dist " + (Get-Date -Format "yyyy-MM-dd HH:mm")
if ($Rebuild) { Check ((Run-Script "build-release" "") -eq 0) "release APK built with the real key and checked" }

$apks = @(Get-ChildItem -LiteralPath $apkDir -Filter *.apk -ErrorAction SilentlyContinue)
Check ($apks.Count -eq 1) "exactly one app file in the parents folder ($($apks.Name -join ', '))"
if ($apks.Count -eq 1) {
    $code = Run-Script "verify-dist" "-Apk `"$($apks[0].FullName)`" -Report `"$(Join-Path $hospital 'verify-dist.txt')`" -Sha256File `"$(Join-Path $hospital 'SHA256.txt')`""
    Check ($code -eq 0) "verify-dist: all checks passed (verify-dist.txt, SHA256.txt in the hospital folder)"
    $version = [regex]::Match($apks[0].Name, '^Koko-Dawa-(.+)\.apk$').Groups[1].Value
    $notes = Join-Path $root "docs\print\release-notes-$version.txt"
    if (Test-Path $notes) {
        # With a BOM, so Notepad on any Windows shows the Arabic correctly.
        [IO.File]::WriteAllText((Join-Path $hospital "ملاحظات_الإصدار_$version.txt"), [IO.File]::ReadAllText($notes, [Text.Encoding]::UTF8), (New-Object Text.UTF8Encoding $true))
    }
    Check (Test-Path $notes) "release notes for $version (docs\print\release-notes-$version.txt)"
}

Check ((Run-Script "make-docs" "") -eq 0) "guides and handoff built, page counts right (look at the pages)"
$card = Run-Script "make-card" ""
Check ($card -eq 0) "quick-start card built (QR code read back when dist\link.txt has the link)"

# The developer folder: mirrors of the sources (robocopy /MIR also removes what is gone).
New-Item -ItemType Directory -Force $dev | Out-Null
Copy-Item (Join-Path $root "README.md") (Join-Path $dev "README.md") -Force
Copy-Item (Join-Path $root "docs\guide-shots-needed.md") (Join-Path $dev "guide-shots-needed.md") -Force
Copy-Item (Join-Path $root "docs\whatsapp-message-ar.txt") (Join-Path $dev "whatsapp-message-ar.txt") -Force
robocopy (Join-Path $root "docs\screenshots-v1.1") (Join-Path $dev "screenshots") /MIR /NFL /NDL /NJH /NJS /NP | Out-Null
robocopy (Join-Path $root "docs\print") (Join-Path $dev "document-sources") /MIR /NFL /NDL /NJH /NJS /NP | Out-Null

# Checks on the whole tree
$allowedParents = @("1_التطبيق", "2_طريقة_التثبيت.pdf", "3_طريقة_الاستخدام.pdf", "4_فيديو_التثبيت.mp4")
$extra = @(Get-ChildItem -LiteralPath $parents | Where-Object { $allowedParents -notcontains $_.Name })
Check ($extra.Count -eq 0) "the parents folder holds only the app, the two guides and the video ($(if ($extra) { 'extra: ' + ($extra.Name -join ', ') } else { 'nothing else' }))"
$secret = @(Get-ChildItem -LiteralPath (Join-Path $root "dist") -Recurse -File | Where-Object { $_.Name -match '\.(jks|keystore)$|^keystore\.properties$|^local\.properties$|\.hprof$|^bugreport|window_dump|\.logcat$' })
Check ($secret.Count -eq 0) "no signing, password, local or device-dump file in dist"
$evidence = @(Get-ChildItem -LiteralPath (Join-Path $root "dist") -Recurse -Directory | Where-Object { $_.Name -eq "evidence" })
Check ($evidence.Count -eq 0) "no evidence folder in dist"
foreach ($f in @("وثيقة_التسليم_عربي.pdf", "Handoff-EN.pdf", "SHA256.txt", "verify-dist.txt")) { Check (Test-Path -LiteralPath (Join-Path $hospital $f)) "hospital folder: $f" }
Check (@(Get-ChildItem -LiteralPath $print -Filter *.pdf).Count -eq 1) "print folder: the card"

"Contents of dist\Koko-Dawa-App (sizes in bytes):"
Get-ChildItem -LiteralPath $app -Recurse -File | Where-Object { $_.FullName -notmatch '\\للمطور\\(screenshots|document-sources)\\' } | ForEach-Object { "  " + $_.FullName.Substring($app.Length + 1) + "  " + $_.Length }
"  للمطور\screenshots: $(@(Get-ChildItem -LiteralPath (Join-Path $dev 'screenshots') -Recurse -File).Count) files; للمطور\document-sources: $(@(Get-ChildItem -LiteralPath (Join-Path $dev 'document-sources') -Recurse -File).Count) files"
if ($fails.Count -gt 0) { "RESULT: $($fails.Count) check(s) FAILED"; exit 1 } else { "RESULT: all checks passed" }
