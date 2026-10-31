# PROGRESS

**BLOCKED (batch 7, G0, 2026-10-19):** Git is not installed on this machine (not on PATH, not in the usual install folders) and `kh.env` (the GitHub token) was not found in the project, the home folder, Desktop, Downloads, Documents or OneDrive. Installing software is outside the project (CLAUDE.md 0b), so G0 waits for the user: install Git for Windows, put `kh.env` in the project root. Items 1 to 4 are done locally (below); `.gitignore` and `.gitattributes` are ready. Nothing of batch 7 has touched git or GitHub; no token was read.

**BLOCKED (batch 6, item 5, asset):** "avatar no streak" exists only with a baked-in checkerboard background, so the streak-broken screens keep their current visuals. Needed from the user: the picture as a PNG with a real transparent background, or on one flat solid color (`docs/assets-report.md`). Everything else in batch 6 continues.

## Current phase

All phases DONE (autonomous run 2026-10-16). Change request batch 1 DONE and change request batch 2 DONE (2026-10-16), change request batch 3 DONE (2026-10-17, branding and child-friendly design). Change request batch 4 DONE (2026-10-18, release pack: signed 1.0.0, documents, dist/, GitHub preparation). Change request batch 6 DONE (2026-10-19, Koko Dawa identity 1.1.0; item 5 BLOCKED on the picture; see below). Change request batch 7 local part DONE (2026-10-19: Koko-Dawa-1.1.0.apk, the release pack, the NotebookLM pack, the "avatar of finish" video); its GitHub part (G0, G1) is BLOCKED on Git and `kh.env`. Final report: `docs/FINAL_REPORT.md`.

Phase status overview:

| Phase | Status |
|---|---|
| 0 Baseline | DONE (2026-10-15) |
| 1 Data + first screen | DONE (2026-10-16), snapshot `../tracker-snapshots/phase-1/` |
| 2 Reminders | DONE (2026-10-16), snapshot `../tracker-snapshots/phase-2/` |
| 3 PIN | DONE (2026-10-16), snapshot `../tracker-snapshots/phase-3/` |
| 4 Rewards + animations | DONE (2026-10-16), snapshot `../tracker-snapshots/phase-4/` |
| 5 Release | DONE (2026-10-16), snapshot `../tracker-snapshots/phase-5/` |

Snapshots: git is not installed, so each finished phase is copied (without build folders) to `../tracker-snapshots/phase-N/` (section 0).

End-of-run emulator hygiene (checked 2026-10-16 03:10): auto_time=1, auto_time_zone=1, timezone Africa/Cairo, font_scale=1.0, Don't keep activities off, device locale en-US, no screen-lock credential (removed after the Phase 3 reset test), app uninstalled by the last instrumented run. The AVD `Pixel_API_34` is kept.

## Change request batch 7 (started 2026-10-19, Koko Dawa 1.1.0 deliverables and GitHub)

The doctor approved the batch 6 screenshots and the palette sheet (user, 2026-10-19). Test AVD only, no app code changes, no uploads anywhere except the GitHub repo of G0 and G1. G0 is BLOCKED (no Git, no `kh.env`, see the top); the user chose to do items 1 to 4 locally now and G0 as soon as Git and `kh.env` exist, before anything is committed or pushed (the ignore rules are written before anything is staged). Order: 1 release build, 2 release pack, 3 presentation pack, 4 leftovers, CLAUDE.md "GitHub access", the full suite, then G0 and G1 when unblocked, then the end steps.

### Item 1 checkpoint: release build

```text
Contract: the signed 1.1.0 with the real key as Koko-Dawa-1.1.0.apk (ASCII) in dist\Koko-Dawa-App\للأهل\1_التطبيق\; verify-dist: the certificate fingerprint equals docs/RELEASE.md (else STOP), no INTERNET, not debuggable, allowBackup false, labels كوكو دوا / Koko Dawa; SHA256.txt and verify-dist.txt in the hospital folder; the update test 1.0.0 -> 1.1.0 with every row kept.
Must change: scripts (build-release, build-dist, make-docs, make-card, update-test: the file and folder name Koko-Dawa), dist\ (the 1.0.0 tree moved to ../tracker-snapshots/dist-1.0.0/, not deleted), evidence.
Must not change: app code, the key, keystore.properties (Gradle reads it; nothing prints it).
Worst cases: an APK signed with another key; two APKs in the parents folder; the old 1.0.0 file left beside the new one; the update test run on a stale APK.
Likely wrong implementation: renaming the file by hand after the build (verify-dist then checks a different file than the one shipped); reusing the batch 6 build without checking it.
Tests that catch it: verify-dist on the exact file in dist (fingerprint, labels, permissions, flags); the SHA-256 in SHA256.txt recomputed; update-test.ps1 -NextApk on that file.
```

First pass (stopped by the user): Koko-Dawa-1.1.0.apk built and verified (17,263,386 bytes, the batch 6 content), the update test passed but printed `Compare-Object` errors for the two empty tables (harmless, the result was right; fixed in `scripts/update-test.ps1`: an empty table is compared as empty). Evidence parked in `docs/evidence/batch7/item1/run0_old_video/`.

### User request during batch 7: the "avatar of finish" video (2026-10-19)

The user asked to replace `avatar of end.mp4` with `avatar of finish.mp4` in the app and then repeat batch 7 from the start. Done: exact copy `design/brand/avatar-finish.mp4`; the original's last 2 frames are pure black (the app holds the last frame), so it is re-encoded like the first video and cut at 5.995 s (180 frames, none black) into `res/raw/avatar_end.mp4` (1,537,158 bytes); `CourseEndLayout.VIDEO_ASPECT` 480/854; assets-report, D42, CHANGELOG, CLAUDE.md 9. Checked: `CourseEndUiTest` 6/6, `CourseEndTest` and `ContrastTest` passed; the screen on the test AVD in EN, AR, dark and landscape looked at (`docs/evidence/batch7/video/contact_sheet.png`). The two course-complete shots of `docs/screenshots-v1.1/` are retaken from the new release in item 1. Batch 7 restarts at item 1.

Item 1 result (second pass): DONE (2026-10-19 04:36; evidence `docs/evidence/batch7/item1/`).
- `dist/Koko-Dawa-App/للأهل/1_التطبيق/Koko-Dawa-1.1.0.apk`, 15,427,906 bytes, SHA-256 `f7b3aa43124b5fb4c9f8dcdcb94a19a476d823020824ada5671717d6cb073122` (recomputed, equals `SHA256.txt`); the new video is inside (1,537,158 bytes). verify-dist all passed: the certificate fingerprint equals docs/RELEASE.md, كوكو دوا / Koko Dawa as app and launcher names, no INTERNET, allowBackup false, not debuggable (`verify_dist_1.1.0.txt`).
- The 1.0.0 tree of `dist/` moved (not deleted) to `../tracker-snapshots/dist-1.0.0/`; the parents folder holds one APK.
- Update test (`update_test.txt`): the shipped 1.0.0 with data (3 patients, 4 medicines, a dose confirmed in the app, points, an award, the PIN), then this APK with `adb install -r`: first install time kept, every table identical, `celebrationSoundEnabled` 1, Room's fingerprint 3 to 4, the patients on home, 42 reminders booked; another key refused with nothing changed. No script error this time.
- `docs/screenshots-v1.1/` 21 and 22 retaken from this release with the new video (looked at), README and overview regenerated.

### Items 2 to 4 checkpoint: release pack, presentation pack, leftovers

```text
Contract: item 2 = the batch 4 deliverables for 1.1.0 (install guide, user guide with the celebration sound setting and the end-of-course screen, WhatsApp text, A5 card, handoff AR and EN, dist/ structure, README, GITHUB.md for the real repo and the PR flow), every PDF page rendered and looked at, guide-shots-needed.md rewritten with the placeholders kept; item 3 = the batch 5 pack (7 Arabic sources under 1500 words, claims-check, prompts with the palette v2 hex values, slide-map, screenshots by slide, composites by a script, flow.mp4 30 to 45 s) describing the celebration sound, the streak-broken screen and the course-end video only as they exist; item 4 = the old names and the old palette searched project-wide and listed, CHANGELOG and FINAL_REPORT updated; CLAUDE.md "GitHub access".
Must change: docs/print/, dist/, README, docs/GITHUB.md, docs/guide-shots-needed.md, docs/whatsapp-message-ar.txt, notebooklm/, scripts (make-composites new; build-dist, make-docs, make-card, print-lib, scan-names), app/claude.md, CHANGELOG, DECISIONS, FINAL_REPORT, PILOT, RELEASE, .gitignore, .gitattributes.
Must not change: app code, the key, keystore.properties.
Worst cases: a real name in a document or picture; a PDF with broken Arabic joining or a cut page; a claim with no evidence (approval, numbers, "treatment finished", a real-phone test of 1.1.0); the token or the repo owner's name written into a file.
Likely wrong implementation: renaming text only and keeping the 1.0.0 screenshots; describing the doctor's no-streak avatar as if it were in the app; counting the PDF pages without looking at them.
Tests that catch it: make-docs page counts and every page looked at; the card's QR read back with a test link; build-dist checks; scan-names (counts only); word and path counts of the sources; claims-check against test names that exist; find-purple; an em dash scan.
```

Items 2 to 4 result: DONE (2026-10-19; evidence `docs/evidence/batch7/item2/`, `docs/evidence/batch7/suite/`).
- Item 2: all 19 guide screenshots retaken from the signed 1.1.0 in Arabic with fictional data (the reminder from a real exact alarm, the PIN on the keypad, the reward), plus `u18_settings_celebration` and `u19_course_end`; `print.css` in palette v2, the Koko icon on every document, the hospital logo only in the handoffs with the approval sentence (D44); install guide 3 pages, user guide 5 (page 4 compacted, "changing phones" moved into the FAQ), handoffs 2 each, the card 1: every page rendered and looked at; the QR read back VERIFIED with a test link; `build-dist`: all checks passed (`build_dist.txt`). Release notes 1.1.0, WhatsApp text, DRIVE.md (also replacing a 1.0.0 upload in place), README (1.1.0 screenshots, the credit without the link), GITHUB.md rewritten, guide-shots-needed.md (re-capture, placeholders stay).
- Item 3: `notebooklm/` with 7 sources (254 to 520 words, no paths, no code, no em dash), claims-check (43 claims; 4 test names corrected to the real classes), prompts, slide-map, 19 screenshots, 6 composites (`scripts/make-composites.ps1`; a PowerShell case clash between `$w` and `$W` fixed on the first run), `video/flow.mp4` (37 s, 720x1600, no audio; the reminder in the shade shows two emulator system notifications, no personal data).
- Item 4: old names and old palette searched over every text file (outside build folders, evidence and dist); the hits left are on purpose and listed in FINAL_REPORT section 12. One real name removed from FINAL_REPORT; `scripts/scan-names.ps1` now prints counts only and knows the new ignore rules: 0 hits. RELEASE.md and PILOT.md switched to Koko Dawa 1.1.0 where they give instructions.
- CLAUDE.md: section 2 "GitHub access" (replaces "never push"), 0, 0b, 10, 11 and the test matrix rewritten in place; CHANGELOG entry with before and after; D43 to D45.
- Suite (`suite/suite.txt`, 2026-10-19 05:05, test AVD only): 131 unit, 131 instrumented, 0 failures; lint 0 errors / 4 warnings (dependency versions only). It ran after the video replacement; only documents and scripts changed after it.
- G0 and G1: still BLOCKED (no Git, no `kh.env`). `.gitignore` and `.gitattributes` are ready; nothing touched git or GitHub.

### Batch 7 end (local part)

- Emulator reset (`docs/evidence/batch7/emulator_reset.txt`): size 1080x2400, density 420, font 1.0, device locale en-US, the app language cleared, animation scales 1, ringer normal, Do Not Disturb off, night mode off, auto-rotate on, automatic time on. Volumes reported, not changed.
- Snapshot `../tracker-snapshots/batch7-local/`.
- Left: G0 and G1 when Git and `kh.env` exist (FINAL_REPORT section 13, step 2).

## Change request batch 6 (started 2026-10-18, Koko Dawa identity requested by the doctor)

The doctor's requests R1 to R8: the Koko rooster as the launcher icon, the name كوكو دوا / Koko Dawa, the celebration avatar with the dose sound, the hospital logo on onboarding (the hospital name under it unchanged) and in the home header, the end-of-course video, the no-streak avatar, a new palette. Explicitly allowed: app identity, launcher icon, palette and design tokens (CLAUDE.md 9), onboarding visuals, home header, reward and streak-broken visuals, the celebration duration (D22) and sound. Untouched: Hard rules and the PIN, points, streak, persistence and reminder-alarm invariants. (Batch 5, the NotebookLM pack of `docs/final-plan.md`, has not been run.) Order: 0 assets, 1 name and icon, 2 palette, 3 hospital logo, 4 celebration avatar and sound, 5 streak broken, 6 end of course, 7 docs and screenshots. Each item: checkpoint, full suite on the test AVD, snapshot `../tracker-snapshots/batch6-item<N>/`.

Schema: the request said to stop if a schema change seemed needed. The "Celebration sound" setting has to be stored in the database like every other setting (5b), so the question was asked before any work; the user chose "Allow the change" (2026-10-18): schema v3 to v4, one column with an explicit Migration 3->4, a migration test and the update test from 1.0.0 proving the data is kept. Nothing else in the schema changes.

### Item 0 checkpoint: assets

```text
Contract: copies (never moves or edits) of the 8 originals in design/brand/ with ASCII names; per image alpha and a checkerboard check near the borders, reported in docs/assets-report.md; order real PNG with alpha, flat background on a same-color tile, keying a flat background only if that looks wrong, never keying a checkerboard (then that asset is BLOCKED and its screen keeps the current visual); the hospital logo from the white original on a white tile; ffmpeg checked first; the video probed, framed every 0.5 s, looked at, re-encoded to H.264 baseline + AAC, max 720p, under 4 MB; the sound probed and converted only if Android cannot play it; no new app library; APK size impact in DECISIONS; crop, scale and place only; EN and AR content descriptions.
Must change: design/brand/, scripts/make-brand-assets.ps1, res/drawable-nodpi (icon foreground, notification large icon, hospital logo, celebration avatar), res/raw (video, sound), docs/assets-report.md, DECISIONS, CHANGELOG.
Must not change: the originals in the root, app behavior (the resources are wired in the later items).
Worst cases: a checkerboard visible in the app; the rooster's title text in the icon; a seam between the crop and the icon background; a character redrawn or recolored; an unplayable video on older phones (High profile).
Likely wrong implementation: using the "no background" files because their names promise transparency; keying the checkerboard; cropping the rooster with the title; trusting ffprobe without looking at the frames.
Tests that catch it: the measured border check per image plus looking at every picture; icon previews in a circle and a squircle on light and dark; a zoom on the crop's top edge; ffprobe of the re-encoded video; the frames looked at.
```

Item 0 result: DONE (`docs/assets-report.md`, evidence `docs/evidence/batch6/item0/` and `video-frames/`).
- All 8 originals present, copied with identical hashes. No optional PNG with alpha. ffmpeg 9.0.2 and ffprobe work.
- All six pictures are JPEG without alpha. Checkerboard (measured and seen): `rooster-nobg`, `avatar-no-streak` (dark), `hospital-logo-nobg`. **avatar no streak is BLOCKED**: it exists only on a checkerboard; item 5 keeps the current visuals and FINAL_REPORT asks for a PNG with alpha or a flat background.
- Made by `scripts/make-brand-assets.ps1` (crop, scale, place): the launcher icon foreground from koko.jpg (the text-free square under the title, rooster and props inside the 66dp safe zone, the crop's border faded over a gradient of its own sky blue; a flat color first showed a faint square edge, and the first crop picked up the title's lowest pixels: both fixed), the notification large icon, the hospital logo (white), the celebration creature (a rounded picture with its own blurred-room background, which is not flat and cannot be keyed).
- Video: toddler in a magic garden (not the rooster), 10.07 s, 576x1024, frames looked at; re-encoded to Constrained Baseline + AAC-LC, 3.37 MB. Sound: MP3, 2.77 s, plays as is (copied as `dose_sound.mp3`).
- Suite (see below).

### Item 1 checkpoint: name and icon (R1, R2)

```text
Contract: application label and launcher label كوكو دوا / Koko Dawa; applicationId unchanged; versionName 1.1.0 with versionCode 4 (higher than every build in RELEASE.md: release 1, update test 2, debug pilot 3); every "Smouha Kids", "سموحة للأطفال", "Pill Pals" hit listed with what it became; the hospital name text stays where it names the hospital (onboarding, About); adaptive launcher icon from koko.jpg (rooster in the 66dp safe zone, background from the picture), no monochrome layer unless a clean silhouette exists, old launcher images removed; notification small icon unchanged, large icon the rooster; screenshots on light and dark wallpaper, circle and squircle; blur or ragged edges reported.
Must change: strings (app_name, app_name_short removed, hospital_name added), manifest labels, onboarding text source, header (no short-name switch, Responsive.fullTitleFits removed), About picture, splash (icon, background, name image), the adaptive icon XMLs and a background drawable, icon color tokens, the notification large icon, version, verify-dist (names read from strings.xml), tests (LocalizationTest, IconContrastTest, LauncherIconTest, ResponsiveTest), README, RELEASE.md, PILOT.md, CHANGELOG.
Must not change: applicationId, package, namespace, DB name, class names, the signing certificate (its CN=Smouha Kids is permanent), the hospital name text, the 1.0.0 release pack (dist, PDFs, card, WhatsApp text).
Worst cases: onboarding silently showing "Koko Dawa" instead of the hospital name; the old capsule still on the launcher; the rooster clipped by a mask; a seam or a white square around the rooster; the splash name white on light blue; verify-dist hard-coding the old names; a versionCode not above the pilot's 3.
Likely wrong implementation: reusing app_name for the hospital text; a monochrome layer pointing at a deleted drawable; forgetting the splash; upscaling the small source into a blurry splash icon.
Tests that catch it: LocalizationTest (both labels from the manifest in both languages, hospital_name separate), IconContrastTest (layers, gradient tokens, no monochrome, old files gone, splash name contrast), LauncherIconTest (every rooster pixel within 33 dp, masked renders), screenshots.
```

Item 1 result: DONE (evidence `docs/evidence/batch6/item1/`, `overview_item1.png`).
- Names: `app_name` is Koko Dawa / كوكو دوا (application label; the launcher activity inherits it), `app_name_short` removed, `hospital_name` keeps the hospital text for onboarding and About. Version 1.1.0 (4).
- Icon: the rooster from koko.jpg over its sampled sky gradient, no monochrome layer, the old capsule files removed. Screenshots: launcher in light and dark mode (label "Koko Dawa"), circle and squircle renders from LauncherIconTest. The rooster with its props is small at launcher size but recognizable; no ragged edges; the background is smooth.
- Splash: first try (the crop on a flat color) showed a lighter square around the rooster; now the adaptive icon itself, a round badge, with the name image in dark navy; Arabic and English checked. The rooster on the splash is slightly soft (small source picture). Notification: the rooster as the large icon, the capsule small icon unchanged (test reminder screenshot).
- Every hit of the three names listed in CHANGELOG with what it became (the certificate keeps `CN=Smouha Kids` for good; the 1.0.0 release pack is not regenerated).
- Tests: LocalizationTest, IconContrastTest (4), LauncherIconTest (3) rewritten for the new contract; ResponsiveTest's short-name case removed with the rule. Mutation checks (`mutation_check.txt`): an oversized rooster fails LauncherIconTest (51.4 dp > 33 dp), a pale splash name fails IconContrastTest.
- Suite (`item1/suite.txt`, 22:33): 110 unit, 109 instrumented, 0 failures; lint 0 errors / 8 warnings (4 dependency, 4 resources used from item 3 on). Snapshot `../tracker-snapshots/batch6-item1/`.

### Item 0 suite

