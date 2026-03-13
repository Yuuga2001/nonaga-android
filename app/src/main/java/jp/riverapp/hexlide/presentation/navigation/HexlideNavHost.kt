package jp.riverapp.hexlide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
    val strings = localizationManager.strings

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
                    navController.navigate(Screen.OnlineLobby.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        // Online lobby
        composable(Screen.OnlineLobby.route) {
            OnlineLobbyScreen(
                strings = strings,
                onNavigateToGame = { gameId ->
                    navController.navigate(Screen.OnlineGame.createRoute(gameId)) {
                        popUpTo(Screen.OnlineLobby.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("switchToAI", true)
                    navController.popBackStack()
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
                    navController.navigate(Screen.OnlineLobby.route) {
                        popUpTo(Screen.Game.route) { inclusive = false }
                    }
                },
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                localizationManager = localizationManager,
                onBack = { navController.popBackStack() },
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
