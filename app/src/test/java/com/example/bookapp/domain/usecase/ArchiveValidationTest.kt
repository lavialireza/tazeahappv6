package com.example.bookapp.domain.usecase

import com.example.bookapp.domain.model.NewSourceDocument
import com.example.bookapp.domain.model.NewTaziehVersion
import com.example.bookapp.domain.model.SourceDocumentType
import com.example.bookapp.domain.model.VersionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveValidationTest {
    @Test fun `version requires stable tazieh id and title`() {
        assertEquals("شناسه تعزیه مشخص نشده است.", ArchiveValidation.validateVersion(NewTaziehVersion("", "عنوان")))
        assertEquals("عنوان نسخه الزامی است.", ArchiveValidation.validateVersion(NewTaziehVersion("t-1", " ")))
        assertNull(ArchiveValidation.validateVersion(NewTaziehVersion("t-1", "نسخه اول")))
    }

    @Test fun `source requires valid sha256 and positive page count`() {
        val base = NewSourceDocument("v-1", "اصل.pdf", "application/pdf", "x", "archive/original.pdf")
        assertEquals("هش SHA-256 معتبر نیست.", ArchiveValidation.validateSource(base))
        assertEquals("تعداد صفحه باید حداقل یک باشد.", ArchiveValidation.validateSource(base.copy(sha256 = "a".repeat(64), pageCount = 0)))
        assertNull(ArchiveValidation.validateSource(base.copy(sha256 = "A".repeat(64), pageCount = 1, documentType = SourceDocumentType.ORIGINAL)))
    }
    @Test fun `published archive version is immutable and draft cannot skip review`() {
        assertEquals(false, ArchiveValidation.isAllowedVersionTransition(VersionState.PUBLISHED, VersionState.DRAFT))
        assertEquals(false, ArchiveValidation.isAllowedVersionTransition(VersionState.DRAFT, VersionState.PUBLISHED))
        assertEquals(true, ArchiveValidation.isAllowedVersionTransition(VersionState.DRAFT, VersionState.REVIEWED))
        assertEquals(true, ArchiveValidation.isAllowedVersionTransition(VersionState.REVIEWED, VersionState.PUBLISHED))
    }
}
