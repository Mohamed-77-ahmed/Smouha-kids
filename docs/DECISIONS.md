# DECISIONS

Decisions taken during the autonomous run where CLAUDE.md left something open. Format: decision, alternatives, why.

## D1. Root Gradle files are edited for Room + KSP

- Decision: edit `gradle/libs.versions.toml` and the root `build.gradle.kts` to declare the KSP and Room Gradle plugins (`apply false`). Phase 5 also writes the root README and `.gitignore`.
- Alternatives: keep everything in `app/` (impossible: plugins with versions must be declared in the version catalog / root build).
- Why: Room with KSP is an approved dependency and cannot be wired without these files. Section 2 says "do not touch anything outside `app/` and docs unless asked", and the user asked for Room and for a release README.

## D2. Snapshots instead of git

- Decision: git is not on PATH, so each finished phase is copied (without `build`, `.gradle`, `.kotlin` folders) to `../tracker-snapshots/phase-N/`.
- Alternatives: install git (outside the project, forbidden in section 0).
- Why: section 0 prescribes this fallback.

## D3. Versions

- Decision: Room 2.8.5 (runtime, ktx, compiler via KSP, testing for androidTest, Room Gradle plugin for schema export), KSP 2.3.12. AGP 9.3.3 ships Kotlin 2.2.10.
- Why: latest stable releases on 2026-10-16.

## D4. Coroutines come from Room, no new declared library

- Decision: use `kotlinx.coroutines` (suspend DAOs, Flow, `lifecycleScope`) that arrive transitively through Room and AndroidX Activity/Lifecycle. No extra dependency is declared.
- Alternatives: blocking DAOs on a hand-made executor.
- Why: Room's own public API exposes coroutines and Flow, so this adds nothing new to the APK and keeps receivers simple (`goAsync` + coroutine).

## D5. The whole v1 schema is built in Phase 1

- Decision: Phase 1 already creates every table later phases need (rewards, award ledgers, PIN fields, reminder counters).
- Alternatives: add tables phase by phase and bump the schema version.
- Why: CLAUDE.md 5b says version 1 is the baseline and every later change needs a Migration. Building it complete avoids migrations before the first release.

## D6. PIN length stays 4 to 8 digits

- Decision: 4 to 8 digits (Arabic-Indic digits are converted to 0-9).
- Why: section 0 says "PIN is 4 digits (keep the existing length if the code already uses another)". The existing code accepted 4 to 8.

## D7. PIN hash is salted PBKDF2-HMAC-SHA256 with 120,000 iterations

- Decision: stronger than the old single SHA-256. Stored as `pbkdf2-sha256:<iterations>:<base64>` with a random 16-byte salt.
- Alternatives: plain salted SHA-256.
- Why: a 4-digit PIN has only 10,000 values, so a fast hash can be brute-forced in milliseconds from a copied DB. PBKDF2 makes that slower. The limit is documented: with root access, a 4-digit PIN can still be found.

## D8. Re-reminder interval is per medicine

- Decision: chosen in the Add/Edit medicine screen (5, 10, 15 or 30 minutes, default 10). There is no "off".
- Why: CLAUDE.md 9 lists the interval inside the "+" screen; section 7 lists only 5/10/15/30.

## D9. Deleting a medicine is a hard delete; a points ledger prevents farming

- Decision: delete removes the medicine, its slots and doses (FK cascade). Points live in `dose_award` keyed by (patient, day, minute, normalized medicine name), which is not deleted with the medicine.
- Alternatives: soft delete with the `active` flag.
- Why: CLAUDE.md 5b asks for cascade deletes, and section 6 asks that deleting and re-adding a medicine gives no extra points. Known limit: re-adding under a different name would pay again; this needs the parent PIN.

## D10. A dose can be confirmed at any time on its own day

- Decision: confirmable from 00:00 to 23:59 of its scheduled day. The +5 bonus needs the confirmation within 30 minutes before or after the scheduled time (inclusive, to the second).
- Why: section 0 only sets the end (23:59). The old app also allowed early confirmation.

## D11. Slots carry their own "valid from" moment

- Decision: each slot stores when it was added. A slot added at 14:00 for 08:00 gets its first dose on the next matching day.
- Why: CLAUDE.md 6b says doses scheduled before the medicine was created do not count. Storing the moment on the slot makes edits that add a time behave the same way.

## D12. A medicine's patient cannot be changed when editing

- Decision: the edit screen shows the patient but does not let it change.
- Why: moving a medicine would move its dose history and points between siblings. Delete and add again instead.

## D13. "Reset points and streaks" was removed

- Decision: the old settings button is gone.
- Why: it is not in CLAUDE.md, and with per-patient rewards it would wipe a child's progress with one tap.

## D14. Arabic locale is `ar-EG-u-nu-latn`

- Decision: the app sets this locale tag when Arabic is chosen.
- Why: `values-ar` still matches. The Egypt region gives Saturday as the first day of the week (CLAUDE.md 5c), and `nu-latn` keeps Western digits in formatted numbers (section 0). Verified on the emulator: plurals and times use 0-9.

## D15. Editing the age re-anchors it

- Decision: a new age is stored with today as its anchor day, and it keeps growing from there.
- Why: CLAUDE.md 5 says the age can be edited and must grow by full years.

## D17. Typed PIN digits are not kept in saved state

- Decision: `saveEnabled="false"` on every PIN field. After rotation the PIN dialog is still there (same mode, same lock countdown) but the field is empty.
- Alternatives: keep the digits so the parent does not retype after rotation.
- Why: Android writes saved state into the activity record; a PIN should not live there. Retyping 4 digits is a small cost.

## D18. After a PIN reset, the original action continues

- Decision: when "Forgot PIN" succeeds (phone lock confirmed, new PIN saved), the action that asked for the PIN goes on (for example opening settings or confirming the dose).
- Why: the person just proved they own the phone and set a new parent PIN, which is a stronger check than the PIN itself.

## D19. Home appears before the launch work finishes

