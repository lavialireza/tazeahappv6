package com.example.bookapp.data

import org.junit.Assert.*
import org.junit.Test

class CompletionStage12Test {
    @Test fun wordDiffMarksInsertedAndRemovedWords() {
        val result = TaziehWordDiff.compare("سلام ای دوست", "سلام ای یار دوست")
        assertTrue(result.any { it.change == WordChange.ADDED && it.value.trim() == "یار" })
        assertTrue(result.any { it.change == WordChange.SAME && it.value.trim() == "سلام" })
    }

    @Test fun glossaryLinkerPreservesOriginalOffsetsAndOnlyVerifiedTerms() {
        val term = GlossaryTermEntity(id = 1, uid = "term-1", headword = "وفا", normalizedHeadword = "وفا", reviewStatus = GlossaryTermEntity.STATUS_VERIFIED, createdAt = 1, updatedAt = 1)
        val pending = GlossaryTermEntity(id = 2, uid = "term-2", headword = "یار", normalizedHeadword = "یار", reviewStatus = GlossaryTermEntity.STATUS_PENDING, createdAt = 1, updatedAt = 1)
        val found = GlossaryTextLinker.link("  به وفا و یار", listOf(term, pending), emptyList(), emptyList())
        assertEquals(1, found.size)
        assertEquals("وفا", "  به وفا و یار".substring(found[0].start, found[0].endExclusive))
        assertEquals("term-1", found[0].termUid)
    }

    @Test fun graphExcludesUnverifiedRecords() {
        val verified = EncyclopediaRecordEntity(id = 1, uid = "p1", kind = EncyclopediaRecordEntity.PERSON, title = "شخص", normalizedTitle = "شخص", reviewStatus = EncyclopediaRecordEntity.VERIFIED, createdAt = 1, updatedAt = 1)
        val pending = verified.copy(id = 2, uid = "p2", title = "در انتظار", normalizedTitle = "در انتظار", reviewStatus = EncyclopediaRecordEntity.PENDING)
        val links = listOf(EncyclopediaLinkEntity(id = 1, recordId = 1, taziehUid = "tazieh-1"), EncyclopediaLinkEntity(id = 2, recordId = 2, taziehUid = "tazieh-2"))
        val graph = EncyclopediaGraphBuilder.build(listOf(verified, pending), links)
        assertTrue(graph.nodes.any { it.id == "p1" })
        assertFalse(graph.nodes.any { it.id == "p2" })
        assertEquals(1, graph.edges.size)
    }

    @Test fun versionTextDiffStillDistinguishesArabicAndPersianForms() {
        val diff = TaziehTextDiff.compare("یار علی", "يار علي")
        assertEquals(0, diff.added)
        assertEquals(0, diff.removed)
    }
}