- The test AVD was not running (started again with `-port 5560`). The first run (`item0/suite_run1_arabic_device.txt`) had 8 failures: 7 in AlarmSoundTest and 1 in NotificationChannelTest, all English text checks, because the emulator came back with the app's per-app language set to Arabic, which wins over the stored choice by design. Root cause in the tests: they set English only in the database. Fixed with a JUnit rule `EnglishAppLanguage` (sets the per-app language to English for the test, puts the previous one back). Second run (`item0/suite.txt`): 112 unit, 109 instrumented, 0 failures; lint 0 errors / 10 warnings (the 4 dependency warnings and the 6 new resources, unused until items 1 to 6). Snapshot `../tracker-snapshots/batch6-item0/`.

### Item 2 checkpoint: palette v2 (R8)

```text
Contract: a Material 3 palette from the Koko assets: primary in the rooster orange family, secondary the sunny yellow (with dark text), tertiary the sky blue; surfaces and backgrounds in light sky blue or warm cream tints; the header gradient light sky blue to soft yellow instead of green to blue; light and dark; dynamic color stays off; every text pair >= 4.5:1 (large text and icons >= 3:1), both gradient ends and the translucent boxes over them checked; red stays semantic (missed, overdue, errors), never a brand color; warnings restyled if amber blends with the new primary; status never by color alone; docs/palette-v2.md and docs/palette-v2.png (hex, role, contrast of each text pair); leftover green, blue and teal tokens listed in CHANGELOG.
Must change: values/colors.xml and values-night/colors.xml (palette tokens, header comments), the warning rows in parent settings (container, border, icon) if needed, ContrastTest (new pairs: warning row, secondary as a container color), docs/palette-v2.md, docs/palette-v2.png, CHANGELOG, DECISIONS.
Must not change: token names (layouts and code keep pointing at the same roles), the koko_* icon and splash tokens of item 1, the mascot tokens (the capsule mascot stays until items 4 and 5 replace its uses), the semantic meaning of error, success, points and streak, any behavior.
Worst cases: dark text on the header gradient in light mode but a light gradient end in dark mode (white text unreadable); the sunny yellow used as a text color on white (about 1.6:1); a warning that looks like a normal orange label; red used as the brand accent; a status shown only by color; a purple hue sneaking in.
Likely wrong implementation: picking hex values by eye; checking only the gradient start; keeping secondary as the sunny yellow while layouts use it as text; forgetting values-night.
Tests that catch it: ContrastTest on both palettes (every pair from the tokens, both gradient ends, glass blended over each), a mutation check (a lightened token fails), scripts/find-purple.ps1, screenshots of home, settings warning rows, onboarding, the reward overlay in light and dark.
```

Item 2 result: DONE (2026-10-19; evidence `docs/evidence/batch6/item2/`, `overview_item2_light.png`, `overview_item2_dark.png`; palette sheet `docs/palette-v2.md` and `.png`; decision D38).
- Palette v2 in `values/colors.xml` and `values-night/colors.xml`, same token names: primary the deep rooster orange #A84A0A (light) / #FFB37A (dark), the sunny yellow #FDC440 as the secondary container with dark text (selected chip, PIN keys, tonal buttons), tertiary sky #0B6A86 / container #D4EEF7, light sky page #F4FAFD, cream cards and sheets #FFFCF6. Header gradient light sky, cream, soft yellow (three stops; a two-stop blend turned grey-green in a preview), dark mode deep sky, navy, deep gold with white text. Dynamic color stays off.
- Warnings: an amber box with a border, a warning triangle and dark text, 8dp above its button (alarm volume, Do Not Disturb, battery, exact alarms). Errors keep red words. Screenshots with three warnings (Do Not Disturb set to total silence for the shots, then off again; the alarm volume back at 6).
- Confetti from the Koko colors. Leftover green, blue and teal listed in CHANGELOG (success, the capsule mascot). `find-purple.ps1`: no purple in 166 files.
- ContrastTest: every gradient stop, glass over every stop, the warning box, 3:1 for outlines, the warning icon and the level bar; the drawables must paint exactly the checked stops. Mutation checks 4 of 4 caught (`mutation_check.txt`).
- Screenshots (`shots/`): home, PIN sheet, CHILD and TEEN reward, About in English and Arabic, light and dark, font 1.0 and 1.5 (40), the settings warning rows (4), onboarding (light). Looked at: readable everywhere; the primary orange is deeper (browner) than the picture's orange, the price of 4.5:1 with white text (D38).
- Suite (`item2/suite.txt`, 2026-10-19 00:00): 112 unit, 109 instrumented, 0 failures; lint 0 errors / 8 warnings (4 dependency, 4 resources used from item 3 on). Snapshot `../tracker-snapshots/batch6-item2/`.
- Found on the way: (1) the screenshot script found the reward overlay by the brightness of the left page margin, which the light backdrop no longer changes; it now compares every color channel at both margins (`scripts/screenshots.ps1`). (2) The test AVD froze twice (window not responding, adb and console silent) right after a PIN check while the reward animated, with host GPU rendering and the window minimized; restarted with `-gpu swiftshader_indirect` it ran 50 screenshots and the whole suite without a freeze. (3) The PIN stored on the test AVD was unknown (two guesses failed), so the app's data on the test AVD was cleared (`pm clear`, test data only) and the first launch done again with PIN 1234. (4) Two `pdf-to-png.ps1` processes left over from batch 4 were still running; Windows refuses to end them (taskkill: "no running instance"); harmless, a reboot clears them. (5) Stopping the frozen emulator by name also ended one `crashpad_handler` that belonged to another program (a crash-report helper, nothing else affected); later stops went by command line.

### Item 3 checkpoint: hospital logo (R4, R5)

```text
Contract: onboarding: the capsule emoji becomes the hospital logo on a white rounded tile, the hospital's name under it stays exactly the same text in both languages; home header: the hospital logo on a white rounded tile replaces the app name, "Today: x of y", the chips and the settings button stay, it works on small screens, in RTL, in landscape and at font 2.0, with a content description; every other capsule stays (dose cards, notification icon, mascot); About shows the logo large and keeps the name, version, intro, disclaimer and the data statement; nothing says or implies that the hospital endorses or approved the app.
Must change: activity_onboarding.xml (logo tile), OnboardingActivity (no emoji), activity_main.xml (logo tile instead of appTitle), activity_about.xml (large logo tile instead of the rooster circle; bg_icon_koko removed), strings (logo descriptions, EN and AR), a logo_tile color token, dimens for the logo sizes, a new instrumented test, CHANGELOG.
Must not change: the hospital name strings, the app name, the dose card pill, the notification icon, the mascot, the header behavior (scrolls away, chips pinned), any behavior.
Worst cases: the hospital name under the onboarding logo changed or translated differently; the logo stretched or cut; the header logo overlapping the settings button on a small screen at font 2.0; the logo tile invisible on the light header; a screen reader announcing nothing for the header; an endorsement claim.
Likely wrong implementation: replacing the dose card pill (Mascots.PILL) too; dropping the accessibility heading; a fixed width that breaks landscape; forgetting the About dark mode.
Tests that catch it: a new instrumented test (onboarding: the logo bitmap is the hospital logo, the old emoji is gone, both names exact; header: the logo is a heading with its description, no app-name text in the header, summary and settings button visible, logo and settings button inside the header and not overlapping, also at font 2.0 on a 480x800 screen, in Arabic mirrored, in landscape; About: the large logo plus the disclaimer and data texts); screenshots EN/AR, light/dark, small at font 2.0, landscape.
```

Item 3 result: DONE (2026-10-19; evidence `docs/evidence/batch6/item3/`, `overview_item3.png`; decision D39).
- Onboarding: the hospital logo on a white rounded tile, the hospital name under it unchanged in Arabic and English. Home header: the logo tile (56dp) instead of "Koko Dawa", a heading that a screen reader reads as "Koko Dawa. Smouha University Children's Hospital logo"; summary, stat boxes, chips and settings button unchanged. About: the logo large (168dp) on a white tile, then the name, version, intro, disclaimer and data statement. The dose card pill, the notification icon and the mascot are unchanged. No endorsement wording anywhere (BrandTextTest).
- Screenshots (`shots/`): home and About in English and Arabic, light and dark, font 1.0 and 1.5; home at 480x800 and font 2.0 in both languages; home in landscape in both languages (the logo on the right in Arabic); onboarding light and dark. Looked at: nothing overlaps or is cut; the logo is crisp; its arc text is too small to read in the header (the name is written out on onboarding and About).
- Tests: HospitalLogoUiTest (4, instrumented; it sets 480x800, font 2.0, Arabic and landscape itself and puts them back), BrandTextTest (3, unit). Mutation checks 5 of 5 caught (`mutation_check.txt`).
- Found and fixed on the way: (1) Gradle skipped unit tests as "up to date" after a resource-only change, because the resource files those tests read were not task inputs; `app/build.gradle.kts` now declares `src/main/res` as an input of every unit test task (the endorsement mutation was first missed for this reason, then caught). (2) The first suite run (`suite_run1_flaky.txt`) failed once in `CelebrationUiTest.emptyStateMascotBreathesAFewTimesThenRests`: the test caught the mascot's breathing by timing, and under load the test tools only looked after the breathing had ended; the test now reads the started animation from the view (finite, 3 breaths) and still checks that it stops at rest; two mutations catch an endless and a missing idle motion (`idle_test_fix.txt`).
- Suite (`item3/suite.txt`, 2026-10-19 00:39): 115 unit, 113 instrumented, 0 failures; lint 0 errors / 7 warnings (4 dependency, 3 resources used from item 4 on). Snapshot `../tracker-snapshots/batch6-item3/`.

### Item 4 checkpoint: celebration avatar and sound (R3)

```text
Contract: CHILD and TEEN rewards show the celebration avatar (a rounded picture) with a spring bounce, confetti (TEEN shorter, as before), the points count-up and the streak line; ADULT unchanged (checkmark, no sound). The dose sound plays once when the reward appears, only if the new parent setting "Celebration sound" is on (default on, stored in the DB, PIN-gated like the alarm sound); USAGE_GAME (media volume), never a volume change, audio focus may-duck; stopped and released when the reward closes, the screen goes to the background or the activity ends; not replayed after a rotation. The reward stays max(3 s, min(sound length, 6 s)), with a fade when a longer sound is cut at 6 s; without a sound 3 s (overrides D22). Never two sounds: the confirmed dose's alarm sound stops first, then the celebration sound starts; while another reminder's alarm rings the celebration stays silent; an alarm that starts during the celebration stops the celebration sound. Schema v4: one column app_settings.celebrationSoundEnabled (INTEGER NOT NULL DEFAULT 1), Migration 3->4, exported schema 4.json, migration tests (v1 and v3 data to v4, every row kept). Streak-broken visuals unchanged (item 5 BLOCKED).
Must change: SettingsEntity, AppDatabase (version 4), Migrations, TrackerRepository (setCelebrationSound), SettingsAction (CELEBRATION_SOUND, PIN), SettingsActivity and its layout (a "Rewards" section with the switch), strings EN/AR, SoundPolicy (celebration plan, duration, fade), a CelebrationSound player, Celebration (avatar, duration, sound), MainActivity (passes the setting and the reminder id), AlarmSoundService (stops the celebration sound before it rings), view_celebration.xml (avatar view), Mascots (cheer picture and teen badge no longer used), tests (SoundPolicyTest, MigrationTest, SettingsGatesUiTest, CelebrationUiTest, a new CelebrationSoundTest), D22 superseded in DECISIONS, CHANGELOG.
Must not change: points, streaks, PIN rules, the alarm sound's own rules (30 s cap, stop reasons, fallback), the reminder channels, ADULT reward, streak-broken screens, any data except the one new column.
Worst cases: the celebration sound and an alarm playing together; the sound replaying on rotation; a player never released (a leak, or sound after the screen closed); the sound playing with the setting off or for an adult; a changed device volume; a destructive migration; the PIN not asked for the new setting; the overlay closing before the sound ends or staying longer than 6 s.
Likely wrong implementation: MediaPlayer.create without release; checking the alarm once before its asynchronous stop finished (both play for a moment); USAGE_ALARM or USAGE_MEDIA instead of a game sound; computing the duration from a constant instead of the sound; forgetting the default in the migration.
Tests that catch it: SoundPolicyTest (plan: setting off, adult, no alarm, the confirmed dose's alarm, another alarm; duration 3/3/4.5/6/6 s; fade only when cut), MigrationTest (v1 and v3 data to v4, every row equal, the new column 1, alarm sound and gender untouched; fresh v4 matches the exported schema), SettingsGatesUiTest (wrong PIN leaves it on, right PIN turns it off), CelebrationUiTest (CHILD and TEEN show the avatar, ADULT the checkmark, closes after the computed time), CelebrationSoundTest on the test AVD with dumpsys audio (a USAGE_GAME player of the app starts once; none with the setting off or for an adult; released after closing; with an alarm ringing for the confirmed dose: the alarm player stops before the game player starts, never both in any sample). Audibility on a real phone stays UNVERIFIED.
```

