package com.example.bookapp.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.InputStream
import java.io.OutputStream
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** انتقال امنِ سیاست دسترسی بین Admin و Viewer؛ بدون سرور. */
object ViewerAccessTransfer {
    private const val SCHEMA = 1
    private const val TARGET_PUBLIC = "*"
    // این راز فقط برای اعتبارسنجی فایل سیاست است؛ امنیت مطلق/DRM نیست.
    private const val SHARED_SECRET = "TaziehAccessPolicy-2026-v1"
    private const val KEY_LAST_IMPORTED_VERSION = "last_imported_policy_version"
    private const val KEY_LAST_IMPORTED_VERSIONS = "last_imported_policy_versions"

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SHARED_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun buildPolicyJson(context: Context, targetInstallationId: String): String {
        val rawTarget = targetInstallationId.trim()
        val target = if (rawTarget.isBlank()) TARGET_PUBLIC else rawTarget.uppercase(java.util.Locale.US)
        val specialUser = if (target == TARGET_PUBLIC) null else
            ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId.equals(target, ignoreCase = true) }
        val permissions = if (target == TARGET_PUBLIC) {
            ViewerAccessPolicy.getPublicPermissions(context)
        } else {
            specialUser?.permissions ?: throw IllegalArgumentException("کاربر موردنظر پیدا نشد.")
        }
        val root = JSONObject()
            .put("schema", SCHEMA)
            .put("targetInstallationId", target)
            .put("policyVersion", ViewerAccessPolicy.getPolicyVersion(context, target))
            .put("issuedAt", System.currentTimeMillis())
            .put("expiresAt", if (target == TARGET_PUBLIC) JSONObject.NULL else specialUser?.expiresAt ?: JSONObject.NULL)
            .put("profile", if (target == TARGET_PUBLIC) ViewerAccessPolicy.PROFILE_PUBLIC else specialUser?.profile ?: ViewerAccessPolicy.PROFILE_CUSTOM)
        .put("enabled", if (target == TARGET_PUBLIC) true else specialUser?.enabled ?: false)
            .put("policyFingerprint", if (target == TARGET_PUBLIC) "" else ViewerAccessPolicy.policyFingerprint(specialUser ?: error("کاربر موردنظر پیدا نشد.")))
        val p = JSONObject(); ViewerAccessPolicy.permissionLabels.keys.forEach { p.put(it, permissions[it] == true) }
        root.put("permissions", p)
        val unsigned = root.toString()
        return JSONObject().put("schema", SCHEMA).put("payload", root).put("signature", sign(unsigned)).toString(2)
    }

    fun writePolicy(context: Context, targetInstallationId: String, output: OutputStream) {
        output.use { it.write(buildPolicyJson(context, targetInstallationId).toByteArray(Charsets.UTF_8)) }
    }

    /** فایل سیاست را در cache آماده می‌کند تا Admin بتواند آن را مستقیماً با Viewer به اشتراک بگذارد. */
    fun createShareUri(context: Context, targetInstallationId: String): android.net.Uri {
        val file = java.io.File(context.cacheDir, "viewer-access-share.json")
        file.outputStream().use { writePolicy(context, targetInstallationId, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun importPolicy(context: Context, input: InputStream): Result<String> = runCatching {
        val text = input.use { it.readBytes().toString(Charsets.UTF_8) }
        val envelope = JSONObject(text)
        require(envelope.optInt("schema", -1) == SCHEMA) { "نسخه فایل سیاست پشتیبانی نمی‌شود." }
        val payload = envelope.getJSONObject("payload")
        val signature = envelope.optString("signature")
        require(signature == sign(payload.toString())) { "امضای سیاست معتبر نیست." }
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
