package com.example.bookapp.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * پوششی ساده روی TextToSpeech اندروید برای خواندن متن اشعار با صدای مصنوعی.
 * onStatus با مقادیر "ready" / "no_engine" / "no_persian_voice" / "started" /
 * "done" / "error" فراخوانی می‌شود تا رابط کاربری بتواند وضعیت را نشان دهد.
 */
class SpeechHelper(context: Context, private val onStatus: (String) -> Unit = {}) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var persianAvailable = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val result = tts?.setLanguage(Locale("fa", "IR"))
                persianAvailable = result == TextToSpeech.LANG_AVAILABLE ||
                        result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                        result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                if (!persianAvailable) {
                    // زبان فارسی نصب نیست؛ تلاش با زبان پیش‌فرض دستگاه (ممکن است باز هم کار نکند)
                    tts?.setLanguage(Locale.getDefault())
                    onStatus("no_persian_voice")
                } else {
                    onStatus("ready")
                }
            } else {
                onStatus("no_engine")
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onStatus("started") }
            override fun onDone(utteranceId: String?) { onStatus("done") }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onStatus("error") }
        })
    }

    fun speak(text: String) {
        if (!ready) {
            onStatus("no_engine")
            return
        }
        val params = android.os.Bundle()
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tazieh_tts")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
