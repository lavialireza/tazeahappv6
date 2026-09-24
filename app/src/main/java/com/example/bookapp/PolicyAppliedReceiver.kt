package com.example.bookapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.bookapp.data.AccessAuditLog
import com.example.bookapp.data.ViewerAccessPolicy

/** تأیید دریافت/اعمال سیاست از Viewer به Admin. */
class PolicyAppliedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.bookapp.POLICY_APPLIED") return
        if (com.example.bookapp.BuildConfig.PUBLIC_VIEWER) return
        val id = intent.getStringExtra("installationId")?.trim()?.uppercase(java.util.Locale.US) ?: return
        val version = intent.getIntExtra("policyVersion", 0)
        val fingerprint = intent.getStringExtra("policyFingerprint")?.trim().orEmpty()
        if (version <= 0 || fingerprint.isBlank()) return
        val updated = runCatching { ViewerAccessPolicy.markPolicyApplied(context, id, version, fingerprint) }.getOrNull() ?: return
        AccessAuditLog.record(context, "تأیید اعمال سیاست", updated, "Viewer تأیید کرد که سیاست نسخه $version روی دستگاه اعمال شده است.")
    }
}
