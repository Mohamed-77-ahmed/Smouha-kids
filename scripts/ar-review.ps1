# Builds docs/ar-review.md, the sheet for a native Egyptian Arabic review, from the two strings files.
# It never changes a string. Usage:
#   powershell -File scripts\ar-review.ps1 [-Out docs\ar-review.md] [-Baseline ..\tracker-snapshots\batch2-item1]
# -Baseline: an older copy of the project; strings that are new or changed since then are marked.
param(
    [string]$Out = "docs\ar-review.md",
    [string]$Baseline = "..\tracker-snapshots\batch2-final",
    [string]$Label = "batch 3"
)
$ErrorActionPreference = "Stop"
$res = "app\src\main\res"

function Read-Strings([string]$path) {
    # Map: row key ("name", "name [few]", "name [2]") -> text. Order kept.
    $map = [ordered]@{}
    $meta = @{}
    if (-not (Test-Path $path)) { return @{ Map = $map; Meta = $meta } }
    $xml = [xml](Get-Content -Raw -Encoding UTF8 $path)
    foreach ($node in $xml.resources.ChildNodes) {
        if ($node.NodeType -ne "Element") { continue }
        $name = $node.GetAttribute("name")
        # .Name would be the "name" attribute here (PowerShell XML adapter), so LocalName.
        switch ($node.LocalName) {
            "string" {
                $map[$name] = Clean $node.InnerText
                $meta[$name] = @{ Base = $name; Translatable = ($node.GetAttribute("translatable") -ne "false") }
            }
            "plurals" {
                foreach ($item in $node.item) {
                    $k = "$name [$($item.quantity)]"
                    $map[$k] = Clean $item.InnerText
                    $meta[$k] = @{ Base = $name; Quantity = $item.quantity; Translatable = $true }
                }
            }
            "string-array" {
                $i = 0
                foreach ($item in $node.item) {
                    $i++
                    $k = "$name [$i]"
                    # An <item> without attributes comes back as a plain string from the XML adapter.
                    $map[$k] = Clean $(if ($item -is [string]) { $item } else { $item.InnerText })
                    $meta[$k] = @{ Base = $name; Translatable = $true }
                }
            }
        }
    }
    return @{ Map = $map; Meta = $meta }
}

function Clean([string]$s) {
    # Android escapes as a reader sees them; keep placeholders like %1$s visible.
    $s = $s.Trim()
    if ($s.StartsWith('"') -and $s.EndsWith('"') -and $s.Length -ge 2) { $s = $s.Substring(1, $s.Length - 2) }
    return ($s -replace "\\'", "'" -replace '\\"', '"' -replace '\\n', ' / ')
}

function Cell([string]$s) { if ($null -eq $s) { return "" } ; return ($s -replace '\|', '\|') }

# --- the strings now and at the baseline
$en = Read-Strings "$res\values\strings.xml"
$ar = Read-Strings "$res\values-ar\strings.xml"
$baseEn = Read-Strings (Join-Path $Baseline "$res\values\strings.xml")
$baseAr = Read-Strings (Join-Path $Baseline "$res\values-ar\strings.xml")
$haveBaseline = $baseEn.Map.Count -gt 0

# --- where each resource is used: file name -> screen
$screens = [ordered]@{
    'MainActivity.kt' = 'Home'; 'activity_main.xml' = 'Home'; 'HomeAdapter.kt' = 'Home'; 'item_dose.xml' = 'Home';
    'item_patient_block.xml' = 'Home'; 'item_section.xml' = 'Home'; 'item_no_doses.xml' = 'Home';
    'SettingsActivity.kt' = 'Parent settings'; 'activity_settings.xml' = 'Parent settings';
    'PinSheet.kt' = 'PIN sheet'; 'sheet_pin.xml' = 'PIN sheet'; 'view_pin_header.xml' = 'PIN sheet';
    'view_pin_keypad.xml' = 'PIN sheet'; 'view_pin_forgot.xml' = 'PIN sheet';
    'OnboardingActivity.kt' = 'First launch'; 'activity_onboarding.xml' = 'First launch';
    'MedicineEditActivity.kt' = 'Add / edit medicine'; 'activity_medicine_edit.xml' = 'Add / edit medicine'; 'item_day_times.xml' = 'Add / edit medicine';
    'ManageActivity.kt' = 'Patients and medicines'; 'activity_manage.xml' = 'Patients and medicines';
    'item_manage_medicine.xml' = 'Patients and medicines'; 'item_manage_patient.xml' = 'Patients and medicines';
    'PatientDialogFragment.kt' = 'Patient dialog'; 'dialog_patient.xml' = 'Patient dialog'; 'view_gender.xml' = 'Patient form (gender)';
    'GenderPicker.kt' = 'Patient form (gender)';
    'Celebration.kt' = 'Reward and streak screens'; 'view_celebration.xml' = 'Reward and streak screens';
    'Notifications.kt' = 'Reminder notification'; 'AlarmSound.kt' = 'Reminder notification'; 'AlarmSoundService.kt' = 'Reminder notification';
    'ConfirmDialogFragment.kt' = 'Yes/no dialog'; 'Fmt.kt' = 'Numbers and dates (many screens)'; 'AndroidManifest.xml' = 'Launcher (app name)';
    'Validation.kt' = 'Form errors'
}
$sources = Get-ChildItem app\src\main -Recurse -File -Include *.kt, *.xml | Where-Object { $_.FullName -notmatch '\\values' }
$usage = @{}
foreach ($f in $sources) {
    $text = [IO.File]::ReadAllText($f.FullName)
    foreach ($m in [regex]::Matches($text, '(?:R\.(?:string|plurals|array)\.|@(?:string|plurals|array)/)([A-Za-z0-9_]+)')) {
        $key = $m.Groups[1].Value
        $screen = if ($screens.Contains($f.Name)) { $screens[$f.Name] } else { [IO.Path]::GetFileNameWithoutExtension($f.Name) }
        if (-not $usage.ContainsKey($key)) { $usage[$key] = New-Object System.Collections.Generic.List[string] }
        if (-not $usage[$key].Contains($screen)) { $usage[$key].Add($screen) }
    }
}

