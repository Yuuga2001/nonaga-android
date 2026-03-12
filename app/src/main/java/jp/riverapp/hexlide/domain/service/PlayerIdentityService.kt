package jp.riverapp.hexlide.domain.service

import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerIdentityService @Inject constructor(
    private val prefs: SharedPreferences,
) {
    companion object {
        private const val KEY = "hexlide_player_id"
        private const val LEGACY_KEY = "nonaga_player_id"
    }

    fun getPlayerId(): String {
        // Return existing value under new key
        prefs.getString(KEY, null)?.let { return it }

        // Migrate from legacy key
        prefs.getString(LEGACY_KEY, null)?.let { legacy ->
            prefs.edit()
                .putString(KEY, legacy)
                .remove(LEGACY_KEY)
                .apply()
            return legacy
        }

        // Generate new ID
        val newId = UUID.randomUUID().toString().lowercase()
        prefs.edit().putString(KEY, newId).apply()
        return newId
    }
}
