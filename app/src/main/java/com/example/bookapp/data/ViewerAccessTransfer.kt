package com.example.bookapp.data

import android.content.Context
import android.content.Intent
import android.util.Base64
import java.io.InputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import org.json.JSONObject

/**
 * انتقال امنِ سیاست دسترسی بین Admin و Viewer؛ بدون سرور.
 *
 * امضا با کلید خصوصی RSA انجام می‌شود که فقط در app/src/admin/java
 * (کلاس ViewerAccessSigner) وجود دارد — هرگز در APK نسخه Viewer کامپایل
 * نمی‌شود. اینجا (مشترک بین هر دو Flavor) فقط با کلید عمومی اعتبارسنجی
 * می‌شود؛ کلید عمومی را می‌توان آزادانه در هر دو APK قرار داد چون فقط
 * برای «تأیید» به‌کار می‌رود، نه «ساخت» امضای جدید.
 *
 * قبلاً این فایل از یک HMAC با راز مشترک استفاده می‌کرد که چون هم در Admin
 * و هم در Viewer وجود داشت، مهندسی‌معکوسِ Viewer می‌توانست همان راز را
 * استخراج و Policy جعلی معتبر بسازد. با امضای نامتقارن، حتی مهندسی‌معکوسِ
 * کامل Viewer فقط کلید عمومی را نشان می‌دهد که برای جعل Policy جدید کافی
 * نیست.
 */
object ViewerAccessTransfer {
    internal const val SCHEMA = 1
    internal const val TARGET_PUBLIC = "*"
    private const val KEY_LAST_IMPORTED_VERSION = "last_imported_policy_version"
    private const val KEY_LAST_IMPORTED_VERSIONS = "last_imported_policy_versions"

    private val publicKey: PublicKey? by lazy {
        val b64 = com.example.bookapp.BuildConfig.POLICY_PUBLIC_KEY
        if (b64.isBlank()) return@lazy null
        runCatching {
            val der = Base64.decode(b64, Base64.NO_WRAP)
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
        }.getOrNull()
    }

    internal fun verify(payload: String, signatureHex: String): Boolean {
        val key = publicKey ?: return false
        return runCatching {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(key)
            sig.update(payload.toByteArray(Charsets.UTF_8))
            val clean = signatureHex.trim()
            if (clean.length % 2 != 0) return false
            val sigBytes = ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            sig.verify(sigBytes)
        }.getOrDefault(false)
    }

    fun importPolicy(context: Context, input: InputStream): Result<String> = runCatching {
        val text = input.use { it.readBytes().toString(Charsets.UTF_8) }
        val envelope = JSONObject(text)
        require(envelope.optInt("schema", -1) == SCHEMA) { "نسخه فایل سیاست پشتیبانی نمی‌شود." }
        val payload = envelope.getJSONObject("payload")
        val signature = envelope.optString("signature")
        require(verify(payload.toString(), signature)) { "امضای سیاست معتبر نیست." }
        val rawTarget = payload.optString("targetInstallationId").trim()
        val target = if (rawTarget == TARGET_PUBLIC) TARGET_PUBLIC else rawTarget.uppercase(java.util.Locale.US)
        val ownId = ViewerAccessPolicy.installationId(context).uppercase(java.util.Locale.US)
        require(target == TARGET_PUBLIC || target == ownId) { "این سیاست برای این دستگاه صادر نشده است." }
        val expiresAt = if (payload.isNull("expiresAt")) null else payload.optLong("expiresAt")
        require(expiresAt == null || expiresAt <= 0L || System.currentTimeMillis() <= expiresAt) { "تاریخ اعتبار این سیاست گذشته است." }
        val p = payload.getJSONObject("permissions")
        val rawPermissions = ViewerAccessPolicy.permissionLabels.keys.associateWith { p.optBoolean(it, false) }.toMutableMap()
        // سازگاری با سیاست‌های Stage53 که از کلید قدیمی footnoteSync استفاده می‌کردند.
        if (!p.has("footnoteSync.dictionary") && p.has("footnoteSync")) {
            rawPermissions["footnoteSync"] = p.optBoolean("footnoteSync", false)
        }
        val permissions = ViewerAccessPolicy.normalizedPermissions(rawPermissions)
        val version = payload.optInt("policyVersion", 1)
        require(version > 0) { "نسخه سیاست نامعتبر است." }
        val prefs = context.getSharedPreferences("viewer_access_policy", Context.MODE_PRIVATE)
        val versionKey = if (target == TARGET_PUBLIC) TARGET_PUBLIC else target
        val versions = runCatching { JSONObject(prefs.getString(KEY_LAST_IMPORTED_VERSIONS, "{}") ?: "{}") }.getOrElse { JSONObject() }
        val lastImported = versions.optInt(versionKey, prefs.getInt(KEY_LAST_IMPORTED_VERSION, 0))
        require(version > lastImported) { "این سیاست قبلاً دریافت شده یا از سیاست فعلی قدیمی‌تر است." }
        val enabled = payload.optBoolean("enabled", true)
        if (target == TARGET_PUBLIC) {
            ViewerAccessPolicy.setImportedPublicPermissions(context, permissions, version)
        } else {
            val profile = payload.optString("profile", ViewerAccessPolicy.PROFILE_CUSTOM)
            ViewerAccessPolicy.setImportedSpecialPermissions(context, permissions, expiresAt, version, profile, enabled)
        }
        if (target != TARGET_PUBLIC) {
            // تأیید اعمال واقعی را به Admin برمی‌گردانیم؛ این با «ارسال فایل» فرق دارد.
            runCatching {
                context.sendBroadcast(Intent("com.example.bookapp.POLICY_APPLIED").apply {
                    setPackage("com.example.bookapp")
                    putExtra("installationId", ownId)
                    putExtra("policyVersion", version)
                    putExtra("policyFingerprint", payload.optString("policyFingerprint", ""))
                    putExtra("appliedAt", System.currentTimeMillis())
                })
            }
        }
        versions.put(versionKey, version)
        check(prefs.edit()
            .putString(KEY_LAST_IMPORTED_VERSIONS, versions.toString())
            .putInt(KEY_LAST_IMPORTED_VERSION, maxOf(prefs.getInt(KEY_LAST_IMPORTED_VERSION, 0), version))
            .commit()) { "ثبت نسخه سیاست انجام نشد." }
        "سیاست دسترسی با موفقیت اعمال شد."
    }
}