# --- second person in the Arabic (the app does not know if the reader is a boy or a girl, CLAUDE.md 9b)
$secondPerson = @(
    'أنت', 'انت', 'إنت', 'أنتي', 'انتي', 'إنتي', 'أنتم', 'انتو', 'إنتو',
    'ليك', 'ليكي', 'لك', 'لكي', 'عندك', 'عندكي', 'بتاعك', 'بتاعتك', 'منك', 'معاك', 'معاكي', 'عليك', 'عليكي', 'نفسك',
    'اضغط', 'اضغطي', 'دوس', 'دوسي', 'اختار', 'اختاري', 'اكتب', 'اكتبي', 'ادخل', 'ادخلي', 'افتح', 'افتحي', 'روح', 'روحي',
    'شوف', 'شوفي', 'خلي', 'خليك', 'خليكي', 'حط', 'حطي', 'استنى', 'استني', 'جرب', 'جربي', 'اعمل', 'اعملي', 'سيب', 'سيبي',
    'اقفل', 'اقفلي', 'شغل', 'شغلي', 'فعل', 'فعلي', 'تأكد', 'تأكدي', 'اتأكد', 'اتأكدي', 'متأكد', 'متأكدة',
    'تقدر', 'تقدري', 'عايز', 'عايزة', 'عاوز', 'عاوزة', 'محتاج', 'محتاجة', 'هتقدر', 'هتقدري', 'نسيت'
)
$pattern = '(?<![\p{L}\p{M}])(' + (($secondPerson | ForEach-Object { [regex]::Escape($_) }) -join '|') + ')(?![\p{L}\p{M}])'
function Second-Person([string]$s) {
    if (-not $s) { return @() }
    return @([regex]::Matches($s, $pattern) | ForEach-Object { $_.Value } | Select-Object -Unique)
}

# --- rows
$rows = foreach ($k in $en.Map.Keys) {
    $m = $en.Meta[$k]
    $arText = if ($ar.Map.Contains($k)) { $ar.Map[$k] } elseif (-not $m.Translatable) { "(not translated: the same in both languages)" } else { "" }
    $state = ""
    if ($haveBaseline) {
        if (-not $baseEn.Map.Contains($k) -and -not ($m.Quantity -and $baseEn.Meta.Values.Where({ $_.Base -eq $m.Base }).Count -gt 0)) { $state = "new in $Label" }
        elseif ($baseAr.Map.Contains($k) -and $ar.Map.Contains($k) -and $baseAr.Map[$k] -ne $ar.Map[$k]) { $state = "changed in $Label" }
        elseif ($baseEn.Map.Contains($k) -and $baseEn.Map[$k] -ne $en.Map[$k]) { $state = "English changed in $Label" }
    }
    $hits = Second-Person $arText
    [pscustomobject]@{
        Key = $k; Base = $m.Base; En = $en.Map[$k]; Ar = $arText
        Screen = if ($usage.ContainsKey($m.Base)) { ($usage[$m.Base] | Sort-Object) -join ', ' } else { "(not used in code)" }
        State = $state; Second = ($hits -join ', ')
    }
}
# Arabic plural forms that English does not have (zero, two, few, many) get their own rows too.
$extra = foreach ($k in $ar.Map.Keys) {
    if ($en.Map.Contains($k)) { continue }
    $m = $ar.Meta[$k]
    $enOther = $en.Map["$($m.Base) [other]"]
    $hits = Second-Person $ar.Map[$k]
    [pscustomobject]@{
        Key = $k; Base = $m.Base; En = "(English has no '$($m.Quantity)' form; 'other': $enOther)"; Ar = $ar.Map[$k]
        Screen = if ($usage.ContainsKey($m.Base)) { ($usage[$m.Base] | Sort-Object) -join ', ' } else { "(not used in code)" }
        State = if ($haveBaseline -and -not $baseAr.Map.Contains($k)) { "new in $Label" } elseif ($haveBaseline -and $baseAr.Map[$k] -ne $ar.Map[$k]) { "changed in $Label" } else { "" }
        Second = ($hits -join ', ')
    }
}
$all = @($rows) + @($extra)
# Plural forms next to each other, in CLDR order.
$order = @{ 'zero' = 0; 'one' = 1; 'two' = 2; 'few' = 3; 'many' = 4; 'other' = 5 }
$position = @{}; $i = 0; foreach ($r in $rows) { if (-not $position.ContainsKey($r.Base)) { $position[$r.Base] = $i++ } }
$all = $all | Sort-Object @{ Expression = { $position[$_.Base] } }, @{ Expression = { $q = [regex]::Match($_.Key, '\[(\w+)\]').Groups[1].Value; if ($order.ContainsKey($q)) { $order[$q] } else { [int]("0" + $q) } } }

