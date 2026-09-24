package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent user-added dictionary entries. Built-in entries remain in GLOSSARY_TERMS. */
object GlossaryStore {
    private const val PREFS = "tazieh_glossary"
    private const val KEY_TERMS = "custom_terms"

    fun get(context: Context): List<Pair<String, String>> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TERMS, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val term = o.optString("term").trim()
                val explanation = o.optString("explanation").trim()
                if (term.isBlank() || explanation.isBlank()) null else term to explanation
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, term: String): Boolean {
        val normalized = normalize(term)
        return get(context).any { normalize(it.first) == normalized }
    }

    fun add(context: Context, term: String, explanation: String): Boolean {
        ViewerContentWriteGuard.check()
        val cleanTerm = term.trim()
        val cleanExplanation = explanation.trim()
        if (cleanTerm.isBlank() || cleanExplanation.isBlank()) return false
        val current = get(context).toMutableList()
        if (current.any { normalize(it.first) == normalize(cleanTerm) }) return false
        current.add(cleanTerm to cleanExplanation)
        val arr = JSONArray()
        current.forEach { (t, e) -> arr.put(JSONObject().put("term", t).put("explanation", e)) }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TERMS, arr.toString()).commit()
    }

    private fun normalize(value: String): String = value.trim()
        .replace('ي', 'ی').replace('ك', 'ک').replace('ۀ', 'ه').replace('ة', 'ه')
        .replace(Regex("\\s+"), " ")
}
