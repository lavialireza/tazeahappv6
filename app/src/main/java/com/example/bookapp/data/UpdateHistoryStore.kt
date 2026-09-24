package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object UpdateHistoryStore {
    private const val PREFS = "tazieh_update_history"
    private const val KEY = "items"
    data class Entry(val at: Long, val fromVersion: Int, val toVersion: Int, val success: Boolean, val message: String)

    fun record(context: Context, from: Int, to: Int, success: Boolean, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        val out = JSONArray()
        out.put(JSONObject().put("at", System.currentTimeMillis()).put("from", from).put("to", to).put("success", success).put("message", message))
        for (i in 0 until minOf(old.length(), 19)) out.put(old.getJSONObject(i))
        prefs.edit().putString(KEY, out.toString()).apply()
    }

    fun get(context: Context): List<Entry> {
        val a = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]")
        return (0 until a.length()).mapNotNull { i ->
            runCatching {
                val o = a.getJSONObject(i)
                Entry(o.optLong("at"), o.optInt("from"), o.optInt("to"), o.optBoolean("success"), o.optString("message"))
            }.getOrNull()
        }
    }
}
