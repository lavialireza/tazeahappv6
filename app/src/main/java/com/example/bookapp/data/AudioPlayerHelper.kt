package com.example.bookapp.data

import android.content.Context
import android.media.MediaPlayer

/**
 * پخش صوت واقعی (ضبط‌شده) برای بخش‌هایی که آهنگ/لحن خاص دارند، به‌عنوان
 * جایگزینی برای صدای مصنوعی (TTS). آدرس صوت (audioUrl) می‌تواند:
 *   - یک URL امن HTTPS باشد، یا
 *   - یک مسیر نسبی داخل assets باشد (مثلاً "audio/karbala_shahadat.mp3").
 * onStatus با مقادیر "started" / "done" / "error" فراخوانی می‌شود.
 */
class AudioPlayerHelper(private val context: Context, private val onStatus: (String) -> Unit = {}) {
    private var player: MediaPlayer? = null

    fun play(audioUrl: String) {
        stop()
        try {
            val mp = MediaPlayer()
            if (audioUrl.startsWith("https://")) {
                mp.setDataSource(audioUrl)
            } else if (audioUrl.startsWith("http://")) {
                // Cleartext HTTP is intentionally rejected; use HTTPS for remote audio.
                mp.release()
                onStatus("error")
                return
            } else if (audioUrl.startsWith("/")) {
                // مسیر مطلق فایل (مثلاً فایلی که کاربر از داخل اپ اضافه کرده و در
                // حافظه‌ی اختصاصی اپ کپی شده)، نه یک asset داخل بسته‌ی نصب
                mp.setDataSource(audioUrl)
            } else {
                val afd = context.assets.openFd(audioUrl)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }
            mp.setOnPreparedListener {
                onStatus("started")
                it.start()
            }
            mp.setOnCompletionListener {
                onStatus("done")
            }
            mp.setOnErrorListener { _, _, _ ->
                onStatus("error")
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (e: Exception) {
            onStatus("error")
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {
            // نادیده گرفته می‌شود؛ پخش‌کننده ممکن است در وضعیت نامعتبر بوده باشد
        }
        player = null
    }

    fun isPlaying(): Boolean = try { player?.isPlaying == true } catch (e: Exception) { false }

    /** پیشرفت پخش بین صفر و یک؛ برای همگام‌سازی نمای متن با صوت. */
    fun progress(): Float {
        return try {
            val p = player ?: return 0f
            val duration = p.duration
            if (duration <= 0) 0f else (p.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }
    }
}
