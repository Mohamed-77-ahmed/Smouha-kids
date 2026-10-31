# Koko Dawa (formerly Smouha Kids, before that Pill Pals): final report of the autonomous run (2026-10-16) and the change requests after it

All five phases of `app/claude.md` are done. Everything below marked VERIFIED was run on the Android 14 (API 34) emulator `Pixel_API_34` or by the test suites named; nothing was tested on a real phone. Evidence lives in `docs/evidence/phase-N/`, the step-by-step log in `docs/PROGRESS.md`, choices in `docs/DECISIONS.md`.

## 1. Features

| Feature | Status | Evidence |
|---|---|---|
| **Data and screens** | | |
| Room database v1 (9 tables, FK on, cascades, unique dose index), schema exported | VERIFIED | phase-1/schema_v1.json, instrumented SchemaTest + RepositoryDataTest |
| Several patients per phone, same names kept apart ("Yasmin · 7 (1)/(2)"), deleting one leaves the other exact | VERIFIED | phase-1/15, RepositoryDataTest.deletingOnePatientLeavesTheOtherExact |
| Flexible schedules: every day, specific days, different times per day, start/end dates | VERIFIED | phase-1/14, 17, phase-2/22, ScheduleTest |
| Add / edit / delete medicine; edits change only the future, TAKEN history kept | VERIFIED | phase-1/23 to 26, RepositoryDataTest.editRemovingSlots... |
| Home filter (All + one chip per patient), view-only, saved in DB, falls back to All | VERIFIED | phase-1/19 + 19_filter_db_check.txt |
| State survives restart, process kill, reboot, rotation, Don't keep activities (half-filled form too) | VERIFIED | phase-1/20, 21, 22, 27 to 29 |
| Egyptian Arabic + English, RTL mirrored, Western digits, ص/م, Saturday-first week, per-app language | VERIFIED | phase-1/32 to 45, LocalizationTest |
| Font scale 1.5 and landscape without covered controls | VERIFIED | phase-1/30, 31, 40 to 44 |
| **Reminders** | | |
| Exact alarm per slot, fires on time with the patient's name, child wording for children | VERIFIED | phase-2/03 (01:30:00.044), phase-5/03 (release build) |
| Notification text in the app language at fire time | VERIFIED | phase-2/07 |
| Follow-ups at the chosen interval, exactly 3, stop on confirm, continue after kill and reboot with the right count | VERIFIED | phase-2/04, 07, 08, 14, 15, RepositoryReminderTest |
| Tap opens the confirm step of exactly that dose (warm and cold start) | VERIFIED | phase-2/05, 09 |
| Deleted medicine silent, edited slot moves its alarm | VERIFIED | phase-2/01, 03, 04 |
| Re-arm on every launch, boot, time change, timezone change, app update, force-stop then launch | VERIFIED | phase-2/11, 14 to 17, phase-1/22 |
| Weekday-only and date-range medicines (emulator clock) | VERIFIED | phase-2/22 |
| Notification permission asked after language, PIN and first medicine; banner when missing | VERIFIED | phase-1/11, 12, phase-2/02, 12, 13 |
| Exact alarms automatic on Android 13+ (USE_EXACT_ALARM); banner and inexact fallback when missing | VERIFIED on emulator (fallback with a temporary test build) | phase-2/18 to 20 |
| Battery settings hint and button; test reminder button | VERIFIED (button opens the screen; test reminder arrives in about 10 s) | phase-2/13 |
| Reminders on Xiaomi, Samsung, Oppo battery managers, Doze on a real phone | UNVERIFIED (needs real device) | |
| Exact-alarm grant behavior on a real Android 12 phone | UNVERIFIED (needs real device) | |
| **PIN** | | |
| Created on first launch after the language; salted PBKDF2 hash only | VERIFIED | phase-1/02 to 04, phase-3/08 |
| Gates: confirm (toggle), add, edit, settings; delete behind edit/settings; toggle off affects confirm only | VERIFIED | phase-3/01 to 03, 08, 09 |
| Wrong PIN never confirms; 5th wrong try locks 60 s; lock survives a process kill | VERIFIED | phase-3/02, 05 to 07, RepositoryPinTest, PinTest |
| Forgot PIN only through the phone's screen lock, all data kept; disabled with explanation without a lock | VERIFIED | phase-3/10 to 13, 07_reset_via_phone_lock.txt |
| One PIN dialog at a time (rapid taps, rotation) | VERIFIED | phase-3/04 |
| **Rewards and animations** | | |
| Points, levels, perfect day, milestones, all anti-farming invariants (CLAUDE.md 6) | VERIFIED | RewardsTest, RepositoryRewardsTest (16 cases) |
| Streak rules (6b): missed day breaks, neutral days, one animation after days away, shown once, DST, month/year, timezone, clock backward | VERIFIED | RepositoryRewardsTest, phase-4/06 to 11 |
| Reward animation CHILD / TEEN / ADULT | VERIFIED (screenshots) | phase-4/01 to 05 |
| Streak-broken animation CHILD / TEEN / ADULT, siblings one after another, no replay | VERIFIED (screenshots) | phase-4/06 to 11 |
| Animations survive rotation and mirror in RTL | VERIFIED | phase-4/12 and the Arabic screenshots |
| **Release** | | |
| Signed release from keystore.properties; clear failure without it | VERIFIED with a throwaway key (deleted) | phase-5/01, 02 |
| No INTERNET permission, allowBackup false, not debuggable | VERIFIED (aapt) | phase-5/02 |
| Release APK installs and the full flow works | VERIFIED on emulator | phase-5/03 to 06 |
| Clean-copy build with only ANDROID_HOME | VERIFIED | phase-5/04 |
| Real signing key, release to patients | NOT DONE (manual step) | docs/RELEASE.md |
| Typing an Arabic name with a real keyboard | UNVERIFIED (adb cannot type Arabic; unit tests accept Arabic names) | DECISIONS D16 |
| Speed on a real phone (PIN check, cold start) | UNVERIFIED (the emulator uses software rendering and is slow) | |

