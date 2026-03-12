package jp.riverapp.hexlide.data.remote

import jp.riverapp.hexlide.data.model.AbandonRequest
import jp.riverapp.hexlide.data.model.CreateGameRequest
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.JoinGameRequest
import jp.riverapp.hexlide.data.model.PieceMoveRequest
import jp.riverapp.hexlide.data.model.RematchRequest
import jp.riverapp.hexlide.data.model.TileMoveRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

interface HexlideApi {

    @POST("api/game")
    suspend fun createGame(@Body body: CreateGameRequest): GameSession

    @GET("api/game/{gameId}")
    suspend fun getGame(@Path("gameId") gameId: String): GameSession

    @POST("api/game/{gameId}/join")
    suspend fun joinGame(
        @Path("gameId") gameId: String,
        @Body body: JoinGameRequest,
    ): GameSession

    @POST("api/game/{gameId}/move")
    suspend fun movePiece(
        @Path("gameId") gameId: String,
        @Body body: PieceMoveRequest,
    ): GameSession

    @POST("api/game/{gameId}/move")
    suspend fun moveTile(
        @Path("gameId") gameId: String,
        @Body body: TileMoveRequest,
    ): GameSession

    @HTTP(method = "DELETE", path = "api/game/{gameId}", hasBody = true)
    suspend fun abandonGame(
        @Path("gameId") gameId: String,
        @Body body: AbandonRequest,
    ): GameSession

    @POST("api/game/{gameId}/rematch")
    suspend fun rematch(
        @Path("gameId") gameId: String,
        @Body body: RematchRequest,
    ): GameSession

    @GET("api/game/room/{roomCode}")
    suspend fun getGameByRoomCode(@Path("roomCode") roomCode: String): GameSession
}