- Decision: after the onboarding check, home is shown and observed at once; finishing past days, creating today's doses and re-arming alarms run after that and the screen updates live.
- Alternatives: keep home hidden until everything is done (was blank for several seconds on the emulator).
- Why: a parent opening the app from a reminder should see it immediately.

## D20. PIN check shows a progress bar; PIN dialog pans with the keyboard

- Decision: a thin indeterminate bar while the PBKDF2 hash is computed; `SOFT_INPUT_ADJUST_PAN` on the PIN dialog window.
- Why: on the emulator the check took over 1 s with no feedback; in landscape the dialog was resized until the PIN field disappeared.

## D21. Erase all data (user request after the run)

- Decision: parent settings has "Erase all data". It asks "Erase everything?", then the parent PIN again, then deletes every row of every table in one transaction, cancels all alarms and notifications, empties the cache folder and restarts on the language screen. "Forgot PIN" inside that PIN prompt still works (D18), so the phone owner can erase too.
- Alternatives: `ActivityManager.clearApplicationUserData()` (the system "Clear storage"): complete by construction, but it closes the app at once, takes back the notification permission and cannot run inside an instrumented test. Clearing only the cache folder: the app keeps nothing there, so nothing visible would change. Resetting only points and streaks: still removed (D13).
- Why: the user chose the full reset. Doing it inside the app keeps the notification permission, ends on the language screen instead of closing, and is tested table by table. Row ids are never reused after the erase (AUTOINCREMENT), so an alarm that slipped through finds nothing and stays silent.
- Known: the screen language stays as it was until a language is chosen again on the first screen. On Android 13+ the per-app language entry in the phone settings also stays. The nightly check alarm (00:05) stays booked; with no patients it does nothing, and it is not a reminder.

## D22. Celebrations stay 3 seconds (user request after the run)

- Superseded for rewards by D40 (batch 6): a reward stays max(3 s, min(its sound, 6 s)); streak-ended screens and rewards without a sound keep 3 s.
- Decision: every reward and streak-ended screen, for every age group, closes by itself after 3 seconds (`Celebration.SHOW_MILLIS`). A tap still closes it earlier.
- Before: reward 4.5 s (child), 3 s (teen), 2.5 s (adult); streak ended 5.5 s (child), 4 s (teen and adult).
- Why: the user asked for 3 seconds. Tap to close stays because CLAUDE.md 14 says an animation never blocks the next action. All animations end within 3 s (child confetti 2.8 s, child sleepy-to-sprout mascot 2 s), so nothing is cut off.

## D23. Settings gates (batch 1, item 1)

- Decision: settings open without the PIN; `SettingsAction` lists every action and whether it needs the PIN; the screen routes everything through it. "Manage patients and medicines" is gated too, because section 8 requires the PIN for editing and deleting.
- Not built: a global re-reminder interval (it is per medicine, D8, and editing a medicine is gated) and "reset points and streaks" (removed in D13; it would deduct points, against section 6, which the request did not explicitly override).
- Change PIN asks for the current PIN first, then the new one, in the same dialog (one PIN dialog at a time). "Forgot PIN" in that prompt still goes through the phone's screen lock.
- UI tests run the app with an in-memory DB through a test-only runner (`TestRunner`, `TestApp`), so instrumented runs never read or change the device's real data.

## D24. Alarm sound: two channels, schema v2 (batch 1, item 2)

- Decision: two new channels (alarm and standard) instead of one, because channel sound cannot change after creation and "off" must keep the old behavior; the v1 channel is deleted. The on/off option lives in `app_settings.alarmSoundEnabled` (DB is the source of truth, 5b), added by an explicit Migration 1 to 2 with default 1, so updated phones get the alarm sound.
- Consequence: patient gender (item 3) becomes schema v3, not v2 as the request says. Shipping v2 now and changing it later would break every database already on v2 (the emulator has one).
- Category alarm on the alarm channel so Do Not Disturb lets it through when alarms are allowed there. No `setBypassDnd` (needs the notification policy permission, not requested). No looping/insistent sound.

## D25. Patient gender (batch 1, item 3)

- Decision: `patient.gender` TEXT, nullable ("MALE" / "FEMALE"), schema v3 with `MIGRATION_2_3` (v2 was taken by the alarm sound option, D24). Required only for new patients (`ValidationError.GENDER_MISSING`); the patient dialog lets it be set but never asks. Editing without a choice keeps the stored value (a set gender is not cleared by accident).
- Avatars (CHILD 👦 👧 🧒, TEEN 👨‍🎓 👩‍🎓 🧑‍🎓, ADULT 👨 👩 🧑) live in `Mascots.avatar`. Labels say "for the picture only" so parents know it changes no wording.
- `androidx.room:room-testing` was already in androidTest (Phase 1), nothing added.
- Found and fixed while testing: `AppLanguage.syncWithSystem` ran in `Application.onCreate`, where AppCompat on Android 13+ cannot see the per-app language (no activity yet). It treated that as "none" and re-applied the stored language, overriding a choice made in Android's per-app language screen. Now it reads and sets the language through `LocaleManager` on 13+. Test: `AppLanguageTest`.

## D26. Visual refresh: dependencies, palette, motion (batch 1, item 4+5)

Dependencies (approved by the request; APK impact measured with clean debug builds, release is not minified either):

| Dependency | Why | APK impact |
|---|---|---|
| `com.google.android.material:material` 1.10.0 -> 1.14.0 (latest stable) | Material 3 components and color roles (surface containers) | +995 KB |
| `androidx.core:core-splashscreen` 1.2.0 (latest stable) | Launch screen on every API level from 28 | +43 KB |
| `androidx.dynamicanimation:dynamicanimation` 1.1.0 | Spring press feedback and the spring entrance of the reward card | 0 (Material already ships it) |
| Lottie | Not added: `design/lottie/` does not exist, so the canvas animations stay (as the request says) | 0 |