## 2. Tests and what they cover

Final run: `gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`, exit 0, lint 0 errors (MissingTranslation, HardcodedText, RtlHardcoded, RtlSymmetry are errors). File: `docs/evidence/phase-5/05_final_test_results.txt`.

**64 JUnit tests** (pure Kotlin, no Android, injected time):

- **AgesTest (6):** age groups at 12/13/17/18, aging after a year, leap day, clock before the anchor.
- **ScheduleTest (11):** ISO weekdays, Monday-only vs daily, per-day times, start/end, slot added late, next occurrence strictly after now, Cairo DST gap and overlap.
- **ValidationTest (17):** empty and too long names, Arabic names, age 0 to 120, Arabic-Indic digits, missing patient, no slots, duplicate slots, end before start, intervals.
- **ReminderRulesTest (5):** follow-up times, exactly 3, overdue catch-up, count after a kill.
- **PinTest (9):** 4 to 8 digits, salted hash without the PIN, verify, 4 vs 5 wrong tries, 60 s boundary, clock moved back.
- **RewardsTest (15):** on-time window to the second, levels, milestone list, confirm window, streak transitions.
- One template test.

**55 instrumented tests** on the emulator (in-memory Room, injected clock):

- **RepositoryDataTest (16):** same-name patients, delete with a control patient, unique index, FK, transaction rollback, schedules, edits, filter, derived perfect day.
- **RepositoryReminderTest (8):** announce once, 3 follow-ups, stop on confirm, other day, deleted medicine, taken early, running follow-ups after a kill.
- **RepositoryPinTest (6):** storage, 4 vs 5, lock survives restart, reset keeps everything, wrong PIN changes nothing.
- **RepositoryRewardsTest (16):** every invariant of section 6 and rule of 6b with exact totals.
- **LocalizationTest (7):** Arabic plurals 0/1/2/3/10/11/100, Western digits, ص/م, week order, bidi.
- **SchemaTest (1):** exported v1 equals the DB, migration harness ready for v2.
- One template test.

**Mutation checks:** 37 planted bugs (14 in Phase 1, 6 in Phase 2, 8 in Phase 3, 9 in Phase 4), all caught. Reports are in `docs/evidence/phase-N/mutation_check*.txt`.

**Not covered by automated tests:** UI flows (they were driven on the emulator with `docs/evidence/tools/ui.ps1` instead), real devices, real OEM battery managers.

## 3. Decisions taken

Full list with alternatives in `docs/DECISIONS.md`. The ones that change behavior:

- **D5:** the whole v1 schema was built in Phase 1.
- **D6:** PIN is 4 to 8 digits (kept from the old code).
- **D7:** PBKDF2 with 120,000 iterations.
- **D8:** the re-reminder interval is per medicine.
- **D9:** hard delete plus a points ledger keyed by medicine name.
- **D10:** a pill can be confirmed any time on its own day.
- **D11:** slots carry their own "valid from" time.
- **D12:** a medicine's patient cannot change on edit.
- **D13:** the old "reset points" button was removed.
- **D14:** the Arabic locale is `ar-EG-u-nu-latn`.
- **D17:** typed PIN digits are not kept across rotation.
- **D18:** after a PIN reset the original action continues.
- **D19:** home shows before the launch work finishes.
- **D20:** progress bar while the PIN is checked; the PIN dialog pans in landscape.

