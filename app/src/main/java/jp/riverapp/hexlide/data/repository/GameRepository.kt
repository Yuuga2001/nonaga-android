package jp.riverapp.hexlide.data.repository

import jp.riverapp.hexlide.data.model.AbandonRequest
import jp.riverapp.hexlide.data.model.CreateGameRequest
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.JoinGameRequest
import jp.riverapp.hexlide.data.model.PieceMoveRequest
import jp.riverapp.hexlide.data.model.RematchRequest
import jp.riverapp.hexlide.data.model.TileMoveRequest
import jp.riverapp.hexlide.data.remote.HexlideApi
import jp.riverapp.hexlide.domain.service.PlayerIdentityService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val api: HexlideApi,
    private val playerIdentityService: PlayerIdentityService,
) {
    private val playerId: String
        get() = playerIdentityService.getPlayerId()

    suspend fun createGame(): GameSession {
        return api.createGame(CreateGameRequest(playerId = playerId))
    }

    suspend fun getGame(gameId: String): GameSession {
        return api.getGame(gameId)
    }

    suspend fun joinGame(gameId: String): GameSession {
        return api.joinGame(gameId, JoinGameRequest(playerId = playerId))
    }

    suspend fun movePiece(
        gameId: String,
        pieceId: String,
        toQ: Int,
        toR: Int,
    ): GameSession {
        return api.movePiece(
            gameId,
            PieceMoveRequest(
                playerId = playerId,
                pieceId = pieceId,
                toQ = toQ,
                toR = toR,
            ),
        )
    }

    suspend fun moveTile(
        gameId: String,
        tileIndex: Int,
        toQ: Int,
        toR: Int,
    ): GameSession {
        return api.moveTile(
            gameId,
            TileMoveRequest(
                playerId = playerId,
                tileIndex = tileIndex,
                toQ = toQ,
                toR = toR,
            ),
        )
    }

    suspend fun abandonGame(gameId: String): GameSession {
        return api.abandonGame(gameId, AbandonRequest(playerId = playerId))
    }

    suspend fun rematch(gameId: String): GameSession {
        return api.rematch(gameId, RematchRequest(playerId = playerId))
    }

    suspend fun getGameByRoomCode(roomCode: String): GameSession {
        return api.getGameByRoomCode(roomCode)
    }
}
