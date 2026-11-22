package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MultiVersionComparisonPlanTest {
    @Test fun twoVersionsCreateOneComparison() {
        assertEquals(listOf("v1" to "v2"), MultiVersionComparisonPlan.pairs(listOf("v1", "v2")))
    }

    @Test fun threeVersionsUseFirstAsBaseline() {
        assertEquals(listOf("v1" to "v2", "v1" to "v3"), MultiVersionComparisonPlan.pairs(listOf("v1", "v2", "v3")))
    }

    @Test fun fourVersionsCreateThreeComparisons() {
        assertEquals(listOf("v1" to "v2", "v1" to "v3", "v1" to "v4"), MultiVersionComparisonPlan.pairs(listOf("v1", "v2", "v3", "v4")))
    }

    @Test fun rejectsLessThanTwoOrMoreThanFour() {
        assertThrows(IllegalArgumentException::class.java) { MultiVersionComparisonPlan.pairs(listOf("v1")) }
        assertThrows(IllegalArgumentException::class.java) { MultiVersionComparisonPlan.pairs(listOf("1", "2", "3", "4", "5")) }
    }

    @Test fun rejectsDuplicateAndBlankIds() {
        assertThrows(IllegalArgumentException::class.java) { MultiVersionComparisonPlan.pairs(listOf("v1", "v1")) }
        assertThrows(IllegalArgumentException::class.java) { MultiVersionComparisonPlan.pairs(listOf("v1", " ")) }
    }
}
