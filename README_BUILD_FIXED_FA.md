# نسخه اصلاح‌شده Build — Tazieh Android

این نسخه برای Build در GitHub Actions آماده شده است.

## اصلاحات اصلی

1. مسیر `app/src/viewer/assets` به Flavor `viewer` متصل شد تا فایل‌های رمزنگاری‌شده `.taz` واقعاً داخل APK کاربر قرار بگیرند.
2. مسیر `app/src/admin/assets` نیز به Flavor `admin` صریحاً متصل شد.
3. Manifest مخصوص Viewer اضافه شد تا مجوز `INTERNET` از Viewer حذف شود؛ Viewer کاملاً آفلاین باقی می‌ماند.
4. Workflow به نسخه پایدار `actions/*@v4` برگشت و Gradle 8.6 را صریحاً نصب می‌کند.
5. Workflow بعد از Build بررسی می‌کند که APK Viewer فایل `assets/content/001_sample.taz` را دارد و JSON خام محتوایی داخل Viewer بسته‌بندی نشده باشد.
6. دو APK جداگانه به عنوان Artifact ذخیره می‌شوند.
7. دو APK در GitHub Release نیز منتشر می‌شوند تا در صورت مشکل دانلود Artifact، مسیر جایگزین وجود داشته باشد.
8. `.gitignore` برای جلوگیری از ارسال `local.properties` و کلیدهای امضا اضافه شد.
9. اعلان محتوای جدید از لایه Sync حذف شد تا هنگام ورود به برنامه دوبار اعلان ایجاد نشود؛ مدیریت اعلان در UI باقی می‌ماند.

## Build محلی

این پروژه Wrapper ندارد؛ GitHub Actions از Gradle 8.6 استفاده می‌کند.

دستورات:

```text
gradle :app:assembleAdminDebug
gradle :app:assembleViewerDebug
```

خروجی‌ها:

```text
app/build/outputs/apk/debug/app-admin-debug.apk
app/build/outputs/apk/debug/app-viewer-debug.apk
```

## نکته امنیتی

Viewer آفلاین است و محتوای همراه آن با AES-GCM رمزگذاری شده است. این روش سخت‌سازی APK است و به معنی غیرقابل‌استخراج بودن مطلق محتوا نیست؛ هر برنامه آفلاینی که باید محتوا را نمایش دهد، در نهایت باید توانایی رمزگشایی آن را داشته باشد.
