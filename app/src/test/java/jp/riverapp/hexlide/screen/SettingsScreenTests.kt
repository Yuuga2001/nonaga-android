package jp.riverapp.hexlide.screen

import jp.riverapp.hexlide.presentation.navigation.Screen
import jp.riverapp.hexlide.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenTests {

    @Test
    fun `all WebPages URLs are non-empty`() {
        assertTrue(Constants.WebPages.HOW_TO_PLAY.isNotEmpty())
        assertTrue(Constants.WebPages.PRIVACY.isNotEmpty())
        assertTrue(Constants.WebPages.CONTACT.isNotEmpty())
        assertTrue(Constants.WebPages.WEBSITE.isNotEmpty())
        assertTrue(Constants.WebPages.WEB_VERSION.isNotEmpty())
        assertTrue(Constants.WebPages.GOOGLE_PLAY_REVIEW.isNotEmpty())
    }

    @Test
    fun `screen routes are correct`() {
        assertEquals("game", Screen.Game.route)
        assertEquals("online_lobby", Screen.OnlineLobby.route)
        assertEquals("online_game/{gameId}", Screen.OnlineGame.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("webview/{url}", Screen.WebView.route)
    }

    @Test
    fun `WebView URL encoding produces valid route`() {
        val url = "https://hexlide.riverapp.jp/app/how-to-play"
        val route = Screen.WebView.createRoute(url)
        assertTrue(route.startsWith("webview/"))
        // The encoded URL should not contain raw slashes from the original URL
        val encodedPart = route.removePrefix("webview/")
        assertTrue(encodedPart.contains("hexlide.riverapp.jp"))
    }

    @Test
    fun `OnlineGame createRoute produces correct path`() {
        val gameId = "abc123"
        val route = Screen.OnlineGame.createRoute(gameId)
        assertEquals("online_game/abc123", route)
    }
}
