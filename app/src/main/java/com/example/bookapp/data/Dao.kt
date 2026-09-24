package com.example.bookapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FieldDao {
    @Query("SELECT * FROM fields ORDER BY id")
    suspend fun getAll(): List<FieldEntity>

    @Query("SELECT * FROM fields WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): FieldEntity?

    @Query("SELECT * FROM fields WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): FieldEntity?

    @Query("UPDATE fields SET title = :title, uid = :uid WHERE id = :id")
    suspend fun updateIdentity(id: Long, title: String, uid: String)

    @Insert
    suspend fun insert(field: FieldEntity): Long

    @Query("DELETE FROM fields WHERE id = :fieldId")
    suspend fun delete(fieldId: Long)

    @Query("DELETE FROM fields")
    suspend fun deleteAll()
}

@Dao
interface TaziehDao {
    @Query("SELECT * FROM taziehs WHERE fieldId = :fieldId ORDER BY id")
    suspend fun getByField(fieldId: Long): List<TaziehEntity>

    @Query("SELECT * FROM taziehs ORDER BY fieldId, id")
    suspend fun getAll(): List<TaziehEntity>

    @Query("SELECT * FROM taziehs WHERE id = :taziehId")
    suspend fun getById(taziehId: Long): TaziehEntity?

    @Query("SELECT * FROM taziehs WHERE fieldId = :fieldId AND title = :title LIMIT 1")
    suspend fun getByTitle(fieldId: Long, title: String): TaziehEntity?

    @Query("SELECT * FROM taziehs WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): TaziehEntity?

    @Query("UPDATE taziehs SET fieldId = :fieldId, title = :title, uid = :uid WHERE id = :id")
    suspend fun updateIdentity(id: Long, fieldId: Long, title: String, uid: String)

    @Insert
    suspend fun insert(tazieh: TaziehEntity): Long

    @Query("UPDATE taziehs SET author = :author, authorEmail = :authorEmail WHERE id = :taziehId")
    suspend fun updateAuthor(taziehId: Long, author: String?, authorEmail: String?)

    @Query("DELETE FROM taziehs WHERE id = :taziehId")
    suspend fun delete(taziehId: Long)

    @Query("DELETE FROM taziehs")
    suspend fun deleteAll()
}

@Dao
interface RoleDao {
    @Query("SELECT * FROM roles WHERE taziehId = :taziehId ORDER BY orderIndex, id")
    suspend fun getByTazieh(taziehId: Long): List<RoleEntity>

    @Query("SELECT * FROM roles ORDER BY taziehId, orderIndex, id")
    suspend fun getAllForSync(): List<RoleEntity>

    @Query("SELECT * FROM roles WHERE id = :roleId")
    suspend fun getById(roleId: Long): RoleEntity

    @Query("SELECT * FROM roles WHERE id IN (:ids)")
    suspend fun getByIds(ids: Set<Long>): List<RoleEntity>

    @Query("SELECT * FROM roles WHERE taziehId = :taziehId AND title = :title LIMIT 1")
    suspend fun getByTitle(taziehId: Long, title: String): RoleEntity?

    @Query("SELECT * FROM roles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): RoleEntity?

    @Query("UPDATE roles SET taziehId = :taziehId, title = :title, orderIndex = :orderIndex, uid = :uid WHERE id = :id")
    suspend fun updateFromContent(id: Long, taziehId: Long, title: String, orderIndex: Int, uid: String)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM roles WHERE taziehId = :taziehId")
    suspend fun getMaxOrderIndex(taziehId: Long): Int

    @Insert
    suspend fun insert(role: RoleEntity): Long

    @Query("UPDATE roles SET title = :title WHERE id = :roleId")
    suspend fun updateTitle(roleId: Long, title: String)

    @Query("UPDATE roles SET orderIndex = :orderIndex WHERE id = :roleId")
    suspend fun updateOrderIndex(roleId: Long, orderIndex: Int)

    @Query("DELETE FROM roles WHERE id = :roleId")
    suspend fun delete(roleId: Long)

    @Query("UPDATE roles SET orderIndex = :orderIndex WHERE id = :roleId")
    suspend fun setOrder(roleId: Long, orderIndex: Int)

    @Query("DELETE FROM roles")
    suspend fun deleteAll()
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections ORDER BY roleId, orderIndex, id")
    suspend fun getAll(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE roleId = :roleId ORDER BY orderIndex")
    suspend fun getByRole(roleId: Long): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :sectionId")
    suspend fun getById(sectionId: Long): SectionEntity

    @Query("SELECT * FROM sections")
    suspend fun getAllForUserMigration(): List<SectionEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM sections WHERE roleId = :roleId")
    suspend fun getMaxOrderIndex(roleId: Long): Int

    @Query("SELECT * FROM sections WHERE roleId = :roleId AND title = :title LIMIT 1")
    suspend fun getByTitle(roleId: Long, title: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE sourceUid = :sourceUid AND roleId = :roleId")
    suspend fun getBySourceAndRole(sourceUid: String, roleId: Long): List<SectionEntity>

    @Query("UPDATE sections SET roleId = :roleId, title = :title, content = :content, audioUrl = :audioUrl, orderIndex = :orderIndex, uid = :uid, sourceUid = :sourceUid WHERE id = :id")
    suspend fun updateFromContent(id: Long, roleId: Long, title: String, content: String, audioUrl: String?, orderIndex: Int, uid: String, sourceUid: String)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun delete(sectionId: Long)

    @Query("UPDATE sections SET orderIndex = :orderIndex WHERE id = :sectionId")
    suspend fun setOrder(sectionId: Long, orderIndex: Int)

    @Insert
    suspend fun insert(section: SectionEntity): Long

    @Query("UPDATE sections SET audioUrl = :audioUrl WHERE id = :sectionId")
    suspend fun updateAudioUrl(sectionId: Long, audioUrl: String?)

    @Query("UPDATE sections SET content = :content WHERE id = :sectionId")
    suspend fun updateContent(sectionId: Long, content: String)

    @Query("DELETE FROM sections")
    suspend fun deleteAll()
}

@Dao
interface FootnoteDao {
    @Query("SELECT * FROM footnotes WHERE sectionId = :sectionId ORDER BY id")
    suspend fun getBySection(sectionId: Long): List<FootnoteEntity>

    @Query("SELECT * FROM footnotes WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): FootnoteEntity?

    @Insert
    suspend fun insert(footnote: FootnoteEntity): Long

    @Update
    suspend fun update(footnote: FootnoteEntity)

    @Query("DELETE FROM footnotes WHERE id = :footnoteId")
    suspend fun delete(footnoteId: Long)
}

@Dao
interface DialogueDao {
    @Query("SELECT * FROM dialogues WHERE taziehId = :taziehId ORDER BY id")
    suspend fun getByTazieh(taziehId: Long): List<DialogueEntity>

    @Query("SELECT * FROM dialogues ORDER BY id")
    suspend fun getAllForSync(): List<DialogueEntity>

    @Query("SELECT * FROM dialogues WHERE id = :dialogueId")
    suspend fun getById(dialogueId: Long): DialogueEntity

    @Query("SELECT * FROM dialogues WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): DialogueEntity?

    @Insert
    suspend fun insert(dialogue: DialogueEntity): Long

    @Query("UPDATE dialogues SET taziehId = :taziehId, title = :title, uid = :uid WHERE id = :dialogueId")
    suspend fun updateIdentity(dialogueId: Long, taziehId: Long, title: String, uid: String)

    @Query("UPDATE dialogues SET title = :title WHERE id = :dialogueId")
    suspend fun updateTitle(dialogueId: Long, title: String)

    @Query("DELETE FROM dialogues WHERE id = :dialogueId")
    suspend fun delete(dialogueId: Long)
}

@Dao
interface DialogueTurnDao {
    @Query("SELECT * FROM dialogue_turns WHERE dialogueId = :dialogueId ORDER BY orderIndex")
    suspend fun getByDialogue(dialogueId: Long): List<DialogueTurnEntity>

    @Query("SELECT * FROM dialogue_turns WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): DialogueTurnEntity?

    @Insert
    suspend fun insert(turn: DialogueTurnEntity): Long

    @Query("UPDATE dialogue_turns SET orderIndex = :orderIndex WHERE id = :turnId")
    suspend fun updateOrderIndex(turnId: Long, orderIndex: Int)

    @Query("DELETE FROM dialogue_turns WHERE id = :turnId")
    suspend fun deleteTurn(turnId: Long)

    @Query("DELETE FROM dialogue_turns WHERE dialogueId = :dialogueId")
    suspend fun deleteAllForDialogue(dialogueId: Long)
}

@Dao
interface TaziehImageDao {
    @Query("SELECT * FROM tazieh_images WHERE taziehId = :taziehId ORDER BY id")
    suspend fun getByTazieh(taziehId: Long): List<TaziehImageEntity>

    @Query("SELECT * FROM tazieh_images ORDER BY id")
    suspend fun getAllForSync(): List<TaziehImageEntity>

    @Query("SELECT * FROM tazieh_images WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): TaziehImageEntity?

    @Insert
    suspend fun insert(image: TaziehImageEntity): Long

    @Query("UPDATE tazieh_images SET caption = :caption WHERE id = :imageId")
    suspend fun updateCaption(imageId: Long, caption: String)

    @Query("SELECT * FROM tazieh_images WHERE id = :imageId")
    suspend fun getById(imageId: Long): TaziehImageEntity

    @Query("DELETE FROM tazieh_images WHERE id = :imageId")
    suspend fun delete(imageId: Long)
}

@Dao
interface UserDataDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getBookmarks(): List<BookmarkEntity>
    @Query("SELECT * FROM bookmarks WHERE sectionUid = :uid LIMIT 1")
    suspend fun getBookmark(uid: String): BookmarkEntity?
    @Insert
    suspend fun insertBookmark(value: BookmarkEntity)
    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
    @Query("DELETE FROM bookmarks WHERE sectionUid = :uid")
    suspend fun deleteBookmark(uid: String)

    @Query("SELECT * FROM section_tags ORDER BY updatedAt DESC")
    suspend fun getTags(): List<SectionTagEntity>
    @Query("SELECT * FROM section_tags WHERE sectionUid = :uid LIMIT 1")
    suspend fun getTag(uid: String): SectionTagEntity?
    @Insert
    suspend fun insertTag(value: SectionTagEntity)
    @Query("DELETE FROM section_tags")
    suspend fun deleteAllTags()
    @Query("UPDATE section_tags SET tag = :tag, updatedAt = :updatedAt WHERE sectionUid = :uid")
    suspend fun updateTag(uid: String, tag: String, updatedAt: Long)
    @Query("DELETE FROM section_tags WHERE sectionUid = :uid")
    suspend fun deleteTag(uid: String)

    @Query("SELECT * FROM recent_sections ORDER BY position ASC")
    suspend fun getRecentSections(): List<RecentSectionEntity>
    @Insert
    suspend fun insertRecent(value: RecentSectionEntity)
    @Query("DELETE FROM recent_sections")
    suspend fun deleteAllRecent()

    @Query("SELECT * FROM reading_history ORDER BY lastReadAt DESC")
    suspend fun getReadingHistory(): List<ReadingHistoryEntity>
    @Query("SELECT * FROM reading_history WHERE sectionUid = :uid LIMIT 1")
    suspend fun getReading(uid: String): ReadingHistoryEntity?
    @Insert
    suspend fun insertReading(value: ReadingHistoryEntity)
    @Query("DELETE FROM reading_history")
    suspend fun deleteAllReadingHistory()
    @Query("UPDATE reading_history SET lastReadAt = :lastReadAt, readCount = :readCount WHERE sectionUid = :uid")
    suspend fun updateReading(uid: String, lastReadAt: Long, readCount: Int)

    @Query("SELECT * FROM my_roles ORDER BY updatedAt DESC")
    suspend fun getMyRoles(): List<MyRoleEntity>
    @Query("SELECT * FROM my_roles WHERE taziehUid = :uid LIMIT 1")
    suspend fun getMyRole(uid: String): MyRoleEntity?
    @Insert
    suspend fun insertMyRole(value: MyRoleEntity)
    @Query("DELETE FROM my_roles")
    suspend fun deleteAllMyRoles()
    @Query("UPDATE my_roles SET roleUid = :roleUid, updatedAt = :updatedAt WHERE taziehUid = :taziehUid")
    suspend fun updateMyRole(taziehUid: String, roleUid: String, updatedAt: Long)
    @Query("DELETE FROM my_roles WHERE taziehUid = :uid")
    suspend fun deleteMyRole(uid: String)

    @Query("SELECT * FROM active_days ORDER BY dayKey")
    suspend fun getActiveDays(): List<ActiveDayEntity>
    @Insert
    suspend fun insertActiveDay(value: ActiveDayEntity)
    @Query("DELETE FROM active_days")
    suspend fun deleteAllActiveDays()
    @Query("SELECT * FROM active_days WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getActiveDay(dayKey: String): ActiveDayEntity?
}
