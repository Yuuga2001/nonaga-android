package jp.riverapp.hexlide.localization

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.riverapp.hexlide.presentation.localization.LocalizationManager
import jp.riverapp.hexlide.presentation.localization.LocalizationManager.Language
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties

class LocalizationTests {

    private fun createMockPrefs(savedLang: String? = null): SharedPreferences {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString("hexlide_lang", null) } returns savedLang
        every { prefs.edit() } returns editor
        return prefs
    }

    // --- English strings are non-empty ---

    @Test
    fun `English strings are non-empty`() {
        val s = LocalizedStrings.EN
        assertTrue(s.aiMode.isNotEmpty())
        assertTrue(s.pvpMode.isNotEmpty())
        assertTrue(s.onlineMode.isNotEmpty())
        assertTrue(s.you.isNotEmpty())
        assertTrue(s.phaseMoveToken.isNotEmpty())
        assertTrue(s.phaseMoveTile.isNotEmpty())
        assertTrue(s.youWin.isNotEmpty())
        assertTrue(s.playAgain.isNotEmpty())
        assertTrue(s.createRoom.isNotEmpty())
        assertTrue(s.joinGame.isNotEmpty())
        assertTrue(s.goal.isNotEmpty())
        assertTrue(s.settings.isNotEmpty())
        assertTrue(s.privacyPolicy.isNotEmpty())
        assertTrue(s.contact.isNotEmpty())
        assertTrue(s.howToPlay.isNotEmpty())
    }

    // --- Japanese strings are non-empty ---

    @Test
    fun `Japanese strings are non-empty`() {
        val s = LocalizedStrings.JA
        assertTrue(s.aiMode.isNotEmpty())
        assertTrue(s.pvpMode.isNotEmpty())
        assertTrue(s.onlineMode.isNotEmpty())
        assertTrue(s.you.isNotEmpty())
        assertTrue(s.phaseMoveToken.isNotEmpty())
        assertTrue(s.phaseMoveTile.isNotEmpty())
        assertTrue(s.youWin.isNotEmpty())
        assertTrue(s.playAgain.isNotEmpty())
        assertTrue(s.createRoom.isNotEmpty())
        assertTrue(s.joinGame.isNotEmpty())
        assertTrue(s.goal.isNotEmpty())
        assertTrue(s.settings.isNotEmpty())
        assertTrue(s.privacyPolicy.isNotEmpty())
        assertTrue(s.contact.isNotEmpty())
        assertTrue(s.howToPlay.isNotEmpty())
    }

    // --- English and Japanese strings differ ---

    @Test
    fun `English and Japanese strings differ`() {
        val en = LocalizedStrings.EN
        val ja = LocalizedStrings.JA
        assertNotEquals(en.aiMode, ja.aiMode)
        assertNotEquals(en.youWin, ja.youWin)
        assertNotEquals(en.goal, ja.goal)
        assertNotEquals(en.settings, ja.settings)
    }

    // --- LocalizationManager defaults to SYSTEM ---

    @Test
    fun `LocalizationManager defaults to SYSTEM`() {
        val prefs = createMockPrefs(savedLang = null)
        val manager = LocalizationManager(prefs)
        assertEquals(Language.SYSTEM, manager.language.value)
    }

    // --- resolvedLanguage never returns SYSTEM ---

    @Test
    fun `resolvedLanguage never returns SYSTEM`() {
        val prefs = createMockPrefs(savedLang = null)
        val manager = LocalizationManager(prefs)
        assertNotEquals(Language.SYSTEM, manager.resolvedLanguage)
    }

    // --- Setting explicit language persists and resolves correctly ---

    @Test
    fun `Setting explicit language persists and resolves correctly`() {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString("hexlide_lang", null) } returns null
        every { prefs.edit() } returns editor

        val manager = LocalizationManager(prefs)
        manager.setLanguage(Language.FR)
        assertEquals(Language.FR, manager.resolvedLanguage)
        assertTrue(manager.strings.settings.isNotEmpty())
        verify { editor.putString("hexlide_lang", "FR") }
    }

    // --- detectSystemLanguage returns valid language ---

    @Test
    fun `detectSystemLanguage returns valid language`() {
        val detected = LocalizationManager.detectSystemLanguage()
        assertNotEquals(Language.SYSTEM, detected)
    }

    // --- All strings non-empty for every language (reflection) ---

    @Test
    fun `All strings non-empty for EN`() = assertAllStringsNonEmpty("EN", LocalizedStrings.EN)

    @Test
    fun `All strings non-empty for JA`() = assertAllStringsNonEmpty("JA", LocalizedStrings.JA)

    @Test
    fun `All strings non-empty for KO`() = assertAllStringsNonEmpty("KO", LocalizedStrings.KO)

    @Test
    fun `All strings non-empty for ZH_HANS`() = assertAllStringsNonEmpty("ZH_HANS", LocalizedStrings.ZH_HANS)

    @Test
    fun `All strings non-empty for ZH_HANT`() = assertAllStringsNonEmpty("ZH_HANT", LocalizedStrings.ZH_HANT)

    @Test
    fun `All strings non-empty for ES`() = assertAllStringsNonEmpty("ES", LocalizedStrings.ES)

    @Test
    fun `All strings non-empty for FR`() = assertAllStringsNonEmpty("FR", LocalizedStrings.FR)

    @Test
    fun `All strings non-empty for DE`() = assertAllStringsNonEmpty("DE", LocalizedStrings.DE)

    @Test
    fun `All strings non-empty for PT`() = assertAllStringsNonEmpty("PT", LocalizedStrings.PT)

    @Test
    fun `All strings non-empty for IT`() = assertAllStringsNonEmpty("IT", LocalizedStrings.IT)

    @Test
    fun `All strings non-empty for RU`() = assertAllStringsNonEmpty("RU", LocalizedStrings.RU)

    @Test
    fun `All strings non-empty for TH`() = assertAllStringsNonEmpty("TH", LocalizedStrings.TH)

    @Test
    fun `All strings non-empty for VI`() = assertAllStringsNonEmpty("VI", LocalizedStrings.VI)

    @Test
    fun `All strings non-empty for ID`() = assertAllStringsNonEmpty("ID", LocalizedStrings.ID)

    @Test
    fun `All strings non-empty for TR`() = assertAllStringsNonEmpty("TR", LocalizedStrings.TR)

    private fun assertAllStringsNonEmpty(langName: String, strings: LocalizedStrings) {
        LocalizedStrings::class.memberProperties.forEach { prop ->
            val value = prop.get(strings) as String
            assertTrue("[$langName] ${prop.name} is empty", value.isNotEmpty())
        }
    }

    // --- Language.nativeName is non-empty for all concrete languages ---

    @Test
    fun `Language nativeNames are non-empty for all concrete languages`() {
        Language.entries.filter { it != Language.SYSTEM }.forEach { lang ->
            assertTrue("${lang.name} nativeName is empty", lang.nativeName.isNotEmpty())
        }
    }

    // --- Language.SYSTEM nativeName is empty ---

    @Test
    fun `SYSTEM nativeName is empty`() {
        assertTrue(Language.SYSTEM.nativeName.isEmpty())
    }

    // --- Language enum has 16 cases (SYSTEM + 15 languages) ---

    @Test
    fun `Language enum has 16 cases`() {
        assertEquals(16, Language.entries.size)
    }
}
