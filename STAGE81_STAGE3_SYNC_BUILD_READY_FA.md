

## اصلاح Build پس از لاگ 2026-09-20
- SyncServerHelper.kt: ناسازگاری JSONArray/JSONObject در captureRemoteState اصلاح شد؛ captureRemoteTreeState برای ساختار درختی اضافه شد.
- ContentEditorScreen.kt: Context قبل از استفاده در حذف Tombstone تعریف شد.
- این اصلاحات برای رفع خطاهای کامپایل گزارش‌شده در logs_96149579313.zip اعمال شدند.
- این محیط ابزار Gradle/Android SDK کامل برای اجرای APK ندارد؛ بنابراین APK نهایی ادعا نمی‌شود مگر Build خارجی موفق شود.


FIX2: رفع خطای Unresolved reference: context در SectionDialog؛ LocalContext به Scope صحیح منتقل شد.
