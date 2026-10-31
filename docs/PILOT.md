# Real phone pilot (Koko Dawa 1.1.0, versionCode 4, signed release; app id com.smouha.kidsreminder)

From 1.1.0 (versionCode 4, batch 6) the app is called **Koko Dawa / كوكو دوا** on the phone, with the rooster icon. 1.0.0 and the older debug builds show "Smouha Kids" (and the oldest ones "Pill Pals"). A 1.1.0 release installs over 1.0.0 and keeps all data (same app id, same key).

What the emulator cannot prove: whether the reminder is really heard on a real phone in silent and vibrate mode, Do Not Disturb, low volume, the phone maker's battery manager, reboot and the lock screen. This pilot is run by a person with the phone in hand. Everything else was tested on the emulator (`docs/FINAL_REPORT.md`).

The developer tools never install anything on a physical phone (CLAUDE.md 0b). The person running the pilot installs the build with the steps below.

## 0a. The signed release 1.1.0 (do this one now)

The first pilot ran a debug build (1.2.0, versionCode 3, signed with the Android debug key). The releases (1.0.0, now 1.1.0) are signed with the real key; neither has been on a real phone yet. A release installs over the debug build only after an uninstall: it has another key, and 1.0.0 also a lower versionCode, so Android refuses to install it over the debug build (`INSTALL_FAILED_UPDATE_INCOMPATIBLE` or `INSTALL_FAILED_VERSION_DOWNGRADE`). This is expected and happens only on test phones; families never had the debug build.

1. On the phone: Settings > Apps > Smouha Kids (the debug build's name) > **Uninstall** (this deletes the pilot's test data; nothing of a family is on it).
2. Install `Koko-Dawa-1.1.0.apk` the way parents will: from the Google Drive link once it is uploaded (`dist/DRIVE.md`), following `2_طريقة_التثبيت.pdf`. Before the upload, from the computer:

   ```powershell
   adb -s <serial> install "dist\Koko-Dawa-App\للأهل\1_التطبيق\Koko-Dawa-1.1.0.apk"
   ```

3. Check the version (section 1 below, expected **Version 1.1.0 (4)**), then repeat at least checks 1, 2, 8, 9 and 10 with the signed release, and listen once to a reward of a child (the short celebration sound, media volume) and write them into the second results table (section 4).

From now on every next release is installed over the previous one with the same key and keeps all data (`docs/RELEASE.md`).

## 0. Install a debug build for testing (keeps all data)

**Since 1.2.0 the app has a new id (`com.smouha.kidsreminder`), so Android treats it as a different app: an older "Pill Pals" stays installed next to it with its own data and its own reminders; remove the old one by hand (Settings > Apps > Pill Pals > Uninstall) only after its data is no longer needed, because uninstalling deletes it.** A debug build never installs over the signed release (another key): use it only on a test phone without the release.

