# Screenshots needed from a real phone (install guide, Koko Dawa 1.1.0)

The install guide (`2_طريقة_التثبيت.pdf`) shows the screens parents see while downloading and installing the app. Those screens belong to Android, Chrome, Google Drive, Play Protect and Samsung, and they look different on every phone, so they must come from a **real phone**. They are never drawn, mocked or faked, and they cannot be taken on the emulator (Play Protect, Drive and Samsung screens are not there).

**Take them (again) for 1.1.0.** The app is now called Koko Dawa (كوكو دوا), its file is `Koko-Dawa-1.1.0.apk` and its icon is the Koko rooster. Any picture taken for 1.0.0 shows the old file name, the old name "سموحة للأطفال" and the old capsule icon, so it is stale and must not be used. Right now `design/guide-shots/` does not exist, so the guide shows a dashed placeholder box for every screen until the new files are there.

When the files are in `design/guide-shots/`, build the guides again:

```powershell
powershell -File scripts\make-docs.ps1
```

`make-docs.ps1` puts each file in its place, renders every page to PNG for checking, and copies the PDFs into `dist/`.

## How to take them

- Use the Samsung (or another Android phone) with the **phone language set to Arabic**, because the guide is in Arabic. If one screen only exists in English on that phone, that is fine.
- Install from the real Drive link, the way parents will (`dist/DRIVE.md`). First uninstall any copy of the app on that phone (Settings > Apps > Koko Dawa, or Smouha Kids on an older copy, > Uninstall; test data only), so every screen appears.
- Screenshot on Samsung: Power + Volume down. Portrait only. PNG or JPG.
- Before saving, check every picture: no real names, no phone numbers, no other people's chats or notifications. Crop the WhatsApp header and any notification that is not ours. Use the fictional demo data only.
- Save them with exactly these names (ASCII) in `design/guide-shots/`:

| File | What the screen must show |
|---|---|
| `01_link_whatsapp.png` | The WhatsApp message with the Drive link (the text of `docs/whatsapp-message-ar.txt`, which now names «كوكو دوا»), before the link is tapped. Chat name cropped. |
| `02_drive_page.png` | The Google Drive page that opens from the link: the file name `Koko-Dawa-1.1.0.apk` and the download button (arrow or "تنزيل"). If the direct link starts the download at once and no Drive page appears, take the folder link's page instead. |
| `03_download_warning.png` | The browser warning that this type of file can harm the device, with "تنزيل على أي حال" / "Download anyway" visible. |
| `04_download_open.png` | The finished download of `Koko-Dawa-1.1.0.apk` (Chrome's bar or the notification) with "فتح" / "Open". |
| `05_unknown_sources.png` | Android's message that the phone is not allowed to install apps from this source, with the "الإعدادات" / "Settings" button. |
| `06_allow_source.png` | The "Install unknown apps" page of the browser (or "My Files"), with "السماح من هذا المصدر" / "Allow from this source" turned ON. |
| `07_install.png` | The question "Do you want to install this app?" with the rooster icon, the name «كوكو دوا» and "تثبيت" / "Install". |
| `08_play_protect.png` | The Google Play Protect warning (for example "Unsafe app blocked" or "Unknown app"), with "مزيد من التفاصيل" / "More details". If Play Protect does not appear on that phone, write that down instead of making one up. |
| `09_play_protect_anyway.png` | The same warning opened up, with "التثبيت على أي حال" / "Install anyway". |
| `10_auto_blocker.png` | Samsung only, with Auto Blocker on: the message that Auto Blocker stopped the install, or the page Settings > Security and privacy > Auto Blocker with its switch. Skip on phones without Auto Blocker. |
| `11_installed_open.png` | "تم تثبيت التطبيق" / "App installed" with the rooster icon, «كوكو دوا» and "فتح" / "Open". |

Optional, also from the real phone:

| File | What it shows |
|---|---|
| `12_home_icon.png` | The phone's home screen or app list with the rooster icon and the name «كوكو دوا». |
| `13_update_over_1_0_0.png` | Only on a phone that still has 1.0.0 (Smouha Kids) with test data: the install question when 1.1.0 is installed over it ("Do you want to update this app?"). |
| `install-video.mp4` | A screen recording of 60 to 90 seconds of the whole install from the Drive link (Samsung: swipe down twice > Screen recorder). `make-docs.ps1` copies it to the parents folder as `4_فيديو_التثبيت.mp4`. Same rules: no real names, no other notifications. |

After adding the files, run `scripts\make-docs.ps1` and look at the new pages in `docs/evidence/batch7/item2/pages/` before replacing anything on Drive.
