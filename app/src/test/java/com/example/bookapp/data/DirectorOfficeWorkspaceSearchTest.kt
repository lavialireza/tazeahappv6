package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectorOfficeWorkspaceSearchTest {
    private val turns = listOf(
        DirectorTurnEntity(id = 1, versionId = 4, turnNumber = 1, sequenceIndex = 0, title = "آغاز مجلس"),
        DirectorTurnEntity(id = 2, versionId = 4, turnNumber = 12, sequenceIndex = 1, title = "پاسخ حضرت عباس")
    )
    private val searchable = mapOf(
        1L to "1 آغاز مجلس امام حسین متن نخست",
        2L to "12 پاسخ حضرت عباس صدای سپاه"
    )

    @Test
    fun `search matches Persian digits in turn number`() {
        assertEquals(listOf(2L), DirectorOfficeWorkspaceSearch.filterTurns(turns, searchable, "۱۲").map { it.id })
    }

    @Test
    fun `search matches role and dialogue text from common index`() {
        assertEquals(listOf(2L), DirectorOfficeWorkspaceSearch.filterTurns(turns, searchable, "صدای سپاه").map { it.id })
        assertEquals(listOf(2L), DirectorOfficeWorkspaceSearch.filterTurns(turns, searchable, "حضرت عباس").map { it.id })
    }

    @Test
    fun `blank search preserves original turn order`() {
        assertEquals(turns, DirectorOfficeWorkspaceSearch.filterTurns(turns, searchable, "   "))
    }
}