1. On the computer, in the project folder, build the debug APK:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat assembleDebug
   ```

   The APK is `app\build\outputs\apk\debug\app-debug.apk`.
2. Connect the phone with USB debugging on and find its serial number:

   ```powershell
   adb devices -l
   ```

   For example `RKGL100FEFP   device ... model:SM_A266B`. Use that serial in every command below, so nothing goes to another device.
3. Install over the existing app. `-r` replaces the app and keeps its data (patients, medicines, points, the PIN):

   ```powershell
   adb -s <serial> install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   Expected: `Success`. If it says `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (the installed app was signed on another computer) or `INSTALL_FAILED_VERSION_DOWNGRADE` (the phone already has a newer build), stop and report it. Never uninstall to get around it: uninstalling deletes all data.

## 1. First check: is the latest build installed?

Before anything else (the first real-phone test ran an old build without anyone knowing):

- In the app: Parent settings, at the very bottom: **Version 1.0.0 (1)** for the signed release (the first pilot ran Version 1.2.0 (3), a debug build).
- Or on the computer:

  ```powershell
  adb -s <serial> shell dumpsys package com.smouha.kidsreminder | findstr /C:"versionCode" /C:"versionName" /C:"lastUpdateTime"
  ```

  Expected for the signed release: `versionCode=1`, `versionName=1.0.0`, and `lastUpdateTime` = the time of the install.

If it shows anything else, stop: the results would not be about this build.

## 2. Prepare

- Parent settings: **Alarm sound for reminders** is on (the default). Note the sound rows under it (alarm volume, Do Not Disturb, battery) for the results.
- Add a test medicine for a test patient with a time 3 minutes ahead and re-reminders every 5 minutes. A second test patient with a medicine at the same minute is needed for check 13.
- **Send a test reminder** (Parent settings) arrives in about 10 seconds and is the quickest way to test the sound. It behaves like a real reminder but opens home instead of a dose.
- Note the phone's normal settings first (ringer mode, Do Not Disturb, font size, battery setting for Koko Dawa) to put them back at the end.

## 3. Checklist

For every check write down what was heard and seen in the results table (section 4). "The alarm sound" means the phone's own alarm tone, played by the app for at most 30 seconds, with a **Stop sound** button on the reminder.

| # | Check | How | Expected | Record |
|---|---|---|---|---|
| 1 | Ringer **silent** | Set the phone to Mute / Silent. Send a test reminder, lock the phone. | Within about 10 s the reminder shows and the alarm sound plays, up to 30 s. | Heard? Loud enough? Seconds it played. |
| 2 | Ringer **vibrate** | Set the phone to Vibrate. Send a test reminder. | The alarm sound plays (and the phone may vibrate). | Heard? Vibrated? |
| 3 | **Stop sound** | During check 1 or 2, tap **Stop sound** on the reminder. | Sound stops at once. The reminder stays (without the button). Re-reminders still come. | Stopped at once? Reminder still there? |
| 4 | **30 seconds** | Let a reminder ring without touching anything. | Sound stops by itself after about 30 s; the reminder stays. | Seconds until it stopped. |
| 5 | **Do Not Disturb, alarms allowed** | Turn on Do Not Disturb with alarms allowed (Samsung: Do not disturb > Allow exceptions > Alarms on). Send a test reminder. | The alarm sound plays. Parent settings say "Do Not Disturb is on and lets alarms through". | Heard? What the settings row said. |
| 6 | **Do Not Disturb, alarms not allowed** | Same, with alarms not allowed. Send a test reminder. | No sound (Android blocks alarms then; by design). Parent settings show the amber warning "blocks alarms" with a button to the Do Not Disturb settings. | Silent? Warning shown? |
| 7 | **Alarm volume 0** | Turn the alarm volume all the way down (many phones stop at 1). Send a test reminder. | At 0: no sound and the amber warning "Alarm volume is at 0" with a Sound settings button. At the lowest step above 0: a quiet sound. | Lowest step possible. Heard? Warning shown? |
| 8 | **Swiped from recents** | Open the app, swipe it away in the recent apps screen, wait for the 3-minute medicine. | Reminder and sound on time. | Minutes late (0 = on time). |
| 9 | **After reboot** | Add a medicine 5 minutes ahead, restart the phone, unlock it, wait. | Reminder and sound on time (reminders are booked again at boot). | On time? Did it wait for the unlock? |
| 10 | **Battery: Optimized vs Unrestricted** | Samsung: Settings > Apps > Koko Dawa > Battery: test once with Optimized, once with Unrestricted. Also Settings > Battery > Background usage limits: put Smouha Kids in **Sleeping apps** and test, then in **Never sleeping apps** and test (screen off for 10 minutes before each reminder). | On time with Unrestricted and in Never sleeping apps. Optimized and Sleeping apps may delay it: that is the phone, record it. Parent settings show the battery state. | Minutes late for each setting. |
| 11 | **Locked screen** | Lock the phone, wait for a reminder, then tap it. | Sound plays on the lock screen. Tapping asks for the phone unlock, then opens that dose. | Sound? Shown on the lock screen? Opened the right dose? |
| 12 | **3 re-reminders, then stop** | Do not confirm the 3-minute medicine. | Re-reminders every 5 minutes, exactly 3, each with the alarm sound up to 30 s; nothing after the third. | Times of the first reminder and of each re-reminder. |
| 13 | **Tap opens the right dose** | Two patients with a medicine at the same minute. Tap each reminder. | One sound for both (the newer one has Stop sound), two reminders. Each tap opens the sheet titled "Confirm <name>'s pill" for that patient and medicine, and stops the sound. | Right patient and medicine each time? One sound? |
| 14 | **Confirm stops it** | Confirm a ringing dose with the PIN. | Sound stops, the reminder goes, no more re-reminders for it. | Any re-reminder after confirming? |
| 15 | **Arabic and English** | Parent settings > Language: Arabic. Send a test reminder; open the PIN sheet (Change PIN). Then the same in English. | Reminder text in the chosen language. In Arabic the screens mirror; the PIN keypad stays 1-2-3 from left to right with Western digits. | Anything in the wrong language or cut? |
| 16 | **Font size 1.5** | Samsung: Settings > Display > Font size and style, about 1.5 times. Open home, parent settings, a PIN sheet. | Nothing cut off; the whole keypad is on screen. | Screens with problems (screenshot). |
| 17 | **PIN sheet, no keyboard** | Ask for the PIN everywhere: confirm a pill, add a medicine, change PIN, erase (cancel at the last step). Try a wrong PIN 5 times once. | No system keyboard ever opens; keys give a short vibration; a wrong PIN shakes and says how many tries are left; after 5 wrong tries a 60 second countdown and the keys are off. | Keyboard seen anywhere? Feel of the keys? |

At the end put the phone back to its normal settings (ringer, Do Not Disturb, alarm volume, font size, battery setting).

## 4. Results

One row per phone. Pass = the expected result happened.

### First pilot (debug build)

Filled in before batch 4 (2026-10-18): all 17 checks pass. The phone model, the Android version and the build were not written into the table (most likely the Samsung SM-A266B of the batch 2 diagnosis and the latest build at the time, the debug Version 1.2.0 (3), but that is not recorded). Left as filled in.

| Phone model | Android / One UI version | Check | Result (pass / fail) | Notes (heard, seen, minutes late) |
|---|---|---|---|---|
| | | 1 Silent |pass | |
| | | 2 Vibrate |pass | |
| | | 3 Stop sound |pass | |
| | | 4 30 seconds |pass | |
| | | 5 DND, alarms allowed |pass | |
| | | 6 DND, alarms not allowed |pass | |
| | | 7 Alarm volume 0 |pass | |
| | | 8 Swiped from recents |pass | |
| | | 9 After reboot |pass | |
| | | 10 Battery settings |pass | |
| | | 11 Locked screen |pass | |
| | | 12 3 re-reminders |pass | |
| | | 13 Right dose |pass | |
| | | 14 Confirm stops it |pass | |
| | | 15 Arabic and English |pass | |
| | | 16 Font 1.5 |pass | |
| | | 17 PIN sheet |pass | |

### Signed release 1.1.0 (section 0a)

Write the phone model, the Android version and "Version 1.1.0 (4)" from parent settings. Installed from: Drive link / adb (circle one).

| Phone model | Android / One UI version | Check | Result (pass / fail) | Notes (heard, seen, minutes late) |
|---|---|---|---|---|
| | | Version shown: 1.0.0 (1) | | |
| | | 1 Silent | | |
| | | 2 Vibrate | | |
| | | 8 Swiped from recents | | |
| | | 9 After reboot | | |
| | | 10 Battery settings | | |
| | | Other checks (optional) | | |

## 5. If something fails

Write the time it happened. These read-only commands show what the phone did without changing anything (the same ones allowed by CLAUDE.md 2):

```powershell
adb -s <serial> shell dumpsys package com.smouha.kidsreminder | findstr /C:"versionCode" /C:"lastUpdateTime"
adb -s <serial> shell dumpsys audio | findstr /C:"mode (internal)" /C:"USAGE_ALARM"
adb -s <serial> shell dumpsys alarm | findstr com.smouha.kidsreminder
adb -s <serial> shell settings get global zen_mode
```

The app also writes why it fell back to the notification's own alarm sound (for example when Android refused to start the sound service):

```powershell
adb -s <serial> logcat -d -s SmouhaKidsSound
```

Send the results table, the times, and this output. Patient names stay out of it: the commands above do not print notification text. The audio lines can name other apps that played sound at the time; delete those lines before sending if you prefer.
