package com.example.bookapp.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * رمزگذاری/رمزگشایی ساده‌ی AES-GCM برای فایل پشتیبان، با کلیدی که از رمزی که
 * خود کاربر انتخاب می‌کند ساخته می‌شود (PBKDF2) — نه یک کلید ثابت داخل کد،
 * چون کلید ثابت هیچ امنیتی واقعی نمی‌دهد.
 *
 * فرمت خروجی (باینری): [16 بایت salt][12 بایت IV][متن رمزشده]
 * این آرایه بایت مستقیم داخل فایل پشتیبان نوشته می‌شود.
 */
private const val PBKDF2_ITERATIONS = 120_000
private const val KEY_LENGTH_BITS = 256
private const val GCM_TAG_LENGTH_BITS = 128

private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, "AES")
}

fun encryptBackupText(plainText: String, password: String): ByteArray {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val key = deriveKey(password, salt)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

    return salt + iv + encrypted
}

/** اگر رمز اشتباه باشد یا فایل خراب باشد، استثنا پرتاب می‌شود (توسط فراخوان گرفته می‌شود) */
fun decryptBackupBytes(data: ByteArray, password: String): String {
    require(data.size > 28) { "فایل پشتیبان خیلی کوچک/نامعتبر است" }
    val salt = data.copyOfRange(0, 16)
    val iv = data.copyOfRange(16, 28)
    val encrypted = data.copyOfRange(28, data.size)
    val key = deriveKey(password, salt)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val decrypted = cipher.doFinal(encrypted)
    return String(decrypted, Charsets.UTF_8)
}
