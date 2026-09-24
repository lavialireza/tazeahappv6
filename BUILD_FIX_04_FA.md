# اصلاح Build — FIX4

لاگ Build شماره 87 نشان داد که هر دو APK با موفقیت ساخته و versionCode برابر 87 بوده است، اما در مرحله بررسی APK Viewer یک JSON خام تشخیص داده شده است.

در FIX4 جداسازی Assetها صریح‌تر شده است:
- source set اصلی (`main`) هیچ assetای ندارد.
- Viewer با `setRoot("src/viewer")` و فقط `src/viewer/assets` ساخته می‌شود.
- Admin با `setRoot("src/admin")` و فقط `src/admin/assets` ساخته می‌شود.
- پیش از ساخت Viewer، درخت assetهای Viewer بررسی می‌شود و وجود JSON در آن Build را متوقف می‌کند.
- بررسی APK با `unzip -Z1` انجام می‌شود تا فهرست واقعی فایل‌های APK بدون ابهام بررسی شود.
- وجود `assets/content/001_sample.taz` و نبود هرگونه `assets/**/*.json` به‌طور مستقل بررسی می‌شود.

این اصلاح عمداً فایل JSON محتوای Admin را حذف یا رمزنگاری نمی‌کند؛ Admin باید همچنان محتوای JSON قابل مدیریت خود را داشته باشد.
