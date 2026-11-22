package com.example.bookapp.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/** ضمائم استخراج‌شده از DOCX؛ فایل اصلی همیشه جداگانه و بدون تغییر نگهداری می‌شود. */
data class DocxSupplementalResult(
    val footnotes: List<ImportedFootnote>,
    val extractedMedia: List<File>,
    val warnings: List<String>
)
data class ImportedFootnote(val id: String, val text: String)

object DocxSupplementalExtractor {
    private const val MAX_ENTRY_BYTES = 20L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 60L * 1024L * 1024L
    private val allowedMedia = setOf("png", "jpg", "jpeg", "gif", "bmp", "tif", "tiff", "webp")

    fun extract(bytes: ByteArray, destination: File): DocxSupplementalResult {
        destination.mkdirs()
        val footnotes = mutableListOf<ImportedFootnote>()
        val media = mutableListOf<File>()
        val warnings = mutableListOf<String>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val normalized = entry.name.replace('\\', '/')
                require(!normalized.startsWith("/") && normalized.split('/').none { it == ".." }) { "مسیر ناامن در بسته Word" }
                if (!entry.isDirectory && (normalized == "word/footnotes.xml" || normalized.startsWith("word/media/"))) {
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var entryBytes = 0L
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        entryBytes += n; total += n
                        require(entryBytes <= MAX_ENTRY_BYTES && total <= MAX_TOTAL_BYTES) { "حجم ضمائم Word بیش از حد مجاز است" }
                        out.write(buffer, 0, n)
                    }
                    val data = out.toByteArray()
                    if (normalized == "word/footnotes.xml") {
                        footnotes += parseFootnotes(data)
                    } else {
                        val ext = normalized.substringAfterLast('.', "").lowercase()
                        if (ext in allowedMedia) {
                            val target = File(destination, "media-${media.size + 1}.$ext")
                            FileOutputStream(target).use { it.write(data) }
                            media += target
                        } else warnings += "رسانه با قالب پشتیبانی‌نشده نادیده گرفته شد: $ext"
                    }
                }
                zip.closeEntry()
            }
        }
        if (footnotes.isEmpty()) warnings += "پاورقی مستقل Word پیدا نشد."
        if (media.isEmpty()) warnings += "تصویر تعبیه‌شده قابل استخراج پیدا نشد."
        return DocxSupplementalResult(footnotes, media, warnings)
    }

    private fun parseFootnotes(xml: ByteArray): List<ImportedFootnote> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(ByteArrayInputStream(xml), "UTF-8") }
        val result = mutableListOf<ImportedFootnote>()
        var id: String? = null
        var text = StringBuilder()
        var inFootnote = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "footnote" -> { id = parser.getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "id") ?: parser.getAttributeValue(null, "w:id"); text = StringBuilder(); inFootnote = true }
                    "t" -> if (inFootnote) text.append(parser.nextText())
                    "tab" -> if (inFootnote) text.append('\t')
                    "br" -> if (inFootnote) text.append('\n')
                }
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "footnote") {
                val value = text.toString().trim()
                if (inFootnote && value.isNotBlank() && id !in setOf("-1", "0")) result += ImportedFootnote(id ?: "", value)
                inFootnote = false; id = null
            }
            parser.next()
        }
        return result
    }
}

/** تصویرسازی صفحه PDF برای اتصال به OCR؛ هر صفحه با ابعاد محدود رندر می‌شود تا RAM کنترل شود. */
object PdfPageRasterizer {
    const val MAX_RENDER_DIMENSION = 2200
    fun renderPage(pdfFile: File, pageIndex: Int, output: File, maxDimension: Int = MAX_RENDER_DIMENSION): File {
        require(pdfFile.isFile && pdfFile.length() > 0) { "فایل PDF در دسترس نیست." }
        require(maxDimension in 400..MAX_RENDER_DIMENSION) { "ابعاد تصویر خارج از محدوده مجاز است." }
        val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                require(pageIndex in 0 until renderer.pageCount) { "شماره صفحه PDF معتبر نیست." }
                renderer.openPage(pageIndex).use { page ->
                    val scale = minOf(1f, maxDimension.toFloat() / maxOf(page.width, page.height))
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "ذخیره تصویر صفحه PDF ناموفق بود." }
                    }
                    bitmap.recycle()
                }
            }
        }
        return output
    }
}

/**
 * OCR engine adapter contract. Persian/Arabic OCR is deliberately not faked: an engine must
 * explicitly advertise language support and return provenance/confidence for each page.
 */
data class OcrRecognition(val text: String, val language: String, val confidence: Float, val engine: String)
interface PersianArabicOcrEngine {
    val supportedLanguages: Set<String>
    suspend fun recognize(imageFile: File, language: String): OcrRecognition
}
fun requirePersianArabicOcr(engine: PersianArabicOcrEngine, language: String): PersianArabicOcrEngine {
    require(language in setOf("fas", "ara")) { "کد زبان باید fas یا ara باشد." }
    require(language in engine.supportedLanguages) { "موتور OCR نصب‌شده زبان $language را پشتیبانی نمی‌کند." }
    return engine
}

/** قرارداد استخراج متن لایه دیجیتال PDF؛ پیاده‌سازی باید ترتیب صفحه‌ها را حفظ کند. */
data class PdfPageText(val pageNumber: Int, val text: String)
interface PdfTextExtractionProvider {
    val providerId: String
    suspend fun extract(pdfFile: File): List<PdfPageText>
}
object PdfTextExtractionGateway {
    suspend fun extract(provider: PdfTextExtractionProvider, pdfFile: File): List<PdfPageText> {
        require(pdfFile.isFile && pdfFile.length() in 1..(50L * 1024L * 1024L)) { "فایل PDF نامعتبر یا بیش از حد بزرگ است." }
        val pages = provider.extract(pdfFile).sortedBy { it.pageNumber }
        require(pages.map { it.pageNumber }.distinct().size == pages.size) { "شماره صفحه تکراری در خروجی استخراج PDF" }
        require(pages.all { it.pageNumber > 0 && it.text.length <= 1_000_000 }) { "خروجی استخراج PDF معتبر نیست." }
        return pages
    }
}
