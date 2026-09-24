package com.example.bookapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Viewer دریافت تغییر سیاست را به‌صورت محلی ثبت می‌کند تا در اجرای بعدی به کاربر اطلاع دهد. */
class PolicyChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!BuildConfig.PUBLIC_VIEWER || intent?.action != "com.example.bookapp.POLICY_CHANGED") return
        val target = intent.getStringExtra("targetInstallationId") ?: "*"
        val version = intent.getIntExtra("policyVersion", 0)
        context.getSharedPreferences("viewer_access_policy", Context.MODE_PRIVATE).edit()
            .putBoolean("pending_policy_change_notice", true)
            .putString("pending_policy_change_target", target)
            .putInt("pending_policy_change_version", version)
            .apply()
    }
}
