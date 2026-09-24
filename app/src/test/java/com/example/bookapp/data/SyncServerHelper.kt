package com.example.bookapp.data

import android.content.Context
import android.util.Base64
import com.example.bookapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * همگام‌سازی Android با Sync Server مشترک Web.
 *
 * برنامه همچنان offline-first است و فقط با فشردن دکمه همگام‌سازی به شبکه وصل می‌شود.
 * نسخه مدیر محتوای اصلی را نیز ارسال می‌کند؛ نسخه Viewer فقط محتوای دریافتی را
 * اعمال می‌کند و برای جلوگیری از تغییر محتوای اصلی، آن را به سرور ارسال نمی‌کند.
 *
 * این مرحله علاوه بر محتوای اصلی، اطلاعات شخصی و رسانه‌های قابل انتقال را نیز
 * همگام می‌کند: یادداشت، علاقه‌مندی، برچسب، سابقه مطالعه، نقش من، گفتگوها،
 * پاورقی، دیکشنری سفارشی، اصلاحات و فایل‌های تصویر/صوت محلی.
 */
object SyncServerHelper {
    private const val PREFS = "tazieh_sync"
    private const val KEY_URL = "server_url"
    private const val DEFAULT_TIMEOUT = 20000
    private const val DATA_PREFIX_IMAGE = "data:image/jpeg;base64,"
    private const val DATA_PREFIX_AUDIO = "data:audio/mpeg;base64,"