## 4. Parking lot (not fixed, on purpose)

- "Every N days" and "as needed" medicines (out of scope for v1).
- Validation messages stay visible while typing until the next Save.
- `androidx.constraintlayout` is unused; newer library versions are available (the only lint warnings left).
- ~~`bundleOf` deprecation warning from the current core-ktx.~~ Fixed after the run.
- Library translations for many languages ship in the APK (only Arabic and English are used).
- ~~Rotating during a PIN check cancels it (the PIN is typed again).~~ Fixed in batch 2: the check runs in the PIN sheet's ViewModel and survives a rotation.
- `app/claude.md` lives in `app/` with a lowercase name, so Claude Code does not load it automatically. Moving it to the project root as `CLAUDE.md` is outside the allowed scope and is left to you.

## 5. Remaining risks

- **Alarms on real phones:** OEM battery savers can still stop reminders. The README tells parents what to change, but it was not tested. The app now plays the alarm sound itself (batch 2), because a Samsung on silent dropped the channel's alarm sound; whether the new sound is heard on that Samsung is UNVERIFIED until `docs/PILOT.md` is run.
- **4-digit PIN:** someone with root access and the database file can brute-force a 4-digit PIN offline (D7). A child who knows the phone's lock code can reset the PIN.
- **Clock changes:** moving the phone clock can shorten the PIN lock (capped at 60 s) and affect streaks. The tests show it never pays twice or goes negative.
- **Arabic copy:** it is my Egyptian colloquial and needs a native speaker.
- **Signing key:** if the real key is lost, installed apps can never be updated (docs/RELEASE.md).

## 6. Changes after the run

- **Erase all data** (user request, D21): parent settings > Erase all data, behind a confirmation and the parent PIN. Deletes every table, cancels reminders and notifications, empties the cache folder and starts again on the language screen. VERIFIED on the emulator in English and Arabic (`docs/evidence/erase-data/`), 3 new instrumented tests, 5 of 5 planted bugs caught. Totals now: 64 unit and 58 instrumented tests.
- **Keyboard over the PIN card on the first screen** (user report): the edge-to-edge screen did not react to the keyboard. Fixed in the onboarding layout and activity; VERIFIED on the emulator in Arabic, English, font scale 1.5 and landscape (`docs/evidence/onboarding-keyboard/`). Samsung keyboard on a real phone UNVERIFIED.
- **Celebrations stay 3 seconds** (user request, D22): every reward and streak-ended screen closes by itself after 3 s (was 2.5 to 5.5 s); a tap still closes it. Measured on the emulator for the adult and child reward (`docs/evidence/celebration-3s/`).
- **Warnings cleaned up** (user request): 0 Kotlin compiler warnings and 0 lint errors; the only lint warnings left are 5 "newer library version available" notes (see PROGRESS.md). Totals now: 64 unit and 60 instrumented tests, all passing on the emulator.

## 7. Change request batch 1 (2026-10-16): verified vs unverified

Details per item in PROGRESS.md, rules in CLAUDE.md, before/after in docs/CHANGELOG.md, decisions D23 to D27. Totals after the batch: 75 unit and 78 instrumented tests, all passing on the emulator; lint 0 errors (4 "newer library version" warnings left).

| Item | Verified (emulator / tests) | Unverified or not built |
|---|---|---|
| 0 Contract | CLAUDE.md 425 -> about 400 lines after all items (375 right after item 0), history in docs/ARCHIVE.md, CHANGELOG created | |
| 1 Settings without PIN | Settings open freely; wrong PIN on toggle, change PIN, manage, alarm sound, erase changes nothing; language works without a PIN (SettingsGatesUiTest, mutation 3/3) | No global re-reminder interval and no "reset points" exist (D23) |
| 2 Alarm sound | Alarm channel USAGE_ALARM with the alarm ringtone; plays with the ringer in vibrate (dumpsys audio); first reminder, 3 follow-ups, tap, confirm, reboot re-arm; upgrade over the real v1 install keeps data and deletes the old channel | How loud it is, ringer mode SILENT as such, Do Not Disturb: real device |
| 3 Gender | Required for new patients, NULL for old ones with a neutral avatar; avatars in chips, headers, reward; migration v1/v2 -> v3 keeps every row (schema v3, not v2: D24/D25); also fixed: Android's per-app language setting was overridden at app start | Native review of the new Arabic labels |
| 4+5 Visual refresh | Palette and gradients, ContrastTest (mutation 3/3), light and dark, EN and AR, splash, fade-through, springs, animations off, no INTERNET, allowBackup false | Lottie (no `design/lottie/`); smoothness on a real phone |
| 6 Screen sizes | 4 sizes x 3 fonts EN, AR at 1.5, two columns at 840dp+, chips wrap, narrow cards, dialog width, rotation; ResponsiveTest (mutation 3/3) | Real foldable postures, drag-resizing in multi-window |

