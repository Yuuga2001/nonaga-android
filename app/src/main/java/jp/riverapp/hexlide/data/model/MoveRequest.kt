package jp.riverapp.hexlide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRequest(
    val playerId: String,
)

@Serializable
data class JoinGameRequest(
    val playerId: String,
)

@Serializable
data class PieceMoveRequest(
    val playerId: String,
    val type: String = "piece",
    val pieceId: String,
    val toQ: Int,
    val toR: Int,
)

@Serializable
data class TileMoveRequest(
    val playerId: String,
    val type: String = "tile",
    val tileIndex: Int,
    val toQ: Int,
    val toR: Int,
)

@Serializable
data class AbandonRequest(
    val playerId: String,
)

@Serializable
data class RematchRequest(
    val playerId: String,
)
