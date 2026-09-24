package com.example.bookapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@Database(
    entities = [
        FieldEntity::class,
        TaziehEntity::class,
        RoleEntity::class,
        SectionEntity::class,
        NoteEntity::class,
        FootnoteEntity::class,
        DialogueEntity::class,
        DialogueTurnEntity::class,
        TaziehImageEntity::class,
        BookmarkEntity::class,
        SectionTagEntity::class,
        RecentSectionEntity::class,
        ReadingHistoryEntity::class,
        MyRoleEntity::class,
        ActiveDayEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun taziehDao(): TaziehDao
    abstract fun roleDao(): RoleDao
    abstract fun sectionDao(): SectionDao
    abstract fun searchDao(): SearchDao
    abstract fun noteDao(): NoteDao
    abstract fun footnoteDao(): FootnoteDao
    abstract fun dialogueDao(): DialogueDao
    abstract fun dialogueTurnDao(): DialogueTurnDao
    abstract fun taziehImageDao(): TaziehImageDao
    abstract fun userDataDao(): UserDataDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dialogues` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `taziehId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        FOREIGN KEY(`taziehId`) REFERENCES `taziehs`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogues_taziehId` ON `dialogues` (`taziehId`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dialogue_turns` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `dialogueId` INTEGER NOT NULL,
                        `sectionId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`dialogueId`) REFERENCES `dialogues`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`sectionId`) REFERENCES `sections`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogue_turns_dialogueId` ON `dialogue_turns` (`dialogueId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dialogue_turns_sectionId` ON `dialogue_turns` (`sectionId`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tazieh_images` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `taziehId` INTEGER NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `caption` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`taziehId`) REFERENCES `taziehs`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tazieh_images_taziehId` ON `tazieh_images` (`taziehId`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `taziehs` ADD COLUMN `author` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `taziehs` ADD COLUMN `authorEmail` TEXT DEFAULT NULL")
            }
        }

        /** Version 9: add portable UIDs and content-source tracking without destroying user data. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val additions = listOf(
                    "ALTER TABLE `fields` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `taziehs` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `roles` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `sections` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `sections` ADD COLUMN `sourceUid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `notes` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `footnotes` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `dialogues` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `dialogue_turns` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `tazieh_images` ADD COLUMN `uid` TEXT NOT NULL DEFAULT ''"
                )
                additions.forEach(db::execSQL)

                // Existing installations get deterministic, unique IDs based on their local primary key.
                db.execSQL("UPDATE fields SET uid = 'legacy-field-' || id WHERE uid = ''")
                db.execSQL("UPDATE taziehs SET uid = 'legacy-tazieh-' || id WHERE uid = ''")
                db.execSQL("UPDATE roles SET uid = 'legacy-role-' || id WHERE uid = ''")
                db.execSQL("UPDATE sections SET uid = 'legacy-section-' || id WHERE uid = ''")
                db.execSQL("UPDATE notes SET uid = 'legacy-note-' || id WHERE uid = ''")
                db.execSQL("UPDATE footnotes SET uid = 'legacy-footnote-' || id WHERE uid = ''")
                db.execSQL("UPDATE dialogues SET uid = 'legacy-dialogue-' || id WHERE uid = ''")
                db.execSQL("UPDATE dialogue_turns SET uid = 'legacy-dialogue-turn-' || id WHERE uid = ''")
                db.execSQL("UPDATE tazieh_images SET uid = 'legacy-image-' || id WHERE uid = ''")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fields_uid` ON `fields` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_taziehs_uid` ON `taziehs` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_roles_uid` ON `roles` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sections_uid` ON `sections` (`uid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sections_sourceUid` ON `sections` (`sourceUid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notes_uid` ON `notes` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_footnotes_uid` ON `footnotes` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dialogues_uid` ON `dialogues` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dialogue_turns_uid` ON `dialogue_turns` (`uid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tazieh_images_uid` ON `tazieh_images` (`uid`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `sectionUid` TEXT DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `sectionUid` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_sectionUid` ON `bookmarks` (`sectionUid`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `section_tags` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `sectionUid` TEXT NOT NULL, `tag` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_section_tags_sectionUid` ON `section_tags` (`sectionUid`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `recent_sections` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `sectionUid` TEXT NOT NULL, `position` INTEGER NOT NULL, `visitedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_sections_sectionUid` ON `recent_sections` (`sectionUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_sections_position` ON `recent_sections` (`position`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reading_history` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `sectionUid` TEXT NOT NULL, `firstReadAt` INTEGER NOT NULL, `lastReadAt` INTEGER NOT NULL, `readCount` INTEGER NOT NULL, `totalSeconds` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_sectionUid` ON `reading_history` (`sectionUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_lastReadAt` ON `reading_history` (`lastReadAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `my_roles` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `taziehUid` TEXT NOT NULL, `roleUid` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_my_roles_taziehUid` ON `my_roles` (`taziehUid`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `active_days` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `dayKey` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_active_days_dayKey` ON `active_days` (`dayKey`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookapp.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

suspend fun syncLocalContentFiles(context: Context, db: AppDatabase): Int {
    val isProtectedViewer = com.example.bookapp.BuildConfig.PUBLIC_VIEWER
    val names = context.assets.list("content")
        ?.filter { name ->
            if (isProtectedViewer) name.endsWith(".taz") else name.endsWith(".json") && name != "manifest.json"
        }
        ?.sorted() ?: emptyList()
    val allFiles = names.map { name ->
        val text = if (isProtectedViewer) {
            ContentProtectionHelper.readProtectedJson(context, name)
        } else {
            context.assets.open("content/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        FileWithKey(name, text, "$name:${sha256(text)}", "")
    }

    var processed = Prefs.getProcessedContentFiles(context)
    val hadContentBefore = db.fieldDao().getAll().isNotEmpty()
    if (!hadContentBefore && processed.isNotEmpty()) processed = emptySet()

    val newOrChangedFiles = allFiles.filter { it.key !in processed }
    if (newOrChangedFiles.isEmpty()) return 0

    for (file in newOrChangedFiles) {
        val errors = ContentValidator.validate(file.text)
        if (errors.isNotEmpty()) throw IllegalArgumentException("محتوای ${file.name} نامعتبر است: ${errors.take(3).joinToString("؛ ")}")
        mergeContentFromJson(db, file.text, ContentUid.source(file.name))
    }
    Prefs.setProcessedContentFiles(context, allFiles.map { it.key }.toSet())

    return newOrChangedFiles.size
}

private data class FileWithKey(val name: String, val text: String, val key: String, val legacyKey: String)

suspend fun syncRemoteContent(
    db: AppDatabase,
    url: String = "https://raw.githubusercontent.com/lavialireza/tazeahappv-1/main/app/src/admin/assets/content/001_sample.json"
): Result<Unit> {
    if (com.example.bookapp.BuildConfig.PUBLIC_VIEWER) {
        return Result.failure(IllegalStateException("نسخه عمومی اجازه دریافت محتوای آنلاین را ندارد"))
    }
    return try {
        val jsonText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { withHttpGet(url) }
        val errors = ContentValidator.validate(jsonText)
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException("محتوای آنلاین نامعتبر است: ${errors.take(3).joinToString("؛ ")}"))
        mergeContentFromJson(db, jsonText, ContentUid.source(url))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun withHttpGet(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.requestMethod = "GET"
    return try {
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/**
 * Imports content by stable UID. Legacy JSON without UIDs remains supported by title matching.
 * A source UID lets us safely remove sections deleted from a particular content file without
 * touching sections supplied by other files.
 */
internal suspend fun mergeContentFromJson(db: AppDatabase, jsonText: String, sourceUid: String = ContentUid.source(jsonText), syncContext: Context? = null) {
    val fields = JSONArray(jsonText)
    db.withTransaction {
        for (fi in 0 until fields.length()) {
            val fieldObj = fields.getJSONObject(fi)
            val fieldTitle = fieldObj.getString("title")
            val explicitFieldUid = fieldObj.optString("uid").trim()
            val fieldUid = explicitFieldUid.ifBlank { ContentUid.derived("field", "root", fieldTitle) }
            val remoteFieldUpdatedAt = fieldObj.optString("updatedAt")
            val applyField = syncContext == null || SyncMetaStore.shouldApplyRemote(syncContext, "fields", fieldUid, remoteFieldUpdatedAt)
            val existingField = db.fieldDao().getByUid(fieldUid)
                ?: if (explicitFieldUid.isBlank()) db.fieldDao().getByTitle(fieldTitle) else null
            val fieldId = if (existingField == null) {
                val insertedId = db.fieldDao().insert(FieldEntity(title = fieldTitle, uid = fieldUid))
                if (syncContext != null) {
                    SyncMetaStore.baseline(syncContext, "fields", fieldUid, fieldObj, remoteFieldUpdatedAt, fieldObj.optString("createdAt"))
                }
                insertedId
            } else {
                if (applyField && (existingField.uid != fieldUid || existingField.title != fieldTitle)) {
                    db.fieldDao().updateIdentity(existingField.id, fieldTitle, fieldUid)
                }
                if (syncContext != null && applyField) {
                    SyncMetaStore.baseline(syncContext, "fields", fieldUid, fieldObj, remoteFieldUpdatedAt, fieldObj.optString("createdAt"))
                }
                existingField.id
            }

            val taziehs = fieldObj.getJSONArray("taziehs")
            for (ti in 0 until taziehs.length()) {
                val taziehObj = taziehs.getJSONObject(ti)
                val taziehTitle = taziehObj.getString("title")
                val explicitTaziehUid = taziehObj.optString("uid").trim()
                val taziehUid = explicitTaziehUid.ifBlank { ContentUid.derived("tazieh", fieldUid, taziehTitle) }
                val remoteTaziehUpdatedAt = taziehObj.optString("updatedAt")
                val applyTazieh = syncContext == null || SyncMetaStore.shouldApplyRemote(syncContext, "taziehs", taziehUid, remoteTaziehUpdatedAt)
                val existingTazieh = db.taziehDao().getByUid(taziehUid)
                    ?: if (explicitTaziehUid.isBlank()) db.taziehDao().getByTitle(fieldId, taziehTitle) else null
                val taziehId = if (existingTazieh == null) {
                    val insertedId = db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = taziehTitle, uid = taziehUid))
                    if (syncContext != null) {
                        SyncMetaStore.baseline(syncContext, "taziehs", taziehUid, taziehObj, remoteTaziehUpdatedAt, taziehObj.optString("createdAt"))
                    }
                    insertedId
                } else {
                    if (applyTazieh && (existingTazieh.uid != taziehUid || existingTazieh.title != taziehTitle)) {
                        db.taziehDao().updateIdentity(existingTazieh.id, fieldId, taziehTitle, taziehUid)
                    }
                    if (syncContext != null && applyTazieh) {
                        SyncMetaStore.baseline(syncContext, "taziehs", taziehUid, taziehObj, remoteTaziehUpdatedAt, taziehObj.optString("createdAt"))
                    }
                    existingTazieh.id
                }

                val author = taziehObj.optString("author", "").ifBlank { null }
                val authorEmail = taziehObj.optString("authorEmail", "").ifBlank { null }
                if (applyTazieh && (author != null || authorEmail != null)) {
                    db.taziehDao().updateAuthor(
                        taziehId,
                        author ?: existingTazieh?.author,
                        authorEmail ?: existingTazieh?.authorEmail
                    )
                }

                val roles = taziehObj.getJSONArray("roles")
                for (ri in 0 until roles.length()) {
                    val roleObj = roles.getJSONObject(ri)
                    val roleTitle = roleObj.getString("title")
                    val explicitRoleUid = roleObj.optString("uid").trim()
                    val roleUid = explicitRoleUid.ifBlank { ContentUid.derived("role", taziehUid, roleTitle) }
                    val remoteRoleUpdatedAt = roleObj.optString("updatedAt")
                    val applyRole = syncContext == null || SyncMetaStore.shouldApplyRemote(syncContext, "roles", roleUid, remoteRoleUpdatedAt)
                    val roleOrder = ri
                    val existingRole = db.roleDao().getByUid(roleUid)
                        ?: if (explicitRoleUid.isBlank()) db.roleDao().getByTitle(taziehId, roleTitle) else null
                    val roleId = if (existingRole == null) {
                        val insertedId = db.roleDao().insert(RoleEntity(taziehId = taziehId, title = roleTitle, orderIndex = roleOrder, uid = roleUid))
                        if (syncContext != null) {
                            SyncMetaStore.baseline(syncContext, "roles", roleUid, roleObj, remoteRoleUpdatedAt, roleObj.optString("createdAt"))
                        }
                        insertedId
                    } else {
                        if (applyRole) {
                            db.roleDao().updateFromContent(existingRole.id, taziehId, roleTitle, roleOrder, roleUid)
                        }
                        if (syncContext != null && applyRole) {
                            SyncMetaStore.baseline(syncContext, "roles", roleUid, roleObj, remoteRoleUpdatedAt, roleObj.optString("createdAt"))
                        }
                        existingRole.id
                    }

                    val sections = roleObj.getJSONArray("sections")
                    val seenSectionUids = mutableSetOf<String>()
                    for (si in 0 until sections.length()) {
                        val secObj = sections.getJSONObject(si)
                        val sectionTitle = secObj.getString("title")
                        val explicitSectionUid = secObj.optString("uid").trim()
                        val sectionUid = explicitSectionUid.ifBlank {
                            ContentUid.derivedWithIndex("section", roleUid, sectionTitle, si)
                        }
                        val newContent = secObj.getString("content")
                        val newAudio = secObj.optString("audio", "").ifBlank { null }
                        val remoteSectionUpdatedAt = secObj.optString("updatedAt")
                        val applySection = syncContext == null || SyncMetaStore.shouldApplyRemote(syncContext, "sections", sectionUid, remoteSectionUpdatedAt)
                        seenSectionUids += sectionUid

                        val existingSection = db.sectionDao().getByUid(sectionUid)
                            ?: if (explicitSectionUid.isBlank()) db.sectionDao().getByTitle(roleId, sectionTitle) else null
                        if (existingSection == null) {
                            val insertedId = db.sectionDao().insert(
                                SectionEntity(
                                    roleId = roleId,
                                    orderIndex = si,
                                    title = sectionTitle,
                                    content = newContent,
                                    audioUrl = newAudio,
                                    uid = sectionUid,
                                    sourceUid = sourceUid
                                )
                            )
                            if (syncContext != null) {
                                SyncMetaStore.baseline(syncContext, "sections", sectionUid, secObj, remoteSectionUpdatedAt, secObj.optString("createdAt"))
                            }
                            insertedId
                        } else {
                            if (applySection) {
                                db.sectionDao().updateFromContent(
                                    id = existingSection.id,
                                    roleId = roleId,
                                    title = sectionTitle,
                                    content = newContent,
                                    audioUrl = newAudio ?: existingSection.audioUrl,
                                    orderIndex = si,
                                    uid = sectionUid,
                                    sourceUid = sourceUid
                                )
                                if (syncContext != null) {
                                    SyncMetaStore.baseline(syncContext, "sections", sectionUid, secObj, remoteSectionUpdatedAt, secObj.optString("createdAt"))
                                }
                            }
                        }
                    }

                    // Only sections owned by this source are candidates for removal.
                    if (syncContext == null) {
                        db.sectionDao().getBySourceAndRole(sourceUid, roleId)
                            .filter { it.uid !in seenSectionUids }
                            .forEach { db.sectionDao().delete(it.id) }
                    }
                }
            }
        }
    }
}

private fun sha256(text: String): String = ContentUid.sha256(text)
