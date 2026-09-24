# Tazieh Android — Stage 18 Footnote Build Fix

مبنای این مرحله: Stage 17، همان نسخه‌ای که قابلیت گالری Viewer و بزرگنمایی تصویر در آن حفظ شده بود.

لاگ Build ارسالی کاربر بررسی شد. چهار خطای کامپایل در `TextScreen.kt` وجود داشت:
- ناسازگاری نوع `onAdd` با callback تعلیقی (`suspend` و `Result<Unit>`).
- ارجاع نابجای `saving` در پنجره برچسب شخصی.
- خطاهای زنجیره `onSuccess/onFailure` که پیامد همان ناسازگاری نوع callback بودند.

اصلاح واقعی:
- امضای callback افزودن پاورقی در `FootnotesSection` به `suspend (...) -> Result<Unit>` تغییر کرد تا با `TextScreen.onAddFootnote` سازگار باشد.
- اجرای ذخیره همچنان در coroutine انجام می‌شود و نتیجه موفق/ناموفق را کنترل می‌کند.
- ارجاع اشتباه `saving` از پنجره برچسب شخصی حذف شد.
- مسیر گالری و بزرگنمایی تصویر تغییر داده نشد.

در این محیط Android SDK/Gradle در دسترس نیست؛ بنابراین Build محلی ادعا نمی‌شود. این بسته برای Build در GitHub Actions آماده شده است.
