package com.example.bookapp.data

import android.content.Context

/** Static/in-memory QA for the parent/child permission contract. Does not modify stored policy. */
object PermissionQa {
    data class Result(val passed: Int, val failed: Int, val failures: List<String>)

    fun run(context: Context): Result {
        val failures = mutableListOf<String>()
        val base = ViewerAccessPolicy.defaultPermissions().toMutableMap()
        val keys = ViewerAccessPolicy.permissionLabels.keys.toList()
        if (keys.size != 65) failures += "تعداد مجوزها ${keys.size} است؛ انتظار 65 مورد بود."
        ViewerAccessPolicy.permissionParents.forEach { (child, parent) ->
            if (child !in ViewerAccessPolicy.permissionLabels) failures += "فرزند ناشناخته: $child"
            if (parent !in ViewerAccessPolicy.permissionLabels) failures += "والد ناشناخته: $parent"
        }
        ViewerAccessPolicy.permissionParents.forEach { (child, parent) ->
            val childOn = base + (child to true) + (parent to false)
            if (ViewerAccessPolicy.hasPermissionForTest(childOn, child)) failures += "والد خاموش، فرزند روشن هنوز مؤثر است: $child"
            val both = base + (parent to true) + (child to true)
            if (!ViewerAccessPolicy.hasPermissionForTest(both, child)) failures += "والد و فرزند روشن، فرزند مؤثر نیست: $child"
        }
        val addOff = base + ("bookmarks" to true) + ("bookmarks.add" to false) + ("bookmarks.delete" to true)
        if (ViewerAccessPolicy.hasPermissionForTest(addOff, "bookmarks.add")) failures += "bookmarks.add مستقل خاموش نمی‌شود."
        if (!ViewerAccessPolicy.hasPermissionForTest(addOff, "bookmarks.delete")) failures += "bookmarks.delete مستقل روشن نمی‌شود."
        return Result(keys.size * 2 + 2 - failures.size, failures.size, failures)
    }
}
