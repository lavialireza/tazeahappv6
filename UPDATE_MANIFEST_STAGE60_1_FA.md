# Stage60.1 — Smart Update Manifest

این مرحله سیستم بروزرسانی را از اتکا به نام Release/نام APK خارج می‌کند.

## روش تشخیص نسخه
- نسخه نصب‌شده از PackageManager و `versionCode` واقعی APK خوانده می‌شود.
- برنامه Releaseهای GitHub مخزن `lavialireza/taziehappv3` را بررسی می‌کند.
- اگر Release دارای asset به نام `update.json` باشد، `versionCode` و `versionName` و حداقل نسخه و اجباری بودن بروزرسانی از آن خوانده می‌شود.
- APK با نام درج‌شده در `apkFile` انتخاب می‌شود؛ اگر Manifest موجود نباشد، سازگاری با Releaseهای قدیمی `apk-build-N` حفظ شده است.
- قبل از نصب، Package و `versionCode` واقعی داخل APK دوباره بررسی می‌شود.

## update.json
فایل باید به‌عنوان asset همان Release در کنار APK قرار گیرد.
Admin از بخش تنظیمات → مدیریت Manifest بروزرسانی می‌تواند آن را تولید و ارسال کند.

## تاریخچه
موفقیت/خطای دانلود و درخواست نصب در SharedPreferences داخلی ثبت می‌شود و آخرین 20 رکورد نگهداری می‌شود.

## محدودیت تست
Gradle Wrapper/Android SDK در محیط فعلی موجود نبود؛ بنابراین Build واقعی ادعا نشده است. بررسی توازن براکت‌ها و ساختار ZIP انجام شده است.
