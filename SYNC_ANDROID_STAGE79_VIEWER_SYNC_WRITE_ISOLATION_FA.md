# Stage 79 — جداسازی کامل ارسال Sync در Viewer

## هدف
نسخه User/Viewer باید محتوای اصلی را فقط از سرور دریافت کند. داده‌های محتوایی
محلی Viewer نباید در PUT به Sync Server قرار بگیرند.

## داده‌های شخصی قابل ارسال
- notes
- bookmarks
- sectionTags
- recentSections
- readingHistory
- myRoles
- activeDays

## محتوای اصلی
- fields / taziehs / roles / sections
- footnotes
- dialogues / dialogueTurns
- images / audios
- corrections
- glossary

در Viewer این موارد فقط دریافت می‌شوند. در Admin همه `CONTENT_KEYS` از state
محلی برای انتشار به سرور ارسال می‌شوند.

## نتیجه
حتی اگر Viewer دارای داده محتوایی قدیمی باشد، Sync آن داده را به سرور برنمی‌گرداند.
این مرحله مکمل قفل Stage77/78 است و لایه Sync را نیز با همان سیاست دسترسی هماهنگ می‌کند.

## تست
`ViewerSyncWritePolicyTest.kt` whitelist ارسال Viewer را بررسی می‌کند.