## 8. Change request batch 2 (2026-10-16): verified vs unverified

Hardening after the first real-phone test (Samsung SM-A266B). Details per item in PROGRESS.md, rules in CLAUDE.md, before/after in docs/CHANGELOG.md, decisions D28 to D30, evidence in `docs/evidence/batch2/`. Final suite on the test AVD only: 108 unit and 101 instrumented tests, 0 failures; lint 0 errors (4 "newer library version" warnings). Every automated run used the test AVD `Pixel_API_34_test`; the manual AVD was not touched, and the phone only got read-only commands. Version 1.1.0 (versionCode 2).

| Item | Verified (emulator / tests) | Unverified or not built |
|---|---|---|
| 1 Test isolation | Second AVD for all automation; `scripts/run-tests.cmd` refuses an unset serial, the phone and the manual AVD, and always keeps the app installed; direct `gradlew connected*` is stopped by a Gradle guard (5 of 5 checks); CLAUDE.md Hard rules and 0b updated | |
| 2a Phone diagnosis | Read-only, saved without other apps or patient names: the phone ran a batch 1 build (and before 17:53 an older one, all versionCode 1); DND off, alarm volume 15/15; three reminders on the alarm channel while silent got no alarm-stream player at all, two while normal did. Ranked causes in `phone-diagnosis.txt` | The internal One UI rule; behavior in vibrate on that phone |
| 2b App plays the alarm sound | `AlarmSoundService` (type systemExempted, per the official docs, D28): on SILENT and VIBRATE a USAGE_ALARM player of the app within 3 s, one sound only; stops on tap, confirm, Stop sound, dismiss, 30 s; setting off = no app sound; two doses = one sound, two reminders; taken follow-up silent; both forced start failures fall back to the alarm channel. A real exact alarm with the app in the background started it ("Background started FGS: Allowed"). Mutation 7 of 7 | Whether it is heard on the Samsung on silent, and how loud: `docs/PILOT.md` |
| 2c Sound status rows | Alarm volume, Do Not Disturb, battery and the silent note in parent settings, EN and AR, no PIN; `SoundStatusTest` with a fake phone | The rows on a real phone's DND schedules |
| 2d | | Audibility on the real phone: UNVERIFIED until the pilot |
| 3 Icon | Gradient #16A34A to #2563EB, white two-half capsule (3.29:1 and 5.16:1), safe zone, themed icon, splash; no purple left (scan); `IconContrastTest`, `LauncherIconTest`, mutation 4 of 4; screenshots | Launcher icon shapes other than the emulator's circle (squircle shown by a test render) |
| 4 PIN sheet | One bottom sheet with its own keypad for every PIN entry including the first launch (no keyboard); wrong PIN, lockout, checking, animations off, LTR keypad in Arabic, rotation, no digits in saved state, one sheet; `PinEntryTest`, `PinSheetUiTest`, mutation 6 of 6; screenshot matrix (EN/AR, light/dark, 4 sizes x 3 fonts, landscape). Also fixed: the lock now lasts the full 60 s | The feel of the keys (haptics) on a real phone |
| 5 Arabic review sheet | `docs/ar-review.md` generated (281 rows, all plural forms, 39 new or changed in batch 2, none missing) | The native review itself; one real second-person string waits for it (`broken_adult_title`) |
| 6 Pilot pack | `docs/PILOT.md`: install with `adb -s <serial> install -r`, version check first, 17 checks, results table | The pilot run itself |

## 9. Change request batch 3 (2026-10-17): verified vs unverified

Branding and child-friendly design for the patients of Smouha University Children's Hospital (ages 0 to 18). Details per item in PROGRESS.md, rules in CLAUDE.md (updated in place), before/after in docs/CHANGELOG.md, decisions D31 to D33, evidence in `docs/evidence/batch3/`, screenshots in `docs/screenshots/`. Final suite on the test AVD only: 112 unit and 109 instrumented tests, 0 failures; lint 0 errors (4 "newer library version" warnings). One earlier full run had a single timeout in `AlarmSoundTest` (fallback reminder, first class after the install); that class then passed 10 of 10 twice and the full rerun passed (possible timing flake, not changed). Every automated run used `Pixel_API_34_test`; the manual AVD and the phone were not touched. Version 1.2.0 (versionCode 3), app id `com.smouha.kidsreminder`.

