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

@RunWith(RobolectricTestRunner::class)
class ContentMergeTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun json(sections: String = "{\"title\":\"ورود\",\"content\":\"بیت اول\"},{\"title\":\"شهادت\",\"content\":\"بیت دوم\"}") =
        """[{"title":"اصفهان","taziehs":[{"title":"عاشورا","roles":[{"title":"شمر","sections":[$sections]}]}]}]"""

    @Test
    fun `same source is idempotent`() = runTest {
        val source = ContentUid.source("test-source")
        mergeContentFromJson(db, json(), source)
        mergeContentFromJson(db, json(), source)

        assertEquals(1, db.fieldDao().getAll().size)
        val field = db.fieldDao().getAll().first()
        val tazieh = db.taziehDao().getByField(field.id).first()
        val role = db.roleDao().getByTazieh(tazieh.id).first()
        val sections = db.sectionDao().getByRole(role.id)
        assertEquals(2, sections.size)
        assertEquals(listOf(0, 1), sections.map { it.orderIndex })
    }

    @Test
    fun `content update changes text and order`() = runTest {
        val source = ContentUid.source("update-source")
        mergeContentFromJson(db, json(), source)
        mergeContentFromJson(
            db,
            json("{\"title\":\"شهادت\",\"content\":\"متن جدید\"},{\"title\":\"ورود\",\"content\":\"بیت اول\"}"),
            source
        )

        val field = db.fieldDao().getAll().first()
        val tazieh = db.taziehDao().getByField(field.id).first()
        val role = db.roleDao().getByTazieh(tazieh.id).first()
        val sections = db.sectionDao().getByRole(role.id)
        assertEquals(2, sections.size)
        assertEquals(listOf("شهادت", "ورود"), sections.map { it.title })
        assertEquals(listOf(0, 1), sections.map { it.orderIndex })
        assertEquals("متن جدید", sections[0].content)
    }

    @Test
    fun `removed sections from one source are deleted while another source survives`() = runTest {
        val sourceA = ContentUid.source("source-a")
        val sourceB = ContentUid.source("source-b")
        mergeContentFromJson(db, json(), sourceA)
        mergeContentFromJson(db, """[{"title":"اصفهان","taziehs":[{"title":"عاشورا","roles":[{"title":"شمر","sections":[{"uid":"source-b-section","title":"ورود","content":"منبع دوم"}]}]}]}]""", sourceB)

        mergeContentFromJson(db, json("{\"title\":\"ورود\",\"content\":\"بیت اول\"}"), sourceA)

        val field = db.fieldDao().getAll().first()
        val tazieh = db.taziehDao().getByField(field.id).first()
        val role = db.roleDao().getByTazieh(tazieh.id).first()
        val sections = db.sectionDao().getByRole(role.id)
        assertEquals(2, sections.size)
        assertEquals(setOf("بیت اول", "منبع دوم"), sections.map { it.content }.toSet())
    }

    @Test
    fun `new role is added without duplicating tazieh`() = runTest {
        mergeContentFromJson(db, json())
        val second = """[{"title":"اصفهان","taziehs":[{"title":"عاشورا","roles":[{"title":"امام حسین","sections":[{"title":"ورود","content":"بیت تازه"}]}]}]}]"""
        mergeContentFromJson(db, second)
        assertEquals(1, db.fieldDao().getAll().size)
        assertEquals(1, db.taziehDao().getAll().size)
        val tazieh = db.taziehDao().getAll().first()
        assertEquals(2, db.roleDao().getByTazieh(tazieh.id).size)
    }
}

