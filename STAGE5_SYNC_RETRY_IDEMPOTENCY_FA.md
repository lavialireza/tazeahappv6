# Stage 5 Android Sync Retry

`SyncServerHelper.kt` برای عملیات PUT از یک `requestId` ثابت استفاده می‌کند و در صورت خطای شبکه تا 3 بار همان درخواست را تکرار می‌کند. Server درخواست تکراری را دوباره Merge نمی‌کند.
