package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.bookapp.domain.model.NewSourceDocument
import com.example.bookapp.domain.model.SourceDocumentType
import com.example.bookapp.domain.repository.ArchiveRepository
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

/**
 * Admin-only, review-first intake for original DOCX/PDF sources.
 *
 * The original bytes are copied into app-private storage before parsing. Nothing is
 * written into the canonical Tazieh content tables by this class. Extracted DOCX
 * structure is stored as a review artifact and remains pending human approval.
 */
data class SourceIntakePreview(
    val sourceUid: String,
    val originalName: String,
    val mimeType: String,
    val sha256: String,
    val byteCount: Long,
    val pageCount: Int?,
    val detectedFields: Int,
    val detectedTaziehs: Int,
    val detectedRoles: Int,
    val detectedSections: Int,
    val extractedPreviewPath: String?,
    val extractedPreviewText: String?,
    val storedSourcePath: String,
    val extractedFootnotes: Int = 0,
    val extractedMediaPaths: List<String> = emptyList(),
    val warnings: List<String>
)

sealed class SourceIntakeResult {
    data class ReadyForReview(val preview: SourceIntakePreview) : SourceIntakeResult()
    data class Rejected(val reason: String) : SourceIntakeResult()
}

class SourceDocumentIntake(
    private val context: Context,
    private val archiveRepository: ArchiveRepository
) {
    companion object {
        const val MAX_SOURCE_BYTES = 50L * 1024L * 1024L
        private const val MAX_DOCX_EXPANDED_BYTES = 100L * 1024L * 1024L
        private const val MAX_DOCX_ENTRIES = 5000
    }

    /**
     * Import is deliberately two-phase:
     * 1) preserve original + prepare preview; 2) register source metadata as pending.
     * It never calls importContentJson/importWordContent.
     */
    suspend fun intake(uri: Uri, versionUid: String): SourceIntakeResult {
        val version = versionUid.trim()
        if (version.isEmpty()) return SourceIntakeResult.Rejected("شناسه نسخه تعزیه مشخص نیست")

        val originalName = safeDisplayName(uri)
            ?: return SourceIntakeResult.Rejected("نام فایل قابل تشخیص نیست")
        val extension = originalName.substringAfterLast('.', "").lowercase()
        if (extension !in setOf("docx", "pdf", "txt", "jpg", "jpeg", "png")) {
            return SourceIntakeResult.Rejected("فرمت‌های مجاز: DOCX، PDF، TXT، JPG و PNG")
        }

        val mime = when (extension) {
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/png"
        }
        val incoming = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_SOURCE_BYTES) throw IllegalArgumentException("حجم فایل از حد مجاز ۵۰ مگابایت بیشتر است")
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            } ?: return SourceIntakeResult.Rejected("فایل از مسیر انتخاب‌شده قابل خواندن نیست")
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            return SourceIntakeResult.Rejected(e.message ?: "خواندن فایل ناموفق بود")
        }
        if (incoming.isEmpty()) return SourceIntakeResult.Rejected("فایل خالی است")

        val uid = UUID.randomUUID().toString()
        val sourceDir = File(context.filesDir, "archive_sources").apply { mkdirs() }
        val stored = File(sourceDir, "$uid.$extension")
        try {
            if (extension == "docx") validateDocxContainer(incoming)
            if (extension == "pdf" && !incoming.copyOfRange(0, minOf(incoming.size, 5)).toString(Charsets.US_ASCII).startsWith("%PDF-")) {
                throw IllegalArgumentException("ساختار فایل PDF معتبر نیست")
            }
            if (extension in setOf("jpg", "jpeg", "png")) {
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(incoming, 0, incoming.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("فایل تصویر معتبر نیست")
            }
            FileOutputStream(stored).use { it.write(incoming) }
            val digest = sha256(incoming)

            var pageCount: Int? = null
            var fields = 0
            var taziehs = 0
            var roles = 0
            var sections = 0
            var previewPath: String? = null
            var previewText: String? = null
            var extractedFootnotes = 0
            val extractedMediaPaths = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            if (extension == "docx") {
                val json = wordToCompatibleJson(incoming)
                val root = JSONArray(json)
                fields = root.length()
                for (i in 0 until root.length()) {
                    val field = root.optJSONObject(i) ?: continue
                    val ts = field.optJSONArray("taziehs") ?: JSONArray()
                    taziehs += ts.length()
                    for (j in 0 until ts.length()) {
                        val rs = ts.optJSONObject(j)?.optJSONArray("roles") ?: JSONArray()
                        roles += rs.length()
                        for (k in 0 until rs.length()) {
                            val ss = rs.optJSONObject(k)?.optJSONArray("sections") ?: JSONArray()
                            sections += ss.length()
                        }
                    }
                }
                val preview = File(sourceDir, "$uid.review-preview.json")
                preview.writeText(json, Charsets.UTF_8)
                previewPath = preview.absolutePath
                previewText = json.take(12000)
                val supplementalDir = File(sourceDir, "$uid-supplemental")
                val supplemental = DocxSupplementalExtractor.extract(incoming, supplementalDir)
                extractedFootnotes = supplemental.footnotes.size
                extractedMediaPaths += supplemental.extractedMedia.map { it.absolutePath }
                if (supplemental.footnotes.isNotEmpty()) {
                    val footnotesJson = org.json.JSONArray().apply {
                        supplemental.footnotes.forEach { put(org.json.JSONObject().put("id", it.id).put("text", it.text).put("reviewStatus", "PENDING")) }
                    }
                    File(sourceDir, "$uid.footnotes-review.json").writeText(footnotesJson.toString(2), Charsets.UTF_8)
                }
                warnings += "ساختار استخراج‌شده پیشنهادی است؛ پیش از تأیید باید با سند اصلی تطبیق داده شود."
                warnings += supplemental.warnings
                if (sections == 0) warnings += "عنوان‌های Heading 1 تا Heading 4 شناسایی نشدند؛ ساختار سند نیازمند بازبینی دستی است."
            } else if (extension == "pdf") {
                val extracted = extractPdfText(context, incoming)
                pageCount = extracted.pageCount
                previewText = extracted.text.take(12000).ifBlank { null }
                if (!previewText.isNullOrBlank()) {
                    val preview = File(sourceDir, "$uid.review-preview.txt")
                    preview.writeText(extracted.text, Charsets.UTF_8)
                    previewPath = preview.absolutePath
                    warnings += "متن PDF استخراج شد؛ پیش از تبدیل نهایی باید ترتیب نقش‌ها و بخش‌ها بازبینی شود."
                } else {
                    warnings += "این PDF احتمالاً اسکن‌شده یا تصویری است؛ موتور OCR فارسی/عربی هنوز در این نسخه فعال نیست."
                }
            } else if (extension == "txt") {
                val text = incoming.toString(Charsets.UTF_8).removePrefix("\uFEFF").trim()
                previewText = text.take(12000).ifBlank { null }
                val preview = File(sourceDir, "$uid.review-preview.txt")
                preview.writeText(text, Charsets.UTF_8)
                previewPath = preview.absolutePath
                warnings += "متن خام آماده بازبینی است؛ ساختار زمینه/مجلس/نقش/بخش باید پیش از انتشار تعیین شود."
            } else {
                warnings += "تصویر اصلی ذخیره شد؛ تشخیص متن فارسی/عربی از تصویر پس از افزودن مدل OCR انجام می‌شود."
            }

            val note = buildString {
                append("واردسازی اولیه؛ وضعیت بررسی: در انتظار تأیید انسانی.")
                if (previewPath != null) append(" مسیر پیش‌نمایش ساختاری: ").append(previewPath)
                if (extractedFootnotes > 0) append(" پاورقی‌های استخراج‌شده در انتظار تطبیق: ").append(extractedFootnotes)
                if (extractedMediaPaths.isNotEmpty()) append(" مسیر رسانه‌های استخراج‌شده: ").append(extractedMediaPaths.joinToString(";"))
            }
            val registered = archiveRepository.addSource(
                NewSourceDocument(
                    versionUid = version,
                    fileName = originalName,
                    mimeType = mime,
                    sha256 = digest,
                    storagePath = stored.absolutePath,
                    documentType = SourceDocumentType.ORIGINAL,
                    pageCount = pageCount,
                    notes = note
                )
            )
            if (registered.isFailure) {
                stored.delete()
                previewPath?.let { File(it).delete() }
                File(sourceDir, "$uid-supplemental").deleteRecursively()
                return SourceIntakeResult.Rejected(registered.exceptionOrNull()?.message ?: "ثبت مشخصات سند در آرشیو ناموفق بود")
            }

            val registeredDocument = registered.getOrThrow()
            return SourceIntakeResult.ReadyForReview(
                SourceIntakePreview(
                    sourceUid = registeredDocument.uid,
                    originalName = originalName,
                    mimeType = mime,
                    sha256 = digest,
                    byteCount = incoming.size.toLong(),
                    pageCount = pageCount,
                    detectedFields = fields,
                    detectedTaziehs = taziehs,
                    detectedRoles = roles,
                    detectedSections = sections,
                    extractedPreviewPath = previewPath,
                    extractedPreviewText = previewText,
                    storedSourcePath = stored.absolutePath,
                    extractedFootnotes = extractedFootnotes,
                    extractedMediaPaths = extractedMediaPaths.toList(),
                    warnings = warnings
                )
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            stored.delete()
            File(sourceDir, "$uid-supplemental").deleteRecursively()
            throw cancelled
        } catch (e: Exception) {
            stored.delete()
            File(sourceDir, "$uid-supplemental").deleteRecursively()
            return SourceIntakeResult.Rejected(e.message ?: "پردازش سند ناموفق بود")
        }
    }

    private fun safeDisplayName(uri: Uri): String? {
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val raw = fromProvider?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: return null
        return raw.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").take(180)
    }

    private fun validateDocxContainer(bytes: ByteArray) {
        val temp = File.createTempFile("docx-check-", ".zip", context.cacheDir)
        try {
            temp.writeBytes(bytes)
            ZipFile(temp).use { zip ->
                val entries = zip.entries()
                var count = 0
                var expanded = 0L
                var hasDocument = false
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    count++
                    if (count > MAX_DOCX_ENTRIES) throw IllegalArgumentException("تعداد اجزای داخلی Word غیرعادی است")
                    if (entry.name.startsWith("/") || entry.name.split('/').any { it == ".." }) {
                        throw IllegalArgumentException("مسیر غیرمجاز در بسته Word شناسایی شد")
                    }
                    if (entry.name == "word/document.xml") hasDocument = true
                    if (entry.size > 0) {
                        expanded += entry.size
                        if (expanded > MAX_DOCX_EXPANDED_BYTES) throw IllegalArgumentException("حجم بازشده Word از حد مجاز بیشتر است")
                    }
                }
                if (!hasDocument) throw IllegalArgumentException("فایل DOCX معتبر نیست؛ document.xml وجود ندارد")
            }
        } finally {
            temp.delete()
        }
    }

    private fun countPdfPages(file: File): Int {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { fd -> PdfRenderer(fd).use { renderer -> return renderer.pageCount } }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
