package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persistent offline queue for full-state sync requests. */
object OfflineSyncQueue {
    private const val PREFS = "tazieh_sync_queue"
    private const val KEY = "items"
    data class Item(val requestId: String, val deviceId: String, val body: String, val createdAt: Long, val attempts: Int)

    @Synchronized fun enqueue(context: Context, deviceId: String, body: String, requestId: String = UUID.randomUUID().toString()) {
        val a = read(context)
        if (a.any { it.requestId == requestId }) return
        a.add(Item(requestId, deviceId, body, System.currentTimeMillis(), 0))
        write(context, a.takeLast(20))
    }
    @Synchronized fun peek(context: Context): Item? = read(context).firstOrNull()
    @Synchronized fun remove(context: Context, requestId: String) { write(context, read(context).filterNot { it.requestId == requestId }) }
    @Synchronized fun markAttempt(context: Context, requestId: String) {
        write(context, read(context).map { if (it.requestId == requestId) it.copy(attempts = it.attempts + 1) else it })
    }
    fun size(context: Context): Int = read(context).size
    private fun read(context: Context): MutableList<Item> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val a = JSONArray(raw); val out = mutableListOf<Item>()
        for (i in 0 until a.length()) { val o=a.optJSONObject(i) ?: continue; out.add(Item(o.optString("requestId"),o.optString("deviceId"),o.optString("body"),o.optLong("createdAt"),o.optInt("attempts"))) }
        return out
    }
    private fun write(context: Context, items: List<Item>) {
        val a=JSONArray(); items.forEach { a.put(JSONObject().apply { put("requestId",it.requestId);put("deviceId",it.deviceId);put("body",it.body);put("createdAt",it.createdAt);put("attempts",it.attempts) }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply()
    }
}
