# CHANGELOG of the contract (CLAUDE.md)

One entry per change request: date, request, sections edited, before/after of every changed rule, decisions. Newest first.

## 2026-10-19, batch 7: Koko Dawa 1.1.0 deliverables and GitHub access

Request: rebuild the APK and every deliverable for Koko Dawa 1.1.0 (signed `Koko-Dawa-1.1.0.apk`, the release pack of batch 4, the presentation pack of batch 5, a leftover search), then publish to the private GitHub repository. It explicitly changes the Hard rule "never push / never create remotes": GitHub access is allowed only under its G0 and G1 rules. G0 and G1 wait for Git and `kh.env` (BLOCKED in PROGRESS); everything else is done locally.

| Section | Before | After |
|---|---|---|
| 0 step 4 (snapshot) | one local commit on branch `autonomous-run` if git exists | local commits on the change request's feature branch once the repository is set up |
| 0b Forbidden | `git push`, `git commit --amend`, force, rebase, merge or touching remotes | `git commit --amend`, force, rebase, merging pull requests, any GitHub action beyond section 2 "GitHub access" |
| 2 Hard rules | Do NOT run `git push`, `git commit --amend`, `git push --force`, or change any remote | No amend, rebase or force push; GitHub only under the new "GitHub access" subsection: the one private repository, the token rules (`kh.env`, never printed or stored), ignore rules and `.gitattributes` before staging, the privacy check, the pre-push scan over the full history (counts only), the allowed actions (fetch, pull, push to `main` for the baseline and to feature branches, pull requests, issues and labels, a DRAFT release) and the forbidden ones (force push, deleting or rewriting, settings, Actions, merging, other repositories, tokens or keys) |
| 10 Versions | the 1.0.0 pack describes 1.0.0 until rebuilt | the 1.1.0 pack rebuilt in batch 7; the 1.0.0 pack kept in `../tracker-snapshots/dist-1.0.0/` |
| 10 Checks, Distribution | `Smouha-Kids-<v>.apk`, `dist/Smouha-Kids-App/` | `Koko-Dawa-<v>.apk`, `dist/Koko-Dawa-App/` |
| 10 GitHub | linked only by the user | the repository `Smouha-kids` and its pull-request flow (`docs/GITHUB.md`), with `kh.env`, `dist/` and the raw originals left out |
| 11 Environment | git not on PATH; install it or use Android Studio | git and gh not on PATH and no `kh.env` (2026-10-19): GitHub access waits for the user |
| 11 (credit line) | the original's repository link | the plain project name (the link carries a real name, which the pre-push scan forbids) |

Files: `.gitignore` (kh.env, `*.env`, `dist/`, the raw originals), `.gitattributes` (new), the release scripts (Koko-Dawa names), `docs/print/` (Koko palette in `print.css`, the icon, the hospital logo in the handoffs, the 1.1.0 texts, two new guide sections), `docs/GITHUB.md` (rewritten), `docs/guide-shots-needed.md` (re-capture), README, `notebooklm/` (new), `scripts/make-composites.ps1` (new). Decisions D43 to D45.

## 2026-10-19, user request: the "avatar of finish" video replaces "avatar of end"

Request: replace `avatar of end.mp4` with the new `avatar of finish.mp4` in the app, then repeat batch 7 from the start.

| Section | Before | After |
|---|---|---|
| 9 Course-complete screen | the "avatar of end" video (`avatar_end.mp4`, 9:16) | the "avatar of finish" video (`avatar_end.mp4`, 480x854, 9:16, 6 s), its 2 black closing frames trimmed so the held last frame is the smiling child |

Files: `design/brand/avatar-finish.mp4` (exact copy), `app/src/main/res/raw/avatar_end.mp4` (re-encoded), `CourseEndLayout.VIDEO_ASPECT` 576/1024 to 480/854, `docs/assets-report.md` (new video section, the old one marked replaced). Decision D42. Checked: the frames looked at (`docs/evidence/batch7/video-frames/`), every frame of the app's file measured non-black, `CourseEndUiTest` 6 of 6, `CourseEndTest` and `ContrastTest` passed, the screen on the test AVD in English, Arabic, dark and landscape (`docs/evidence/batch7/video/`).

## 2026-10-19, batch 6, item 7: CLAUDE.md, review sheet, screenshots, update test

Request: update CLAUDE.md in place (identity, assets and the transparency rule, palette v2 tokens, onboarding and header logo, celebration sound, streak-broken avatar, end-of-course rule, test matrix), CHANGELOG, ar-review.md, FINAL_REPORT (verified and unverified, every BLOCKED asset and what the user must provide); screenshots into `docs/screenshots-v1.1/`; the update test 1.0.0 to 1.1.0 with the same key and the data intact. Not regenerated: `dist/`, the PDFs, the notebooklm pack.

CLAUDE.md sections edited (rewritten in place, not appended):

| Section | Before | After |
|---|---|---|
| Title | Smouha Kids | Koko Dawa / كوكو دوا, for the patients of Smouha University Children's Hospital |
| 0c App name | the full hospital name as the application label, "Smouha Kids" on the launcher | Koko Dawa / كوكو دوا on both; the hospital's name a separate string |
| 1 Identity | full and short names; "no hospital logo ... only if design/brand/logo exists, splash and About only" | Koko Dawa; `hospital_name` only where it names the hospital; the rooster icon; the splash from the launcher icon on `koko_sky`; the hospital logo on onboarding, the header and About; no endorsement; the certificate keeps `CN=Smouha Kids` |
| 1 Brand assets (new) | | `design/brand/` as the source of truth; crop, scale, place, re-encode only; transparency only from real alpha or a flat background; a checkerboard is never used or keyed: BLOCKED, the screen keeps its visual |
| 5 Age groups | CHILD: wiggling cheering mascot; TEEN: simple badge | CHILD and TEEN: the celebration avatar springing in, count-up, streak and the reward sound; ADULT no cartoon, no sound |
| 5b Schema | versions in general | current version 4, what v2, v3 and v4 added |
| 5c End of a course (new) | | the final-dose rule (`CourseEnd`) and when the course-complete screen shows |
| 6b CHILD streak broken | sleepy mascot | the same, plus: the "avatar no streak" picture replaces it once usable; BLOCKED now |
| 8 PIN-gated settings | ... the alarm sound option, erase all data | ... the alarm sound option, the celebration sound option, erase all data (explicitly requested as a PIN-gated row) |
| 9 Design tokens | "Calm and Growth": green, blue, teal; green to blue gradient; white capsule icon with a monochrome layer | palette v2 "Koko": rooster orange, sunny yellow container, sky blue; sky, cream, yellow three-stop gradient; the rooster icon, no monochrome layer |
| 9 Contrast | both gradient ends | every stop, glass over every stop, 3:1 for icons, borders and the level bar, drawables paint the checked stops |
| 9 Semantic colors | amber words for warnings | the bordered amber warning box with a triangle |
| 9 Rewards, mascot | CHILD reward cheering mascot, TEEN badge | the avatar for CHILD and TEEN; the mascot for the CHILD streak end and the empty state |
| 9 Reward time and sound (new) | "the 3 second celebration" | max(3 s, min(sound, 6 s)), the sound's rules, one sound at a time |
| 9 Course-complete screen (new) | | presentation, copy, sound, lifecycle, fallback, closing |
| 9 Hospital logo (new) | | the white tile, the three places, the header's heading |
| 9 Any screen size | the header's full or short name (`fullTitleFits`) | the header's fixed logo tile, never over the settings button |
| 9 About | "no logo unless design/brand has one" | the logo large, no endorsement wording |
| 10 Versions | | 1.1.0 (4) is the Koko Dawa identity and schema v4; the 1.0.0 pack is not rebuilt yet |
| 11 Environment | | PowerShell quoting with adb, array flattening, the AVD freeze and software rendering, idle waits in UI tests, unit test resource inputs, the player event log of dumpsys audio |
| 14 Test matrix | | migrations to v4, avatar and mascot, reward sound, course complete, palette and logo tests, warning rows, the rooster icon tests, identity, the update test across the new column |

Review sheet: `docs/ar-review.md` regenerated against `../tracker-snapshots/batch4-final`: 296 rows, 10 strings new or changed in batch 6, none flagged as second person, 0 missing.

## 2026-10-19, batch 6, item 6: end of course (R6)

