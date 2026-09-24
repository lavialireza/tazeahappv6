package com.example.bookapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * تست‌های واحد برای قابلیت‌هایی که بعد از ContentMergeTest اضافه شدند:
 * پاورقی، گفتگو (و نوبت‌هایش)، ترتیب نقش‌ها، و پشتیبان‌گیری/بازیابی.
 */
@RunWith(RobolectricTestRunner::class)
class DataLayerTest {

    private lateinit var db: AppDatabase
    private lateinit var fieldId: Long
    private lateinit var taziehId: Long
    private var roleAId: Long = 0
    private var roleBId: Long = 0
    private var sectionAId: Long = 0
    private var sectionBId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fieldId = db.fieldDao().insert(FieldEntity(title = "اصفهان", uid = "field-test"))
        taziehId = db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = "عاشورا", uid = "tazieh-test"))
        roleAId = db.roleDao().insert(RoleEntity(taziehId = taziehId, title = "امام حسین", orderIndex = 0, uid = "role-imam"))
        roleBId = db.roleDao().insert(RoleEntity(taziehId = taziehId, title = "علی‌اکبر", orderIndex = 1, uid = "role-akbar"))
        sectionAId = db.sectionDao().insert(SectionEntity(roleId = roleAId, orderIndex = 0, title = "وداع", content = "بیت امام", uid = "section-goodbye"))
        sectionBId = db.sectionDao().insert(SectionEntity(roleId = roleBId, orderIndex = 0, title = "جواب", content = "بیت علی‌اکبر", uid = "section-answer"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `footnote can be added, read back, and updated`() = runTest {
        val id = db.footnoteDao().insert(FootnoteEntity(sectionId = sectionAId, term = "وداع", explanation = "خداحافظی"))
        var list = db.footnoteDao().getBySection(sectionAId)
        assertEquals(1, list.size)
        assertEquals("خداحافظی", list[0].explanation)

        db.footnoteDao().update(list[0].copy(explanation = "خداحافظی پیش از میدان"))
        list = db.footnoteDao().getBySection(sectionAId)
        assertEquals("خداحافظی پیش از میدان", list[0].explanation)

        db.footnoteDao().delete(id)
        assertTrue(db.footnoteDao().getBySection(sectionAId).isEmpty())
    }

    @Test
    fun `role orderIndex controls getByTazieh ordering and can be swapped`() = runTest {
        var roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(listOf("امام حسین", "علی‌اکبر"), roles.map { it.title })

        // جابه‌جایی ترتیب (شبیه دکمه‌های بالا/پایین در فهرست)
        db.roleDao().updateOrderIndex(roleAId, 1)
        db.roleDao().updateOrderIndex(roleBId, 0)

        roles = db.roleDao().getByTazieh(taziehId)
        assertEquals(listOf("علی‌اکبر", "امام حسین"), roles.map { it.title })
    }

    @Test
    fun `dialogue turns keep given order and cascade-delete with dialogue`() = runTest {
        val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = "گفتگوی امام و علی‌اکبر"))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionAId, orderIndex = 0))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionBId, orderIndex = 1))

        var turns = db.dialogueTurnDao().getByDialogue(dialogueId)
        assertEquals(2, turns.size)
        assertEquals(sectionAId, turns[0].sectionId)
        assertEquals(sectionBId, turns[1].sectionId)

        // جابه‌جایی ترتیب نوبت‌ها
        db.dialogueTurnDao().updateOrderIndex(turns[0].id, 1)
        db.dialogueTurnDao().updateOrderIndex(turns[1].id, 0)
        turns = db.dialogueTurnDao().getByDialogue(dialogueId)
        assertEquals(sectionBId, turns[0].sectionId)

        // حذف گفتگو باید نوبت‌هایش را هم پاک کند (ForeignKey CASCADE)
        db.dialogueDao().delete(dialogueId)
        assertTrue(db.dialogueTurnDao().getByDialogue(dialogueId).isEmpty())
    }

    @Test
    fun `portable v2 backup restores by uid into database with different numeric ids`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db.noteDao().insert(NoteEntity(title = "یادداشت من", content = "متن یادداشت", uid = "note-test"))
        Prefs.toggleBookmark(context, sectionAId)
        db.footnoteDao().insert(FootnoteEntity(sectionId = sectionAId, term = "وداع", explanation = "خداحافظی", uid = "footnote-test"))
        Prefs.setMyRole(context, taziehId, roleAId)
        val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = "گفتگوی تست", uid = "dialogue-test"))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionAId, orderIndex = 0, uid = "turn-test"))

        val json = buildBackupJson(context, db)
        val tempFile = File.createTempFile("backup_test", ".json")
        tempFile.writeText(json)
        val uri = android.net.Uri.fromFile(tempFile)

        val freshDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        // Add unrelated records first so destination auto-generated IDs are different.
        val otherField = freshDb.fieldDao().insert(FieldEntity(title = "زمینه دیگر", uid = "other-field"))
        val otherTazieh = freshDb.taziehDao().insert(TaziehEntity(fieldId = otherField, title = "تعزیه دیگر", uid = "other-tazieh"))
        val otherRole = freshDb.roleDao().insert(RoleEntity(taziehId = otherTazieh, title = "نقش دیگر", uid = "other-role"))
        freshDb.sectionDao().insert(SectionEntity(roleId = otherRole, orderIndex = 0, title = "بخش دیگر", content = "متن دیگر", uid = "other-section"))

        val freshFieldId = freshDb.fieldDao().insert(FieldEntity(title = "اصفهان", uid = "field-test"))
        val freshTaziehId = freshDb.taziehDao().insert(TaziehEntity(fieldId = freshFieldId, title = "عاشورا", uid = "tazieh-test"))
        val freshRoleId = freshDb.roleDao().insert(RoleEntity(taziehId = freshTaziehId, title = "امام حسین", uid = "role-imam"))
        val freshSectionId = freshDb.sectionDao().insert(SectionEntity(roleId = freshRoleId, orderIndex = 0, title = "وداع", content = "بیت امام", uid = "section-goodbye"))

        val result = restoreBackupFromUri(context, freshDb, uri)
        assertTrue(result.isSuccess)
        assertEquals(1, freshDb.noteDao().getAll().count { it.uid == "note-test" })
        assertTrue(freshDb.footnoteDao().getBySection(freshSectionId).any { it.uid == "footnote-test" })
        assertTrue(freshDb.dialogueDao().getByTazieh(freshTaziehId).any { it.uid == "dialogue-test" })
        assertTrue(Prefs.isBookmarked(context, freshSectionId))
        assertEquals(freshRoleId, Prefs.getMyRole(context, freshTaziehId))

        tempFile.delete()
        freshDb.close()
    }

    @Test
    fun `restoring same v2 backup twice does not duplicate notes or dialogues`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db.noteDao().insert(NoteEntity(title = "یادداشت من", content = "متن یادداشت", uid = "note-test"))
        val dialogueId = db.dialogueDao().insert(DialogueEntity(taziehId = taziehId, title = "گفتگوی تست", uid = "dialogue-test"))
        db.dialogueTurnDao().insert(DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionAId, orderIndex = 0, uid = "turn-test"))
        val json = buildBackupJson(context, db)
        val tempFile = File.createTempFile("backup_test", ".json").apply { writeText(json) }
        val uri = android.net.Uri.fromFile(tempFile)

        restoreBackupFromUri(context, db, uri)
        restoreBackupFromUri(context, db, uri)

        assertEquals(1, db.noteDao().getAll().count { it.uid == "note-test" })
        assertEquals(1, db.dialogueDao().getByTazieh(taziehId).count { it.uid == "dialogue-test" })
        tempFile.delete()
    }

}
