# ARCHIVE: history moved out of CLAUDE.md

Moved on 2026-10-16 (change request batch 1, item 0). Text below is verbatim from CLAUDE.md as it was; the rules it contains stay in force through CLAUDE.md (see docs/CHANGELOG.md for where each one now lives).

## 0. Autonomous run protocol (read first)

The user starts a long unattended session and reviews later. Work through the phases in order without waiting for replies. Your docs are the product of the run.

Resume rule: at the start of every session and after any context compaction, re-read CLAUDE.md and docs/PROGRESS.md, then continue from the current phase. PROGRESS.md is the source of truth for where you are.

Loop per phase:
1. Write the checkpoint (section 3 format) into docs/PROGRESS.md. Do not wait for approval, proceed.
2. Implement in small steps.
3. Run unit tests, lint and `assembleDebug`, then verify on the emulator using the test matrix (section 14). Save evidence in `docs/evidence/phase-N/` (screenshots via `adb exec-out screencap -p`, `dumpsys alarm`, `logcat` excerpts, `uiautomator dump`).
4. Fix failures at the root cause. Never weaken or delete a test to make it pass. Never mark something verified that you did not actually run.
5. Mutation-check the important tests (section 4).
6. Update PROGRESS.md (done, verified, not verified, parking lot, risks), snapshot the work, move to the next phase.

Decisions: never wait for an answer. Use the defaults below. If something is genuinely unspecified, pick the safest option, log it in `docs/DECISIONS.md` (decision, alternatives, why), and continue.

Stop and write "BLOCKED" at the top of PROGRESS.md (then work only on independent items, or end the run) when:
- a DoD item still fails after 3 serious attempts
- you need a dependency that is not approved
- a change would destroy or migrate real data in a way not described here
- two sections of this file contradict each other in a way that changes behavior
- you would need to touch anything outside the project folder, the Android SDK, or the AVD

Version control: git may not be installed. If it is, work on a local branch `autonomous-run` with one local commit per finished phase (Conventional Commits). Never push, amend, force, rebase, merge, or touch remotes. If git is not available, copy the project without build folders to `../tracker-snapshots/phase-N/` at the end of each phase.

Commands:
- Allowed: `gradlew` / `gradlew.bat`, `adb`, `emulator`, `sdkmanager`, `avdmanager`, normal file tools inside the project.
- Forbidden: anything outside the project and the SDK, `git push`, deleting the AVD, uninstalling system packages, creating the real signing keystore (see Phase 5).

Emulator hygiene: use the AVD "Pixel_API_34". When you move the emulator clock for a test, restore automatic time right after the test and again at the end of the run. At the end, also reset locale, font scale and screen lock to defaults.

Things that cannot be verified on an emulator (real phone battery managers, real exact-alarm grant behavior, real device performance) stay marked UNVERIFIED (needs real device). Do not claim them.

Defaults (never ask):
- Re-reminder interval default 10 minutes (options 5, 10, 15, 30), max 3 repeats.
- Level = floor(points / 100) + 1.
- A dose can be confirmed until 23:59 of its scheduled day.
- Western digits (0-9) in Arabic too.
- App name stays "Pill Pals" in both languages.
- PIN is 4 digits (keep the existing length if the code already uses another).
- Language picker on first launch, before PIN creation.
- "Every N days" and "as needed" medicines are out of scope.

Final deliverable of the run: `docs/FINAL_REPORT.md` with: each feature as verified (with evidence path) or unverified, test counts and what they actually cover, decisions taken, parking lot, remaining risks, and the manual steps left for the user (keystore, real phone test, native Arabic review, push to GitHub).


## 10. Phases

Every phase: new user-facing strings are added to BOTH strings.xml files in the same change, and the Arabic layout is checked in RTL. Do not leave translation for the end.

Each phase ends with a Definition of Done (DoD). Do not start the next phase until the DoD is met and `docs/PROGRESS.md` is updated.

### Phase 0: Baseline on a real run
- Set up an emulator (Android Studio, API 34 image) or use a USB-debugging phone.
- Run the current build. Write down in `docs/PROGRESS.md` what works and what does not, with exact observations.
- Also record: how data is stored today (SharedPreferences, file, other), minSdk, whether AppCompat is present and its version, and how many hardcoded user-facing strings exist.
- DoD: app launches on a device/emulator, a list of verified vs unverified features exists.

