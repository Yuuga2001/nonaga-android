package jp.riverapp.hexlide.service

import jp.riverapp.hexlide.data.model.ApiError
import jp.riverapp.hexlide.data.model.CreateGameRequest
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.JoinGameRequest
import jp.riverapp.hexlide.data.model.PieceMoveRequest
import jp.riverapp.hexlide.data.remote.HexlideApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiServiceTests {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: HexlideApi

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        api = retrofit.create(HexlideApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `Successful response decodes GameSession`() = runTest {
        val responseJson = """
        {
            "gameId": "g1",
            "status": "PLAYING",
            "hostPlayerId": "h1",
            "hostColor": "red",
            "tiles": [{"q": 0, "r": 0}],
            "pieces": [],
            "turn": "red",
            "phase": "move_token",
            "createdAt": "",
            "updatedAt": ""
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson),
        )

        val game = api.createGame(CreateGameRequest(playerId = "test"))
        assertEquals("g1", game.gameId)
        assertEquals(GameStatus.PLAYING, game.status)
    }

    @Test
    fun `404 response maps to gameNotFound`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"),
        )

        try {
            api.getGame("missing")
            fail("Should have thrown HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
            val apiError = mapHttpExceptionToApiError(e)
            assertEquals(ApiError.GameNotFound, apiError)
        }
    }

    @Test
    fun `403 response maps to notYourTurn`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("Forbidden"),
        )

        try {
            api.movePiece(
                "g1",
                PieceMoveRequest(playerId = "p1", pieceId = "r1", toQ = 0, toR = 0),
            )
            fail("Should have thrown HttpException")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
            val apiError = mapHttpExceptionToApiError(e)
            assertEquals(ApiError.NotYourTurn, apiError)
        }
    }

    @Test
    fun `409 response maps to gameAlreadyStarted`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("Conflict"),
        )

        try {
            api.joinGame("g1", JoinGameRequest(playerId = "p1"))
            fail("Should have thrown HttpException")
        } catch (e: HttpException) {
            assertEquals(409, e.code())
            val apiError = mapHttpExceptionToApiError(e)
            assertEquals(ApiError.GameAlreadyStarted, apiError)
        }
    }

    /**
     * Maps Retrofit HttpException to domain ApiError, mirroring the iOS APIService error handling.
     */
    private fun mapHttpExceptionToApiError(e: HttpException): ApiError {
        return when (e.code()) {
            404 -> ApiError.GameNotFound
            403 -> ApiError.NotYourTurn
            409 -> ApiError.GameAlreadyStarted
            else -> ApiError.HttpError(e.code(), e.message())
        }
    }
}
