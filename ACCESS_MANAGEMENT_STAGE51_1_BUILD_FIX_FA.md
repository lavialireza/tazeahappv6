# Stage 51.1 — اصلاح خطای Build صفحه مدیریت دسترسی

مبنای این نسخه: Tazieh Android ACCESS Stage51 Permission Categories

## اصلاح واقعی
در فایل:
`app/src/main/java/com/example/bookapp/ui/screens/ViewerAccessManagementScreen.kt`

ساختار `accessPermissionGroups` از نوع `List<PermissionGroup>` است و هر گروه پس از فیلتر ممکن بود `null` شود. کد قبلی از `flatMap` استفاده می‌کرد که با خروجی nullable سازگار نبود و خطای Kotlin ایجاد می‌کرد.

اصلاح انجام‌شده:
- `flatMap` به `mapNotNull` تغییر یافت.
- ساختار Pair گروه و آیتم‌ها حفظ شده تا کمترین تغییر ممکن ایجاد شود.
- کلیدهای ۱۵ Permission قبلی تغییر نکرده‌اند.

## وضعیت Build
این محیط Android SDK/Gradle قابل اجرای کامل در اختیار ندارد؛ بنابراین Build موفق ادعا نشده است. اصلاح بر اساس خطای واقعی گزارش‌شده از GitHub Actions انجام شده است.
