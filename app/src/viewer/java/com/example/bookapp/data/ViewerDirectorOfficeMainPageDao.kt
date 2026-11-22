package com.example.bookapp.data

/** اعمال سیاست دسترسی در لایه DAO؛ پنهان‌کردن دکمه به‌تنهایی مجوز نوشتن ایجاد نمی‌کند. */
class ViewerDirectorOfficeMainPageDao(
    private val delegate: ViewerDirectorOfficeDao,
    private val permissions: Map<String, Boolean>
) : DirectorOfficeMainPageDao by delegate {

    private fun requireAny(vararg keys: String) {
        require(keys.any { ViewerPermissionEngine.can(permissions, it) }) {
            "مجوز لازم برای این عملیات دفتر معین‌البکاء فعال نیست."
        }
    }

    private fun requireWrite() = requireAny("director.personal.create", "director.personal.edit")

    override suspend fun insertPlan(value: DirectorPlanEntity): Long {
        requireAny("director.personal.create")
        return delegate.insertPlan(value)
    }

    override suspend fun insertVersion(value: DirectorPlanVersionEntity): Long {
        requireWrite()
        require(value.status != DirectorPlanVersionEntity.STATUS_PUBLISHED) {
            "انتشار نسخه رسمی فقط از Admin امکان‌پذیر است."
        }
        return delegate.insertVersion(value)
    }

    override suspend fun updateVersion(value: DirectorPlanVersionEntity) {
        requireWrite()
        require(value.status != DirectorPlanVersionEntity.STATUS_PUBLISHED) {
            "انتشار نسخه رسمی فقط از Admin امکان‌پذیر است."
        }
        delegate.updateVersion(value)
    }

    override suspend fun insertSourceFile(value: DirectorSourceFileEntity): Long {
        requireAny("director.source.import")
        return delegate.insertSourceFile(value)
    }

    override suspend fun updatePlan(value: DirectorPlanEntity) {
        requireWrite()
        delegate.updatePlan(value)
    }

    override suspend fun softDeletePlan(id: Long, deletedAt: Long) {
        requireAny("director.personal.edit")
        delegate.softDeletePlan(id, deletedAt)
    }

    override suspend fun insertTurn(value: DirectorTurnEntity): Long {
        requireWrite()
        return delegate.insertTurn(value)
    }

    override suspend fun insertTurns(values: List<DirectorTurnEntity>): List<Long> {
        requireWrite()
        return delegate.insertTurns(values)
    }

    override suspend fun updateTurn(value: DirectorTurnEntity) {
        requireAny("director.personal.edit", "director.personal.create")
        delegate.updateTurn(value)
    }

    override suspend fun deleteTurn(id: Long) {
        requireAny("director.personal.edit")
        delegate.deleteTurn(id)
    }

    override suspend fun moveTurn(turnId: Long, sequenceIndex: Int) {
        requireAny("director.personal.edit")
        delegate.moveTurn(turnId, sequenceIndex)
    }

    override suspend fun insertParticipant(value: DirectorParticipantEntity): Long {
        requireWrite()
        return delegate.insertParticipant(value)
    }

    override suspend fun insertParticipants(values: List<DirectorParticipantEntity>): List<Long> {
        requireWrite()
        return delegate.insertParticipants(values)
    }

    override suspend fun updateParticipant(value: DirectorParticipantEntity) {
        requireAny("director.personal.edit")
        delegate.updateParticipant(value)
    }

    override suspend fun deleteParticipant(id: Long) {
        requireAny("director.personal.edit")
        delegate.deleteParticipant(id)
    }

    override suspend fun insertTextSegment(value: DirectorTextSegmentEntity): Long {
        requireWrite()
        return delegate.insertTextSegment(value)
    }

    override suspend fun insertTextSegments(values: List<DirectorTextSegmentEntity>): List<Long> {
        requireWrite()
        return delegate.insertTextSegments(values)
    }

    override suspend fun updateTextSegment(value: DirectorTextSegmentEntity) {
        requireAny("director.personal.edit", "director.personal.create")
        delegate.updateTextSegment(value)
    }

    override suspend fun deleteTextSegment(id: Long) {
        requireAny("director.personal.edit")
        delegate.deleteTextSegment(id)
    }

    override suspend fun insertStageCue(value: DirectorStageCueEntity): Long {
        requireWrite()
        return delegate.insertStageCue(value)
    }

    override suspend fun updateStageCue(value: DirectorStageCueEntity) {
        requireAny("director.personal.edit", "director.personal.create")
        delegate.updateStageCue(value)
    }

    override suspend fun deleteStageCue(id: Long) {
        requireAny("director.personal.edit")
        delegate.deleteStageCue(id)
    }

    override suspend fun insertChangeHistory(value: DirectorChangeHistoryEntity): Long {
        requireAny("director.personal.edit", "director.personal.create")
        return delegate.insertChangeHistory(value)
    }
}
