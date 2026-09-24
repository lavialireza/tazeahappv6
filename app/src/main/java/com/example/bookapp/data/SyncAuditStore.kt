package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SyncAuditStore {
    private const val PREFS="tazieh_sync_audit"; private const val KEY="events"
    fun add(context: Context, event: String, details: Map<String,String> = emptyMap()) {
        val old=runCatching{JSONArray(context.getSharedPreferences(PREFS,0).getString(KEY,"[]"))}.getOrElse{JSONArray()}
        val next=JSONArray(); next.put(JSONObject().apply { put("at",System.currentTimeMillis());put("event",event); details.forEach{(k,v)->put(k,v)} })
        for(i in 0 until old.length()) if(i >= maxOf(0,old.length()-99)) next.put(old.get(i))
        context.getSharedPreferences(PREFS,0).edit().putString(KEY,next.toString()).apply()
    }
    fun all(context: Context): JSONArray = runCatching{JSONArray(context.getSharedPreferences(PREFS,0).getString(KEY,"[]"))}.getOrElse{JSONArray()}
}
