package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable JSON used by the companion content program.
 * The external contract intentionally contains only title/taziehs/roles/sections/content.
 */
data class ContentImportPreview(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val fields: Int = 0,
    val taziehs: Int = 0,
    val roles: Int = 0,
    val sections: Int = 0,
    val existingItems: Int = 0,
    val newItems: Int = 0
)

private val FIELD_KEYS = setOf("title", "taziehs")
private val TAZIEH_KEYS = setOf("title", "roles")
private val ROLE_KEYS = setOf("title", "sections")
private val SECTION_KEYS = setOf("title", "content")

/** Validate the exact external schema used by the companion program. */
fun validateExternalContentJson(text: String): List<String> {
    val errors = mutableListOf<String>()
    val root = try { JSONArray(text) } catch (e: Exception) {
        return listOf("ریشه فایل باید یک آرایه JSON باشد")
    }

    val fieldTitles = mutableSetOf<String>()
    for (fi in 0 until root.length()) {
        val f = root.optJSONObject(fi)
        if (f == null) { errors += "زمینه شماره ${fi + 1} یک شیء JSON معتبر نیست"; continue }
        checkKeys(f, FIELD_KEYS, "زمینه ${fi + 1}", errors)
        val fieldTitle = f.optString("title", "").trim()
        if (fieldTitle.isBlank()) errors += "زمینه ${fi + 1} عنوان ندارد"
        else if (!fieldTitles.add(fieldTitle)) errors += "زمینه تکراری است: $fieldTitle"

        val taziehs = f.optJSONArray("taziehs")
        if (taziehs == null) { errors += "زمینه «$fieldTitle» آرایه taziehs ندارد"; continue }
        val taziehTitles = mutableSetOf<String>()
        for (ti in 0 until taziehs.length()) {
            val t = taziehs.optJSONObject(ti)
            if (t == null) { errors += "تعزیه ${fi + 1}/${ti + 1} معتبر نیست"; continue }
            checkKeys(t, TAZIEH_KEYS, "تعزیه «${t.optString("title")}" , errors)
            val title = t.optString("title", "").trim()
            if (title.isBlank()) errors += "تعزیه ${fi + 1}/${ti + 1} عنوان ندارد"
            else if (!taziehTitles.add(title)) errors += "تعزیه تکراری در زمینه «$fieldTitle»: $title"

            val roles = t.optJSONArray("roles")
            if (roles == null) { errors += "تعزیه «$title» آرایه roles ندارد"; continue }
            val roleTitles = mutableSetOf<String>()
            for (ri in 0 until roles.length()) {
                val r = roles.optJSONObject(ri)
                if (r == null) { errors += "نقش ${fi + 1}/${ti + 1}/${ri + 1} معتبر نیست"; continue }
                checkKeys(r, ROLE_KEYS, "نقش «${r.optString("title")}" , errors)
                val roleTitle = r.optString("title", "").trim()
                if (roleTitle.isBlank()) errors += "نقش ${fi + 1}/${ti + 1}/${ri + 1} عنوان ندارد"
                else if (!roleTitles.add(roleTitle)) errors += "نقش تکراری در تعزیه «$title»: $roleTitle"

                val sections = r.optJSONArray("sections")
                if (sections == null) { errors += "نقش «$roleTitle» آرایه sections ندارد"; continue }
                val sectionTitles = mutableSetOf<String>()
                for (si in 0 until sections.length()) {
                    val s = sections.optJSONObject(si)
                    if (s == null) { errors += "بخش ${fi + 1}/${ti + 1}/${ri + 1}/${si + 1} معتبر نیست"; continue }
                    checkKeys(s, SECTION_KEYS, "بخش «${s.optString("title")}" , errors)
                    val sectionTitle = s.optString("title", "").trim()
                    if (sectionTitle.isBlank()) errors += "بخش ${fi + 1}/${ti + 1}/${ri + 1}/${si + 1} عنوان ندارد"
                    else if (!sectionTitles.add(sectionTitle)) errors += "بخش تکراری در نقش «$roleTitle»: $sectionTitle"
                    if (!s.has("content") || s.optString("content", "").isEmpty()) errors += "بخش «$sectionTitle» فیلد content معتبر ندارد"
                }
            }
        }
    }
    return errors
}

private fun checkKeys(obj: JSONObject, allowed: Set<String>, label: String, errors: MutableList<String>) {
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (key !in allowed) errors += "$label شامل فیلد اضافی «$key» است"
    }
}

/** Export exactly the companion-program schema. No UID/internal metadata is written. */
suspend fun exportContentJson(db: AppDatabase): String {
    val root = JSONArray()
    db.fieldDao().getAll().forEach { field ->
        root.put(JSONObject().apply {
            put("title", field.title)
            put("taziehs", JSONArray().also { taziehArray ->
                db.taziehDao().getByField(field.id).forEach { tazieh ->
                    taziehArray.put(JSONObject().apply {
                        put("title", tazieh.title)
                        put("roles", JSONArray().also { roleArray ->
                            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                                roleArray.put(JSONObject().apply {
                                    put("title", role.title)
                                    put("sections", JSONArray().also { sectionArray ->
                                        db.sectionDao().getByRole(role.id).forEach { section ->
                                            sectionArray.put(JSONObject().apply {
                                                put("title", section.title)
                                                put("content", section.content)
                                            })
                                        }
                                    })
                                })
                            }
                        })
                    })
                }
            })
        })
    }
    return root.toString(2)
}

suspend fun previewContentImport(db: AppDatabase, text: String): ContentImportPreview {
    val errors = validateExternalContentJson(text)
    if (errors.isNotEmpty()) return ContentImportPreview(false, errors)

    val root = JSONArray(text)
    var fields = 0; var taziehs = 0; var roles = 0; var sections = 0; var existing = 0
    for (fi in 0 until root.length()) {
        val f = root.getJSONObject(fi); fields++
        val field = db.fieldDao().getByTitle(f.getString("title"))
        if (field != null) existing++
        val ta = f.getJSONArray("taziehs")
        for (ti in 0 until ta.length()) {
            val t = ta.getJSONObject(ti); taziehs++
            val tazieh = field?.let { db.taziehDao().getByTitle(it.id, t.getString("title")) }
            if (tazieh != null) existing++
            val rs = t.getJSONArray("roles")
            for (ri in 0 until rs.length()) {
                val r = rs.getJSONObject(ri); roles++
                val role = tazieh?.let { db.roleDao().getByTitle(it.id, r.getString("title")) }
                if (role != null) existing++
                val ss = r.getJSONArray("sections")
                for (si in 0 until ss.length()) {
                    val s = ss.getJSONObject(si); sections++
                    val section = role?.let { db.sectionDao().getByTitle(it.id, s.getString("title")) }
                    if (section != null) existing++
                }
            }
        }
    }
    val total = fields + taziehs + roles + sections
    return ContentImportPreview(true, fields = fields, taziehs = taziehs, roles = roles, sections = sections, existingItems = existing, newItems = total - existing)
}

suspend fun importContentJson(db: AppDatabase, text: String): ContentImportPreview {
    val preview = previewContentImport(db, text)
    if (!preview.valid) return preview
    // Use the content hash as source ownership. This keeps imported section cleanup isolated.
    mergeContentFromJson(db, text, ContentUid.source(text))
    return preview
}

fun readJsonFromUri(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: throw IllegalArgumentException("امکان خواندن فایل وجود ندارد")
