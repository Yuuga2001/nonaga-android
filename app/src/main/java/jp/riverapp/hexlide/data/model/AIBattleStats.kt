package jp.riverapp.hexlide.data.model

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class AIGameRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val won: Boolean,
    val turns: Int,
    val myColor: String,
    val wentFirst: Boolean,
)

@Serializable
data class AIBattleStats(
    val records: List<AIGameRecord> = emptyList(),
) {
    val wins: Int get() = records.count { it.won }
    val losses: Int get() = records.count { !it.won }
    val totalGames: Int get() = records.size
    val winRate: Double get() = if (totalGames == 0) 0.0 else wins.toDouble() / totalGames
    val averageTurns: Double
        get() {
            val wonRecords = records.filter { it.won }
            return if (wonRecords.isEmpty()) 0.0
            else wonRecords.sumOf { it.turns }.toDouble() / wonRecords.size
        }
}

object AIBattleStatsService {
    private const val PREFS_NAME = "hexlide_prefs"
    private const val KEY = "hexlide_ai_stats"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): AIBattleStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY, null) ?: return AIBattleStats()
        return try {
            json.decodeFromString<AIBattleStats>(data)
        } catch (_: Exception) {
            AIBattleStats()
        }
    }

    fun save(context: Context, stats: AIBattleStats) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, json.encodeToString(stats)).apply()
    }

    fun recordResult(
        context: Context,
        won: Boolean,
        turns: Int,
        myColor: PlayerColor,
        wentFirst: Boolean,
    ) {
        val stats = load(context)
        val record = AIGameRecord(
            won = won,
            turns = turns,
            myColor = myColor.name.lowercase(),
            wentFirst = wentFirst,
        )
        save(context, AIBattleStats(records = stats.records + record))
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY).apply()
    }
}
