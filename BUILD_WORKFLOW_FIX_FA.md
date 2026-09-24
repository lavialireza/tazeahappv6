# BUILD FIX

خطای Build این نسخه از کامپایل برنامه نبود. هر دو APK با موفقیت ساخته شدند:
- Admin Debug: BUILD SUCCESSFUL
- Viewer Debug: BUILD SUCCESSFUL

شکست Workflow در مرحله «Verify APK version codes match release number» رخ داد.
دستور قبلی `sed` مقدار versionCode را همراه با متن بعدی استخراج می‌کرد و نتیجه‌ای مانند
`76' versionName='...'` تولید می‌کرد؛ بنابراین مقایسه با `76` شکست می‌خورد.

اصلاح انجام‌شده:
1. استخراج versionCode با الگوی محدود به عدد.
2. اعتبارسنجی خالی نبودن مقدار استخراج‌شده.
3. نمایش badging کامل در صورت خطا.
4. حذف وابستگی به نسخه ثابت 34.0.0 برای aapt و پیدا کردن خودکار aapt نصب‌شده.
5. بررسی وجود هر APK قبل از خواندن versionCode.

هشدارهای Kotlin/Java موجود در لاگ، خطای Build نبودند و مانع تولید APK نشده‌اند.
