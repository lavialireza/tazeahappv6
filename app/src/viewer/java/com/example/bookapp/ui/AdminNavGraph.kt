package com.example.bookapp.ui

// نسخه Viewer عمومی این تابع را با بدنه خالی پیاده‌سازی می‌کند.
// صفحات مدیریتی واقعی (مدیریت محتوا، ویرایشگر، ورود JSON/Word، مدیریت
// دسترسی، کاربران ویژه، لاگ دسترسی) فقط در app/src/admin/java وجود دارند
// و به همین دلیل اصلاً در classpath این variant قرار نمی‌گیرند؛ یعنی نه‌فقط
// در دسترس کاربر نیستند، بلکه هیچ بایت‌کدی از آن‌ها داخل APK نسخه Viewer
// ساخته نمی‌شود.

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.example.bookapp.data.AppDatabase

fun NavGraphBuilder.adminOnlyRoutes(
    navController: NavHostController,
    db: AppDatabase,
    context: android.content.Context
) {
    // عمداً خالی: هیچ مسیر مدیریتی در نسخه Viewer ثبت نمی‌شود.
}
