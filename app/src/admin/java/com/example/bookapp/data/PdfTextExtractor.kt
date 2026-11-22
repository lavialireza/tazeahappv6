package com.example.bookapp.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

/** Text extraction for text-based PDFs. Scanned/image-only PDFs need OCR separately. */
data class PdfTextExtractionResult(
    val text: String,
    val pageCount: Int,
    val isScannedOrEmpty: Boolean
)

object PdfTextExtractor {
    @Volatile private var initialized = false

    @Synchronized
    private fun initialize(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    fun extract(context: Context, bytes: ByteArray): PdfTextExtractionResult {
        require(bytes.isNotEmpty()) { "فایل PDF خالی است" }
        initialize(context)
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            val text = PDFTextStripper().apply {
                sortByPosition = true
                addMoreFormatting = true
            }.getText(document).replace('\u0000', ' ').trim()
            return PdfTextExtractionResult(
                text = text,
                pageCount = document.numberOfPages,
                isScannedOrEmpty = text.replace(Regex("\\s+"), "").length < 12
            )
        }
    }
}

fun extractPdfText(context: Context, bytes: ByteArray): PdfTextExtractionResult =
    PdfTextExtractor.extract(context, bytes)