| Item | Verified (emulator / tests) | Unverified, skipped or not built |
|---|---|---|
| 0 applicationId | `com.smouha.kidsreminder` (namespace and package unchanged), every old-id hit fixed and listed; clean install, full flow, a background reminder started the alarm sound service, tap opened the dose and stopped the sound, reboot re-armed the alarms; recorded as permanent (D31), PILOT.md says the old app is removed by hand | The update path from a real phone with the old app (it is a new app for Android; data does not move) |
| 1 App name | Full name as application label, short name on the launcher (`LocalizationTest`), launcher in EN and AR at font 1.0 and 1.5, splash in both languages (Android 12+ image), home header full name or short name by width (`ResponsiveTest`) at 1.0/1.5/2.0, language picker with both names, About; every "Pill Pals" hit listed | Splash on Android 9 to 11 shows the icon only (no slot for text); the logo: `design/brand/` does not exist, nothing drawn or downloaded |
| 2 About | Opens from parent settings without a PIN, installed version, disclaimer, local-only data, no contact details (`SettingsGatesUiTest`); EN/AR at font 1.0 and 2.0 | Native review of the new text |
| 3 Age groups | CHILD 0 to 12, TEEN 13 to 18, ADULT 19+; `AgesTest` boundaries 12/13/18/19 and aging by one year; mutation caught | |
| 4 Design | 28dp cards and sheets, 56dp pill buttons, larger confirm button, softer tinted shadows; vector mascot (happy, cheer, sleepy) on the CHILD reward, CHILD streak ended and empty state; TEEN and ADULT unchanged; idle breath (3 breaths, none with animations off); friendlier gender-neutral copy (13 string changes, `empty_state` no longer an imperative); `MascotTest`, `CelebrationUiTest` (6), `ContrastTest`; 4 mutations caught (one after strengthening the test) | Rounded font: `design/fonts/` does not exist, skipped; how the mascot and motion feel on a real phone |
| 5 Docs and screenshots | `scripts/seed-demo.ps1` (fictional data), `scripts/screenshots.ps1`, 72 shots checked by eye, CLAUDE.md, CHANGELOG, ar-review (288 rows, 5 second-person hits, 20 changed in batch 3, none missing) | |

New parking lot items: Arabic chosen in Android's own per-app language setting comes as plain `ar` and shows Arabic-Indic digits (the in-app picker keeps Western digits); `broken_adult_title` still says "ليك"; at 480x800 and font 2.0 the empty state text starts under the "Add medicine" button (it scrolls clear).

## 10. Change request batch 4 (2026-10-18): verified vs unverified

Release pack for families who install from a Google Drive link. Details per item in PROGRESS.md, rules in CLAUDE.md 10 and 14 (rewritten in place), before/after in docs/CHANGELOG.md, decisions D34 and D35, evidence in `docs/evidence/batch4/`. The real key existed (`keystore.properties`, created by you), so nothing was BLOCKED. Final suite on the test AVD only (`docs/evidence/batch4/final/suite.txt`): 112 unit tests (run again fresh after the suite reused unchanged results) and 109 instrumented tests, 0 failures; lint 0 errors (4 "newer library version" warnings). Version 1.0.0 (versionCode 1). Every automated step used the test AVD `Pixel_API_34_test`; the manual AVD and the Samsung (connected over USB during the batch) got no command at all. Nothing was uploaded or pushed; no repository or remote was created.

