package jp.riverapp.hexlide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.lifecycle.Lifecycle
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.presentation.localization.LocalizationManager
import jp.riverapp.hexlide.presentation.screen.game.GameScreen
import jp.riverapp.hexlide.presentation.screen.online.OnlineGameScreen
import jp.riverapp.hexlide.presentation.screen.online.OnlineLobbyScreen
import jp.riverapp.hexlide.presentation.screen.settings.InAppWebViewScreen
import jp.riverapp.hexlide.presentation.screen.settings.SettingsScreen
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameViewModel

@Composable
fun HexlideNavHost(
    localizationManager: LocalizationManager,
) {
    val navController = rememberNavController()
    val language by localizationManager.language.collectAsStateWithLifecycle()
    val strings = remember(language) { localizationManager.strings }

    NavHost(
        navController = navController,
        startDestination = Screen.Game.route,
    ) {
        // Game screen (local)
        composable(Screen.Game.route) { backStackEntry ->
            val viewModel: LocalGameViewModel = hiltViewModel()

            // ロビーから戻った際にAIモードに切り替え
            LaunchedEffect(Unit) {
                backStackEntry.savedStateHandle.getStateFlow("switchToAI", false)
                    .collect { shouldSwitch ->
                        if (shouldSwitch) {
                            viewModel.switchMode(GameMode.AI)
                            backStackEntry.savedStateHandle["switchToAI"] = false
                        }
                    }
            }

            GameScreen(
                viewModel = viewModel,
                localizationManager = localizationManager,
                onNavigateToOnline = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.OnlineLobby.route)
                    }
                },
                onNavigateToSettings = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.Settings.route)
                    }
                },
            )
        }

        // Online lobby
        composable(Screen.OnlineLobby.route) { backStackEntry ->
            OnlineLobbyScreen(
                strings = strings,
                onNavigateToGame = { gameId ->
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.OnlineGame.createRoute(gameId)) {
                            popUpTo(Screen.OnlineLobby.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onBack = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle?.set("switchToAI", true)
                        navController.popBackStack()
                    }
                },
            )
        }

        // Online game (with deep link support)
        composable(
            route = Screen.OnlineGame.route,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://hexlide.riverapp.jp/game/{gameId}"
                },
            ),
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
            OnlineGameScreen(
                gameId = gameId,
                strings = strings,
                onBack = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.OnlineLobby.route) {
                            popUpTo(Screen.Game.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        // Settings
        composable(Screen.Settings.route) { backStackEntry ->
            SettingsScreen(
                localizationManager = localizationManager,
                onBack = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.popBackStack()
                    }
                },
                onNavigateToWebView = { url ->
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.navigate(Screen.WebView.createRoute(url))
                    }
                },
            )
        }

        // In-app WebView
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val url = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            InAppWebViewScreen(
                url = url,
                onBack = {
                    if (backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}
