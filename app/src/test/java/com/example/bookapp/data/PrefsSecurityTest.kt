package com.example.bookapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrefsSecurityTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `new password is hashed and verifies without exposing plaintext`() {
        Prefs.setAppPassword(context, "12345")
        val stored = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("app_password", "") ?: ""

        assertTrue(stored.startsWith("v2$"))
        assertFalse(stored.contains("12345"))
        assertTrue(Prefs.hasAppPassword(context))
        assertTrue(Prefs.verifyAppPassword(context, "12345"))
        assertFalse(Prefs.verifyAppPassword(context, "54321"))
    }

    @Test
    fun `legacy plaintext password is upgraded after successful verification`() {
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("app_password", "legacy-secret").commit()

        assertTrue(Prefs.verifyAppPassword(context, "legacy-secret"))
        val stored = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("app_password", "") ?: ""
        assertTrue(stored.startsWith("v2$"))
        assertFalse(stored.contains("legacy-secret"))
    }
}
