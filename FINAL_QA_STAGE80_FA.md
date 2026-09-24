# Stage 80 — نهایی‌سازی و کنترل انتشار

این مرحله بر پایه Stage 79 ساخته شده و تغییرات آن فقط برای کنترل نهایی انتشار دو نسخه است.

## خروجی‌های نهایی
- Admin: `com.example.bookapp`
- Viewer/User: `com.example.bookapp.viewer`
- هر دو APK در GitHub Actions جداگانه ساخته و Artifact می‌شوند.

## کنترل‌های انتشار
1. هر دو Flavor باید Build شوند.
2. versionCode هر دو APK باید برابر شماره Release همان اجرای GitHub Actions باشد.
3. Viewer فقط فایل محتوای محافظت‌شده `.taz` را از `app/src/viewer/assets/content/` دریافت می‌کند.
4. هیچ JSON متنی محتوای اصلی نباید داخل APK Viewer وجود داشته باشد.
5. Admin محتوای مدیریتی را از مسیر `app/src/admin/assets/` می‌گیرد.
6. applicationId دو APK باید متفاوت باشد تا نصب همزمان ممکن باشد.

## وضعیت
این بسته برای اجرای Build نهایی آماده است. Build واقعی در GitHub Actions باید به عنوان تأیید نهایی محیط CI انجام شود؛ در محیط فعلی Gradle/Android SDK اجرا نشده است.
