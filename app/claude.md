# CLAUDE.md - Koko Dawa / كوكو دوا (medicine reminder for the patients of Smouha University Children's Hospital)

This file is the contract: what the app must do and the rules for working on it. It is not a task list and not a history. History (the first autonomous run, its phases and the Phase 0 findings) is in `docs/ARCHIVE.md`; every change to this file is recorded in `docs/CHANGELOG.md`.

## 0. Working loop (read first)

1. At the start of every session and after any context compaction, re-read CLAUDE.md and `docs/PROGRESS.md` (the source of truth for where work stands).
2. Per item: write the checkpoint (section 3) into PROGRESS.md and continue without waiting; implement in small steps; run unit tests, lint, `assembleDebug` and instrumented tests; verify on the emulator with the test matrix (section 14); save evidence in `docs/evidence/`.
3. Fix failures at the root cause. Never weaken or delete a test to make it pass. Never mark something verified that you did not actually run. Mutation-check the important tests (section 4).
4. Update PROGRESS.md (done, verified, not verified, parking lot, risks) and snapshot: local commits on the change request's feature branch (section 2 "GitHub access", `docs/GITHUB.md`) once the repository is set up, else a copy without build folders in `../tracker-snapshots/<name>/`.
5. Decisions: never wait for an answer. Use the defaults (0c); if something is genuinely unspecified, pick the safest option and log it in `docs/DECISIONS.md` (decision, alternatives, why).
6. BLOCKED: write it at the top of PROGRESS.md, then work only on independent items or stop, when a DoD item still fails after 3 serious attempts, a dependency is not approved, a change would destroy or migrate real data in a way not described here, two sections contradict in a way that changes behavior, work would touch anything outside the project folder, the Android SDK or the AVD, or a request conflicts with a Hard rule (section 2) or a safety invariant (PIN, points, streak, persistence) without explicitly overriding it.

The long form of this loop, as used in the first run, is in `docs/ARCHIVE.md`.

### 0a. Change requests

- Requests arrive as prompts. For each request: checkpoint, implement, test, then update the affected contract sections of this file IN PLACE (rewrite them, do not append history), add new scenarios to the test matrix (section 14), and add an entry to `docs/CHANGELOG.md` (date, request, sections edited, before/after of every changed rule, decisions).
- Never edit the Hard rules (section 2) or the safety invariants (PIN in 8, points in 6, streak in 6b, persistence in 5b) unless the request explicitly says so. If a request conflicts with one of them, mark BLOCKED.

### 0b. Commands, devices and hygiene

- Allowed: `gradlew` / `gradlew.bat`, `adb`, `emulator`, `sdkmanager`, `avdmanager`, the SDK's build tools (`apksigner`, `aapt`, `d8`), normal file tools inside the project; for the documents (section 10) headless Microsoft Edge or Chrome and Windows' built-in PDF renderer.
- Forbidden: anything outside the project and the SDK, `git commit --amend`, force, rebase, merging pull requests, any GitHub action beyond section 2 "GitHub access", deleting the AVD, uninstalling system packages, creating the real signing keystore (section 10).
- Two AVDs from the same image and flags. "Pixel_API_34_test" (serial `emulator-5560`, started with `-port 5560`) takes ALL automated work: instrumented tests, mutation scripts, installs, clock, locale, size and sound experiments. "Pixel_API_34" is reserved for the user's manual use and never receives an automated install, uninstall or test run.
- Set `ANDROID_SERIAL` for every adb and gradle command. Every Gradle task that can install or uninstall the app runs through `scripts/run-tests.cmd`, which refuses unless `ANDROID_SERIAL` belongs to the test AVD (checked with `adb emu avd name`) and always passes `leaveApksInstalledAfterRun=true`. A Gradle guard in `app/build.gradle.kts` fails `connected*` and `uninstall*` tasks that were not started by that script or by Android Studio. The helper scripts in `docs/evidence/tools/` refuse any device but the test AVD.
- Physical phones: only the read-only diagnostics of section 2 are allowed. No install, uninstall, `pm clear`, force-stop, input events, `settings put` or Gradle tasks on a physical phone; the user installs builds on phones themselves (`docs/PILOT.md`).
- When you move the emulator clock for a test, restore automatic time right after the test and again at the end. At the end also reset locale, font scale, screen lock, `wm size` and `wm density` to defaults.
- Things that cannot be verified on an emulator (real phone battery managers, real exact-alarm grant behavior, real device performance, how loud a sound really is) stay marked UNVERIFIED (needs real device). Do not claim them.

### 0c. Defaults (never ask)

- Re-reminder interval default 10 minutes (options 5, 10, 15, 30), max 3 repeats.
- Level = floor(points / 100) + 1.
- A dose can be confirmed until 23:59 of its scheduled day.
- Western digits (0-9) in Arabic too.
- App name: Koko Dawa / كوكو دوا is the application label and the launcher label; the hospital's name is a separate string used only where it names the hospital. Section 1.
- PIN is 4 digits (keep the existing length if the code already uses another).
- Language picker on first launch, before PIN creation.
- "Every N days" and "as needed" medicines are out of scope.

## 1. Project goal

Android app (Kotlin) that helps a patient, mostly kids, take medicines on time. applicationId `com.smouha.kidsreminder`, permanent after the first real release (docs/DECISIONS.md D31); the namespace and the Kotlin package stay `com.example.tracker` (no code is renamed for the id).
It was extracted from the Medipulse repo (pill-tracker part only). Everything else from Medipulse is out of scope.

Identity (since 1.1.0, batch 6): the app is Koko Dawa / كوكو دوا, for the patients of Smouha University Children's Hospital (Alexandria), ages 0 to 18 (the age input stays 0 to 120). `app_name` (EN "Koko Dawa", AR "كوكو دوا") is the application label; the launcher activity has no label of its own, so the launcher shows the same name. The hospital's name is its own string (`hospital_name`: EN "Smouha University Children's Hospital", AR "مستشفى سموحة الجامعي للأطفال"), used only where it names the hospital: under the logo on the language picker (both languages) and in the About intro. The launcher icon is the Koko rooster (section 9). The splash shows the launcher icon on `koko_sky` and, on Android 12+, the name as an image rendered from `app_name` in `splash_name_text` by `scripts/render-splash-name.ps1` (Android 9 to 11 show the icon only). The hospital logo is shown on a white tile on the language picker, in the home header and large on About; nothing says or implies that the hospital made, approved or recommends the app. Class names (`PillPalsApp`), the DB file `pill_pals.db`, the namespace, the package and the signing certificate (`CN=Smouha Kids`) keep their old names.

Brand assets: the doctor's originals stay untouched in the project root; exact copies with ASCII names live in `design/brand/` (the source of truth, `docs/assets-report.md`). The app's pictures, sound and video come from those copies only by cropping, scaling, placing and re-encoding (`scripts/make-brand-assets.ps1`, ffmpeg); a character is never redrawn or recolored. Transparency only from a real alpha channel or a flat solid background; a baked-in checkerboard is never used and never keyed: that asset is BLOCKED and its screen keeps its current visual until the user supplies a usable file (now: "avatar no streak").

Distribution: signed APK shared through Google Drive for a doctor's patients. No Play Store, no backend.

Languages: Egyptian Arabic (primary, colloquial) and English, with full RTL support.
Persistence: everything lives in a local Room database, so the app continues exactly where it stopped after being closed, killed or rebooted.

Core features (the contract):
1. Notification + reminder for each medicine.
2. Parent PIN needed to confirm a pill was taken.
3. Reward animation after a confirmed pill, different per age group.
4. Streaks + points system, one per patient.

## 2. Hard rules (read first)

- Local only. No network, no analytics, no `INTERNET` permission. Health data of kids stays on the device.
- Do not add libraries unless asked. Animation is custom (Canvas / View animations, springs from androidx.dynamicanimation, Lottie only as below).
- Approved dependencies: Room (androidx.room) with KSP. AppCompat 1.6+ only if already in the project and needed for in-app language switching. Material Components (latest stable, Material 3 theme), androidx.core:core-splashscreen, androidx.dynamicanimation, Lottie (only with animation files from `design/lottie/`; never downloaded from the internet). Each one recorded in docs/DECISIONS.md with its reason and APK size impact. Anything else: ask first. After adding any dependency, check with aapt that `INTERNET` is still absent and `allowBackup` is still false.
- Do not touch anything outside `app/`, docs and `scripts/` unless asked.
- Devices (details in 0b): automated runs only on the test AVD "Pixel_API_34_test", through `scripts/run-tests.cmd` for any Gradle task that can install or uninstall. The manual AVD "Pixel_API_34" never gets an automated install, uninstall or test run. On a physical phone only read-only diagnostics are allowed (`adb -s <serial>` with `getprop`, `dumpsys notification|audio|alarm|package|usagestats|deviceidle`, `settings get`, `cmd appops get`), and saved output keeps only this app's lines plus audio and Do Not Disturb state: never other apps' data or notification text.
- Git and GitHub: never `git commit --amend`, rebase or force push. Local commits as described in section 0. GitHub only under "GitHub access" below; anything it does not allow stays forbidden.

