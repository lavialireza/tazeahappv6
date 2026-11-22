package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueRoleSelectionPolicyTest {
    @Test
    fun allowsTwoOrMoreDistinctRolesAndAtLeastTwoTurns() {
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 2), 2))
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 2, 3, 4), 8))
        assertTrue(DialogueRoleSelectionPolicy.isValid((1L..25L).toList(), 25))
        assertFalse(DialogueRoleSelectionPolicy.isValid(listOf(1), 4))
        assertFalse(DialogueRoleSelectionPolicy.isValid(listOf(1, 2), 1))
    }

    @Test
    fun doesNotImposeAnUpperLimitWhenAddingRoles() {
        val selected = (1L..25L).toList()
        assertFalse(DialogueRoleSelectionPolicy.canAddRole(2L, selected))
        assertTrue(DialogueRoleSelectionPolicy.canAddRole(26L, selected))
    }

    @Test
    fun ignoresInvalidAndDuplicateRoleIds() {
        assertTrue(DialogueRoleSelectionPolicy.isValid(listOf(1, 1, 2, 0, -1), 2))
        assertFalse(DialogueRoleSelectionPolicy.canAddRole(0L, emptyList()))
        assertFalse(DialogueRoleSelectionPolicy.canAddRole(-1L, emptyList()))
    }
}
