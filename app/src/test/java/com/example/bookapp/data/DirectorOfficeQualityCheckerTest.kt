package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorOfficeQualityCheckerTest {
    private fun workspace(
        participantRows: List<DirectorParticipantEntity>,
        turns: List<DirectorTurnEntity> = listOf(
            DirectorTurnEntity(id = 3, versionId = 2, turnNumber = 1, sequenceIndex = 0, title = "نوبت اول")
        ),
        segmentsByTurn: Map<Long, List<DirectorTextSegmentEntity>>? = null
    ): DirectorOfficeWorkspaceState {
        val plan = DirectorPlanEntity(
            id = 1, uid = "plan-1", taziehUid = "tazieh-1", title = "دفتر آزمایشی",
            createdAt = 1, updatedAt = 1
        )
        val version = DirectorPlanVersionEntity(
            id = 2, planId = 1, versionNumber = 1, createdAt = 1
        )
        val participantMap = turns.associate { turn ->
            turn.id to if (turn.id == 3L) participantRows else emptyList()
        }
        val segmentMap = segmentsByTurn ?: mapOf(3L to listOf(
            DirectorTextSegmentEntity(id = 4, turnId = 3, segmentIndex = 0, text = "متن نوبت")
        ))
        return DirectorOfficeWorkspaceState(
            plan = plan,
            versions = listOf(version),
            selectedVersion = version,
            turns = turns,
            participantsByTurn = participantMap,
            textSegmentsByTurn = segmentMap,
            stageCues = emptyList()
        )
    }

    @Test
    fun `one entered role does not mark office complete while other source roles are missing`() {
        val state = workspace(listOf(
            DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین", isLead = true)
        ))

        val issues = DirectorOfficeQualityChecker.check(
            workspace = state,
            currentSourceRoleUids = setOf("role-a", "role-b", "role-c"),
            expectedSourceRoles = mapOf(
                "role-a" to "امام حسین",
                "role-b" to "حضرت عباس",
                "role-c" to "حضرت زینب"
            )
        )

        val missingRoles = issues.filter { it.code == "MISSING_SOURCE_ROLE" }
        assertEquals(2, missingRoles.size)
        assertTrue(missingRoles.any { it.message.contains("حضرت عباس") })
        assertTrue(missingRoles.any { it.message.contains("حضرت زینب") })
    }

    @Test
    fun `all source roles present avoids missing role warnings`() {
        val state = workspace(listOf(
            DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین", isLead = true),
            DirectorParticipantEntity(id = 6, turnId = 3, roleUid = "role-b", roleTitle = "حضرت عباس")
        ))

        val issues = DirectorOfficeQualityChecker.check(
            workspace = state,
            currentSourceRoleUids = setOf("role-a", "role-b"),
            expectedSourceRoles = mapOf("role-a" to "امام حسین", "role-b" to "حضرت عباس")
        )

        assertTrue(issues.none { it.code == "MISSING_SOURCE_ROLE" })
    }

    @Test
    fun `unavailable source role list cannot produce a clean completeness result`() {
        val state = workspace(emptyList())
        val issues = DirectorOfficeQualityChecker.check(state, expectedSourceRoles = null)
        assertTrue(issues.any { it.code == "SOURCE_ROLE_ROSTER_UNAVAILABLE" })
    }
    @Test
    fun `same role and turn number in multiple turns is reported as duplicate`() {
        val turns = listOf(
            DirectorTurnEntity(id = 3, versionId = 2, turnNumber = 1, sequenceIndex = 0),
            DirectorTurnEntity(id = 7, versionId = 2, turnNumber = 1, sequenceIndex = 1)
        )
        val state = workspace(
            participantRows = listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
            turns = turns
        ).copy(
            participantsByTurn = mapOf(
                3L to listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
                7L to listOf(DirectorParticipantEntity(id = 8, turnId = 7, roleUid = "role-a", roleTitle = "امام حسین"))
            ),
            textSegmentsByTurn = mapOf(
                3L to listOf(DirectorTextSegmentEntity(id = 4, turnId = 3, segmentIndex = 0, text = "متن اول")),
                7L to listOf(DirectorTextSegmentEntity(id = 9, turnId = 7, segmentIndex = 0, text = "متن دوم"))
            )
        )
        val issues = DirectorOfficeQualityChecker.check(state, expectedSourceRoles = mapOf("role-a" to "امام حسین"))
        assertEquals(2, issues.count { it.code == "DUPLICATE_ROLE_NUMBER" })
    }

    @Test
    fun `whitespace only dialogue is reported as empty text`() {
        val state = workspace(
            participantRows = listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
            segmentsByTurn = mapOf(3L to listOf(
                DirectorTextSegmentEntity(id = 4, turnId = 3, segmentIndex = 0, text = "  \n\t ")
            ))
        )
        val issues = DirectorOfficeQualityChecker.check(state, expectedSourceRoles = mapOf("role-a" to "امام حسین"))
        assertTrue(issues.any { it.code == "NO_TEXT" })
        assertTrue(issues.any { it.code == "BLANK_SEGMENT" })
    }

    @Test
    fun `missing segment index is reported as incomplete dialogue`() {
        val state = workspace(
            participantRows = listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
            segmentsByTurn = mapOf(3L to listOf(
                DirectorTextSegmentEntity(id = 4, turnId = 3, segmentIndex = 0, text = "ابتدا"),
                DirectorTextSegmentEntity(id = 6, turnId = 3, segmentIndex = 2, text = "انتها")
            ))
        )
        val issues = DirectorOfficeQualityChecker.check(state, expectedSourceRoles = mapOf("role-a" to "امام حسین"))
        assertTrue(issues.any { it.code == "MISSING_TEXT_SEGMENT" })
    }

    @Test
    fun `same normalized dialogue text for same role is reported`() {
        val turns = listOf(
            DirectorTurnEntity(id = 3, versionId = 2, turnNumber = 1, sequenceIndex = 0),
            DirectorTurnEntity(id = 7, versionId = 2, turnNumber = 2, sequenceIndex = 1)
        )
        val state = workspace(
            participantRows = listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
            turns = turns
        ).copy(
            participantsByTurn = mapOf(
                3L to listOf(DirectorParticipantEntity(id = 5, turnId = 3, roleUid = "role-a", roleTitle = "امام حسین")),
                7L to listOf(DirectorParticipantEntity(id = 8, turnId = 7, roleUid = "role-a", roleTitle = "امام حسین"))
            ),
            textSegmentsByTurn = mapOf(
                3L to listOf(DirectorTextSegmentEntity(id = 4, turnId = 3, segmentIndex = 0, text = "سلام  بر   شما")),
                7L to listOf(DirectorTextSegmentEntity(id = 9, turnId = 7, segmentIndex = 0, text = "سلام بر شما"))
            )
        )
        val issues = DirectorOfficeQualityChecker.check(state, expectedSourceRoles = mapOf("role-a" to "امام حسین"))
        assertEquals(2, issues.count { it.code == "DUPLICATE_ROLE_TEXT" })
    }

}
