package com.example.bookapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** ثبت سوابق محلی تغییرات مدیریت کاربران خاص؛ فقط در نسخه Admin. */
object AccessAuditLog {
    data class Entry(
        val timestamp: Long,
        val action: String,
        val installationId: String,
        val displayName: String,
        val details: String
    )

    private const val PREFS = "viewer_access_audit"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 500

    fun record(context: Context, action: String, user: ViewerAccessPolicy.SpecialUser, details: String = "") {
        val id = user.installationId.trim().uppercase(Locale.US)
        val entry = Entry(System.currentTimeMillis(), action, id, user.displayName.trim(), details.trim())
        val all = getAll(context).toMutableList()
        all.add(0, entry)
        save(context, all.take(MAX_ENTRIES))
    }

    fun record(context: Context, action: String, installationId: String, displayName: String = "", details: String = "") {
        val fake = ViewerAccessPolicy.SpecialUser(
            installationId = installationId,
            profile = ViewerAccessPolicy.PROFILE_CUSTOM,
            expiresAt = null,
            permissions = emptyMap(),
            displayName = displayName
        )
        record(context, action, fake, details)
    }

    fun getAll(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(o.optLong("timestamp"), o.optString("action"), o.optString("installationId"), o.optString("displayName"), o.optString("details"))
            }
        }.getOrElse { emptyList() }
    }

    fun describeChanges(oldUser: ViewerAccessPolicy.SpecialUser?, newUser: ViewerAccessPolicy.SpecialUser): String {
        if (oldUser == null) return "کاربر جدید ایجاد شد."
        val changes = mutableListOf<String>()
        if (oldUser.enabled != newUser.enabled) changes += if (newUser.enabled) "کاربر فعال شد" else "کاربر غیرفعال شد"
        if (oldUser.profile != newUser.profile) changes += "پروفایل از ${oldUser.profile} به ${newUser.profile} تغییر کرد"
        if (oldUser.expiresAt != newUser.expiresAt) changes += "تاریخ انقضا تغییر کرد"
        if (oldUser.displayName != newUser.displayName) changes += "نام کاربر تغییر کرد"
        val labels = ViewerAccessPolicy.permissionLabels
        labels.forEach { (key, label) ->
            val before = oldUser.permissions[key] == true
            val after = newUser.permissions[key] == true
            if (before != after) changes += "$label ${if (after) "فعال شد" else "غیرفعال شد"}"
        }
        return if (changes.isEmpty()) "تغییر مؤثری در اطلاعات یا مجوزهای کاربر ثبت نشد." else changes.joinToString("؛ ")
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit()
    }

    private fun save(context: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().put("timestamp", e.timestamp).put("action", e.action)
                .put("installationId", e.installationId).put("displayName", e.displayName).put("details", e.details))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).commit()
    }
}
