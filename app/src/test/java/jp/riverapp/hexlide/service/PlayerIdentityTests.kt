package jp.riverapp.hexlide.service

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.riverapp.hexlide.domain.service.PlayerIdentityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerIdentityTests {

    private fun createMockPrefs(
        data: MutableMap<String, String?> = mutableMapOf(),
    ): SharedPreferences {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = mockk<SharedPreferences>()

        every { prefs.getString(any(), any()) } answers {
            data[firstArg()]
        }

        every { prefs.edit() } returns editor

        every { editor.putString(any(), any()) } answers {
            data[firstArg()] = secondArg()
            editor
        }

        every { editor.remove(any()) } answers {
            data.remove(firstArg<String>())
            editor
        }

        every { editor.apply() } answers { }

        return prefs
    }

    @Test
    fun `getPlayerId returns consistent UUID`() {
        val data = mutableMapOf<String, String?>()
        val prefs = createMockPrefs(data)
        val service = PlayerIdentityService(prefs)

        val id1 = service.getPlayerId()
        val id2 = service.getPlayerId()

        assertEquals(id1, id2)
        assertNotNull(id1)
        assertTrue(id1.isNotEmpty())
    }

    @Test
    fun `Player ID is lowercase`() {
        val data = mutableMapOf<String, String?>()
        val prefs = createMockPrefs(data)
        val service = PlayerIdentityService(prefs)

        val id = service.getPlayerId()

        assertEquals(id, id.lowercase())
    }

    @Test
    fun `Migration from legacy key preserves ID`() {
        val legacyId = "legacy-test-uuid-12345678"
        val data = mutableMapOf<String, String?>(
            "nonaga_player_id" to legacyId,
        )
        val prefs = createMockPrefs(data)
        val service = PlayerIdentityService(prefs)

        val id = service.getPlayerId()

        assertEquals(legacyId, id)
        // New key should have the value
        assertEquals(legacyId, data["hexlide_player_id"])
        // Legacy key should be removed
        assertTrue(data["nonaga_player_id"] == null)
    }
}
