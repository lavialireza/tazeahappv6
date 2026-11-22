package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TextDiffTest {
    @Test fun detectsAddedAndRemovedLines() {
        val result = TaziehTextDiff.compare("سلام\nخداحافظ", "سلام\nیا حسین")
        assertEquals(1, result.same)
        assertEquals(1, result.removed)
        assertEquals(1, result.added)
    }

    @Test fun normalizesArabicYehAndKafForComparison() {
        val result = TaziehTextDiff.compare("علي كریم", "علی کریم")
        assertEquals(1, result.same)
        assertEquals(0, result.added)
        assertEquals(0, result.removed)
    }

    @Test fun handlesEmptyTexts() {
        val result = TaziehTextDiff.compare("", "یک سطر")
        assertEquals(1, result.added)
        assertEquals(0, result.removed)
    }
}
