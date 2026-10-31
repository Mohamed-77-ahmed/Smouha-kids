# Assets report: Koko Dawa (batch 6, item 0)

The doctor's originals stay untouched in the project root. Exact copies (same SHA-256) are in `design/brand/` with ASCII names. Everything the app uses is made from those copies by `scripts/make-brand-assets.ps1` (crop, scale and place only; nothing drawn or redrawn, no background keyed) or, for the video, by ffmpeg (re-encode only). Optional files `koko-transparent.png`, `avatar-celebration.png`, `avatar-no-streak.png` were not provided.

## Images

Every original is a JPEG: 24-bit, no alpha channel. Checkerboard check: the outer 8 percent of each picture was measured (two gray levels in near equal shares and regular steps along the border rows), then every picture was looked at.

| Original (root) | Copy in design/brand | Size | Border measurement | Looked at | Decision |
|---|---|---|---|---|---|
| koko.jpeg | koko.jpg | 1024x559 | sky blue, no steps | the rooster on a platform with medicine props, title "KOKO DAWA / كوكو دوا" and a small chick above it, soft sky blue background lighter towards the bottom | **Used** for the launcher icon, the notification large icon and the splash icon: the text-free square under the title (x 353..673, y 238..558) |
| logo no background.jpeg | rooster-nobg.jpg | 1407x768 | light checkerboard (gray steps in 38 percent of border samples) | the same rooster on a baked-in light gray checkerboard | **Not used** (checkerboard; koko.jpg gives the same rooster) |
| avatar of celebration.jpeg | avatar-celebration.jpg | 1024x559 | warm light colors, no steps | a blue and green fluffy flying creature with wings, a green cape and an "H" shield, on a blurred bedroom background (not a flat color) | **Used** for the CHILD and TEEN reward: the creature's square (x 250..740, y 20..510) as a rounded picture with its own background (no transparency is faked, the background cannot be keyed) |
| avatar no streak.jpeg | avatar-no-streak.jpg | 1407x768 | dark checkerboard (gray steps in 41 percent) | a sad, crying rooster with a cracked platform, a stopped clock and broken hearts, on a baked-in DARK checkerboard right up to the outline | **BLOCKED** (only exists with a checkerboard; never keyed). The streak-ended screens keep their current visuals |
| hospital logo.jpeg | hospital-logo.jpg | 1024x1024 | pure white (255) | the hospital logo: two figures in a stethoscope circle, red and dark blue rings, and the arc text "مستشفى سموحة الجامعي لطب وجراحة الأطفال" | **Used** on a white rounded tile (onboarding, home header, About): the logo's square (x 132..892, y 110..870) |
| hospital logo no background.jpeg | hospital-logo-nobg.jpg | 1024x1024 | light checkerboard (29 percent) | the same logo on a baked-in checkerboard | **Not used** (checkerboard; the white version is used, as requested) |

Made for the app (`app/src/main/res/drawable-nodpi/`):

