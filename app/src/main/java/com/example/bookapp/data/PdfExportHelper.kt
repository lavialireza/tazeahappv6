package com.example.bookapp.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * خروجی PDF از تمام بخش‌های یک نقش (برای چاپ یا نگه‌داری آفلاین).
 * از StaticLayout برای چیدمان صحیح متن فارسی (راست‌به‌چپ) استفاده می‌شود.
 */
suspend fun exportRoleToPdf(context: Context, roleTitle: String, sections: List<SectionEntity>) {
    exportPdfInternal(context, roleTitle, listOf(roleTitle to sections))
}

/**
 * خروجی PDF از کل یک تعزیه: همه‌ی نقش‌ها پشت سر هم، هرکدام با عنوان نقش
 * به‌عنوان زیرعنوان، مناسب برای کارگردانی که می‌خواهد کل متن تعزیه را چاپ کند.
 */
suspend fun exportTaziehToPdf(context: Context, taziehTitle: String, roles: List<Pair<String, List<SectionEntity>>>) {
    exportPdfInternal(context, taziehTitle, roles)
}

/**
 * خروجی PDF از یک گفتگو: هر نوبت به‌عنوان یک بخش مستقل با نام نقش‌گوینده‌اش
 * به‌ترتیب چاپ می‌شود، شبیه یک نمایش‌نامه.
 */
suspend fun exportDialogueToPdf(context: Context, dialogueTitle: String, turns: List<Triple<String, String, String>>) {
    val roles = turns.map { (roleTitle, sectionTitle, content) ->
        roleTitle to listOf(SectionEntity(id = 0, roleId = 0, orderIndex = 0, title = sectionTitle, content = content))
    }
    exportPdfInternal(context, dialogueTitle, roles)
}

private suspend fun exportPdfInternal(
    context: Context,
    documentTitle: String,
    roles: List<Pair<String, List<SectionEntity>>>
) {
    val pageWidth = 595 // اندازه تقریبی A4 در نقطه (72dpi)
    val pageHeight = 842
    val margin = 40f
    val contentWidth = (pageWidth - margin * 2).toInt()

    val document = PdfDocument()
    val titlePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 18f
        textAlign = Paint.Align.RIGHT
    }
    val roleTitlePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 15f
        isFakeBoldText = true
    }
    val bodyPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 13f
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = margin

    fun newPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
    }

    // عنوان کلی سند (نام نقش یا نام کل تعزیه)
    canvas.drawText(documentTitle, pageWidth - margin, y + 20f, titlePaint)
    y += 40f

    val isMultiRole = roles.size > 1

    for ((roleTitle, sections) in roles) {
        if (isMultiRole) {
            val roleHeader = StaticLayout.Builder
                .obtain(roleTitle, 0, roleTitle.length, roleTitlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .build()
            if (y + roleHeader.height > pageHeight - margin) newPage()
            canvas.save()
            canvas.translate(margin, y)
            roleHeader.draw(canvas)
            canvas.restore()
            y += roleHeader.height + 14f
        }

        for (section in sections) {
            val header = StaticLayout.Builder
                .obtain(section.title, 0, section.title.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE) // راست‌چین
                .build()

            if (y + header.height > pageHeight - margin) newPage()
            canvas.save()
            canvas.translate(margin, y)
            header.draw(canvas)
            canvas.restore()
            y += header.height + 8f

            val body = StaticLayout.Builder
                .obtain(section.content, 0, section.content.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setLineSpacing(6f, 1f)
                .build()

            // متن طولانی را خط‌به‌خط بین چند صفحه تقسیم می‌کنیم تا از صفحه بیرون نزند.
            var startLine = 0
            while (startLine < body.lineCount) {
                val available = (pageHeight - margin - y).toInt()
                if (available < body.getLineBottom(startLine) - body.getLineTop(startLine)) {
                    newPage()
                    continue
                }

                var endLine = body.getLineForVertical(body.getLineTop(startLine) + available)
                if (endLine <= startLine) endLine = startLine + 1
                endLine = endLine.coerceAtMost(body.lineCount)

                val chunkHeight = body.getLineTop(endLine) - body.getLineTop(startLine)
                canvas.save()
                canvas.clipRect(margin, y, pageWidth - margin, pageHeight - margin)
                canvas.translate(margin, y - body.getLineTop(startLine))
                body.draw(canvas)
                canvas.restore()
                y += chunkHeight
                startLine = endLine

                if (startLine < body.lineCount) {
                    newPage()
                } else {
                    y += 24f
                }
            }
        }
    }

    document.finishPage(page)

    val safeTitle = documentTitle.replace(Regex("[^\\p{L}\\p{N}]"), "_")
    val file = File(context.cacheDir, "$safeTitle.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "ذخیره یا اشتراک‌گذاری PDF"))
}
