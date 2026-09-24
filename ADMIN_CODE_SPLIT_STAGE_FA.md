# جداسازی واقعی کد Admin از خروجی Viewer (Compile-Time Split)

## هدف
تا پیش از این مرحله، صفحات مدیریتی (مدیریت محتوا، ویرایشگر، ورود JSON/Word،
مدیریت دسترسی Viewer، کاربران ویژه، لاگ دسترسی) در `app/src/main/java` بودند
و فقط با شرط زمان‌اجرا `if (!publicViewer)` در Navigation از دسترس کاربر
Viewer پنهان می‌شدند. یعنی کد آن‌ها همچنان در build نوع `debug` بدون هیچ
obfuscation‌ای داخل APK نسخه Viewer وجود داشت، و در build نوع `release` هم
حذف‌شدنشان به تشخیص خودکار R8 وابسته بود، نه یک تضمین صریح.

## تغییر این مرحله
شش صفحه/مسیر کاملاً مخصوص Admin به یک source set جدا منتقل شدند:

- `ContentManagementScreen`
- `ContentEditorScreen`
- `DialogueBuilderScreen` (نوع داده مشترک `SectionPickerItem` در `main` باقی ماند چون صفحه‌ی گفتگوی Viewer هم به آن نیاز دارد)
- `SpecialUsersManagementScreen`
- `AccessAuditLogScreen`
- `ViewerAccessManagementScreen`

این فایل‌ها اکنون در `app/src/admin/java/com/example/bookapp/ui/screens/` هستند،
نه `app/src/main/java`.

ثبت مسیرهای Navigation مربوط به آن‌ها هم از `AppNavigation.kt` مشترک بیرون آمده
و به یک تابع extension به نام `adminOnlyRoutes(...)` منتقل شده که دو پیاده‌سازی
جدا دارد:

- `app/src/admin/java/com/example/bookapp/ui/AdminNavGraph.kt` — پیاده‌سازی واقعی.
- `app/src/viewer/java/com/example/bookapp/ui/AdminNavGraph.kt` — بدنه‌ی کاملاً خالی.

`AppNavigation.kt` مشترک فقط `adminOnlyRoutes(navController, db, context)` را
صدا می‌زند؛ اینکه این تابع چه کاری انجام می‌دهد به‌طور کامل به flavor‌ای که
دارد کامپایل می‌شود بستگی دارد.

## نتیجه
- در build نسخه Viewer (چه `debug` چه `release`)، این شش کلاس اصلاً در
  classpath آن variant قرار نمی‌گیرند و هیچ بایت‌کدی از آن‌ها داخل خروجی
  Viewer ساخته نمی‌شود — نه فقط پنهان یا obfuscate، بلکه از اساس کامپایل نشده‌اند.
- نسخه Admin بدون هیچ تغییری در رفتار، دقیقاً مثل قبل کار می‌کند.

## چیزی که تغییر نکرد (و هنوز به R8/Release وابسته است)
توابع سطح‌پایین‌تر داده (مثل import/export JSON، ورود Word، محاسبه گزارش سلامت
محتوا در `data/WordContentImport.kt`, `data/ContentValidator.kt`,
`data/ContentHealthHelper.kt`) همچنان در `main` مشترک هستند، چون منطق
رمزگشایی/اعتبارسنجی محتوای آن‌ها ممکن است در آینده جای دیگری هم لازم شود.
این توابع دیگر از هیچ‌جای کد Viewer صدا زده نمی‌شوند، پس در build نوع
`release` (با R8 فعال) قابل حذف خودکار هستند، اما در build نوع `debug`
همچنان به‌صورت کامپایل‌شده (بدون obfuscation) داخل APK Viewer باقی می‌مانند.
اگر می‌خواهید حتی همین‌ها هم به‌صورت تضمینی از Viewer حذف شوند، باید مرحله
بعدی این کار را روی این فایل‌های `data/` هم تکرار کنیم (تقسیم به
source set مخصوص Admin با یک Interface مشترک).

## نکته مهم درباره تست
این تغییرات در محیطی بدون دسترسی به Google Maven/Gradle ساخته و فقط از نظر
تعادل براکت/پرانتز و ارجاعات بین‌فایلی بررسی شده‌اند؛ باید حتماً هر دو
variant (`assembleAdminDebug` و `assembleViewerDebug`, و همچنین نسخه‌های
Release) از طریق همان GitHub Actions workflow قبلی Build و تست شوند.
