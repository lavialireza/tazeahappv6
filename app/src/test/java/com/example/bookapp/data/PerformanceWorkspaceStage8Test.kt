package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PerformanceWorkspaceStage8Test {
    @Test fun groupAndMemberNamesNormalizePersianAndArabicForms() {
        assertEquals(normalizePerformanceName("گروه علي كاظم"), normalizePerformanceName("گروه علی کاظم"))
        assertEquals(normalizePerformanceName("شبیه‌خوان"), normalizePerformanceName("شبیه خوان"))
    }

    @Test fun normalizedNamesDoNotCollapseDistinctWords() {
        assertNotEquals(normalizePerformanceName("حسن"), normalizePerformanceName("حسین"))
    }
}
