# اصلاح بروزرسانی Admin/Viewer و بروزرسانی محتوا

- بررسی بروزرسانی برنامه اکنون بر اساس flavor فایل APK صحیح را انتخاب می‌کند:
  - Admin → app-admin-debug.apk
  - Viewer → app-viewer-debug.apk
- Viewer در استفاده عادی آفلاین است و INTERNET فقط برای بررسی بروزرسانی لازم است.
- ContentValidator دیگر UID را برای محتوای سازگار با قالب خارجی اجباری نمی‌کند؛ UID در صورت وجود اختیاری و در صورت وجود، تکراری بودن آن بررسی می‌شود.
- ساختار title/taziehs/roles/sections/content حفظ شده است.
- Backup/Restore دست‌نخورده باقی مانده است.