### GitHub access (batch 7; replaces "never push, never create remotes")

- Only the private repository `Smouha-kids` of the user's GitHub account (`docs/GITHUB.md`; its full address carries a real name, so it is not written in committed files). Nothing else is uploaded anywhere.
- Token: the value of `GIT-HUB-PAT` in `kh.env` (a file, or a folder holding the file), parsed, never sourced. It lives only in the environment of the one command that needs it (for example `GH_TOKEN`) or in a per-command auth header. Never printed, echoed or logged; never written into a file, commit, issue, pull request, release note, `.git/config`, remote URL, credential store, doc or report. After the work `git config --get-regexp .` and `git remote -v` show no token.
- Before anything is staged: the ignore rules (`kh.env`, `*.env`, `keystore.properties`, `*.jks`, `*.keystore`, `local.properties`, build folders, `.gradle`, `dist/`, `docs/evidence/` except `tools/`, device dumps, the raw originals in the project root), each checked with `git check-ignore`; `git log --all -- kh.env` must be empty (else STOP: the user revokes the token); `.gitattributes` (`gradlew` LF, `*.bat` and `*.cmd` CRLF, `gradlew` executable in the index). The repository must be reachable and PRIVATE through the API; public means STOP before any push.
- Pre-push scan before EVERY push, over the full history to be pushed, counts only (never the matched text): the token value, GitHub token patterns (`ghp_`, `gho_`, `github_pat_`), the passwords of `keystore.properties`, keystore files, every excluded path, the real names in text files and pictures, any file over 10 MB. Any hit: STOP, unstage, report. A history that fails is never pushed: a fresh orphan `main` from the clean tree, the old history kept only in a local branch.
- Allowed: fetch, pull, push to `main` (the baseline only) and to a feature branch; open and update pull requests; create and update issues and labels; create a DRAFT release with assets. The APK and the PDFs are release assets, never committed.
- Forbidden: force push; deleting or rewriting branches, tags, releases or the repository; changing repository settings, visibility, collaborators, secrets or webhooks; creating or editing GitHub Actions workflows; merging pull requests; touching any other repository; creating tokens or keys. An auth or permission error stops the work and is reported, never retried in a loop.
- Do not refactor working code for style. Scope control: if you find an adjacent issue, write it under "Parking lot" in `docs/PROGRESS.md` and do not fix it.
- No medical advice in the app. No dosing suggestions. The user types the medicine name and times, the app only reminds.
- No em dashes in any text, UI string, or docs.
- Stop and report if something surprises you and could change scope or correctness.

## 3. Working method

Before writing code for any task, output this checkpoint (short):

```text
Contract:
Must change:
Must not change:
Worst cases:
Likely wrong implementation:
Tests that catch it:
```

After implementing, report:

```text
Changed:
Tests:
Failure modes checked:
Remaining risks:
```

Never say "build passes" or "tests pass" without saying exactly what ran. Say clearly whether it was verified on a real device/emulator or not.
Before declaring a phase done: inspect the diff, confirm only intended files changed, re-read the phase Definition of Done below.
Write the checkpoint into docs/PROGRESS.md and continue (section 0). Do not wait for a reply.

## 4. Test rules

- Pure logic (points, streaks, levels, age group, reminder schedule math, PIN lockout) lives in plain Kotlin classes with no Android dependencies, so it is unit-testable with JUnit.
- Assert exact values, not "non-empty" or "ran without error".
- For every state-changing test: target entity changes, a control entity does NOT change, before-state and after-state are both asserted.
- Mutation check: for each important test, ask "what broken implementation would still pass?" Strengthen the test if one exists.
- DAO, transaction and migration tests are instrumented tests (`androidTest`, in-memory Room) and need the emulator. Pure logic stays in plain JUnit tests.
- Inject a `Clock` (or `() -> Long`) into all time-dependent logic. Never call `System.currentTimeMillis()` directly inside logic classes.

## 5. Domain model (target)

- `Patient`: id, name, ageAtCreation, createdAtEpochDay, gender (MALE / FEMALE, Arabic labels ذكر / أنثى). Gender is required when a new patient is added; patients from before schema v3 have NULL = not set: neutral avatar, editable in the patient dialog, never a nagging prompt. Gender is for visuals only (avatar emoji by age group and gender, from one constants object, used in filter chips, section headers and reward screens); Arabic copy stays gender-neutral (9b) and the same-name rule is unchanged. Current age = ageAtCreation + full years elapsed since createdAt, so a 12 year old becomes a TEEN automatically a year later. Age group is derived, never stored. Age can be edited by the parent.
- `Medicine`: id, patientId (required), name, optional startEpochDay / endEpochDay, active flag. Schedule lives in `MedicineSlot` rows (see section 5c).
- `DoseEvent`: id, medicineId, scheduledDate, scheduledTime, status (PENDING, TAKEN, MISSED), confirmedAt (nullable), pointsAwarded.
- `Rewards` (per patient): totalPoints, currentStreak, bestStreak, lastPerfectDay, awardedStreakMilestones (set).

DECIDED: one phone can hold MULTIPLE patients (e.g. a mother registering her children). One parent PIN per device, but points, streaks, levels and animation style are per patient.
- Patient identity is by `id`, never by name. Two patients may share the same name (twins, same first name). Never look up or merge by name.
- Every notification, confirm screen and animation must show the patient name, so siblings are never confused.
- Deleting a patient deletes only that patient's medicines, doses, alarms and rewards. Other patients stay untouched (needs a control-patient test).
- Each patient gets their own animation style from their own age. Siblings of different ages on the same phone see different styles.

Age groups (change only if the doctor says so):
- CHILD: age 12 and under. Cartoon style: the celebration avatar springing in, confetti, points count-up, streak, the reward's short sound (section 9).
- TEEN: 13 to 18. Lighter version: the celebration avatar springing in, short confetti, points count-up and streak, the reward's short sound.
- ADULT: 19 and over. Minimal: clean checkmark animation, points and streak as text. No cartoon, no sound.

Unresolved (not saved) must be NULL, never an empty string.

## 5b. Persistence (Room database)

DECIDED: all app state lives in a local Room (SQLite) database.

