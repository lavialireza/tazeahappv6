# اصلاح نهایی نشت JSON به Viewer

در Build قبلی، فایل‌های `app/src/main/assets/` در Source Set مشترک قرار داشتند و Android Gradle Plugin آن‌ها را برای هر دو flavor بسته‌بندی می‌کرد. در نتیجه Viewer علاوه بر فایل محافظت‌شده `.taz`، فایل‌های JSON خام را نیز داخل APK می‌گذاشت.

## اصلاح انجام‌شده
- محتوای خام مشترک از `app/src/main/assets/` حذف شد.
- محتوای Admin فقط در `app/src/admin/assets/content/` قرار دارد.
- محتوای Viewer فقط در `app/src/viewer/assets/content/` و با پسوند `.taz` قرار دارد.
- مسیر واردکننده DOCX و مستندات به مسیر Admin اصلاح شد تا در آینده دوباره JSON مشترک ایجاد نشود.
- URL پیش‌فرض محتوای آنلاین Admin به مسیر `app/src/admin/assets/content/001_sample.json` اصلاح شد.
- Workflow علاوه بر `assets/content/*.json`، هر JSON موجود در assets Viewer را رد می‌کند.

## انتظار Build
- Admin: دارای `001_sample.json` برای محتوای قابل مدیریت.
- Viewer: دارای `001_sample.taz` و فاقد JSON خام.
- بررسی versionCode قبلی نیز حفظ شده و باید همچنان مقدار Build Number را دقیقاً تطبیق دهد.
