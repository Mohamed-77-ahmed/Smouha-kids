# Release: signing, checking and sharing Koko Dawa

(Called Smouha Kids up to 1.0.0; renamed Koko Dawa / كوكو دوا in 1.1.0, batch 6. The package and the key did not change.)

The app is shared as a signed APK through Google Drive. There is no Play Store, so the signing key is the only thing that lets a newer APK install over an older one. **If the key is lost, installed copies can never be updated** (families would have to uninstall and lose all their data). Keep it safe and backed up.

## 1. The release key (created once, by a person)

The real key exists since 2026-10-18. It was created by the user, outside the project:

- File: `C:\Users\OPT\smouhakids-signing\smouhakids-release.jks` (PKCS12), alias `smouhakids`, RSA 4096, valid 10000 days.
- Certificate: `CN=Smouha Kids, O=Smouha Kids, C=EG` (no hospital name inside: the name has no official approval, and anyone can read a certificate). It keeps the old app name for good: a certificate cannot change without a new key, and a new key could never update the installed app. The name a phone shows comes from the app, not from the certificate.
- One password for the store and the key (PKCS12 uses the same one).

The command that made it, for reference only. **Never run it again for this app**: an APK signed with a second key cannot update the installed app.

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "C:\Users\OPT\smouhakids-signing\smouhakids-release.jks" -alias smouhakids -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Smouha Kids, O=Smouha Kids, C=EG"
```

Backups: the `.jks` file in two places that are not this laptop (for example a USB stick kept away from the laptop and the password manager's file storage), the password and the alias in a password manager. Never put the `.jks` file or the password in the project, in git, in Google Drive next to the APK or in a message.

The automated runs never create, copy or print the key or its password: `scripts/build-release.ps1` lets Gradle read `keystore.properties`, the snapshots leave out `keystore.properties`, `*.jks` and `*.keystore` (`docs/evidence/tools/snapshot.ps1`), and `.gitignore` ignores all three.

## 2. Point the build at the key: `keystore.properties`

`keystore.properties` in the project root (next to `settings.gradle.kts`), ignored by git, forward slashes in the path, the same password on both password lines:

```properties
storeFile=C:/Users/OPT/smouhakids-signing/smouhakids-release.jks
storePassword=<the password>
keyAlias=smouhakids
keyPassword=<the same password>
```

Without this file a release build stops with:

```text
Release signing is not set up: ...\keystore.properties was not found.
Create the keystore and keystore.properties as described in docs/RELEASE.md.
```

## 3. Release certificate (recorded once, never changes)

Release certificate SHA-256: `FF:0A:9A:1B:D4:09:D3:85:03:76:FF:A2:C8:3D:0F:27:E2:39:4D:7B:2A:B7:3E:D6:9E:16:14:3E:FF:8A:E8:7A`

Every APK of this app, now and in every future update, must show exactly this fingerprint. `scripts/verify-dist.ps1` compares it and fails on any other one. It is the same value `keytool -list -v` shows for the key (line `SHA256:`), so it can be compared with the copy in the password manager. The hospital gets it in `SHA256.txt` and in the handoff document.

## 4. Versions

- **1.0.0 (versionCode 1)** is the first release signed with this key (2026-10-18).
- **1.1.0 (versionCode 4)**: Koko Dawa (batch 6). versionCode 4 is above every build so far: the release 1.0.0 (1), the update test's 1.0.1 (2) and the debug pilot 1.2.0 (3).
- The earlier test builds (1.0.0 to 1.2.0, versionCode 1 to 3, the last one used for the phone pilot) were signed with the Android debug key. They can never be updated into a release: on a phone that has one, uninstall it by hand first (Settings > Apps > Smouha Kids, the name those builds show > Uninstall). That deletes its test data.
- For every next release raise `versionCode` by 1 and set `versionName` (for example 1.0.1) in `app/build.gradle.kts`. Android refuses a lower versionCode, and an equal one would make two different builds look the same in parent settings.
- If the Room schema changed, raise the database version and add a `Migration` with a migration test (see `SchemaTest`). Never add `fallbackToDestructiveMigration()`: it would silently wipe every child's points and streaks on update.

## 5. Build and check

```powershell
$env:ANDROID_SERIAL = "emulator-5560"   # the test emulator Pixel_API_34_test, started with -port 5560
powershell -File docs\evidence\tools\suite.ps1 -Report <file.txt>   # unit tests, lint, all instrumented tests
powershell -File scripts\build-release.ps1
```

`scripts/build-release.ps1` runs `assembleRelease` with the real key, copies the APK to `dist\Koko-Dawa-App\للأهل\1_التطبيق\Koko-Dawa-<versionName>.apk` (ASCII file name) and runs `scripts/verify-dist.ps1`, which writes `verify-dist.txt` and `SHA256.txt` into `dist\Koko-Dawa-App\للمستشفى\`. It warns when an older APK is still in the parents folder (remove it, so parents see only one file).

`scripts/verify-dist.ps1 -Apk <file>` checks, and exits with 1 when anything fails (never share such an APK):

- `apksigner verify`: signed (scheme v2 or v3), exactly one signer, not the Android debug certificate, the fingerprint of section 3.
- `aapt dump badging` and the resource table: package `com.smouha.kidsreminder`, versionCode, versionName, minSdk 28, the app name and the launcher name in English and Arabic.
- `aapt dump permissions`: no `android.permission.INTERNET`. Expected: POST_NOTIFICATIONS, USE_EXACT_ALARM, SCHEDULE_EXACT_ALARM (max SDK 32), RECEIVE_BOOT_COMPLETED, VIBRATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED (the reminder's alarm sound, docs/DECISIONS.md D28), plus androidx's own `com.smouha.kidsreminder.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- The manifest: `allowBackup` false, not debuggable.
- File size and SHA-256.

