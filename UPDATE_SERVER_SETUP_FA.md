# تنظیم آدرس سرور بروزرسانی Admin و User

این مرحله بروزرسانی را از GitHub جدا می‌کند. برنامه فقط وقتی کاربر از داخل «تنظیمات → بروزرسانی برنامه» بررسی را می‌زند، به آدرس مخصوص همان نسخه وصل می‌شود.

## دو آدرس مستقل

- Admin: `UPDATE_SERVER_ADMIN_URL`
- User/Public: `UPDATE_SERVER_VIEWER_URL`

این دو مقدار در `app/build.gradle.kts` قابل تنظیم هستند و برای هر Flavor به صورت `BuildConfig.UPDATE_SERVER_URL` داخل APK قرار می‌گیرند.

### روش ۱ — برای Build محلی

مقادیر را در `local.properties` قرار دهید:

```properties
UPDATE_SERVER_ADMIN_URL=https://YOUR-DOMAIN/tazieh/admin
UPDATE_SERVER_VIEWER_URL=https://YOUR-DOMAIN/tazieh/user
```

### روش ۲ — هنگام GitHub Actions یا هر CI دیگر

بدون تغییر سورس می‌توانید پارامتر Gradle بدهید:

```text
-PUPDATE_SERVER_ADMIN_URL=https://YOUR-DOMAIN/tazieh/admin
-PUPDATE_SERVER_VIEWER_URL=https://YOUR-DOMAIN/tazieh/user
```

پس از Build، آدرس انتخاب‌شده داخل APK همان Flavor قرار می‌گیرد.

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

## SHA-256 فایل APK

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