- Palette "Calm and Growth" in `colors.xml` / `values-night/colors.xml` with the same token names; the theme maps every Material 3 role to them. Text carries the darker tones (green-800 primary, blue-700 secondary, teal-700 tertiary in light); the gradient keeps green-400 to blue-500 (to bottom-right). White text fails on green-400 (1.74:1), so text on the light gradient is near-black #020617; dark mode uses green-800 to blue-800 with white text. Red (error, missed, overdue) and amber (warning: "reminders may be late") stay outside the palette, always next to words or icons.
- Dynamic color: the app never enabled it; nothing to switch off. The app follows the system dark mode as before, now with the night palette.
- Dialogs were already Material 3 (`MaterialAlertDialogBuilder` on a Material 3 theme) and now use the palette and surface containers. They stay dialogs, not bottom sheets, to keep the guarantees already tested: one PIN dialog at a time, rotation safe, no digits in saved state.
- The extended FAB cannot take a custom background in Material 1.14 (it throws), so the gradient sits on a wrapper behind a transparent FAB; the FAB keeps its touch, ripple and accessibility.
- Material 3 tints elevated cards with the primary color; the reward card now has an explicit token color (`surface_lowest`) so the tested contrast is what is shown.
- Motion: fade-through between screens through the theme's window animations (no code, follows the system animation scale); spring press on the main buttons; the reward card springs in. With the system animation scales at 0 everything jumps to its end state (checked on the emulator).

## D27. Any screen size (batch 1, item 6)

- Home is one block per patient (header and doses) in a StaggeredGridLayoutManager: 1 column, 2 from 840dp. Each column may use up to 600dp, so the two-column grid is at most 1200dp wide, centered; single-column content is at most 600dp.
- Width limits are side padding computed at layout time (`Responsive.limitWidth`), not resource qualifiers, so multi-window and display-size changes are covered without extra layouts.
- Chips wrap instead of scrolling sideways (every patient is always visible). On short screens (< 640dp high) pinned wrapped chips would crowd out the list, so there they scroll with the header and return on the first scroll up.
- Dose cards: when the column width divided by the font scale is under 340dp, the action button moves under the medicine name (found on the small screen at font 2.0, where the name was squeezed to one letter per line).
- Found and fixed: when the header scrolls away, its text ran under the status bar clock; a band in the gradient now covers the status bar while scrolled.

## D28. The app plays the alarm sound itself (batch 2, item 2)

- Why: on the user's Samsung SM-A266B (One UI 8.5, Android 16) three reminders on the batch 1 alarm channel with the ringer on silent got no alarm-stream player at all, while the same channel rang with the ringer on normal (`docs/evidence/batch2/phone-diagnosis.txt`). A channel sound alone is not reliable.
- Design: `AlarmSoundService`, a foreground service. MediaPlayer with `USAGE_ALARM` / `CONTENT_TYPE_SONIFICATION`, looping, audio focus `AUDIOFOCUS_GAIN_TRANSIENT`, stopped after 30 s (`SoundPolicy.MAX_PLAY_MILLIS`). Sound: the default alarm sound through its settings URI (`RingtoneManager.getDefaultUri(TYPE_ALARM)`, which Android serves without a storage permission) when an alarm sound is set (`getActualDefaultRingtoneUri`), else the default notification sound. No volume is ever changed. Losing audio focus (a call, another alarm) stops the sound; short ducking is left to Android.
- Foreground service type and permissions, from the official documentation (read 2026-10-16):
  - https://developer.android.com/develop/background-work/services/fgs/service-types : type `systemExempted`, manifest permission `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, constant `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED`; "To use this type, an app must meet at least one of the following criteria: ... Apps holding SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission".
  - https://developer.android.com/about/versions/14/changes/fgs-types-required : the same criterion in full, "Apps holding SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission and are using Foreground Service to continue alarms in the background, including haptics-only alarms", otherwise `ForegroundServiceTypeNotAllowedException`; an app targeting Android 14+ without a type gets `MissingForegroundServiceTypeException`.
  - https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start : apps targeting Android 12+ cannot start a foreground service from the background (`ForegroundServiceStartNotAllowedException`) except in listed cases, among them "Your app invokes an exact alarm to complete an action that the user requests."
  - Result: `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `<service android:foregroundServiceType="systemExempted">`, `startForeground(..., FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)` on API 34+ (0 below, where types are not checked). The app already holds `USE_EXACT_ALARM` (33+) and `SCHEDULE_EXACT_ALARM` (max SDK 32). First reminders, follow-ups and the test reminder are exact alarms, so the background start is allowed. Checked on the emulator with the app in the background: "Background started FGS: Allowed ... code:ALARM_MANAGER_WHILE_IDLE", `types=00000400` (`docs/evidence/batch2/item2b_background_vibrate.txt`).
  - Not chosen: `mediaPlayback` (documented for continuing audio or video playback, not alarms), `shortService` (no type permission, but documented for "critical work that cannot be interrupted or postponed"), `specialUse` (needs a declared use case; the docs name `systemExempted` for this exact case). Google Play restricts `USE_EXACT_ALARM` and these types; the app is shared through Drive (section 10). Re-check if it ever goes to Play.
- Notification: the service's notification is the reminder itself (patient name, tap opens that dose) on a new silent channel `pill_reminders_ringing` (importance high, no sound, vibration on), with "Stop sound" (no PIN), a delete intent (swiping it away stops the sound) and `FOREGROUND_SERVICE_IMMEDIATE`. When the sound ends by Stop, 30 s, focus loss or a newer reminder, the notification is detached and updated without the button and with `FLAG_ONLY_ALERT_ONCE`, so it stays and does not alert again. A tap or a confirm removes it, as `autoCancel` did before.
- Two reminders due together: one player. The newer one takes the sound over; the older one is detached and updated quietly.
- Fallback (never both, never silent by accident): when `startForegroundService` or `startForeground` is refused, audio focus is refused or no sound can play, the reminder goes to `pill_reminders_alarm` (batch 1) and the reason is logged with tag `PillPalsSound` (`SmouhaKidsSound` since batch 3). `AlarmSound.forcedFailure` lets the instrumented test force both failure points.
- Channel names now tell the three channels apart in Android's settings (EN/AR): "(alarm)", "(backup alarm sound)", "(notification sound)".
- versionCode 2, versionName 1.1.0, shown at the bottom of parent settings: every earlier build was 1 / 1.0.0, so the phone could not tell them apart.
- Sound status rows in parent settings (`SoundStatus`, `PhoneSoundSource`, free actions): Do Not Disturb from `getCurrentInterruptionFilter()` and, on API 30+, `getConsolidatedNotificationPolicy()` (the policy in force, for example a schedule's), else `getNotificationPolicy()`; total silence, or priority without alarms, is a warning. Buttons: `ACTION_SOUND_SETTINGS`, `ACTION_ZEN_MODE_PRIORITY_SETTINGS` (API 26+), `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; any of them missing on a phone falls back to the main settings page.
- Found and fixed while testing: `TrackerRepository.settings()` read, inserted and read again outside a transaction, so an erase on another thread could remove the row in between (one instrumented run crashed with "Required value was null"); it is now one transaction. `PinDialogFragment`'s show listener could run after the fragment was destroyed when the screen closed right after opening the dialog (a test crash after a reminder tap); now guarded. That dialog is replaced in item 4.
- Test notes: on this emulator image, ringer SILENT set through `AudioManager` also turns on Do Not Disturb (priority, alarms allowed); the test grants itself Do Not Disturb access through the shell and puts both back. The tap test taps the reminder in the notification shade, because Android blocks an activity start from a PendingIntent sent by the test in the background.