- The DB is the single source of truth. No in-memory-only state for anything that matters: patients, medicines, doses, rewards, streak state, re-reminder counters, "animation already shown" flags, PIN hash, PIN lockout-until timestamp, language choice.
- Alarm and notification receivers read from the DB, never from memory. They must work after process death and after reboot.
- Resume behavior to verify: after the app is killed and reopened, the home screen shows the same state, a running re-reminder sequence continues with the correct remaining count, and a PIN lockout continues with the correct remaining time. Test with process kill (`adb shell am kill <package>`) and "Don't keep activities".
- Half-filled forms are NOT stored in the DB. Use saved instance state so rotation and process death do not wipe the form.
- Schema: `exportSchema = true` and commit the schema JSON. Version 1 is the baseline (no real users yet, no migration from the old storage). From version 2 on, every change needs an explicit `Migration` and its test. Current version 4: v2 `app_settings.alarmSoundEnabled` (default on), v3 `patient.gender` (NULL = not set), v4 `app_settings.celebrationSoundEnabled` (default on; approved by the user in batch 6). NEVER use `fallbackToDestructiveMigration()`: APKs are updated through Drive, and a destructive migration would silently wipe every child's streak.
- Foreign keys ON. Patient -> Medicine -> DoseEvent cascade delete. One Rewards row per patient, cascade delete.
- Unique index on DoseEvent (medicineId, scheduledEpochDay, scheduledMinuteOfDay), so duplicate doses are impossible at the DB level.
- Streak milestone awards table with unique (patientId, streakStartEpochDay, milestone), so a bonus cannot be awarded twice.
- Confirming a dose + awarding points + updating the streak = ONE transaction. A crash in the middle leaves the old state or the new state, never half.
- Doses are created lazily and idempotently (`ensureDosesFor(date)`) on app open, on alarm, and on the daily evaluation. Running it twice creates no duplicates.
- Scheduled date = epoch day, scheduled time = minute of day. Instants (confirmedAt) = epoch millis. Never store scheduled times as UTC instants, because timezone and DST changes would shift doses.
- NULL means "not set". Never use "" or 0 as a placeholder.
- Set `android:allowBackup="false"` explicitly in v1 (children's health data should not go to cloud backup silently). Revisit with an export/import feature later.

## 5c. Schedules and patient assignment

DECIDED: medicines are NOT always daily. Each medicine belongs to exactly one patient and has a flexible schedule.

Model:
- `Medicine.patientId` is NOT NULL with a foreign key. A medicine cannot exist without a patient.
- `MedicineSlot`: medicineId, dayOfWeek (ISO: 1 = Monday ... 7 = Sunday), minuteOfDay. One row per (day, time). A daily medicine is the same times on all 7 days. Different times on different days are allowed (e.g. Saturday 09:00 and Tuesday 21:00). Unique (medicineId, dayOfWeek, minuteOfDay).
- Optional `startEpochDay` and `endEpochDay` on Medicine (course length, e.g. a 7 day antibiotic). NULL end = ongoing. No doses and no alarms outside the range.
- Not in v1: "every N days" and "as needed". Record them in docs/PROGRESS.md parking lot, do not build them.

Add / edit medicine UI:
- Patient: dropdown of existing patients (preselected only when there is exactly one) plus "new patient" inline (name, age). Never silently pick a patient when several exist.
- Schedule presets: "Every day" (pick times) and "Specific days" (pick days, then times). Advanced option: different times per day.
- Day order in the UI follows the locale's first day of the week (Saturday for Arabic/Egypt). Storage stays ISO.
- Validate: patient chosen, at least one slot, no duplicate slot, end date not before start date.

Behavior:
- `ensureDosesFor(date)` creates doses only for slots matching that date's weekday and inside the start/end range. Idempotent.
- Editing a schedule affects the future only. Past DoseEvents never change. Today's PENDING doses whose slot was removed are deleted and their alarms cancelled. TAKEN doses are kept.
- Alarms: schedule only the NEXT occurrence of each slot; when it fires, schedule the next one. Request code derives from the slot id. Deleting a medicine or a slot cancels its alarms.
- Days with no slots are neutral for streak and perfect day (see 6b).
- End of a course (batch 6): a medicine with an end date ends when its FINAL scheduled dose is confirmed: the last dose occurrence inside its start/end range, read from the schedule with the same rule that creates doses (`CourseEnd`): normally the end day's dose; with two doses on the last day only the later one; with a weekday schedule that has no dose on the end day, the last dose before it. No end date: never. A missed final dose never counts; changing the end date moves the end. The course-complete screen (section 9) follows the reward; no data, points or streaks change.
- Use `java.time` (`DayOfWeek`, `LocalDate`); minSdk 28 supports it. Egypt observes DST as far as I know, so test the clock change.

Tests that catch a wrong implementation:
- Monday-only medicine: a dose exists on Monday and none Tuesday to Sunday. Control: a daily medicine has all 7. (Regression: weekday filter ignored, so it fires daily.)
- Weekday off-by-one: assert with fixed dates that Monday = 1 and Sunday = 7.
- Different times per day: each day produces its own time, not the first slot's time.
- Before startEpochDay and after endEpochDay: zero doses and zero alarms.
- Removing a slot today deletes the PENDING dose and its alarm, keeps a TAKEN dose of the same medicine, and leaves other medicines untouched.
- A patient whose only medicine is Monday-only keeps the streak through Tuesday to Sunday, and a missed Monday breaks it.
- A medicine assigned to patient A never appears for patient B.

## 6. Points rules (must match exactly)

- +10 per confirmed pill.
- +5 extra if confirmed within 30 minutes of the scheduled time.
- +20 for a perfect day (all of that day's doses TAKEN). Awarded once per patient per day.
- +50 streak bonus at 3, 7, 14, 30, 60, 100 days. Awarded once per milestone per streak run.
- Level = floor(totalPoints / 100) + 1 (confirm this matches the current code before changing).
- bestStreak never decreases.

Anti-farming invariants (each needs a test):
- Confirming the same DoseEvent twice awards points once.
- Deleting and re-adding a medicine must not create extra points for already-confirmed doses.
- Perfect-day bonus cannot fire twice on the same day.
- Streak milestone cannot fire twice for the same streak.
- A dose can only be confirmed until the end of its scheduled day (23:59 local). After that it becomes MISSED and can no longer be confirmed or earn points. Test at 23:59 vs 00:00 with an injected clock.
- Confirming patient A's dose never changes patient B's points, streak or perfect-day status (target vs control patient).
- Perfect day is computed per patient, not per device.
- A day with zero scheduled doses for a patient is never a perfect day: no +20 and no streak change. Test it.
- Once awarded, the +20 perfect-day bonus is never removed (points are never deducted), even if a medicine with a later dose is added the same day. The on-screen status is derived from current data and may go back to "not complete".

## 6b. Streak rules

DECIDED: a missed day breaks the streak immediately, no grace day.

- A streak day = a day where ALL of that patient's scheduled doses are TAKEN (same condition as a perfect day).
- A day with at least one MISSED dose breaks the streak: currentStreak resets to 0. bestStreak is kept.
- A day with zero scheduled doses for that patient neither extends nor breaks the streak. Test it.
- Doses scheduled before the medicine was created (e.g. medicine added at 14:00, dose at 08:00) are not counted for that day.
- Streak is evaluated per patient. Patient A breaking a streak never touches patient B.
- The app may not be running at midnight, so evaluate on app open and on a daily alarm/worker. Evaluation must be idempotent: running it twice gives the same result.
- If several days were missed while the app was closed, reset once and show ONE streak-broken animation, not one per day.
- Show the streak-broken animation once per break (store an "acknowledged" flag). It must not replay on every app open.
- Streak-broken animation is per age group and must never shame the patient:
  - CHILD: gentle cartoon (sleepy or sad mascot, then an encouraging "let's start again!"), no harsh sounds. The doctor's "avatar no streak" picture is to replace the mascot here (and the TEEN badge) once a usable file exists; it is BLOCKED (only a checkerboard version, section 1), so the current visuals stay.
  - TEEN: short neutral animation with the lost streak count and a "start again" prompt.
  - ADULT: minimal text and icon, shows best streak.
- Streak-broken animation never awards or removes points. Points are never deducted.
- Worst cases to test: month/year boundary, DST change, timezone change, phone clock moved backward (must not create negative streaks or double bonuses), streak milestone bonus not re-awarded after a break and rebuild of the same milestone only when a NEW run reaches it.

## 7. Reminder rules

- Notification text is localized from strings.xml in the language chosen in the app at the time it fires, and always includes the patient name.
- Sound: the reminder is a normal notification (patient name, tap opens that dose) and the app plays the alarm sound itself, because a channel sound alone stayed silent on a real phone on silent (docs/DECISIONS.md D28). At every reminder (first, follow-ups, test reminder) with the parent settings option "Alarm sound" on (PIN gated, default on, stored in the DB) and the dose still pending, a short foreground service (`AlarmSoundService`, type `systemExempted` with `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, allowed because the app holds the exact-alarm permission; started from the exact alarm) plays the phone's default alarm sound (RingtoneManager TYPE_ALARM; the default notification sound when no alarm sound is set) with `AudioAttributes.USAGE_ALARM`, audio focus and looping, for at most 30 seconds. It never changes any volume. `SoundPolicy` decides (pure, unit-tested).
  - The service's notification is the reminder itself, on the silent channel `pill_reminders_ringing` (never a double sound), with a "Stop sound" button that needs no PIN (silencing is harmless; follow-ups still come).
  - The sound stops on: a tap on the reminder (it opens the dose and the reminder goes), the dose confirmed or deleted, "Stop sound", the reminder swiped away, 30 seconds, losing audio focus (a call, another alarm), the service ending. After "Stop sound", 30 seconds or a focus loss the reminder stays in the list, quiet (it does not alert again).
  - One sound at a time: a newer reminder takes the sound over, both reminders stay. A follow-up of a dose that is no longer pending plays nothing.
  - Fallback: if the service, the foreground start, audio focus or the sound cannot start, the reminder goes to the alarm channel `pill_reminders_alarm` (default alarm ringtone, `USAGE_ALARM`, importance high, category alarm) and the reason is logged (tag `SmouhaKidsSound`). Never both, never silent by accident.
  - "Alarm sound" off: channel `pill_reminders_standard` (v1 behavior, default notification sound), no app sound. Channel settings cannot change after creation, so each style has its own id; the v1 channel `pill_reminders` is deleted on start.
  - Do Not Disturb without alarms allowed, or alarm volume 0, still silence it. Parent settings show read-only sound rows without a PIN (`SoundStatus`, unit-tested with a fake phone): alarm volume (warning at 0, button to the sound settings), Do Not Disturb (warning when it is on and alarms are not allowed, button to its settings), battery optimization state (button to its settings), and a one-line note that the alarm sound plays on silent. Settings rows only, no home banner.
  - Whether it is audible on a real phone, and how loud, stays UNVERIFIED until the user runs `docs/PILOT.md`.
- One exact alarm per slot (`AlarmManager.setExactAndAllowWhileIdle`), only the next occurrence scheduled (section 5c).
- On EVERY app launch, re-arm all alarms from the DB (idempotent). Alarms must also be re-armed by the boot, time change, timezone change and app-update receivers.
- Tapping the notification opens the confirm screen for that exact dose.
- Re-reminder interval is selectable: 5, 10, 15 or 30 minutes, max 3 repeats, stops once confirmed.
- Reschedule after reboot (`BOOT_COMPLETED`), time change, timezone change, and app update.
- Editing or deleting a medicine cancels its old alarms. Test that a deleted medicine never fires.
- Handle missing exact-alarm permission (Android 12+) and notification permission (Android 13+) with a clear UI path, not a silent failure.
- Exact alarms: declare `USE_EXACT_ALARM` (auto-granted on API 33+) and `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"`. If exact alarms are still not allowed, show a banner on home with a button to the setting and fall back to `setAndAllowWhileIdle`. If the app is ever published on Google Play, re-check the policy for USE_EXACT_ALARM. A permission granted through adb that does not survive a reboot is expected; do not use it as test evidence.
- Force-stop cancels all alarms and the app cannot prevent it: re-arming on the next launch restores them, and the parent README says so.
- Permission order on first use: language, PIN, first medicine, then the notification permission request with an explanation.
- Worst case to design against: alarm silently never fires (battery optimization on Xiaomi/Samsung/Oppo, Doze). Add a settings screen hint with a button to the battery optimization settings.

## 8. PIN rules

- Parent creates the PIN on first launch, before anything else.
- The PIN is entered only in the PIN sheet (section 9), with its own keypad: never through the system keyboard. The same sheet verifies, creates (first launch, not dismissible there), changes (current PIN, new PIN, the new PIN again) and resets after "Forgot PIN". 4 to 8 digits; an OK key submits (no auto-submit, so a longer PIN is never counted as wrong). Typed digits and the first new PIN are never written to saved state.
- Needed for: confirming a pill (toggle in parent settings), adding/deleting/editing medicines.
- Parent settings open WITHOUT the PIN. Inside settings the PIN is asked every time, per action, for: the "PIN on confirm" toggle, change PIN (current PIN first, then the new one twice, in the same sheet), manage patients and medicines (edit and delete), the alarm sound option, the celebration sound option (batch 6), erase all data (after an "are you sure" step). Free inside settings: language, permission status and fix buttons, battery settings, the sound status rows and their buttons to the phone's sound and Do Not Disturb settings (section 7), test reminder, About, forgot PIN (inside every PIN sheet that asks for the current PIN, still only via the phone's screen lock). The "Stop sound" button of a ringing reminder needs no PIN either. One table (`SettingsAction`) decides; a wrong PIN changes nothing.
- There is no global re-reminder interval (it is per medicine and part of editing a medicine) and no "reset points and streaks" action (points are never deducted, section 6). Adding either needs an explicit request.
- Stored as salted hash, never plain text. Never log the PIN.
- 5 wrong attempts lock for 60 seconds. Lockout must survive app restart (store lock-until timestamp).
- Known limit: a kid can change the phone clock to bypass a time-based lock or cheat streaks. Document it, do not over-engineer it.
- Forgot PIN: allowed, but only the phone owner can reset it. Reset requires the phone's own screen lock (PIN/pattern/password). Use `KeyguardManager.isDeviceSecure` and `createConfirmDeviceCredentialIntent`, which works on all API levels with no library. Use `BiometricPrompt` with `DEVICE_CREDENTIAL` only if minSdk is 30 or higher. After success the user sets a new PIN.
- Resetting the PIN must NOT delete patients, medicines, doses, points or streaks. Test it.
- If the device has no screen lock, the reset option is disabled and the UI tells the parent to set a phone screen lock first.
- Known limit: a child who knows the phone's lock code can reset the PIN. Mention it in the README for parents.

## 9. UI spec

- First screen: empty state with the simple app design, a clear message, and a floating "+" button.
- "+" opens Add Medicine (section 5c): patient picker, medicine name, flexible schedule, optional start/end date, re-reminder interval. Validate: empty name, age out of range (0 to 120), no slots, duplicate slots. Duplicate patient names are allowed.
- Home has filter chips at the top: "All" plus one chip per patient. Two patients with the same name must be distinguishable (show the age or an emoji avatar, e.g. "ياسمين 7").
- "All" shows today's doses grouped by patient sections, each with its own points and streak. A selected patient shows only that patient's doses, points, streak and header ("2 of 3 pills today" is computed for that patient only).
- The filter is view-only: it never changes alarms, doses, points or streaks. Test: select A, assert B's doses are absent from the list and unchanged in the DB.
- The selected filter is saved in the DB settings. If that patient is deleted, fall back to "All".
- Status text such as "Perfect day!" is derived from current data for that patient, never a stuck flag.
- Large tap targets and simple words, because kids use it.
- Supports light mode and a phone rotation without losing state. Follows the system dark mode (night palette); dynamic color is never used.

Design tokens (palette v2 "Koko", `values/colors.xml` and `values-night/colors.xml`, same names; `docs/palette-v2.md` and `.png` are generated from them by `scripts/make-palette-sheet.ps1`):
- From the Koko pictures: primary the rooster orange family (light #A84A0A, the picture's #F38E32 only in containers, confetti and dark mode #FFB37A), secondary the sunny yellow #FDC440 as the secondary container with dark text (the `secondary` token is its ochre tone #7A5700), tertiary the sky blue (#0B6A86, container #D4EEF7); the page a light sky tint #F4FAFD, cards, the PIN sheet and dialogs warm cream #FFFCF6, the reward card white; dark mode on sky-tinted navy surfaces. Every Material 3 color role in the theme points at a token; layouts never use raw hex. Dynamic color is never used.
- Gradient light sky blue #BEE5F4, cream #FFF6E0, soft yellow #FFDD80 (three stops, to bottom-right; a two-stop sky-to-yellow blend turns grey-green) for the home header, the onboarding background, the "Add medicine" button, the PIN badge, the status bar scrim and (90 percent) the celebration backdrop; dark mode deep sky #0B5577, navy #1C2E3D, deep gold #6B4A0E. Text on it uses `on_gradient` (near-black in light, white in dark).
- Launcher icon: adaptive icon, the Koko rooster cropped from `koko.jpg` (`ic_launcher_koko`, every rooster pixel inside the 66dp safe zone, the crop's border faded) over a vertical gradient of its own sky blue (`koko_icon_bg_top` #C3E4F0 to `koko_icon_bg_bottom` #E5F8FF); no monochrome layer (no clean silhouette exists, so themed-icon launchers show the full-color icon); the old capsule icon files are gone. Notification: the small icon stays the capsule, the large icon is the rooster. Same icon in light and dark mode. `IconContrastTest` and `LauncherIconTest` check it.
- No purple, violet or indigo anywhere (resources, Kotlin, manifest, themes, splash, notifications): `scripts/find-purple.ps1` lists any hit.
- Contrast: every text/background pair >= 4.5:1 (large text, icons, borders and the level bar >= 3:1); on the gradient every stop is checked, and translucent boxes blended over each stop; the gradient drawables must paint exactly the checked stops. `ContrastTest` reads the tokens and asserts this for both palettes.
- Shapes: cards, sheets and dialogs 28dp corners; filled, tonal and outlined buttons at least 56dp high with fully rounded ends and no insets (`styles_shapes.xml`, set in the theme), the "Add medicine" button fully rounded; the dose confirm button larger (64dp, 18sp, check icon); card shadows low (1dp, the reward card 4dp) and tinted with `shadow_soft` instead of grey. Exceptions: the PIN keys keep their height-based sizes, text-only buttons keep a 48dp touch target, the small stat boxes keep 16dp.
- Rewards (batch 6): CHILD and TEEN show the celebration avatar (`avatar_celebration`, a rounded picture with its own background, 128dp, 96dp under 640dp height) springing in, with confetti (TEEN shorter), the points count-up and the streak; ADULT keeps its checkmark (calm and minimal).
- Mascot: a capsule character drawn as VectorDrawables (`mascot_happy`, `mascot_sleepy`, colors `mascot_*`, the same in dark mode), 112dp (80dp under 640dp height). CHILD streak ended: sleepy, then happy after 1.5 s; empty state: happy. TEEN streak ended keeps its badge, ADULT its text. Idle motion: 3 slow breaths (4%, 1.6 s in and out, from its feet), then rest (never endless: every scale change is an accessibility event, and an endless stream keeps TalkBack and test tools from seeing the screen settle), stopped earlier when the overlay closes or the view leaves the window, none at all with animations off (`Motion.idle`). Decorative: no content description, the text says it all.
- Type: the system font everywhere. A rounded font for titles, buttons and reward text is used only once `design/fonts/` holds one with its license (not there yet); fonts are never downloaded.
- Semantic colors stay outside the palette: red for missed/overdue/off/wrong PIN (the comb red of the pictures is never a brand color), amber for warnings. Amber lies between the brand's orange and yellow, so a warning row in parent settings is an amber box (`warning_container`) with a 2dp border and a warning triangle (`warning`) and dark text (`on_warning_container`), never amber words alone. Status never relies on color alone (an icon or a word is always there).
- PIN sheet (`PinSheet`, rules in the pure `PinEntry`): a Material 3 modal bottom sheet with a drag handle, 28dp top corners, a gradient circle with a lock, a title naming the action ("Confirm Yasmin's pill", "Add medicine", "Erase all data"), the reason underneath, a step indicator on multi-step flows ("Step 2 of 3: New PIN"), 4 dots that fill with a spring (more dots for a longer PIN) and its own keypad: large round keys 1-9, then delete, 0 and OK, haptic feedback, every key with a content description. The keypad is always left to right with Western digits 0-9, also in Arabic; the rest of the sheet mirrors. Wrong PIN: shake and reject haptic, dots cleared, "N tries left". Lockout: countdown, keys and OK off. While the hash is checked: a progress bar, keys off. Animations off: no shake, every state change is instant, nothing waits. One sheet at a time; a rotation keeps the step and clears the typed digits.
- The other dialogs (patient, erase, yes/no) stay Material 3 dialogs with the sheet's surface color, 28dp corners and title type. Screens change with a fade-through; main buttons give spring press feedback; the reward card springs in. With system animations off (animator scale 0) nothing waits or breaks. Age-group animation rules and tap to close stay.
- Reward time and sound (batch 6, replaces D22's fixed 3 s for rewards): a reward stays max(3 s, min(its sound's length, 6 s)) from the moment the sound starts, with a 0.5 s fade when a longer sound is cut; without a sound, and for every streak-ended screen, 3 s. The sound is `dose_sound`, once, for CHILD and TEEN only, only with the parent setting "Celebration sound" on (PIN-gated, default on, stored in the DB) and only while the reward is on screen; USAGE_GAME (media volume) with ducking audio focus; never a volume change; stopped and released when the reward closes, a new overlay replaces it or the screen stops; never replayed after a rotation. One sound at a time: confirming a ringing dose stops its alarm first and the reward's sound waits for that; while another reminder rings the reward stays silent; an alarm that starts during a reward or a course video silences them before it rings (`SoundPolicy.celebration`, `CelebrationSound`, `CourseEndScreen`). The reminder's alarm sound itself does not change (section 7).
- Course-complete screen (batch 6): after the reward of a course's final dose (section 5c), the "avatar of finish" video (`avatar_end.mp4`, 480x854, 9:16, 6 s; it replaced "avatar of end" in batch 7, its 2 black closing frames trimmed so the held last frame is the smiling child) in a rounded navy card (`course_card` #1E2A44, the video's edge color, white text), the tallest video that leaves room for the text, all within 80 percent of the screen's width and height (portrait, landscape, tablets), the text at least 300dp wide; a close button on a dark patch from the first second; plays once, then holds the last frame with the text below (EN "The recorded schedule for {medicine} is complete. Bravo!", AR "الجدول المسجل لدوا {medicine} خلص. برافو!", never "the treatment is finished"); its own audio (USAGE_MEDIA) following "Celebration sound", muted without an audio track; pauses in the background and goes on where it was, continues after a rotation, released on close; the celebration avatar as a still with the same text if playback fails. No PIN; the close button, a tap outside the card or Back closes it; it never closes by itself and never blocks anything.
- Hospital logo (batch 6): always on a white rounded tile (`logo_tile`, also in dark mode). Language picker: the logo above the hospital's name in both languages (the text unchanged). Home header: a fixed 56dp logo tile instead of the app's name, the screen's heading, described as "Koko Dawa. Smouha University Children's Hospital logo"; today's summary, the stat boxes, the chips and the settings button unchanged. About: the logo large (168dp).

Any screen size:
- Small and large phones, tablets, foldables, landscape, multi-window, font scale up to 2.0, display size changes. No orientation lock; `resizeableActivity` true.
- Content at most 600dp wide, centered on wide screens; dialogs at most 560dp wide; home patient sections in 2 columns from 840dp width (each column at most 600dp); filter chips wrap onto more lines; on short screens (< 640dp high) they scroll away with the header and come back on scroll up; dose cards narrower than 340dp at the current font size put the action under the medicine name. Nothing clipped or overlapping; header text never runs under the status bar. The home header's logo tile keeps its 56dp size at any font scale and never overlaps the settings button (small phones at font 2.0, RTL, landscape: `HospitalLogoUiTest`).
- PIN sheet: at most 480dp wide, centered. The whole keypad fits on small phones (480x800 @240 up to font 2.0) and in landscape: keys of 48, 56 or 72dp by the height left; under 600dp of height no badge and no drag handle, at most 2 lines per text, the step label instead of the message on multi-step flows; "Forgot PIN" under the keypad (beside it in landscape); landscape side by side; anything left over scrolls, nothing is cut.
- The mascot pictures and the emoji live in one constants place (`Mascots`), not scattered literals.
- About screen (from parent settings, no PIN): the hospital logo large, the app's name, the version read from the installed package, a short text naming who the app is for, the disclaimer (a reminder tool only, no medical advice or doses, the doctor's instructions come first; gender-neutral in Arabic) and that the data stays on this phone with no internet access. No contact details unless the hospital gives them; no endorsement wording anywhere (`BrandTextTest`).

## 9b. Language: Egyptian Arabic first, English second

DECIDED: Arabic in the Egyptian dialect (colloquial, NOT formal Modern Standard Arabic) is the primary language. English is second. Full RTL.

Setup:
- `res/values/strings.xml` = English default. `res/values-ar/strings.xml` = Egyptian Arabic. One Arabic folder, so every Arabic-locale phone gets it, not only ar-EG.
- Zero hardcoded user-facing strings in Kotlin or layouts. This includes notification channel names, notification text, permission explanations, validation errors and content descriptions.
- Lint as errors: `MissingTranslation`, `HardcodedText`, `RtlHardcoded`, `RtlSymmetry`.
- New user-facing strings go into BOTH strings.xml files in the same change, and the Arabic layout is checked in RTL. Never leave translation for the end.
- `android:supportsRtl="true"`. Use start/end, never left/right. Verify every screen mirrored, including the animations and the "+" button position.
- Language choice: first launch shows a two-button picker (العربية / English) before PIN creation, saved in the DB. Parent settings has a switch. Apply with `AppCompatDelegate.setApplicationLocales` if AppCompat 1.6+ is already in the project, otherwise report before adding anything. Add `res/xml/locales_config.xml` for the Android 13+ per-app language setting.

Writing rules for Arabic copy:
- Egyptian colloquial, short, warm: "الدوا" not "الدواء", "النهاردة" not "اليوم", "ورا بعض" not "متتالية".
- Gender-neutral. The app does not know if the patient is a boy or a girl. Use first person ("خدت الدوا"), passive ("لسه متاخدش"), or nouns on buttons ("حفظ", "إضافة", "مسح"). Avoid second-person verbs and imperatives ("اضغط", "ادخل"). If a sentence cannot be made neutral, rewrite it.
- Never build sentences by string concatenation. Use full strings with positional args (`%1$s`), because Arabic word order differs.
- Plurals: use `<plurals>` with Arabic categories (zero, one, two, few, many, other) for days and points. Correct Egyptian forms: يوم، يومين، 3 أيام ... 10 أيام، 11 يوم.
- Digits: Western digits (0-9) everywhere, consistent across the app.
- Time: use system time formatting (ص / م).
- Patient and medicine names can be Arabic, English or mixed. Validation must accept Arabic letters (no ASCII-only regex). Latin medicine names inside Arabic sentences must not scramble punctuation or order: use `BidiFormatter` or `textDirection="firstStrong"` and test with "Panadol" and "Augmentin 1g".
- Layouts must survive long Arabic strings and a larger system font scale without cutting off buttons.
- Egyptian copy must be reviewed by a native speaker before release. The review sheet `docs/ar-review.md` (every key in English and Arabic, every plural form, the screen, second-person hits first, strings new or changed since a baseline) is generated by `scripts/ar-review.ps1`; run it again after any string change and never edit the sheet by hand.

Starter glossary (a starting point, keep the tone):

| English | Egyptian Arabic |
|---|---|
| Add medicine | إضافة دوا |
| Patient name | اسم المريض |
| Age | السن |
| Medicine name | اسم الدوا |
| Times | المواعيد |
| Save / Cancel / Delete | حفظ / إلغاء / مسح |
| Empty state | لسه مفيش أدوية هنا! علامة (+) بتضيف أول دوا. |
| Reminder title | معاد الدوا |
| Reminder body | {patient}: معاد {medicine} |
| Kid reminder | يلا يا {patient}، معاد {medicine}! |
| Re-reminder | {medicine} لسه متاخدش |
| Confirm button | خدت الدوا |
| PIN prompt | الرقم السري للأهل |
| Wrong PIN | الرقم السري غلط |
| Locked | محاولات غلط كتير، التطبيق مقفول دقيقة |
| Forgot PIN | نسيت الرقم السري |
| Change PIN | تغيير الرقم السري |
| Parent settings | إعدادات الأهل |
| Test reminder | إرسال تذكير تجريبي |
| Success | برافو! |
| Streak | 5 أيام ورا بعض |
| Level | المستوى 3 |
| Perfect day | يوم مثالي! كل أدوية النهاردة اتاخدت |
| Streak broken (child) | ولا يهمك، نبدأ من جديد! |
| Streak broken (adult) | السلسلة وقفت. أطول سلسلة ليك: {n} يوم |

## 10. Release and distribution

Identity and signing
- The applicationId `com.smouha.kidsreminder` is permanent since the first signed release 1.0.0 (versionCode 1, 2026-10-18; D31, D34). Android knows the installed app by it: another id is another app, with no update and no data.
- Release builds are signed with the real key through `keystore.properties` (project root, gitignored, never copied into snapshots), which points at the key outside the project (`C:\Users\OPT\smouhakids-signing\smouhakids-release.jks`, alias `smouhakids`, PKCS12), created and backed up twice by the user. Never create, copy, print or change the real keystore or its password; no script opens `keystore.properties` (Gradle reads it). To verify a signing setup a throwaway keystore in a temp folder is allowed; delete it afterwards. `assembleRelease` fails with a clear message when `keystore.properties` is missing. If the key is lost, installed apps can never be updated again (families would uninstall and lose everything).
- Release certificate `CN=Smouha Kids, O=Smouha Kids, C=EG`, SHA-256 `FF:0A:9A:1B:D4:09:D3:85:03:76:FF:A2:C8:3D:0F:27:E2:39:4D:7B:2A:B7:3E:D6:9E:16:14:3E:FF:8A:E8:7A` (recorded in docs/RELEASE.md). Every release carries exactly this certificate.

Versions and updates
- Signed release APK (never a debug one) with app name, icon, versionName and versionCode; parent settings show both at the bottom. Every APK shared through Drive or handed to a pilot has a higher versionCode than the installed one. The debug builds before 1.0.0 (versionCode 1 to 3, debug key) can never update into a release: a test phone uninstalls them first.
- An update is the newer APK with the same key installed over the old one; it keeps every row. A schema change needs a `Migration` and its test; never `fallbackToDestructiveMigration()`.
- 1.1.0 (versionCode 4, batch 6) is the Koko Dawa identity and schema v4; its release pack (`dist/Koko-Dawa-App/`, the PDFs, the card, the WhatsApp text, the presentation pack in `notebooklm/`) was rebuilt in batch 7, and the 1.0.0 pack is kept outside the project in `../tracker-snapshots/dist-1.0.0/`. `scripts/verify-dist.ps1` reads the expected names from `strings.xml`.

Checks before sharing a version (docs/RELEASE.md)
- The full suite on the test AVD. `scripts/build-release.ps1`: `assembleRelease` with the real key, the file `Koko-Dawa-<versionName>.apk` (ASCII name), then `scripts/verify-dist.ps1`: signed (v2 or v3), one signer, not the debug certificate, the recorded certificate, package, versions, minSdk 28, the app and launcher names in English and Arabic, no `INTERNET`, `allowBackup` false, not debuggable, size; it writes `verify-dist.txt` and `SHA256.txt`.
- `scripts/update-test.ps1 -NextApk <new>` on the test AVD: the previous APK with data, the new one with `adb install -r`; the version goes up, the first install time stays, every table row is identical (the PIN hash compared, never printed), the patients are on home, the reminders are booked; the same APK re-signed with another key is refused (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) and changes nothing.
- The main flow from the release APK on the test AVD: first run, add a child, add a medicine, a reminder in the background with the app's alarm sound, tap, PIN confirm, reward, points stored.
- Manual, by the user: the real phone pilot on the signed APK (`docs/PILOT.md` 0a, starting with the installed versionCode), the native Arabic review (`docs/ar-review.md`).

Distribution (Google Drive, by hand only)
- `scripts/build-dist.ps1` builds and checks `dist/Koko-Dawa-App/` exactly as it goes onto Drive: `للأهل` (only `1_التطبيق/Koko-Dawa-<v>.apk`, `2_طريقة_التثبيت.pdf`, `3_طريقة_الاستخدام.pdf`, and `4_فيديو_التثبيت.mp4` when the user adds the video; anyone with the link, viewer), `للمستشفى` (handoff in Arabic and English, `SHA256.txt`, release notes, `verify-dist.txt`; restricted), `للطباعة` (the A5 card; restricted), `للمطور` (README, screenshots, document sources, nothing sensitive; restricted). No signing file, password, evidence or device dump anywhere in `dist`.
- Upload, sharing, the direct link `https://drive.google.com/uc?export=download&id=FILE_ID`, `dist/link.txt` and the private-window test: `dist/DRIVE.md`. Changed files are replaced with "Manage versions", so the links stay. Nothing is uploaded by a script.

Documents (docs/print/, D35)
- Arabic first, simple Egyptian wording, short sentences, one action per step; guardians addressed in the plural imperative (اضغطوا، افتحوا), which is gender-neutral (the app's own copy keeps 9b: no imperatives); "ملف التطبيق", never "APK" for parents; no medical advice; no claim that the hospital approved the app; fictional names only; support contact `<TO BE FILLED>` until the hospital names one, and never shared like that.
- PDFs from right-to-left HTML with headless Edge (`scripts/make-docs.ps1`); every page rendered back to PNG from the PDF (`scripts/pdf-to-png.ps1`) and looked at. Install guide 2 to 3 pages, user guide 4 to 5, handoff 2 per language. Screens of the install itself only from a real phone (`design/guide-shots/`, `docs/guide-shots-needed.md`), never drawn; a marked placeholder until they exist. The A5 card (`scripts/make-card.ps1`) carries the direct link from `dist/link.txt` as a QR code, read back from the rendered page; without a link it is a marked draft.
- README keeps the parent install steps: install from unknown sources, Play Protect, notification permission, exact alarms, battery settings.

GitHub
- The private repository `Smouha-kids` (no license: all rights reserved until decided), used only as section 2 "GitHub access" allows; the flow is in `docs/GITHUB.md`: a baseline on `main`, then per change request a feature branch, small Conventional Commits, the pre-push scan, a pull request to `main` that the user merges, a DRAFT release with `Koko-Dawa-<v>.apk`, `SHA256.txt` and the two parents' PDFs attached, and issues for the manual steps, UNVERIFIED items and BLOCKED assets (labels `manual`, `unverified`, `blocked`, `doctor-approval`). The repository leaves out the signing files, `kh.env`, `local.properties`, build folders, `dist/`, `docs/evidence/` (except `tools/`), device dumps, `dist/link.txt`, `scripts/scan-names.txt`, `docs/final-plan.md` and the raw originals in the project root.

## 11. Environment notes

- `git` and `gh` were not on PATH on the Windows machine (checked again 2026-10-19), and `kh.env` was not there: GitHub access (section 2) waits for the user to install Git for Windows and add `kh.env` (`docs/GITHUB.md`).
- Python is not installed (the Windows `python` command is only the Store stub). Tools that need more than PowerShell are JVM tools built with the project's Gradle wrapper (the QR tool `scripts/qr`) or the SDK.
- Windows PowerShell 5.1 pitfalls met in this project: a script with Arabic text needs a UTF-8 BOM; reading the output of a command that starts a Gradle daemon can wait forever (start it as a process with a log file); a started process reports its exit code only after its handle was read; the old SDK tools (`aapt`, `apksigner`) cannot open a path with Arabic letters; double quotes inside an `adb shell "..."` argument get lost (push a SQL file and run `sqlite3 db < file` instead); `@(@(a, b))` flattens to one array (write `@(, @(a, b))`).
- The test AVD froze twice (window not responding, adb silent) right after a PIN check while the reward animated, with host GPU rendering and a minimized window; started with `-gpu swiftshader_indirect` it ran every suite since. Under that software rendering the main thread can stay busy for a whole animation or video, so UI tests must not catch a running animation by timing (Espresso and `ActivityScenario.onActivity` wait for an idle main thread first): they read the animation's state from the view, or use `runOnMainSync`.
- Unit tests that read resource files declare `src/main/res` as an input of the test task (`app/build.gradle.kts`); without it Gradle reported a resource-only change as "up to date" with the old result.
- `dumpsys audio` keeps a timestamped player event log ("playback activity as reported through PlayerBase"): it shows overlaps of a few milliseconds that sampling the player list cannot see (`CelebrationSoundTest`).
- Source of the original: the earlier project Medipulse-App (only the pill-tracker logic was ported); its link is left out because the account name in it is a real name (the pre-push scan of section 10).
- Known problem in the original repo: add-medicine screen code did not match its layout and referenced missing colors. Do not copy those files back.

## 12. Open questions (use the defaults in section 0c and log in docs/DECISIONS.md)

Decided: multiple patients per phone, PIN reset via the phone's screen lock, missed day breaks streak with an animation, flexible weekday schedules with optional start/end dates, home filter by patient.

No open blockers. "Every N days" and "as needed" medicines are out of scope for v1 (parking lot only).

## 14. Test matrix (what "tried everything" means)

Each line is a scenario to run on the emulator (or a unit/instrumented test where noted). Record pass/fail and the evidence path in PROGRESS.md. Use fixed test patients: a child (age 7), a teen (age 15), an adult (age 40), plus two patients with the same name.

Reminders
- Alarm fires at the slot time and the notification shows the patient name.
- Weekday-only medicine fires on its day and NOT on other days (control: daily medicine fires every day). Use the emulator clock, and unit tests with an injected clock.
- Medicine outside its start/end dates: no dose, no alarm.
- Re-reminder at the chosen interval, exactly 3 times, then stops; confirming stops it earlier.
- Tapping the notification opens the confirm screen for that exact dose (cold start and warm start).
- Edit a slot: the old alarm is gone, the new one fires. Delete a medicine: its alarm never fires.
- Reboot, force-stop then relaunch, time change, timezone change: alarms are re-armed (section 7).
- Notification permission denied and exact alarm permission missing: banner shown, no crash, fallback behavior as specified.
- Alarm sound, app playback (instrumented `AlarmSoundTest` on the test AVD; "playing" = a started `USAGE_ALARM` player of the app's uid in `dumpsys audio`): with ringer SILENT and with VIBRATE a reminder plays within 3 seconds and no other alarm-stream player exists (one sound); it stops on a tap in the notification shade, on confirm, on "Stop sound", on dismiss and by itself after 30 seconds (still playing at 25 s); after "Stop sound" and 30 s the reminder stays without the button. Alarm sound off: no app player, standard channel. Two doses due together: one player, two reminders, only the newer one has "Stop sound". A follow-up of a taken dose: no player, no reminder. Service start refused and `startForeground` refused (forced by a test hook): the alarm channel rings (a system player) and the app plays nothing.
- Alarm sound on the emulator with the app in the background: a real exact alarm starts the service (ActivityManager "Background started FGS: Allowed", code `ALARM_MANAGER_WHILE_IDLE`, type `0x400`), the player starts within a second with the ringer in VIBRATE and stops after 30 s (`docs/evidence/batch2/`).
- Channels (`NotificationChannelTest`): ringing (no sound), alarm (`usage=USAGE_ALARM`, sound `alarm_alert`, importance 4) and standard channels; the v1 channel is deleted after an upgrade over an installed v1 app (data intact, DB migrated); each style builds its own channel and buttons. Fire, 3 follow-ups, tap, confirm stops, reboot re-arm all still work.
- `SoundPolicyTest`: setting off = no playback, dose taken = no playback, notifications off = nothing, 30 second cap, every stop reason, alarm sound before notification sound. Mutation checks: the TAKEN check removed, the 30 second cap removed (policy and service), the setting ignored (policy and announce).
- Sound status rows (`SoundStatusTest`, fake phone): alarm volume 0 warns (1 does not), Do Not Disturb warns only when it blocks alarms (total silence, priority without alarms), battery optimization warns, which rows show a button, the silent note only with the alarm sound on, constants equal Android's.
- Real phone: audibility on silent and vibrate, Do Not Disturb, alarm volume 0, Samsung "Sleeping apps", swipe from recents, reboot: `docs/PILOT.md`. UNVERIFIED until the user runs it.
- DST: unit test with `Africa/Cairo` and a fixed clock across a clock change (an emulator cannot do this reliably).

PIN
- Create on first launch. Wrong PIN never confirms a dose. 5 wrong tries lock for 60 seconds, the lockout survives a process kill.
- Gates: confirm (when toggled on), add, delete, edit. Toggle off: confirm no longer asks, other gates still ask.
- About opens from parent settings with no PIN prompt and shows the installed version and the disclaimer (`SettingsGatesUiTest`; screenshots EN/AR at font 1.0 and 2.0).
- Settings open from home with no PIN prompt. Inside settings, a wrong PIN on each gated action (PIN-on-confirm toggle, change PIN, manage, alarm sound, erase) changes nothing; language, permission buttons and test reminder work with no PIN prompt (instrumented `SettingsGatesUiTest`, in-memory DB).
- Reset via the phone screen lock (set one with `adb shell locksettings set-pin`), data intact after reset. No screen lock: the option is disabled with the explanation.
- PIN sheet (instrumented `PinSheetUiTest`, keypad taps): a wrong PIN says "4 tries left", clears the dots and changes nothing; 5 wrong tries show the countdown and turn the keys and OK off (a tap types nothing); a rotation keeps exactly one sheet, the step and no typed digits, and the flow then finishes; the saved state of the screen contains neither the typed digits nor the first new PIN; a second sheet is refused; in Arabic the sheet mirrors and the keys read 1-2-3 from left to right; with animations off no shake and no waiting; the first launch creates the PIN on the keypad with no text field, the sheet cannot be dismissed there. `PinEntryTest` (JUnit) covers the input state machine. Mutation checks: the repeat not compared, keys during the lockout, digits kept on rotation, digits in saved state, a mirrored keypad, a second sheet.
- Screenshots of the sheet: English and Arabic, light and dark, the 4 screen sizes at font 1.0 / 1.5 / 2.0, landscape (small at 2.0 included), wrong PIN, lockout, step 2, confirm pill, first launch.

Data and persistence
- Kill, force-stop, reboot, rotation: same state, running re-reminders continue with the right count.
- Two patients with the same name stay independent. Deleting one leaves the other's data exact (control patient).
- Gender: a new patient cannot be saved without it (error shown at the picker); an existing patient without it shows the neutral avatar and no prompt; editing can set it, editing without choosing keeps it; a control patient never changes. Avatar per (age group, gender) in chips, headers and the reward screen, in English and Arabic.
- Migrations: a realistic v1 DB (both patients, medicines, slots, doses in every state, rewards, awards, settings) migrates to the latest version with every row identical, new columns at their documented values (alarm sound on, gender NULL, celebration sound on); v2 to v3 and v3 to v4 likewise (v3 data with the alarm sound off and a gender set keeps both); a fresh latest DB matches the exported schema. Installing over a real installed older version keeps the data (check on the emulator).
- Home filter: select A, B's doses are absent and unchanged in the DB. The filter survives a restart. Deleting the selected patient falls back to "All".
- Instrumented tests: unique dose index, cascade deletes, transaction rollback on a forced failure, schema export, migration test harness ready for v2.

Rewards and streaks (unit tests with an injected clock, plus on-device spot checks)
- Age groups (`AgesTest`): 12 is CHILD, 13 TEEN, 18 TEEN, 19 ADULT; a 12 year old is TEEN and an 18 year old ADULT exactly one year after being added (not a day earlier). Mutation check: the TEEN limit back to 17.
- Exact points: +10, +15 on time, late and on time on a perfect day, milestone bonuses at 3, 7, 14, 30, 60, 100 once per run.
- Confirm the same dose twice: points once. Delete and re-add a medicine: no extra points.
- Perfect day +20 once. A day with no scheduled doses: neutral, never perfect, never breaks the streak.
- Missed day breaks the streak, bestStreak kept, ONE streak-broken animation even after several days away, shown once.
- Patient A's actions never change patient B.
- Dose at 23:59 confirmable, at 00:00 it is MISSED and cannot be confirmed.
- Phone clock moved backward: no negative streaks, no double bonuses.

Animations
- Reward animation for child, teen, adult (screenshots of each). Streak-broken animation for each age group.
- Animation never blocks the next action, survives rotation, and mirrors correctly in RTL.
- With window, transition and animator scales at 0: the reward still shows and closes by itself after 3 s, screens open, the PIN prompt appears, no crash.
- Avatar and mascot (`CelebrationUiTest` on the overlay, `MascotTest` JUnit): CHILD and TEEN rewards show exactly the celebration avatar picture (no mascot, no emoji), ADULT the checkmark; with animations off the avatar is at full size at once; CHILD streak ended sleepy then happy (no avatar there); a new overlay cancels the pending wake-up; the idle motion stops when the overlay closes, the empty-state mascot's idle motion was started, lasts exactly 3 breaths (finite) and ends at rest, and it never runs (mascot exactly at rest) with animations off; two different vector expressions, `mascot_*` tokens only, outline 4.5:1 on both body halves, the body 3:1 against the card in light and dark. Mutation checks: the wake-up not cancelled, the idle motion with animations off, the idle motion left running, an endless idle motion, a missing idle motion. Screenshots: rewards per age group, streak ended (sleepy and happy), empty state in English and Arabic, light and dark, small screen at font 2.0.
- Reward sound (`SoundPolicyTest` JUnit; `CelebrationSoundTest` on the test AVD, "playing" = a started USAGE_GAME player of the app in `dumpsys audio`): only CHILD and TEEN, only with the setting on, only on screen, never with an alarm (the confirmed dose's alarm first, silent while another reminder rings); the reward's time 3/3/4.5/6/6 s for no sound, 2.77, 4.5, 6 and 60 s, the fade only when cut. On the AVD: one player that ends by itself and is released; none with the setting off or for an adult; a tap stops it at once; no replay after a rotation; with animations off it still plays once and the reward closes; the alarm of the confirmed dose stops before the reward's player starts and an alarm during a reward stops it first, both checked to the millisecond in the player event log. Settings: a wrong PIN leaves "Celebration sound" on, the right PIN turns it off and leaves the alarm sound (`SettingsGatesUiTest`). Media volume unchanged before and after. Audibility on a real phone: UNVERIFIED (PILOT).
- Course complete (`CourseEndTest` JUnit, `CourseEndUiTest` on the test AVD): the final dose of a daily course on the end day; of two doses on the last day only the second; a weekday course whose last dose is before the end date; no end date never; an earlier dose never; an extended end date moves the end; a missed final dose never; another medicine and another patient unaffected. On the AVD: confirming the final dose shows the screen once, after the reward, with the exact text, the close button and the video playing; closing releases the player; confirming again shows nothing; a course that goes on shows nothing; the setting off mutes the video; a forced playback failure shows the avatar still with the text; a rotation keeps the screen and its video, the card within 80 percent and the video at 9:16 in portrait and landscape. Frames at the start, middle and end, landscape and after closing, in English and Arabic, light and dark.

Design
- `ContrastTest` passes for light and dark (every gradient stop, glass over every stop, the warning box, 3:1 for outlines, the warning icon and the level bar, the course card and its close icon over any frame; the gradient drawables paint exactly the checked stops); mutation checks (a lightened token, a lightened gradient center, a pale warning text, a drawable without its center stop) make it fail. `scripts/make-palette-sheet.ps1` regenerates `docs/palette-v2.md` and `.png` from the tokens.
- Hospital logo (`HospitalLogoUiTest` on the test AVD, `BrandTextTest` JUnit): the language picker shows exactly the logo bitmap on a white tile and both hospital names unchanged, no capsule emoji; the home header's logo is the heading with its description, no app-name text, the summary under it, logo and settings button inside the header and apart in English and, at 480x800 with font 2.0, in Arabic portrait and landscape (logo on the right); About shows the logo at 160dp or more with the name, disclaimer and data texts; no English or Arabic text claims an endorsement; the tile is white with no dark value. Screenshots: home and About in both languages, light and dark, font 1.0 and 1.5, small at font 2.0, landscape, the language picker light and dark.
- Warning rows: screenshots of parent settings with the alarm volume, Do Not Disturb and battery rows as warning boxes (Do Not Disturb set to total silence for the shot, then off again).
- Home, settings, dialogs, onboarding and the reward overlay in light and dark mode, English and Arabic.
- After a dependency change: aapt shows no `INTERNET`, `allowBackup` 0x0.
- Launcher icon: `IconContrastTest` (the adaptive icons are the rooster over the sampled sky gradient with no monochrome layer, the gradient tokens are light sky, the old capsule files are gone, the splash uses the launcher icon on `koko_sky` and its name image is in `splash_name_text` at 4.5:1) with mutation checks; `LauncherIconTest` (every rooster pixel within the 66dp safe zone, no themed layer, circle and squircle renders show the rooster on the sky blue); screenshots on the test AVD: the icon on light and dark wallpaper, circle and squircle, splash in both languages. `scripts/find-purple.ps1` finds no purple.

Screen sizes (adb `wm size` / `wm density` on the AVD; reset both and the font scale at the end)
- 480x800 @240 (small), 1080x2400 @420 (normal), 1600x2560 @320 (tablet like), 2208x1840 @420 (unfolded like), each at font scale 1.0, 1.5 and 2.0 in English, and at 1.5 in Arabic: all filter chips visible (wrapped), no squeezed or clipped text, the action under the name on narrow cards, two columns on the unfolded size, content and dialogs not wider than their limits, header text never under the status bar when scrolled. `ResponsiveTest` covers the rules.
- Rotation with the PIN sheet open keeps exactly one sheet.

Language and layout
- App identity: `LocalizationTest` checks Koko Dawa / كوكو دوا as the application label, that the launcher activity has no label of its own (it shows the same name), and `hospital_name` in both languages as a separate string. Screenshots: launcher label on light and dark wallpaper, splash in both languages (Android 12+), the home header with the logo, the language picker with the logo and both hospital names.
- First-run language picker (shows the full name in Arabic and English). Switch in parent settings. Android per-app language entry works and wins over the stored choice, also when the app starts without an activity (Application.onCreate, receivers).
- Every screen in Arabic and English (screenshots), RTL mirrored, no truncated buttons at font scale 1.5.
- Plurals for 1, 2, 3, 11 days and points in Arabic. Arabic patient name accepted. "Panadol" and "Augmentin 1g" inside Arabic sentences keep correct order.
- Portrait and landscape: no overlapping controls (for example the "Add medicine" button never covers a row's buttons).
- Lint with MissingTranslation, HardcodedText, RtlHardcoded, RtlSymmetry as errors.
- `scripts/ar-review.ps1` regenerates `docs/ar-review.md` with no Arabic missing; its second-person table is read before a release.

Documentation screenshots
- `scripts/seed-demo.ps1` puts FICTIONAL patients and medicines on the test AVD only (never real names; options: Arabic names, a just-ended streak, empty). `scripts/screenshots.ps1` takes `docs/screenshots/`: home, add patient, PIN sheet, reward (child, teen, adult), streak ended, About, in Arabic and English, light and dark, font 1.0 and 1.5, and home on the 4 screen sizes, from the installed build, then resets size, density, font, dark mode and app language. Every shot is checked by eye in `docs/screenshots/overview/` before it is called verified.

Release
- `scripts/build-release.ps1` with the real key, `scripts/verify-dist.ps1`: every check passes (signature v2, one signer, the recorded certificate and not the debug one, package and versions, minSdk 28, app and launcher names in English and Arabic, no `INTERNET`, `allowBackup` 0x0, not debuggable, size). Mutation-like checks: a debug-signed APK and an APK with another certificate fail verify-dist.
- Update test (`scripts/update-test.ps1`): the next version with the same key over the release with data keeps every row, the first install time and the reminders; the same APK with another key is refused and changes nothing. For 1.1.0 over 1.0.0 the dump compares the columns both versions have and reports the new column's value (the celebration sound on), never the PIN hash.
- The main flow from the release APK on the test AVD (first run, child, medicine, background reminder with the app's alarm sound, tap opens the dose, PIN, reward, points in the DB).

Documents and distribution
- `scripts/make-docs.ps1`: install guide 2 to 3 pages, user guide 4 to 5, handoff 2 per language; every page rendered from the PDF and looked at (letters joined, right to left, digits, English names not split, nothing cut off). `scripts/make-card.ps1`: one A5 page, the QR code read back from the rendered page equals `dist/link.txt`'s direct link (with a test link until the real one exists). A text scan of the parents' documents: no singular imperative, no "APK", no em dash.
- `scripts/build-dist.ps1`: the Drive tree, the parents' folder holds only the app, the two guides and the video, no secret or evidence file in `dist`.
- `scripts/scan-names.ps1` before a push: no real test name in a file the repository would contain (counts only, never the matched text).
- The presentation pack (`notebooklm/`): every source in Arabic under 1500 words with no file paths or code; every claim in `claims-check.md` with its evidence and status; screenshots named by slide and composites (`scripts/make-composites.ps1`) looked at; `video/flow.mp4` 30 to 45 s without audio.
- GitHub (section 2): every ignore rule checked with `git check-ignore`, `kh.env` absent from the history, the repository private through the API, the pre-push scan over the full history with zero hits, no token in `git config` or `git remote -v`; after a push the branch, the pull request and the draft release with its 4 assets exist and the APK's size matches.