### Phase 1: Data + first screen
- Room database (section 5b) with Patient, Medicine, DoseEvent, Rewards. i18n foundation (section 9b): strings.xml in both languages, RTL, language picker. Empty state, "+" screen with patient picker and flexible schedule (section 5c), home filter by patient (section 9). Fix the Phase 0 items assigned to Phase 1 (section 10b).
- DoD: add, edit, delete a medicine survives app restart AND process kill. Schema v1 exported. No hardcoded strings remain and lint passes with the translation and RTL checks as errors. Arabic screens verified mirrored on device.
- DoD (continued): add, edit, delete a medicine survives app restart. Two patients on one phone (including two with the same name) stay fully independent; deleting one leaves the other's data exact. Age group derives correctly at boundaries (12, 13, 17, 18). Unit tests cover boundaries and empty/invalid input.

### Phase 2: Reminders
- Alarms, notification tap deep link, re-reminders, reboot/time change reschedule, permissions flow. Includes the Phase 0 items assigned to Phase 2 (section 10b).
- DoD (on device): reminder fires at the right time, tap opens the right dose, re-reminders stop after confirm, reboot test passed, deleted medicine does not fire, test-reminder button works.

### Phase 3: PIN
- Create, verify, change, lockout, toggle for confirmation.
- DoD: wrong PIN never confirms a dose, PIN reset works only after device credential and keeps all data, lockout survives restart, hash is not reversible by reading prefs, tests for 4 vs 5 failed attempts boundary.

### Phase 4: Rewards + age-based animation
- Points, levels, streaks, perfect day, milestones, the three animation styles. Keep the emoji mascots in one constants place, not scattered literals.
- DoD: all anti-farming invariants from section 6 and streak rules from section 6b have tests with exact expected values, including the streak-broken animation showing once. CHILD, TEEN, ADULT each verified visually on device.

### Phase 5: Release
- Signed release APK (not only debug), app name, icon, versionName/versionCode.
- Do NOT create the real keystore. Configure signing to read `keystore.properties` (gitignored, kept outside version control). Write the exact `keytool` command and the steps into `docs/RELEASE.md`, and stop there for that part.
- To verify the signing config, a throwaway test keystore in a temp folder is allowed. Delete it afterwards. Also verify that `assembleRelease` fails with a clear message when `keystore.properties` is missing.
- The real keystore must never be committed and must be backed up by the user. If it is lost, updates to installed apps become impossible.
- README with install steps (allow install from unknown sources, notification permission, exact alarms, battery settings).
- Fresh clone or clean worktree build to confirm CI-shaped results.
- Only then: link the new GitHub repo (user does this, or asks explicitly).
- DoD: release build works with a supplied keystore, APK installs on a clean emulator, full flow works, no `INTERNET` permission in the merged manifest (check with `aapt dump permissions`). Manual items left for the user: native Arabic review, real phone test, real keystore.

## 10b. Phase 0 findings (verified on an API 34 emulator)

Facts: storage was SharedPreferences with JSON, minSdk 28, AppCompat 1.6.1 present, 0 hardcoded words, 9 emoji literals in Kotlin, only a template unit test, no Arabic yet.

| # | Problem | Fix | Phase |
|---|---|---|---|
| 1 | Force-stop kills all alarms and opening the app does not restore them | Re-arm all alarms from the DB on every launch. The app cannot prevent force-stop, so say it in the parent README | 2 |
| 2 | Exact alarms are off by default on Android 14, reminders up to 1 hour late | Declare `USE_EXACT_ALARM` (auto-granted on API 33+) and `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"`. Verify on the emulator. If still not granted, show a banner on home with a button to the setting. If the app is ever published on Google Play, re-check the policy for USE_EXACT_ALARM | 2 |
| 3 | After reboot, alarms stay frozen until the app is opened when the permission is missing | Boot receiver re-arms from the DB. Without the permission, fall back to `setAndAllowWhileIdle` plus the banner. Test reboot with the permission granted and not granted. A permission granted through adb not surviving reboot is expected, do not use it as test evidence | 2 |
| 4 | "Perfect day" stays on after adding a medicine | Derived status (section 9) | 1 |
| 5 | Landscape: "Add medicine" button covers a row's delete button | Layout fix, check portrait, landscape, RTL | 1 |
| 6 | PIN dialogs pile up on each other | One dialog at a time, state survives rotation | 3 |
| 7 | Notification permission prompt appears together with PIN creation | Order: language, PIN, first medicine, then ask for notification permission with an explanation | 2 |


## 13. Progress log

Keep `docs/PROGRESS.md` with: current phase, done, verified on device (yes/no), parking lot, open risks.

