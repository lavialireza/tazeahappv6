# Tazieh Android — Stage 50
## اصلاح واقعی اعمال دسترسی‌های Viewer

این مرحله بر اساس ممیزی کامل Stage49 انجام شد.

### تغییرات واقعی اعمال‌شده
1. اتصال `advancedSearch` به `SearchScreen` از طریق `advancedEnabled = featureEnabled("advancedSearch")`.
2. اتصال `pdf` به `TaziehIndexScreen` تا مقدار پیش‌فرض صفحه نتواند محدودیت ادمین را دور بزند.
3. محافظت UI دکمه «مقایسه» با `compare` در فهرست بخش‌های نقش.
4. محافظت UI دکمه «حالت تمرین» با `training` و محافظت دوباره callback پیش از Navigation.
5. محافظت مستقیم Route گالری اختصاصی `ROUTE_TAZIEH_GALLERY` با `gallery`.

### مواردی که عمداً در این مرحله اضافه نشدند
- `footnoteSync`: هنوز Permission مستقل ندارد؛ ابتدا باید مکانیزم واقعی همگام‌سازی پاورقی تعریف/تأیید شود.
- دیکشنری اصطلاحات تعزیه: هنوز به عنوان Permission مستقل Viewer تعیین نشده است.
- تقویم محرم: فعلاً قابلیت عمومی است و Permission مستقل ندارد.
- همگام‌سازی محتوای آنلاین: طبق معماری فعلی Admin-only است و به Permission Viewer اضافه نشده است.

### اعتبارسنجی ساختاری
اسکریپت بررسی، حضور اتصال‌های زیر را تأیید کرد:
- advancedSearch -> SearchScreen
- pdf -> TaziehIndexScreen
- gallery -> direct gallery route
- compare -> sections UI
- training -> sections UI

### وضعیت Build
در محیط فعلی Android SDK/Gradle Wrapper قابل استفاده برای Build کامل موجود نیست؛ بنابراین این ZIP **به عنوان Build/Test شده اعلام نمی‌شود**. تغییرات سورس واقعاً اعمال و کنترل ساختاری شده‌اند.
