package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** نگهداری Tombstoneهای Sync برای حذف دوطرفه بدون از دست رفتن تاریخچه حذف. */
object TombstoneStore {
    private const val PREFS="tazieh_sync_tombstones"
    private const val KEY="items"
    private fun now():String=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",Locale.US).format(Date())
    fun add(context:Context, collection:String, uid:String){
        if(collection.isBlank()||uid.isBlank()) return
        val arr=get(context); val out=JSONArray(); var replaced=false
        for(i in 0 until arr.length()){ val o=arr.optJSONObject(i) ?: continue; if(o.optString("collection")==collection&&o.optString("uid")==uid){ if(!replaced){out.put(JSONObject().apply{put("uid",uid);put("collection",collection);put("deleted",true);put("createdAt",o.optString("createdAt",now()));put("updatedAt",now())}); replaced=true } } else out.put(o) }
        if(!replaced) out.put(JSONObject().apply{put("uid",uid);put("collection",collection);put("deleted",true);put("createdAt",now());put("updatedAt",now())})
        save(context,out)
    }
    fun get(context:Context):JSONArray=runCatching{JSONArray(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]")?:"[]")}.getOrDefault(JSONArray())
    private fun save(context:Context,a:JSONArray){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply()}
    fun applyRemote(context:Context, remote:JSONArray){
        val local=get(context); val seen=HashMap<String,JSONObject>()
        for(i in 0 until local.length()){val o=local.optJSONObject(i)?:continue;seen[o.optString("collection")+"|"+o.optString("uid")]=o}
        for(i in 0 until remote.length()){val o=remote.optJSONObject(i)?:continue;val k=o.optString("collection")+"|"+o.optString("uid");val old=seen[k];if(old==null||o.optString("updatedAt")>=old.optString("updatedAt"))seen[k]=o}
        val out=JSONArray();seen.values.forEach{out.put(it)};save(context,out)
    }
    fun all(context:Context)=get(context)
}