    fun getServerUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, BuildConfig.SYNC_SERVER_URL)?.trim()?.removeSuffix("/").orEmpty()

    fun setServerUrl(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, value.trim().removeSuffix("/")).apply()
    }

    suspend fun check(context: Context): Result<String> = withContext(Dispatchers.IO) {
        request(context, "GET", "/api/health", null)
    }

    /**
     * نام تابع برای سازگاری با Settings/Navigation حفظ شده، اما اکنون کل state
     * قابل انتقال را همگام می‌کند، نه فقط چهار جدول اصلی محتوا.
     */
    suspend fun syncContent(context: Context, db: AppDatabase): Result<SyncReport> = withContext(Dispatchers.IO) {
        val base = getServerUrl(context)
        if (base.isBlank()) return@withContext Result.failure(IllegalStateException("ابتدا آدرس Sync Server را وارد کنید."))

        try {
            val remoteText = request(context, "GET", "/api/state", null).getOrThrow()
            val remoteRoot = JSONObject(remoteText)
            val remoteData = remoteRoot.optJSONObject("data") ?: JSONObject()

            var pulledSections = 0
            val remoteContent = remoteContentToJson(remoteData)
            if (remoteContent.length() > 0) {
                val before = db.sectionDao().getAll().size
                mergeContentFromJson(db, remoteContent.toString(), ContentUid.source("sync-server"))
                SyncMetaStore.captureRemoteState(context, remoteContent)
                val after = db.sectionDao().getAll().size
                pulledSections = (after - before).coerceAtLeast(0)
            }

            // اطلاعات شخصی/کمکی از سرور به Android منتقل می‌شود و سپس state نهایی
            // به سرور برمی‌گردد. رکوردهای قدیمی‌تر با رکورد جدیدتر جایگزین نمی‌شوند.
            val userReport = mergeRemoteUserState(context, db, remoteData)
            val localState = fullLocalState(context, db)
            val mergedData = JSONObject(remoteData.toString())

            // Admin می‌تواند محتوای اصلی را منتشر کند؛ Viewer فقط نسخه سرور را
            // نگه می‌دارد و هیچ‌یک از CONTENT_KEYS را از localState ارسال نمی‌کند.
            if (!BuildConfig.PUBLIC_VIEWER) {
                for (key in CONTENT_KEYS) {
                    mergedData.put(key, localState.opt(key))
                }
            }

            // فقط داده‌های شخصی دستگاه بین Viewer/Admin و سرور مشترک می‌شوند.
            for (key in PERSONAL_KEYS) {
                mergedData.put(key, localState.opt(key))
            }
            mergedData.put("syncSource", "android-${if (BuildConfig.PUBLIC_VIEWER) "viewer" else "admin"}")
            mergedData.put("syncUpdatedAt", System.currentTimeMillis())
            mergedData.put("syncVersion", 5)

            val body = JSONObject().apply {
                put("deviceId", deviceId(context))
                put("data", mergedData)
            }
            val putText = request(context, "PUT", "/api/state", body.toString()).getOrThrow()
            val putRoot = JSONObject(putText)
            val finalData = putRoot.optJSONObject("data") ?: mergedData

            // اگر سرور بعد از merge داده‌ای را برگرداند، داده‌های شخصی آن را نیز یک بار
            // دیگر اعمال می‌کنیم تا روی Android باقی بماند.
            mergeRemoteUserState(context, db, finalData)

            Result.success(
                SyncReport(
                    pulledNewSections = pulledSections,
                    uploadedSections = localState.getJSONArray("sections").length(),
                    pulledUserItems = userReport.pulled,
                    uploadedUserItems = countPersonalItems(localState),
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * داده‌های شخصی دستگاه؛ Viewer فقط همین موارد را می‌تواند به سرور ارسال کند.
     * محتوای اصلی هرگز از Viewer به سرور برنمی‌گردد.
     */
    private val PERSONAL_KEYS = listOf(
        "notes", "bookmarks", "sectionTags", "recentSections",
        "readingHistory", "myRoles", "activeDays"
    )

    /** محتوای اصلی که Admin می‌تواند منتشر/همگام کند و Viewer فقط دریافت می‌کند. */
    private val CONTENT_KEYS = listOf(
        "fields", "taziehs", "roles", "sections",
        "footnotes", "dialogues", "dialogueTurns",
        "images", "audios", "corrections", "glossary"
    )

    private fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: ("android-" + UUID.randomUUID()).also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    private fun request(context: Context, method: String, path: String, body: String?): Result<String> {
        val base = getServerUrl(context)
        if (base.isBlank()) return Result.failure(IllegalStateException("آدرس Sync Server تنظیم نشده است."))
        return runCatching {
            val c = (URL(base + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = DEFAULT_TIMEOUT
                readTimeout = DEFAULT_TIMEOUT
                requestMethod = method
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Cache-Control", "no-cache")
                }
            }
            try {
                if (body != null) c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                val stream = if (code in 200..299) c.inputStream else c.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Sync Server خطای HTTP $code${if (text.isBlank()) "" else ": $text"}")
                text
            } finally {
                c.disconnect()
            }
        }
    }

    private suspend fun fullLocalState(context: Context, db: AppDatabase): JSONObject {
        val state = contentState(context, db)
        putPersonalState(context, db, state)
        return state
    }

    private suspend fun contentState(context: Context, db: AppDatabase): JSONObject {
        val fields = db.fieldDao().getAll()
        val taziehs = db.taziehDao().getAll()
        val roles = db.roleDao().getAllForSync()
        val sections = db.sectionDao().getAll()
        return JSONObject().apply {
            put("fields", JSONArray().also { a -> fields.forEach { f ->
                a.put(JSONObject().apply { put("id", f.id); put("uid", f.uid); put("title", f.title) }.let { o -> o.put("createdAt", ""); o.put("updatedAt", SyncMetaStore.local(context, "fields", f.uid, o, "")); o })
            } })
            put("taziehs", JSONArray().also { a -> taziehs.forEach { t ->
                a.put(JSONObject().apply {
                    put("id", t.id); put("uid", t.uid); put("fieldId", t.fieldId)
                    put("title", t.title); put("author", t.author ?: ""); put("authorEmail", t.authorEmail ?: "")
                }.let { o -> o.put("createdAt", ""); o.put("updatedAt", SyncMetaStore.local(context, "taziehs", t.uid, o, "")); o })
            } })
            put("roles", JSONArray().also { a -> roles.forEach { r ->
                a.put(JSONObject().apply { put("id", r.id); put("uid", r.uid); put("taziehId", r.taziehId); put("title", r.title); put("orderIndex", r.orderIndex) }.let { o -> o.put("createdAt", ""); o.put("updatedAt", SyncMetaStore.local(context, "roles", r.uid, o, "")); o })
            } })
            put("sections", JSONArray().also { a -> sections.forEach { s ->
                a.put(JSONObject().apply {
                    put("id", s.id); put("uid", s.uid); put("roleId", s.roleId); put("title", s.title)
                    put("content", s.content); put("audioUrl", s.audioUrl ?: ""); put("orderIndex", s.orderIndex); put("sourceUid", s.sourceUid)
                }.let { o -> o.put("createdAt", ""); o.put("updatedAt", SyncMetaStore.local(context, "sections", s.uid, o, "")); o })
            } })
        }
    }

    private suspend fun putPersonalState(context: Context, db: AppDatabase, state: JSONObject) {
        val sectionsByUid = db.sectionDao().getAll().associateBy { it.uid }
        val taziehById = db.taziehDao().getAll().associateBy { it.id }
        val roleById = db.roleDao().getAllForSync().associateBy { it.id }

        state.put("notes", JSONArray().also { a -> db.noteDao().getAll().forEach { n ->
            a.put(JSONObject().apply {
                put("uid", n.uid); put("id", n.uid); put("sectionId", n.sectionUid ?: "")
                put("title", n.title); put("text", n.content); put("content", n.content)
                put("createdAt", iso(n.createdAt)); put("updatedAt", iso(n.createdAt))
            })
        } })

        state.put("bookmarks", JSONArray().also { a -> db.userDataDao().getBookmarks().forEach { b ->
            val s = sectionsByUid[b.sectionUid]
            a.put(JSONObject().apply {
                put("id", "android-bookmark-${b.sectionUid}"); put("uid", "android-bookmark-${b.sectionUid}")
                put("sectionId", b.sectionUid); put("title", s?.title ?: ""); put("createdAt", iso(b.createdAt))
            })
        } })

        state.put("sectionTags", JSONArray().also { a -> db.userDataDao().getTags().forEach { t ->
            a.put(JSONObject().apply { put("uid", "android-tag-${t.sectionUid}"); put("sectionUid", t.sectionUid); put("tag", t.tag); put("updatedAt", iso(t.updatedAt)) })
        } })

        state.put("recentSections", JSONArray().also { a -> db.userDataDao().getRecentSections().forEach { r ->
            a.put(JSONObject().apply { put("uid", "android-recent-${r.sectionUid}"); put("sectionUid", r.sectionUid); put("position", r.position); put("visitedAt", iso(r.visitedAt)) })
        } })

        state.put("readingHistory", JSONArray().also { a -> db.userDataDao().getReadingHistory().forEach { h ->
            val s = sectionsByUid[h.sectionUid]
            a.put(JSONObject().apply {
                put("uid", "android-reading-${h.sectionUid}"); put("sectionId", h.sectionUid); put("title", s?.title ?: "")
                put("at", iso(h.lastReadAt)); put("lastReadAt", h.lastReadAt); put("firstReadAt", h.firstReadAt)
                put("readCount", h.readCount); put("totalSeconds", h.totalSeconds); put("updatedAt", iso(h.lastReadAt))
            })
        } })

        state.put("myRoles", JSONArray().also { a -> db.userDataDao().getMyRoles().forEach { m ->
            a.put(JSONObject().apply { put("uid", "android-myrole-${m.taziehUid}"); put("taziehUid", m.taziehUid); put("roleUid", m.roleUid); put("updatedAt", iso(m.updatedAt)) })
        } })
        val firstRole = db.userDataDao().getMyRoles().maxByOrNull { it.updatedAt }
        state.put("myRoleId", firstRole?.roleUid ?: JSONObject.NULL)

        state.put("activeDays", JSONArray().also { a -> db.userDataDao().getActiveDays().forEach { a.put(it.dayKey) } })

        state.put("footnotes", JSONArray().also { a ->
            sectionsByUid.values.forEach { s -> db.footnoteDao().getBySection(s.id).forEach { f ->
                a.put(JSONObject().apply { put("uid", f.uid); put("sectionUid", s.uid); put("term", f.term); put("explanation", f.explanation) })
            } }
        })

        state.put("dialogues", JSONArray().also { a -> db.dialogueDao().getAllForSync().forEach { d ->
            val t = taziehById[d.taziehId]
            a.put(JSONObject().apply { put("uid", d.uid); put("id", d.uid); put("taziehUid", t?.uid ?: ""); put("title", d.title) })
        } })
        state.put("dialogueTurns", JSONArray().also { a ->
            db.dialogueDao().getAllForSync().forEach { d ->
                db.dialogueTurnDao().getByDialogue(d.id).forEach { turn ->
                    val s = sectionsByUid.values.firstOrNull { it.id == turn.sectionId }
                    a.put(JSONObject().apply {
                        put("uid", turn.uid); put("dialogueUid", d.uid); put("sectionUid", s?.uid ?: ""); put("orderIndex", turn.orderIndex)
                    })
                }
            }
        })

        state.put("images", JSONArray().also { a -> db.taziehImageDao().getAllForSync().forEach { image ->
            val t = taziehById[image.taziehId]
            a.put(mediaObject(context, image.uid, t?.uid ?: "", image.filePath, image.caption, "image"))
        } })

        state.put("audios", JSONArray().also { a -> sectionsByUid.values.forEach { s ->
            val path = s.audioUrl ?: return@forEach
            if (path.isBlank()) return@forEach
            a.put(mediaObject(context, "android-audio-${s.uid}", s.uid, path, File(path).name, "audio"))
        } })

        state.put("corrections", JSONArray().also { a -> TaziehCorrectionStore.get(context).forEach { c ->
            a.put(JSONObject().apply { put("id", c.id.toString()); put("uid", "android-correction-${c.id}"); put("original", c.original); put("corrected", c.corrected); put("explanation", c.explanation) })
        } })

        state.put("glossary", JSONArray().also { a -> GlossaryStore.get(context).forEach { (term, explanation) ->
            a.put(JSONArray().put(term).put(explanation))
        } })
    }

    private fun mediaObject(context: Context, uid: String, parentUid: String, path: String, label: String, kind: String): JSONObject {
        val o = JSONObject().apply {
            put("uid", uid); put("fileName", label); put("caption", label); put("createdAt", iso(System.currentTimeMillis()))
        }
        if (kind == "image") o.put("taziehUid", parentUid) else o.put("sectionUid", parentUid)
        if (path.startsWith("http://") || path.startsWith("https://")) {
            o.put(if (kind == "image") "dataUrl" else "audioUrl", path)
        } else {
            val f = File(path)
            if (f.exists() && f.length() > 0) {
                val mime = if (kind == "image") "image/jpeg" else "audio/mpeg"
                val encoded = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
                o.put("dataUrl", "data:$mime;base64,$encoded")
            } else {
                o.put(if (kind == "image") "filePath" else "audioUrl", path)
            }
        }
        return o
    }

    private suspend fun mergeRemoteUserState(context: Context, db: AppDatabase, data: JSONObject): MergeUserReport {
        var pulled = 0

        val notes = data.optJSONArray("notes") ?: JSONArray()
        for (i in 0 until notes.length()) {
            val o = notes.optJSONObject(i) ?: continue
            val uid = o.optString("uid").ifBlank { o.optString("id") }
            if (uid.isBlank()) continue
            val existing = db.noteDao().getByUid(uid)
            val incomingAt = timeOf(o.optString("updatedAt")).takeIf { it > 0 } ?: timeOf(o.optString("createdAt"))
            val existingAt = existing?.createdAt ?: 0L
            val note = NoteEntity(
                id = existing?.id ?: 0,
                uid = uid,
                title = o.optString("title", "یادداشت"),
                content = o.optString("content", o.optString("text")),
                sectionUid = o.optString("sectionId").ifBlank { null },
                createdAt = maxOf(existingAt, incomingAt).takeIf { it > 0 } ?: System.currentTimeMillis()
            )
            if (existing == null) { db.noteDao().insert(note); pulled++ }
            else if (incomingAt > existingAt) { db.noteDao().update(note); pulled++ }
        }

        val bookmarks = data.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarks.length()) {
            val o = bookmarks.optJSONObject(i) ?: continue
            val sectionUid = o.optString("sectionId").ifBlank { o.optString("sectionUid") }
            if (sectionUid.isBlank() || db.sectionDao().getByUid(sectionUid) == null) continue
            if (db.userDataDao().getBookmark(sectionUid) == null) {
                db.userDataDao().insertBookmark(BookmarkEntity(sectionUid = sectionUid, createdAt = timeOf(o.optString("createdAt")).takeIf { it > 0 } ?: System.currentTimeMillis()))
                db.sectionDao().getByUid(sectionUid)?.let { section -> if (!Prefs.isBookmarked(context, section.id)) Prefs.toggleBookmark(context, section.id) }
                pulled++
            }
        }

        val tags = data.optJSONArray("sectionTags") ?: JSONArray()
        for (i in 0 until tags.length()) {
            val o = tags.optJSONObject(i) ?: continue
            val uid = o.optString("sectionUid")
            if (uid.isBlank() || db.sectionDao().getByUid(uid) == null) continue
            val existing = db.userDataDao().getTag(uid)
            val at = timeOf(o.optString("updatedAt"))
            if (existing == null) {
                db.userDataDao().insertTag(SectionTagEntity(sectionUid = uid, tag = o.optString("tag"), updatedAt = if (at > 0) at else System.currentTimeMillis()))
                db.sectionDao().getByUid(uid)?.let { Prefs.setTag(context, it.id, o.optString("tag")) }
                pulled++
            } else if (at > existing.updatedAt) {
                db.userDataDao().updateTag(uid, o.optString("tag"), at)
                db.sectionDao().getByUid(uid)?.let { Prefs.setTag(context, it.id, o.optString("tag")) }
                pulled++
            }
        }

        val recent = data.optJSONArray("recentSections") ?: JSONArray()
        for (i in 0 until recent.length()) {
            val o = recent.optJSONObject(i) ?: continue
            val uid = o.optString("sectionUid")
            if (uid.isBlank() || db.sectionDao().getByUid(uid) == null) continue
            if (db.userDataDao().getRecentSections().none { it.sectionUid == uid }) {
                db.userDataDao().insertRecent(RecentSectionEntity(sectionUid = uid, position = o.optInt("position", i), visitedAt = timeOf(o.optString("visitedAt")).takeIf { it > 0 } ?: System.currentTimeMillis()))
                db.sectionDao().getByUid(uid)?.let { Prefs.addRecent(context, it.id) }
                pulled++
            }
        }

        val history = data.optJSONArray("readingHistory") ?: JSONArray()
        for (i in 0 until history.length()) {
            val o = history.optJSONObject(i) ?: continue
            val uid = o.optString("sectionId").ifBlank { o.optString("sectionUid") }
            if (uid.isBlank() || db.sectionDao().getByUid(uid) == null) continue
            val incoming = ReadingHistoryEntity(
                sectionUid = uid,
                firstReadAt = o.optLong("firstReadAt", timeOf(o.optString("at")).takeIf { it > 0 } ?: System.currentTimeMillis()),
                lastReadAt = maxOf(o.optLong("lastReadAt", 0), timeOf(o.optString("at"))),
                readCount = o.optInt("readCount", 1),
                totalSeconds = o.optLong("totalSeconds", 0)
            )
            val existing = db.userDataDao().getReading(uid)
            if (existing == null) {
                db.userDataDao().insertReading(incoming.copy(lastReadAt = incoming.lastReadAt.takeIf { it > 0 } ?: incoming.firstReadAt))
                db.sectionDao().getByUid(uid)?.let { Prefs.mergeReadSectionIds(context, setOf(it.id)) }
                pulled++
            } else if (incoming.lastReadAt > existing.lastReadAt) {
                db.userDataDao().updateReading(uid, incoming.lastReadAt, maxOf(existing.readCount, incoming.readCount))
                db.sectionDao().getByUid(uid)?.let { Prefs.mergeReadSectionIds(context, setOf(it.id)) }
                pulled++
            }
        }

        val activeDays = data.optJSONArray("activeDays") ?: JSONArray()
        val days = (0 until activeDays.length()).mapNotNull { activeDays.optString(it).takeIf(String::isNotBlank) }
        days.forEach { day -> if (db.userDataDao().getActiveDay(day) == null) { db.userDataDao().insertActiveDay(ActiveDayEntity(dayKey = day)); pulled++ } }
        if (days.isNotEmpty()) Prefs.mergeActiveDayValues(context, days.toSet())

        val myRoles = data.optJSONArray("myRoles") ?: JSONArray()
        for (i in 0 until myRoles.length()) {
            val o = myRoles.optJSONObject(i) ?: continue
            val tUid = o.optString("taziehUid"); val rUid = o.optString("roleUid")
            if (tUid.isBlank() || rUid.isBlank()) continue
            if (db.taziehDao().getByUid(tUid) == null || db.roleDao().getByUid(rUid) == null) continue
            val at = timeOf(o.optString("updatedAt"))
            val existing = db.userDataDao().getMyRole(tUid)
            if (existing == null) {
                db.userDataDao().insertMyRole(MyRoleEntity(taziehUid = tUid, roleUid = rUid, updatedAt = if (at > 0) at else System.currentTimeMillis()))
                val taziehId = db.taziehDao().getByUid(tUid)?.id
                val roleId = db.roleDao().getByUid(rUid)?.id
                if (taziehId != null && roleId != null) Prefs.setMyRole(context, taziehId, roleId)
                pulled++
            } else if (at > existing.updatedAt) {
                db.userDataDao().updateMyRole(tUid, rUid, at)
                val taziehId = db.taziehDao().getByUid(tUid)?.id
                val roleId = db.roleDao().getByUid(rUid)?.id
                if (taziehId != null && roleId != null) Prefs.setMyRole(context, taziehId, roleId)
                pulled++
            }
        }

        val footnotes = data.optJSONArray("footnotes") ?: JSONArray()
        for (i in 0 until footnotes.length()) {
            val o = footnotes.optJSONObject(i) ?: continue
            val uid = o.optString("uid"); val sectionUid = o.optString("sectionUid")
            val sectionId = db.sectionDao().getByUid(sectionUid)?.id ?: continue
            if (uid.isBlank()) continue
            val existing = db.footnoteDao().getByUid(uid)
            val fn = FootnoteEntity(existing?.id ?: 0, sectionId, o.optString("term"), o.optString("explanation"), uid)
            if (existing == null) { db.footnoteDao().insert(fn); pulled++ }
            else if (existing.term != fn.term || existing.explanation != fn.explanation || existing.sectionId != fn.sectionId) { db.footnoteDao().update(fn); pulled++ }
        }

        val dialogues = data.optJSONArray("dialogues") ?: JSONArray()
        for (i in 0 until dialogues.length()) {
            val o = dialogues.optJSONObject(i) ?: continue
            val uid = o.optString("uid"); val tUid = o.optString("taziehUid")
            val tId = db.taziehDao().getByUid(tUid)?.id ?: continue
            if (uid.isBlank()) continue
            val existing = db.dialogueDao().getByUid(uid)
            val dId = if (existing == null) { pulled++; db.dialogueDao().insert(DialogueEntity(taziehId = tId, title = o.optString("title"), uid = uid)) }
            else { if (existing.title != o.optString("title") || existing.taziehId != tId) db.dialogueDao().updateIdentity(existing.id, tId, o.optString("title"), uid); existing.id }
            // turns are merged below
            if (dId <= 0) continue
        }

        val turns = data.optJSONArray("dialogueTurns") ?: JSONArray()
        for (i in 0 until turns.length()) {
            val o = turns.optJSONObject(i) ?: continue
            val uid = o.optString("uid"); val dUid = o.optString("dialogueUid"); val sUid = o.optString("sectionUid")
            val dId = db.dialogueDao().getByUid(dUid)?.id ?: continue
            val sId = db.sectionDao().getByUid(sUid)?.id ?: continue
            if (uid.isBlank() || db.dialogueTurnDao().getByUid(uid) != null) continue
            db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dId, sectionId = sId, orderIndex = o.optInt("orderIndex", i), uid = uid)); pulled++
        }

        val images = data.optJSONArray("images") ?: JSONArray()
        for (i in 0 until images.length()) {
            val o = images.optJSONObject(i) ?: continue
            val uid = o.optString("uid"); val tUid = o.optString("taziehUid")
            val tId = db.taziehDao().getByUid(tUid)?.id ?: continue
            if (uid.isBlank()) continue
            val existing = db.taziehImageDao().getByUid(uid)
            if (existing == null) {
                val path = materializeMedia(context, o.optString("dataUrl"), "tazieh_images", ".jpg") ?: o.optString("filePath")
                if (path.isNotBlank()) { db.taziehImageDao().insert(TaziehImageEntity(taziehId = tId, filePath = path, caption = o.optString("caption", o.optString("fileName")), uid = uid)); pulled++ }
            } else if (o.has("caption") && existing.caption != o.optString("caption")) {
                db.taziehImageDao().updateCaption(existing.id, o.optString("caption")); pulled++
            }
        }

        val audios = data.optJSONArray("audios") ?: JSONArray()
        for (i in 0 until audios.length()) {
            val o = audios.optJSONObject(i) ?: continue
            val sectionUid = o.optString("sectionUid")
            val section = db.sectionDao().getByUid(sectionUid) ?: continue
            val path = when {
                o.optString("dataUrl").startsWith("data:") -> materializeMedia(context, o.optString("dataUrl"), "tazieh_audio", ".mp3")
                o.optString("audioUrl").isNotBlank() -> o.optString("audioUrl")
                else -> null
            }
            if (!path.isNullOrBlank() && path != section.audioUrl) { db.sectionDao().updateAudioUrl(section.id, path); pulled++ }
        }

        val corrections = data.optJSONArray("corrections") ?: JSONArray()
        for (i in 0 until corrections.length()) {
            val o = corrections.optJSONObject(i) ?: continue
            val id = o.optLong("id", o.optString("id").toLongOrNull() ?: 0L)
            if (id <= 0) continue
            val current = TaziehCorrectionStore.get(context)
            if (current.none { it.id == id }) {
                TaziehCorrectionStore.add(context, o.optString("original"), o.optString("corrected"), o.optString("explanation")); pulled++
            }
        }

        val glossary = data.optJSONArray("glossary") ?: JSONArray()
        for (i in 0 until glossary.length()) {
            val pair = glossary.optJSONArray(i) ?: continue
            val term = pair.optString(0).trim(); val explanation = pair.optString(1).trim()
            if (term.isNotBlank() && explanation.isNotBlank() && !GlossaryStore.contains(context, term)) {
                GlossaryStore.add(context, term, explanation); pulled++
            }
        }

        return MergeUserReport(pulled)
    }

    private suspend fun applyRemoteTombstones(db: AppDatabase, arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.optBoolean("deleted", true)) continue
            val uid = o.optString("uid").trim()
            when (o.optString("collection")) {
                "fields" -> db.fieldDao().getByUid(uid)?.let { db.fieldDao().delete(it.id) }
                "taziehs" -> db.taziehDao().getByUid(uid)?.let { db.taziehDao().delete(it.id) }
                "roles" -> db.roleDao().getByUid(uid)?.let { db.roleDao().delete(it.id) }
                "sections" -> db.sectionDao().getByUid(uid)?.let { db.sectionDao().delete(it.id) }
                "footnotes" -> db.footnoteDao().getByUid(uid)?.let { db.footnoteDao().delete(it.id) }
                "images" -> db.taziehImageDao().getByUid(uid)?.let { db.taziehImageDao().delete(it.id) }
            }
        }
    }

    private fun materializeMedia(context: Context, dataUrl: String, folder: String, suffix: String): String? {
        if (!dataUrl.startsWith("data:")) return null
        return runCatching {
            val comma = dataUrl.indexOf(',')
            if (comma <= 0) return null
            val encoded = dataUrl.substring(comma + 1)
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val file = File(dir, "sync-${UUID.randomUUID()}$suffix")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    private fun countPersonalItems(state: JSONObject): Int = PERSONAL_KEYS.sumOf { key ->
        when (val v = state.opt(key)) {
            is JSONArray -> v.length()
            is JSONObject -> v.length()
            else -> 0
        }
    }

    private fun iso(time: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date(time))

    private fun timeOf(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        value.toLongOrNull()?.let { return it }
        return runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).parse(value)?.time ?: 0L }.getOrDefault(0L)
    }

    private fun remoteContentToJson(data: JSONObject): JSONArray {
        val fields = data.optJSONArray("fields") ?: return JSONArray()
        val taziehs = data.optJSONArray("taziehs") ?: JSONArray()
        val roles = data.optJSONArray("roles") ?: JSONArray()
        val sections = data.optJSONArray("sections") ?: JSONArray()
        val out = JSONArray()
        for (fi in 0 until fields.length()) {
            val f = fields.optJSONObject(fi) ?: continue
            val fo = JSONObject().apply { put("title", f.optString("title")); f.optString("uid").takeIf { it.isNotBlank() }?.let { put("uid", it) } }
            val ta = JSONArray()
            for (ti in 0 until taziehs.length()) {
                val t = taziehs.optJSONObject(ti) ?: continue
                if (t.optString("fieldId") != f.optString("id")) continue
                val to = JSONObject().apply { put("title", t.optString("title")); t.optString("uid").takeIf { it.isNotBlank() }?.let { put("uid", it) } }
                val ra = JSONArray()
                for (ri in 0 until roles.length()) {
                    val r = roles.optJSONObject(ri) ?: continue
                    if (r.optString("taziehId") != t.optString("id")) continue
                    val ro = JSONObject().apply { put("title", r.optString("title")); r.optString("uid").takeIf { it.isNotBlank() }?.let { put("uid", it) } }
                    val sa = JSONArray()
                    for (si in 0 until sections.length()) {
                        val s = sections.optJSONObject(si) ?: continue
                        if (s.optString("roleId") != r.optString("id")) continue
                        sa.put(JSONObject().apply { put("title", s.optString("title")); put("content", s.optString("content")); s.optString("uid").takeIf { it.isNotBlank() }?.let { put("uid", it) } })
                    }
                    ro.put("sections", sa); ra.put(ro)
                }
                to.put("roles", ra); ta.put(to)
            }
            fo.put("taziehs", ta); out.put(fo)
        }
        return out
    }

    data class SyncReport(
        val pulledNewSections: Int,
        val uploadedSections: Int,
        val timestamp: Long,
        val pulledUserItems: Int = 0,
        val uploadedUserItems: Int = 0
    )

    private data class MergeUserReport(val pulled: Int)
}
