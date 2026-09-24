# BUILD FIX 03

## علت خطای آخر
در Build قبلی، Viewer پس از Build شدن Admin بدون clean مستقل ساخته می‌شد و امکان باقی‌ماندن assetهای merge شده/کش Gradle وجود داشت. نتیجه این بود که `001_sample.json` وارد APK Viewer می‌شد، با اینکه منبع Viewer فقط `001_sample.taz` بود.

## اصلاح
- حذف hook قبلی حذف JSON از مسیر `merged_assets` که به مسیر واقعی همه نسخه‌های AGP وابسته بود.
- clean مستقل قبل از Build Viewer.
- اجرای Build هر flavor با `--no-build-cache` در CI.
- حفظ sourceSetهای مستقل:
  - Admin: `src/admin/assets`
  - Viewer: `src/viewer/assets`
- بررسی نهایی APK همچنان هر JSON خام داخل `assets/` را خطا می‌گیرد و نام فایل را چاپ می‌کند.

این اصلاح عمداً JSON را بعد از ساخت APK دستکاری نمی‌کند تا امضای APK آسیب نبیند.