| Item | Verified (emulator / tests / scripts) | Unverified or left to you |
|---|---|---|
| 0 Prerequisite | `keystore.properties` and the key exist; Gradle's signing check passed; snapshots never copy the key or its password | |
| 1 Release | 1.0.0 (versionCode 1) signed with the real key, `Smouha-Kids-1.0.0.apk`; `verify-dist`: all checks passed (v2 signature, `CN=Smouha Kids, O=Smouha Kids, C=EG`, SHA-256 `FF:0A:9A:...:E8:7A` recorded in RELEASE.md, both names in both languages, no INTERNET, allowBackup off, not debuggable, 12.8 MB); a debug build, the release re-signed with the debug key and the release re-signed with a throwaway key (deleted afterwards) all fail verify-dist (`final/verify_dist_negative.txt`); main flow from the release APK (reminder in the background with the app's alarm sound, tap, PIN, reward +35, points in the DB); update test 1.0.0 to 1.0.1 with the same key: every row identical (twice, the last on 60 rows of demo data), the reminders booked again; the same APK with another key refused, nothing changed | The signed APK on a real phone (PILOT.md 0a) |
| 2 Documents | Install guide (3 pages), user guide (5 pages, pictures from the release build, fictional names), hospital handoff in Arabic and English (2 pages each), A5 card, WhatsApp message; every PDF page rendered from the PDF and looked at (letters joined, right to left, nothing cut off; 4 layout problems found and fixed); the card's QR code read back from the rendered page equals a test link; no singular imperative, no em dash | The 11 real-phone screens and the install video (dashed boxes until then); the card's QR code with the real link; native review of the documents' Arabic; whether non-technical parents follow the guides |
| 3 `dist/` | `Smouha-Kids-App/` with the four folders as requested; `build-dist`: all checks passed (only parent files in the parents' folder, no secret, password, evidence or dump file in `dist`); `dist/DRIVE.md` | The upload, the sharing settings and the link test (by hand) |
| 4 GitHub | README (English and Arabic, demo screenshots, permissions, privacy, disclaimer, not endorsed by the hospital, all rights reserved), `.gitignore` (evidence, dumps, builds, link file, name list, internal plan), name scan: 3 lines left, all the credit link to the original Medipulse-App project (its author's public GitHub name), redacted 6 old PROGRESS entries; `docs/GITHUB.md` | The repository itself (git is not installed); your decision on the credit link and on publishing `docs/final-plan.md` |

Found and fixed on the way (tooling, not the app): Windows PowerShell waited forever on a pipe kept open by a new Gradle daemon (scripts now wait for the process), gave no exit code until the process handle was read, the old SDK tools cannot open Arabic paths (an ASCII copy is checked), Windows' PDF renderer once kept its process alive (the script ends itself), UiAutomator 1.0 needed a missing jar and the accessibility "set text" action to type Arabic names.

New parking lot items: the emulator's notification shade shows the Arabic reminder left-aligned when the phone's own language is English (Android's layout, correct reading order); the user guide's notification picture shows "now" in English for that reason.

Note: the keystore password was typed into a chat during the setup (stage 4). The key is safe as long as the `.jks` file stays private; changing the password with `keytool -storepasswd` keeps the same key and certificate (then update `keystore.properties`).

## 11. Change request batch 6 (2026-10-18 to 2026-10-19): verified vs unverified

The doctor's Koko Dawa identity (R1 to R8). Details per item in PROGRESS.md, rules in CLAUDE.md (rewritten in place), before/after in docs/CHANGELOG.md, decisions D36 to D41, evidence in `docs/evidence/batch6/`. Version 1.1.0 (versionCode 4), schema v4 (one column, approved by you before the work). Every automated step used the test AVD `Pixel_API_34_test`; no phone, no upload, no push, no remote. `dist/`, the PDFs and the notebooklm pack were NOT rebuilt (they still describe 1.0.0).

| Item | Verified (emulator / tests / scripts) | Unverified, BLOCKED or left to you |
|---|---|---|
| 0 Assets | 8 originals copied to `design/brand/` unchanged; each picture checked for alpha and a checkerboard (measured and looked at, `docs/assets-report.md`); the video re-encoded to H.264 baseline (3.37 MB) and its 20 frames looked at; the sound plays as is | **BLOCKED: "avatar no streak"** exists only on a baked-in checkerboard |
| 1 Name and icon | Koko Dawa / كوكو دوا as the application and launcher label (`LocalizationTest`), the rooster as the adaptive icon inside the safe zone (`LauncherIconTest`, circle and squircle renders), no monochrome layer, splash and notification large icon; 2 mutations caught | How the icon looks on other launchers and themed-icon phones |
| 2 Palette | Palette v2 from the pictures in light and dark (`docs/palette-v2.md`, `.png`), the sky, cream, yellow header gradient, warnings as bordered amber boxes; `ContrastTest` on every pair and gradient stop; 4 mutations caught; no purple; leftovers listed | Your and the doctor's taste |
| 3 Hospital logo | On the language picker over the unchanged hospital name, in the home header (a heading, also at 480x800 with font 2.0, in Arabic and landscape), large on About; no endorsement wording (`HospitalLogoUiTest`, `BrandTextTest`); 5 mutations caught | |
| 4 Avatar and sound | The avatar on CHILD and TEEN rewards, ADULT unchanged; the dose sound once (USAGE_GAME), only with "Celebration sound" on (new PIN-gated setting), never with an alarm (checked to the millisecond), released on close, not replayed after a rotation; the reward lasts max(3 s, min(sound, 6 s)); schema v4 with its Migration and tests; the real database on the emulator migrated with every row; 10 mutations caught | **How loud the sound is on a real phone** (UNVERIFIED) |
| 5 Streak broken | Nothing changed (the asset is BLOCKED): the gentle capsule character stays for a child, the badge for a teen | The picture (see below) |
| 6 End of course | The final-dose rule from the schedule (`CourseEndTest`, 9 cases); the course-complete screen once after the reward of the final dose, the video in a rounded navy card within 80 percent of the screen, holding its last frame with the text, sound following the setting, avatar still on failure, rotation and close (`CourseEndUiTest`, 6); frames in English and Arabic; 6 mutations caught | **The video and its sound on a real phone** (UNVERIFIED) |
| 7 Docs and update | CLAUDE.md, CHANGELOG, `docs/ar-review.md` (296 rows, 10 new or changed, none in the second person), 25 screenshots in `docs/screenshots-v1.1/` from the signed 1.1.0; the signed 1.1.0 passes verify-dist (the recorded certificate, both names in both languages, no INTERNET, allowBackup off, not debuggable, 16.5 MB); update test: 1.0.0 with data, 1.1.0 with the same key over it, every family-data row identical, the new column on, Room's schema fingerprint moved to the exported v4, the patients on home, 42 reminders booked again, another key refused (`docs/evidence/batch6/item7/update_test.txt`) | The doctor's approval of the screenshots and the palette sheet |

BLOCKED asset and exactly what is needed: **"avatar no streak"** (the crying rooster). Please provide the same picture as a PNG with a real transparent background (alpha channel), or on one flat solid color, and put it in the project root. Then the CHILD and TEEN streak-ended screens can show it (CLAUDE.md 6b). Optional: `koko-transparent.png` and `avatar-celebration.png` with real transparency would let the rooster and the celebration creature stand on the app's own background instead of their picture backgrounds.

Found and fixed on the way: Gradle reported resource-only changes as "up to date" for unit tests that read resource files (now declared inputs); the screenshot script missed the reward on the new light backdrop; update-test's `-NextApk` mode overwrote its own parameter (PowerShell names ignore case) and compared whole rows across schema versions; verify-dist expected a launcher label of its own; several UI tests caught animations by timing (now read from the views). The test emulator froze twice with host GPU rendering and now runs with software rendering. Two `pdf-to-png.ps1` processes from batch 4 could not be ended by Windows (a reboot clears them).

New parking lot items: none beyond the BLOCKED picture. Still open from before: `broken_adult_title` "ليك" and `forgot_pin` "نسيت" for the native review.

## 12. Change request batch 7 (2026-10-19): verified vs unverified

Rebuild every deliverable for Koko Dawa 1.1.0 and publish to GitHub, after the doctor approved the batch 6 screenshots and the palette sheet. During the batch you also replaced the end-of-course video ("avatar of finish", D42) and asked to repeat the batch from the start, which was done. Details in PROGRESS.md, rules in CLAUDE.md (section 2 "GitHub access", 0, 0b, 10, 11 rewritten in place), before/after in CHANGELOG, decisions D42 to D45, evidence in `docs/evidence/batch7/`. Test AVD only; nothing was uploaded anywhere.

| Item | Verified (emulator / tests / scripts) | Unverified, BLOCKED or left to you |
|---|---|---|
| Video replacement | `avatar of finish.mp4` copied to `design/brand/`, its 12 frames looked at; its last 2 frames are pure black (measured), so it was cut at 5.995 s and re-encoded like the first video (1.54 MB, every frame non-black); `CourseEndUiTest` 6/6; the screen looked at in EN, AR, dark and landscape | The video and its sound on a real phone |
| 1 Release build | `dist/Koko-Dawa-App/للأهل/1_التطبيق/Koko-Dawa-1.1.0.apk`, 15,427,906 bytes, SHA-256 `f7b3aa43...6cb073122` (equals `SHA256.txt`); verify-dist all passed: the certificate equals docs/RELEASE.md, كوكو دوا / Koko Dawa, no INTERNET, allowBackup false, not debuggable; update test 1.0.0 with data to 1.1.0: every row kept, another key refused | |
| 2 Release pack | Install guide (3 pages), user guide (5 pages, new parts: "Celebration sound" and the end-of-course screen), both handoffs (2 pages each), the A5 card; Koko palette, icon, name, 1.1.0 facts; all 19 guide screenshots taken again from the signed 1.1.0 (fictional names); every PDF page rendered and looked at (joining, right to left, nothing cut); the card's QR read back VERIFIED with a test link; `build-dist`: all checks passed; WhatsApp text, release notes 1.1.0, `dist/DRIVE.md` (also how to replace a 1.0.0 upload in place), README, `docs/GITHUB.md` (the real repository and the PR flow), `docs/guide-shots-needed.md` (re-capture) | The 11 real-phone install screens (placeholder boxes), the support contact, the real Drive link and its QR code |
| 3 Presentation pack | `notebooklm/`: 7 Arabic sources (254 to 520 words, no paths or code), `claims-check.md` (43 claims, each with its evidence and status), `prompts.md` (palette hex values, how to use), `slide-map.md`, 19 screenshots by slide, 6 composites (`scripts/make-composites.ps1`), `video/flow.mp4` (37 s, no audio: reminder in the shade, tap, PIN, reward) | The slides themselves (NotebookLM, by you); the emulator cannot record the alarm sound |
| 4 Leftovers | Searched the whole project for the old names and the old palette (list below); the last real name in a text file removed; `scripts/scan-names.ps1` now reports counts only and knows the new ignore rules: 0 name hits | |
| Suite | 131 unit and 131 instrumented tests, 0 failures; lint 0 errors, 4 warnings (dependency versions) (`docs/evidence/batch7/suite/suite.txt`) | |
| G0 and G1 (GitHub) | `.gitignore` (kh.env, `*.env`, `dist/`, the raw originals) and `.gitattributes` written before anything is staged | **BLOCKED:** Git is not installed and `kh.env` is not in the project. Nothing touched git or GitHub, no token was read. Then: git init, check-ignore, privacy check, pre-push scan, baseline, branch `batch-7-koko-dawa`, PR, draft release v1.1.0 with 4 assets, issues |

Old names left on purpose: the class `PillPalsApp`, the DB file `pill_pals.db`, the test-runner variable `PILLPALS_TEST_RUNNER`, the log tag `SmouhaKidsSound`, the style names, the signing certificate `CN=Smouha Kids` (permanent), the key files `smouhakids-*`, the repository name `Smouha-kids`, and the history in CHANGELOG, DECISIONS, PROGRESS, ARCHIVE, FINAL_REPORT, `docs/final-plan.md`, `docs/screenshots/` (1.0.0 look) and `release-notes-1.0.0.txt`; the user-facing texts name "سموحة للأطفال" / Smouha Kids only to say it is the old name. Old palette values left on purpose: the semantic `success` green, the capsule character's greens (the streak-ended screen keeps it while "avatar no streak" is BLOCKED) and the amber placeholder and draft marks.

## 13. Manual steps left for you

1. **Keystore:** the two backups (USB stick and password manager) and the password in the password manager; tell the second trusted person where the copies are, never the password. Optional: change the password (note above).
2. **GitHub (unblocks G0 and G1):** install Git for Windows, set your commit name and the GitHub noreply e-mail, put `kh.env` (one line `GIT-HUB-PAT=<token>`, a fine-grained token for `Smouha-kids` only, Contents, Pull requests and Issues read and write) in the project folder (`docs/GITHUB.md`). Then ask to continue batch 7: the repository is set up, scanned and pushed, with the PR, the draft release and the issues. You review and merge the PR yourself.
3. **Signed release on a real phone:** on the Samsung, uninstall the debug pilot build by hand (Settings > Apps > Smouha Kids > Uninstall; test data only), install `Koko-Dawa-1.1.0.apk`, ideally from the Drive link like a parent, and run `docs/PILOT.md` 0a (silent, vibrate, swiped from recents, reboot, battery settings, and listen to a child's reward) into its second results table. A second brand (Xiaomi, Oppo) if possible.
4. **Real-phone screenshots and video:** take the 11 screens of `docs/guide-shots-needed.md` again for 1.1.0 (and the install video) into `design/guide-shots/`, then `powershell -File scripts\make-docs.ps1` and look at the new pages.
5. **Support contact:** replace `<TO BE FILLED>` in `docs/print/` (install guide, user guide, card, both handoffs) and in the NotebookLM sources 05 and 07, then `powershell -File scripts\build-dist.ps1`. Nothing goes to families while it says `<TO BE FILLED>`.
6. **Drive:** follow `dist/DRIVE.md` (if 1.0.0 is already there, replace the files with "Manage versions"; otherwise upload `dist/Koko-Dawa-App`), sharing, the direct link, `dist/link.txt`, `scripts\make-card.ps1` until it says VERIFIED, the WhatsApp message links, the private-window and phone test.
7. **Presentation:** upload `notebooklm/sources/` (01 to 07, in order) to NotebookLM, paste the slide prompt of `notebooklm/prompts.md`, check every slide against `notebooklm/slide-map.md` and `claims-check.md`, place the screenshots by hand; fill the pilot placeholders of source 07 with the hospital.
8. **Try it like a parent:** a non-technical person on a phone of another brand, without help; write down where they get stuck.
9. **Native Arabic review:** `docs/ar-review.md` (second-person table first: `broken_adult_title` "ليك", `forgot_pin` "نسيت"; the 10 strings marked "batch 6"), the Arabic documents in `docs/print/` and the NotebookLM sources.
10. **"avatar no streak" picture:** as a PNG with transparency or on a flat color (section 11).
11. **Hospital decisions:** approval of the name and the logo, who answers families, the sharing channel, the pilot (families, length, dates).
12. Optional: move `app/claude.md` to the project root as `CLAUDE.md`; add `design/fonts/` (still skipped); decide on `docs/final-plan.md` in the repository (it stays ignored).