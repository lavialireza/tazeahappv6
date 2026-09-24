# مرحله 61 — سرور بروزرسانی مستقل برای Admin و User

در این مرحله GitHub Releases از منطق بروزرسانی داخل APK حذف شد.

## آدرس‌های داخل APK

فایل اصلی برای تغییر آدرس:

`update-servers.properties`

دو مقدار مستقل دارد:

- `UPDATE_SERVER_ADMIN_URL`
- `UPDATE_SERVER_VIEWER_URL`

مثلاً:

```properties
UPDATE_SERVER_ADMIN_URL=https://your-domain.com/tazieh/admin
UPDATE_SERVER_VIEWER_URL=https://your-domain.com/tazieh/user
```

در زمان Build، این مقادیر به صورت جداگانه داخل `BuildConfig.UPDATE_SERVER_URL` همان Flavor قرار می‌گیرند.

### بدون تغییر فایل اصلی هم قابل Override است

```text
-PUPDATE_SERVER_ADMIN_URL=https://your-domain.com/tazieh/admin
-PUPDATE_SERVER_VIEWER_URL=https://your-domain.com/tazieh/user
```

## رفتار برنامه

- برنامه در حالت عادی به اینترنت وصل نمی‌شود.
- فقط با انتخاب «بررسی بروزرسانی» به سرور همان Flavor متصل می‌شود.
- Admin فقط `admin/update.json` را می‌خواند.
- User فقط `user/update.json` را می‌خواند.
- `packageName` و `access` بررسی می‌شوند تا APK اشتباه دریافت نشود.
- اگر `versionCode` سرور بیشتر از نسخه نصب‌شده نباشد، هیچ APK دانلود نمی‌شود.
- اگر نسخه جدید باشد، APK از `apkUrl` دریافت و بعد از تکمیل برای نصب سیستم ارسال می‌شود.
- APK ناقص (`.part`) قابل نصب نیست.

## نکته مهم قبل از Build نهایی

مقادیر `example.com` در `update-servers.properties` نمونه هستند. قبل از Build نهایی باید با آدرس واقعی سرور خودتان جایگزین شوند.

نمونه فایل‌های سمت سرور در پوشه `update-server-template` قرار گرفته‌اند.
