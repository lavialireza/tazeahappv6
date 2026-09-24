package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** قرارداد Stage79: Viewer فقط داده‌های شخصی را به Sync Server ارسال می‌کند. */
class ViewerSyncWritePolicyTest {
    @Test
    fun personalWhitelistContainsOnlyLocalUserData() {
        val source = java.io.File(
            "src/main/java/com/example/bookapp/data/SyncServerHelper.kt"
        ).readText()
        val start = source.indexOf("private val PERSONAL_KEYS")
        val end = source.indexOf("private val CONTENT_KEYS")
        val block = source.substring(start, end)
        listOf(
            "notes", "bookmarks", "sectionTags", "recentSections",
            "readingHistory", "myRoles", "activeDays"
        ).forEach { key -> assertTrue(block.contains("\"$key\"")) }
    }

    @Test
    fun contentKeysAreNotInViewerPersonalWhitelist() {
        val source = java.io.File(
            "src/main/java/com/example/bookapp/data/SyncServerHelper.kt"
        ).readText()
        val start = source.indexOf("private val PERSONAL_KEYS")
        val end = source.indexOf("private fun deviceId")
        val block = source.substring(start, end)
        listOf(
            "footnotes", "dialogues", "dialogueTurns", "images",
            "audios", "corrections", "glossary"
        ).forEach { key ->
            assertFalse(block.substring(0, block.indexOf("private val CONTENT_KEYS"))
                .contains("\"$key\""))
        }
    }
}