## D29. Launcher icon without purple (batch 2, item 3)

- Background: palette gradient green-600 #16A34A to blue-600 #2563EB (the request's example), to bottom-right like the in-app gradients, as tokens `icon_gradient_start` / `icon_gradient_end`. White reaches 3.29:1 on green-600 and 5.16:1 on blue-600. The old lighter circle behind the capsule is gone (it would lower the contrast behind the symbol).
- Symbol: the capsule stays, now two white halves with a gap (`icon_symbol`). The old yellow half was 2.2:1 on green-600; any color other than white falls under 3:1 on that end, so the two halves are told apart by the gap, not by color. Farthest point 29dp from the center, inside the 66dp safe zone. The same drawable is the monochrome (themed icon) layer and the splash icon.
- Splash background: `icon_gradient_start` (white symbol 3.29:1); the old light green-400 was 1.74:1.
- No night variant for the icon tokens: launchers show the same icon in light and dark mode; themed icons cover the system-colored case.
- Checks: `IconContrastTest` (unit: tokens, the drawables and the splash theme), `LauncherIconTest` (instrumented: safe zone in pixels, monochrome layer, circle and squircle renders), `scripts/find-purple.ps1` (no purple left). The emulator image has no icon-shape overlays, so the squircle comes from the test render, not the launcher.

## D30. The PIN sheet (batch 2, item 4)

- One component, `PinSheet` (a Material 3 `BottomSheetDialogFragment`), for every PIN entry: verify (confirm a pill, add, edit, gated settings), create (first launch, or a verify that finds no PIN), change (current, new, repeat) and reset after "Forgot PIN" (new, repeat). The old `PinDialogFragment` and `dialog_pin.xml` are gone; the first-launch screen opens the same sheet, not dismissible, so no system keyboard can open there.
- Input rules live in the pure `PinEntry` (unit-tested state machine). Typed digits and the first new PIN stay in memory (`PinSheetModel`, a ViewModel that survives rotation); only the flow and the step are saved, so a rotation keeps the step and clears the typed digits, and after a process death a "repeat" step without its first PIN starts the new PIN over. The check and the save run in the ViewModel, so a rotation in the middle neither repeats nor loses them.
- Length: the code already accepted 4 to 8 digits (`PinRules`), and CLAUDE.md 0c says to keep an existing length. So 4 dots show and more appear for a longer PIN, and an OK key submits. No auto-submit after 4 digits: a parent with a longer PIN would be counted as wrong.
- Keypad: always left to right with Western digits (layout direction LTR on the keypad only; the rest mirrors). Delete, 0 and OK on the last row like Android's own PIN pad. Haptic tap on every key, a reject haptic and a shake on a wrong PIN or a mismatch (no shake when animations are off). Every key has a content description; the dots row reads "N digits entered". A hardware keyboard works too (digits, Delete, Enter).
- Lockout and checking: the keys and OK are off; the countdown follows the stored lock-until time every second; a progress bar while the PBKDF2 hash runs.
- Found and fixed while testing: `verifyPin` stamped the failure (and the lock start) with the time from before the slow hash, so the lock lasted about 58 s instead of 60 and the first second of the countdown showed 60; the time is now taken after the hash.
- Small screens: the keys are 48dp under 480dp of height, 56dp up to 640dp, 72dp above (resource qualifiers). Under 600dp of height the badge and the drag handle are left out (back and a tap outside still close the sheet), titles and messages are at most 2 lines, a multi-step flow shows the step label instead of the message, and an empty status line collapses. "Forgot PIN" sits under the keypad (beside it in landscape), so the keypad comes first. Landscape uses a side-by-side layout (words beside the keypad, top aligned). Everything scrolls rather than being cut. The sheet is at most 480dp wide, centered.
- The other dialogs stay dialogs (as requested) but share the sheet's look through the theme: `materialAlertDialogTheme` with the low surface container, 28dp corners and the Title Large title.
- Copy: the first-launch text no longer says the PIN opens parent settings (it has not since batch 1 item 1); five strings of the old dialog were removed in both languages.

## D31. applicationId com.smouha.kidsreminder (batch 3, item 0)