$flagged = @($all | Where-Object { $_.Second })
# What the generator's author read in each hit; the reviewer decides.
$notes = @{
    'pin_reset_message' = 'Probably fine: اتأكد is passive here (the phone lock was confirmed).'
    'forgot_pin' = 'نسيت reads as "I forgot" or as "you forgot" (masculine). A noun phrase would be neutral for sure.'
    'notif_rationale_message' = 'Probably fine: محتاج describes the app (التطبيق), not the reader.'
    'reward_teen_title' = 'Probably fine: شغل is the noun "work" here.'
    'broken_adult_title' = 'Second person: ليك ("for you", masculine) addresses the reader. Same wording as the CLAUDE.md glossary; needs a neutral form.'
}
$missing = @($all | Where-Object { -not $_.Ar })
$new = @($all | Where-Object { $_.State })

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Arabic review sheet (Egyptian Arabic)")
$lines.Add("")
$lines.Add("Generated by ``scripts/ar-review.ps1`` from ``app/src/main/res/values/strings.xml`` (English) and ``values-ar/strings.xml`` (Egyptian Arabic) on $(Get-Date -Format 'yyyy-MM-dd'). Nothing in the app was changed to make it. Run the script again after any string change.")
$lines.Add("")
$lines.Add("For the reviewer (CLAUDE.md 9b): Egyptian colloquial, short and warm; gender-neutral (first person, passive or nouns, no second-person verbs or imperatives); full sentences with placeholders (``%1`$s`` is a name or a medicine, ``%d`` a number); Western digits; every plural form must read naturally (Arabic uses zero, one, two, few for 3 to 10, many for 11 to 99, other).")
$lines.Add("")
$lines.Add("- Strings: $($all.Count) rows ($(@($all | Where-Object { $_.Key -match '\[' }).Count) of them plural forms or list items).")
$lines.Add("- Second person found by the word list: $($flagged.Count) (first table). The list: $($secondPerson -join ', ').")
$lines.Add("- New or changed in $Label (against $Baseline): $($new.Count), marked in the last column$(if (-not $haveBaseline) { ' (no baseline found, so nothing is marked)' }).")
$lines.Add("- Arabic missing: $($missing.Count).")
$lines.Add("")
$lines.Add("## 1. Second person (check first)")
$lines.Add("")
if ($flagged.Count -eq 0) {
    $lines.Add("No Arabic string matched the word list. A native speaker should still read every row below, because a list cannot catch every form.")
} else {
    $lines.Add("| Key | English | Egyptian Arabic | Screen | Words found | Note | $Label |")
    $lines.Add("|---|---|---|---|---|---|---|")
    foreach ($r in $flagged) { $lines.Add("| ``$($r.Key)`` | $(Cell $r.En) | $(Cell $r.Ar) | $(Cell $r.Screen) | $(Cell $r.Second) | $(Cell $notes[$r.Base]) | $($r.State) |") }
}
$lines.Add("")
$lines.Add("## 2. Every string")
$lines.Add("")
$lines.Add("| Key | English | Egyptian Arabic | Screen | $Label |")
$lines.Add("|---|---|---|---|---|")
foreach ($r in $all) { $lines.Add("| ``$($r.Key)`` | $(Cell $r.En) | $(Cell $r.Ar) | $(Cell $r.Screen) | $($r.State) |") }
$lines.Add("")
$lines.Add("## 3. Reviewer notes")
$lines.Add("")
$lines.Add("| Key | Suggested Arabic | Why |")
$lines.Add("|---|---|---|")
$lines.Add("|  |  |  |")

[IO.File]::WriteAllLines((Join-Path (Get-Location) $Out), $lines, (New-Object System.Text.UTF8Encoding $false))
"Wrote ${Out}: $($all.Count) rows, $($flagged.Count) second person, $($new.Count) new or changed in $Label, $($missing.Count) missing."