Item 4 result: DONE (2026-10-19; evidence `docs/evidence/batch6/item4/`, `overview_item4.png`; decision D40, which supersedes D22 for rewards).
- CHILD and TEEN rewards show the celebration avatar (a rounded picture) springing in, with confetti (TEEN shorter), the count-up and the streak; ADULT unchanged; the CHILD streak-ended screen and the empty state keep the capsule mascot. The cheering mascot drawable and the teen badge are gone (not shown anywhere any more).
- The dose sound plays once as a game sound (media volume, ducking focus, no volume change), only for CHILD and TEEN, only with "Celebration sound" on and only while the reward is on screen; it stops and is released on close, when the screen stops, and is not replayed after a rotation. Never with an alarm: the confirmed dose's alarm stops first, another ringing reminder keeps the reward silent, an alarm during the reward stops it. The reward lasts max(3 s, min(sound, 6 s)) (3 s with the 2.77 s dose sound), with a fade if cut.
- Parent settings: section "Rewards" with the PIN-gated switch "Celebration sound" (default on). Schema v4: one column with Migration 3->4 and the exported 4.json. On the test AVD the real database was migrated by the new build and kept every row (`device_migration.txt`; the app's update receiver migrated it at install time).
- On the test AVD (`reward_sound_dumpsys.txt`, sampled while the reward shows in the installed app): a USAGE_GAME player of the app for CHILD and TEEN, none for ADULT, none with the setting off; the media volume 15 before and after. Screenshots: rewards per age group in English and Arabic, light and dark, font 1.0 and 1.5 (24), the settings row in English light and Arabic dark. Audibility on a real phone: UNVERIFIED (PILOT).
- Tests: SoundPolicyTest +6, MigrationTest (v3 to v4), SettingsActionTest, SettingsGatesUiTest +2, CelebrationUiTest (avatar), new CelebrationSoundTest (9). Mutation checks: 10 planted bugs, all caught by the final tests; two were first missed because the overlap lasted only milliseconds, so the never-both tests now read the timestamped player event log of dumpsys audio (`mutation_check.txt`).
- Suite (`item4/suite.txt`, 2026-10-19 01:24): 121 unit, 125 instrumented, 0 failures; lint 0 errors / 5 warnings (4 dependency, the video of item 6). Snapshot `../tracker-snapshots/batch6-item4/`.

### Item 5 checkpoint: streak broken (R7)

```text
Contract: CHILD and TEEN streak-broken screens show the "avatar no streak" picture instead of the current mascot, followed by the existing gentle text, only if the asset is usable per item 0; shown once per break as today, never shaming, no points change; ADULT unchanged. If the asset is BLOCKED, keep the current mascot and list it in FINAL_REPORT.
Must change: nothing in the app (the asset is BLOCKED); PROGRESS (BLOCKED at the top), FINAL_REPORT in item 7.
Must not change: the streak-broken screens, their once-per-break rule, points.
Worst cases: a checkerboard picture shown to a child; a keyed cut-out with a halo; a sad picture that shames.
Likely wrong implementation: using avatar-no-streak.jpg anyway; keying its checkerboard.
Tests that catch it: CelebrationUiTest keeps asserting the sleepy then happy mascot for a CHILD's streak end and no avatar there.
```

Item 5 result: BLOCKED (asset), nothing changed in the app. `avatar no streak.jpeg` exists only with a baked-in dark checkerboard right up to the outline (`docs/assets-report.md`): it cannot be shown and must not be keyed. The CHILD streak-ended screen keeps the sleepy then happy capsule mascot, TEEN its restart badge, ADULT its text. What the user must provide: the same picture as a PNG with a real transparent background, or on one flat solid color. No code changed, so the item 4 suite is the state of the code (no separate run). Snapshot `../tracker-snapshots/batch6-item5/`.

### Item 6 checkpoint: end of course (R6)

```text
Contract: when the FINAL scheduled dose of a medicine with an end date is confirmed (the last occurrence inside its start/end range: the end day's dose, the later one of two on that day, or the last weekday dose before an end day without one; no end date never), the course-complete screen comes after the reward closes. The video in a rounded navy card (#1E2A44, the frames' edge color) sized to its 9:16 aspect, centered, at most 80 percent of the screen width and height, portrait, landscape and tablets; a close button from the first second; plays once, then holds the last frame with the text below; muted without an audio track, else its own audio on the media volume following "Celebration sound"; pauses in the background, released on close, continues after a rotation; if playback fails, the celebration avatar as a still with the same text; no PIN, a tap outside, the close button or Back closes it, never blocking. Copy AR "الجدول المسجل لدوا {medicine} خلص. برافو!", EN "The recorded schedule for {medicine} is complete. Bravo!"; never "the treatment is finished". No schema change, no points change.
Must change: a pure CourseEnd rule, TrackerRepository.isFinalDoseOfCourse (read only), MainActivity (pending course end after the reward, saved over rotation), a CourseEndScreen with its layout and a TextureView player, color tokens for the card, strings EN/AR, the alarm sound service (mutes the video's sound before it rings: one sound at a time), tests (CourseEndTest unit, CourseEndUiTest instrumented), CHANGELOG, DECISIONS.
Must not change: points, streaks, confirm rules, the reward, the schema, the alarm sound itself.
Worst cases: the screen for a non-final dose or for a medicine without an end date; shown twice; shown for a missed dose; the video blocking the next action; sound with the setting off; a leaked player (sound after closing); a stretched or cut video; text under the video cut off at font 2.0 or in landscape; "treatment finished" wording.
Likely wrong implementation: comparing with the end date only (misses weekday schedules and two doses on the last day); computing from the dose list instead of the schedule; VideoView (SurfaceView is not clipped by the rounded card); restarting from zero after the screen comes back.
Tests that catch it: CourseEndTest (daily course on the end day; two doses on the last day, first no, second yes; a weekday course whose last dose is before the end date; no end date; an earlier dose; the end date extended later; a missed final dose; another medicine and another patient as controls); CourseEndUiTest (confirming the final dose shows the screen once after the reward, with the exact text and the close button; confirming again does nothing; a non-final dose shows nothing; the setting off mutes it; a forced playback failure shows the avatar still with the text; a rotation keeps it); emulator frames at the start, middle and end, rotation, close button, English and Arabic.
```

Item 6 result: DONE (2026-10-19; evidence `docs/evidence/batch6/item6/`, `overview_item6.png`; decision D41).
- `CourseEnd` (pure) finds a course's last dose from its schedule; the repository reads it for the confirmed dose (no write). On home the course-complete screen follows the reward of a final dose, once, and survives a rotation while it waits.
- `CourseEndScreen`: the video in a rounded navy card (TextureView, so the corners clip it), the tallest 9:16 video that leaves room for the text, within 80 percent of the screen; close button from the start; plays once and holds the last frame with the text; own audio following "Celebration sound" (muted when off, without an audio track, while an alarm rings or after losing focus); pauses in the background, goes on after a rotation, released on close; the avatar still if playback fails; the close button, a tap outside or Back closes it.
- Frames on the test AVD (`shots/`): start, middle and end of playback, landscape and after closing, in English light, Arabic light and English dark; the end frames hold the last picture; the Arabic sentence keeps "Panadol" in place; the close button mirrors in Arabic.
- Tests: CourseEndTest (9), ContrastTest (+1, the card), CourseEndUiTest (6). Mutation checks 6 of 6 caught (`mutation_check.txt`).
- Found on the way: (1) the first card layout let a narrow portrait video squeeze the text into many lines and went over 80 percent in landscape (974 of 1080 px); fixed with a minimum text width and a binary search for the tallest video that fits. (2) The UI test first lost the reward because Espresso's click waited for an idle screen until the reward had closed; it now clicks the dialog button directly and always reads the current activity. (3) The first full suite (`suite_run1_volume_and_timing.txt`) failed 10 tests outside this item: the emulator's media stream had been muted (index 0) and the alarm set to its minimum, which no app or test code does (perhaps the emulator window's volume keys while the new sounds played), and CelebrationSoundTest required a non-zero media volume although it only reads player states; that precondition was removed and the volumes were left as found. One timing check of CelebrationUiTest made six main-thread trips before checking "still there at 2.6 s"; it now reads everything in one trip and counts from the moment the reward was shown.
- Suite (`item6/suite.txt`, 2026-10-19 02:15): 131 unit, 131 instrumented, 0 failures; lint 0 errors / 4 warnings (dependency versions only). Snapshot `../tracker-snapshots/batch6-item6/`.

### Item 7 checkpoint: docs, screenshots, update test

```text
Contract: CLAUDE.md rewritten in place (identity, assets and the transparency rule, palette v2, the logo, the celebration sound, the streak-broken avatar BLOCKED, the end-of-course rule, the test matrix); CHANGELOG; ar-review.md regenerated; FINAL_REPORT (verified vs unverified, every BLOCKED asset and exactly what the user must provide); screenshots in docs/screenshots-v1.1/ with demo data only (launcher icon masks, onboarding with the logo and the hospital name AR and EN, home header with the logo AR and EN, small screen, font 2.0, dark mode, About, reward child and teen, streak-broken child, the course-complete screen, the settings row, the palette sheet); the update test: the previous release 1.0.0 with data, 1.1.0 built with the same real key installed over it, every row kept. Not regenerated: dist/, the PDFs, the notebooklm pack.
Must change: app/claude.md, docs/CHANGELOG.md, docs/ar-review.md, docs/FINAL_REPORT.md, docs/screenshots-v1.1/, scripts/update-test.ps1 (compares the columns both versions have, shows a new column), evidence.
Must not change: app code, dist/, docs/print/, the signing key and keystore.properties (Gradle reads it; nothing prints it).
Worst cases: a real name in a screenshot; the update test passing because the new column made every row "different" and the check was loosened; the PIN hash printed; 1.1.0 built into dist/ by accident; a screenshot list that does not match the files.
Likely wrong implementation: comparing whole rows across schema versions; building with build-release's default output (dist/); forgetting dark mode and the small screen.
Tests that catch it: update-test.ps1 report (versions, first install time, every table equal in the shared columns, the new column's value, refused other key); scan-names.ps1 on the screenshots folder; the screenshot list checked against the folder; the full suite at the end.
```

Item 7 result: DONE (2026-10-19; evidence `docs/evidence/batch6/item7/`).
- CLAUDE.md rewritten in place (title, 0c, 1 with the brand assets rule, 5, 5b, 5c, 6b, 8, 9, 10, 11, 14), CHANGELOG entry with every section's before and after, `docs/ar-review.md` regenerated (296 rows, 10 new or changed in batch 6, none in the second person, 0 missing), FINAL_REPORT section 11 (verified vs unverified, the BLOCKED picture and exactly what to provide) and manual steps 10 to 13.
- Signed 1.1.0 built with the real key into `app/build/release-1.1.0/` (not `dist/`): verify-dist all passed (`verify_dist_1.1.0.txt`: the recorded certificate, Koko Dawa / كوكو دوا, no INTERNET, allowBackup off, not debuggable, 16.5 MB). verify-dist now accepts a launcher activity without its own label (it shows the application label).
- Update test (`update_test.txt`): 1.0.0 from `dist/` with data (3 patients, 4 medicines, 6 doses with one confirmed in the app, points, an award, the PIN) then 1.1.0 with the same key: installed over it, first install time unchanged, every family-data table identical, `celebrationSoundEnabled` 1, Room's schema fingerprint moved from 3 to the exported 4, the patients on home, 42 reminders booked, another key refused with nothing changed. Two earlier runs are kept: run 1 hit a bug of the script's `-NextApk` mode (PowerShell names ignore case: the local copy overwrote the parameter; nothing was installed), run 2 counted Room's fingerprint as family data; both fixed in `scripts/update-test.ps1`.
- Screenshots: 25 in `docs/screenshots-v1.1/` from the signed 1.1.0 with made-up names (README.md with captions, `00_overview.png`); looked at one by one.
- Not regenerated, as requested: `dist/`, the PDFs, the notebooklm pack.

### Batch 6 end

- Final suite (`docs/evidence/batch6/final/suite.txt`, 2026-10-19 02:51, test AVD only): 131 unit, 131 instrumented, 0 failures; lint 0 errors / 4 warnings (dependency versions only).
- Emulator reset (`final/emulator_reset.txt`): size 1080x2400, density 420, font 1.0, device locale en-US, app language cleared, the three animation scales 1, ringer normal, Do Not Disturb off, night mode off, auto-rotate on, automatic time and time zone on. The volumes were left as found (media muted, alarm at its minimum: not done by the app or the tests).
- Batch 7 (rebuild the 1.1.0 release pack and publish to GitHub) arrived during this batch; it runs only after the doctor approved the batch 6 screenshots and the palette sheet, so nothing of it was started (no token read, no git, no GitHub).
- Snapshot `../tracker-snapshots/batch6-final/`.

## Change request batch 4 (started 2026-10-18, release pack, install-first)

Audience: parents of the hospital's patients, mostly non-technical, installing from a Google Drive link on an Android phone. Order: 0 prerequisite, 1 release build and verification, 2 documents, 3 `dist/`, 4 GitHub preparation, then the end steps. Each item: checkpoint, snapshot `../tracker-snapshots/batch4-item<N>/` (without build folders and without `keystore.properties`, `*.jks`, `*.keystore`). Test AVD only; never the phone (the Samsung is connected over USB during this batch and gets no command at all: every adb and Gradle call names `emulator-5560`). No push, no upload, no remote.

### Item 0: prerequisite

`keystore.properties` exists in the project root (created by the user on 2026-10-18, stage 4 of `docs/final-plan.md`); it points at `C:/Users/OPT/smouhakids-signing/smouhakids-release.jks` (exists, created 05:38 by the user) with alias `smouhakids`. Gradle's `validateSigningRelease` passed with it (exit 0) before this batch. The release items are therefore NOT blocked. The password is never printed, logged or copied: snapshots now leave out `keystore.properties`, `*.jks` and `*.keystore` (`docs/evidence/tools/snapshot.ps1`, used by `suite.ps1`). Found: a stray empty file `keystore.propertie` (0 bytes, from a cut-off Notepad command) in the root; not a secret; listed for the user.

### Item 1 checkpoint: release build and verification

```text
Contract: versionName 1.0.0, versionCode 1, release signed with the real key; APK named Smouha-Kids-1.0.0.apk (ASCII); scripts/verify-dist: apksigner verify + certificate SHA-256 printed and saved in docs/RELEASE.md, aapt badging (package, versionName, versionCode, minSdk, AR and EN labels), permissions (no INTERNET), not debuggable, allowBackup false, size, SHA256.txt; update test on the test AVD (1.0.0 with data, then 1.0.1 / 2 with the SAME key over it with adb install -r, data intact); a different key recorded as a failure, never a path; the main flow from the release APK (first run, add a child, add a medicine, reminder, alarm sound, confirm, reward).
Must change: app/build.gradle.kts (version 1 / 1.0.0, test-only version override for the update test), scripts/build-release.ps1, scripts/verify-dist.ps1, scripts/update-test.ps1, docs/RELEASE.md (fingerprint, procedure), docs/PILOT.md (version and the uninstall of the debug pilot build), DECISIONS, CHANGELOG.
Must not change: applicationId, the keystore, signing logic, app code, DB schema, permissions.
Worst cases: a release signed with the debug key; the password in a log, evidence file or snapshot; the keystore modified; a "Success" install that did not change the version; data compared by row counts only; the different-key APK left where it could be shared.
Likely wrong implementation: verifying the debug APK; reading the fingerprint from the keystore instead of from the APK; only counting rows; forgetting that the pilot phone has the debug 1.2.0 (3) build (higher versionCode and another key), so 1.0.0 cannot install over it.
Tests that catch it: verify-dist fails on a debug certificate, on INTERNET, debuggable, allowBackup true or a fingerprint that differs from the one in RELEASE.md; update test asserts versionCode 1 then 2 in dumpsys package and every table row identical before and after (full dumps), then opens the app; the different key gives INSTALL_FAILED_UPDATE_INCOMPATIBLE and the data and version stay; flow evidence: notification with the patient, USAGE_ALARM player of the app, PIN confirm, points in the DB, reward screenshot.
```

Item 1 result: DONE (evidence `docs/evidence/batch4/item1/`).
- Version 1.0.0 (1). `scripts/build-release.ps1` built `dist/Smouha-Kids-App/للأهل/1_التطبيق/Smouha-Kids-1.0.0.apk` (13,465,081 bytes) with the real key; `scripts/verify-dist.ps1`: all checks passed (v2 signature, one signer, `CN=Smouha Kids, O=Smouha Kids, C=EG`, SHA-256 `FF:0A:9A:...:E8:7A` recorded in RELEASE.md; package, 1 / 1.0.0, minSdk 28, both names in both languages; no INTERNET; allowBackup 0x0; not debuggable). `SHA256.txt` and `verify-dist.txt` in `dist/Smouha-Kids-App/للمستشفى/`. First run of verify-dist failed on the Arabic folder path (old tools cannot open it); now it checks an ASCII copy and compares the hashes.
- Main flow from the release APK on the test AVD (`flow.txt`, shots 01 to 10): first run, PIN, child Lina 7 (fictional), Vitamin C at 06:10, notifications allowed, reminder at 06:10:00 with the app in the background ("Background started FGS: Allowed", `USAGE_ALARM` player of the app started), tap opened "Confirm Lina's pill" and stopped the sound (TAPPED), PIN, reward +35, DB: dose TAKEN, 35 points.
- Update test (`update_test_run1.txt`): 1.0.1 (2) with the same key over 1.0.0: Success, versionCode 2, first install time unchanged, all 10 tables identical before and after the update and after opening the app (PIN hash compared, not printed), Lina on home, 8 slot alarms booked; the same APK re-signed with the debug key: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, version and every row unchanged, the copy deleted. After that run `update-test.ps1` got `-NextApk` (a real next release). Final run on a fresh 1.0.0 install with the Arabic demo data (`update_test.txt`, 06:43): 3 patients, 4 medicines, 42 slots, 6 doses, 3 rewards rows, 60 rows in 10 tables, all identical after the update, after opening the app and after the refused other-key install; 42 slot alarms booked; all checks passed. Two script bugs found on the way, both in the tooling, not the app: a nested build that started a new Gradle daemon kept the output pipe open and the run waited forever (stopped by hand; nothing had been installed), and Windows PowerShell gave no exit code for a started process until its handle was read (one run reported a failed build that had not failed). Both fixed (`build-release.ps1` and `update-test.ps1` now start Gradle as a process with a log file and wait for that process).
- Docs: RELEASE.md rewritten, PILOT.md 0a (uninstall the debug pilot build, install the signed 1.0.0), D34, CHANGELOG. Not changed: app code, schema, permissions, signing logic.
- Not verified: the signed APK on a real phone (PILOT.md 0a, the user).

### Item 2 checkpoint: documents

```text
Contract: Arabic first, simple Egyptian wording, short sentences, one action per step, no jargon ("ملف التطبيق", never "APK"), guardians addressed in the plural imperative (اضغطوا، افتحوا), big type. PDFs from RTL HTML with headless Edge; every page rendered to PNG (from the PDF itself) and looked at until clean. Install guide 2 to 3 pages with clearly marked placeholder boxes for the real-phone screens + docs/guide-shots-needed.md (exact names in design/guide-shots/), rebuilt with the files once they exist. User guide 4 to 5 pages with test AVD screenshots and fictional data only, the "reminder only, not medical advice" statement. WhatsApp message 5 to 6 lines with {DIRECT_LINK} and {FOLDER_LINK}. A5 card (one page, QR, 4 steps) from scripts/make-card reading dist/link.txt; QR decoded by a decoder or marked UNVERIFIED; the QR tooling recorded in DECISIONS. Hospital handoff AR and EN, 2 pages each: purpose, features, every permission and why, privacy, safety limits, install and update, the certificate fingerprint, known limits and the devices actually tested, support <TO BE FILLED>, test summary from FINAL_REPORT without overclaiming.
Must change: docs/print/ (HTML, CSS, AVD shots), scripts/make-docs.ps1, scripts/make-card.ps1, scripts/qr/ (tool build), docs/guide-shots-needed.md, docs/whatsapp-message-ar.txt, the PDFs in dist/, DECISIONS (QR tooling, fonts), CHANGELOG.
Must not change: app code and strings, data.
Worst cases: Arabic letters broken or reversed in a PDF; a drawn or faked system screen; a real name in a screenshot; a claim the hospital approved the app; medical advice or dosing; an imperative singular (gender) form; a QR that points to the wrong place or does not scan; a page that overflows (cut text); a placeholder that looks final.
Likely wrong implementation: checking the HTML in a browser instead of the PDF; letting Edge add headers and footers; images from the debug build with old names; a hard-coded link in the card.
Tests that catch it: every PDF page rendered by Windows' own PDF renderer and viewed; page counts checked by the script; the QR decoded from the rendered card page with ZXing and compared with dist/link.txt; a word scan of the Arabic texts for singular imperatives and "APK"; grep for em dashes.
```

Item 2 result: DONE (evidence `docs/evidence/batch4/item2/`, `check.txt`).
- PDFs (built by `scripts/make-docs.ps1`, every page rendered to PNG and looked at): install guide 3 pages (11 steps; all 11 real-phone screens are marked placeholder boxes until `design/guide-shots/` has them), user guide 5 pages (15 pictures from the release build on the test AVD, Arabic, fictional names نور 7, كريم 15, سلمى 40; the "reminder only, no medical advice" box on page 1 and again at the end), hospital handoff 2 pages in Arabic and 2 in English (facts from FINAL_REPORT, RELEASE.md, verify-dist.txt and PILOT.md; support `<TO BE FILLED>`).
- First build problems found by looking at the pages, all fixed: English button names split over two lines in Arabic sentences, too many pages (6 and 3), a squashed step circle, the card's draft label over the title.
- Card: `scripts/make-card.ps1`; with a sample link the QR code was read back from the rendered PDF page by ZXing and equals the link (VERIFIED for the pipeline); the card in `dist/` is a marked DRAFT until `dist/link.txt` exists (its QR code UNVERIFIED until then).
- WhatsApp message `docs/whatsapp-message-ar.txt` (6 lines). `docs/guide-shots-needed.md` (11 required screens and the video, with names and rules).
- Scans: no singular imperative (only passive "اتأكد"), "APK" only as the real file name in the handoff, 0 em/en dashes.
- Tooling found and fixed: Windows' PDF renderer once kept its process alive (the script ends itself now, with a time limit in `print-lib.ps1`); the Arabic typing helper needed `android.test.base.jar` and the accessibility "set text" action.
- Not verified: the real-phone pictures (the user takes them), the printed card's QR code with the real link, how parents understand the guides (the plan's test with a non-technical person).

### Item 3 checkpoint: dist/ arranged as on Drive

```text
Contract: dist/Smouha-Kids-App/ with للأهل (1_التطبيق/Smouha-Kids-1.0.0.apk, 2_طريقة_التثبيت.pdf, 3_طريقة_الاستخدام.pdf, 4_فيديو_التثبيت.mp4 only if the user adds it), للمستشفى (handoff AR and EN, SHA256.txt, release notes, verify-dist output), للطباعة (the card), للمطور (screenshots, README, document sources, nothing sensitive); the parents folder holds only what parents need; dist/DRIVE.md: upload steps, sharing (parents: anyone with the link, viewer; hospital: restricted), the direct link from the file id, a private-window test checklist.
Must change: scripts/build-dist.ps1, docs/print/release-notes-1.0.0.txt, dist/DRIVE.md.
Must not change: the APK (same file and hash as item 1), app code.
Worst cases: a keystore, keystore.properties or evidence copied into dist; a second (old or other-key) APK in the parents folder; the parents folder shared with internal files; a link format that opens a preview instead of the download.
Likely wrong implementation: copying docs/ wholesale into للمطور (evidence, RELEASE.md with the key path); forgetting that the developer copy of README goes stale.
Tests that catch it: build-dist checks exactly one APK and verify-dist, a whitelist for the parents folder, no signing/password/device-dump file and no evidence folder anywhere in dist, the hospital and print files present.
```

Item 3 result: DONE. `scripts/build-dist.ps1` (runs verify-dist, writes the release notes, make-docs, make-card, mirrors the developer folder, checks the tree): all checks passed. Tree: للأهل (the APK 13,465,081 bytes, the 2 guides; no video yet), للمستشفى (handoff AR and EN, SHA256.txt, `ملاحظات_الإصدار_1.0.0.txt` with a BOM for Notepad, verify-dist.txt), للطباعة (the DRAFT card), للمطور (README, 83 screenshot files, 24 document-source files, guide-shots-needed.md, whatsapp-message-ar.txt). Not in the developer folder on purpose: docs/evidence, RELEASE.md (names the key's location on this laptop). `dist/DRIVE.md` written (upload, sharing table, both links, `dist/link.txt` format, private-window and phone checks, "Manage versions", a note on Google's announced sideloading verification, not checked here). The README copy is refreshed by the last build-dist run after item 4.

### Item 4 checkpoint: GitHub preparation (prepare only)

```text
Contract: README in English with a short Arabic paragraph (idea, features, demo screenshots, build, test, permissions, privacy, the disclaimer, no hospital endorsement), no license file, "All rights reserved" with the license as an open decision; the repo leaves out keystore files and passwords, local.properties, build folders, docs/evidence, ../tracker-snapshots and device dumps; every remaining tracked text file scanned for the real test names, every hit reported; no screenshot with real names; docs/GITHUB.md with the exact commands for a PRIVATE repo, run by the user; git not installed said plainly.
Must change: README.md, .gitignore, scripts/scan-names.ps1, docs/GITHUB.md, PROGRESS (redact names in old entries), CHANGELOG.
Must not change: app code; no git init, no remote, no push.
Worst cases: the scan list itself publishing the real names; evidence with real names or the phone's data in the repo; the scripts that dot-source docs/evidence/tools breaking because the folder is ignored; a README that implies the hospital approved the app.
Likely wrong implementation: ignoring docs/evidence wholesale (breaks scripts/screenshots.ps1 and suite.ps1); scanning only *.kt; trusting a scan without listing what it scanned.
Tests that catch it: the scan prints the file count and how the list was made, every hit with file:line, and the pictures per folder; check-ignore commands in GITHUB.md for the user to confirm with git.
```

Item 4 result: DONE (evidence `docs/evidence/batch4/item4/`).
- README rewritten (English, an Arabic paragraph, 4 demo screenshots, features, privacy and the permission table, parent install steps kept as CLAUDE.md 10 asks, developer section, "independent project, not approved or endorsed by the hospital", "All rights reserved", license an open decision). No LICENSE file.
- `.gitignore`: added every `build/` folder (also `scripts/qr/build`), `.kotlin/`, `docs/evidence/*` except `docs/evidence/tools/` (scripts dot-source it), device dumps (`*.hprof`, `bugreport*.zip`, `window_dump.xml`, `*.logcat`), `dist/link.txt`, `.idea/deploymentTargetSelector.xml` (a local AVD path), `scripts/scan-names.txt`, `docs/final-plan.md` (the internal plan names the people to ask; kept on disk, two lines to delete to publish it). Signing files and `local.properties` were already ignored; `../tracker-snapshots` is outside the project.
- Name scan `scripts/scan-names.ps1` (git is not installed, so the file list comes from the .gitignore rules; with git it uses `git ls-files`): first run 40 hits in 442 files (`scan_names_before.txt`): the scanner's own name list, the plan's text, 6 old PROGRESS entries with the user's own test patients, the Medipulse credit. Done: the PROGRESS entries redacted ("the user's two patients"); the name list moved to the ignored `scripts/scan-names.txt`; the plan kept out of the repo. Last run (`scan_names.txt`): 3 hits, all the credit link to the original Medipulse-App repository (README, app/claude.md, the README copy in dist), which contains its author's public GitHub account name: reported, left for the user to decide (GITHUB.md). 198 pictures would be published: the app's screens with fictional names (docs/screenshots, docs/print/shots and their copies in dist) and the two splash name images.
- Removed the empty stray file `keystore.propertie` (0 bytes, from the cut-off Notepad command during stage 4).
- `docs/GITHUB.md`: git not installed (checked), install Git for Windows, identity, `git init -b main`, check-ignore and the name scan before the commit, the private repo on the website (or `gh repo create --private`), `git remote add origin`, `git push -u origin main`, the private-window check, never force-push, never public before approval. Nothing was run: no repository, no remote, no push.

### Batch 4 end

- Final suite on the test AVD (`docs/evidence/batch4/final/suite.txt`, 07:19 to 07:26): `scripts\run-tests.cmd testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` exit 0; 109 instrumented tests, 0 failures; lint 0 errors / 4 dependency warnings. Gradle reused the unit test results of 2026-10-17 (task up to date); forced rerun `testDebugUnitTest --rerun` at 07:26: 112 tests, 0 failures. The release app (1.0.1 from the update test) was uninstalled from the test AVD before the suite (another signature).
- verify-dist negative checks (`final/verify_dist_negative.txt`): the debug build (3 checks fail), the release re-signed with the debug key (2 fail), the release re-signed with a throwaway key from a temp folder (the recorded-certificate check fails); the throwaway key and all copies deleted.
- CLAUDE.md updated in place: 0b (allowed tools), 10 rewritten as "Release and distribution", 11 (environment), 14 (Release, Documents and distribution); Hard rules and invariants untouched. CHANGELOG (end entry), FINAL_REPORT section 10 (batch 4 verified vs unverified) and 11 (manual steps). Handoff test summary date updated and `scripts/build-dist.ps1` run again: all checks passed.
- Emulator reset (`final/emulator_reset.txt`): 1080x2400 @420, font 1.0, the three animation scales 1, ringer normal, Do Not Disturb off, light mode, system en-US, app language en, automatic time on. The manual AVD and the phone got no command.
- Em/en dashes in the 47 files written or changed in this batch: 0.
- Snapshot `../tracker-snapshots/batch4-final/` (without build folders and signing files).

## Change request batch 3 (started 2026-10-17, branding and child-friendly design)

Context: patients of Smouha University Children's Hospital (Alexandria), ages 0 to 18. Explicitly allowed edits: the age group boundary rule, the applicationId, the app identity sections. Hard rules and the PIN, points, streak and persistence invariants stay. Order: 0 applicationId, 1 app name, 2 About screen, 3 age groups, 4 child-friendly design, 5 docs and screenshots. Each item: checkpoint, full suite on the test AVD (`docs/evidence/tools/suite.ps1`), snapshot `../tracker-snapshots/batch3-item<N>/`.

Inputs checked at the start: `design/` does not exist (no `design/brand/logo.*`, no `design/fonts/`), so the logo on splash and About and the rounded font are skipped (items 1 and 4 say so).

### Item 0 checkpoint: applicationId

```text
Contract: applicationId com.smouha.kidsreminder (permanent after the first real release); namespace and Kotlin package stay com.example.tracker; every runtime use of the old id fixed and listed; new app on Android (old installs keep their data, removed by hand); verified on the test AVD (clean install, flow, reminder, tap, alarm sound, reboot re-arm) and the full suite.
Must change: app/build.gradle.kts (applicationId, version 3 / 1.2.0), intent action strings, the instrumented package test, ui.ps1, suite.ps1, PILOT.md, RELEASE.md, CLAUDE.md, DECISIONS, CHANGELOG.
Must not change: package and class names, namespace, DB name, data model.
Worst cases: an action or authority still on the old id so a PendingIntent or a provider breaks; tests pass because they read the package at runtime while a hard-coded string is wrong; helper scripts still poking the old package (and its old data) on the emulator.
Likely wrong implementation: search-and-replace of "com.example.tracker" everywhere (would rename packages and class references).
Tests that catch it: ExampleInstrumentedTest asserts the new id; full suite; emulator: aapt shows the new package, reminder, tap, sound, reboot re-arm.
```

Item 0 result: DONE. applicationId `com.smouha.kidsreminder`, version 1.2.0 (3); action strings, the package test, ui.ps1, suite.ps1, PILOT.md (new-app line), RELEASE.md, CLAUDE.md 1 changed (list in CHANGELOG); D31. Test AVD: old `com.example.tracker` uninstalled there (it would have kept firing its own alarms), clean install, onboarding, reminder in the background with the app's alarm sound, tap opened the right dose and stopped the sound, reboot re-armed 7 slots, the daily check and the follow-up (`docs/evidence/batch3/item0/flow.txt`; the boot broadcast came about 45 s after boot_completed, the first look was too early). Suite: 108 unit, 101 instrumented, 0 failures, lint 0 errors / 4 dependency warnings (`item0_suite.txt`). Snapshot `../tracker-snapshots/batch3-item0/`.

### Item 1 checkpoint: app name

```text
Contract: application label = full name (EN "Smouha University Children's Hospital", AR "مستشفى سموحة الجامعي للأطفال"); launcher activity label = short name ("Smouha Kids", "سموحة للأطفال"); full name on the splash, home header (collapses on small screens and at font 2.0), language picker, About; every "Pill Pals"/"pill_pals"/"PillPals" hit listed with what it became; package, namespace, DB name, class names unchanged; no logo (design/brand missing).
Must change: strings EN/AR (names, sentences naming the app), manifest labels, home header and onboarding layouts, splash branding (API 31+), Responsive rule for the header, docs naming the app, CLAUDE.md identity sections (1, 0c), CHANGELOG list.
Must not change: PillPalsApp and other class names, pill_pals.db, the package, channel ids.
Worst cases: a header that runs under the settings button or the status bar at font 2.0; launcher label truncated; Arabic name shaped wrongly in an image; the name only in one language on the language picker (before a language is chosen).
Likely wrong implementation: one long label everywhere (launchers cut it); renaming PillPalsApp or the DB file (data loss on update).
Tests that catch it: LocalizationTest-style check of the labels per language; ResponsiveTest for the header rule; screenshots (launcher EN/AR at 1.0 and 1.5, header at small/2.0, splash, language picker); full suite.
```

Item 1 result: DONE. Full and short names in both languages; application label full, launcher label short (LocalizationTest checks both, and what Android reads); splash name image for Android 12+ (D32; Android 9 to 11 icon only); home header full name, short name when tight (ResponsiveTest); language picker with both names; every Pill Pals hit listed in CHANGELOG (internal names kept). Screenshots in `docs/evidence/batch3/item1/`: launcher EN and AR at 1.0 and 1.5 (Arabic needed the system language switched through Settings on the test AVD, then put back to en-US only), splash EN and AR, home header EN/AR at 1.0/1.5/2.0, language picker at 1.0 and 2.0. No logo (`design/brand/` missing). Found: two strings edited outside this work before batch 3 (`empty_state` with "دوس", a second-person imperative; `gender_label`); left for item 4. Suite: 109 unit, 102 instrumented, 0 failures; lint 0 errors / 4 dependency warnings (`item1_suite.txt`). Snapshot `../tracker-snapshots/batch3-item1/`. The test AVD app data was cleared once for the language picker shot (test AVD only).

### Item 2 checkpoint: About screen

```text
Contract: About screen, opens without PIN from parent settings: full name, versionName, short EN/AR text, the gender-neutral disclaimer (reminder only, no medical advice or dosing, follow the doctor's instructions), data on this phone only and no internet access; no invented contact details; no logo (none provided).
Must change: AboutActivity + layout, SettingsAction.ABOUT (free), settings row, strings EN/AR, manifest, tests (SettingsActionTest, a UI test), CLAUDE.md 8 and 14.
Must not change: PIN gates of the other actions, data.
Worst cases: About behind the PIN; a disclaimer that gives advice or addresses the reader by gender; an invented phone number or address; a version that does not match the APK.
Likely wrong implementation: hard-coded version text; a dialog that cannot scroll at font 2.0.
Tests that catch it: SettingsActionTest (ABOUT free), UI test opens About from settings without a PIN and reads the version from PackageManager; screenshots EN/AR at 1.0 and 2.0.
```

Item 2 result: DONE. `AboutActivity` (name, installed version, text, disclaimer, local-only data; no contact details, no logo), linked from parent settings as a free action. `SettingsActionTest` and `SettingsGatesUiTest.aboutOpensWithoutPinAndShowsTheInstalledVersion`. Screenshots EN/AR at 1.0 and 2.0 (`docs/evidence/batch3/item2/overview_about.png`). Suite: 109 unit, 103 instrumented, 0 failures; lint 0 errors / 4 dependency warnings (`item2_suite.txt`). Snapshot `../tracker-snapshots/batch3-item2/`.

### Item 3 checkpoint: age groups

```text
Contract: CHILD 0 to 12, TEEN 13 to 18, ADULT 19 and over (explicitly allowed); age still derived, never stored; aging by full years.
Must change: AgeGroup.TEEN_MAX_AGE 17 -> 18, AgesTest (12/13/18/19, aging into TEEN and into ADULT), CLAUDE.md 5 and 14, README.
Must not change: age input 0 to 120, points, streaks, the DB (the group is never stored).
Worst cases: an 18 year old still shown the adult style; a boundary off by one; a stored group somewhere that keeps the old value.
Likely wrong implementation: changing only the docs; "age < 18" left in some UI code.
Tests that catch it: AgesTest boundaries 12/13/18/19 and an 18 year old becoming ADULT exactly one year later; grep for other age literals; mutation (TEEN_MAX_AGE back to 17).
```

Item 3 result: DONE. `AgeGroup.TEEN_MAX_AGE` = 18 (grep: no other age limit in code, docs updated: CLAUDE.md 5 and 14, README). `AgesTest` 7 tests: boundaries 0/12/13/17/18/19/120, 12 to 13 and 18 to 19 exactly one year after being added (TEEN/ADULT not a day earlier). Mutation: TEEN_MAX_AGE back to 17 fails 3 of 7 (`ageGroupBoundaries`, `seventeenStaysTeenAtEighteen`, `eighteenBecomesAdultExactlyOneYearLater`), restored passes. Suite: 110 unit, 103 instrumented, 0 failures; lint 0 errors / 4 dependency warnings (`docs/evidence/batch3/item3/suite.txt`). Snapshot `../tracker-snapshots/batch3-item3/`.

### Item 4 checkpoint: child-friendly design

```text
Contract: only the listed changes. Fonts: design/fonts/ is missing, so skipped and noted (no download). Shapes: 28dp cards and sheets, buttons >= 56dp with fully rounded ends, a larger dose-confirm button, softer shadows. Mascot: vector capsule character in the palette with 3 expressions (happy, cheer, sleepy) replacing the emoji mascots in the CHILD reward, CHILD streak-broken and the empty state; a small idle motion. Copy: friendlier gender-neutral Egyptian wording for rewards and empty states, listed in ar-review. ADULT stays calm.
Must change: mascot drawables and colors, Celebration (CHILD), empty state, button and card styles, dose button, strings.
Must not change: 3 s celebration, tap to close, animations-off safety, contrast rules, PIN keypad sizing rules (keys stay sized by height), TEEN badge and ADULT checkmark, data.
Worst cases: the idle motion runs forever with animations off or after the overlay closed; a mascot that looks poor; a 56dp button pushes the PIN sheet or small screens out of shape; contrast broken by new colors; second-person Arabic in new copy.
Likely wrong implementation: infinite animator started with animator scale 0; raw hex in layouts; mascot left visible for TEEN/ADULT; only the layout height changed so the visible button stays 36dp.
Tests that catch it: MascotTest (JVM: 3 drawables, tokens only, outline contrast), ContrastTest, instrumented reward test with animations off (closes after 3 s), screenshots CHILD/TEEN/ADULT reward, streak-broken, empty state, small screen at font 2.0, ar-review second-person table.
```

Item 4 result: DONE (fonts SKIPPED: `design/fonts/` does not exist, nothing downloaded; D33).
- Shapes: `styles_shapes.xml` set in the theme (56dp pill buttons without insets, tonal and outlined too; 28dp cards with a 1dp palette-tinted shadow), dose button `Widget.PillPals.Button.Confirm` (64dp, 18sp, check icon), FAB fully rounded, reward card shadow 8 to 4dp. PIN keys and text buttons unchanged on purpose.
- Mascot: 3 VectorDrawables (happy, cheer, sleepy), CHILD reward, CHILD streak ended (sleepy, then happy), empty state; TEEN badge and ADULT checkmark unchanged; idle breath via `Motion.idle`. Judged good in the screenshots, emoji not kept.
- Copy: 7 Arabic and 6 English changes (CHANGELOG item 4 table), `empty_state` no longer imperative; `docs/ar-review.md` regenerated: 288 rows, 5 second-person hits (was 6), 20 new or changed in batch 3, 0 missing.
- Tests: `MascotTest` (2, JUnit), `CelebrationUiTest` (5, instrumented). Mutation checks in `docs/evidence/batch3/item4/mutation_check.txt` (3 caught; one needed a stronger test first). ContrastTest still passes.
- Evidence (`docs/evidence/batch3/item4/`): `broken_child_sleepy_en`, `broken_child_happy_en`, `reward_{child,teen,adult}_en`, `reward_child_ar`, `home_en_light`, `overview_empty.png` (EN light/dark, AR, small at 2.0), `overview_ar.png`, `overview_shapes.png` (settings buttons). Demo data from the new `scripts/seed-demo.ps1` (fictional).
- Suite: 112 unit, 108 instrumented, 0 failures; lint 0 errors / 4 dependency warnings (`item4/suite.txt`). Snapshot `../tracker-snapshots/batch3-item4/`.
- Parking lot: Arabic-Indic digits when Arabic comes from Android's per-app setting, `broken_adult_title` second person, empty state under the FAB at rest on 480x800 at font 2.0.

### Item 5 checkpoint: docs and screenshots

```text
Contract: CLAUDE.md, CHANGELOG, ar-review up to date; scripts/seed-demo with fictional data; screenshots in docs/screenshots/: home, add patient, PIN sheet, reward (child, teen, adult), streak-broken, About, in AR and EN, light and dark, font 1.0 and 1.5, and home on the 4 sizes.
Must change: docs/screenshots/ (new), README pointer, CLAUDE.md 14 screenshot line, FINAL_REPORT at the end.
Must not change: app behavior.
Worst cases: real names in demo data; screenshots taken with a system locale that the app would not use (Arabic-Indic digits); a stale screenshot claimed as new.
Likely wrong implementation: screenshots from a different build; light/dark mixed up.
Tests that catch it: every shot taken by one script from the installed build after seed-demo; an index file listing every shot with its settings; overview montages checked by eye.
```

Item 5 result: DONE.
- `scripts/seed-demo.ps1` (fictional: Nour 7, Karim 15, Salma 40; `-Lang ar`, `-Broken`, `-Empty`; test AVD only, needs a finished first launch) and `scripts/screenshots.ps1` (waits for screens, picks the overlay frame from a burst, resets the look at the end).
- `docs/screenshots/`: 72 shots (8 screens x EN/AR x light/dark x font 1.0/1.5, home on 4 sizes x EN/AR), index `README.md`, overviews in `overview/`, all checked by eye. First run: 3 of 8 "add patient" shots were wrong (the tap came before the screen settled); the step now waits for the PIN keypad and the form, and the retake is correct.
- Found while taking them: the endless idle breath made uiautomator fail on the empty state (every scale change is an accessibility event). Fixed in item 4 code (3 breaths, then rest), new test `emptyStateMascotBreathesAFewTimesThenRests`, mutation (endless) caught.
- CLAUDE.md 14 (documentation screenshots), README link, CHANGELOG item 5; `docs/ar-review.md` current (no string changed in item 5).

### Batch 3 end

- Final suite on the test AVD (`docs/evidence/batch3/final/suite.txt`): 112 unit, 109 instrumented, 0 failures; lint 0 errors / 4 dependency warnings. The run before it had 1 failure, `AlarmSoundTest.startForegroundThatFailsFallsBackToTheAlarmChannel` (10 s timeout waiting for the fallback reminder, the first class of the run right after the install; `suite_run1_flake.txt`); the class alone then passed 10 of 10 twice and the full rerun passed. Not changed; noted as a possible timing flake under load.
- Snapshot `../tracker-snapshots/batch3-final/`.
- Emulator reset (`docs/evidence/batch3/final/emulator_reset.txt`): 1080x2400 @420, font 1.0, the three animation scales 1, ringer normal, Do Not Disturb off, light mode, system en-US, app language en, automatic time on. The manual AVD and the phone were not touched.

## Change request batch 2 (started 2026-10-16, hardening after the first real-phone test)

Phone findings to address: (a) no reminder sound on silent (Samsung SM-A266B), (b) purple launcher icon background, (c) old-looking PIN dialog. Order: 1 test isolation, 2 alarm sound (2a read-only diagnosis, 2b foreground playback, 2c sound status, 2d pilot), 3 icon and purple leftovers, 4 PIN bottom sheet, 5 Arabic review sheet, 6 pilot pack. Each item: checkpoint, full suite on the TEST AVD through `docs/evidence/tools/suite.ps1` (which uses `scripts/run-tests.cmd`), snapshot `../tracker-snapshots/batch2-item<N>/`.

### Item 1 checkpoint: test isolation

```text
Contract: automated work only on Pixel_API_34_test; Pixel_API_34 manual only; uninstall-capable Gradle tasks only through scripts/run-tests.cmd (test AVD check, always leaveApksInstalledAfterRun=true); physical phone read-only.
Must change: new AVD files, scripts/run-tests.cmd, Gradle guard in app/build.gradle.kts, helper scripts (suite, mutate, ui), CLAUDE.md 0b and 2, README, RELEASE.md, CHANGELOG.
Must not change: app behavior, the manual AVD and its data, anything on the phone.
Worst cases: a run picks the phone or the manual AVD and uninstalls the app (the batch 1 incident); the guard blocks Android Studio's own runs.
Likely wrong implementation: checking only that the serial starts with "emulator-" (the manual AVD is an emulator too).
Tests that catch it: refusal checks for unset, non-emulator and manual-AVD serials; direct gradlew connected run blocked; full suite through the script on the test AVD.
```

Item 1 result: DONE. AVD `Pixel_API_34_test` created (copy of the Pixel_API_34 config, only the id and display name differ), runs as `emulator-5560`. Guard checks 5 of 5 as expected (`docs/evidence/batch2/item1_guard_checks.txt`: unset serial refused, phone serial refused, manual AVD refused, test AVD runs, direct `gradlew connectedDebugAndroidTest` stopped by the Gradle guard). Suite on the test AVD through the script: 75 unit, 78 instrumented, lint 0 errors / 4 dependency-version warnings (`item1_suite.txt`). The manual AVD was not touched. Snapshot `../tracker-snapshots/batch2-item1/`.

### Item 2 checkpoint: alarm must sound on silent

```text
Contract: at every reminder (first, follow-ups, test) with "Alarm sound" on and the dose still pending, the app itself plays the default alarm ringtone (fallback: default notification sound) on USAGE_ALARM from a short foreground service, up to 30 s, with audio focus; stops on tap, confirm, "Stop sound", dismiss, service end; never changes a volume; the notification stays as is and is silent while the service plays; if the service cannot start, the batch 1 alarm channel rings instead and the reason is logged. Settings show sound status rows (alarm volume, DND, battery) without a PIN.
Must change: new SoundPolicy (pure), AlarmSoundService, stop receiver, a silent "ringing" channel, Notifications (choose channel, Stop action, delete intent), ReminderReceiver, AlarmScheduler.onDoseTaken, MainActivity (tap stops), manifest (exact FGS permission and type from the official docs), SoundStatus (pure) + settings rows, strings EN/AR, versionCode 2, DECISIONS, README, CLAUDE.md 7/8/14.
Must not change: reminder timing, follow-up count, deep link, PIN gates, DB schema, INTERNET absent, allowBackup false.
Worst cases: double sound (channel and service), no sound at all (service fails and the notification is silent too), a sound that never stops, a follow-up of a TAKEN dose ringing, any volume changed, a crash from a background start not allowed.
Likely wrong implementation: keep posting on the alarm channel while the service plays; start the service without checking the dose status; forget the 30 s cap; a guessed foreground service type that the system rejects on targetSdk 37.
Tests that catch it: SoundPolicyTest and SoundStatusTest (pure); instrumented on the test AVD with ringer SILENT and VIBRATE: USAGE_ALARM player in dumpsys audio within 3 s, stops on tap / confirm / Stop / dismiss / 30 s, setting off = no player, two doses = one player, forced start failure = alarm channel; mutation checks (TAKEN check removed, cap removed, setting ignored).
```

Item 2 result (2b, 2c, 2d): DONE on the test AVD; the real phone stays UNVERIFIED (2d).

- 2b: `SoundPolicy` (pure), `AlarmSoundService` (type `systemExempted`, sources quoted in D28), `AlarmSound` (announce / stop), silent channel `pill_reminders_ringing` with "Stop sound" and a delete intent, tap and confirm stop the sound, fallback to the alarm channel with the reason logged, versionCode 2 / 1.1.0. aapt: no INTERNET, allowBackup 0x0, service type 0x400 (`item2_aapt.txt`).
- 2c: `SoundStatus` (pure, fake-phone tests) and `PhoneSoundSource`; settings rows for alarm volume, Do Not Disturb, battery, the silent note and the version line; EN + AR strings, gender-neutral.
- Tests: `SoundPolicyTest` 8, `SoundStatusTest` 6, `AlarmSoundTest` 10 (silent and vibrate within 3 s, one sound, stop on tap in the shade / confirm / Stop / dismiss / 30 s, setting off, two doses one sound, taken follow-up silent, two forced failures fall back), `NotificationChannelTest` 4. Mutation check 7 of 7 killed (`item2_mutation_check.txt`).
- Emulator, real exact alarm with the app in the background and the ringer in VIBRATE: "Background started FGS: Allowed ... ALARM_MANAGER_WHILE_IDLE", player 137 ms after the alarm, TIMEOUT after 30.0 s, the reminder stays quiet (`item2b_background_vibrate.txt`).
- Suite on the test AVD: 89 unit, 89 instrumented, 0 failures; lint 0 errors / 4 dependency-version warnings after removing a `VisibleForTesting` warning (`item2_suite.txt`). Snapshot `../tracker-snapshots/batch2-item2/`.
- Found and fixed: `TrackerRepository.settings()` not atomic (crashed a test run during a concurrent erase); `PinDialogFragment` show listener after destroy (crashed a test after a reminder tap). Both in D28.
- 2d UNVERIFIED (needs the real phone): audibility on silent and vibrate, Do Not Disturb, alarm volume 0, Samsung sleeping apps, how loud it is. The user runs `docs/PILOT.md` (item 6). SILENT on the emulator is set through `AudioManager`, which on this image also turns on Do Not Disturb (priority, alarms allowed); VIBRATE is a plain ringer mode.
- Test hygiene incident (no data lost): an earlier `AlarmSoundTest` run left a follow-up alarm on the test AVD (a home screen opened by the tap test re-armed it after tearDown had cancelled it). Found before the background check and cleared with force-stop + relaunch on the test AVD; tearDown now waits for that re-arm before cancelling.
- Parking lot: the onboarding PIN card still says the PIN is needed to "open parent settings" (stale since batch 1 item 1). The onboarding PIN step is rebuilt in item 4; the copy is fixed there.

Item 2a result: DONE (read-only, phone RKGL100FEFP).

### Item 3 checkpoint: launcher icon and purple leftovers

```text
Contract: adaptive icon background = palette gradient (green-600 #16A34A to blue-600 #2563EB, to bottom-right), white symbol >= 3:1 on both ends, symbol inside the 66dp safe zone, monochrome layer kept, no legacy purple mipmaps; no purple left anywhere (res, Kotlin, manifest, themes, splash, notification code); splash, notification icon/accent and recents color checked.
Must change: drawable/ic_launcher_background.xml, ic_launcher_foreground.xml (all-white symbol), colors.xml icon tokens, splash background, IconContrastTest, CHANGELOG list of every purple hit, CLAUDE.md 9 tokens.
Must not change: app behavior, other colors, the in-app palette and its contrast.
Worst cases: a colored symbol half that fails 3:1 on green-600; the white splash icon on the light green splash background (1.74:1); a purple hex hidden in a drawable or Kotlin literal; themed icon broken by the background change.
Likely wrong implementation: changing only the background color and keeping the yellow capsule half; checking contrast by eye.
Tests that catch it: IconContrastTest reads the tokens AND the icon drawables (gradient uses the tokens, every symbol fill is white) and computes both ratios; a scan script lists every purple-range hex and purple-named resource; screenshots on the test AVD (light and dark launcher, circle and squircle masks, themed icon, splash).
```

Item 3 result: DONE.

- Icon: gradient #16A34A to #2563EB (white 3.29:1 and 5.16:1), white two-half capsule, monochrome layer kept, splash background green-600. Purple scan: 3 hits before (icon background twice, one confetti color), none after (`item3_purple_scan.txt`, listed in CHANGELOG). No legacy mipmaps existed.
- Tests: `IconContrastTest` 5 (unit), `LauncherIconTest` 3 (instrumented: safe zone in pixels, monochrome layer, circle and squircle renders). Mutation check 4 of 4 killed (`item3_mutation_check.txt`).
- Screenshots on the test AVD (`docs/evidence/batch2/item3/`, described in `screenshots.txt`): drawer light and dark, icon on the light and dark parts of the wallpaper, themed icon (launcher setting switched on and back off), splash, circle and squircle renders. Not possible on this image: launcher icon-shape switching (no overlays), a plain wallpaper from the shell.
- Suite on the test AVD: 94 unit, 92 instrumented, 0 failures; lint 0 errors / 4 dependency-version warnings (`item3_suite.txt`). Snapshot `../tracker-snapshots/batch2-item3/`.
- Test AVD state changed for the screenshots and kept: the Pill Pals icon on the home screen. Night mode and themed icons were set back.

### Item 4 checkpoint: PIN panel as a modern bottom sheet

```text
Contract: every PIN entry in one Material 3 bottom sheet with its own keypad (verify, create, change, reset, first launch); look as requested; wrong PIN, lockout, checking and animations-off behavior; keypad always LTR with Western digits; one sheet at a time; rotation keeps the step and clears digits; digits never in saved state; lockout and per-action gating unchanged; 480dp max, keypad fits small phones at font 2.0 and landscape; other dialogs aligned.
Must change: new PinSheet + PinSheetModel (ViewModel), pure PinEntry, sheet layouts (portrait, landscape, keypad, header, forgot), styles and dimens by height, callers (home, settings, onboarding), onboarding without text fields, strings EN/AR, theme (dialogs and sheet), tests (PinEntryTest, PinSheetUiTest, SettingsGatesUiTest), ui.ps1 Enter-Pin, CLAUDE.md 8, 9, 14.
Must not change: PIN hashing and storage, failure counting and the 60 s lock, which actions need the PIN, forgot PIN via the phone screen lock, data.
Worst cases: digits in saved state; a second sheet; a mirrored keypad in Arabic; a lockout the keypad ignores; a rotation that loses the step or repeats a check; the onboarding sheet dismissed without a PIN; the keypad cut off on a small phone; a longer existing PIN that can no longer be entered.
Likely wrong implementation: auto-submit at 4 digits; digits in an EditText or in onSaveInstanceState; the check in the fragment's scope (cancelled by a rotation); layoutDirection forgotten on the keypad.
Tests that catch it: PinEntryTest (state machine), PinSheetUiTest (wrong PIN, lockout, rotation, saved-state bytes, one sheet, RTL keypad, animations off, first launch), SettingsGatesUiTest on the keypad, mutation checks, the screenshot matrix.
```

Item 4 result: DONE.

- `PinSheet` replaces `PinDialogFragment` (moved out of the project); the first launch uses the same sheet, not dismissible, with no text field. 4 to 8 digits, 4 dots growing, OK key; hardware keys work too.
- Tests: `PinEntryTest` 14 (unit), `PinSheetUiTest` 9 and `SettingsGatesUiTest` 9 on the keypad (instrumented). Mutation check 6 of 6 killed (`item4_mutation_check.txt`).
- Found and fixed: icon-only keys cut by baseline alignment; the keypad partly below the fold on the small phone at font 2.0 (now compact rules and height-based key sizes); landscape keypad centered against taller text; the Change PIN message shown on steps 2 and 3; `verifyPin` started the lock before the slow hash (lock about 58 s, countdown showed 60 for a second), now after it.
- Screenshots (`docs/evidence/batch2/item4/`, index in `screenshots.txt`, 4 overview sheets): EN light at 4 sizes x 3 fonts, AR light, EN and AR dark, landscape (small at 2.0 in both languages), step 2, wrong PIN, lockout, confirm pill, first launch EN and AR. Display reset afterwards (wm size/density, font 1.0, app language en, night no, rotation free).
- Test AVD data: erased twice through the app for the first-launch screenshots and set up again (English, PIN 1234, no medicines).
- Parking lot item from item 2 (onboarding copy) done here.
- Suite on the test AVD: 108 unit, 101 instrumented, 0 failures; lint 0 errors / 4 dependency-version warnings (`item4_suite.txt`). Snapshot `../tracker-snapshots/batch2-item4/`.

### Item 5 checkpoint: Arabic review sheet

```text
Contract: docs/ar-review.md generated from values/strings.xml and values-ar/strings.xml: key, English, Egyptian Arabic, screen; every plural form; second-person (gender-addressed) strings first; no Arabic changed; the new strings of items 2c and 4 included.
Must change: scripts/ar-review.ps1 (generator), docs/ar-review.md (output).
Must not change: any string.
Worst cases: a plural form left out (Arabic has six); a second-person string not on top; a screen column that is guessed instead of looked up; hand edits that drift from the strings files.
Likely wrong implementation: a hand-written table; only the "other" plural form.
Tests that catch it: the generator reads the files and the code (usage lookup); the counts in the sheet (rows, plural forms, flagged, new in batch 2, missing) checked against the files; spot checks of the 2c and 4 strings.
```

Item 5 result: DONE. `scripts/ar-review.ps1` (UTF-8 with BOM: Windows PowerShell 5.1 reads a file without BOM in the ANSI code page) writes `docs/ar-review.md`: 281 rows (78 plural forms or list items, all six Arabic forms per plural), screen per key from where the code and layouts use it, 39 rows marked new or changed in batch 2 against the `batch2-item1` snapshot, 0 missing. Second person (word list, flagged first with a note): 5 hits, one real: `broken_adult_title` "أطول سلسلة ليك" addresses the reader as masculine ("for you"); it is the CLAUDE.md glossary wording and the request says not to change Arabic, so it waits for the native reviewer. `forgot_pin` "نسيت" can read as "you forgot". The other three are not second person (noted in the sheet). No string changed. No app file changed (robocopy list against `batch2-item4`: 0 files), so the item 4 suite stands for this code; snapshot `../tracker-snapshots/batch2-item5/`.

### Item 6 checkpoint: real phone pilot pack

```text
Contract: docs/PILOT.md: install the debug APK on a physical phone only with adb -s <serial> install -r; first step = confirm the installed versionCode; checklist with expected result and what to record for every item of the request; a results table (phone model, Android version, pass/fail, notes).
Must change: docs/PILOT.md (and links to it).
Must not change: the app; nothing is run on the phone by the developer tools.
Worst cases: a step that uninstalls (data loss); a check whose expected result promises more than the design (Do Not Disturb without alarms, volume 0); no way to tell which build is installed; diagnostics that would leak patient names.
Likely wrong implementation: "uninstall and reinstall" as the install fix; expected results copied from the emulator without the phone's own limits.
Tests that catch it: review of every request item against the checklist (17 checks); the commands are the read-only ones of CLAUDE.md 2 plus the app's own log tag.
```

Item 6 result: DONE. `docs/PILOT.md`: build and `adb -s <serial> install -r` (data kept; stop instead of uninstalling on a signature or version error), first check = Version 1.1.0 (2) in parent settings or `dumpsys package`, preparation, 17 checks (silent, vibrate, Stop sound, 30 s, DND with and without alarms, alarm volume 0, swiped from recents, reboot, Optimized vs Unrestricted plus Samsung Sleeping and Never sleeping apps, locked screen, 3 re-reminders then stop, right dose on tap, confirm stops, Arabic and English, font 1.5, PIN sheet without keyboard), a results table, and read-only commands plus `logcat -s PillPalsSound` for failures. Docs only. `docs/evidence/batch2/phone-diagnosis.txt`. The phone runs a batch 1 build (alarm channel present, unchanged by the user; v1 channel deleted at 17:53:42). DND off, alarm volume 15/15 unmuted, exact alarms scheduled and on time, no background restriction. Three reminders on the alarm channel with the ringer SILENT (18:08, 18:10, 18:44) got no USAGE_ALARM focus request or player at all; two with the ringer NORMAL (18:11, 18:14) did. Ranked cause 1: One UI drops channel sounds on silent even for USAGE_ALARM; cause 2: before 17:53 the phone ran a pre-batch-1 build (v1 channel, USAGE_NOTIFICATION). All builds were versionCode 1, so the build could not be told apart.

### End of batch 2

- Final suite on the test AVD only: 108 unit, 101 instrumented, 0 failures; lint 0 errors / 4 dependency-version warnings (`docs/evidence/batch2/final_suite.txt`). Snapshot `../tracker-snapshots/batch2-final/`.
- CLAUDE.md updated in place during the items: Hard rules and 0b (item 1), section 7 sound design (item 2), 8 and 9 PIN sheet (item 4), 9 icon and palette tokens (item 3), 9b and 10 review sheet and pilot (items 5 and 6), 14 test matrix (every item). CHANGELOG has one entry per item; FINAL_REPORT section 8 lists verified vs unverified.
- Test AVD reset (`docs/evidence/batch2/end_reset.txt`): wm size and density, font 1.0, app language en (device en-US), animator, transition and window scales 1, ringer NORMAL, Do Not Disturb off, ring and notification volume 5, alarm 6, night mode off, rotation free, auto time on, themed icons off. The manual AVD and the phone: untouched (phone: read-only commands only, item 2a).
- UNVERIFIED (needs the real phone): whether the alarm sound is heard on the Samsung on silent and vibrate, Do Not Disturb, battery managers, haptics of the keypad. The user runs `docs/PILOT.md`.
- Process notes (no effect on any device): a few Gradle runs that never touch a device (unit tests, lint, assembleDebug, `dependencies`) were started without `ANDROID_SERIAL` in that shell; every device task had it and went through `scripts/run-tests.cmd`. On the phone, besides the allowed read-only commands, `adb shell date` was run (read-only) to line up timestamps; it is not on the allowed list of CLAUDE.md 2.

## Change request batch 1 (started 2026-10-16)

Order: 0 contract, 1 settings gates, 2 alarm sound, 3 gender, 4+5 visual refresh, 6 screen sizes. Each item: checkpoint here, implement, full suite (`docs/evidence/tools/suite.ps1`, DB backed up first, app left installed), snapshot `../tracker-snapshots/batch1-item<N>/`.

### Item 0 checkpoint: CLAUDE.md as a stable contract

```text
Contract: CLAUDE.md = what the app must do and its rules. History moves to docs/ARCHIVE.md verbatim; every rule keeps its meaning.
Must change: app/claude.md (section 0 short + 0a change requests + 0b/0c rule blocks, 10 = release rules, 10b and 13 out), docs/ARCHIVE.md, docs/CHANGELOG.md.
Must not change: any rule's meaning, Hard rules (2), safety invariants (5b, 6, 6b, 8), app code.
Worst cases: a rule that only lived in a moved section disappears (exact-alarm permissions, permission order, keystore rules, strings in both files).
Likely wrong implementation: cutting section 10b and 10 wholesale.
Tests that catch it: line-by-line review of every moved line, listed in CHANGELOG with its new home; full suite still green (no code change).
```

Item 0 result: DONE. CLAUDE.md 425 -> 375 lines, ARCHIVE.md 108 lines, CHANGELOG.md created. Suite: 64 unit, 60 instrumented, lint 0 errors / 5 dependency-version warnings (`docs/evidence/batch1/item0_suite.txt`). Snapshot `../tracker-snapshots/batch1-item0/`.

### Item 1 checkpoint: parent settings open without the PIN

```text
Contract: settings open freely; inside, PIN every time for: PIN-on-confirm toggle, change PIN, manage patients/medicines, alarm sound (item 2), erase all. Free: language, permission buttons, battery, test reminder, forgot PIN.
Must change: MainActivity (settings button), SettingsActivity (router), PinDialogFragment (change = verify then create), new SettingsAction table, strings EN/AR, CLAUDE.md 8 and 14.
Must not change: PIN storage, lockout, reset via phone lock, confirm/add/edit gates on home.
Worst cases: a wrong PIN still flips the toggle; change PIN skips the current PIN; manage (edit/delete) becomes free; the switch shows the new state although nothing changed.
Likely wrong implementation: removing the PIN at the door without adding per-action gates; toggling the switch first and asking afterwards.
Tests that catch it: SettingsActionTest (exact gated and free sets), SettingsGatesUiTest on the real screens with an in-memory DB (wrong PIN per gated action changes nothing, right PIN toggles, language and opening work without a prompt), mutation check.
```

Not applicable, logged in D23: global re-reminder interval (per medicine) and "reset points and streaks" (does not exist; would deduct points against section 6).

Item 1 result: DONE. Suite: 66 unit, 67 instrumented (7 new SettingsGatesUiTest), lint 0 errors / 5 dependency-version warnings (`docs/evidence/batch1/item1_suite.txt`). Mutation check: 3 of 3 caught (`item1_mutation_check.txt`). Snapshot `../tracker-snapshots/batch1-item1/`.

INCIDENT (item 1, fixed): `docs/evidence/tools/mutate.ps1` ran instrumented tests without `leaveApksInstalledAfterRun`, so Gradle uninstalled the app after a mutation run and the user's emulator data was deleted. Restored from the backup taken by the item 0 suite run (14:39): the user's two patients, 8 reminder times, PIN, language English (system per-app language and DB both set back to English), notification permission granted again, 8 slot alarms re-armed. Anything entered on the emulator between 14:39 and the uninstall is lost. Root cause fixed: mutate.ps1 now always passes `leaveApksInstalledAfterRun=true` and sets `ANDROID_SERIAL`. The emulator also froze once during this item (software rendering) and was restarted.

### Item 2 checkpoint: alarm style notification sound

```text
Contract: reminders stay normal notifications (patient name, tap opens the dose) but ring on the alarm stream: new channel, default alarm ringtone, USAGE_ALARM; first reminder, follow-ups, test reminder. PIN-gated "Alarm sound" option; off = v1 behavior. v1 channel deleted.
Must change: Notifications (two channels), ReminderReceiver (pass the option), settings switch, SettingsAction, DB column + Migration 1->2 + schema 2, strings EN/AR, README, CLAUDE.md 7 and 14.
Must not change: reminder timing, follow-up count, deep link, data of existing users.
Worst cases: update wipes or alters data (destructive migration); old channel survives and keeps the quiet sound; option off still rings as alarm; silent mode mutes it anyway.
Likely wrong implementation: changing the sound on the existing channel id (ignored by Android); fallbackToDestructiveMigration.
Tests that catch it: MigrationTest (realistic v1 data, every row identical, new column 1; fresh v2), NotificationChannelTest (definitions and stored channels, v1 deleted), SettingsGatesUiTest (alarm sound wrong/right PIN), emulator: upgrade over the real v1 install, dumpsys, ringer in vibrate.
```

Item 2 verified on the emulator (evidence `docs/evidence/batch1/item2/`):

| Check | Result | Evidence |
|---|---|---|
| Upgrade over the installed v1 app with the user's real data: DB user_version 1 -> 2, 2 patients and 8 slots kept, alarmSoundEnabled 1; v1 channel `pill_reminders` mDeleted=true; alarm channel usage=USAGE_ALARM, sound alarm_alert, importance 4; standard channel = v1 behavior | PASS | 01, 02 |
| First reminder at 16:00 posted with the patient name; the system played it as MediaPlayer usage=USAGE_ALARM on STREAM_ALARM | PASS | 03 |
| Ringer VIBRATE (ring and notification streams muted): follow-ups at 16:05, 16:10, 16:15 each played on the alarm stream; exactly 3 (remindersSent 3, no follow-up alarm left) | PASS | 04, 05 |
| Tap on the reminder opens the PIN check for exactly that dose; confirmed: TAKEN, 35 points, notification gone | PASS | 06, 07 |
| Reboot without opening the app: all 8 app alarms re-armed by the boot receiver (about 60 s after boot_completed on this slow emulator) | PASS | 08 |
| Mutation check: migration default 0, alarm channel on notification usage (first survived because Android keeps an existing channel's sound; test strengthened to check the definitions, then caught), wrong channel picked | 3 of 3 caught | mutation_check.txt, mutation_check_2b.txt |

Not verified: audibility (needs real device; the emulator shows the alarm-stream player started, not how loud it is); ringer mode SILENT as such (this image has no command for it; ring volume 0 gives VIBRATE, which mutes the same notification streams); Do Not Disturb.

Test data handling: the user's DB was moved aside, the reminder flow ran on a fresh DB, then the user's DB was moved back (the user's two patients, 8 slots, English, v2, alarm sound on); force-stop removed the test alarms, the app re-armed only the user's 08:00 alarms. Ringer volumes set back to 5 (mode NORMAL).

Item 2 result: DONE. Suite: 66 unit, 74 instrumented, lint 0 errors (`docs/evidence/batch1/item2/suite.txt`; a first run failed 9 UI tests because a "System UI isn't responding" dialog held the focus; suite.ps1 now closes it first). Snapshot `../tracker-snapshots/batch1-item2/`.

### Item 3 checkpoint: patient gender

```text
Contract: gender MALE/FEMALE required for new patients, NULL = not set for existing ones (neutral avatar, editable, no prompt); avatar by (age group, gender) from one place in chips, headers, reward screens; Arabic copy stays neutral.
Must change: Gender enum, Validation (GENDER_MISSING), PatientEntity + Migration 2->3 + schema 3, repository add/update, Mascots, add form + patient dialog (shared picker), chips, headers, Celebration, strings EN/AR, tests.
Must not change: any existing row on update, same-name rule, wording of any sentence.
Worst cases: migration rewrites or drops rows; existing patients forced through a gender prompt; editing clears a stored gender; gender changes Arabic wording.
Likely wrong implementation: NOT NULL column with a default (invents a gender for old patients); prompting on home.
Tests that catch it: MigrationTest (v1 -> v3 and v2 -> v3, every row identical, gender NULL; fresh v3), ValidationTest, RepositoryDataTest.genderIsStoredAndEditedPerPatient (control patient), MascotsTest (exact table), emulator upgrade over the user's v2 install.
```

Item 3 verified on the emulator (evidence `docs/evidence/batch1/item3/`):

| Check | Result | Evidence |
|---|---|---|
| Upgrade over the user's installed v2 app: user_version 2 -> 3, patients, slots, doses and settings identical, both patients gender NULL | PASS | 01 |
| User's patients with gender not set: neutral avatar 🧑 in chips and headers, no prompt | PASS | 03 |
| New patient without gender: save refused, "Choose male or female." at the picker, form scrolled to it | PASS | 04 |
| Girl 7, teen boy 15, woman 40: 👧, 👨‍🎓, 👩 in chips and headers | PASS | 05 |
| Reward screen shows 👧 before "Yasmin took Panadol" | PASS | 06 |
| Arabic: home and picker ("النوع (للصورة بس)", ذكر / أنثى) mirrored | PASS | 07, 08 |
| Bug found and fixed: Android per-app language setting overridden at app start (D25). Now: set Arabic in Android -> app and DB Arabic; back to English -> both English | PASS | 02, AppLanguageTest |

Side effect found and fixed: the settings language UI test finished before the app had applied the language, so its restore was overwritten and the user's app ended up in Arabic (the DB followed through the sync bug). The test now waits for the language to be applied and for the restore. The user's emulator was set back to English in both places.

Test data: the user's DB was moved aside for the form and reward screenshots and moved back afterwards (the user's two patients, gender NULL, 8 slots, English); force-stop removed the test alarms.

Item 3 result: DONE. Suite: 69 unit, 78 instrumented (new: AppLanguageTest 2, MigrationTest v2->v3, RepositoryDataTest gender, ValidationTest 2, MascotsTest), lint 0 errors / 5 dependency-version warnings (`docs/evidence/batch1/item3/suite.txt`). Mutation check 4 of 4 caught (`mutation_check.txt`). The language leak is gone: the user's emulator stays English through the suite (the settings test now restores through LocaleManager; with no activity AppCompat only kept the restore in memory, the same blind spot as the app bug). Snapshot `../tracker-snapshots/batch1-item3/`.

### Item 4+5 checkpoint: palette and modern UX

```text
Contract: Material 3 with palette "Calm and Growth" (green/blue/teal tokens, light and dark), green-to-blue gradient on header, FAB and celebration backdrop, every text pair >= 4.5:1, semantic red/amber kept, splash screen, fade-through between screens, spring press, smoother reward overlay; 3 s celebration, tap to close, age-group rules, one PIN dialog, rotation safe kept; nothing breaks with animations off.
Must change: colors (light + night), themes, drawables, layouts using old colors, MainActivity (splash, FAB), Motion helper, Celebration, versions (Material, splashscreen, dynamicanimation), ContrastTest.
Must not change: data, PIN flow, reminder logic, strings.
Worst cases: unreadable text on the light gradient (white on green-400 is 1.9:1); dark mode broken; a crash from the new Material version; animations waiting forever with scale 0.
Likely wrong implementation: copying Tailwind 400/500 tones as text colors; android:background on the extended FAB.
Tests that catch it: ContrastTest (both palettes, gradient ends, glass blended over each end) with a mutation check; full suite; emulator screenshots light/dark/EN/AR; animations-off run; aapt check.
```

Item 4+5 verified (evidence `docs/evidence/batch1/item45/`):

| Check | Result | Evidence |
|---|---|---|
| ContrastTest light and dark (every pair the screens use >= 4.5:1) | PASS after darkening 3 tokens it caught | ContrastTest |
| Mutation: lighten on_surface_variant, primary back to green-400, lighter dark gradient end | 3 of 3 caught | mutation_check.txt |
| Home, settings, dialog in light and dark, English and Arabic (RTL, FAB on the left in Arabic) | PASS | 01 to 04, 14 to 16 |
| Onboarding, PIN dialog, reward overlay child and teen (gradient backdrop, avatar, readable points/streak) | PASS | 05, 08, 09, 11 |
| Crash found and fixed: extended FAB with a custom background throws on Material 1.14 | fixed (wrapper) | |
| Reward card tinted green by Material 3 elevation | fixed (explicit token color) | 11 |
| Animations off (window, transition, animator 0): reward shows and closes by itself, settings and PIN prompt work, no crash; scales restored (animator scale back to unset) | PASS | 13 |
| aapt: no INTERNET, allowBackup 0x0 | PASS | 17 |
| Suite: 72 unit, 78 instrumented, lint 0 errors / 4 dependency-version warnings | PASS | suite.txt |

Not built: Lottie (no `design/lottie/` folder). Not verified: real-device smoothness of the springs (emulator uses software rendering).

Item 4+5 result: DONE. Snapshot `../tracker-snapshots/batch1-item45/`.

### Item 6 checkpoint: any screen size

```text
Contract: phones small to large, tablets, foldables, landscape, multi-window, font up to 2.0; no orientation lock, resizeableActivity; content max 600dp centered, dialogs max width, chips wrap, home 2 columns from 840dp; nothing clipped or overlapping.
Must change: home list (blocks per patient, grid), chips container, Responsive helper, all screens' content containers, dialogs' onStart, manifest, item_dose (action under the name when narrow).
Must not change: data, PIN flow, reminder logic, the look on a normal phone at font 1.0.
Worst cases: text squeezed to nothing at font 2.0; chips filling a small screen; dialogs stretched across a tablet; patients split across columns.
Likely wrong implementation: GridLayoutManager over the flat item list (headers and doses of different patients would interleave).
Tests that catch it: ResponsiveTest (+ mutation check), the size matrix on the emulator (4 sizes x 3 fonts EN, AR at 1.5), rotation with the PIN dialog.
```

Item 6 verified on the emulator (evidence `docs/evidence/batch1/item6/`):

| Check | Result |
|---|---|
| 480x800 @240, 1080x2400 @420, 1600x2560 @320, 2208x1840 @420, each at font 1.0 / 1.5 / 2.0 (English): all 4 patient chips visible in every case (`matrix_log.txt`), home and settings screenshots | PASS |
| Arabic at font 1.5 on all four sizes; PIN dialog limited in width on tablet and unfolded sizes | PASS |
| Found and fixed: dose name squeezed to one letter per line (small, font 2.0) -> action under the name | fixed |
| Found and fixed: wrapped pinned chips filled the small screen -> they scroll away on short screens | fixed |
| Found and fixed: header text under the status bar clock when scrolled -> gradient band | fixed |
| Two columns on the unfolded size, one column on the 800dp tablet (below 840dp), content centered | PASS |
| Landscape with the PIN dialog open: still one dialog with the field | PASS |
| ResponsiveTest 3 tests; mutation check 3 of 3 caught | PASS |
| Suite after the change: 72 unit, 78 instrumented, lint 0 errors | PASS |
| Emulator reset: wm size and density back to physical 1080x2400 / 420, font 1.0, auto-rotate on, app language English | done |

Not verified: a real foldable's hinge and posture changes, real multi-window resizing by drag (the rules use the window size, so they apply, but no drag test was run).

Item 6 result: DONE. Snapshot `../tracker-snapshots/batch1-item6/`.

### Batch 1 result

All items done (0, 1, 2, 3, 4+5, 6). Contract updated in place (sections 0, 2, 5, 7, 8, 9, 9b, 10, 14), CHANGELOG has one entry per item, decisions D23 to D27. Deviations from the request, each logged: gender is schema v3 (v2 was taken by the alarm sound option, D24/D25); "reset points and streaks" and a global re-reminder interval are not built (D23); Lottie not added because `design/lottie/` does not exist (D26); dialogs stay Material 3 dialogs rather than bottom sheets (D26). Incidents, both recovered: the user's emulator data was deleted once by a mutation run (restored from the 14:39 backup, cause fixed), and the user's app language was switched to Arabic by a test (two causes fixed: a test race and an app bug in the language sync).

## After the run: Erase all data (user request, 2026-10-16)

Request: "add a reset button in setting screen to remove a cache of app". Asked what to delete (the app keeps nothing visible in the cache folder); the user chose "Erase everything". Decision D21.

Checkpoint:

```text
Contract: Erase all data in parent settings. Second confirmation, then parent PIN. Deletes every row of every table, cancels all reminders and notifications, clears the cache folder, then the app restarts on the language screen.
Must change: TrackerRepository (eraseAllData), PillPalsApp (orchestration), SettingsActivity, activity_settings.xml, strings EN/AR, one instrumented test file, docs.
Must not change: schema (v1), PIN reset (still keeps data), alarm rules, any other screen.
Worst cases: a stale alarm fires for a deleted slot; ids reused so an old alarm hits a new medicine; one table missed (old points come back); erase runs without the PIN.
Likely wrong implementation: deleting only some tables, or not cancelling the alarms first.
Tests that catch it: instrumented RepositoryEraseTest with exact before/after counts for all 9 tables, settings back to defaults, ids not reused, old slot/dose alarms find nothing, new points start from 0.
```

Report:

```text
Changed: TrackerRepository.eraseAllData (Room clearAllTables in one transaction, returns slot and today's dose ids), PillPalsApp.eraseAllData (cancel alarms, cancel all notifications, empty cache folder), SettingsActivity (confirm dialog, then PIN, then erase in the app scope, then MainActivity with NEW_TASK | CLEAR_TASK), activity_settings.xml (new "Erase data" section), 7 strings in values and values-ar, RepositoryEraseTest (3 tests), README, DECISIONS D21.
Tests: see "Verified" below for exactly what ran.
Failure modes checked: Cancel deletes nothing; wrong PIN deletes nothing (counts the try); Back after the erase leaves the app (no old screen in the task); a second erase right after a first works; erase with nothing stored is harmless.
Remaining risks: if the phone is turned off in the middle, the DB part is one transaction (all or nothing); alarms not cancelled stay silent because receivers read the DB and ids are not reused. Arabic copy needs the native review like the rest.
```

Verified (evidence in `docs/evidence/erase-data/`):

| Check | Result | Evidence |
|---|---|---|
| RepositoryEraseTest (3 instrumented tests on the emulator): exact counts of all 9 tables before (2/2/14/6/2/3/3/1/1) and after (all 0), settings equal the first-launch defaults, PIN gone, refs = all 14 slot ids and today's 2 dose ids, old slot and follow-up alarms find nothing, new ids above the old ones, new points start at 35 not 190, empty DB erase harmless | PASS | 12_full_test_results.txt |
| Full suite after the change: `gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`, 64 unit + 58 instrumented tests, lint 0 errors | PASS | 12_full_test_results.txt |
| Mutation check: 5 planted bugs (patients only, no slot refs, wrong-day dose refs, ids reused, settings kept), all caught | 5/5 KILLED | mutation_check.txt, mutation_check_5b.txt |
| Emulator, English: section and button at the bottom of parent settings | PASS | 02 |
| Cancel in "Erase everything?": nothing deleted, no PIN prompt | PASS | 03 |
| Wrong PIN: nothing deleted, "4 tries left" | PASS | 04, 05 |
| Right PIN: language screen, only OnboardingActivity in the task, Back goes to the launcher | PASS | 06, 07_after_erase.txt |
| After the erase: all tables 0 (plus the fresh default settings row), 14 slot alarms gone (only the 00:05 nightly check left), test notification gone, cache folder empty, notification permission still granted, sqlite_sequence kept | PASS | 01_before_erase.txt, 07_after_erase.txt |
| Emulator, Arabic: section, confirm dialog and PIN prompt mirrored RTL; second erase lands on the language screen | PASS | 08 to 11 |

Not verified: font scale 1.5 and landscape for the new section (it is at the end of the existing scroll view, same styles as the sections above); real phone.

Emulator left as found: app installed on the English language screen (per-app locale reset with `cmd locale set-app-locales`), font scale 1.0, automatic time on.

## After the run: keyboard covered the PIN card on the first screen (user report, 2026-10-16)

Cause: the screen is edge-to-edge (`enableEdgeToEdge()`), where `adjustResize` no longer shrinks the window. Onboarding only applied the status and navigation bar insets, so the keyboard covered the second PIN field and Save. The medicine form already handled the keyboard inset.

Fix: `activity_onboarding.xml` wraps the scroll view in a FrameLayout root that takes `systemBars() or ime()` as padding (same idiom as `MedicineEditActivity`), so the scroll view ends above the keyboard. When the keyboard opens, `OnboardingActivity.showWholePinCard()` scrolls so both fields and Save are above it; if the card does not fit (landscape), the scroll view keeps the focused field in view.

Verified on the emulator (evidence in `docs/evidence/onboarding-keyboard/`):

| Check | Result | Evidence |
|---|---|---|
| Before the fix, Arabic: second field half hidden, Save behind the keyboard | reproduced | 01 |
| After the fix, Arabic: title, both fields and Save above the keyboard | PASS | 02 |
| Typing the PIN, soft keyboard Next, typing the repeat: Save still visible | PASS | 03 |
| Keyboard hidden: layout back to centered, no leftover gap | PASS | 04 (and the uiautomator positions) |
| Landscape: the keyboard uses its own full-screen typing box with Next (standard Android for phones in landscape) | PASS | 05 |
| Font scale 1.5: whole card above the keyboard | PASS | 06 |
| English | PASS | 07 |
| `gradlew testDebugUnitTest lintDebug assembleDebug`: 64 unit tests pass, lint 0 errors | PASS | |

Not run: instrumented tests (the Gradle task uninstalls the app afterwards and would have deleted the data the user entered on the emulator; this change touches only the onboarding layout and its activity). Not verified: the user's Samsung phone (SM-A266B) was connected at the same time and was not used, so the Samsung keyboard stays UNVERIFIED (needs real device).

The emulator held the user's own data (two patients). To reach the PIN step without losing it, the app database was backed up, only the stored PIN was cleared for the test, and the backup was copied back afterwards. Data, PIN, Arabic app language and the 14 reminder alarms were checked after the restore (08). Font scale 1.0 and auto-rotate were restored.

## After the run: celebration shown for 3 seconds (user request, 2026-10-16)

Change: `Celebration.kt` uses one constant, `SHOW_MILLIS = 3_000`, for every reward and streak-ended screen and every age group (was 2.5 to 5.5 s). Tap to close is unchanged. Decision D22.

Verified (evidence in `docs/evidence/celebration-3s/`, method and numbers in `03_summary.txt`):

| Check | Result |
|---|---|
| `gradlew testDebugUnitTest lintDebug assembleDebug`: 64 unit tests pass, lint 0 errors (only the old Overdraw warning on view_celebration.xml) | PASS |
| Emulator, adult reward (was 2.5 s): backdrop visible between 2.60 and 3.23 s | PASS |
| Emulator, child reward (was 4.5 s): about 2.3 to 2.8 s between screenshot timestamps plus up to 0.4 s screenshot delay under confetti load | consistent with 3 s |

Not measured on the device: teen reward and streak-ended screens (same constant and code line). Not run: instrumented tests (they uninstall the app and would delete the user's emulator data; the change is UI timing only).

Test data: the user's emulator database was backed up, the app ran on an empty database with two test patients (Yasmin 7, Omar 40), then force-stop (cancels the test alarms and notifications) and the backup was copied back. Afterwards: the user's two patients with 35 points each, Arabic app language, 14 reminder alarms plus the nightly check, no notifications left.

## After the run: compiler and lint warnings cleaned up (user request, 2026-10-16)

Before: 8 Kotlin compiler warnings (deprecated `bundleOf`) and 25 lint warnings, 0 errors. After: 0 compiler warnings in app code, 0 lint errors, 5 lint warnings left, all of them "a newer version is available" in `gradle/libs.versions.toml` (left on purpose: dependency upgrades are outside `app/` and change library behavior; they need their own test pass).

| Warning | Fix |
|---|---|
| `bundleOf` deprecated (Celebration, ConfirmDialogFragment, PatientDialogFragment, PinDialogFragment) | Platform `Bundle().apply { putX(...) }` with the same keys and types |
| SetTextI18n: numbers set with `toString()` (MainActivity points, streak, level; PatientDialogFragment age) | New `Fmt.number()`: `String.format(Locale.ROOT, "%d")`, always digits 0-9 (CLAUDE.md 9b) |
| PluralsCandidate: "digits" and "letters" | `pin_format_error` and `err_name_too_long` are now plurals in English and Arabic (all six Arabic forms); the text for the real limits is unchanged |
| PluralsCandidate: "X of Y taken" (3 strings) | Marked as intended: no word depends on the numbers in either language |
| Overdraw: activity roots painted the same color as the window | Root background removed from activity_main, activity_manage, activity_medicine_edit, activity_settings; marked as intended on the celebration overlay (it must dim the home screen) |
| UseKtx | `"package:...".toUri()` in MainActivity; `Canvas.withTranslation` in ConfettiView |
| DrawAllocation: `PathMeasure` created in `onDraw` | Allocated once in CheckmarkView and reused |
| InlinedApi: exact-alarm broadcast (API 31) | Added to SystemEventsReceiver's set only on Android 12+ |
| AppBundleLocaleChanges | `bundle { language { enableSplit = false } }`: an app bundle keeps both languages for the in-app switch |
| UnusedResources: `color/black` | Removed |
| Build script: `srcDir` deprecated (seen once the script was recompiled) | `assets.directories.add(...)`; SchemaTest still finds the exported schema |

The user's own change in activity_settings.xml (vertical language buttons) was kept.

Verified:

| Check | Result |
|---|---|
| `gradlew compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin --rerun-tasks`: no `w:` lines | PASS |
| `gradlew testDebugUnitTest lintDebug assembleDebug`: 64 unit tests pass, lint 0 errors, 5 dependency-version warnings | PASS |
| `gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` on Pixel_API_34: 60 tests pass (2 new in LocalizationTest: plural texts for the real limits, `Fmt.number` keeps 0-9 under an `ar-EG` default locale) | PASS |
| Unused imports in every Kotlin file (script check, since Android Studio was open and its own inspections could not run headless) | none |
| Emulator screenshots of home, settings, manage and add medicine in Arabic after removing the root backgrounds | same look, `docs/evidence/lint-cleanup/` |

The user's emulator data (the user's two patients, 35 points each, 14 reminder times, Arabic, PIN) was backed up before the instrumented run, which was told to leave the app installed; it was checked intact afterwards. The leftover test package was uninstalled and the backup removed from the emulator. The emulator had been closed in between and was started again, this time saving its state normally on exit.

## Phase 2 checkpoint

```text
Contract: one exact alarm per slot for its next occurrence; notification names the patient and is
  localized in the app language at fire time; tap opens the confirm step of that exact dose (cold
  and warm start); follow-ups at the medicine's interval, exactly 3, stop on confirm; alarms
  re-armed from the DB on every launch and by boot, time change, timezone change, app update;
  deleting or editing cancels old alarms; missing notification or exact-alarm permission shows a
  home banner with a fix button and falls back to inexact alarms; battery settings hint.
  Phase 0 items 1, 2, 3, 7.
Must change: reminders/*, repeat timing moved into a pure rule for tests, tests, docs.
Must not change: schema v1, Phase 1 behavior, no new libraries.
Worst cases: alarm silently never fires; reminder for a deleted medicine; a follow-up after the
  pill was confirmed; more than 3 follow-ups, or the count restarting after a kill; tap opens the
  wrong dose or the wrong patient; notification in the old language; weekday medicine firing daily.
Likely wrong implementation: alarms kept only in memory; request codes colliding between slots;
  repeat count stored in the PendingIntent instead of the DB; notification text built once at
  scheduling time; re-arm forgotten after force-stop.
Tests that catch it: JUnit ReminderRules (due times, max 3, late catch-up); instrumented
  onSlotDue/onRepeatDue (announce once, 3 repeats then stop, stop after confirm, other day,
  deleted slot, running repeats after kill); emulator: real fire at slot time with patient name,
  3 follow-ups then silence, confirm stops follow-ups, tap cold/warm, deleted medicine silent,
  edited slot, reboot/force-stop/time/timezone/app update re-arm, permission denied banners, test
  reminder button, Arabic notification text at fire time, weekday-only medicine via emulator clock.
```

## Phase 3 checkpoint

```text
Contract: parent PIN created on first launch (after the language), stored only as a salted hash;
  required for confirming a pill (toggle in parent settings), add, edit, delete, settings;
  5 wrong tries lock entry for 60 s and the lock survives restart and process kill; forgot PIN
  only through the phone's own screen lock, keeping every patient, medicine, dose, point and
  streak; disabled with an explanation when the phone has no screen lock; one PIN dialog at a
  time, surviving rotation. Phase 0 item 6.
Must change: PIN fields keep no typed digits in saved state; tests; docs.
Must not change: schema v1, reminders, rewards.
Worst cases: a wrong PIN confirms a dose; lockout lost by killing the app; the 5th try not
  locking or the 4th already locking; reset wipes data; reset possible without the phone lock;
  PIN readable from the DB; two PIN dialogs stacked; toggle off also disables the other gates.
Likely wrong implementation: lockout counter kept in memory; comparing hashes with the PIN in
  plain text; reset implemented as "clear app data"; toggle wired to every gate.
Tests that catch it: JUnit PinRules/PinHasher/PinLockout (format 3/4/8/9 digits, salt makes
  hashes differ, hash never contains the PIN, 4 vs 5 failures, 60 s boundary, clock moved back,
  success resets); instrumented (lock survives a new repository on the same DB, reset keeps
  all data exact, wrong PIN leaves the dose PENDING); emulator: gates, toggle off, lockout across
  am kill, rotation and repeated taps, reset with locksettings PIN, no-lock explanation.
```

## Phase 4 checkpoint

```text
Contract: +10 per pill, +5 within 30 min, +20 perfect day once per patient per day, +50 at
  3/7/14/30/60/100 once per milestone per run, level = floor(points/100)+1, bestStreak never
  decreases, points never deducted. Streak per patient: missed day breaks it, zero-dose day is
  neutral, evaluation idempotent on app open and the daily alarm, several missed days = one
  break and one animation, shown once. Reward and streak-broken animations differ for CHILD,
  TEEN, ADULT, never shame, never block the next action, survive rotation, mirror in RTL.
Must change: show the next pending streak-broken animation when one closes (siblings); tests;
  Arabic plural checks; docs.
Must not change: schema v1, reminder and PIN behavior.
Worst cases: double points (same dose twice, delete and re-add, perfect day twice, milestone
  twice); a missed day not breaking the streak; a neutral day breaking it; one animation per
  missed day; the animation replaying on every open; patient A's action changing patient B;
  a dose confirmable after midnight; a clock moved backward giving negative streaks or bonuses
  again; Arabic plurals wrong for 1, 2, 3, 11.
Likely wrong implementation: streak recomputed from scratch on each open; perfect day as a
  boolean on rewards; milestone check "streak >= 3"; confirm window based on "last 24 h".
Tests that catch it: JUnit PointsRules/DoseRules/StreakRules with exact values; instrumented
  RepositoryRewardsTest (every invariant of section 6 and rule of 6b with an injected clock,
  incl. month/year boundary, Cairo DST, timezone change, clock backward) and LocalizationTest
  (Arabic plurals 0/1/2/3/11/100 with Western digits); emulator: three reward styles, three
  streak-broken styles, shown once, rotation, RTL.
```

## Phase 5 checkpoint

```text
Contract: signed release APK (name "Pill Pals", own icon, versionName/versionCode) built from a
  keystore.properties file that is never committed; no real keystore created by me (only a
  throwaway one in a temp folder, deleted afterwards); assembleRelease fails with a clear message
  when keystore.properties is missing; docs/RELEASE.md with the exact keytool command; README
  with install steps and limits for parents; clean-copy build; release APK installs on a clean
  emulator and the main flow works; no INTERNET permission (aapt), allowBackup false.
Must change: app/build.gradle.kts (signing, version), launcher icon, .gitignore, README.md,
  docs/RELEASE.md, docs.
Must not change: app behavior, schema v1, dependencies.
Worst cases: keystore or passwords committed; release build silently signed with the debug key;
  release build that cannot be updated later (lost key); INTERNET permission pulled in by a
  library; backup of children's data enabled; release crashing because of shrinking.
Likely wrong implementation: passwords written into build.gradle.kts; signingConfig falling back
  to debug; minify on without Room keep rules.
Tests that catch it: assembleRelease without keystore.properties (must fail with the message);
  with the throwaway keystore (apksigner verify shows the test certificate, not the debug one);
  aapt dump permissions; install on a clean emulator and run the flow; full test suite again.
```

## Phase 5 report (DONE 2026-10-16)

```text
Changed: release signing from keystore.properties (never committed) with a check task that stops
  release builds without it; versionName 1.0.0 (versionCode 1); own launcher icon (adaptive, with
  monochrome layer), template webp icons removed; .gitignore ignores keystore.properties, *.jks,
  *.keystore; README rewritten for parents and developers; docs/RELEASE.md.
Tests: assembleRelease without keystore.properties fails with the message; with a throwaway
  keystore it is signed by that key (apksigner); aapt: no INTERNET, allowBackup=0x0, no
  debuggable; the release APK installed on a clean app state and the full flow worked on the
  emulator (onboarding, add, permission, reminder at 03:05:01, tap, PIN, reward). Clean copy
  without build folders or local.properties builds and passes 64 unit tests with only
  ANDROID_HOME. Final suite on the project: 64 unit + 55 instrumented, lint 0 errors.
Failure modes checked: missing keystore, wrong signer, INTERNET permission, backup flag,
  debuggable flag, build from a clean copy.
Remaining risks: the real keystore does not exist yet (user step, docs/RELEASE.md); real phone
  test and native Arabic review are still open (FINAL_REPORT.md).
```

### Phase 5 verified (evidence in `docs/evidence/phase-5/`)

| Check | Result | Evidence |
|---|---|---|
| assembleRelease without keystore.properties fails with a clear message | PASS | 01_release_without_keystore.txt |
| Release signed with the supplied (throwaway) keystore, v2 signature verifies | PASS | 02_release_signed_with_throwaway.txt |
| No INTERNET permission in the release APK (aapt dump permissions) | PASS | 02 |
| allowBackup=false, not debuggable, versionName 1.0.0, label Pill Pals | PASS | 02 |
| Release APK installs on a clean app state; full flow incl. a real reminder works | PASS | 03_release_first_launch.png, 04, 05, 06, 03_release_full_flow.txt |
| Clean copy (no build folders, no local.properties) builds and tests with ANDROID_HOME only | PASS | 04_clean_copy_build.txt |
| Launcher icon | PASS | 07_launcher_icon.png |
| Throwaway keystore, keystore.properties and the throwaway-signed APK deleted afterwards | PASS | 02 (cleanup line) |
| Final full suite | PASS | 05_final_test_results.txt |

## Phase 4 report (DONE 2026-10-16)

```text
Changed: next pending streak-broken animation shown when one closes (siblings); home shown before
  the launch work finishes (was blank for seconds on the slow emulator); progress bar while the
  PIN hash is checked; PIN dialog pans instead of resizing in landscape. Reward and streak logic
  and the three animation styles were written in Phase 1 and are verified here.
Tests: 64 JUnit (RewardsTest 15 new) and 55 instrumented (RepositoryRewardsTest 16 and
  LocalizationTest 7 new), all passing; lint 0 errors.
Failure modes checked: every invariant of CLAUDE.md 6 and rule of 6b with exact totals and an
  injected clock (same dose twice, delete and re-add, perfect day once, milestone once per run and
  again only for a new run, 23:59 vs 00:00, A never changes B, zero-dose day neutral, missed day
  breaks, several days away = one animation, shown once, idempotent evaluation, month/year
  boundary, Cairo DST, timezone change, clock backward); Arabic plurals and digits on the device.
Remaining risks: the emulator is slow (software rendering): PIN check over 1 s and a cold start of
  several seconds; real phone speed UNVERIFIED. Egyptian copy still needs native review.
```

### Phase 4 verified on the emulator (evidence in `docs/evidence/phase-4/`)

| Check | Result | Evidence |
|---|---|---|
| CHILD reward: confetti, trophy/cheer mascot, points count-up, streak line, in RTL | PASS | 01 to 03_child_reward_*_ar.png |
| TEEN reward: medal badge, short confetti, points and streak | PASS | 04_teen_reward_frame1_ar.png, 05_teen_reward_frame2_ar.png |
| ADULT reward: animated checkmark, points and streak as text, no cartoon | PASS | 05_adult_reward_frame2_ar.png |
| Signed numbers in Arabic read "+10", "+20" (fixed in Phase 2) | PASS | 03, 05 |
| CHILD streak-broken: sleepy mascot, then sprout and "ولا يهمك، نبدأ من جديد!" | PASS | 06_broken_child_ar.png |
| TEEN streak-broken: neutral, lost streak count, "نبدأ تاني" | PASS | 07_broken_teen_ar.png |
| ADULT streak-broken: minimal icon and text with best streak | PASS | 08_broken_adult_ar.png |
| Siblings' animations shown one after another, each once; none after a relaunch | PASS | 09, 10_broken_sequence_log.txt, 11_shown_once.txt, 11_relaunch_no_replay.png |
| Streak break never changes points (30 before and after for all three) | PASS | 10, 11 |
| Reward overlay survives rotation (final state, scrollable in landscape) | PASS | 12_reward_after_rotation_landscape_ar.png |
| PIN entry in landscape with the keyboard (fixed with adjustPan) | PASS | 13_pin_dialog_landscape_keyboard_ar.png |
| Arabic plurals 0/1/2/3/10/11/100 days and points, Western digits, ص/م, Saturday first | PASS | instrumented LocalizationTest (test_results.txt) |
| Mutation check: 9 planted bugs (6 unit, 3 instrumented) all caught | PASS | mutation_check.txt |

Emulator hygiene: the clock was moved to 2026-10-18 for the streak-broken test and automatic time was restored right after (`auto_time=1`, `Africa/Cairo`).

## Phase 3 report (DONE 2026-10-16)

```text
Changed: PIN input fields no longer keep typed digits in saved state (saveEnabled=false). The PIN
  flow itself was written in Phase 1 and is verified here.
Tests: 49 JUnit (PinTest 9 new) and 32 instrumented (RepositoryPinTest 6 new), all passing;
  lint 0 errors.
Failure modes checked: wrong PIN on confirm, 4 vs 5 wrong tries, lock across force-stop, Confirm
  disabled while locked, rapid taps, rotation with the dialog open, toggle off for confirm only,
  reset only after the phone lock with all data identical, reset disabled without a phone lock.
Remaining risks: a 4-digit PIN can be brute-forced offline by someone with root and the DB file
  (D7). A child who knows the phone's lock code can reset the PIN (README for parents). A child
  can move the phone clock to shorten the lock (capped at 60 s, never longer).
```

### Phase 3 verified on the emulator (evidence in `docs/evidence/phase-3/`)

| Check | Result | Evidence |
|---|---|---|
| Wrong PIN on the confirm gate: dose stays PENDING, 0 points, failure counted in the DB | PASS | 01_confirm_gate.png, 02, 02_wrong_pin_never_confirms.txt |
| Gates: confirm, add, edit, settings each ask for the PIN (delete is behind edit/settings) | PASS | 03_gate_*.png, 03_gates.txt |
| Three rapid taps open one PIN dialog; rotation keeps that one dialog (Phase 0 item 6) | PASS | 04_pin_dialog_after_rotation.png, 04_no_pile_up.txt |
| 4th wrong try: "1 try left"; 5th: locked 60 s with countdown, Confirm disabled | PASS | 05_fourth_wrong_try.png, 06_locked_60s.png, 05_lockout.txt |
| Lock survives a real process death (force-stop) with the remaining time | PASS | 07_lock_survives_kill.png, 05_lockout.txt |
| Toggle off: confirm asks yes/no only; add and settings still ask for the PIN | PASS | 08_toggle_off.png, 09_confirm_without_pin.png, 06_toggle_off.txt |
| Forgot PIN with a phone lock: system credential screen, then new PIN; all tables identical; old PIN fails, new works | PASS | 10, 11_device_credential_screen.png, 12_reset_mode.png, 07_reset_via_phone_lock.txt |
| No phone lock: Forgot PIN disabled with the explanation | PASS | 13_forgot_disabled_no_lock.png |
| DB holds only salt + PBKDF2 hash, the PIN never appears; no logging in app code | PASS | 08_db_pin_storage.txt |
| Mutation check: 8 planted bugs (6 unit, 2 instrumented) all caught | PASS | mutation_check.txt |

Emulator hygiene: the phone screen lock set for the reset test was removed again (`locksettings clear`).

## Phase 2 report (DONE 2026-10-16)

```text
Changed: follow-up timing moved into domain/ReminderRules (used by AlarmScheduler and the
  repository); exact-alarm settings button falls back to the app details page; Arabic signed
  numbers isolated (+10 showed as 10+ in RTL). Reminder code itself was written in Phase 1.
Tests: 40 JUnit (ReminderRulesTest 5 new) and 26 instrumented (RepositoryReminderTest 8 new)
  on the API 34 emulator, all passing; lint 0 errors.
Failure modes checked on the emulator: real fire time, follow-up count and stop, confirm stops
  follow-ups, tap warm and cold, deleted and edited medicines silent, process kill, reboot,
  force-stop, app update, time change, timezone change, notification permission revoked,
  exact-alarm permission missing (temporary build), weekday-only and date-range via clock.
Remaining risks: OEM battery managers and real exact-alarm grant on a phone (UNVERIFIED, needs a
  real device). App standby deferral seen in Phase 0 did not happen in this run (bucket 10).
```

### Phase 2 verified on the emulator (evidence in `docs/evidence/phase-2/`)

| Check | Result | Evidence |
|---|---|---|
| Reminder fires at the slot time (01:30:00.044, seen 01:30:02), child wording with patient name | PASS | 03_reminder_0130_child_en.png, 03_fire_0130.txt |
| Teen/adult wording "Omar: time for Vitamin D" | PASS | 04_fire_0135.txt |
| Edited slot: old time silent, new time fires; deleted medicine silent | PASS | 01_setup_before_fire.txt, 03, 04 |
| Follow-ups at the chosen interval (01:35, 01:40, 01:45), exactly 3, none at 01:50 | PASS | 04, 07, 08_followup3_after_kill_and_stop.txt |
| Notification language = app language at fire time (follow-up 2 in Arabic) | PASS | 07_followup2_arabic_0140.png, 07_followup2_arabic.txt |
| Follow-up 3 fired with the app process killed, count from the DB | PASS | 08 |
| Tap (warm start) opens the confirm step of exactly that dose; confirm clears the notification and its follow-up alarm | PASS | 05_tap_warm_opens_vitamin_d.png, 05_warm_tap_confirm.txt |
| Tap (cold start, process killed) opens the confirm step of exactly that dose | PASS | 09_tap_cold_opens_panadol_ar.png |
| App update (MY_PACKAGE_REPLACED) re-arms without opening the app | PASS | 11_app_update_rearm.txt |
| Reboot during a follow-up sequence: follow-up re-armed at the same time, fired with count 1 | PASS | 14_reboot_running_followup.txt |
| Force-stop: 0 alarms; launch re-arms everything incl. the running follow-up (Phase 0 item 1) | PASS | 15_force_stop_running_followup.txt |
| Time change (clock moved back) re-arms; auto time restored | PASS | 16_time_change_rearm.txt |
| Timezone change keeps local wall-clock times; timezone restored | PASS | 17_timezone_change_rearm.txt |
| Notification permission missing: home banner, settings status, no crash, nothing posted | PASS | 02_banner_notifications_off.png, 12, 13, 13_permission_denied_and_test_reminder.txt |
| Test reminder button: arrives about 10 s later | PASS | 13 |
| Exact alarms granted automatically on API 34 via USE_EXACT_ALARM (Phase 0 item 2) | PASS | 11, 14 (window=0, exactAllowReason=policy_permission) |
| Exact alarm missing (temporary test build only): banner "up to an hour late", Fix opens the system screen, alarms fall back to inexact, re-armed after reboot (Phase 0 item 3) | PASS | 18_banner_exact_missing.png, 19, 20_no_exact_permission.txt |
| Notification permission asked only after language, PIN and first medicine, with an explanation (Phase 0 item 7) | PASS | phase-1 11, 12 |
| Weekday-only medicine fires Monday, silent Tuesday and Wednesday; date-range medicine fires only inside its dates | PASS | 22_weekday_and_date_range_clock.txt |
| Mutation check: 6 planted bugs (3 unit, 3 instrumented) all caught | PASS | mutation_check.txt |

Note: the device clock was moved for tests 16 and 22 and automatic time and timezone were restored right after (`auto_time=1`, `auto_time_zone=1`, `Africa/Cairo`).

## Phase 1 checkpoint

```text
Contract: Room v1 database is the single source of truth (patients, medicines, weekday slots,
  doses, rewards, award ledgers, settings incl. PIN and language). Multiple patients per phone,
  medicine always tied to one patient, flexible weekday schedule with optional start/end.
  Arabic (Egyptian, primary) + English with RTL, language picker before PIN creation.
  Home: empty state, "+" to add, filter chips All + one per patient, sections per patient,
  status derived from current data. Fix Phase 0 items 4 (derived perfect day) and 5 (landscape overlap).
Must change: app/ (build config, data layer, UI, strings en + ar), gradle/libs.versions.toml and root
  build.gradle.kts (Room + KSP plugins, see DECISIONS D1), docs/.
Must not change: no new libraries beyond Room/KSP, no INTERNET permission, no network code,
  no fallbackToDestructiveMigration, nothing outside the project/SDK/AVD.
Worst cases: two patients with the same name merged or cross-wired; deleting one patient deletes
  the other's data; duplicate doses; a Monday-only medicine producing doses every day; weekday
  off-by-one; Arabic plurals or digits wrong; RTL not mirrored; form lost on rotation; "Perfect day"
  stuck after adding a medicine; schema not exported.
Likely wrong implementation: lookups by patient name; Calendar weekday (Sunday = 1) instead of ISO;
  doses created eagerly for all days; deleting by name; age stored as a fixed number that never
  grows; perfect-day as a stored flag; String.format in Arabic producing Eastern Arabic digits.
Tests that catch it: JUnit (age boundaries 12/13/17/18 and aging after a year, ISO weekday
  Monday=1 Sunday=7, Monday-only vs daily control, per-day times, start/end range, validation of
  empty/invalid input incl. Arabic names, age 0..120, duplicate slots, end before start);
  instrumented Room tests (unique dose index, cascade delete with control patient, same-name
  patients independent, ensureDosesFor idempotent, filter query excludes other patient, schema
  export + migration harness); emulator checks in EN and AR (RTL), rotation, process kill, landscape.
```

## Phase 1 report (DONE 2026-10-16)

```text
Changed: Room v1 (9 tables, schema exported to app/schemas), TrackerRepository, pure domain rules
  (domain/*.kt), new screens (onboarding, home with filter, add/edit medicine, parent settings,
  patients and medicines), dialog fragments, strings en + ar (values-ar), manifest (allowBackup
  false, localeConfig, no INTERNET), backup rules exclude everything, lint rules as errors.
  Old SharedPreferences code removed. Reminder/PIN/reward code also written now (schema needs it),
  verified in their own phases.
Tests: 35 JUnit (AgesTest 6, ScheduleTest 11, ValidationTest 17, template 1) and 18 instrumented
  (RepositoryDataTest 16, SchemaTest 1, template 1) on the API 34 emulator, all passing.
  Lint 0 errors with MissingTranslation, HardcodedText, RtlHardcoded, RtlSymmetry as errors.
Failure modes checked: same-name patients, delete with control patient, unique dose index, FK
  enforcement, transaction rollback, Monday-only vs daily, start/end range, slot added after its
  time, edit keeps TAKEN history, filter view-only, deleted selected patient falls back to All,
  derived perfect day, rotation, process death with Don't keep activities, force-stop, reboot,
  landscape, RTL, font scale 1.5, per-app language in Android settings.
Remaining risks: Arabic copy not reviewed by a native speaker; Arabic typing in forms not tried
  through adb (D16); dropdown popups are not visible to uiautomator (tapped by position).
```

### Phase 1 verified on the emulator (evidence in `docs/evidence/phase-1/`)

| Check | Result | Evidence |
|---|---|---|
| First launch: language picker before anything else, no notification prompt | PASS | 01_language_picker.png |
| Choosing Arabic switches the app to RTL, DB stores `ar`, app locale `ar-EG-u-nu-latn` | PASS | 02_pin_create_ar.png |
| PIN creation (too short rejected with Western digits in Arabic), stored as PBKDF2 hash | PASS | 03, 04 |
| Empty state + "+" (FAB mirrored to the left in RTL) | PASS | 04_home_empty_ar.png, 05 |
| Settings reachable only after PIN; language switch to English updates DB and system | PASS | 06, 07 |
| Add form validation (patient name, age, medicine name, no times) | PASS | 09_add_form_errors_en.png, 39 (AR) |
| Add medicine with new patient; notification rationale only after the first medicine | PASS | 10, 11, 12, 13 |
| Same-name twins shown apart ("Yasmin · 7 (1)" / "(2)") | PASS | 15_home_twins_en.png |
| Specific days + different times per day stored as ISO slots (Tue 21:00, Sat 09:00) | PASS | 14, DB query in this report's run |
| Exactly one patient is preselected; with several, none is | PASS | 16_no_preselect_with_two_patients.png |
| End date stored as epoch day, start stays NULL | PASS | 17 |
| Home "All" grouped by patient with own points/streak/level | PASS | 18_home_all_en.png, 33 (AR) |
| Filter is view-only (doses and rewards identical before/after), saved in DB | PASS | 19_filter_teen_en.png, 19_filter_db_check.txt |
| Reboot: alarms re-armed by the boot receiver from the DB, exact | PASS | 21_alarms_after_reboot.txt |
| Force-stop removes all alarms; next launch re-arms all 31 (Phase 0 item 1) | PASS | 22_force_stop_then_launch.txt |
| Process kill: same state, filter kept | PASS | 20_after_kill_filter_kept_en.png |
| Edit: old slot alarms gone, new slots and today's dose replaced | PASS | 23, 24, 24_edit_slots_alarms.txt |
| Delete medicine: alarms cancelled (21:00 alarms 8 -> 1), patient kept | PASS | 25, 26 |
| Half-filled form survives rotation and process death (Don't keep activities) | PASS | 27, 28, 29, 29_form_state_checks.txt |
| Landscape: no control covered by "+", chips stay below status bar (Phase 0 item 5) | PASS (after 2 fixes) | 30, 31, 40 |
| Arabic: mirrored home, filter, settings, manage, edit; Saturday-first day order; Western digits; ص/م | PASS | 32 to 38 |
| Bidi: "Vitamin D" and "Augmentin 1g" inside Arabic sentences keep order | PASS | 35, 38 |
| Font scale 1.5 in Arabic, no truncated buttons | PASS | 41 to 44 |
| Android 13+ per-app language setting syncs to the DB both ways | PASS | 45 |
| USE_EXACT_ALARM granted on API 34 without user action (Phase 0 item 2) | PASS | 21 (exactAllowReason=policy_permission) |
| Mutation check: 12 unit + 2 instrumented planted bugs, all caught | PASS | mutation_check_unit.txt, mutation_check_instrumented.txt |

Bugs found and fixed during Phase 1 verification: app title not aligned to start in RTL; header said "0 of 0" for a patient without doses; "No pills today" shown twice; pinned chips slid under the status bar in landscape; "All" chip scrolled out of view in RTL; repeatOnLifecycle started from onResume (lint error).

### Phase 1 not verified

- Typing an Arabic name into the form (adb cannot type Arabic; covered by unit tests, D16). UNVERIFIED on device.
- Deleting a patient through the Manage screen on the emulator (covered by the instrumented test `deletingOnePatientLeavesTheOtherExact`, which runs on the emulator).

## Parking lot

- "Every N days" and "as needed" medicines: out of scope for v1 (CLAUDE.md 5c).
- Validation errors stay visible while typing until the next Save (cosmetic).
- Unused dependency `androidx.constraintlayout` (lint warning only).
- Header line of the collapsing app bar can peek above the chips mid-scroll in landscape (cosmetic, only while scrolling).
- One emulator freeze (qemu "not responding") happened while driving the UI; the AVD now runs with `-gpu swiftshader_indirect`.
- `bundleOf` is reported as deprecated by the current core-ktx (compiler warning only).
- Lint warnings (25, none errors): newer dependency versions available, PluralsCandidate on "x of y" strings, SetTextI18n for plain numbers (intentional: Western digits), Overdraw, UseKtx.
- The app ships library translations for many languages (aapt badging lists them); only Arabic and English are app languages. `localeFilters` could trim the APK later.
- Rotating while a PIN check is running cancels that check; the parent types the PIN again (digits are not kept on purpose, D17).
- Batch 3: Arabic chosen in Android's per-app language settings arrives as plain `ar` and shows Arabic-Indic digits (٧، ١٣٥); the in-app picker uses `ar-EG-u-nu-latn` (Western digits, 0c). Seen on the test AVD with `cmd locale set-app-locales --locales ar`; not fixed (outside the batch 3 list).
- Batch 3: `broken_adult_title` (Arabic) says "أطول سلسلة ليك" (second person, from the glossary); not in the item 4 copy list (rewards and empty states), left for the native review (`docs/ar-review.md` table 1).
- Batch 3: at 480x800 and font 2.0 the empty state text starts under the "Add medicine" button; it scrolls clear (112dp bottom padding, like the list), but at rest they overlap.

## Open risks

- Real phone battery managers (Xiaomi, Samsung, Oppo) are not testable on the emulator: UNVERIFIED (needs real device).
- Egyptian Arabic copy needs a native speaker review before release.

---

# Phase 0 record (2026-10-15, before the autonomous run)

## Environment used

- Emulator: AVD `Pixel_API_34`, image `system-images/android-34/google_apis/x86_64` (r14), 1080x2400, 420 dpi, WHPX acceleration. Visible in Android Studio Device Manager.
- SDK cmdline-tools installed at `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` (build 16111833). Note: `sdkmanager.bat` in this version wraps the new `android` CLI and splits package names on `;`. Use `android sdk install system-images/android-34/google_apis/x86_64` instead.
- JDK: Android Studio bundled JBR.
- APK under test: `app/build/outputs/apk/debug/app-debug.apk` (debug, unsigned for release).

## What ran exactly

| Command | Result |
|---|---|
| `gradlew assembleDebug lintDebug` | exit 0. Lint: 0 errors, 21 warnings (6 PluralsCandidate, 5 SetTextI18n, 4 GradleDependency, 2 UseKtx, 2 Overdraw, 1 AndroidGradlePluginVersion, 1 UnusedResources). 0 HardcodedText. |
| `gradlew testDebugUnitTest` | exit 0. 1 test (`ExampleUnitTest`, template 2+2). No real logic tests exist. |
| `adb install -r app-debug.apk` + `am start` | Success, cold start, no crash (`AndroidRuntime:E` empty). |
| Manual flows driven via `adb shell input` + `uiautomator dump` | See tables below. Screenshots kept in the session scratchpad, not in repo. |

## Baseline facts (requested by Phase 0)

- Storage today: SharedPreferences file `tracker` (`TrackerStore.kt`). Medicines as a JSON array string, taken doses and completed days as string sets (`yyyy-MM-dd|medicineId`), points and best streak as ints, PIN as salt + SHA-256 hex, lockout as `pin_locked_until` epoch millis. No Room, no SQLite.
- minSdk 28, targetSdk 37, compileSdk 37. AGP 9.3.3 with built-in Kotlin, Gradle 9.5.0.
- AppCompat: present, `androidx.appcompat:appcompat:1.6.1`. That is 1.6+, so `AppCompatDelegate.setApplicationLocales` is available without adding anything.
- Material 1.10.0, RecyclerView 1.4.0, ConstraintLayout 2.1.4 (unused), core-ktx, activity-ktx. No Room, no KSP.
- Hardcoded user-facing strings: 0 words. Lint HardcodedText = 0. Kotlin has 9 emoji literals used as UI text: `MainActivity.kt` 🏆 plus a 6-emoji cheer list, `MedicineAdapter.kt` ✅ and 💊. Layouts only have `tools:text` (design preview, not shipped).
- Translations: none. Only `res/values/strings.xml` (English). No `values-ar`. No `locales_config.xml`.
- Merged manifest permissions: POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, VIBRATE (+ the AndroidX dynamic receiver permission). No INTERNET. `android:allowBackup="true"`.

## Verified on emulator (API 34)

| Feature | Observation |
|---|---|
| Launch | Cold start OK, no crash. |
| Notification permission prompt (Android 13+) | Shown on first launch, Allow granted it (`POST_NOTIFICATIONS: granted=true`). |
| PIN create on first launch | Dialog cannot be dismissed with Back. "12" gives "Use at least 4 digits". 1234 vs 1235 gives "PINs don't match". 1234/1234 saves. |
| PIN storage | Prefs hold only `pin_salt` and `pin_hash` (64 hex chars). No plain PIN. |
| Empty state | Teddy emoji, "No medicines yet" text, "Add medicine" FAB at bottom end. |
| PIN gate on Add | Wrong PIN 9999 gives "Wrong PIN, try again". Correct PIN opens Add dialog. |
| Add validation | Empty name gives "Please enter a name". No time gives "Please pick a time". |
| Add medicine | Saved, row shows name, "1 pill · 7:00 PM", snackbar "Reminder set for Panadol at 7:00 PM". |
| Overdue display | Past-time untaken row shows red time and "Time to take it!". |
| Confirm pill with PIN | "Grown-up check" dialog names the medicine. After PIN: celebration overlay. |
| Points math (1 medicine, late) | +10 pill, +20 perfect day = +30. Header 30 points, streak 1, best 1, level 1, "70 points to level 2". Matches code rules. |
| Points math (on time) | VitaminD confirmed 5 min after schedule: +10 +5 = +15, total 45. |
| Celebration animation | Confetti, trophy emoji, title, count-up, details, streak line, "Yay!" closes it. Only one style exists (no age groups). |
| Persistence after process kill | `am kill` + `am force-stop` + cold start: points 30, streak 1, row "Done!", no PIN prompt again. |
| Rotation | Landscape keeps state (30 points). See layout issue below. |
| Settings via PIN | Opens. Shows PIN toggle, Change PIN, re-reminder chips (Off/5/10/15/30), notification status, exact alarm status + "Allow exact alarms" button, test reminder, reset. |
| PIN lockout | 4 wrong: "Wrong PIN". 5th wrong: "Too many tries. Wait 60 seconds.", `pin_locked_until` stored. After force-stop + relaunch, correct PIN still refused ("Wait 44 seconds"). |
| Test reminder | Notification posted about 12 s after tap (polled every 3 s), channel `pill_reminders`, importance high, 1 action. |
| Delete medicine | PIN gate, "Delete Augmentin?" confirm, row removed, its REMIND alarm removed from `dumpsys alarm`. |
| Exact-alarm permission change reschedule | After granting via appops, existing alarm became `window=0 exactAllowReason=permission`. |
| Reboot reschedule | `adb reboot`: BootReceiver started at boot, both REMIND alarms re-armed for next day. |
| Real reminder fires on time | VitaminD at 23:30: alarm exact, notification present at 23:30:04 ("Time for VitaminD!"). Next day 23:30 re-booked. |
| Re-reminder | Interval 5 min: "Don't forget VitaminD!" at 23:35:00, next repeat booked 23:40. |
| Notification tap deep link | Tapping the re-reminder opened the confirm dialog for VitaminD specifically. |
| Repeats stop after confirm | After confirm: VitaminD notification gone, 0 REPEAT alarms left. |

## Problems found (observed, not fixed)

1. **Force-stop silently kills all reminders.** After `am force-stop` (also what some OEMs do on swipe-away), `dumpsys alarm` showed 0 alarms for the app, the 23:15 Augmentin reminder never fired, and opening the app does not re-arm alarms. Only reboot, time change or exact-alarm permission change re-arm them. Phase 2 must reschedule on app open.
2. **Exact alarms are denied by default on API 34.** Fresh install: `SCHEDULE_EXACT_ALARM: granted=false`, alarms scheduled inexact with `window=+1h`. Nothing on the home screen tells the parent. Settings text says "may be a few minutes late" but the real window is up to 1 hour.
3. **After reboot, alarms are deferred until the app is opened.** Exact grant made with `adb shell appops set` was back to `default` after reboot (unclear if a real Settings toggle grant behaves the same, retest in Phase 2). With inexact alarms and the app in standby bucket 50 (never opened since boot), the alarms showed `app_standby=+364d`. Opening the app moved it to bucket 10 and the delay cleared.
4. **Perfect day stays "complete" after adding a medicine.** Day was marked complete with 1 medicine. After adding two more (one untaken), header shows "Perfect day! All pills taken." next to "2 of 3 pills taken today".
5. **Missed reminders are invisible.** After reboot the missed 23:15 dose was just rebooked for tomorrow. No MISSED state exists.
6. **Landscape layout.** Header takes most of the height, the list gets one row, the FAB covers the row's "Done!" label and delete button.
7. **PIN dialogs stack.** Repeated taps can open several PIN dialogs on top of each other (seen while driving the UI). Not a security issue, untidy.
8. **First-launch order.** The system notification prompt and the PIN creation dialog appear at the same time (PIN dialog behind). CLAUDE.md wants PIN (and later language picker) first.

## Not verified (exists in code, not tested on device)

- Change PIN flow.
- "Ask for the PIN when a pill is taken" toggle OFF path (self-confirm dialog).
- Reset points and streaks.
- "Turn on notifications" button path (permission was granted).
- "Allow exact alarms" button opening the system screen (grant was done via adb appops instead).
- Time change and timezone change reschedule.
- Multi-day streak growth, milestone bonuses, level up past 100 points.
- Dark mode.
- Behavior across midnight.
- Large font scale.

## Not built yet (gap vs CLAUDE.md contract)

- Room database, schema export, transactions, unique indexes (today: SharedPreferences).
- Patients (multiple per phone), age, age groups, per-patient points and streaks.
- Multiple times per medicine (today: one time per medicine), active flag, edit medicine.
- DoseEvent with PENDING/TAKEN/MISSED, `ensureDosesFor(date)`, MISSED after 23:59.
- Three age-based reward animations, streak-broken animation.
- Egyptian Arabic strings, RTL verification, language picker, `locales_config.xml`.
- PIN reset via device credential.
- Battery optimization hint in settings.
- Reschedule on app update (`MY_PACKAGE_REPLACED`) and on app open.
- Injected `Clock` (code calls `LocalDate.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()` directly).
- Any real unit or instrumented tests.

## Parking lot (adjacent issues, not fixed)

- `android:allowBackup="true"` in manifest. CLAUDE.md 5b wants `false`.
- `app/claude.md` lives inside `app/` with a lowercase name, so Claude Code does not auto-load it. Consider moving to repo root as `CLAUDE.md` (outside `app/`, needs user OK).
- README.md (root) is outside `app/`. Its title uses an en dash (not an em dash, so not a rule violation).
- ConstraintLayout dependency is unused.
- 5 `SetTextI18n` lint warnings (number formatting not locale-aware). Relevant for Arabic.
- Settings exact-alarm warning text understates the delay (says "a few minutes", real window up to 1 hour).

## Open risks

- Reminder reliability on real OEM phones (Xiaomi/Samsung/Oppo battery managers) is untested. Emulator does not reproduce them.
- Whether a parent's exact-alarm grant through the real Settings toggle survives reboot is unknown (problem 3).
- Known limit: changing the phone clock can bypass the PIN lockout and cheat streaks (documented, not to be over-engineered).