| File | From | Size | Notes |
|---|---|---|---|
| `ic_launcher_koko.png` | koko.jpg | 432x432 | Adaptive icon foreground (108dp at xxxhdpi). The rooster and its props scaled into the 66dp safe zone; the crop's outer 12 px fade into transparency over the background layer, a vertical gradient of the crop's own top and bottom rows (#C3E4F0 to #E5F8FF). First try with one flat color showed a faint square edge, the gradient removed it (`docs/evidence/batch6/item0/icon_masks_preview.png`). First crop started 6 px under the title and picked up faint marks of its lowest pixels; the crop now starts at y 238 |
| `notification_large_koko.png` | koko.jpg | 256x256 | the same text-free square |
| `hospital_logo.png` | hospital-logo.jpg | 512x512 | white background, shown on a white rounded tile |
| `avatar_celebration.jpg` | avatar-celebration.jpg | 512x512 | JPEG quality 90, shown as a rounded picture |

Content descriptions (English / Arabic):

| Picture | English | Arabic |
|---|---|---|
| Hospital logo | Smouha University Children's Hospital logo | شعار مستشفى سموحة الجامعي للأطفال |
| Celebration avatar | A happy flying friend celebrating | صاحب طاير فرحان بيحتفل |
| Course complete video | Celebration video: a happy child in a magic garden | فيديو احتفال: طفل فرحان في جنينة سحرية |
| Launcher icon, notification icon | (none: Android names the app) | |

Note: the arc text inside the hospital logo reads "مستشفى سموحة الجامعي لطب وجراحة الأطفال", slightly longer than the name the app writes under it ("مستشفى سموحة الجامعي للأطفال", kept exactly as requested in R4).

## Sound: dose-sound.mpeg

`ffprobe`: MPEG audio layer 3 (MP3), 1 channel, 44.1 kHz, 177 kb/s, 2.769 s, 61,240 bytes. Android's platform MediaPlayer plays MP3, so it is not converted: copied unchanged to `app/src/main/res/raw/dose_sound.mp3`. With the duration rule of item 4 the reward stays max(3 s, min(2.77 s, 6 s)) = 3 s.

## Video: avatar of finish.mp4 (batch 7, the app's video now)

The user replaced "avatar of end.mp4" with "avatar of finish.mp4" on 2026-10-19 (both originals stay untouched in the project root). Exact copy: `design/brand/avatar-finish.mp4` (SHA-256 `CA25F54C63C3D1AFF0E301520F8BF5E8696D0AEC550ABDA17977C4C7FE7C0620`).

`ffprobe` of the original: MP4, H.264 High profile, 480x854 (portrait, 9:16 rounded to an even height), 30 fps, 182 frames, 6.07 s, yuv420p; audio AAC-LC, 2 channels, 44.1 kHz; 1,217,599 bytes.

Frames every 0.5 s (12 frames) in `docs/evidence/batch7/video-frames/` (`frame_01.png` = 0 s ... `frame_12.png` = 5.5 s, and `contact_sheet.png`), all looked at:

- The same toddler and night-time magic garden as the first video (glowing mushrooms, floating islands, rune stones, flowers).
- 0 to 2 s: the child laughs with fire ribbons on both sides and a ribbon of water around them.
- 2.5 to 4.5 s: the child is lifted inside a whirl of water and fire with falling leaves.
- 5 to 5.97 s: the child lands, kneels on the grass and smiles at the camera.
- The last 2 frames (6.00 s and 6.03 s) are pure black (luma 16 in every pixel, measured with ffmpeg `signalstats`; `last_frame.png`). The app holds the video's last frame, so these 2 frames are trimmed (`-t 5.995`): the held frame is the smiling child (`last_frame_app.png`). Nothing else is changed.
- Edge colors: a starry navy sky at the top, dark grass at the bottom: the navy card (`course_card` #1E2A44) still matches.

Re-encoded for the app with ffmpeg (`-t 5.995 -c:v libx264 -profile:v baseline -level 3.1 -pix_fmt yuv420p -crf 23 -preset slow -movflags +faststart -c:a aac -b:a 96k -ac 2 -ar 44100`): H.264 Constrained Baseline, 480x854 (not scaled up), 30 fps, 180 frames (lowest frame luma average 92.9, none black), AAC-LC stereo 44.1 kHz, 6.00 s, 1,537,158 bytes in `app/src/main/res/raw/avatar_end.mp4` (the resource name stays, so the code does not change; `CourseEndLayout.VIDEO_ASPECT` is now 480/854). On the test AVD (`docs/evidence/batch7/video/`): start, middle, held end, landscape and after closing in English and Arabic light and English dark, looked at; `CourseEndUiTest` 6 of 6.

## Video: avatar of end.mp4 (batch 6, replaced in batch 7, no longer in the app)
`ffprobe` of the original: MP4, H.264 High profile, 576x1024 (portrait 9:16), 30 fps, 302 frames, 10.07 s, yuv420p; audio AAC HE-AACv2, 2 channels, 44.1 kHz; 994,396 bytes.

Frames every 0.5 s (20 frames) in `docs/evidence/batch6/video-frames/` (`frame_01.png` = 0 s ... `frame_20.png` = 9.5 s, and `contact_sheet.png`), all looked at:

- A toddler (not the rooster) in a night-time magic garden with glowing mushrooms, floating islands and flowers.
- 0 to 1.5 s: the child laughs and spins inside a ribbon of water.
- 2 to 3 s: close-up, glowing stones around the child.
- 3.5 to 5 s: the child laughs with fire ribbons and water ribbons on both sides.
- 5.5 to 7.5 s: the child is lifted inside a whirl of water and fire with leaves.
- 8 to 10 s: the child lands, kneels on the grass and smiles at the camera (the last frame).
- The character stays in the middle third; the picture fills the whole frame (no flat border).
- Edge colors are dark, muted blue gray and navy (top #121E32 to #5C5B6A, sides #393941 to #6B6664, bottom dark green gray #172D30 to #4D5541). The card behind the video and its text is therefore a deep navy (#1E2A44 in item 6) with white text.

Re-encoded for the app with ffmpeg (`-c:v libx264 -profile:v baseline -level 3.1 -pix_fmt yuv420p -crf 23 -preset slow -movflags +faststart -c:a aac -b:a 96k -ac 2 -ar 44100`): H.264 Constrained Baseline, 576x1024 (already under 720p), 30 fps, AAC-LC stereo 44.1 kHz, 10.08 s, 3,372,639 bytes (under the 4 MB target). It was `app/src/main/res/raw/avatar_end.mp4` until batch 7. The original and its copy `design/brand/avatar-end.mp4` are kept.

## What the user must provide

- **avatar no streak:** a PNG with a real alpha channel (transparent background), or the same picture on one flat solid color. Then item 5 can use it. Until then the streak-ended screens keep the current mascot (CHILD) and badge (TEEN).
- Optional: `koko-transparent.png` and `avatar-celebration.png` with real alpha would let the rooster and the creature stand on the app's own background instead of their picture backgrounds.
