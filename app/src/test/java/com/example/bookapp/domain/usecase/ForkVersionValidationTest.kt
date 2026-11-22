package com.example.bookapp.domain.usecase

import com.example.bookapp.domain.model.ForkTaziehVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForkVersionValidationTest {
    @Test fun rejectsMissingSource() {
        assertEquals("نسخه مبدأ مشخص نشده است.", ArchiveValidation.validateFork(ForkTaziehVersion("", "نسخه دوم")))
    }

    @Test fun rejectsBlankTitle() {
        assertEquals("عنوان نسخه جدید الزامی است.", ArchiveValidation.validateFork(ForkTaziehVersion("v1", "  ")))
    }

    @Test fun rejectsOverlongTitle() {
        assertEquals("عنوان نسخه نباید بیشتر از ۲۰۰ نویسه باشد.", ArchiveValidation.validateFork(ForkTaziehVersion("v1", "ا".repeat(201))))
    }

    @Test fun acceptsValidFork() {
        assertNull(ArchiveValidation.validateFork(ForkTaziehVersion("v1", "نسخه دوم", "اصلاح متن")))
    }
}
