package jp.riverapp.hexlide.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import jp.riverapp.hexlide.presentation.localization.LocalizationManager
import jp.riverapp.hexlide.presentation.screen.game.GameScreen
import jp.riverapp.hexlide.presentation.screen.settings.InAppWebViewScreen
import jp.riverapp.hexlide.presentation.screen.settings.SettingsScreen

@Composable
fun HexlideNavHost(
    localizationManager: LocalizationManager,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Game.route,
    ) {
        // Game screen (local)
        composable(Screen.Game.route) {
            GameScreen(
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
            // TODO: Replace with OnlineLobbyScreen when implemented
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Online Lobby - Coming Soon")
            }
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
            // TODO: Replace with OnlineGameScreen when implemented
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Online Game: $gameId - Coming Soon")
            }
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
