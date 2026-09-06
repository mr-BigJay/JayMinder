# یادداشت آفلاین — Offline Journal

اپلیکیشن اندروید بومی برای یادداشت روزانه، ضبط صدا، تبدیل گفتار به متن (آفلاین) و یادآوری — با رابط کاربری فارسی و تقویم شمسی.

## ویژگی‌ها

- **کاملاً آفلاین** — بدون نیاز به اینترنت، حساب کاربری یا سرویس ابری
- **یادداشت صوتی و متنی** — ضبط صدا، ذخیره فایل صوتی و تبدیل به متن
- **تشخیص گفتار آفلاین** — با موتور Vosk و مدل فارسی
- **تقویم شمسی (جلالی)** — تمام تاریخ‌ها با منطقه زمانی `Asia/Tehran`
- **دسته‌بندی** — ایجاد، ویرایش، تغییر نام و حذف دسته‌ها
- **فیلتر و جستجو** — بر اساس روز، هفته، ماه، سال و دسته‌بندی
- **یادآوری** — اعلان‌های دقیق با `AlarmManager`
- **رابط RTL فارسی** — فونت Vazirmatn محلی

## معماری

```
presentation/   → Jetpack Compose UI + ViewModels (MVVM)
domain/         → مدل‌ها و قراردادها
data/           → Room Database + Repositories
service/        → صدا، تشخیص گفتار، یادآوری
util/           → تقویم جلالی، فرمت فارسی
```

موتور تشخیص گفتار از طریق interface `SpeechToTextEngine` پیاده‌سازی شده تا در نسخه‌های بعدی قابلیت‌های آنلاین/AI بدون بازنویسی هسته اضافه شوند.

## پیش‌نیازها

- Android Studio Hedgehog یا جدیدتر
- JDK 17
- Android SDK 34
- دستگاه یا شبیه‌ساز اندروید 8.0+ (API 26)

## نصب مدل Vosk فارسی

قبل از build، مدل تشخیص گفتار را نصب کنید:

1. دانلود: [vosk-model-small-fa-0.5](https://alphacephei.com/vosk/models/vosk-model-small-fa-0.5.zip)
2. استخراج در: `app/src/main/assets/model/vosk-model-small-fa-0.5/`

جزئیات بیشتر: `app/src/main/assets/model/README.md`

> بدون مدل، ضبط صدا و یادداشت متنی کار می‌کند؛ فقط «تبدیل به متن» غیرفعال است.

## Build و Run

```bash
./gradlew assembleDebug
```

یا از Android Studio: **Run** روی دستگاه/شبیه‌ساز.

## دانلود APK

فایل آماده نصب:

```
releases/OfflineJournal-v1.0.0.apk
```

> این APK unsigned است. برای نصب روی گوشی، «نصب از منابع ناشناس» را فعال کنید.

## مجوزها

| مجوز | کاربرد |
|------|--------|
| `RECORD_AUDIO` | ضبط یادداشت صوتی |
| `POST_NOTIFICATIONS` | اعلان یادآوری (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | یادآوری در زمان دقیق |

## ساختار داده

- **یادداشت‌ها** — متن، ضبط صوتی، رونوشت، دسته، تاریخ ایجاد
- **دسته‌بندی‌ها** — نام و رنگ
- **یادآوری‌ها** — عنوان، توضیحات، زمان (شمسی/تهران)

همه داده‌ها در Room SQLite و فایل‌های صوتی در `files/recordings/` ذخیره می‌شوند.

## فناوری‌ها

- Kotlin · Jetpack Compose · Material 3
- Room · Coroutines · Flow / StateFlow
- Vosk (آفلاین STT فارسی)
- AlarmManager · NotificationCompat

## مجوز نرم‌افزار

Apache 2.0 (کتابخانه Vosk)
