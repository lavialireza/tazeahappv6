package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Sidecar metadata for shared-sync timestamps without requiring a Room migration. */
object SyncMetaStore {
    private const val PREFS = "tazieh_sync_meta_v3"
    private const val KEY = "records"
    private fun key(collection:String, uid:String)=collection+"|"+uid
    private fun stable(o:JSONObject):JSONObject { val out=JSONObject(); val it=o.keys().asSequence().toList().sorted(); for(k in it) if(k!="createdAt"&&k!="updatedAt") out.put(k,o.get(k)); return out }
    private fun hash(o:JSONObject):String { val md=MessageDigest.getInstance("SHA-256"); val b=md.digest(stable(o).toString().toByteArray(Charsets.UTF_8)); return b.joinToString(""){String.format("%02x",it)} }
    private fun all(context:Context):JSONObject=runCatching{JSONObject(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"{}")?:"{}")} .getOrDefault(JSONObject())
    private fun save(context:Context,o:JSONObject){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,o.toString()).apply()}
    fun local(context:Context, collection:String, uid:String, obj:JSONObject, createdAt:String):String { val m=all(context); val k=key(collection,uid); val h=hash(obj); val old=m.optJSONObject(k); val now=System.currentTimeMillis(); val updated=if(old!=null&&old.optString("hash")!=h) iso(now) else old?.optString("updatedAt").takeUnless{it.isNullOrBlank()}?:createdAt.ifBlank{iso(now)}; m.put(k,JSONObject().apply{put("hash",h);put("updatedAt",updated);put("createdAt",createdAt.ifBlank{updated})}); save(context,m); return updated }
    fun captureRemoteState(context:Context, root:JSONObject){ val keys=listOf("fields","taziehs","roles","sections"); for(c in keys){val a=root.optJSONArray(c)?:continue;for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;val u=o.optString("uid");if(u.isNotBlank())baseline(context,c,u,o,o.optString("updatedAt"),o.optString("createdAt"))}} }
    fun captureRemoteTreeState(context: Context, tree: JSONArray) {
        fun walk(items: JSONArray, collection: String) {
            for (i in 0 until items.length()) {
                val o = items.optJSONObject(i) ?: continue
                val uid = o.optString("uid")
                if (uid.isNotBlank()) baseline(context, collection, uid, o, o.optString("updatedAt"), o.optString("createdAt"))
                when (collection) {
                    "fields" -> walk(o.optJSONArray("taziehs") ?: JSONArray(), "taziehs")
                    "taziehs" -> walk(o.optJSONArray("roles") ?: JSONArray(), "roles")
                    "roles" -> walk(o.optJSONArray("sections") ?: JSONArray(), "sections")
                }
            }
        }
        walk(tree, "fields")
    }

    fun shouldApplyRemote(context:Context, collection:String, uid:String, remoteUpdatedAt:String):Boolean {
        if (uid.isBlank()) return true
        val local = all(context).optJSONObject(key(collection,uid))?.optString("updatedAt").orEmpty()
        if (local.isBlank() || remoteUpdatedAt.isBlank()) return true
        return timeValue(remoteUpdatedAt) >= timeValue(local)
    }
    private fun timeValue(value:String):Long {
        value.toLongOrNull()?.let { return it }
        return runCatching { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",java.util.Locale.US).parse(value)?.time ?: 0L }.getOrDefault(0L)
    }
    fun baseline(context:Context, collection:String, uid:String, obj:JSONObject, updatedAt:String, createdAt:String){val m=all(context);m.put(key(collection,uid),JSONObject().apply{put("hash",hash(obj));put("updatedAt",updatedAt.ifBlank{createdAt});put("createdAt",createdAt)});save(context,m)}
    private fun iso(ms:Long)=java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",java.util.Locale.US).format(java.util.Date(ms))
}
