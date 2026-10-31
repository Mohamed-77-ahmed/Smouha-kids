# Seeds FICTIONAL demo data into the app on the TEST emulator only (Pixel_API_34_test), for screenshots
# (CLAUDE.md 14). The names are made up and belong to no real patient.
#   set ANDROID_SERIAL=emulator-5560
#   powershell -File scripts\seed-demo.ps1 [-Lang en|ar] [-Broken none|child|teen|adult] [-Empty]
# A child (7), a teen (15) and an adult (40), each with daily medicines, points and a running streak.
# -Lang ar: Arabic patient names (medicine names stay Latin, as on Egyptian packs).
# -Broken: that patient's streak just ended, so home shows the streak-ended screen once.
# -Empty: no patients at all (the empty state).
# Replaces patients, medicines, doses and rewards; never touches the PIN, the language or other
# settings. Needs a finished first launch (language and PIN) and adb root (google_apis image).
# Refuses a phone and the manual emulator Pixel_API_34 (CLAUDE.md 0b).
param(
    [ValidateSet("en", "ar")][string]$Lang = "en",
    [ValidateSet("none", "child", "teen", "adult")][string]$Broken = "none",
    [switch]$Empty
)
$ErrorActionPreference = "Stop"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.smouha.kidsreminder"
$db = "/data/data/$pkg/databases/pill_pals.db"

if (-not $env:ANDROID_SERIAL) { throw "seed-demo: set ANDROID_SERIAL to the test emulator (emulator-5560)" }
$avd = "$(& $adb -s $env:ANDROID_SERIAL emu avd name 2>$null | Select-Object -First 1)".Trim()
if ($avd -ne "Pixel_API_34_test") { throw "seed-demo: refusing, $env:ANDROID_SERIAL is '$avd', not Pixel_API_34_test" }

& $adb root | Out-Null
Start-Sleep 2
& $adb wait-for-device
$hasPin = "$(& $adb shell "sqlite3 $db 'SELECT COUNT(*) FROM app_settings WHERE pinHash IS NOT NULL'" 2>$null)".Trim()
if ($hasPin -ne "1") { throw "seed-demo: the app has no PIN yet. Finish the first launch (language, PIN) first." }

# Today on the device clock, as the app sees it (local epoch day).
$secs = [long]"$(& $adb shell date +%s)".Trim()
$z = "$(& $adb shell date +%z)".Trim()
$offset = ([int]$z.Substring(1, 2) * 3600 + [int]$z.Substring(3, 2) * 60) * $(if ($z[0] -eq '-') { -1 } else { 1 })
$today = [long][math]::Floor(($secs + $offset) / 86400)
$since = $today - 30

if ($Lang -eq "ar") { $names = @("نور", "كريم", "سلمى") } else { $names = @("Nour", "Karim", "Salma") }
# id, name, age, gender, points, streak, best
$patients = @(
    @(1, $names[0], 7, "FEMALE", 135, 3, 5),
    @(2, $names[1], 15, "MALE", 240, 6, 9),
    @(3, $names[2], 40, "FEMALE", 80, 2, 4)
)
# id, patient, name, minutes of day
$medicines = @(
    @(1, 1, "Vitamin D", @(540, 1200)),
    @(2, 1, "Iron syrup", @(840)),
    @(3, 2, "Augmentin 1g", @(480, 1200)),
    @(4, 3, "Omega 3", @(600))
)
$brokenId = @{ none = 0; child = 1; teen = 2; adult = 3 }[$Broken]

$sql = @("PRAGMA foreign_keys=ON;", "BEGIN;")
foreach ($t in "dose_event", "medicine_slot", "medicine", "dose_award", "perfect_day_award", "streak_milestone_award", "rewards", "patient") {
    $sql += "DELETE FROM $t;"
}
$sql += "UPDATE app_settings SET selectedPatientId = NULL;"
if (-not $Empty) {
    foreach ($p in $patients) {
        $sql += "INSERT INTO patient (id, name, ageAtCreation, createdAtEpochDay, gender) VALUES ($($p[0]), '$($p[1])', $($p[2]), $since, '$($p[3])');"
        if ($p[0] -eq $brokenId) {
            # The streak of $($p[5]) days ended yesterday: shown once on the next open.
            $sql += "INSERT INTO rewards (patientId, totalPoints, currentStreak, streakStartEpochDay, bestStreak, lastPerfectDay, lastEvaluatedEpochDay, brokenStreakPending) VALUES ($($p[0]), $($p[4]), 0, NULL, $($p[6]), $($today - 2), $($today - 1), $($p[5]));"
        } else {
            $sql += "INSERT INTO rewards (patientId, totalPoints, currentStreak, streakStartEpochDay, bestStreak, lastPerfectDay, lastEvaluatedEpochDay, brokenStreakPending) VALUES ($($p[0]), $($p[4]), $($p[5]), $($today - $p[5]), $($p[6]), $($today - 1), $($today - 1), NULL);"
        }
    }
    $slot = 1
    foreach ($m in $medicines) {
        $sql += "INSERT INTO medicine (id, patientId, name, startEpochDay, endEpochDay, active, reminderIntervalMinutes) VALUES ($($m[0]), $($m[1]), '$($m[2])', NULL, NULL, 1, 10);"
        foreach ($minute in $m[3]) {
            foreach ($day in 1..7) {
                $sql += "INSERT INTO medicine_slot (id, medicineId, dayOfWeek, minuteOfDay, validFromEpochDay, validFromMinuteOfDay) VALUES ($slot, $($m[0]), $day, $minute, $since, 0);"
                $slot++
            }
        }
    }
}
$sql += "COMMIT;"

$tmp = Join-Path $env:TEMP "seed-demo.sql"
[IO.File]::WriteAllText($tmp, ($sql -join "`n") + "`n", (New-Object System.Text.UTF8Encoding $false))
& $adb shell am force-stop $pkg
& $adb push $tmp /data/local/tmp/seed-demo.sql | Out-Null
& $adb shell "sqlite3 $db < /data/local/tmp/seed-demo.sql"
& $adb shell rm /data/local/tmp/seed-demo.sql
$count = "$(& $adb shell "sqlite3 $db 'SELECT COUNT(*) FROM patient'")".Trim()
"seed-demo: $count demo patients (lang $Lang, broken streak: $Broken, today epoch day $today) on $env:ANDROID_SERIAL"
& $adb shell am start -n "$pkg/com.example.tracker.MainActivity" | Out-Null