Request: when the child confirms the FINAL scheduled dose of a medicine with an end date (the last occurrence inside its start/end range: the end day's dose, only the second of two on that day, or the last weekday dose before an end day without one; no end date never), the "avatar of end" video is shown after the reward closes; no schema or points change. Presentation decided from the frames: a rounded card the color of the frame edges, sized to the video's aspect ratio, centered, at most 80 percent of the screen width and height in portrait, landscape and on tablets, a close button from the first second, plays once then holds the last frame with the text below, muted without an audio track else its own audio on the media volume following "Celebration sound", pauses in the background, released on close, restarts after rotation, the celebration avatar as a still if playback fails, no PIN, never blocking. Copy AR "الجدول المسجل لدوا {medicine} خلص. برافو!", EN "The recorded schedule for {medicine} is complete. Bravo!". Decision D41. CLAUDE.md 5c, 9 and 14 are rewritten in item 7.

| Where | Before | After |
|---|---|---|
| Rule | (none) | `CourseEnd` (pure): the course's last dose is read from the schedule with the same rule that creates doses (`ScheduleRules.producesDose`), within the last 7 days of the range; `isFinalDose`, and `celebrates` only for a confirmed dose. `TrackerRepository.isFinalDoseOfCourse` reads it for a dose (no write) |
| Home | the reward, then a sibling's streak-broken screen | the reward, then the course-complete screen when the confirmed dose was its course's final dose, then a sibling's streak-broken screen; the waiting screen survives a rotation |
| Course-complete screen | (none) | `CourseEndScreen` with `view_course_end.xml`: the video (`avatar_end.mp4`) in a TextureView (rounded corners clip it, a SurfaceView would not be), in a navy card `course_card` #1E2A44 with white text `on_course_card`, sized by `CourseEndLayout` (the tallest 9:16 video that leaves room for the whole text, the text at least 300dp wide, all within 80 percent of the screen); a close button on a dark round patch from the start; once, then the last frame stays; USAGE_MEDIA with ducking focus, muted with the setting off, without an audio track, while an alarm rings (the alarm service silences it first) or after losing focus; pauses in the background and goes on where it was, continues after a rotation (a finished video shows its last frame again), released on close; the celebration avatar still with the same text if the player fails; a tap outside the card, the close button or Back closes it |
| Strings (EN, AR) | | `course_end_message`, `course_video_desc` |
| Tokens | | `course_card`, `on_course_card`, `course_close_bg` (no dark values: the video's colors do not change) |
| Tests | | CourseEndTest (unit, 9: a daily course on the end day; two doses on the last day; a weekday course ending before the end date; no end date; the end date extended; a missed final dose; another medicine and another patient as controls; no dose in range or a stopped medicine; ISO weekdays), ContrastTest (the card's text and the close icon over the brightest and darkest frame, no dark values), CourseEndUiTest (instrumented, 6: the final dose shows the screen once after the reward with the exact text, the close button and the video playing, closing releases the player, confirming again shows nothing; the first of two doses on the last day shows nothing and the second does; a course that goes on shows nothing; the setting off mutes the video; a forced failure shows the avatar still with the text; a rotation keeps the screen and its video, the card within 80 percent and the video at 9:16 in both orientations) |

## 2026-10-19, batch 6, item 4: celebration avatar and sound (R3)

Request: CHILD and TEEN rewards use the celebration avatar with a spring bounce, confetti, the count-up and the streak; ADULT unchanged; the dose sound plays once when the reward appears, only with the new parent setting "Celebration sound" on (default on, PIN-gated); USAGE_GAME or ASSISTANCE_SONIFICATION, never a volume change, stopped and released on close; the reward lasts max(3 s, min(sound length, 6 s)) with a fade if cut, overriding D22; the alarm stops first, then the celebration sound, never both; unit tests for the duration rule and the sound policy, emulator evidence from dumpsys audio, audibility UNVERIFIED. Schema v4 approved by the user (PROGRESS, batch 6). Decision D40 (supersedes D22). CLAUDE.md 5, 5b, 7, 8, 9 and 14 are rewritten in item 7.

| Where | Before | After |
|---|---|---|
| CHILD reward | the cheering capsule mascot (vector) with a wiggle, confetti, count-up, streak | the celebration avatar (`avatar_celebration`, a rounded picture, `ShapeableImageView`) springing in (`Motion.springPop`), confetti, count-up, streak |
| TEEN reward | the 🏅 emoji badge, short confetti, points without count-up, streak | the celebration avatar springing in, short confetti (as before), count-up, streak |
| ADULT reward, every streak-ended screen, the empty state | | unchanged (checkmark; the capsule mascot for a CHILD's streak ended and the empty state) |
| Reward sound | none | `dose_sound.mp3` once, `CelebrationSound`: USAGE_GAME (media volume), audio focus that lets music duck, no volume ever changed, stopped and released when the reward closes, when a new overlay replaces it, when the screen stops (background, rotation, closing); not played again after a rotation; CHILD and TEEN only, with the setting on, and only while the reward is on screen |
| Never two sounds | (only the alarm sound existed) | `SoundPolicy.celebration`: the confirmed dose's ringing alarm is stopped first and the reward waits for it (at most 1.5 s); while another reminder rings the reward stays silent; `AlarmSoundService` stops the reward's sound before it rings |
| Reward time | 3 s for everything (D22) | a reward stays max(3 s, min(sound length, 6 s)) from the moment its sound starts, with a 0.5 s fade when a longer sound is cut at 6 s (the dose sound is 2.77 s, so 3 s); without a sound and for every streak-ended screen 3 s; a tap still closes it |
| Parent settings | | section "Rewards" / "المكافآت" with the switch "Celebration sound" / "صوت الاحتفال" and a one-line description; PIN asked every time (`SettingsAction.CELEBRATION_SOUND`, needsPin) |
| Database | v3 | v4: `app_settings.celebrationSoundEnabled INTEGER NOT NULL DEFAULT 1`, `Migrations.MIGRATION_3_4`, exported `app/schemas/.../4.json`; no other change |
| Removed | `mascot_cheer.xml`, `Mascots.ART_CHEER`, `Mascots.TEEN_BADGE` | no longer shown anywhere (MascotTest checks the two remaining expressions) |
| Strings (EN, AR) | | `section_rewards`, `celebration_sound`, `celebration_sound_desc`, `celebration_avatar_desc` |
| Tests | | SoundPolicyTest (+6: who plays, no sound without the screen, never with an alarm, 3/3/4.5/6/6 s, the fade, the "no alarm" id), MigrationTest (v3 to v4 with every row equal, the alarm sound choice and genders kept; v1 to v4), SettingsActionTest, SettingsGatesUiTest (+2: wrong PIN keeps it, right PIN turns it off and leaves the alarm sound), CelebrationUiTest (avatar for CHILD and TEEN, checkmark for ADULT, 3 s), new CelebrationSoundTest (9, dumpsys audio: plays once and is released; TEEN too; nothing with the setting off or for an adult; a tap stops it; no replay after a rotation; with animations off it still plays once and closes; the confirmed dose's alarm stops first; silent while another reminder rings; an alarm during the reward cuts it short; "never both" read to the millisecond from the player event log of dumpsys audio). Mutation checks: 10 planted bugs, all caught by the final tests (`docs/evidence/batch6/item4/mutation_check.txt`) |

## 2026-10-19, batch 6, item 3: hospital logo (R4, R5)

Request: on onboarding the red and yellow capsule becomes the hospital logo on a white rounded tile and the text under it stays exactly the hospital name (Arabic and English); in the home header the logo tile replaces the app name, keeping "Today: x of y", the chips and the settings button, on small screens, in RTL, in landscape and at font 2.0, with a content description; every other capsule stays; About shows the logo large and keeps the disclaimer and the data statement; nothing implies that the hospital endorses the app. Decision D39. CLAUDE.md 1 and 9 are rewritten in item 7.

| Where | Before | After |
|---|---|---|
| Onboarding | the capsule emoji (64sp) above the hospital name in Arabic and English | the hospital logo (96dp, 128dp from 640dp of height) on a white rounded tile (`logo_tile`, 28dp corners), described as "Smouha University Children's Hospital logo" / "شعار مستشفى سموحة الجامعي للأطفال"; the two names under it unchanged (`hospital_name`) |
| Home header | "Koko Dawa" as text (22sp, the screen's heading) | the hospital logo (56dp) on a white rounded tile, the screen's heading, described as "Koko Dawa. Smouha University Children's Hospital logo" (a screen reader still names the app); today's summary, the stat boxes, the chips and the settings button unchanged |
| About | the rooster in a sky circle (88dp, decorative) | the hospital logo, large (168dp), on a white tile with its description; name, version, intro, disclaimer and data statement unchanged; `bg_icon_koko.xml` removed |
| Kept | | the pill on dose cards (`Mascots.PILL`), the notification small icon, the capsule mascot |
| Strings (EN, AR) | | `hospital_logo_desc`, `home_logo_desc` |
| Tokens, dimens | | `logo_tile` #FFFFFF (no dark value: the logo is drawn on white), `onboarding_logo_size`, `header_logo_size`, `about_logo_size` |
| Tests | | `HospitalLogoUiTest` (instrumented, 4): onboarding shows exactly the logo bitmap on a white tile, both names exact, no capsule emoji left; the header logo is a heading with its description, no app-name text, the summary under it, logo and settings button inside the header and apart (English; and Arabic at 480x800, font 2.0, portrait then landscape, logo on the right); About shows the logo at 160dp or more with the name, disclaimer and data texts. `BrandTextTest` (unit, 3): no English or Arabic text claims an endorsement (official, approved, endorsed, recommended, معتمد, رسمي, بموافقة, برعاية and more), the pattern itself catches those words, the tile is white with no dark value |

Endorsement: no text says or implies that the hospital made, approved or recommends the app; the About intro keeps "A medicine reminder for the patients of Smouha University Children's Hospital and their families" (who it is for).

Found and fixed (build): the unit tests that read resource files (ContrastTest, MascotTest, IconContrastTest, BrandTextTest) were not re-run by Gradle when only a resource changed, because those files were not inputs of the test task: a planted endorsement string passed as "up to date" with the old result. `app/build.gradle.kts` now declares `src/main/res` as an input of every unit test task; the same mutation then failed the test (`docs/evidence/batch6/item3/mutation_check.txt`).

## 2026-10-19, batch 6, item 2: palette v2 (R8)

Request: a new Material 3 palette from the Koko pictures (primary the rooster orange family, secondary the sunny yellow with dark text, tertiary the sky blue, surfaces light sky or warm cream, the header gradient light sky blue to soft yellow instead of green to blue, light and dark, dynamic color off); contrast stays a hard rule (4.5:1 text, 3:1 large text and icons, both gradient ends) with a mutation check; red stays semantic; warnings restyled if amber blends with the new primary; status never by color alone; `docs/palette-v2.md` and `docs/palette-v2.png`; leftover green, blue and teal tokens listed here. Decision D38. CLAUDE.md 9 (design tokens) is rewritten in item 7.

| Where | Before | After |
|---|---|---|
| Palette (light) | "Calm and Growth": primary green #166534, secondary blue #1D4ED8 (container #DBEAFE), tertiary teal #0F766E, page and surfaces slate #F8FAFC / #F1F5F9 | "Koko": primary #A84A0A (the rooster orange's deep tone, white text 5.8:1), secondary container the sunny yellow #FDC440 with #3B2800 text (secondary #7A5700), tertiary #0B6A86 with container #D4EEF7, page light sky #F4FAFD, cards, sheet and dialogs warm cream #FFFCF6, reward card white |
| Palette (dark) | primary #86EFAC, secondary #93C5FD, tertiary #5EEAD4, slate surfaces | primary #FFB37A, secondary #F6C547 (container #574000), tertiary #82D2EC, sky-tinted navy surfaces #0F1A21 to #24343F |
| Header gradient | green-400 #4ADE80 to blue-500 #3B82F6 (dark #166534 to #1E40AF) | light sky #BEE5F4, cream #FFF6E0, soft yellow #FFDD80 (dark #0B5577, #1C2E3D, #6B4A0E, white text): three stops, because a straight sky-to-yellow blend turns grey-green in the middle. New tokens `gradient_center`, `gradient_scrim_center`; the header, onboarding, FAB, PIN badge, status bar scrim and celebration backdrop drawables use all three stops |
| `glass` (light) | #40FFFFFF | #66FFFFFF (the boxes stay visible on the lighter header) |
| `progress_on_gradient` | near-black (light), white (dark) | primary orange #A84A0A (light), #FFB37A (dark) |
| Warnings | amber words #B45309 on the row | a bordered amber box with a warning triangle and dark text, an 8dp gap to the button under it (`bg_warning_row`, `ic_warning`, new tokens `warning_container`, `on_warning_container`; `warning` #8A5300 colors the icon and the border): amber lies between the brand's orange and yellow, so amber words looked like a section title |
| `points` | teal #115E59 (dark #5EEAD4) | sky #0B6A86 (dark #82D2EC) |
| `shadow_soft` (light) | darkest green #14532D | darkest sky #073A4C |
| Confetti (`ConfettiView`) | blue-500, orange, green, yellow, pink, blue | sampled from the pictures: rooster orange, sunny yellow, the platform's sky blue, lime and deep orange, plus the old pink |
| `ContrastTest` | text pairs 4.5:1, both gradient ends, glass over both ends | also the warning box text, every gradient stop (the center too), glass over every stop, 3:1 for the outline on every surface, the warning icon and border, the level bar (on every stop and on the glass); new: the gradient drawables paint exactly the three checked stops, the backdrop stops equal the gradient stops. Mutation checks: 4 of 4 caught (`docs/evidence/batch6/item2/mutation_check.txt`) |
| `scripts/make-palette-sheet.ps1` | (none) | writes `docs/palette-v2.md` and `docs/palette-v2.png` from the tokens (the same pairs and formula as ContrastTest) |
| `scripts/screenshots.ps1` | finds the reward overlay by the brightness of the left page margin | by any color channel at both margins: the light backdrop is as bright as the page at the left edge (cream on light sky) |

Unchanged: the token names and their mapping in `themes.xml`, dynamic color off, the semantic colors `error` (red, never a brand color; the comb red #F15958 is not used) and `success`, `streak` (red, as before), the `koko_*` and `splash_name_text` tokens of item 1, the mascot tokens.

Leftover green, blue and teal (scan of `app/src/main`: hex colors in hues 75 to 250 and the words green, blue, teal):
- `success` #166534 / #86EFAC: semantic (a dose marked taken, always with the word), kept.
- `mascot_top` #F0FDF4, `mascot_bottom` #4ADE80, `mascot_line` #14532D, `mascot_cheek` #99F6E4, `mascot_spark` #14B8A6 and the comment in `mascot_happy.xml`: the green capsule mascot of the empty state and the CHILD streak-ended screen, kept (the request keeps the streak-broken visuals while the no-streak avatar is BLOCKED; the CHILD reward gets the celebration avatar in item 4). Listed for the doctor in FINAL_REPORT.
- Every other blue is the new Koko sky family (`tertiary`, `points`, the gradient start, the dark surfaces, `koko_*`, `splash_name_text`), sampled from the pictures. No other green, blue or teal brand token is left.

## 2026-10-18, batch 6, item 1: name and icon (R1, R2)

Request: application and launcher label كوكو دوا / Koko Dawa, applicationId unchanged, versionName 1.1.0 with a higher versionCode; list every hit of "Smouha Kids", "سموحة للأطفال", "Pill Pals"; the hospital name stays where it names the hospital; the Koko rooster as the adaptive launcher icon (no checkerboard source), monochrome only if clean, old launcher images removed; notification small icon unchanged, large icon the rooster.

| Where | Before | After |
|---|---|---|
| `app_name` | EN "Smouha University Children's Hospital", AR "مستشفى سموحة الجامعي للأطفال" (application label) | EN "Koko Dawa", AR "كوكو دوا" (application label; the launcher activity has no label of its own and inherits it) |
| `app_name_short` | "Smouha Kids" / "سموحة للأطفال" (launcher label) | removed |
| `hospital_name` | (none) | new, the old hospital text in both languages: the onboarding names under the logo and About |
| Home header | full hospital name, the short name when tight (`Responsive.fullTitleFits`) | "Koko Dawa" (the rule and its ResponsiveTest case removed: one short name now); the logo replaces it in item 3 |
| Launcher icon | white capsule on a green to blue gradient, monochrome layer | the rooster from koko.jpg over a gradient of its sky blue (`ic_launcher_koko`, `ic_launcher_koko_background`), no monochrome layer (D36); `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `bg_icon_badge.xml` removed |
| Splash | white capsule on green-600, the hospital name in white (Android 12+) | the adaptive launcher icon (a round badge) on `koko_sky`, the name "كوكو دوا" / "Koko Dawa" in dark navy `splash_name_text` (`render-splash-name.ps1` reads the color token) |
| Notification | small icon the capsule | small icon unchanged; large icon the rooster (`notification_large_koko`) |
| Color tokens | `icon_gradient_start`, `icon_gradient_end`, `icon_symbol` | removed; `koko_icon_bg_top` #C3E4F0, `koko_icon_bg_bottom` #E5F8FF, `koko_sky` #D4EEF7, `splash_name_text` #12304A |
| Version | 1.0.0 (1) | 1.1.0 (4): above the release (1), the update test's 1.0.1 (2) and the debug pilot (3) |
| `scripts/verify-dist.ps1` | the old names hard-coded | expected names read from `strings.xml` |
| Tests | LocalizationTest (full and short name), IconContrastTest (white symbol 3:1), LauncherIconTest (white symbol in the safe zone, monochrome kept) | LocalizationTest (Koko Dawa as both labels, hospital_name apart), IconContrastTest (rooster layers, no monochrome, sampled gradient, old files gone, splash name 4.5:1 and the rendered image in that color), LauncherIconTest (every rooster pixel within 33 dp, no themed layer, masked renders) |

Every hit of the three names (outside build folders and evidence):

| Hit | What it became |
|---|---|
| `values/strings.xml`, `values-ar/strings.xml`: `app_name_short` | removed (see above) |
| `AndroidManifest.xml`: launcher label `@string/app_name_short` | removed (inherits `app_name`) |
| `LocalizationTest.kt`: the two names | Koko Dawa and `hospital_name` |
| `scripts/verify-dist.ps1`: the two launcher names | read from `strings.xml` |
| `README.md` (title, intro, Arabic paragraph, battery step) | Koko Dawa / كوكو دوا, "until 1.0.0 it was called Smouha Kids" |
| `docs/RELEASE.md` (title, certificate line, uninstall path) | title Koko Dawa with a note; the certificate keeps `CN=Smouha Kids` for good (a certificate cannot change without a new key, which could never update the app); the uninstall path names "Smouha Kids" as those old builds show it |
| `docs/PILOT.md` (title, 3 steps, the "Pill Pals" line) | a note on top: from 1.1.0 the phone shows Koko Dawa; the 1.0.0 steps keep the name 1.0.0 shows; "Pill Pals" is the pre-batch-3 app and stays |
| `app/claude.md` (title, 0c, 1) | rewritten in item 7 |
| `docs/FINAL_REPORT.md` (title, batch 3 and 4 rows, manual step), `docs/GITHUB.md` (example commit message), `docs/ar-review.md` (`app_name_short` row) | item 7 (the report's history rows stay as history; ar-review is regenerated) |
| `docs/screenshots/README.md` | kept: it describes the 1.2.0 screenshot set |
| The 1.0.0 release pack: `docs/print/*` (guides, card, handoffs, release notes), `docs/whatsapp-message-ar.txt`, `docs/guide-shots-needed.md`, `dist/` | kept unchanged: the request says not to regenerate dist/, the PDFs or the pack in this batch; they describe 1.0.0 |
| History: CHANGELOG, PROGRESS, DECISIONS, ARCHIVE; the user's `docs/final-plan.md` | kept |
| Internal names (not the three strings but the same identity): class `PillPalsApp`, DB `pill_pals.db`, styles `PillPals.*`, `PILLPALS_TEST_RUNNER`, the key file and alias `smouhakids`, the file name `Smouha-Kids-<v>.apk` and the Drive folder `Smouha-Kids-App` | kept (D32, D34): internal, or part of the release pack that is not regenerated now |

## 2026-10-18, batch 6, item 0: Koko Dawa assets

Request (the doctor's identity batch): copy the assets into `design/brand/` with ASCII names, check alpha and checkerboards, report in `docs/assets-report.md`, prepare the video (H.264 baseline + AAC, max 720p, under 4 MB) and the sound, no new app library, crop/scale/place only. Schema v4 for the celebration sound setting was approved by the user before the work started (PROGRESS, batch 6).

New: `design/brand/` (8 copies), `scripts/make-brand-assets.ps1`, `app/src/main/res/drawable-nodpi/` `ic_launcher_koko.png`, `notification_large_koko.png`, `hospital_logo.png`, `avatar_celebration.jpg`, `app/src/main/res/raw/avatar_end.mp4` (re-encoded), `dose_sound.mp3` (unchanged MP3), `docs/assets-report.md`. BLOCKED: avatar no streak (checkerboard only). Not used: the three "no background" files (checkerboard). Decision D36. The resources are wired into the app in items 1 to 6.

## 2026-10-18, batch 4, end: CLAUDE.md release and distribution

Request: update CLAUDE.md in place with a release and distribution section (applicationId permanence, signing and fingerprint, update procedure, Drive structure), CHANGELOG, FINAL_REPORT; full suite; emulator reset.

| Section | Before | After |
|---|---|---|
| 0b Allowed | gradlew, adb, emulator, sdkmanager, avdmanager, file tools | plus the SDK build tools (apksigner, aapt, d8); for the documents headless Edge or Chrome and Windows' PDF renderer |
| 10 | "Release rules": signed APK, higher versionCode, keystore.properties, throwaway key for checks, README install steps, checks before a release, GitHub by the user | "Release and distribution": identity permanent since 1.0.0; the real key through keystore.properties (never created, copied or printed by a run; never in snapshots); the recorded certificate SHA-256; versions (the debug builds 1 to 3 never update into a release); updates keep every row; checks before sharing (suite, build-release + verify-dist, update-test with another-key refusal, the release main flow, the pilot and native review by the user); the Drive tree and DRIVE.md; documents (wording, Edge PDFs checked page by page, real-phone screens only, the card's QR read back, `<TO BE FILLED>` never shared); GitHub (private, what stays out, scan-names) |
| 11 | git not on PATH | plus: checked again; Python not installed; Windows PowerShell pitfalls (BOM for Arabic scripts, Gradle daemons and pipes, exit codes, Arabic paths and old SDK tools) |
| 14 Release | `assembleRelease` with a throwaway keystore, install, main flow, no INTERNET, allowBackup false | build-release + verify-dist (a debug-signed and an other-key APK fail it), update test, the release main flow; new block "Documents and distribution" (page counts and page checks, the card's QR read back, text scan, build-dist, scan-names) |

Not edited: the Hard rules (section 2), the PIN, points, streak and persistence invariants, app code.

## 2026-10-18, batch 4, items 3 and 4: dist/ and GitHub preparation

Request (3): `dist/Smouha-Kids-App/` arranged exactly as on Drive (parents, hospital, printing, developer), the parents' folder with only what parents need, `dist/DRIVE.md`. Request (4): README for GitHub, no license file ("All rights reserved"), keep secrets, evidence, snapshots and device dumps out of the repo, scan the tracked text files for the real test names, `docs/GITHUB.md` for a private repo, run by the user.

| Where | Before | After |
|---|---|---|
| `dist/` | (none) | `Smouha-Kids-App/` (للأهل, للمستشفى, للطباعة, للمطور) built and checked by `scripts/build-dist.ps1`; `DRIVE.md` |
| README.md | English, parent steps, developer notes | English plus an Arabic paragraph, demo screenshots, permissions table, privacy, disclaimer, "not approved or endorsed by the hospital", "All rights reserved" (license an open decision) |
| .gitignore | IDE files, `local.properties`, `/build`, signing files | plus every `build/`, `.kotlin/`, `docs/evidence/*` (not `tools/`), device dumps, `dist/link.txt`, `.idea/deploymentTargetSelector.xml`, `scripts/scan-names.txt`, `docs/final-plan.md` |
| PROGRESS.md | 6 entries named the user's own test patients | names replaced by "the user's two patients" |

New: `scripts/build-dist.ps1`, `scripts/scan-names.ps1` (names in the ignored `scripts/scan-names.txt`), `docs/print/release-notes-1.0.0.txt`, `docs/GITHUB.md`. Removed: the empty stray root file `keystore.propertie`.

## 2026-10-18, batch 4, item 2: documents

Request: Arabic-first documents in simple Egyptian wording (plural imperative for guardians, no jargon, big type), PDFs from right-to-left HTML with headless Edge or Chrome, every page rendered to PNG and checked: install guide (2 to 3 pages, real-phone screens as marked placeholders until `design/guide-shots/` has them, `docs/guide-shots-needed.md`), user guide (4 to 5 pages, test AVD pictures with fictional data, "reminder only, not medical advice"), WhatsApp message with `{DIRECT_LINK}` and `{FOLDER_LINK}`, A5 card with a QR code from `dist/link.txt` (`scripts/make-card`), hospital handoff in Arabic and English (2 pages each).

| Where | Before | After |
|---|---|---|
| Parents' documents | the README's English install steps only | `2_طريقة_التثبيت.pdf` (3 pages, 11 steps, 11 placeholders), `3_طريقة_الاستخدام.pdf` (5 pages, 15 pictures from the release build) |
| Hospital | (nothing) | `وثيقة_التسليم_عربي.pdf` and `Handoff-EN.pdf`, 2 pages each: purpose, features, permissions and why, privacy, safety limits, install and update, certificate fingerprint, known limits, the devices actually tested, test summary, `<TO BE FILLED>` support line |
| Printing | (nothing) | A5 card `كارت_التثبيت_A5.pdf`, DRAFT until `dist/link.txt` exists; QR code checked by reading it back from the rendered page |
| Sharing | (nothing) | `docs/whatsapp-message-ar.txt` (6 lines) |

New: `docs/print/` (`print.css`, 5 HTML sources, `shots/`), `scripts/make-docs.ps1`, `scripts/make-card.ps1`, `scripts/print-lib.ps1`, `scripts/pdf-to-png.ps1`, `scripts/qr/` (ZXing tool build), `docs/guide-shots-needed.md`, `docs/evidence/tools/settext/` (types Arabic names on the test AVD). Decision D35. No app code or string changed.

## 2026-10-18, batch 4, item 1: release build and verification

Request: versionName 1.0.0, versionCode 1, release signed with the real key, APK `Smouha-Kids-1.0.0.apk`; `scripts/verify-dist` (apksigner and the certificate SHA-256 saved in docs/RELEASE.md, aapt badging, permissions, not debuggable, allowBackup false, size, SHA256.txt); update test 1.0.0 to 1.0.1 with the same key on the test AVD; another key recorded as a failure; the main flow from the release APK.

| Where | Before | After |
|---|---|---|
| `app/build.gradle.kts` | versionCode 3, versionName 1.2.0 (debug key builds) | versionCode 1, versionName 1.0.0, the first release with the real key; test-only override `-PupdateTestVersionCode/-PupdateTestVersionName` (D34) |
| docs/RELEASE.md | how to create the key; check the APK by hand; share "SmouhaKids-1.2.0.apk" | the key as created (no hospital name in the certificate), the recorded release certificate SHA-256, versions, `build-release.ps1`, `verify-dist.ps1`, `update-test.ps1`, the main flow, sharing through `dist/DRIVE.md`, what a lost key means |
| docs/PILOT.md | pilot of the debug 1.2.0 (3) | 0a: uninstall the debug pilot build, install the signed 1.0.0, repeat the key checks; the first results kept, a second table for 1.0.0 |
| Snapshots | robocopy of the whole project without build folders | also without `keystore.properties`, `*.jks`, `*.keystore` (`docs/evidence/tools/snapshot.ps1`) |

New: `scripts/build-release.ps1`, `scripts/verify-dist.ps1`, `scripts/update-test.ps1`, `scripts/pdf-to-png.ps1` (used from item 2 on), `docs/evidence/tools/snapshot.ps1`. Certificate `CN=Smouha Kids, O=Smouha Kids, C=EG`, SHA-256 `FF:0A:9A:1B:D4:09:D3:85:03:76:FF:A2:C8:3D:0F:27:E2:39:4D:7B:2A:B7:3E:D6:9E:16:14:3E:FF:8A:E8:7A`. Decision D34. The CLAUDE.md release section is rewritten at the end of the batch.

## 2026-10-17, batch 3, item 5: docs and screenshots

Request: update CLAUDE.md in place (app identity, age groups, design tokens, matrix), CHANGELOG and ar-review; `scripts/seed-demo` with fictional data; screenshots in `docs/screenshots/`.

| Section | Before | After |
|---|---|---|
| 1, 0c, 5, 9, 9b, 14 | (see items 0 to 4) | updated in place by items 0 to 4 |
| 14 | (no documentation screenshots) | Documentation screenshots: `seed-demo.ps1` (fictional, test AVD only), `screenshots.ps1`, the list of screens and looks, checked by eye in `overview/` |

New: `scripts/seed-demo.ps1` (Nour 7, Karim 15, Salma 40, medicines Vitamin D, Iron syrup, Augmentin 1g, Omega 3; `-Lang ar`, `-Broken child|teen|adult`, `-Empty`; refuses any device but the test AVD and an app without a PIN), `scripts/screenshots.ps1`, `docs/screenshots/` (72 shots, `README.md` index, `overview/`), README link. `docs/ar-review.md` regenerated (no string changed in item 5).

## 2026-10-17, batch 3, item 4: child-friendly design

Request: (this list only) a rounded font if `design/fonts/` has one; 28dp cards and sheets, buttons at least 56dp with fully rounded ends, a larger dose-confirm button, softer shadows; a vector mascot with 3 expressions replacing the emoji mascots in the reward, streak-broken and empty-state screens; friendlier gender-neutral Egyptian copy for rewards and empty states; a small idle motion on the mascot; ADULT stays calm.

| Section | Before | After |
|---|---|---|
| 5 Age groups | CHILD: wiggling/bouncing emoji mascot | CHILD: the wiggling vector mascot (cheering) |
| 9 Design tokens | (no shape, mascot or type rules) | Shapes (28dp, 56dp pill buttons, larger confirm button, tinted low shadows, exceptions), Mascot (3 vectors, where used, idle motion, decorative), Type (system font until `design/fonts/` has one) |
| 9 Any screen size | Emoji mascots live in one constants place | The mascot pictures and the emoji live in one constants place (`Mascots`) |
| 9b Glossary | Empty state: لسه مفيش أدوية. علامة + لإضافة أول دوا | لسه مفيش أدوية هنا! علامة (+) بتضيف أول دوا. |
| 14 Animations | (no mascot checks) | `CelebrationUiTest`, `MascotTest`, mutation checks, screenshots |

Fonts: skipped, `design/fonts/` does not exist (D33). Changed strings (all marked "changed in batch 3" in `docs/ar-review.md`):

| Key | English before / after | Arabic before / after |
|---|---|---|
| `empty_state` | No medicines yet. Tap (+) to add the first one. / Nothing here yet! The (+) button adds the first medicine. | لسه مفيش أدوية. دوس علي علامة (+) لإضافة أول دوا / لسه مفيش أدوية هنا! علامة (+) بتضيف أول دوا. |
| `no_doses_today` | No pills today 🎈 / No pills today. Enjoy the day! 🎈 | مفيش أدوية النهاردة 🎈 / مفيش أدوية النهاردة. يوم حلو! 🎈 |
| `cheer_perfect_day` | Perfect day! / Perfect day, bravo! | يوم مثالي! / يوم مثالي، برافو! |
| `cheers` [2] | Well done! / Yay, well done! | عاش! / الله ينوّر! |
| `cheers` [3] | (unchanged) High five! | هايل! / عاش! |
| `cheers` [4] | Amazing! / Wow, amazing! | تحفة! / يا سلام، تحفة! |
| `cheers` [6] | Great job! / One more done, great job! | إنجاز حلو! / خطوة كمان، هايل! |

New: `mascot_happy`, `mascot_cheer`, `mascot_sleepy` (drawable), colors `mascot_*` and `shadow_soft`, `styles_shapes.xml` (`Widget.PillPals.Button`, `.Tonal`, `.Outlined`, `.Confirm`, `Widget.PillPals.Card`, set in the theme), `Motion.idle` / `stopIdle`, `ids.xml`, dimen `mascot_size` (80dp, 112dp from h640dp), `scripts/seed-demo.ps1` (fictional demo data, test AVD only). Removed: emoji `CHILD_CHEERS`, `PERFECT_DAY`, `CHILD_SLEEPY`, `CHILD_RESTART`, `EMPTY`. Cards 20/16/24dp became 28dp, `bg_soft_card` 28dp, the FAB fully rounded. The empty state now scrolls (NestedScrollView) with room for the "Add medicine" button.

## 2026-10-17, batch 3, item 3: age groups

Request: CHILD 0 to 12, TEEN 13 to 18, ADULT 19 and over (an explicitly allowed edit of the age group rule). The age input stays 0 to 120.

| Section | Before | After |
|---|---|---|
| 5 Age groups | CHILD 12 and under, TEEN 13 to 17, ADULT 18 and over | CHILD 12 and under, TEEN 13 to 18, ADULT 19 and over |
| 14 Rewards and streaks | (age groups only inside the unit test list) | `AgesTest` boundaries 12/13/18/19, aging into TEEN and into ADULT exactly one year later, mutation check |

Code: `AgeGroup.TEEN_MAX_AGE` 17 to 18 (the only age limit in the code; the group is derived, never stored, so no migration). `AgesTest`: boundaries 12/13/17/18/19, `seventeenStaysTeenAtEighteen` (was `seventeenBecomesAdultAtEighteen`), new `eighteenBecomesAdultExactlyOneYearLater`. README rewards line updated. Effect on installed data: an 18 year old patient now sees the TEEN style instead of ADULT; points and streaks are unchanged.

## 2026-10-17, batch 3, item 2: About screen

Request: an About screen that opens without the PIN from parent settings, with the name, versionName, a short EN/AR text, the gender-neutral disclaimer and the local-only data statement; no invented contact details.

| Section | Before | After |
|---|---|---|
| 8 PIN | Free inside settings: ... test reminder, forgot PIN | + About |
| 9 UI | (no About screen) | About screen: full name, installed version, short text, disclaimer, data on this phone only and no internet; no contact details or logo unless given |
| 14 PIN | settings open without PIN | + About opens without a PIN and shows the installed version and the disclaimer |

New: `AboutActivity`, `activity_about.xml`, `bg_icon_badge.xml` (the icon gradient), `SettingsAction.ABOUT` (free), strings `about_title`, `about_intro`, `about_disclaimer_title`, `about_disclaimer`, `about_privacy_title`, `about_privacy` (EN and AR, in `docs/ar-review.md`). The version text is shared with parent settings (`AboutActivity.versionLabel`). Contact details: none given, none shown. Logo: none.

## 2026-10-17, batch 3, item 1: app name

Request: Arabic and English full hospital name as the application label, short name as the launcher label, the full name on the splash, home header (collapsing on small screens and at font 2.0), language picker and About; list every "Pill Pals" hit; no logo unless `design/brand/` has one.

| Section | Before | After |
|---|---|---|
| Title | "CLAUDE.md - Pill Pals (Medicine Tracker)" | "CLAUDE.md - Smouha Kids (medicine reminder, Smouha University Children's Hospital)" |
| 0c | "App name stays "Pill Pals" in both languages." | full name = application label, short name = launcher label, both languages |
| 1 | (no identity rules) | Identity paragraph: hospital, ages 0 to 18, the two names, where the full name shows, splash image, no logo, internal names kept |
| 9 Any screen size | header text never under the status bar | + the header shows the full name in at most 2 lines, the short name under 360dp per font-scale unit |
| 14 Language | first-run language picker | + identity checks and screenshots; the language picker shows both names |

Every hit of "Pill Pals", "pill_pals", "PillPals" (outside build folders and old evidence) and what it became:

| Where | Before | After |
|---|---|---|
| `values/strings.xml` `app_name` | "Pill Pals", not translatable | "Smouha University Children's Hospital"; new `app_name_short` "Smouha Kids" |
| `values-ar/strings.xml` | (no app_name; the English one was used) | `app_name` "مستشفى سموحة الجامعي للأطفال", `app_name_short` "سموحة للأطفال" |
| `notif_rationale_message` EN / AR | "Pill Pals needs to show notifications ..." / "Pill Pals محتاج ..." | "The app needs ..." / "التطبيق محتاج ..." |
| `battery_hint` EN / AR | "Let Pill Pals run ..." / "الأفضل إن Pill Pals يشتغل ..." | "Let the app run ..." / "الأفضل إن التطبيق يشتغل ..." |
| `battery_unrestricted` EN / AR | "Battery: no limits for Pill Pals" / "البطارية: Pill Pals شغال ..." | "... for the app" / "البطارية: التطبيق شغال ..." |
| `values/strings.xml` comment | App name is "Pill Pals" in both languages | identity comment |
| `AndroidManifest.xml` | MainActivity without a label | `android:label="@string/app_name_short"` (application label stays `@string/app_name`, now the full name) |
| `reminders/AlarmSound.kt` | log tag `PillPalsSound` | `SmouhaKidsSound` |
| `reminders/AlarmSoundService.kt` | URI scheme `pillpals` | `kidsreminder` |
| `drawable/ic_launcher_foreground.xml` comment | "Pill Pals: a tilted capsule" | "App icon: a tilted capsule" |
| `README.md` | 7 mentions | "Smouha Kids", title with the full name, hospital and ages in the intro |
| `docs/RELEASE.md` | title, keystore folder `pillpals-signing`, `pillpals-release.jks`, alias `pillpals`, `CN=Pill Pals`, `PillPals-1.0.0.apk` | `smouhakids-*`, alias `smouhakids`, `CN=Smouha Kids`, `SmouhaKids-1.2.0.apk` (no keystore exists yet) |
| `docs/PILOT.md` | 4 mentions and the log tag | "Smouha Kids" / "the app", `SmouhaKidsSound`; the new-app line keeps "Pill Pals" on purpose (it names the old app) |
| `app/claude.md` | title, 0c, the log tag | as in the table above |
| `docs/FINAL_REPORT.md` title | "Pill Pals: final report ..." | "Smouha Kids (formerly Pill Pals): final report ..." |
| `docs/DECISIONS.md` D28 | tag `PillPalsSound` | notes the new tag |
| `scripts/ar-review.ps1` note | "(Pill Pals)" | "(التطبيق)" |
| Kept on purpose | class `PillPalsApp` (and its uses in 4 files), DB file `pill_pals.db` (`AppDatabase`, `ui.ps1`), styles `PillPals.*` / `Widget.PillPals.*` / `ThemeOverlay.PillPals.*` / `ShapeAppearance.PillPals.*` (themes.xml, styles_pin.xml, view_pin_keypad.xml), `PILLPALS_TEST_RUNNER` (run-tests.cmd, build.gradle.kts), history in PROGRESS, CHANGELOG, ARCHIVE, DECISIONS and old evidence | unchanged: internal names, the DB name and class names must not change (request) |

Also new: splash branding image `splash_name.png` (EN, AR) and its generator `scripts/render-splash-name.ps1`; `values-v31/themes.xml`; onboarding shows both names; `Responsive.fullTitleFits`. Logo: `design/brand/` missing, skipped. Not mine, found on disk at the start of batch 3: `empty_state` (EN "Tap (+) ...", AR "دوس علي علامة (+) ...") and `gender_label` ("Gender" / "النوع") were edited outside this work; they are marked in `docs/ar-review.md`.

Decisions: D32.

## 2026-10-17, batch 3, item 0: applicationId com.smouha.kidsreminder

Request: change the applicationId (namespace and package stay), record that it is permanent, fix every use of the old id, note that it is a new app for Android, verify on the test AVD.

| Section | Before | After |
|---|---|---|
| 1 Project goal | "Android app (Kotlin, package `com.example.tracker`) ..." | applicationId `com.smouha.kidsreminder`, permanent after the first real release (D31); namespace and Kotlin package stay `com.example.tracker` |

Every hit of `com.example.tracker` that is not a package or import line, and what it became:

| Where | Before | After |
|---|---|---|
| `app/build.gradle.kts` | `applicationId = "com.example.tracker"`, version 2 / 1.1.0 | `applicationId = "com.smouha.kidsreminder"`, version 3 / 1.2.0 |
| `reminders/AlarmScheduler.kt` | actions `com.example.tracker.action.SLOT`, `REPEAT`, `TEST`, `DAILY` | `com.smouha.kidsreminder.action.*` |
| `reminders/AlarmSoundService.kt` | actions `com.example.tracker.action.PLAY_SOUND`, `STOP_SOUND` | `com.smouha.kidsreminder.action.*` |
| `androidTest/ExampleInstrumentedTest.kt` | asserts package `com.example.tracker` | asserts `com.smouha.kidsreminder` |
| `docs/evidence/tools/ui.ps1` | `$pkg = "com.example.tracker"` | `com.smouha.kidsreminder` |
| `docs/evidence/tools/suite.ps1` | uninstalls `com.example.tracker.test` | `com.smouha.kidsreminder.test` |
| `docs/PILOT.md` | `dumpsys package` / `dumpsys alarm` filters on the old id; version 1.1.0 (2) | new id, version 1.2.0 (3), one line: new app, the old one keeps its data and is removed by hand |
| `docs/RELEASE.md` | `com.example.tracker.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `com.smouha.kidsreminder....` (androidx names it after the applicationId) |
| `app/claude.md` section 1 | package `com.example.tracker` | as above |

Kept on purpose (package or class names, not the app id): `namespace`, `testInstrumentationRunner = "com.example.tracker.TestRunner"`, the custom views `com.example.tracker.ui.CheckmarkView` / `ConfettiView` in `view_celebration.xml`, fully qualified Kotlin names in three tests, every `package`/`import` line. The manifest has no provider, FileProvider authority or other id-based string; notification channel ids (`pill_reminders_*`) are per app and do not contain the id. Old evidence files under `docs/evidence/phase-*`, `batch1`, `batch2` keep the old id as history.

Verified on the test AVD (`docs/evidence/batch3/item0/flow.txt`): the old app uninstalled from the test AVD, clean install of the new id, onboarding, a medicine 3 minutes ahead, the reminder fired on time in the background ("Background started FGS: Allowed ... com.smouha.kidsreminder.action.SLOT"), the app's alarm-stream player started, the tap in the shade opened "Confirm Demo's pill" and stopped the sound, after a reboot the alarms were booked again (7 slots, daily check, next follow-up).

Decisions: D31.

## 2026-10-16, batch 2, items 5 and 6: Arabic review sheet and real phone pilot pack

Requests: (5) generate `docs/ar-review.md` from the two strings files (key, English, Egyptian Arabic, screen, every plural form, second person first, no Arabic changed, new strings of 2c and 4 included); (6) write `docs/PILOT.md` (install with `adb -s <serial> install -r` only, first confirm the installed versionCode, checklist with expected results, results table).

| Section | Before | After |
|---|---|---|
| 9b | "Egyptian copy must be reviewed by a native speaker before release." | + the review sheet `docs/ar-review.md` is generated by `scripts/ar-review.ps1`, run again after any string change, never edited by hand |
| 10 | "Native Arabic review, real phone test and the real keystore are manual steps for the user." | "Native Arabic review (`docs/ar-review.md`), the real phone pilot (`docs/PILOT.md`, which starts by checking the installed versionCode) and the real keystore are manual steps for the user." |
| 14 Language | lint rules | + `scripts/ar-review.ps1` leaves no Arabic missing; its second-person table is read before a release |

Finding for the native reviewer (no Arabic changed, as requested): `broken_adult_title` "أطول سلسلة ليك" addresses the reader as masculine; it is the glossary wording of 9b. `forgot_pin` "نسيت" can read as "you forgot".

## 2026-10-16, batch 2, item 4: PIN panel as a modern bottom sheet

Request: replace the PIN dialog with a Material 3 modal bottom sheet (drag handle, 28dp corners, gradient lock circle, contextual title, 4 dots with a spring, own keypad with haptics and backspace, no system keyboard), the same component for create (with steps), change (old, new, confirm) and the first-launch PIN; wrong PIN, lockout, checking and animations-off behavior; keypad always left to right; all PIN guarantees kept; 480dp max width and a keypad that fits small phones and landscape; the other dialogs aligned with the sheet; tests, mutation check and screenshots.

| Section | Before | After |
|---|---|---|
| 8 PIN | (no rule on how the PIN is typed); change PIN "current PIN first, then the new one, in the same dialog"; forgot PIN "inside every PIN prompt" | + the PIN is typed only in the PIN sheet with its own keypad (verify, create, change, reset; first launch not dismissible), 4 to 8 digits with an OK key, digits never in saved state; change PIN "then the new one twice, in the same sheet"; forgot PIN "inside every PIN sheet that asks for the current PIN" |
| 9 UI | "Dialogs are Material 3 (one PIN dialog at a time, rotation safe, no PIN digits in saved state)"; "One PIN dialog at a time; its state survives rotation" | The PIN sheet spec (look, keypad LTR with Western digits, wrong PIN, lockout, checking, animations off, one sheet, rotation keeps the step and clears digits); the other dialogs share its surface, 28dp corners and title type; responsive rules for the sheet (480dp, key sizes by height, compact rules, landscape side by side, scroll instead of cut) |
| 14 PIN | "PIN dialogs do not pile up (rotation, repeated taps)" | `PinSheetUiTest` scenarios, `PinEntryTest`, mutation checks, the screenshot matrix of the sheet |
| 14 Screen sizes | "Rotation with the PIN dialog open keeps exactly one dialog" | "... PIN sheet ... one sheet" |

Copy: `pin_create_message` no longer says the PIN opens parent settings (wrong since batch 1 item 1). Removed unused strings (both languages): `confirm`, `pin_verify_title`, `pin_hint`, `pin_confirm_hint`, `confirm_dose_title`. New strings for the sheet in both languages (D30, `docs/ar-review.md`).

Decisions: D30 (one component, state machine, 4 to 8 digits with OK, compact rules; the lock now starts after the slow hash so it lasts the full 60 s).

## 2026-10-16, batch 2, item 3: launcher icon and purple leftovers

Request: the adaptive icon background becomes a palette gradient (green-600 to blue-600) with a white symbol at >= 3:1 on both ends, inside the 66dp safe zone, monochrome layer kept, no legacy purple mipmaps; list every purple left in the project and what it became; check splash, notification icon and accent, recents color; screenshots; a contrast test from the tokens.

| Section | Before | After |
|---|---|---|
| 9 Design tokens | Palette, gradient, contrast, semantic colors, motion (no rule for the launcher icon) | + launcher icon rule (gradient tokens `icon_gradient_start` #16A34A / `icon_gradient_end` #2563EB, white `icon_symbol` >= 3:1 on both ends, 66dp safe zone, monochrome layer, splash background `icon_gradient_start`, same icon in light and dark); + "no purple, violet or indigo anywhere" with `scripts/find-purple.ps1` |
| 14 Design | ContrastTest, light/dark screenshots, aapt after dependency changes | + `IconContrastTest` with mutation check, `LauncherIconTest` (safe zone, masks), icon screenshots, purple scan |

Every purple hit and what it became (`docs/evidence/batch2/item3_purple_scan.txt`):

| Where | Before | After |
|---|---|---|
| `drawable/ic_launcher_background.xml:8` | `#6C5CE7` square | gradient `@color/icon_gradient_start` #16A34A to `@color/icon_gradient_end` #2563EB |
| `drawable/ic_launcher_background.xml:12` | `#8E7CF0` circle behind the capsule | removed |
| `ui/ConfettiView.kt:127` | `0xFF6C5CE7` confetti piece ("purple") | `0xFF3B82F6` (palette blue-500) |

Checked and not purple: the splash background (`gradient_start` #4ADE80, now `icon_gradient_start` because the white symbol was 1.74:1 on it), the notification small icon (white `ic_pill`) and accent (`primary` 0xff166534), the recents task colors (#f8fafc background, neutral #f0f0f3 primary), theme template colors (none left), legacy mipmaps (none; adaptive icons only). The light lavender notification dot on the emulator's launcher is drawn in the launcher's own theme color. The icon symbol's yellow lower half became white too (2.2:1 on green-600).

Decisions: D29.

## 2026-10-16, batch 2, item 2: alarm must sound on silent

Request: diagnose the silent reminder on the user's Samsung with read-only commands; then play the alarm sound from a short foreground service (TYPE_ALARM, fallback notification sound, USAGE_ALARM, audio focus, up to 30 s, stops on tap / confirm / "Stop sound" / dismiss / service end, never a volume change, silent notification while it plays, fallback to the batch 1 alarm channel when the service cannot start, one playback for two reminders, nothing for a taken dose), the exact permissions and type from the official docs, tests and mutation checks; sound status rows in parent settings; audibility UNVERIFIED until the pilot.

| Section | Before | After |
|---|---|---|
| 7 Sound | Alarm channel `pill_reminders_alarm` (alarm ringtone, USAGE_ALARM, importance high, category alarm) for every reminder, so silent and vibrate do not mute it; "Alarm sound" off = standard channel; v1 channel deleted; DND without alarms or alarm volume 0 silence it; loudness UNVERIFIED | The app plays the alarm sound itself from `AlarmSoundService` (type `systemExempted`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, started from the exact alarm), TYPE_ALARM else the notification sound, USAGE_ALARM, audio focus, looping, at most 30 s, no volume change; the reminder is the service's notification on the silent channel `pill_reminders_ringing` with "Stop sound" (no PIN); stops on tap, confirm or delete, "Stop sound", dismiss, 30 s, focus loss, service end (the reminder stays after Stop, 30 s, focus loss); one sound at a time, nothing for a dose no longer pending; fallback to `pill_reminders_alarm` with the reason logged; "Alarm sound" off and the v1 deletion unchanged; read-only sound status rows (alarm volume, DND, battery, silent note) in parent settings; audibility UNVERIFIED until `docs/PILOT.md` |
| 8 PIN | Free inside settings: language, permission status and fix buttons, battery settings, test reminder, forgot PIN | + the sound status rows and their buttons; "Stop sound" on a reminder needs no PIN |
| 10 Release | Every APK shared through Drive needs a higher versionCode | + "or handed to a pilot"; parent settings show versionName and versionCode |
| 14 Reminders | Alarm sound: alarm channel attributes in dumpsys, v1 channel deleted, ringer silent posts on the alarm channel, option off uses the standard channel, full flow re-test, audibility UNVERIFIED | App playback scenarios (`AlarmSoundTest`: silent and vibrate within 3 s, one sound, stop on tap in the shade / confirm / Stop / dismiss / 30 s, setting off, two doses one sound, taken follow-up silent, both failure points fall back), the background exact-alarm start on the emulator, channel and style checks, `SoundPolicyTest` and `SoundStatusTest`, mutation checks, the real phone in `docs/PILOT.md` |

Decisions: D28 (why and how, the three official pages quoted, alternatives, fallback, channel names, versionCode 2 / 1.1.0, two robustness fixes found while testing).

## 2026-10-16, batch 2, item 1: test isolation

Request: a second AVD for all automated work, the manual AVD never touched by automation, every uninstall-capable Gradle task behind a guarded script that always leaves the app installed, a read-only rule for physical phones; edit the Hard rules and section 0 (explicitly allowed by the request).

| Section | Before | After |
|---|---|---|
| 0b | "Use the AVD Pixel_API_34. Set ANDROID_SERIAL to the emulator for every adb and gradle command and never touch any other connected device. Back up the emulator DB before any run that can uninstall the app." | Two AVDs: "Pixel_API_34_test" (serial emulator-5560) for ALL automated work; "Pixel_API_34" manual only, never an automated install, uninstall or test run. ANDROID_SERIAL for every adb and gradle command; every install/uninstall-capable Gradle task through `scripts/run-tests.cmd` (refuses unless the serial is the test AVD by `adb emu avd name`, always `leaveApksInstalledAfterRun=true`); Gradle guard on `connected*`/`uninstall*`; helper scripts refuse other devices. Physical phones: read-only diagnostics only, no install, uninstall, pm clear, force-stop, input, settings put or Gradle tasks |
| 2 (Hard rules) | "Do not touch anything outside `app/` and docs unless asked." | "... outside `app/`, docs and `scripts/` ..." plus a new Devices rule: automated runs only on the test AVD through the script; manual AVD never automated; on a physical phone only read-only diagnostics (getprop, dumpsys notification/audio/alarm/package/usagestats/deviceidle, settings get, cmd appops get), saving only this app's lines plus audio and Do Not Disturb state |

Evidence: `docs/evidence/batch2/item1_guard_checks.txt` (unset serial, a non-emulator serial and the manual AVD are refused; the test AVD runs; direct `gradlew connectedDebugAndroidTest` fails with the guard message). README and RELEASE.md now run instrumented tests through the script on the test AVD.

## 2026-10-16, batch 1, item 6: any screen size

Request: small and large phones, tablets, foldables, landscape, multi-window, font scale up to 2.0, display size changes; no orientation lock, resizeableActivity; content max 600dp centered, dialogs max width, chips wrap, home in 2 columns from 840dp, nothing clipped or overlapping; tested with wm size/density in EN and AR.

| Section | Before | After |
|---|---|---|
| 9 | "Supports light mode and a phone rotation" | + "Any screen size" rules: 600dp content, 560dp dialogs, 2 columns from 840dp, wrapping chips (scroll away on short screens), action under the name on narrow cards, header never under the status bar; resizeableActivity true, no orientation lock |
| 14 | landscape and font 1.5 checks | + screen-size matrix (4 sizes x 3 fonts, EN; AR at 1.5), rotation with the PIN dialog, `ResponsiveTest` |
| 0b | (item 0) reset wm size and density at the end | applied: reset after the matrix |

Decisions: D27.

## 2026-10-16, batch 1, item 4+5: palette and modern UX

Request: Material 3 refresh with approved dependencies (latest Material, core-splashscreen, dynamicanimation, Lottie from `design/lottie/` only), palette "Calm and Growth", gradients, contrast >= 4.5:1 with a JUnit test and mutation check, semantic red/amber, dark mode kept, Material 3 dialogs, transitions, spring feedback, smoother overlays, animations-off safety.

| Section | Before | After |
|---|---|---|
| 2 (Hard rules, explicitly requested) | Approved: Room with KSP, AppCompat 1.6+ if present | + Material Components (latest stable, Material 3), core-splashscreen, dynamicanimation, Lottie only from `design/lottie/`; each recorded with reason and APK impact; aapt check for INTERNET and allowBackup after any dependency change. "Animation is custom" now names springs and Lottie |
| 9 | Light mode, rotation | + dark mode follows the system, no dynamic color; design tokens (palette, gradient, contrast rule and test, semantic colors, motion, animations-off rule) |
| 14 | Animation checks | + animations-off scenario; Design block (ContrastTest + mutation, light/dark/EN/AR screenshots, aapt after dependency changes) |

Decisions: D26 (dependency table with sizes, Lottie not added because `design/lottie/` is missing, dialogs kept as Material 3 dialogs, FAB wrapper, card color).

## 2026-10-16, batch 1, item 3: patient gender

Request: gender (Male / Female, ذكر / أنثى) in the add/edit patient form, required for new patients; existing NULL = not set, neutral avatar, no nagging; Room migration (nullable column, never destructive), migration tests; avatar by (age group, gender) in one constants object for chips, headers, reward screens; Arabic copy stays neutral.

| Section | Before | After |
|---|---|---|
| 5 Patient | id, name, ageAtCreation, createdAtEpochDay | + gender (required for new, NULL = not set for old, avatar only, neutral copy, same-name rule unchanged) |
| 14 Data | no gender or migration scenarios | gender scenarios; migration scenarios (v1 -> latest, v2 -> v3, fresh, upgrade over an installed app) |
| 14 Language | per-app language entry works | ... and wins over the stored choice also when the app starts without an activity |
| Schema | v2 | v3: `patient.gender TEXT` nullable, `MIGRATION_2_3`, schema 3 exported. The request said "v2"; v2 was already used by item 2 (D24) |

Decisions: D25 (gender storage and avatars; language sync bug found and fixed).

## 2026-10-16, batch 1, item 2: alarm style notification sound

Request: reminders stay normal notifications but ring on the alarm stream (new channel, old channel deleted, default alarm ringtone, USAGE_ALARM) for the first reminder, follow-ups and the test reminder; PIN-gated "Alarm sound" option, off = previous behavior; README note on Do Not Disturb and alarm volume.

| Section | Before | After |
|---|---|---|
| 7 | One notification channel (implicit), default notification sound | Alarm channel `pill_reminders_alarm` (alarm ringtone, USAGE_ALARM, high importance, category alarm) and standard channel `pill_reminders_standard` (v1 behavior) chosen by the "Alarm sound" option (default on, in the DB); v1 channel deleted on start; DND/alarm-volume limits; audibility UNVERIFIED |
| 8 | (item 1 already listed the alarm sound option as gated) | unchanged |
| 14 Reminders | no sound scenarios | Alarm channel attributes, upgrade over v1 with the old channel, ringer silent, option off, full reminder flow re-test |
| Schema | v1 | v2: `app_settings.alarmSoundEnabled INTEGER NOT NULL DEFAULT 1`, explicit `MIGRATION_1_2`, schema 2 exported |

Decisions: D24 (two channels; gender becomes schema v3 because v2 is taken here).

## 2026-10-16, batch 1, item 1: parent settings open without the PIN

Request: opening settings no longer asks for the PIN; specific actions inside still ask every time; free actions listed.

| Section | Before | After |
|---|---|---|
| 8 (PIN, explicitly requested) | PIN needed for: confirming a pill (toggle), adding/deleting/editing medicines, opening parent settings | PIN needed for confirming (toggle) and adding/deleting/editing medicines. Settings open freely; inside, per action every time: PIN-on-confirm toggle, change PIN (current then new), manage patients and medicines, alarm sound option, erase all data. Free: language, permission status and fix buttons, battery settings, test reminder, forgot PIN (phone screen lock). No global re-reminder interval and no reset of points exist; adding them needs an explicit request |
| 14 PIN | Gates: confirm, add, delete, edit, settings | Gates: confirm, add, delete, edit; plus the settings gate scenarios (wrong PIN changes nothing per gated action, free actions need no PIN) |

Decisions: D23 (manage is gated, the two missing actions are not built, change PIN in one dialog, UI tests on an in-memory DB).

## 2026-10-16, batch 1, item 0: CLAUDE.md as a stable contract

Request: make CLAUDE.md the contract, not a task list; add a "Change requests" section; move history (long section 0, sections 10, 10b, 13) to `docs/ARCHIVE.md` without changing the meaning of any rule.

Line counts: CLAUDE.md 425 before, 375 after. `docs/ARCHIVE.md` 108 lines (verbatim copy of the moved text).

Sections edited:

| Section | Before | After |
|---|---|---|
| 0 | "Autonomous run protocol", 45 lines: unattended run description, 6-step loop, BLOCKED list, version control, commands, emulator hygiene, unverifiable items, defaults, final deliverable of the run | "Working loop": 6 numbered lines (loop and BLOCKED). Verbatim long form in ARCHIVE.md. The final-deliverable paragraph belonged to the first run only and moved to ARCHIVE.md |
| 0a (new) | none | Change requests: checkpoint, implement, test, update contract IN PLACE, add matrix scenarios, CHANGELOG entry; never edit Hard rules or safety invariants (PIN, points, streak, persistence) unless the request says so explicitly, else BLOCKED |
| 0 BLOCKED | 5 triggers | Same 5 plus: a request that conflicts with a Hard rule or safety invariant without explicitly overriding it |
| 0b (new home) | Commands, forbidden list, emulator hygiene, unverifiable items inside section 0 | Same rules. Added: set `ANDROID_SERIAL` to the emulator for every adb and gradle command, never touch another connected device, back up the emulator DB before any run that can uninstall the app; reset `wm size` and `wm density` at the end too; "how loud a sound really is" added to the UNVERIFIED examples |
| 0c (new home) | Defaults inside section 0 | Same 8 defaults, verbatim |
| 3 | "In the autonomous run (section 0), write the checkpoint ..." | "Write the checkpoint into docs/PROGRESS.md and continue (section 0)." Same meaning |
| 7 | (rules for exact alarms, force-stop and permission order lived in the 10b table) | Same rules written into section 7: USE_EXACT_ALARM + SCHEDULE_EXACT_ALARM maxSdk 32, banner and `setAndAllowWhileIdle` fallback, Play policy note, adb-granted permission is not evidence; force-stop and README; permission order |
| 9 | (one PIN dialog at a time in 10b; mascot constants in Phase 4) | Both written into section 9 |
| 9b | ("strings in both files in the same change" lived in section 10) | Written into section 9b |
| 10 | "Phases" 0 to 5 with DoD | "Release rules" (the lasting Phase 5 rules: signing, keystore never committed or created, throwaway keystore allowed, clear failure without keystore.properties, README steps, release checks, GitHub linking by the user). Phase descriptions and DoD moved to ARCHIVE.md |
| 10b | Phase 0 findings table | Moved to ARCHIVE.md; its lasting rules are now in 7 and 9 (item 4 "derived perfect day" was already in 9) |
| 12 | "use the defaults in section 0" | "use the defaults in section 0c" |
| 13 | "Progress log: keep PROGRESS.md with current phase, done, verified, parking lot, risks" | Moved to ARCHIVE.md; the same duty is step 4 of section 0 |
| 14 | "(Phase 0 items 1 to 3)", "(Phase 0 item 5)" | "(section 7)", "(for example the Add medicine button never covers a row's buttons)" |

Unchanged: sections 1, 2 (Hard rules), 4, 5, 5b, 5c, 6, 6b, 8, 9b glossary, 11, the rest of 14.

Decisions: section numbers 1 to 14 kept so existing references stay valid; 10b and 13 have no section in CLAUDE.md any more.
