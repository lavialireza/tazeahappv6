package com.example.bookapp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorOfficeWorkspaceOperationsTest {
    @Test
    fun `role linked turn creation uses shared operation and keeps section link`() = runTest {
        val dao = FakeWorkspaceDao()
        DirectorOfficeWorkspaceOperations.addRoleLinkedTurn(
            dao = dao,
            versionId = 20,
            turnNumber = 7,
            roleUid = "role-1",
            roleTitle = "حضرت عباس",
            text = "متن نوبت",
            sectionUid = "section-4",
            transactionRunner = DirectDirectorOfficeTransactionRunner
        )

        val turn = dao.turnRows.values.single()
        assertEquals(7, turn.turnNumber)
        assertEquals("حضرت عباس", turn.title)
        assertEquals("role-1", dao.participantRows.single().roleUid)
        assertEquals("section-4", dao.segmentRows.single().sectionUid)
        assertEquals("متن نوبت", dao.segmentRows.single().text)
    }

    @Test
    fun `reordering uses shared transaction boundary and normalized sequence`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "اول")
            turnRows[2] = DirectorTurnEntity(id = 2, versionId = 20, turnNumber = 2, sequenceIndex = 1, title = "دوم")
        }
        var transactionCalled = false

        DirectorOfficeWorkspaceOperations.reorderTurns(
            dao,
            listOf(dao.turnRows.getValue(2), dao.turnRows.getValue(1)),
            transactionRunner = object : DirectorOfficeTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T {
                    transactionCalled = true
                    return block()
                }
            }
        )

        assertTrue(transactionCalled)
        assertEquals(listOf(2L, 1L), dao.turnRows.values.sortedBy { it.sequenceIndex }.map { it.id })
    }

    @Test
    fun `numbered builder groups same number across roles and preserves source sections`() = runTest {
        val dao = FakeWorkspaceDao()
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao = dao,
            versionId = 20,
            method = DirectorOfficeSharedContract.ROLE_NUMBERED,
            roles = listOf(
                DirectorOfficeBuildRole("role-a", "امام حسین", 0),
                DirectorOfficeBuildRole("role-b", "حضرت عباس", 1)
            ),
            sections = listOf(
                DirectorOfficeBuildSection("sec-a", "role-a", "۱- سلام بر سپاه", 0, "بخش اول"),
                DirectorOfficeBuildSection("sec-b", "role-b", "1- پاسخ سپاه", 0, "بخش دوم"),
                DirectorOfficeBuildSection("sec-c", "role-a", "۲- حرکت به میدان", 1, "بخش سوم")
            )
        )

        assertEquals(2, result.insertedTurnCount)
        assertEquals(2, dao.turnRows.size)
        val first = dao.turnRows.values.single { it.turnNumber == 1 }
        assertEquals(DirectorTurnEntity.TYPE_COLLECTIVE, first.turnType)
        assertEquals(setOf("role-a", "role-b"), dao.participantRows.filter { it.turnId == first.id }.map { it.roleUid }.toSet())
        assertEquals(setOf("sec-a", "sec-b"), dao.segmentRows.filter { it.turnId == first.id }.mapNotNull { it.sectionUid }.toSet())
        val sourceSegment = dao.segmentRows.first { it.sectionUid == "sec-a" }
        assertEquals("بخش اول", sourceSegment.sourceSectionTitle)
        assertEquals("sec-a", sourceSegment.sourceSectionUids)
        assertEquals("ROLE_NUMBERED", sourceSegment.sourceMethod)
        assertTrue(sourceSegment.sourceContentHash.isNotBlank())
        assertEquals(first.id, result.turnIdsByNumber[1])
    }

    @Test
    fun `full text builder creates one turn per role with ordered source sections`() = runTest {
        val dao = FakeWorkspaceDao()
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao = dao,
            versionId = 20,
            method = DirectorOfficeSharedContract.FULL_TEXT,
            roles = listOf(DirectorOfficeBuildRole("role-a", "امام حسین", 0), DirectorOfficeBuildRole("role-b", "حضرت عباس", 1)),
            sections = listOf(
                DirectorOfficeBuildSection("sec-a1", "role-a", "بخش اول", 0),
                DirectorOfficeBuildSection("sec-a2", "role-a", "بخش دوم", 1),
                DirectorOfficeBuildSection("sec-b1", "role-b", "بخش سوم", 0)
            )
        )

        assertEquals(2, result.insertedTurnCount)
        assertEquals("امام حسین", dao.turnRows.values.single { it.turnNumber == 1 }.title)
        assertEquals(listOf("بخش اول", "بخش دوم"), dao.segmentRows.filter { it.turnId == result.turnIdsByRole.getValue("role-a") }.sortedBy { it.segmentIndex }.map { it.text })
        assertEquals(2, dao.participantRows.size)
    }

    @Test
    fun `workspace repository loads one flavor independent snapshot with grouped relations`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 4, sequenceIndex = 0, title = "نوبت نخست")
            participantRows += DirectorParticipantEntity(id = 1, turnId = 1, roleUid = "role-1", roleTitle = "امام حسین", isLead = true)
            segmentRows += DirectorTextSegmentEntity(id = 1, turnId = 1, segmentIndex = 0, text = "آغاز دیالوگ", sectionUid = "section-1")
            cueRows += DirectorStageCueEntity(id = 1, versionId = 20, turnId = 1, title = "حرکت", instruction = "ورود به صحنه")
        }

        val state = DaoDirectorOfficeWorkspaceRepository(dao).loadWorkspace(planId = 10, versionId = 20)

        assertEquals(10L, state.plan.id)
        assertEquals(20L, state.selectedVersion?.id)
        assertEquals(1, state.turns.size)
        assertEquals("امام حسین", state.participantsByTurn.getValue(1).single().roleTitle)
        assertEquals("آغاز دیالوگ", state.textSegmentsByTurn.getValue(1).single().text)
        assertEquals("حرکت", state.stageCues.single().title)
        assertTrue(state.searchableTextByTurn.getValue(1).contains("امام حسین"))
        assertTrue(state.searchableTextByTurn.getValue(1).contains("آغاز دیالوگ"))
    }

    @Test
    fun `workspace repository loads all dialogue segments in one bulk query`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "اول")
            turnRows[2] = DirectorTurnEntity(id = 2, versionId = 20, turnNumber = 2, sequenceIndex = 1, title = "دوم")
            segmentRows += DirectorTextSegmentEntity(id = 1, turnId = 1, segmentIndex = 0, text = "متن اول")
            segmentRows += DirectorTextSegmentEntity(id = 2, turnId = 2, segmentIndex = 0, text = "متن دوم")
        }

        val snapshot = DaoDirectorOfficeWorkspaceRepository(dao).loadWorkspace(10, 20)

        assertEquals(1, dao.bulkSegmentQueryCount)
        assertEquals("متن اول", snapshot.textSegmentsByTurn.getValue(1).single().text)
        assertEquals("متن دوم", snapshot.textSegmentsByTurn.getValue(2).single().text)
    }

    @Test
    fun `selector dialogue is created by shared operation and links source section`() = runTest {
        val dao = FakeWorkspaceDao()
        DaoDirectorOfficeWorkspaceRepository(dao).addSelectorDialogue(
            planId = 10,
            entry = DirectorOfficeSelectorEntry(
                key = "section-8:0", roleUid = "role-8", roleTitle = "حضرت زینب",
                sectionUid = "section-8", sectionTitle = "خطاب", text = "متن انتخاب‌شده",
                sourceContentHash = ContentUid.sha256("متن کامل بخش خطاب")
            )
        )

        val turn = dao.turnRows.values.single()
        assertEquals(1, turn.turnNumber)
        assertEquals("section-8:0", turn.continuityGroupUid)
        assertEquals("role-8", dao.participantRows.single().roleUid)
        assertEquals("section-8", dao.segmentRows.single().sectionUid)
        assertEquals("متن انتخاب‌شده", dao.segmentRows.single().text)
        assertEquals("role-8", dao.segmentRows.single().sourceRoleUid)
        assertEquals("خطاب", dao.segmentRows.single().sourceSectionTitle)
        assertEquals(ContentUid.sha256("متن کامل بخش خطاب"), dao.segmentRows.single().sourceContentHash)
    }

    @Test
    fun `numbered office can be saved empty when the source has no numbered dialogue`() = runTest {
        val dao = FakeWorkspaceDao()
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao = dao,
            versionId = 20,
            method = DirectorOfficeSharedContract.ROLE_NUMBERED,
            roles = listOf(DirectorOfficeBuildRole("role-a", "امام حسین", 0)),
            sections = listOf(DirectorOfficeBuildSection("sec-a", "role-a", "متنی بدون شماره نوبت", 0))
        )

        assertEquals(0, result.insertedTurnCount)
        assertTrue(dao.turnRows.isEmpty())
    }

    @Test
    fun `full text office can be saved empty when no role sections contain text`() = runTest {
        val dao = FakeWorkspaceDao()
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao = dao,
            versionId = 20,
            method = DirectorOfficeSharedContract.FULL_TEXT,
            roles = listOf(DirectorOfficeBuildRole("role-a", "امام حسین", 0)),
            sections = emptyList()
        )

        assertEquals(0, result.insertedTurnCount)
        assertTrue(dao.turnRows.isEmpty())
    }

    @Test
    fun `failed numbered rebuild preserves existing turns when no numbered source is found`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "نوبت موجود")
        }
        val error = runCatching {
            DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
                dao = dao,
                versionId = 20,
                method = DirectorOfficeSharedContract.ROLE_NUMBERED,
                roles = listOf(DirectorOfficeBuildRole("role-a", "امام حسین", 0)),
                sections = listOf(DirectorOfficeBuildSection("sec-a", "role-a", "متنی بدون شماره نوبت", 0))
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("نوبت موجود", dao.turnRows.getValue(1).title)
    }

    @Test
    fun `duplicate office titles are assigned a readable ordinal suffix`() {
        assertEquals(
            "دفتر مجلس",
            DirectorOfficeWorkspaceUseCases.uniqueOfficeTitle("دفتر مجلس", emptyList())
        )
        assertEquals(
            "دفتر مجلس (۳)",
            DirectorOfficeWorkspaceUseCases.uniqueOfficeTitle("دفتر مجلس", listOf("دفتر مجلس", "دفتر مجلس (۲)"))
        )
    }

    @Test
    fun `manual builder creates a raw empty draft when no turns exist`() = runTest {
        val dao = FakeWorkspaceDao()
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao, 20, DirectorOfficeSharedContract.MANUAL, emptyList(), emptyList()
        )
        assertTrue(dao.turnRows.isEmpty())
        assertEquals(0, result.insertedTurnCount)
    }

    @Test
    fun `manual builder does not delete already recorded turns`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 5, sequenceIndex = 0, title = "حفظ شود")
        }
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao, 20, DirectorOfficeSharedContract.MANUAL, emptyList(), emptyList()
        )
        assertEquals(1, dao.turnRows.size)
        assertEquals(0, result.insertedTurnCount)
    }

    @Test
    fun `selector builder clears current turns but does not create turns`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 5, sequenceIndex = 0, title = "پاک شود")
        }
        val result = DirectorOfficeWorkspaceOperations.buildDraftFromLibrary(
            dao, 20, DirectorOfficeSharedContract.SELECTOR,
            listOf(DirectorOfficeBuildRole("role-a", "نقش", 0)),
            listOf(DirectorOfficeBuildSection("sec-a", "role-a", "متن", 0))
        )
        assertTrue(dao.turnRows.isEmpty())
        assertEquals(0, result.insertedTurnCount)
    }

    @Test
    fun `archived office cannot be edited through shared operations`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            planRow = planRow.copy(status = DirectorPlanEntity.STATUS_ARCHIVED)
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "اول")
        }
        val error = runCatching {
            DirectorOfficeWorkspaceOperations.updateTurn(dao, dao.turnRows.getValue(1).copy(turnNumber = 2))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(1, dao.turnRows.getValue(1).turnNumber)
    }

    @Test
    fun `admin may edit a draft version while the parent plan has a published version`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            planRow = planRow.copy(status = DirectorPlanEntity.STATUS_PUBLISHED)
        }
        DirectorOfficeWorkspaceOperations.addRoleLinkedTurn(
            dao = dao,
            versionId = 20,
            turnNumber = 1,
            roleUid = "role-2",
            roleTitle = "امام حسین",
            text = "متن نسخه جدید",
            allowPublishedPlan = true,
            transactionRunner = DirectDirectorOfficeTransactionRunner
        )
        assertEquals(1, dao.turnRows.size)
    }

    @Test
    fun `published version stays immutable even when admin allows published parent plan`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            versionRow = versionRow.copy(status = DirectorPlanVersionEntity.STATUS_PUBLISHED)
        }
        val error = runCatching {
            DirectorOfficeWorkspaceOperations.addRoleLinkedTurn(
                dao = dao,
                versionId = 20,
                turnNumber = 1,
                roleUid = "role-2",
                roleTitle = "امام حسین",
                text = "متن",
                allowPublishedPlan = true,
                transactionRunner = DirectDirectorOfficeTransactionRunner
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(dao.turnRows.isEmpty())
    }

    @Test
    fun `stage cue cannot point to a turn in another version`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[99] = DirectorTurnEntity(id = 99, versionId = 999, turnNumber = 1, sequenceIndex = 0, title = "نسخه دیگر")
        }
        val error = runCatching {
            DirectorOfficeWorkspaceOperations.addStageCue(dao, 20, "راهنما", "شرح", 99)
        }.exceptionOrNull()

        assertTrue(error != null)
        assertTrue(dao.cueRows.isEmpty())
    }

    @Test
    fun `dialogue edit is recorded and previous text can be restored`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "امام حسین")
            segmentRows += DirectorTextSegmentEntity(id = 1, turnId = 1, segmentIndex = 0, text = "متن اولیه", sectionUid = "section-1")
        }
        val repository = DaoDirectorOfficeWorkspaceRepository(dao)

        repository.updateTextSegment(dao.segmentRows.single().copy(text = "متن اصلاح‌شده"))
        val edit = dao.historyRows.single()
        assertEquals("DIALOGUE", edit.entityType)
        assertEquals("متن اصلاح‌شده", dao.segmentRows.single().text)

        repository.restoreChangeHistory(edit.id)
        assertEquals("متن اولیه", dao.segmentRows.single().text)
        assertEquals("role-a", dao.segmentRows.single().sourceRoleUid)
        assertEquals("section-1", dao.segmentRows.single().sourceSectionUids)
        assertEquals("RESTORE", dao.historyRows.last().changeType)
        assertEquals(edit.id, dao.historyRows.last().restoredFromId)
    }

    @Test
    fun `role changes are recorded and previous participants can be restored`() = runTest {
        val dao = FakeWorkspaceDao().apply {
            turnRows[1] = DirectorTurnEntity(id = 1, versionId = 20, turnNumber = 1, sequenceIndex = 0, title = "نوبت")
            participantRows += DirectorParticipantEntity(id = 1, turnId = 1, roleUid = "role-a", roleTitle = "نقش اول", isLead = true)
        }
        val repository = DaoDirectorOfficeWorkspaceRepository(dao)

        repository.updateParticipant(dao.participantRows.single().copy(roleUid = "role-b", roleTitle = "نقش دوم"))
        val edit = dao.historyRows.single()
        assertEquals("ROLE", edit.entityType)
        assertEquals("role-b", dao.participantRows.single().roleUid)

        repository.restoreChangeHistory(edit.id)
        assertEquals("role-a", dao.participantRows.single().roleUid)
        assertEquals("نقش اول", dao.participantRows.single().roleTitle)
    }

    private class FakeWorkspaceDao : DirectorOfficeWorkspaceDao {
        var planRow = DirectorPlanEntity(
            id = 10, uid = "plan-10", taziehUid = "tazieh-1", title = "دفتر آزمایشی",
            status = DirectorPlanEntity.STATUS_DRAFT, createdAt = 1, updatedAt = 1
        )
        var versionRow = DirectorPlanVersionEntity(
            id = 20, planId = 10, versionNumber = 1, title = "نسخه پایه",
            status = DirectorPlanVersionEntity.STATUS_DRAFT, createdAt = 1
        )
        val turnRows = linkedMapOf<Long, DirectorTurnEntity>()
        val participantRows = mutableListOf<DirectorParticipantEntity>()
        val segmentRows = mutableListOf<DirectorTextSegmentEntity>()
        val cueRows = mutableListOf<DirectorStageCueEntity>()
        val historyRows = mutableListOf<DirectorChangeHistoryEntity>()
        private var nextHistoryId = 1L
        private var nextTurnId = 100L
        var bulkSegmentQueryCount = 0
            private set

        override suspend fun plan(id: Long) = planRow.takeIf { it.id == id }
        override suspend fun versionsForPlan(planId: Long) = listOf(versionRow).filter { it.planId == planId }
        override suspend fun softDeletePlan(id: Long, deletedAt: Long) { if (planRow.id == id) planRow = planRow.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = deletedAt) }
        override suspend fun insertParticipant(value: DirectorParticipantEntity): Long {
            participantRows += value.copy(id = participantRows.size + 1L)
            return participantRows.last().id
        }
        override suspend fun updateParticipant(value: DirectorParticipantEntity) {
            val index = participantRows.indexOfFirst { it.id == value.id }
            if (index >= 0) participantRows[index] = value
        }
        override suspend fun deleteParticipant(id: Long) { participantRows.removeAll { it.id == id } }
        override suspend fun deleteTurn(id: Long) {
            turnRows.remove(id)
            participantRows.removeAll { it.turnId == id }
            segmentRows.removeAll { it.turnId == id }
        }
        override suspend fun deleteTextSegment(id: Long) { segmentRows.removeAll { it.id == id } }
        override suspend fun stageCue(id: Long) = cueRows.firstOrNull { it.id == id }
        override suspend fun deleteStageCue(id: Long) { cueRows.removeAll { it.id == id } }
        override suspend fun stageCues(versionId: Long) = cueRows.filter { it.versionId == versionId }
        override suspend fun version(id: Long) = versionRow.takeIf { it.id == id }
        override suspend fun turn(id: Long) = turnRows[id]
        override suspend fun participants(turnId: Long) = participantRows.filter { it.turnId == turnId }
        override suspend fun participantsForTurns(turnIds: List<Long>) = participantRows.filter { it.turnId in turnIds }
        override suspend fun textSegments(turnId: Long) = segmentRows.filter { it.turnId == turnId }
        override suspend fun textSegmentsForTurns(turnIds: List<Long>): List<DirectorTextSegmentEntity> {
            bulkSegmentQueryCount++
            return segmentRows.filter { it.turnId in turnIds }.sortedWith(compareBy<DirectorTextSegmentEntity> { it.turnId }.thenBy { it.segmentIndex }.thenBy { it.id })
        }
        override suspend fun turns(versionId: Long) = turnRows.values.filter { it.versionId == versionId }.sortedBy { it.sequenceIndex }
        override suspend fun updatePlan(value: DirectorPlanEntity) { planRow = value }
        override suspend fun insertTurn(value: DirectorTurnEntity): Long {
            val id = if (value.id != 0L) value.id else nextTurnId++
            turnRows[id] = value.copy(id = id)
            return id
        }
        override suspend fun updateTurn(value: DirectorTurnEntity) { turnRows[value.id] = value }
        override suspend fun moveTurn(turnId: Long, sequenceIndex: Int) {
            turnRows[turnId]?.let { turnRows[turnId] = it.copy(sequenceIndex = sequenceIndex) }
        }
        override suspend fun insertTextSegment(value: DirectorTextSegmentEntity): Long {
            segmentRows += value.copy(id = segmentRows.size + 1L)
            return segmentRows.last().id
        }
        override suspend fun updateTextSegment(value: DirectorTextSegmentEntity) {
            val index = segmentRows.indexOfFirst { it.id == value.id }
            if (index >= 0) segmentRows[index] = value
        }
        override suspend fun insertStageCue(value: DirectorStageCueEntity): Long {
            cueRows += value.copy(id = cueRows.size + 1L)
            return cueRows.last().id
        }
        override suspend fun updateStageCue(value: DirectorStageCueEntity) {
            val index = cueRows.indexOfFirst { it.id == value.id }
            if (index >= 0) cueRows[index] = value
        }
        override suspend fun insertChangeHistory(value: DirectorChangeHistoryEntity): Long {
            val id = nextHistoryId++
            historyRows += value.copy(id = id)
            return id
        }
        override suspend fun changeHistory(planId: Long, turnId: Long?): List<DirectorChangeHistoryEntity> =
            historyRows.filter { it.planId == planId && (turnId == null || it.turnId == turnId) }
                .sortedByDescending { it.changedAt }
        override suspend fun changeHistoryEntry(id: Long): DirectorChangeHistoryEntity? = historyRows.firstOrNull { it.id == id }
    }
}
