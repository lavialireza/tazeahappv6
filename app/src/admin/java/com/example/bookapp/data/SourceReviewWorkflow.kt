package com.example.bookapp.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** چرخه بازبینی مستقل از داده قطعی؛ تا زمانی که مدیر تأیید نکند، هیچ تغییری در جدول متن ایجاد نمی‌شود. */
object SourceReviewWorkflow {
    enum class Status { PENDING, ACCEPTED, REJECTED }
    data class ReviewArtifact(val sourceUid: String, val originalSha256: String, val extractedText: String, val status: Status, val reviewerNote: String, val updatedAt: Long)

    fun save(context: Context, sourceUid: String, originalSha256: String, extractedText: String, status: Status = Status.PENDING, note: String = ""): File {
        require(sourceUid.matches(Regex("[A-Za-z0-9._-]{4,160}"))) { "شناسه سند معتبر نیست." }
        require(originalSha256.matches(Regex("[a-fA-F0-9]{64}"))) { "هش سند اصلی معتبر نیست." }
        require(extractedText.length <= 8_000_000) { "متن بازبینی بیش از حد مجاز است." }
        val dir = File(context.filesDir, "source_reviews").apply { mkdirs() }
        val updatedAt = System.currentTimeMillis()
        val file = File(dir, "$sourceUid-$updatedAt.review.json")
        val root = JSONObject().put("reviewSchema", 1).put("sourceUid", sourceUid).put("originalSha256", originalSha256)
            .put("extractedText", extractedText).put("status", status.name).put("reviewerNote", note.take(20_000))
            .put("updatedAt", updatedAt)
        root.put("artifactSha256", sha256(root.toString()))
        file.writeText(root.toString(2), Charsets.UTF_8)
        return file
    }

    fun read(file: File): ReviewArtifact {
        require(file.isFile && file.length() <= 12L * 1024L * 1024L) { "فایل بازبینی موجود نیست یا بیش از حد بزرگ است." }
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val expected = root.optString("artifactSha256")
        val copy = JSONObject(root.toString()).apply { remove("artifactSha256") }
        require(expected == sha256(copy.toString())) { "هش پرونده بازبینی با محتوای آن تطبیق ندارد." }
        return ReviewArtifact(root.getString("sourceUid"), root.getString("originalSha256"), root.getString("extractedText"), Status.valueOf(root.getString("status")), root.optString("reviewerNote"), root.getLong("updatedAt"))
    }

    fun list(context: Context): List<File> = File(context.filesDir, "source_reviews").listFiles { f -> f.isFile && f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun verifyOriginal(sourceFile: File, expectedSha256: String): Boolean {
        if (!sourceFile.isFile || sourceFile.length() > 50L * 1024L * 1024L) return false
        val digest = MessageDigest.getInstance("SHA-256")
        sourceFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
