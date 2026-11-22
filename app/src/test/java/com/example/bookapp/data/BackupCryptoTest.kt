package com.example.bookapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun `backup encryption round trips and uses fresh salt and iv`() {
        val original = "Tazieh backup v4 — داده آزمایشی"
        val first = encryptBackupText(original, "correct horse battery staple")
        val second = encryptBackupText(original, "correct horse battery staple")

        assertEquals(original, decryptBackupBytes(first, "correct horse battery staple"))
        assertNotEquals(first.toList(), second.toList())
        assertThrows(Exception::class.java) {
            decryptBackupBytes(first, "wrong password")
        }
    }

    @Test
    fun `truncated encrypted backup is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            decryptBackupBytes(ByteArray(20), "password")
        }
    }
}
