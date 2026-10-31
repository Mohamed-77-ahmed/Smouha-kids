# خريطة الشرايح (slide map)

Each slide: its title, the screenshots that belong on it (`screenshots/`, all 1080x2400 PNG unless noted, Arabic and light mode unless noted, fictional demo names), the composite that fits it (`screenshots/composites/`), and the source file every statement on it must come from. Compare every generated slide with this table.

| # | Title (Arabic) | Screenshots | Composite | Source |
|---|---|---|---|---|
| 1 | كوكو دوا: تذكير بمواعيد دوا الأطفال، وتشجيع ليهم | none (the app icon may be placed from the app, no other art) | none | 01 |
| 2 | المشكلة: المواعيد بتتنسي | none | none | 01 |
| 3 | الحل في جملة | none | none | 01 |
| 4 | الأسرة بتبدأ إزاي | `04_01_language_picker`, `04_02_pin_sheet`, `04_03_add_child`, `04_04_add_medicine_days_times` | `c04_family_starts` | 02 |
| 5 | تذكير بيرن حتى لو الموبايل صامت | `05_01_notification_shade`, `05_02_confirm_screen` | `c05_reminder_to_confirm` | 02, 03 |
| 6 | تحكم الأهل: الرقم السري ومواعيد لكل يوم | `06_01_pin_gate`, `06_02_weekday_schedule` | none | 03 |
| 7 | تشجيع الأطفال | `07_01_reward_child`, `07_02_reward_teen`, `07_03_reward_adult`, `07_04_streak_broken_child`, `07_05_course_end` | `c07_rewards_by_age`, `c07_streak_and_course_end` | 03, 02 |
| 8 | أكتر من طفل على موبايل واحد | `08_01_home_several_children` | none | 03 |
| 9 | عربي الأول، وسهل على أي شاشة | `09_01_home_small_phone` (480x800), `09_01_home_tablet` (1600x2560), `09_02_home_english` | `c09_any_screen` | 03 |
| 10 | الخصوصية والأمان | `10_01_about`, `10_02_settings_sound_rows` | `c10_privacy_and_settings` | 04 |
| 11 | التثبيت والتحديث | none (the real install screens come from a real phone; until then no picture) | none | 05 |
| 12 | اللي اتجرب، والحدود بأمانة | none | none | 06 |
| 13 | التجربة المقترحة والقرارات المطلوبة | none | none | 07 |
| 14 | شكرًا، والتواصل `<TO BE FILLED>` | none | none | 05, 07 |

The video `video/flow.mp4` (about 40 seconds, no sound) fits slide 5: the reminder in the notification shade, the tap, the PIN, the reward. It has no audio because the emulator's screen recording cannot capture the alarm sound; say so if it is shown.

Things that must never appear on a slide: improvement or adherence numbers, "the hospital approved / recommends", "the treatment is finished", a real phone test of version 1.1.0 (there was none), real names.
