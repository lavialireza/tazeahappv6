# دو نوع Build: مدیر و کاربر

این پروژه دو APK مستقل می‌سازد:

- Admin: `com.example.bookapp` — دسترسی کامل مدیریت محتوا
- Viewer: `com.example.bookapp.viewer` — فقط مشاهده

Workflow:
- `gradle assembleAdminDebug`
- `gradle assembleViewerDebug`

هر دو APK به‌صورت Artifact و GitHub Release منتشر می‌شوند.

در نسخه Viewer، محتوای داخلی همراه APK فقط برای نمایش در برنامه بارگذاری می‌شود و مسیرهای مدیریت محتوا، ورود JSON/Word و ابزارهای ویرایش در دسترس نیستند.
