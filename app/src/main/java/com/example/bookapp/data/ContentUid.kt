package com.example.bookapp.data

import java.security.MessageDigest
import java.util.UUID

/** Stable identifiers used for content portability (backup/sync/deep links). */
object ContentUid {
    fun new(): String = UUID.randomUUID().toString()

    fun legacy(kind: String, databaseId: Long): String = "legacy-$kind-$databaseId"

    fun derived(kind: String, parentUid: String, title: String): String =
        sha256("$kind|$parentUid|${normalize(title)}")

    fun derivedWithIndex(kind: String, parentUid: String, title: String, index: Int): String =
        sha256("$kind|$parentUid|${normalize(title)}|$index")

    fun source(source: String): String = sha256("source|$source")

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
}
