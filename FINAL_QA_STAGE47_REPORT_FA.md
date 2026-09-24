# Stage47 — Final QA & Security Audit

مبنای بررسی: Stage46

## بررسی‌های انجام‌شده
- جداسازی assetهای Admin و Viewer: Viewer فقط `assets/content/001_sample.taz` دارد و JSON متن در `src/admin/assets` باقی مانده است.
- کنترل دسترسی Viewer: کاربر خاص فعال/منقضی/غیرفعال بررسی شد و در حالت غیرفعال/منقضی سقوط به دسترسی عمومی انجام نمی‌شود.
- انتقال سیاست: شناسه مقصد، امضای HMAC، انقضا و نسخه سیاست بررسی می‌شوند.
- نسخه سیاست به‌صورت per-target نگهداری می‌شود.
- همگام‌سازی: fingerprint، آخرین ارسال، آخرین نسخه ارسال‌شده و آخرین تأیید اعمال ثبت می‌شوند.
- یک ایراد امنیتی واقعی پیدا و اصلاح شد: `PolicyAppliedReceiver` قبلاً exported و بدون permission بود و یک برنامه دیگر می‌توانست broadcast جعلی ارسال کند. Receiver اکنون با permission سطح `signature` محافظت شده است.
- تأیید اعمال سیاست اکنون فقط وقتی ثبت می‌شود که نسخه گزارش‌شده دقیقاً همان نسخه آخرین سیاست ارسال‌شده باشد و fingerprint نیز با fingerprint ثبت‌شده Admin مطابقت داشته باشد.
- Backup/Restore شامل فیلد نسخه آخرین ارسال نیز شد.
- Viewer receipt شامل fingerprint سیاست شد.

## تست‌های ساختاری
- Manifestهای Admin/Viewer با XML parser بررسی شدند.
- ZIP با `testzip` بررسی شد.
- assetهای Viewer بررسی شدند.
- فراخوانی‌های `markPolicySent` و `markPolicyApplied` بررسی شدند.
- فیلد `lastPolicySentVersion` در serialization، backup/restore و history بررسی شد.

## محدودیت Build
در محیط بررسی Android SDK/Gradle Wrapper موجود نبود؛ بنابراین APK build محلی ادعا نمی‌شود. Build نهایی باید در GitHub Actions انجام شود.