- Decision: `applicationId = "com.smouha.kidsreminder"`. The namespace, the Kotlin package (`com.example.tracker`), class names and the DB name stay, so no code moved. Version 1.2.0 (3).
- **Permanent after the first real release.** Android identifies an installed app by this id: a later APK with another id installs as a separate app, and an update must keep both this id and the signing key (docs/RELEASE.md). Never change it again once a parent has installed a release.
- Consequence now: for Android it is a new app. Phones with an older build (`com.example.tracker`, "Pill Pals") keep that app and its data side by side; the old one is removed by hand (docs/PILOT.md). There is no data transfer between the two (they cannot read each other's files).
- Intent actions of our own alarms and of the sound service moved to `com.smouha.kidsreminder.action.*` so they carry the app id. They are explicit intents to our own components, so the old names would also have worked; nothing outside the app uses them. Fully qualified class names (`com.example.tracker.TestRunner`, custom views in layouts) are package names and stay.

## D45. GitHub: the repository address and real names stay out of committed files (batch 7)

- Decision: the repository is named in files as `Smouha-kids` of the user's account (`https://github.com/<owner>/Smouha-kids`); the credit to the original project is its plain name "Medipulse-App", without the link.
- Why: the pre-push scan must find none of the real names in a text file, and both the repository address and the old credit link contain the account name, which is a real first name. The address is in the change request and in `git remote` (where it is needed), not in documents.
- `dist/` is ignored (as the request lists it), so `docs/GITHUB.md` no longer suggests committing it: the APK and the PDFs go to a draft release as assets. The raw originals in the project root are ignored by extension at the root only (`/*.jpeg`, `/*.mp4`, ...); `design/brand/` keeps their exact copies.

## D44. The documents in the Koko identity (batch 7, item 2)

- `print.css` takes palette v2: headings in the deep rooster orange #A84A0A (5.8:1 on white) and the sky tone #0B6A86, notes in cream with a sunny yellow edge, info boxes in a light sky blue; amber stays the warning color. The number circles are #A84A0A with white digits.
- The Koko icon stands beside the title of every document and on the card. The hospital logo is shown only in the two hospital handoffs, next to a sentence saying that its use, like the name's, needs the hospital's approval: the parents' documents carry no hospital logo, so nothing printed for families looks like a hospital leaflet. The logo still appears inside the app screenshots, as it is in the app.
- The user guide stays at 5 pages with the two new parts (the "Celebration sound" setting, the end-of-course screen): page 4 holds three text-and-picture blocks with smaller pictures, "changing phones" became one question of the FAQ, and the last page has a slightly smaller text.
- The guide screenshots (`docs/print/shots/`) were all taken again from the signed 1.1.0 on the test emulator in Arabic with fictional data, plus `u18_settings_celebration` and `u19_course_end`; the real-phone install screens stay placeholders (`docs/guide-shots-needed.md`).

## D43. NotebookLM composites (batch 7, item 3; the batch 5 pack)

- Decision: `scripts/make-composites.ps1` (System.Drawing in Windows PowerShell, tooling only) puts three screenshots side by side, right to left like the Arabic slides, with rounded corners and a soft shadow on a light sky to cream background (the palette's #F4FAFD and #FFF6E0), 2400x1500, no text and no brand art.
- Alternatives: by hand in a slide tool (not repeatable), ImageMagick or Python (not installed; nothing new is installed).

## D42. The "avatar of finish" video replaces "avatar of end" (user request, 2026-10-19, before batch 7)

- Decision: the course-complete screen plays `avatar of finish.mp4` (copy `design/brand/avatar-finish.mp4`), re-encoded like the first video (H.264 Constrained Baseline, AAC-LC, faststart) into the same resource `res/raw/avatar_end.mp4`, so `CourseEndScreen` does not change. Kept at its own 480x854 (scaling up adds nothing); `CourseEndLayout.VIDEO_ASPECT` follows it (480/854, within 0.1 percent of the old 9:16). APK about 1.8 MB smaller (the video is 1.54 MB instead of 3.37 MB).
- Trimmed: the original's last 2 frames (6.00 s and 6.03 s) are pure black. The screen holds the last frame until the family closes it (D41), so untrimmed it would have ended on a black box under the text. Cutting at 5.995 s keeps every picture frame and ends on the smiling child; the audio is cut at the same point. Alternatives: hold an earlier frame in code (a code change for a file problem), or ask for a new file (the rest of the video is fine).
- Unchanged: the card color (the new video has the same navy night sky at its edges), the text, the sound rule and every course-end behavior. The old original and its copy stay in the root and `design/brand/` and are no longer used.

## D41. The course-complete screen (batch 6, item 6)

- The final dose is computed from the medicine's schedule with the same rule that creates doses, not from the dose rows: dose rows exist only for days the app has seen, while the schedule knows the whole range. Only the last 7 days of the range need checking, because a weekly schedule repeats. A changed end date changes the answer at once, as requested.
- When: after the reward closes (the reward stays the immediate thanks for the dose), before a sibling's streak-broken screen. It is decided when the dose is confirmed and only then, so a missed final dose never shows it and confirming again (refused) never shows it twice. No data is written.
- Presentation from the frames (`docs/evidence/batch6/video-frames/`): the edges are dark navy, so the card is #1E2A44 with white text (14:1); the card stays the same in dark mode. The text sits under the video and may be wider than the video (at least 300dp), so in landscape the narrow portrait video does not squeeze it into many short lines; the extra card width has the video's own edge color.
- TextureView with MediaPlayer, not VideoView: a SurfaceView ignores the card's rounded clipping, and this way the last frame simply stays on screen after the video ends.
- Sound: USAGE_MEDIA, the video's own audio, following "Celebration sound"; one sound at a time stays true: an alarm silences it before ringing (`CourseEndScreen.silenceForAlarm`), and losing audio focus silences it for good while the picture goes on.
- "Restarts after rotation" is read as "keeps playing after a rotation": it goes on from where it was (a finished video shows its last frame again), rather than starting over, which would replay the whole video at every turn of the phone.
- Closing: the close button (48dp, on a dark round patch so it shows on any frame), a tap outside the card, or Back. It never closes by itself: it holds the last frame with the text until the family closes it, and it never blocks anything (no PIN, everything stays reachable once closed).

## D40. The celebration avatar and the reward's sound (batch 6, item 4; supersedes D22 for rewards)

- Who: CHILD and TEEN rewards show the avatar and may play the sound; ADULT stays calm (checkmark, no sound), as the request says "ADULT unchanged". TEEN keeps its shorter confetti (the lighter style of CLAUDE.md 5) but now also gets the count-up.
- Usage: USAGE_GAME, not ASSISTANCE_SONIFICATION. Sonification follows the ring volume on most phones and is muted on silent; a game sound follows the media volume, like the end-of-course video (item 6) and like any game the child plays. A parent who does not want it turns "Celebration sound" off (PIN-gated, default on). Alternative considered: respecting silent mode through sonification; rejected because many parents keep the phone on silent all the time and the doctor asked for the sound.
- Audio focus: transient with ducking, so music lowers for 3 s instead of stopping; refused focus (a call) means no sound. Never a volume change.
- One sound at a time: the alarm always wins. Confirming a ringing dose first asks the alarm service to stop and the reward's sound waits until it has stopped (at most 1.5 s, then it stays silent); a different reminder that still rings keeps the reward silent; an alarm that starts during the reward stops the reward's sound before it rings. Checked by sampling `dumpsys audio` (CelebrationSoundTest).
- Time: max(3 s, min(sound, 6 s)), counted from the moment the sound really starts; a sound longer than 6 s fades out over the last 0.5 s (the player's own gain). The dose sound is 2.77 s, so a reward still lasts 3 s.
- Once: the sound is never replayed: a rotation restores the reward without it, and the screen stopping (background, rotation, closing) stops and releases the player.
- The cheering capsule mascot and the teen badge are no longer shown anywhere, so their drawable and constants were removed; the sleepy and happy mascot stay for the CHILD streak-ended screen (the no-streak avatar is BLOCKED) and the empty state.
- Schema v4 (one column, default on, Migration 3->4) was approved by the user before the batch started; nothing else in the schema changed.

## D39. The hospital logo on onboarding, in the header and on About (batch 6, item 3)

- The logo is always on a white rounded tile (`logo_tile`, also in dark mode): the supplied logo is drawn on white (the "no background" version has a baked-in checkerboard, D36), and a white tile keeps its navy, red and light blue true on the light and the dark gradient.
- Header: a fixed 56dp logo in a tile, then a flexible space, then the settings button. A fixed size cannot grow with the font, so the logo never pushes into the button at font 2.0; the arc text of the logo is too small to read at that size, the logo is still recognizable (the hospital name stays readable on onboarding and About).
- The header logo is the screen's heading and is described as "Koko Dawa. Smouha University Children's Hospital logo": the app's name left the screen visually, so a screen reader would otherwise never hear which app this is.
- About shows the hospital logo large instead of the rooster circle (the launcher already shows the rooster). The rooster circle's drawable was removed.
- No endorsement: the strings are scanned for claim words in English and Arabic (BrandTextTest). Showing a logo the doctor supplied is not a claim; no text calls the app official, approved or recommended.

## D38. Palette v2 "Koko" (batch 6, item 2)

- Primary is the deep tone of the rooster orange (#A84A0A), not the picture's own orange (#F38E32). The contract needs primary as a button with white text and as a text color on every surface at 4.5:1; #F38E32 reaches 2.4:1 with white, and every orange light enough to look like the picture fails one of the two. The lighter oranges live where no text depends on them: the primary container, the confetti, the dark-mode primary. Alternatives: an orange button with dark text (primary would then fail as a text color on cream, so titles would need another role: a layout change the item did not ask for); Material's own tone 40 for this seed (#8B5000, browner).
- "Secondary = the sunny yellow with dark text" is met by the secondary container: #FDC440 itself with #3B2800 text (8.6:1) on the selected chip, the PIN keys and every tonal button. The `secondary` token is the yellow's dark ochre tone (#7A5700), because ContrastTest keeps secondary usable as a text color on surfaces (the test was not weakened).
- Three gradient stops: a two-stop sky-to-yellow gradient passes through a grey-green middle (seen in a preview), which reads as the old green. A cream center keeps it sunny; in dark mode a navy center avoids a muddy olive. Every stop is checked for contrast, and a test makes sure the drawables paint exactly those stops.
- Warnings: amber sits between the brand's orange and yellow, so amber words looked like an orange section title. They became an amber box with a 2dp border, a warning triangle and dark text. Errors keep red words (red is not in the brand).
- Kept: `success` green (semantic) and the green capsule mascot (empty state, CHILD streak ended), because the request keeps the streak-broken visuals while the no-streak avatar is BLOCKED. Listed in CHANGELOG and FINAL_REPORT for the doctor.
- `docs/palette-v2.md` and `.png` are generated from the tokens (`scripts/make-palette-sheet.ps1`), so they cannot drift from the app.

## D37. Koko Dawa name and icon (batch 6, item 1)

- One name for the app: `app_name` is Koko Dawa / كوكو دوا, the application label; the launcher activity has no label of its own (lint RedundantLabel), so the launcher shows the same name. The hospital's name became its own string `hospital_name` and is used only where it names the hospital (onboarding under the logo, About), so it can never turn into the app name by accident.
- The home header's short-name rule (`Responsive.fullTitleFits`, 360dp per font unit) existed only because the full hospital name was long; with one short name it is gone, and in item 3 the logo replaces the text anyway.
- Splash: the first try drew the rooster's crop (its faded square) on a flat sky color; on the emulator a lighter square around the rooster was visible, because the picture's sky gets lighter towards the bottom. The splash icon is now the adaptive launcher icon itself: Android draws it as a round badge with its own gradient, which is also the default look of an Android 12 splash. The rooster there is enlarged from a small source (koko.jpg is 1024x559) and looks slightly soft.
- The splash name is dark navy (`splash_name_text` #12304A, 9:1 or more on `koko_sky`); `render-splash-name.ps1` reads that token, and IconContrastTest checks that the rendered images really use it.
- The certificate keeps `CN=Smouha Kids`: the name on a phone comes from the app, and a new certificate would mean a new key that could never update installed copies.
- versionCode 4 (not 2): the update test of batch 4 already built a 1.0.1 with versionCode 2 and the debug pilot build had 3; a release must be above every build a phone could have.

## D36. Koko Dawa assets: what is used and how (batch 6, item 0)

- Source of truth: the doctor's originals in the project root, never moved or edited; exact copies in `design/brand/` with ASCII names (`docs/assets-report.md`). The app's resources come from those copies through `scripts/make-brand-assets.ps1` (System.Drawing in Windows PowerShell: crop, high-quality bicubic scale, place) and ffmpeg (re-encode). Pillow and numpy were not needed: no background was keyed (Python is not installed anyway).
- Transparency: no original has alpha. The three "no background" files carry a baked-in checkerboard (measured on the borders and seen), so they are never used. The rooster comes from `koko.jpg` (its sky blue background is real); the hospital logo from the white original on a white tile; the celebration creature is shown as a rounded picture with its own blurred-room background, because that background is neither flat nor transparent (keying it would leave a halo, and the request forbids redrawing). avatar no streak exists only on a checkerboard: BLOCKED.
- Launcher icon from a JPEG: the title above the rooster rules out a full-bleed square that keeps the rooster inside the safe zone, so the foreground is the largest text-free square, scaled until the rooster and its props fit the 66dp safe zone, with a 12 px alpha fade at the crop's border over a background layer that is a vertical gradient of the crop's own top and bottom rows. A single flat color showed a faint square edge in the preview; the gradient does not. The rooster's pixels are not touched.
- Monochrome (themed) layer: a clean silhouette cannot be derived from a JPEG character with props, stars and a soft background, so it is omitted. On Android 13+ with themed icons on, launchers show the full-color icon instead.
- Video: the original is H.264 High profile; baseline decodes on every Android 9+ phone. Re-encoded at CRF 23 to 3.37 MB (the High-profile original is 0.99 MB); still under the 4 MB target. Sound: MP3 plays on every Android version with MediaPlayer, kept as is.
- APK size impact (raw and drawable files): video 3.37 MB, icon 0.11 MB, notification icon 0.11 MB, hospital logo 0.16 MB, celebration picture 0.04 MB, sound 0.06 MB: about 3.85 MB before compression (measured on the built APK in item 1).

## D35. Documents: PDF pipeline, QR tool and pictures (batch 4, item 2)

- PDFs: right-to-left HTML (`docs/print/`) printed by headless Microsoft Edge (Chrome as a fallback), as requested, because a browser shapes Arabic correctly. No PDF library. Fonts: Windows' own Segoe UI (Arabic and Latin), nothing downloaded; the PDF embeds the subset it uses. Each page is checked from the PDF itself: `scripts/pdf-to-png.ps1` renders it with Windows' built-in PDF renderer (`Windows.Data.Pdf`), so no extra tool is needed for that either.
- QR code: ZXing 3.5.3 (`com.google.zxing:core` and `javase`, Apache 2.0) in a separate small Gradle build `scripts/qr/` (an `application` with one class `QrTool`: encode to SVG, decode from PNG), fetched from Maven Central by Gradle like the app's own libraries. It is tooling for `scripts/make-card.ps1` only and is never part of the app (the app's dependencies and permissions are unchanged; aapt still shows no INTERNET). Alternatives: a Python QR package (Python is not installed here; the Windows "python" is only the Store stub), a QR encoder written by hand (then no independent decoder would be left to check it). Error correction M with a 4-module quiet zone, 52 mm on the A5 card.
- What the QR code holds: the DIRECT download link from `dist/link.txt` (`DIRECT_LINK=`), so a scan starts the download; the folder link (with the guides and the video) goes into the WhatsApp message. Without a link the card is a marked DRAFT with an empty dashed box, never a made-up link.
- The check reads the QR code back from the page rendered from the PDF (ZXing), so it covers the printed card, not only the encoder.
- Pictures for the user guide: the test AVD, the release build 1.0.0, the Arabic UI, fictional names (`seed-demo.ps1 -Lang ar` and one patient added through the form). `adb shell input text` cannot type Arabic, so `docs/evidence/tools/settext/` (a small UiAutomator 1.0 test built from the SDK's `android.jar` and `uiautomator.jar` with javac and d8, run with `uiautomator runtest`) sets the text through the accessibility "set text" action. Test AVD only.
- Real-phone screens (Drive, browser warning, unknown sources, Play Protect, Samsung Auto Blocker) are not on the emulator and must not be drawn, so the install guide shows a dashed box with the file name until `design/guide-shots/` has the real picture (`docs/guide-shots-needed.md`); `make-docs.ps1` puts them in automatically.
- Wording: parents' documents address guardians in the plural imperative (اضغطوا، افتحوا), as the request says; that is gender-neutral. The app's own copy keeps its rule (no imperatives at all, CLAUDE.md 9b). "ملف التطبيق" instead of APK; the hospital handoff names the real file `Smouha-Kids-1.0.0.apk` once, as a file name.
- System labels quoted in the guides (for example "Download anyway", "Install anyway", "Unrestricted") are given in English inside the Arabic sentence with a plain Arabic description, because the exact Arabic wording differs per phone and Android version and could not be checked here; the real-phone screenshots will show them.

## D34. First signed release 1.0.0 (1) and how a release is checked (batch 4, item 1)

- Version: 1.0.0, versionCode 1, as requested. The builds before it (versionCode 1 to 3) were debug builds signed with the Android debug key, so no installed copy could ever update into a release anyway; the version line starts over with the real key. Consequence: a test phone with the debug pilot build 1.2.0 (3) must uninstall it first (its test data goes); families never had it. Recorded in docs/PILOT.md 0a and docs/RELEASE.md 4.
- Update test version: `-PupdateTestVersionCode` / `-PupdateTestVersionName` override the version only for `scripts/update-test.ps1`, so the next version is built from the same code with the same key and nothing in the project has to be edited and put back. Alternatives: editing `build.gradle.kts` and reverting it (easy to forget), a copy of the project in a temp folder (would copy `keystore.properties` with the password). A real release never passes them; `update-test.ps1 -NextApk` takes a real next release instead.
- The other key for the negative test is the Android debug key (`~/.android/debug.keystore`, password "android", public by design): the same 1.0.1 APK re-signed with it by `apksigner`, so only the key differs. No new keystore is created (CLAUDE.md 10). The re-signed copy is deleted at once.
- Fingerprint: read from the APK (`apksigner`), not from the keystore (that needs the password). The first value was written into docs/RELEASE.md by `verify-dist.ps1 -RecordFingerprint`; every later check compares with it.
- Signature scheme: AGP signs v2 only for minSdk 28 (`apksigner`: v2 true, v3 false). v2 is what Android 9+ needs; v3 would only matter for a future key rotation, which still needs the old key. Not changed.
- Paths with Arabic letters: `aapt` and `apksigner` cannot open `dist\...\للأهل\...`, so `verify-dist.ps1` checks a copy in an ASCII temp folder and compares the hashes; `adb install` gets an ASCII copy too.
- Password hygiene: no script opens `keystore.properties`; Gradle reads it. `app/build` holds no file with the password (searched by key name after the build: only `signing-config-versions.json`, which has no password). Snapshots leave out `keystore.properties`, `*.jks`, `*.keystore` (`snapshot.ps1`, which also fails if one slips through).

## D33. Child-friendly design scope (batch 3, item 4)

- Fonts: `design/fonts/` does not exist, so the system font stays everywhere. Nothing downloaded. When a rounded font with Arabic and Latin glyphs and its license is added, it goes to titles, buttons and reward text only (res/font, `fontFamily` on those styles), with screenshots of both scripts, digits and RTL shaping.
- Mascot: a capsule character (the app's own symbol) drawn by hand as three VectorDrawables in the palette; no library, nothing generated or downloaded. It replaces the emoji where the request names them: the CHILD reward (cheer; the perfect-day trophy and the random emoji too), the CHILD streak ended (sleepy, then happy instead of the sprout) and the empty state (happy instead of the teddy bear). TEEN keeps its badge and ADULT its checkmark: the request keeps ADULT calm, and the TEEN contract says "simple badge". It looked good in the screenshots, so the emoji were not kept.
- Idle motion: a breath (scale, from the feet) instead of a bob, because the reward wiggle already animates rotation and position. Three breaths, then rest: the first version breathed endlessly, and uiautomator then failed on the empty state ("could not get idle state"), because each scale change sends an accessibility event; TalkBack would get the same endless stream. With animations off it does not start at all: without that guard the platform would jump the infinite animator to its end and leave the mascot stretched (shown by the mutation check).
- Buttons: 56dp with no insets, so the visible pill is 56dp (Material's default insets would leave 44dp visible). PIN keys keep their height-based sizes (the whole keypad must fit on small phones, CLAUDE.md 9), text-only buttons keep 48dp.
- Shadows: lower elevation and a palette-tinted shadow color (`outlineSpotShadowColor`, API 28+, the minSdk) instead of grey.
- Copy: `empty_state` in Arabic used "دوس" (an imperative, edited outside this run before batch 3); it is now third person ("علامة (+) بتضيف أول دوا").

## D32. App name: full and short, and the splash image (batch 3, item 1)

- Application label = the full hospital name; the launcher activity (MainActivity) label = the short name, as requested. Recents shows the activity label (short). Android 13+ shows the launcher label in the app's own per-app language (checked: the launcher showed the Arabic short name only once the app language and the system were Arabic). At font 1.5 the Pixel launcher shortens the short name too ("Smouh..."), as it does for "Messages" and "Contacts"; nothing in the app can change that.
- Home header: the full name in at most 2 lines at 22sp; under 360dp per font-scale unit it would need 3 or more lines next to the settings button, so the short name is shown (`Responsive.fullTitleFits`, `ResponsiveTest`). The content description keeps the full name for TalkBack.
- Language picker: the full name in Arabic and in English, because no language is chosen yet on that screen.
- Splash: Android 12+ draws the launch screen itself and accepts only an icon and a "branding image", no text. The full name is therefore a plain white text image (`splash_name.png`, Arabic in `drawable-ar-nodpi`) rendered from `app_name` by `scripts/render-splash-name.ps1`, with the Windows "Segoe UI" font (only the image ships, not the font). It is text, not a logo, and it must be rendered again when the name changes. Android 9 to 11 use the compatibility splash, which has no branding slot: icon only there. White on the splash green-600 is 3.29:1, enough for this large bold text (IconContrastTest now checks the v31 splash theme too).
- Logo: `design/brand/` does not exist, so no logo is shown anywhere (nothing drawn, generated or downloaded).
- Renamed for the name change: the sound log tag `PillPalsSound` -> `SmouhaKidsSound`, the stop intents' URI scheme `pillpals` -> `kidsreminder`. Kept on purpose (internal, never shown): class `PillPalsApp`, DB file `pill_pals.db`, style names `PillPals.*` / `Widget.PillPals.*`, the env var `PILLPALS_TEST_RUNNER` of the test script.

## D16. Arabic patient names on emulator screenshots were set in the DB

- Decision: `adb shell input text` cannot type Arabic, so the Arabic names on the Phase 1 screenshots were written with sqlite (`char()` code points) after the patients were added through the UI with Latin names.
- Why: it shows display, bidi and RTL with real Arabic names. Arabic input itself is covered by unit tests (`arabicAndMixedNamesAreAccepted`). Typing Arabic in the form on a real phone stays UNVERIFIED.
