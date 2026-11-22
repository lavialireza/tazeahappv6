package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorOfficeWorkspaceUseCasesTest {
    @Test fun normalizesPersianAndArabicDigits() {
        assertEquals("123", DirectorOfficeWorkspaceUseCases.normalizeNumber("۱۲٣"))
    }

    @Test fun splitsDialoguesAndPreservesInternalLines() {
        assertEquals(
            listOf("بیت اول\nبیت دوم", "دیالوگ دوم"),
            DirectorOfficeWorkspaceUseCases.splitDialogues("بیت اول\nبیت دوم\n\nدیالوگ دوم")
        )
    }

    @Test fun parsesNumberedDialoguesWithPersianDigits() {
        assertEquals(
            listOf(1 to "سلام\nادامه متن", 2 to "پاسخ"),
            DirectorOfficeWorkspaceUseCases.parseNumberedDialogues("۱- سلام\nادامه متن\nدیالوگ ۲: پاسخ")
        )
    }

    @Test fun ignoresUnnumberedTextForNumberedMethod() {
        assertTrue(DirectorOfficeWorkspaceUseCases.parseNumberedDialogues("متن بدون شماره").isEmpty())
    }
    @Test fun allowsEditingDraftVersionOfPublishedPlan() {
        val plan = DirectorPlanEntity(
            uid = "plan", taziehUid = "tazieh", title = "مجلس",
            status = DirectorPlanEntity.STATUS_PUBLISHED,
            createdAt = 1L, updatedAt = 1L
        )
        val draft = DirectorPlanVersionEntity(
            planId = 1L, versionNumber = 2, title = "پیش‌نویس",
            status = DirectorPlanVersionEntity.STATUS_DRAFT, createdAt = 2L
        )
        DirectorOfficeWorkspaceUseCases.requireDraft(plan, draft)
    }

    @Test fun rejectsEditingPublishedVersion() {
        val plan = DirectorPlanEntity(
            uid = "plan", taziehUid = "tazieh", title = "مجلس",
            status = DirectorPlanEntity.STATUS_PUBLISHED,
            createdAt = 1L, updatedAt = 1L
        )
        val published = DirectorPlanVersionEntity(
            planId = 1L, versionNumber = 1, title = "رسمی",
            status = DirectorPlanVersionEntity.STATUS_PUBLISHED, createdAt = 1L, publishedAt = 1L
        )
        val result = runCatching { DirectorOfficeWorkspaceUseCases.requireDraft(plan, published) }
        assertTrue(result.isFailure)
    }

}
