package com.example.bookapp.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/**
 * Version 2 portable backup. Relationships are stored by stable UID rather than SQLite IDs,
 * so the backup can be restored into a database whose auto-generated IDs are different.
 */
suspend fun buildBackupJson(context: Context, db: AppDatabase): String {
    // Keep the Room copy current while legacy UI code still writes SharedPreferences.
    migratePrefsUserDataToRoom(context, db)
    return db.withTransaction { buildBackupJsonInTransaction(context, db) }
}

private suspend fun buildBackupJsonInTransaction(context: Context, db: AppDatabase): String {
    val root = JSONObject().apply {
        put("app", "taziehapp")
        put("backupVersion", 3)
        put("schemaVersion", 10)
        put("createdAt", System.currentTimeMillis())
    }

    root.put("notes", JSONArray().apply {
        db.noteDao().getAll().forEach { note ->
            put(JSONObject().apply {
                put("uid", note.uid)
                put("title", note.title)
                put("content", note.content)
                note.sectionUid?.let { put("sectionUid", it) }
                put("createdAt", note.createdAt)
            })
        }
    })

    val sectionsById = mutableMapOf<Long, SectionEntity>()
    val taziehsById = mutableMapOf<Long, TaziehEntity>()
    val rolesById = mutableMapOf<Long, RoleEntity>()
    db.fieldDao().getAll().forEach { field ->
        db.taziehDao().getByField(field.id).forEach { tazieh ->
            taziehsById[tazieh.id] = tazieh
            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                rolesById[role.id] = role
                db.sectionDao().getByRole(role.id).forEach { section -> sectionsById[section.id] = section }
            }
        }
    }

    root.put("bookmarks", JSONArray().apply {
        db.userDataDao().getBookmarks().map { it.sectionUid }.forEach(::put)
    })

    root.put("recent", JSONArray().apply {
        Prefs.getRecent(context).mapNotNull { sectionsById[it]?.uid }.forEach(::put)
    })

    root.put("readSections", JSONArray().apply {
        db.userDataDao().getReadingHistory().map { it.sectionUid }.forEach(::put)
    })

    root.put("activeDays", JSONArray().apply { db.userDataDao().getActiveDays().map { it.dayKey }.forEach(::put) })

    root.put("tags", JSONArray().apply {
        db.userDataDao().getTags().forEach { tag ->
            put(JSONObject().apply { put("sectionUid", tag.sectionUid); put("tag", tag.tag) })
        }
    })

    root.put("footnotes", JSONArray().apply {
        sectionsById.values.forEach { section ->
            db.footnoteDao().getBySection(section.id).forEach { fn ->
                put(JSONObject().apply {
                    put("uid", fn.uid)
                    put("sectionUid", section.uid)
                    put("term", fn.term)
                    put("explanation", fn.explanation)
                })
            }
        }
    })

    root.put("myRoles", JSONArray().apply {
        db.userDataDao().getMyRoles().forEach { role ->
            put(JSONObject().apply {
                put("taziehUid", role.taziehUid)
                put("roleUid", role.roleUid)
            })
        }
    })

    root.put("dialogues", JSONArray().apply {
        taziehsById.values.forEach { tazieh ->
            db.dialogueDao().getByTazieh(tazieh.id).forEach { dialogue ->
                val turns = JSONArray().apply {
                    db.dialogueTurnDao().getByDialogue(dialogue.id).forEach { turn ->
                        sectionsById[turn.sectionId]?.let { section ->
                            put(JSONObject().apply {
                                put("uid", turn.uid)
                                put("sectionUid", section.uid)
                                put("orderIndex", turn.orderIndex)
                            })
                        }
                    }
                }
                put(JSONObject().apply {
                    put("uid", dialogue.uid)
                    put("taziehUid", tazieh.uid)
                    put("title", dialogue.title)
                    put("turns", turns)
                })
            }
        }
    })

    return root.toString(2)
}

suspend fun writeBackupToUri(context: Context, db: AppDatabase, uri: Uri, password: String? = null) {
    val json = buildBackupJson(context, db)
    val bytes = if (password.isNullOrBlank()) json.toByteArray(Charsets.UTF_8) else encryptBackupText(json, password)
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        ?: throw IllegalStateException("فایل پشتیبان قابل نوشتن نیست")
}

