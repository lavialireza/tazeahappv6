package com.example.bookapp.data

import android.content.Context
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Offline viewer content protection.
 *
 * The public viewer ships only encrypted .taz content. The content key is not
 * kept as one readable Kotlin string; its material is reconstructed from
 * encoded byte fragments and then hashed with SHA-256. R8/obfuscation is
 * enabled for Release builds.
 *
 * IMPORTANT: this is APK hardening, not absolute secrecy. An offline app must
 * eventually possess enough information to decrypt its packaged content.
 * The key format is versioned so future content updates can preserve backward
 * compatibility without changing the reader features.
 */
object ContentProtectionHelper {
    private const val NONCE_SIZE = 12
    private const val KEY_VERSION = 1
    private const val AES_KEY_SIZE = 32

    /** Versioned key derivation; keep V1 for all existing V1 .taz content. */
    private fun contentKey(version: Int): ByteArray {
        require(version == KEY_VERSION) { "نسخه کلید محتوای پشتیبانی‌نشده است" }

        // Encoded fragments are deliberately stored as byte values rather than
        // one readable password/string. The resulting material is identical to
        // the previous V1 key material, so existing encrypted content remains
        // compatible across app updates.
        val fragments = arrayOf(
            byteArrayOf(0x54, 0x7A, 0x39, 0x21),
            byteArrayOf(0x69, 0x65, 0x68, 0x23),
            byteArrayOf(0x32, 0x30, 0x32, 0x36),
            byteArrayOf(0x56, 0x69, 0x65, 0x77, 0x65, 0x72, 0x2D, 0x50, 0x72, 0x6F, 0x74, 0x65, 0x63, 0x74, 0x69, 0x6F, 0x6E),
            byteArrayOf(0x46, 0x61)
        )
        val material = ByteArray(fragments.sumOf { it.size })
        var offset = 0
        for (fragment in fragments) {
            fragment.copyInto(material, offset)
            offset += fragment.size
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(material)
        material.fill(0)
        require(digest.size == AES_KEY_SIZE)
        return digest
    }

    fun readProtectedJson(context: Context, assetName: String): String {
        require(assetName.endsWith(".taz")) { "فایل محتوای محافظت‌شده نامعتبر است" }
        val encrypted = context.assets.open("content/$assetName").use { it.readBytes() }
        require(encrypted.size > NONCE_SIZE) { "فایل محتوای محافظت‌شده نامعتبر است" }

        // V1 file format: [12-byte GCM nonce][ciphertext + 16-byte tag]
        val nonce = encrypted.copyOfRange(0, NONCE_SIZE)
        val ciphertext = encrypted.copyOfRange(NONCE_SIZE, encrypted.size)
        val key = contentKey(KEY_VERSION)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, nonce)
            )
            return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } finally {
            key.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }
}
