package com.example.bookapp.data

import android.content.Context
import android.util.Base64
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.OutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * ساخت و امضای فایل Policy با کلید خصوصی RSA.
 *
 * عمداً فقط در app/src/admin/java قرار دارد — یعنی این کلاس و کلید خصوصی
 * که استفاده می‌کند، اصلاً در APK نسخه Viewer کامپایل نمی‌شوند (نه فقط از
 * دید کاربر پنهان‌اند، بلکه هیچ بایت‌کدی از آن‌ها در آنجا وجود ندارد).
 *
 * کلید خصوصی هرگز نباید در Git commit شود. آن را در local.properties
 * (برای build محلی) یا Secret گیت‌هاب با نام ADMIN_POLICY_PRIVATE_KEY
 * (برای release.yml) قرار دهید — به RELEASE_AUTOMATION_FA.md مراجعه کنید.
 */
object ViewerAccessSigner {
    private val privateKey: PrivateKey by lazy {
        val b64 = com.example.bookapp.BuildConfig.POLICY_PRIVATE_KEY
        require(b64.isNotBlank()) {
            "کلید خصوصی امضای Policy تنظیم نشده است. ADMIN_POLICY_PRIVATE_KEY را در local.properties یا Secret گیت‌هاب قرار دهید."
        }
        val der = Base64.decode(b64, Base64.NO_WRAP)
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
    }

    private fun sign(payload: String): String {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(payload.toByteArray(Charsets.UTF_8))
        return sig.sign().joinToString("") { "%02x".format(it) }
    }

    fun buildPolicyJson(context: Context, targetInstallationId: String): String {
        val rawTarget = targetInstallationId.trim()
        val target = if (rawTarget.isBlank()) ViewerAccessTransfer.TARGET_PUBLIC else rawTarget.uppercase(java.util.Locale.US)
        val specialUser = if (target == ViewerAccessTransfer.TARGET_PUBLIC) null else
            ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId.equals(target, ignoreCase = true) }
        val permissions = if (target == ViewerAccessTransfer.TARGET_PUBLIC) {
            ViewerAccessPolicy.getPublicPermissions(context)
        } else {
            specialUser?.permissions ?: throw IllegalArgumentException("کاربر موردنظر پیدا نشد.")
        }
        val root = JSONObject()
            .put("schema", ViewerAccessTransfer.SCHEMA)
            .put("targetInstallationId", target)
            .put("policyVersion", ViewerAccessPolicy.getPolicyVersion(context, target))
            .put("issuedAt", System.currentTimeMillis())
            .put("expiresAt", if (target == ViewerAccessTransfer.TARGET_PUBLIC) JSONObject.NULL else specialUser?.expiresAt ?: JSONObject.NULL)
            .put("profile", if (target == ViewerAccessTransfer.TARGET_PUBLIC) ViewerAccessPolicy.PROFILE_PUBLIC else specialUser?.profile ?: ViewerAccessPolicy.PROFILE_CUSTOM)
            .put("enabled", if (target == ViewerAccessTransfer.TARGET_PUBLIC) true else specialUser?.enabled ?: false)
            .put("policyFingerprint", if (target == ViewerAccessTransfer.TARGET_PUBLIC) "" else ViewerAccessPolicy.policyFingerprint(specialUser ?: error("کاربر موردنظر پیدا نشد.")))
        val p = JSONObject(); ViewerAccessPolicy.permissionLabels.keys.forEach { p.put(it, permissions[it] == true) }
        root.put("permissions", p)
        val unsigned = root.toString()
        return JSONObject().put("schema", ViewerAccessTransfer.SCHEMA).put("payload", root).put("signature", sign(unsigned)).toString(2)
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
}
