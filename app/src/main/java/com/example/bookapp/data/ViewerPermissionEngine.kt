package com.example.bookapp.data

import android.content.Context

/**
 * موتور مرکزی ارزیابی دسترسی Viewer.
 * تمام بررسی‌های والد/فرزند از یک نقطه انجام می‌شوند تا UI و عملیات واقعی
 * در صورت خاموش شدن یک مجوز، رفتار یکسان داشته باشند.
 */
object ViewerPermissionEngine {
    fun permissions(context: Context): Map<String, Boolean> =
        ViewerAccessPolicy.getEffectivePermissions(context)

    fun can(context: Context, key: String): Boolean =
        ViewerAccessPolicy.hasPermission(context, key)

    fun canAny(context: Context, vararg keys: String): Boolean =
        keys.any { can(context, it) }

    fun canAll(context: Context, vararg keys: String): Boolean =
        keys.all { can(context, it) }

    fun parentOf(key: String): String? =
        ViewerAccessPolicy.permissionParents[key]
}
