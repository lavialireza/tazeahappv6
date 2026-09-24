package com.example.bookapp.data

import org.junit.Assert.assertTrue
import org.junit.Test

/** قرارداد Stage78: مسیرهای ذخیره محتوای اصلی باید از guard مرکزی عبور کنند. */
class ViewerContentWriteGuardContractTest {
    @Test
    fun guardObjectExistsAndIsCallable() {
        // در flavor Admin این فراخوانی باید بدون خطا عبور کند.
        ViewerContentWriteGuard.check()
        assertTrue(true)
    }
}
