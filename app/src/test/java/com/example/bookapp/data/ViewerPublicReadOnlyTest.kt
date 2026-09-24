package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** قرارداد مجوزهای محتوای اصلی برای Viewer عمومی. */
class ViewerPublicReadOnlyTest {
    @Test
    fun protectedContentWritesAreDenied() {
        val permissions = ViewerAccessPolicy.defaultPermissions().toMutableMap()
        setOf(
            "gallery.add", "gallery.edit", "gallery.delete",
            "footnotes.add", "footnotes.edit", "footnotes.delete",
            "footnoteSync.dictionary",
            "dictionary.add", "dictionary.edit", "dictionary.delete",
            "taziehCorrections.add", "taziehCorrections.edit",
            "taziehCorrections.delete", "taziehCorrections.apply"
        ).forEach { permissions[it] = false }

        assertFalse(permissions["dictionary.add"] == true)
        assertFalse(permissions["dictionary.edit"] == true)
        assertFalse(permissions["dictionary.delete"] == true)
        assertFalse(permissions["taziehCorrections.apply"] == true)
        assertFalse(permissions["gallery.delete"] == true)
        assertFalse(permissions["footnotes.edit"] == true)
    }

    @Test
    fun personalViewerFeaturesRemainIndependent() {
        val permissions = ViewerAccessPolicy.defaultPermissions()
        assertTrue(permissions["notes.add"] == true)
        assertTrue(permissions["bookmarks.add"] == true)
        assertTrue(permissions["myRole.select"] == true)
    }
}
