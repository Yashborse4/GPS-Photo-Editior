package com.gpsphotocamera.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gpsphotocamera.app.ui.screens.editor.PhotoEditorScreen
import com.gpsphotocamera.app.ui.screens.home.HomeScreen
import com.gpsphotocamera.app.ui.screens.preview.PreviewScreen
import com.gpsphotocamera.app.viewmodel.EditorViewModel
import com.gpsphotocamera.app.viewmodel.HomeViewModel
import com.gpsphotocamera.app.viewmodel.PreviewViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // Shared EditorViewModel across Editor and Preview
    val editorViewModel: EditorViewModel = viewModel()
    val previewViewModel: PreviewViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // 1. Home Screen
        composable(route = Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onPhotoSelectedForEditing = { uri ->
                    navController.navigate(Screen.Editor.createRoute(uri.toString()))
                },
                onViewRecentPhoto = { uriString ->
                    // Open recent photo directly in editor
                    navController.navigate(Screen.Editor.createRoute(uriString))
                }
            )
        }

        // 2. Photo Editor Screen
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("photoUri") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("photoUri") ?: ""
            val decodedUri = Uri.decode(encodedUri)

            PhotoEditorScreen(
                viewModel = editorViewModel,
                initialPhotoUri = decodedUri,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.route)
                }
            )
        }

        // 3. Preview Screen
        composable(route = Screen.Preview.route) {
            val photoData by editorViewModel.photoData.collectAsState()

            PreviewScreen(
                viewModel = previewViewModel,
                photoData = photoData,
                onNavigateBackToEdit = {
                    navController.popBackStack()
                },
                onSelectNewPhoto = { newUri ->
                    // Change photo while preserving current GPS location and stamp styling
                    editorViewModel.changePhotoUri(newUri.toString(), preserveLocationSettings = true)
                    navController.popBackStack()
                }
            )
        }
    }
}
