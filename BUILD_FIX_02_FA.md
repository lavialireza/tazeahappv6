# اصلاح Build — مرحله ۲

خطای آخر در مرحله Verify APK رخ می‌داد: Viewer با وجود اینکه فایل محتوای محافظت‌شده
را داشت، یک JSON خام نیز در APK نهایی پیدا می‌شد.

اصلاح‌های واقعی:
1. مسیر assets هر flavor با `setSrcDirs` صریح شد.
2. قبل از Build، `app:clean` اجرا می‌شود تا خروجی‌های قدیمی وارد APK نشوند.
3. بعد از Merge شدن assets هر Variant از Viewer، فایل‌های `.json` ناخواسته از
   خروجی merged assets حذف می‌شوند؛ فایل‌های `.taz` دست‌نخورده باقی می‌مانند.
4. Verify خود APK را با استخراج دقیق نام `assets/*.json` بررسی می‌کند و اگر فایلی
   وجود داشته باشد نام واقعی آن را گزارش می‌دهد.
5. بررسی `versionCode` قبلی حفظ شده است.

در سورس، JSON نمونه فقط در `app/src/admin/assets/content/001_sample.json` قرار دارد.
Viewer فقط `app/src/viewer/assets/content/001_sample.taz` را دریافت می‌کند.
