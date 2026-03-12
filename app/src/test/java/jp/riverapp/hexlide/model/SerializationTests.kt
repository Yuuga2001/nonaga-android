package jp.riverapp.hexlide.model

import jp.riverapp.hexlide.data.model.CreateGameRequest
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.PieceMoveRequest
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.TileMoveRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SerializationTests {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `Create game request encoding`() {
        val request = CreateGameRequest(playerId = "test-uuid")
        val jsonString = json.encodeToString(CreateGameRequest.serializer(), request)
        val jsonObject = json.decodeFromString(JsonObject.serializer(), jsonString)
        assertEquals("test-uuid", jsonObject["playerId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `Piece move request encoding`() {
        val request = PieceMoveRequest(
            playerId = "id",
            pieceId = "r1",
            toQ = 2,
            toR = -1,
        )
        val jsonString = json.encodeToString(PieceMoveRequest.serializer(), request)
        val jsonObject = json.decodeFromString(JsonObject.serializer(), jsonString)
        assertEquals("piece", jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("r1", jsonObject["pieceId"]?.jsonPrimitive?.content)
        assertEquals(2, jsonObject["toQ"]?.jsonPrimitive?.int)
        assertEquals(-1, jsonObject["toR"]?.jsonPrimitive?.int)
    }

    @Test
    fun `Tile move request encoding`() {
        val request = TileMoveRequest(
            playerId = "id",
            tileIndex = 5,
            toQ = 1,
            toR = 1,
        )
        val jsonString = json.encodeToString(TileMoveRequest.serializer(), request)
        val jsonObject = json.decodeFromString(JsonObject.serializer(), jsonString)
        assertEquals("tile", jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals(5, jsonObject["tileIndex"]?.jsonPrimitive?.int)
    }

    @Test
    fun `GameSession decoding from JSON`() {
        val jsonString = """
        {
            "gameId": "abc123",
            "roomCode": "123456",
            "status": "PLAYING",
            "hostPlayerId": "host-uuid",
            "guestPlayerId": "guest-uuid",
            "hostColor": "red",
            "tiles": [{"q": 0, "r": 0}, {"q": 1, "r": 0}],
            "pieces": [{"id": "r1", "player": "red", "q": 0, "r": 0}],
            "turn": "red",
            "phase": "move_token",
            "winner": null,
            "victoryLine": null,
            "lastMoveAt": null,
            "createdAt": "2026-01-01T00:00:00Z",
            "updatedAt": "2026-01-01T00:00:00Z",
            "ttl": 1234567890
        }
        """.trimIndent()

        val session = json.decodeFromString(GameSession.serializer(), jsonString)
        assertEquals("abc123", session.gameId)
        assertEquals("123456", session.roomCode)
        assertEquals(GameStatus.PLAYING, session.status)
        assertEquals(PlayerColor.RED, session.hostColor)
        assertEquals(2, session.tiles.size)
        assertEquals(1, session.pieces.size)
        assertEquals(GamePhase.MOVE_TOKEN, session.phase)
        assertNull(session.winner)
    }

    @Test
    fun `GameSession decoding with winner`() {
        val jsonString = """
        {
            "gameId": "abc",
            "status": "FINISHED",
            "hostPlayerId": "h",
            "hostColor": "blue",
            "tiles": [],
            "pieces": [],
            "turn": "blue",
            "phase": "ended",
            "winner": "blue",
            "victoryLine": ["0,0", "1,0", "0,1"],
            "createdAt": "",
            "updatedAt": "",
            "ttl": null
        }
        """.trimIndent()

        val session = json.decodeFromString(GameSession.serializer(), jsonString)
        assertEquals(PlayerColor.BLUE, session.winner)
        assertNotNull(session.victoryLine)
        assertEquals(3, session.victoryLine!!.size)
        assertEquals(GameStatus.FINISHED, session.status)
    }
}
