package jp.riverapp.hexlide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSession(
    val gameId: String,
    val roomCode: String? = null,
    val status: GameStatus,
    val hostPlayerId: String,
    val guestPlayerId: String? = null,
    val hostColor: PlayerColor,
    val tiles: List<Tile>,
    val pieces: List<Piece>,
    val turn: PlayerColor,
    val phase: GamePhase,
    val winner: PlayerColor? = null,
    val victoryLine: List<String>? = null,
    val lastMoveAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val ttl: Int? = null,
)