The old Android tools cannot open a path with Arabic letters, so the script checks a copy in a temporary ASCII folder and compares the hashes.

Result for 1.0.0 (2026-10-18): all checks passed, 13,465,081 bytes, file SHA-256 `5656390fa42ea4239a173bcaa2d8d70fadb6f93d5af0cd634180a7dd7e64b37b` (then `dist/Smouha-Kids-App/للمستشفى/verify-dist.txt`, now in `../tracker-snapshots/dist-1.0.0/`).

Result for 1.1.0 (2026-10-19, batch 7, `Koko-Dawa-1.1.0.apk`): all checks passed, 15,427,906 bytes, file SHA-256 `f7b3aa43124b5fb4c9f8dcdcb94a19a476d823020824ada5671717d6cb073122`, the same certificate (`dist/Koko-Dawa-App/للمستشفى/verify-dist.txt`). The update test from 1.0.0 with data passed (`docs/evidence/batch7/item1/update_test.txt`). The 1.0.0 pack is kept in `../tracker-snapshots/dist-1.0.0/`.

## 6. Update test (before sharing any new version)

On the test emulator only. Install the APK that families have now (for the first release: the release itself) with data, then:

```powershell
$env:ANDROID_SERIAL = "emulator-5560"
powershell -File scripts\update-test.ps1 -NextApk <the new release APK>
# or, to rehearse with a build of the same code: scripts\update-test.ps1 [-NextVersionCode 2 -NextVersionName 1.0.1]
```

It dumps every table (kept in memory; the PIN hash is compared, never printed), installs the next version with `adb install -r`, checks that the version changed and the first install time did not, compares every row, opens the app (the patients are on home, the reminders are booked again), and finally tries the same APK re-signed with the Android debug key: Android must refuse it with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and nothing may change. Another key is never a way to update: it only installs after an uninstall, which deletes all data.

1.0.0 to 1.0.1 (versionCode 2, same key) on 2026-10-18: passed, every row identical, the other key refused (`docs/evidence/batch4/item1/`).

## 7. Main flow from the release APK

Before sharing, the release APK (not the debug one) goes through the whole flow on the test emulator: first run (language, PIN), add a child, add a medicine, the reminder fires with the app in the background, the app's alarm sound plays, a tap opens that dose, the PIN confirms it, the reward shows and the points are stored. Done for 1.0.0 on 2026-10-18 (`docs/evidence/batch4/item1/flow.txt`). The real phone checks (how loud, battery managers, reboot on a real phone) are `docs/PILOT.md`, run by a person on the signed APK.

## 8. Share

Upload the `dist/Koko-Dawa-App/` folder to Google Drive as described in `dist/DRIVE.md` (folder structure, sharing, the direct download link, a test in a private window). Parents get only the parents folder. Never share the `.jks` file, `keystore.properties`, a debug APK or an APK signed with another key.

## 9. If the key is lost

No update can ever reach the installed apps again. The only way out is a new key: every family would have to uninstall (losing all points, streaks and medicines) and install the new APK. This cannot be repaired afterwards, which is why section 1 asks for two backups.

## Note about USE_EXACT_ALARM

The app declares `USE_EXACT_ALARM`, which Android 13+ grants automatically so reminders ring on time. It is also what allows the `systemExempted` foreground service that plays the reminder's alarm sound. Google Play only allows this permission for alarm and calendar apps and restricts that service type. This is fine for Drive sharing; if the app is ever published on Google Play, re-check that policy first.
