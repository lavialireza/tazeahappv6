package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Default Viewer policy mirrors the user-operable capabilities of the Admin Director Office page. */
class ViewerDirectorOfficeDefaultPermissionsTest {
    private val userFacingDirectorPermissions = listOf(
        "director.official.view",
        "director.personal.create",
        "director.personal.method.number",
        "director.personal.method.fulltext",
        "director.personal.method.manual",
        "director.personal.method.selector",
        "director.personal.edit",
        "director.source.import"
    )

    @Test
    fun userFacingDirectorOfficePermissionsAreEnabledByDefault() {
        val defaults = ViewerAccessPolicy.defaultPermissions()
        userFacingDirectorPermissions.forEach { key ->
            assertTrue("Expected $key to be enabled by default", defaults[key] == true)
            assertTrue("Expected $key to be effective by default", ViewerPermissionEngine.can(defaults, key))
        }
    }

    @Test
    fun importedPolicyCanDisableEachDirectorOfficePermission() {
        val importedPolicy = ViewerAccessPolicy.defaultPermissions().toMutableMap()
        userFacingDirectorPermissions.forEach { key ->
            importedPolicy[key] = false
            assertFalse("Expected imported policy to disable $key", ViewerPermissionEngine.can(importedPolicy, key))
            importedPolicy[key] = true
        }
    }

    @Test
    fun omittedKeysInOlderPoliciesInheritDirectorOfficeDefaults() {
        val normalized = ViewerAccessPolicy.normalizedPermissions(mapOf("read" to true))
        userFacingDirectorPermissions.forEach { key ->
            assertTrue("Expected omitted legacy key $key to inherit the current default", normalized[key] == true)
        }
    }

    @Test
    fun explicitFalseInPolicyStillDisablesTheSelectedCapability() {
        val policy = ViewerAccessPolicy.normalizedPermissions(
            mapOf("director.personal.create" to true, "director.personal.edit" to false)
        )
        assertFalse(ViewerPermissionEngine.can(policy, "director.personal.edit"))
        assertTrue(ViewerPermissionEngine.can(policy, "director.personal.method.manual"))

        val methodDisabled = ViewerAccessPolicy.normalizedPermissions(
            mapOf("director.personal.create" to true, "director.personal.method.manual" to false)
        )
        assertFalse(ViewerPermissionEngine.can(methodDisabled, "director.personal.method.manual"))
    }

    @Test
    fun publishingCopyAndExportRemainAdminOnly() {
        val defaults = ViewerAccessPolicy.defaultPermissions()
        listOf("director.official.publish", "director.copy", "director.export").forEach { key ->
            assertFalse("$key must not be granted to Viewer", defaults[key] == true)
            assertFalse("$key must never be effective in Viewer", ViewerPermissionEngine.can(defaults, key))
        }
    }
}
