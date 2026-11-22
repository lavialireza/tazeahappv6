package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResearchEngineTest {
    @Test fun aiPermissionsAreDisabledByDefaultAndChildrenRequireParent() {
        val defaults = ViewerAccessPolicy.defaultPermissions()
        assertEquals(false, defaults["ai"])
        assertEquals(false, defaults["ai.analysis"])
        assertEquals(false, ViewerAccessPolicy.hasPermissionForTest(mapOf("ai" to false, "ai.analysis" to true), "ai.analysis"))
        assertEquals(true, ViewerAccessPolicy.hasPermissionForTest(mapOf("ai" to true, "ai.analysis" to true), "ai.analysis"))
    }

    @Test fun repeatedLinesAreFlaggedWithoutChangingInput() {
        val text = "مجلس اول\nای دل غمین مباش\nای دل غمین مباش"
        val result = AiResearchEngine.analyzeStructure(text)
        assertTrue(result.any { it.title == "تکرارهای احتمالی" && it.sample.contains("ای دل غمین مباش") })
        assertEquals("مجلس اول\nای دل غمین مباش\nای دل غمین مباش", text)
    }

    @Test fun speakerMarkersAreGrouped() {
        val result = AiResearchEngine.suggestSpeakers("عباس: سلام\nشمر: جواب\nعباس: دوباره")
        assertEquals("عباس", result.first().title)
        assertTrue(result.first().detail.contains("2"))
    }

    @Test fun glossaryCandidatesExcludeCommonWords() {
        val result = AiResearchEngine.glossaryCandidates("وفاداری وفاداری و برای برای")
        assertTrue(result.any { it.title == "وفاداری" })
        assertTrue(result.none { it.title == "برای" })
    }

    @Test fun semanticSearchRanksMatchingArchiveRow() {
        val matching = SearchCorpusRow(1, "وفاداری عباس", "عباس در میدان وفاداری نشان داد", "عباس", "تعزیه عباس", "حماسی", 1, 1, 1, null)
        val other = SearchCorpusRow(2, "بازار", "شرح بازار", "بازرگان", "تعزیه بازار", "اجتماعی", 2, 2, 2, null)
        val result = AiResearchEngine.semanticSearch(listOf(other, matching), "وفاداری عباس")
        assertEquals(1L, result.first().row.sectionId)
    }
}
