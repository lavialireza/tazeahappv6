package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectorOfficeAccessRulesTest {
    @Test fun eachBuildMethodNeedsBothCreateAndExplicitMethodPermission() {
        val permissions = mutableMapOf(
            "director.personal.create" to true,
            "director.personal.method.number" to false,
            "director.personal.method.fulltext" to true,
            "director.personal.method.manual" to false,
            "director.personal.method.selector" to false
        )
        assertFalse(DirectorOfficeAccessRules.canCreatePersonal(permissions, "ROLE_NUMBERED"))
        assertTrue(DirectorOfficeAccessRules.canCreatePersonal(permissions, "FULL_TEXT"))
        assertFalse(DirectorOfficeAccessRules.canCreatePersonal(permissions, "MANUAL"))
        permissions["director.personal.create"] = false
        assertFalse(DirectorOfficeAccessRules.canCreatePersonal(permissions, "FULL_TEXT"))
    }

    @Test fun sharedBuildMethodContractIsSingleSourceForBothFlavors() {
        assertTrue(DirectorOfficeSharedContract.methodCodes.containsAll(setOf("ROLE_NUMBERED", "FULL_TEXT", "MANUAL", "SELECTOR")))
        assertTrue(DirectorOfficeSharedContract.method("MANUAL")?.requiresLibrarySource == false)
        assertTrue(DirectorOfficeSharedContract.method("ROLE_NUMBERED")?.requiresLibrarySource == true)
        assertTrue(DirectorOfficeSharedContract.method("FULL_TEXT")?.requiresLibrarySource == true)
        assertTrue(DirectorOfficeSharedContract.method("SELECTOR")?.requiresLibrarySource == true)
    }

    @Test fun unknownBuildMethodIsNeverGranted() {
        val permissions = mapOf("director.personal.create" to true)
        assertFalse(DirectorOfficeSharedContract.canCreatePersonal(permissions, "UNKNOWN"))
    }

    @Test fun manualPersonalOfficeCreationDoesNotRequireOfficialOfficeViewPermission() {
        val permissions = mapOf(
            "director.personal.create" to true,
            "director.personal.method.manual" to true,
            "director.official.view" to false
        )
        assertTrue(DirectorOfficeAccessRules.canCreatePersonal(permissions, "MANUAL"))
    }

    @Test fun viewerCannotPublishCopyOrExportEvenIfPolicyContainsTrueValues() {
        val permissions = mapOf(
            "director.official.publish" to true,
            "director.copy" to true,
            "director.export" to true
        )
        assertFalse(DirectorOfficeAccessRules.canPublishOfficial(permissions))
        assertFalse(DirectorOfficeAccessRules.canCopyOrExport(permissions))
    }
}
