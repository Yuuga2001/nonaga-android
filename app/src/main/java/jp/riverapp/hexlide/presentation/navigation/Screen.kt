package jp.riverapp.hexlide.presentation.navigation

sealed class Screen(val route: String) {
    data object Game : Screen("game")
    data object OnlineLobby : Screen("online_lobby")
    data object OnlineGame : Screen("online_game/{gameId}") {
        fun createRoute(gameId: String) = "online_game/$gameId"
    }
    data object Settings : Screen("settings")
    data object WebView : Screen("webview/{url}") {
        fun createRoute(url: String) =
            "webview/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
}