suspend fun restoreBackupFromUri(context: Context, db: AppDatabase, uri: Uri, password: String? = null): Result<Unit> {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(IllegalStateException("فایل خوانده نشد"))
        val text = if (password.isNullOrBlank()) String(bytes, Charsets.UTF_8) else decryptBackupBytes(bytes, password)
        val root = JSONObject(text)
        val version = root.optInt("backupVersion", 1)
        if (version !in 1..3) return Result.failure(IllegalArgumentException("نسخه پشتیبان پشتیبانی نمی‌شود: $version"))

        db.withTransaction {
            restoreNotes(db, root.optJSONArray("notes") ?: JSONArray())
            restoreBookmarks(context, db, root.optJSONArray("bookmarks") ?: JSONArray(), version)
            restoreRecent(context, db, root.optJSONArray("recent") ?: JSONArray(), version)
            restoreReadSections(context, db, root.optJSONArray("readSections") ?: JSONArray(), version)
            restoreActiveDays(context, db, root.optJSONArray("activeDays") ?: JSONArray())
            restoreTags(context, db, root.optJSONArray("tags") ?: JSONArray(), version)
            restoreFootnotes(db, root.optJSONArray("footnotes") ?: JSONArray(), version)
            restoreMyRoles(context, db, root.optJSONArray("myRoles") ?: JSONArray(), version)
            restoreDialogues(db, root.optJSONArray("dialogues") ?: JSONArray(), version)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun restoreNotes(db: AppDatabase, arr: JSONArray) {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val uid = o.optString("uid").ifBlank { ContentUid.sha256("legacy-note|${o.optString("title")} | ${o.optString("content")} | ${o.optLong("createdAt", 0L)}") }
        val existing = db.noteDao().getByUid(uid)
        val note = NoteEntity(
            id = existing?.id ?: 0,
            uid = uid,
            title = o.getString("title"),
            content = o.getString("content"),
            sectionUid = o.optString("sectionUid").ifBlank { existing?.sectionUid },
            createdAt = o.optLong("createdAt", existing?.createdAt ?: System.currentTimeMillis())
        )
        if (existing == null) db.noteDao().insert(note) else db.noteDao().update(note)
    }
}

private suspend fun resolveSectionId(db: AppDatabase, value: String, version: Int): Long? {
    return if (version >= 2) db.sectionDao().getByUid(value)?.id else value.toLongOrNull()
}

private suspend fun resolveTaziehId(db: AppDatabase, value: String, version: Int): Long? {
    return if (version >= 2) db.taziehDao().getByUid(value)?.id else value.toLongOrNull()
}

private suspend fun resolveRoleId(db: AppDatabase, value: String, version: Int): Long? {
    return if (version >= 2) db.roleDao().getByUid(value)?.id else value.toLongOrNull()
}

private suspend fun restoreBookmarks(context: Context, db: AppDatabase, arr: JSONArray, version: Int) {
    for (i in 0 until arr.length()) {
        val value = arr.get(i).toString()
        val id = resolveSectionId(db, value, version) ?: continue
        if (!Prefs.isBookmarked(context, id)) Prefs.toggleBookmark(context, id)
        db.userDataDao().getBookmark(db.sectionDao().getById(id).uid) ?: db.userDataDao().insertBookmark(BookmarkEntity(sectionUid = db.sectionDao().getById(id).uid))
    }
}

private suspend fun restoreRecent(context: Context, db: AppDatabase, arr: JSONArray, version: Int) {
    // Add from oldest to newest because Prefs.addRecent puts each item at the front.
    for (i in arr.length() - 1 downTo 0) {
        val id = resolveSectionId(db, arr.get(i).toString(), version) ?: continue
        Prefs.addRecent(context, id)
        val uid = db.sectionDao().getById(id).uid
        val existing = db.userDataDao().getRecentSections().firstOrNull { it.sectionUid == uid }
        if (existing == null) db.userDataDao().insertRecent(RecentSectionEntity(sectionUid = uid, position = i))
    }
}

private suspend fun restoreReadSections(context: Context, db: AppDatabase, arr: JSONArray, version: Int) {
    val ids = mutableSetOf<Long>()
    for (i in 0 until arr.length()) {
        resolveSectionId(db, arr.get(i).toString(), version)?.let(ids::add)
    }
    Prefs.mergeReadSectionIds(context, ids)
    ids.forEach { id ->
        db.sectionDao().getById(id).let { section ->
            if (db.userDataDao().getReading(section.uid) == null) db.userDataDao().insertReading(ReadingHistoryEntity(sectionUid = section.uid))
        }
    }
}

private suspend fun restoreActiveDays(context: Context, db: AppDatabase, arr: JSONArray) {
    val values = (0 until arr.length()).map { arr.get(it).toString() }.toSet()
    Prefs.mergeActiveDayValues(context, values)
    values.forEach { if (db.userDataDao().getActiveDay(it) == null) db.userDataDao().insertActiveDay(ActiveDayEntity(dayKey = it)) }
}

private suspend fun restoreTags(context: Context, db: AppDatabase, arr: JSONArray, version: Int) {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val key = if (version >= 2) o.optString("sectionUid") else o.optString("sectionId")
        val id = resolveSectionId(db, key, version) ?: continue
        Prefs.setTag(context, id, o.getString("tag"))
        val uid = db.sectionDao().getById(id).uid
        val tag = o.getString("tag")
        if (db.userDataDao().getTag(uid) == null) db.userDataDao().insertTag(SectionTagEntity(sectionUid = uid, tag = tag)) else db.userDataDao().updateTag(uid, tag, System.currentTimeMillis())
    }
}

