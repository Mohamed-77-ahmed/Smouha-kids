# Claims check for the NotebookLM sources

Every factual claim in `sources/`, the evidence for it, and its status. Status: **verified emulator** (a test or a check that ran on the test emulator, or a measured file), **verified real phone** (only the 17-check pilot on an old test build), **under trial** (قيد التجربة: not verified, said so in the sources), **design** (what the app is built to do, read from the contract and the code, shown on the emulator). Checked 2026-10-19 against version 1.1.0. Claims without evidence were removed; none is left open.

Evidence paths are relative to the project; the sources themselves contain no paths.

| # | Claim (source) | Evidence | Status |
|---|---|---|---|
| 1 | The app reminds at the medicine's time with the patient's and the medicine's name (01, 02, 03) | `RepositoryReminderTest`, `NotificationChannelTest`; reminder screenshot `notebooklm/screenshots/05_01_notification_shade.png` | verified emulator |
| 2 | For patients 0 to 18 and their guardians; age groups child up to 12, teen 13 to 18, adult 19 and over (01, 03) | CLAUDE.md 1 and 5; `AgesTest` | verified emulator |
| 3 | Several children on one phone, each with their own points, level and streak; chips filter one child or all (01, 03) | `RepositoryDataTest`, `RepositoryRewardsTest` (control patient), `ResponsiveTest`; `08_01_home_several_children.png` | verified emulator |
| 4 | Name Koko Dawa after the rooster; first version was Smouha Kids; 1.1.0 updates it with data kept (01, 05) | `docs/RELEASE.md`; `docs/evidence/batch7/item1/update_test.txt` | verified emulator |
| 5 | The name was changed at the doctor's request (01) | `docs/PROGRESS.md` batch 6 ("requested by the doctor") | design (project record) |
| 6 | Reminder tool only: no medical advice, no doses, no check of names, doses or interactions (01, 04) | CLAUDE.md 2 and 9 (About), `BrandTextTest`; `10_01_about.png` | design |
| 7 | Independent project, not an official product; the use of the hospital's name and logo needs its approval (01, 07) | README; CLAUDE.md 1 | design (open decision) |
| 8 | Hospital logo on the first screen, the home header and About (01) | `HospitalLogoUiTest`; `04_01_language_picker.png`, `10_01_about.png` | verified emulator |
| 9 | Colors sky blue, sunny yellow, orange (01) | `docs/palette-v2.md`, `ContrastTest` | verified emulator |
| 10 | First screen: the logo, the hospital's name in Arabic and English, the two language buttons; the language can be changed later in parent settings (02) | `LocalizationTest`, `AppLanguageTest`; `04_01_language_picker.png` | verified emulator |
| 11 | Parent PIN 4 to 8 digits, typed twice, on the app's own keypad (02, 03) | `PinEntryTest`, `PinSheetUiTest`; `04_02_pin_sheet.png` | verified emulator |
| 12 | The PIN is asked to confirm a dose (can be turned off), to add, edit and delete medicines, and for change PIN, alarm sound, celebration sound, erase all data (02, 03) | `SettingsGatesUiTest` (12 tests), `SettingsAction` | verified emulator |
| 13 | 5 wrong tries lock entry for a minute, also after the app restarts (03, 04) | `PinTest`, `RepositoryPinTest`, `PinSheetUiTest` | verified emulator |
| 14 | Forgot PIN needs the phone's own screen lock; data stays; no screen lock means the app asks for one first (03, 04) | `RepositoryPinTest`; phase 3 evidence in `docs/PROGRESS.md` | verified emulator |
| 15 | The PIN is not stored as the number (04) | CLAUDE.md 8 (salted PBKDF2 hash), `RepositoryPinTest` | verified emulator |
| 16 | Add medicine: who it is for (name, age, male or female), medicine name, every day or chosen days, one or more times, different times per day, optional start and end dates, follow-up interval 5/10/15/30 minutes (02, 03) | `ScheduleTest`, `RepositoryDataTest`; `04_03_add_child.png`, `04_04_add_medicine_days_times.png`, `06_02_weekday_schedule.png` | verified emulator |
| 17 | The app asks for the notification permission after the first medicine; without it no reminder appears (02) | `docs/print/shots/u08_allow_reminders.png`, `u09_android_permission.png` (batch 7 run) | verified emulator |
| 18 | The alarm sound plays up to 30 seconds even on silent or vibrate, with "Stop sound" that only silences (02, 03) | `AlarmSoundTest` (10 tests), `SoundPolicyTest` | verified emulator |
| 19 | The same on a real phone (silent, vibrate) (06) | `docs/PILOT.md` section 4: 17 checks pass on an old test build, model not recorded | verified real phone (old test build only) |
| 20 | Up to 3 follow-up reminders; they stop once confirmed (02, 03) | `RepositoryReminderTest`, `ReminderRulesTest` | verified emulator |
| 21 | Tapping the reminder opens that exact dose's confirm screen (02) | phase 2 evidence; `05_02_confirm_screen.png`; `video/flow.mp4` | verified emulator |
| 22 | Confirming stops the alarm sound (02) | `AlarmSoundTest` | verified emulator |
| 23 | Points +10, +5 within 30 minutes, +20 perfect day, +50 at 3/7/14/30/60/100 days, a level per 100 points, never deducted (03) | `RewardsTest`, `RepositoryRewardsTest` | verified emulator |
| 24 | Rewards by age: child and teen a celebrating character with confetti and a short happy sound, teen calmer with less confetti; adult a calm checkmark without cartoon or sound (02, 03) | `CelebrationUiTest`, `CelebrationSoundTest`, `SoundPolicyTest`; `07_01` to `07_03` screenshots | verified emulator (how loud: under trial) |
| 25 | The happy sound can be turned off in parent settings with the PIN; it changes no phone volume; it stops when a reminder rings (03) | `SettingsGatesUiTest`, `CelebrationSoundTest` (player event log) | verified emulator |
| 26 | The reward closes by itself after a few seconds or with a tap (02) | `CelebrationUiTest` | verified emulator |
| 27 | A missed day restarts the streak with "ولا يهمك، نبدأ من جديد!" and the capsule character sleepy then happy, shown once, no points removed; a day without medicines changes nothing (03) | `RewardsTest`, `MascotTest`, `CelebrationUiTest`; `07_04_streak_broken_child.png` | verified emulator |
| 28 | End of a medicine with an end date: after the reward a short video of a celebrating child and the message that the recorded schedule is complete; never "the treatment is finished"; closes with the close mark, a tap outside or Back; its sound follows "Celebration sound" (02, 03, 04) | `CourseEndTest`, `CourseEndUiTest`; `07_05_course_end.png`; `docs/evidence/batch7/video/` | verified emulator |
| 29 | Arabic first in Egyptian wording, right to left, full English, Western digits (03) | `LocalizationTest`, lint `MissingTranslation` | verified emulator (native review: under trial) |
| 30 | Small phones, tablets, landscape, font up to double, dark mode (03) | `ResponsiveTest`, `HospitalLogoUiTest`; `09_01_*`, `docs/screenshots-v1.1/` | verified emulator |
| 31 | Data only on the phone; no internet permission; no account, ads or analytics; cloud backup off (04) | `docs/evidence/batch7/item1/verify_dist_1.1.0.txt` (no INTERNET, allowBackup false); CLAUDE.md 2 | verified emulator (file check) |
| 32 | The permissions list and what is not requested (04) | `verify_dist_1.1.0.txt` (aapt permissions) | verified emulator (file check) |
| 33 | Data does not move to a new phone; uninstalling deletes it; "Erase all data" exists (04, 05) | `RepositoryEraseTest`; allowBackup false | verified emulator |
| 34 | A child who knows the phone's screen lock can reset the PIN; changing the clock can affect counting (04) | CLAUDE.md 8 (known limits) | design (documented limit) |
| 35 | Shared as an app file through a Google Drive link, on WhatsApp or a printed card with a QR code (05) | `dist/DRIVE.md`, the card (QR read back with a test link) | design (Drive upload: manual, under trial) |
| 36 | Browser and Play Protect warnings and the steps past them; Samsung's extra protection (05) | `docs/print/install-guide-ar.html`; real-phone screens missing (`docs/guide-shots-needed.md`) | under trial |
| 37 | Updates install over the old version and keep all data; another key cannot update it (05, 06) | `update_test.txt` (1.0.0 to 1.1.0, other key refused) | verified emulator |
| 38 | Android 9 or newer only; no iPhone (05) | minSdk 28 in `verify_dist_1.1.0.txt` | verified emulator (file check) |
| 39 | Battery settings matter on some phones; parent settings has a battery button; test reminder (05) | `SoundStatusTest`; `10_02_settings_sound_rows.png` | verified emulator (brand behavior: under trial) |
| 40 | Support contact not decided (05, 07) | `<TO BE FILLED>` in every document | open placeholder |
| 41 | 131 unit and 131 instrumented tests passed on 2026-10-19 (06) | `docs/evidence/batch7/suite/suite.txt` | verified emulator |
| 42 | Version 1.1.0 not tested on a real phone (06, 07) | `docs/PILOT.md` second results table empty | true (statement of a gap) |
| 43 | The pilot proposal, its numbers and dates (07) | placeholders only | open placeholders, no invented values |

Removed or never written (no evidence): any adherence or improvement number, any approval or recommendation by the hospital, any comparison with other apps, any clinical claim, any real name (the sources use the fictional "نور").
