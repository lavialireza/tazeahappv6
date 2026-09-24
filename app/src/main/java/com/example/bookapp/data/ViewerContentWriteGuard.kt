package com.example.bookapp.data

import com.example.bookapp.BuildConfig

/**
 * آخرین لایه دفاعی برای نوشتن محتوای اصلی.
 * UI و Permission Policy لایه‌های بالاتر هستند؛ این guard جلوی مسیرهای مستقیم
 * ذخیره‌سازی را نیز در نسخه عمومی می‌گیرد.
 */
object ViewerContentWriteGuard {
    fun check() {
        check(!BuildConfig.PUBLIC_VIEWER) {
            "نسخه User/Viewer فقط خواندنی است و اجازه تغییر محتوای اصلی را ندارد."
        }
    }
}
