package com.example.bookapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * تصویر انتخاب‌شده از گالری گوشی را داخل حافظه‌ی اختصاصی خود اپ کپی می‌کند
 * (پوشه‌ی files/tazieh_images) تا حتی اگر کاربر بعداً عکس اصلی را از گالری
 * پاک کند یا گوشی ری‌استارت شود، تصویر داخل اپ باقی بماند.
 * قبل از ذخیره، تصویر فشرده و در صورت لزوم کوچک می‌شود (حداکثر ضلع ۱۶۰۰px،
 * کیفیت JPEG ۸۰٪) تا حجم برنامه و مصرف حافظه به‌شدت افزایش پیدا نکند.
 * مسیر فایل کپی‌شده را برمی‌گرداند (برای ذخیره در دیتابیس).
 */
fun copyImageToAppStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "tazieh_images").apply { mkdirs() }
        val destFile = File(dir, "${UUID.randomUUID()}.jpg")

        val bitmap = decodeSampledBitmap(context, sourceUri, maxDimension = 1600)
        if (bitmap != null) {
            destFile.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)) { "ذخیره تصویر ناموفق بود." }
            }
            bitmap.recycle()
        } else {
            // اگر رمزگشایی به‌عنوان تصویر ممکن نبود، فایل خام کپی می‌شود (بدون فشرده‌سازی)
            val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: 0L
            check(copied > 0L) { "فایل تصویر قابل خواندن نیست." }
        }
        check(destFile.exists() && destFile.length() > 0L) { "فایل تصویر در حافظه برنامه ایجاد نشد." }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

/** تصویر را با یک نمونه‌برداری (inSampleSize) مناسب می‌خواند تا حافظه کمتری مصرف شود و بعد اگر هنوز بزرگ‌تر از حد لازم بود، دوباره اندازه‌اش می‌کند */
private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
    val (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    var inSampleSize = 1
    var halfW = width / 2
    var halfH = height / 2
    while (halfW / inSampleSize >= maxDimension || halfH / inSampleSize >= maxDimension) {
        inSampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    val sampled = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return null

    if (sampled.width <= maxDimension && sampled.height <= maxDimension) return sampled
    val scale = maxDimension.toFloat() / maxOf(sampled.width, sampled.height)
    val scaled = Bitmap.createScaledBitmap(sampled, (sampled.width * scale).toInt(), (sampled.height * scale).toInt(), true)
    if (scaled !== sampled) sampled.recycle()
    return scaled
}

fun deleteImageFromAppStorage(filePath: String) {
    try {
        File(filePath).delete()
    } catch (e: Exception) {
        // نادیده گرفتن؛ حذف رکورد دیتابیس مهم‌تر از حذف فایل باقی‌مانده است
    }
}

/**
 * فایل صوتی انتخاب‌شده از گوشی را برای یک بخش خاص در حافظه داخلی برنامه کپی
 * می‌کند (مشابه copyImageToAppStorage) تا صدای واقعی/ضبط‌شده به‌جای صدای
 * مصنوعی (TTS) پخش شود.
 */
fun copyAudioToAppStorage(context: Context, sourceUri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "tazieh_audio").apply { mkdirs() }
        val destFile = File(dir, "${UUID.randomUUID()}.mp3")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun deleteAudioFromAppStorage(filePath: String) {
    try {
        File(filePath).delete()
    } catch (e: Exception) {
        // نادیده گرفتن
    }
}
