package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedSearchStage6Test {
    private fun row() = SearchCorpusRow(1, "بخش نخست", "آمده‌ام ای شاه", "امام حسین", "واقعه عاشورا", "تاریخی", 2, 3, 4, "واژه: وفا")

    @Test fun findsPersianTextAcrossArabicLetterForms() {
        val result = AdvancedSearchEngine.search(listOf(row()), "امام حسين", AdvancedSearchOptions())
        assertEquals(1, result.size)
    }

    @Test fun respectsHierarchyFilters() {
        val result = AdvancedSearchEngine.search(listOf(row()), "وفا", AdvancedSearchOptions(fieldId = 99))
        assertTrue(result.isEmpty())
    }

    @Test fun searchesFootnotesWhenEnabled() {
        val result = AdvancedSearchEngine.search(listOf(row()), "وفا", AdvancedSearchOptions(inFootnote = true, inText = false, inSectionTitle = false, inRole = false, inTazieh = false, inField = false))
        assertEquals(1, result.size)
        assertEquals("پانویس", result.first().matchSource)
    }
}
