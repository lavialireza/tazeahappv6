package com.example.bookapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.bookapp.data.archive.RoomArchiveRepository
import com.example.bookapp.data.archive.TaziehVersionEntity
import com.example.bookapp.domain.model.NewSourceDocument
import com.example.bookapp.domain.model.NewTaziehVersion
import com.example.bookapp.domain.model.ReviewState
import com.example.bookapp.domain.model.VersionState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArchiveRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomArchiveRepository
    private lateinit var taziehUid: String

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = RoomArchiveRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun createTazieh(): String {
        val fieldId = db.fieldDao().insert(FieldEntity(title = "آزمایش", uid = "field-archive-test"))
        taziehUid = "tazieh-archive-test"
        db.taziehDao().insert(TaziehEntity(fieldId = fieldId, title = "آزمایش تعزیه", uid = taziehUid))
        val now = System.currentTimeMillis()
        db.archiveDao().insertVersion(
            TaziehVersionEntity(
                uid = "version-archive-test", taziehUid = taziehUid,
                versionNumber = 1, title = "نسخه اولیه",
                status = TaziehVersionEntity.STATUS_LEGACY_UNREVIEWED,
                createdAt = now, updatedAt = now, isCurrent = true
            )
        )
        return taziehUid
    }

    @Test
    fun `new version receives next sequence and remains draft`() = runTest {
        val uid = createTazieh()
        val result = repository.createVersion(NewTaziehVersion(uid, "نسخه مقابله‌شده"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().versionNumber)
        assertEquals(VersionState.DRAFT, result.getOrThrow().state)
        assertTrue(!result.getOrThrow().isCurrent)
    }

    @Test
    fun `source document is registered as pending review`() = runTest {
        val uid = createTazieh()
        val result = repository.addSource(
            NewSourceDocument(
                versionUid = "version-archive-test",
                fileName = "اصل.docx",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sha256 = "a".repeat(64),
                storagePath = "archive/$uid/original.docx"
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(ReviewState.PENDING_REVIEW, result.getOrThrow().reviewState)
        assertEquals("a".repeat(64), result.getOrThrow().sha256)
    }

    @Test
    fun `published version cannot be changed back to draft`() = runTest {
        val uid = createTazieh()
        val reviewed = repository.updateVersionState("version-archive-test", VersionState.REVIEWED)
        assertTrue(reviewed.isSuccess)
        val published = repository.updateVersionState("version-archive-test", VersionState.PUBLISHED)
        assertTrue(published.isSuccess)
        val reverted = repository.updateVersionState("version-archive-test", VersionState.DRAFT)
        assertTrue(reverted.isFailure)
    }

    @Test
    fun `source review state is persisted`() = runTest {
        val uid = createTazieh()
        val created = repository.addSource(
            NewSourceDocument(
                versionUid = "version-archive-test", fileName = "اصل.pdf",
                mimeType = "application/pdf", sha256 = "b".repeat(64),
                storagePath = "archive/$uid/original.pdf"
            )
        ).getOrThrow()
        val reviewed = repository.updateSourceReview(created.uid, ReviewState.ACCEPTED, "با سند اصلی تطبیق شد")
        assertTrue(reviewed.isSuccess)
        assertEquals(ReviewState.ACCEPTED, reviewed.getOrThrow().reviewState)
    }

}
