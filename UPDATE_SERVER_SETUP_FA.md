# تنظیم آدرس سرور بروزرسانی Admin و User

این مرحله بروزرسانی را از GitHub جدا می‌کند. برنامه فقط وقتی کاربر از داخل «تنظیمات → بروزرسانی برنامه» بررسی را می‌زند، به آدرس(های) مخصوص همان نسخه وصل می‌شود.

## ⚠️ نکته‌ی امنیتی مهم — کجا میزبانی نکنید

فایل `update.json` و APK هرگز نباید از همان ریپازیتوری‌ای سرو شوند که سورس اصلی برنامه (Admin) در آن است. هر آدرسی که داخل APK بگذارید (چه در `BuildConfig`، چه در هر جای دیگر کد) با یک دستور ساده مثل `strings app.apk` از داخل APK قابل استخراج است — R8/ProGuard رشته‌های متنی را مبهم نمی‌کند، فقط نام کلاس‌ها را. پس اگر آن آدرس مستقیم به `raw.githubusercontent.com/USERNAME/REPO/...` روی همان ریپازیتوری سورس اشاره کند و آن ریپازیتوری Public باشد، هرکسی با پیدا کردن همین یک لینک مستقیم به کل سورس Admin دسترسی پیدا می‌کند.

**راه درست:** یک ریپازیتوری/میزبان کاملاً جدا و بی‌ربط (مثلاً `tazieh-updates` روی گیت‌هاب، یا یک پروژه‌ی جدا روی Cloudflare Pages) بسازید که فقط شامل دو فایل باشد: `update.json` و خود APK — هیچ خط کدی از برنامه در آن نباشد. ریپازیتوری سورس اصلی همچنان Private می‌ماند.

## چند آدرس (اصلی + پشتیبان)

هر Flavor می‌تواند به‌جای یک آدرس، لیستی از آدرس‌ها داشته باشد. برنامه به‌ترتیب امتحان می‌کند و اولین آدرسی که جواب معتبر داد استفاده می‌شود؛ اگر یکی از دسترس خارج شد (مثلاً قطعی یا فیلترینگ)، خودکار سراغ بعدی می‌رود.

- Admin: `UPDATE_SERVER_ADMIN_URLS`
- User/Public: `UPDATE_SERVER_VIEWER_URLS`

(کلید مفرد قدیمی `UPDATE_SERVER_ADMIN_URL` / `UPDATE_SERVER_VIEWER_URL` هم هنوز پشتیبانی می‌شود، برای سازگاری با تنظیمات قبلی — اگر فقط یک آدرس دارید نیازی به تغییر نیست.)

این مقادیر در `app/build.gradle.kts` خوانده می‌شوند و برای هر Flavor به صورت `BuildConfig.UPDATE_SERVER_URLS` (جدا‌شده با کاما در تنظیمات شما، و با `|` داخل خود APK) قرار می‌گیرند.

### روش ۱ — برای Build محلی

مقادیر را در `local.properties` قرار دهید (چند آدرس را با کاما جدا کنید):

```properties
UPDATE_SERVER_ADMIN_URLS=https://update-admin.yourdomain.com,https://backup-mirror.example.com/tazieh/admin
UPDATE_SERVER_VIEWER_URLS=https://update-viewer.yourdomain.com,https://backup-mirror.example.com/tazieh/user
```

### روش ۲ — هنگام GitHub Actions یا هر CI دیگر

بدون تغییر سورس می‌توانید پارامتر Gradle بدهید:

```text
-PUPDATE_SERVER_ADMIN_URLS=https://update-admin.yourdomain.com,https://backup-mirror.example.com/tazieh/admin
-PUPDATE_SERVER_VIEWER_URLS=https://update-viewer.yourdomain.com,https://backup-mirror.example.com/tazieh/user
```

پس از Build، آدرس‌های انتخاب‌شده داخل APK همان Flavor قرار می‌گیرند.

## فایل سمت سرور

هر آدرس باید یک `update.json` داشته باشد. نمونه برای Admin:

```json
{
  "packageName": "com.example.bookapp",
  "access": "admin",
  "versionCode": 100001,
  "versionName": "1.0.1",
  "apkUrl": "https://YOUR-DOMAIN/tazieh/admin/TaziehStudio-Admin.apk",
  "sha256": "SHA256_64_HEX_OF_TaziehStudio-Admin.apk",
  "forceUpdate": false,
  "minSupportedVersion": 0,
  "releaseDate": "2026-09-19",
  "releaseNotes": ["تغییرات نسخه جدید"]
}
```

نمونه User/Public:

```json
{
  "packageName": "com.example.bookapp.viewer",
  "access": "viewer",
  "versionCode": 100001,
  "versionName": "1.0.1",
  "apkUrl": "https://YOUR-DOMAIN/tazieh/user/TaziehStudio-User.apk",
  "sha256": "SHA256_64_HEX_OF_TaziehStudio-User.apk",
  "forceUpdate": false,
  "minSupportedVersion": 0,
  "releaseDate": "2026-09-19",
  "releaseNotes": ["تغییرات نسخه جدید"]
}
```

برنامه `packageName` و Flavor را کنترل می‌کند تا APK اشتباه برای Admin/User نصب نشود و فقط وقتی `versionCode` سرور از نسخه نصب‌شده بیشتر باشد، بروزرسانی را پیشنهاد می‌کند.

> آدرس نمونه `example.com` فقط placeholder است و تا زمانی که آن را با آدرس واقعی خودتان جایگزین نکنید، بروزرسانی سرور فعال نخواهد بود.

## ساخت خودکار update.json داخل برنامه

در نسخه Admin، مسیر «تنظیمات → ساخت فایل update.json» فایل APK را انتخاب می‌کند، `packageName`/`versionCode`/`versionName`/`sha256` را مستقیماً از همان فایل می‌خواند و پس از وارد کردن آدرس دانلود روی سرور، یک `update.json` کامل و آماده‌ی آپلود می‌سازد (کلیدها دقیقاً مطابق قالب همین فایل). این روش نیاز به اجرای دستی `sha256sum`/`certutil` را از بین می‌برد، اما هنوز هم می‌توانید از روش زیر برای بررسی دستی استفاده کنید.

## SHA-256 فایل APK (روش دستی)

بعد از ساخت هر APK، SHA-256 همان فایل را محاسبه و در `update.json` قرار دهید. نمونه در Windows:

```text
certutil -hashfile TaziehStudio-Admin.apk SHA256
certutil -hashfile TaziehStudio-User.apk SHA256
```

در Linux/macOS/CI:

```text
sha256sum TaziehStudio-Admin.apk
sha256sum TaziehStudio-User.apk
```

مقدار باید دقیقاً 64 کاراکتر هگزادسیمال باشد. برنامه قبل از نصب آن را بررسی می‌کند.
