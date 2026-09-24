# اصلاح خطای Build Stage 62

بر اساس لاگ ارسالی، خطای اصلی در `UpdateHelper.kt` بود:

`Unresolved reference: BuildConfig`

علت: کلاس `BuildConfig` مربوط به namespace برنامه در فایل `UpdateHelper.kt` import نشده بود.

اصلاح انجام‌شده:
`import com.example.bookapp.BuildConfig`

این اصلاح فقط مربوط به سیستم بروزرسانی است و به سایر بخش‌های برنامه دست زده نشده است.
