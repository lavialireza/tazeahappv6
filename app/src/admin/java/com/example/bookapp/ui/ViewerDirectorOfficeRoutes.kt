package com.example.bookapp.ui

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.example.bookapp.data.AppDatabase

/** این مسیر فقط در Viewer واقعی است؛ Admin از این route استفاده نمی‌کند. */
fun NavGraphBuilder.viewerOnlyRoutes(navController: NavHostController, context: Context, db: AppDatabase, permissions: Map<String, Boolean>) {
    // عمداً خالی؛ صفحه و منطق Viewer در APK ادمین کامپایل نمی‌شوند.
}
