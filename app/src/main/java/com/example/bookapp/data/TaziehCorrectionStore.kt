package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** اصلاحات تخصصی متن تعزیه؛ مستقل از دیکشنری اصطلاحات عمومی. */
object TaziehCorrectionStore {
    data class Correction(
        val id: Long,
        val original: String,
        val corrected: String,
        val explanation: String = ""
    )

    private const val PREFS = "tazieh_corrections"
    private const val KEY_ITEMS = "items"

    fun get(context: Context): List<Correction> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optLong("id", i.toLong() + 1L)
                val original = o.optString("original").trim()
                val corrected = o.optString("corrected").trim()
                if (original.isBlank() || corrected.isBlank()) null
                else Correction(id, original, corrected, o.optString("explanation").trim())
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, original: String, corrected: String, explanation: String): Boolean {
        ViewerContentWriteGuard.check()
        val o = original.trim(); val c = corrected.trim(); val e = explanation.trim()
        if (o.isBlank() || c.isBlank()) return false
        val current = get(context).toMutableList()
        if (current.any { normalize(it.original) == normalize(o) }) return false
        current.add(Correction(System.currentTimeMillis(), o, c, e))
        return save(context, current)
    }

    fun update(context: Context, item: Correction, original: String, corrected: String, explanation: String): Boolean {
        ViewerContentWriteGuard.check()
        val o = original.trim(); val c = corrected.trim(); val e = explanation.trim()
        if (o.isBlank() || c.isBlank()) return false
        val current = get(context).toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index < 0) return false
        if (current.any { it.id != item.id && normalize(it.original) == normalize(o) }) return false
        current[index] = item.copy(original = o, corrected = c, explanation = e)
        return save(context, current)
    }

    fun delete(context: Context, id: Long): Boolean {
        ViewerContentWriteGuard.check()
        return save(context, get(context).filterNot { it.id == id })
    }

    fun apply(originalText: String, corrections: List<Correction>): String {
        var result = originalText
        corrections.forEach { c -> if (c.original.isNotBlank()) result = result.replace(c.original, c.corrected) }
        return result
    }

    private fun save(context: Context, items: List<Correction>): Boolean {
        val arr = JSONArray()
        items.forEach { c ->
            arr.put(JSONObject().put("id", c.id).put("original", c.original).put("corrected", c.corrected).put("explanation", c.explanation))
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, arr.toString()).commit()
    }

    private fun normalize(value: String): String = value.trim().replace('ي', 'ی').replace('ك', 'ک').replace(Regex("\\s+"), " ")
}