private suspend fun restoreFootnotes(db: AppDatabase, arr: JSONArray, version: Int) {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val sectionKey = if (version >= 2) o.optString("sectionUid") else o.optString("sectionId")
        val sectionId = resolveSectionId(db, sectionKey, version) ?: continue
        val uid = o.optString("uid").ifBlank { ContentUid.new() }
        val existing = db.footnoteDao().getByUid(uid)
        val fn = FootnoteEntity(
            id = existing?.id ?: 0,
            sectionId = sectionId,
            term = o.getString("term"),
            explanation = o.getString("explanation"),
            uid = uid
        )
        if (existing == null) db.footnoteDao().insert(fn) else db.footnoteDao().update(fn)
    }
}

private suspend fun restoreMyRoles(context: Context, db: AppDatabase, arr: JSONArray, version: Int) {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val taziehKey = if (version >= 2) o.optString("taziehUid") else o.optString("taziehId")
        val roleKey = if (version >= 2) o.optString("roleUid") else o.optString("roleId")
        val taziehId = resolveTaziehId(db, taziehKey, version) ?: continue
        val roleId = resolveRoleId(db, roleKey, version) ?: continue
        Prefs.setMyRole(context, taziehId, roleId)
        val tazieh = db.taziehDao().getById(taziehId)!!
        val role = db.roleDao().getById(roleId)
        if (db.userDataDao().getMyRole(tazieh.uid) == null) db.userDataDao().insertMyRole(MyRoleEntity(taziehUid = tazieh.uid, roleUid = role.uid)) else db.userDataDao().updateMyRole(tazieh.uid, role.uid, System.currentTimeMillis())
    }
}

private suspend fun restoreDialogues(db: AppDatabase, arr: JSONArray, version: Int) {
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val taziehKey = if (version >= 2) o.optString("taziehUid") else o.optString("taziehId")
        val taziehId = resolveTaziehId(db, taziehKey, version) ?: continue
        val uid = o.optString("uid").ifBlank { ContentUid.new() }
        val existing = db.dialogueDao().getByUid(uid)
        val dialogueId = if (existing == null) {
            db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = o.getString("title"), uid = uid))
        } else {
            db.dialogueDao().updateIdentity(existing.id, taziehId, o.getString("title"), uid)
            existing.id
        }

        // A dialogue is a snapshot; replace its turns atomically to avoid duplicate turns.
        db.dialogueTurnDao().deleteAllForDialogue(dialogueId)
        val turnsArr = o.optJSONArray("turns") ?: JSONArray()
        for (j in 0 until turnsArr.length()) {
            val t = turnsArr.getJSONObject(j)
            val sectionKey = if (version >= 2) t.optString("sectionUid") else t.optString("sectionId")
            val sectionId = resolveSectionId(db, sectionKey, version) ?: continue
            db.dialogueTurnDao().insert(
                DialogueTurnEntity(
                    dialogueId = dialogueId,
                    sectionId = sectionId,
                    orderIndex = t.getInt("orderIndex"),
                    uid = t.optString("uid").ifBlank { ContentUid.new() }
                )
            )
        }
    }
}
