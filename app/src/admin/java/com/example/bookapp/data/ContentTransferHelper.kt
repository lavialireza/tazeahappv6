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
    val newItems: Int = 0,
    val changedSections: Int = 0
)

// UID is part of the portable schema. Timestamps and author metadata are optional.
private val FIELD_KEYS = setOf("title", "uid", "createdAt", "updatedAt", "taziehs")
private val TAZIEH_KEYS = setOf("title", "uid", "createdAt", "updatedAt", "author", "authorEmail", "roles")
private val ROLE_KEYS = setOf("title", "uid", "createdAt", "updatedAt", "sections")
private val SECTION_KEYS = setOf("title", "uid", "createdAt", "updatedAt", "content", "audio")

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

/** Export the portable content schema, including stable IDs shared with the companion converter. */
suspend fun exportContentJson(db: AppDatabase): String {
    val root = JSONArray()
    db.fieldDao().getAll().forEach { field ->
        root.put(JSONObject().apply {
            put("title", field.title)
            put("uid", field.uid)
            put("taziehs", JSONArray().also { taziehArray ->
                db.taziehDao().getByField(field.id).forEach { tazieh ->
                    taziehArray.put(JSONObject().apply {
                        put("title", tazieh.title)
                        put("uid", tazieh.uid)
                        put("roles", JSONArray().also { roleArray ->
                            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                                roleArray.put(JSONObject().apply {
                                    put("title", role.title)
                                    put("uid", role.uid)
                                    put("sections", JSONArray().also { sectionArray ->
                                        db.sectionDao().getByRole(role.id).forEach { section ->
                                            sectionArray.put(JSONObject().apply {
                                                put("title", section.title)
                                                put("uid", section.uid)
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
    val errors = validateExternalContentJson(text).toMutableList()
    if (errors.isNotEmpty()) return ContentImportPreview(false, errors)

    val root = ensureStableContentUids(JSONArray(text))
    var fields = 0; var taziehs = 0; var roles = 0; var sections = 0; var existing = 0; var changedSections = 0
    for (fi in 0 until root.length()) {
        val f = root.getJSONObject(fi); fields++
        val fieldUid = f.getString("uid")
        val fieldByTitle = db.fieldDao().getByTitle(f.getString("title"))
        val fieldByUid = db.fieldDao().getByUid(fieldUid)
        if (fieldByTitle != null && fieldByTitle.uid != fieldUid) {
            errors += "زمینه «${f.getString("title")}» تکراری است اما شناسه آن با زمینه موجود متفاوت است؛ برای جلوگیری از ساخت زمینه تکراری، ابتدا شناسه را تطبیق دهید."
        }
        val field = fieldByUid ?: fieldByTitle
        if (field != null) existing++
        val ta = f.getJSONArray("taziehs")
        for (ti in 0 until ta.length()) {
            val t = ta.getJSONObject(ti); taziehs++
            val tUid = t.getString("uid")
            val tByTitle = field?.let { db.taziehDao().getByTitle(it.id, t.getString("title")) }
            val tByUid = db.taziehDao().getByUid(tUid)
            if (tByTitle != null && tByTitle.uid != tUid) {
                errors += "مجلس «${t.getString("title")}» در زمینه «${f.getString("title")}» تکراری است اما شناسه آن متفاوت است؛ ورود متوقف شد تا مجلس ناخواسته تکراری نشود."
            }
            val tazieh = tByUid ?: tByTitle
            if (tazieh != null) existing++
            val rs = t.getJSONArray("roles")
            for (ri in 0 until rs.length()) {
                val r = rs.getJSONObject(ri); roles++
                val rUid = r.getString("uid")
                val roleByTitle = tazieh?.let { db.roleDao().getByTitle(it.id, r.getString("title")) }
                val roleByUid = db.roleDao().getByUid(rUid)
                if (roleByTitle != null && roleByTitle.uid != rUid) {
                    errors += "نقش «${r.getString("title")}» در مجلس «${t.getString("title")}» شناسه متفاوت دارد؛ ورود متوقف شد."
                }
                val role = roleByUid ?: roleByTitle
                if (role != null) existing++
                val ss = r.getJSONArray("sections")
                for (si in 0 until ss.length()) {
                    val sectionJson = ss.getJSONObject(si); sections++
                    val sUid = sectionJson.getString("uid")
                    val sectionByTitle = role?.let { db.sectionDao().getByTitle(it.id, sectionJson.getString("title")) }
                    val sectionByUid = db.sectionDao().getByUid(sUid)
                    if (sectionByTitle != null && sectionByTitle.uid != sUid) {
                        errors += "بخش «${sectionJson.getString("title")}» در نقش «${r.getString("title")}» شناسه متفاوت دارد؛ ورود متوقف شد."
                    }
                    val currentSection = sectionByUid ?: sectionByTitle
                    if (currentSection != null) {
                        existing++
                        if (currentSection.content != sectionJson.getString("content")) changedSections++
                    }
                }
            }
        }
    }
    val total = fields + taziehs + roles + sections
    return ContentImportPreview(errors.isEmpty(), errors, fields, taziehs, roles, sections, existing, total - existing, changedSections)
}

suspend fun importContentJson(db: AppDatabase, text: String): ContentImportPreview {
    val preview = previewContentImport(db, text)
    if (!preview.valid) return preview

    // One majlis is one independently-owned source. Editing its text therefore does
    // not change its source identity and cannot disturb another majlis/file.
    val canonical = ensureStableContentUids(JSONArray(text))
    for (fi in 0 until canonical.length()) {
        val field = canonical.getJSONObject(fi)
        val fieldUid = field.getString("uid")
        val majlisList = field.getJSONArray("taziehs")
        for (ti in 0 until majlisList.length()) {
            val majlis = majlisList.getJSONObject(ti)
            val oneMajlis = JSONArray().put(JSONObject().apply {
                put("title", field.getString("title"))
                put("uid", fieldUid)
                put("taziehs", JSONArray().put(majlis))
            })
            val majlisUid = majlis.getString("uid")
            mergeContentFromJson(db, oneMajlis.toString(), ContentUid.source("majlis:$majlisUid"))
        }
    }
    return preview
}

/** Adds deterministic IDs where a source (for example DOCX) did not provide them. */
fun ensureStableContentUids(input: JSONArray): JSONArray {
    val result = JSONArray()
    for (fi in 0 until input.length()) {
        val field = input.getJSONObject(fi)
        val fieldUid = field.optString("uid").ifBlank {
            ContentUid.derived("field", "root", field.getString("title"))
        }
        val outField = JSONObject().put("title", field.getString("title")).put("uid", fieldUid)
        copyOptionalMetadata(field, outField)
        val taziehOut = JSONArray()
        val taziehs = field.getJSONArray("taziehs")
        for (ti in 0 until taziehs.length()) {
            val tazieh = taziehs.getJSONObject(ti)
            val taziehUid = tazieh.optString("uid").ifBlank {
                ContentUid.derived("tazieh", fieldUid, tazieh.getString("title"))
            }
            val outTazieh = JSONObject().put("title", tazieh.getString("title")).put("uid", taziehUid)
            copyOptionalMetadata(tazieh, outTazieh)
            val roleOut = JSONArray()
            val roles = tazieh.getJSONArray("roles")
            for (ri in 0 until roles.length()) {
                val role = roles.getJSONObject(ri)
                val roleUid = role.optString("uid").ifBlank {
                    ContentUid.derived("role", taziehUid, role.getString("title"))
                }
                val outRole = JSONObject().put("title", role.getString("title")).put("uid", roleUid)
                copyOptionalMetadata(role, outRole)
                val sectionOut = JSONArray()
                val sections = role.getJSONArray("sections")
                for (si in 0 until sections.length()) {
                    val section = sections.getJSONObject(si)
                    val sectionUid = section.optString("uid").ifBlank {
                        ContentUid.derivedWithIndex("section", roleUid, section.getString("title"), si)
                    }
                    val outSection = JSONObject()
                        .put("title", section.getString("title"))
                        .put("content", section.getString("content"))
                        .put("uid", sectionUid)
                    copyOptionalMetadata(section, outSection)
                    sectionOut.put(outSection)
                }
                outRole.put("sections", sectionOut)
                roleOut.put(outRole)
            }
            outTazieh.put("roles", roleOut)
            taziehOut.put(outTazieh)
        }
        outField.put("taziehs", taziehOut)
        result.put(outField)
    }
    return result
}

private fun copyOptionalMetadata(source: JSONObject, target: JSONObject) {
    listOf("createdAt", "updatedAt", "author", "authorEmail", "audio").forEach { key ->
        if (source.has(key)) target.put(key, source.get(key))
    }
}

fun readJsonFromUri(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: throw IllegalArgumentException("امکان خواندن فایل وجود ندارد")
