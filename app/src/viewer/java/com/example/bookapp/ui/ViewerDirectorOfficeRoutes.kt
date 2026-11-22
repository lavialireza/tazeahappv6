package com.example.bookapp.ui

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.bookapp.data.AppDatabase

fun NavGraphBuilder.viewerOnlyRoutes(
    navController: NavHostController,
    context: Context,
    db: AppDatabase,
    permissions: Map<String, Boolean>
) {
    composable("viewer_director_office") {
        ViewerDirectorOfficeScreen(contentDb = db, taziehId = null, policyPermissions = permissions, onBack = { navController.popBackStack() })
    }
    composable("viewer_director_office/{taziehId}") { entry ->
        val taziehId = entry.arguments?.getString("taziehId")?.toLongOrNull()
        ViewerDirectorOfficeScreen(contentDb = db, taziehId = taziehId, policyPermissions = permissions, onBack = { navController.popBackStack() })
    }
}
