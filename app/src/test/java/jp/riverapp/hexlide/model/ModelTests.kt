package jp.riverapp.hexlide.model

import jp.riverapp.hexlide.data.model.ApiError
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTests {

    // MARK: - PlayerColor

    @Test
    fun `PlayerColor opposite returns correct value`() {
        assertEquals(PlayerColor.BLUE, PlayerColor.RED.opposite)
        assertEquals(PlayerColor.RED, PlayerColor.BLUE.opposite)
    }

    @Test
    fun `PlayerColor round-trip opposite is identity`() {
        assertEquals(PlayerColor.RED, PlayerColor.RED.opposite.opposite)
        assertEquals(PlayerColor.BLUE, PlayerColor.BLUE.opposite.opposite)
    }

    @Test
    fun `PlayerColor rawValues are correct`() {
        // kotlinx.serialization uses @SerialName for JSON values
        // The serialized names match the iOS rawValue
        assertEquals("red", PlayerColor.RED.name.lowercase())
        assertEquals("blue", PlayerColor.BLUE.name.lowercase())
    }

    // MARK: - SelectedItem

    @Test
    fun `SelectedItem equality for pieces`() {
        assertEquals(SelectedItem.PieceItem("r1"), SelectedItem.PieceItem("r1"))
        assertNotEquals(SelectedItem.PieceItem("r1"), SelectedItem.PieceItem("r2"))
    }

    @Test
    fun `SelectedItem equality for tiles`() {
        assertEquals(SelectedItem.TileIndexItem(0), SelectedItem.TileIndexItem(0))
        assertNotEquals(SelectedItem.TileIndexItem(0), SelectedItem.TileIndexItem(1))
    }

    @Test
    fun `SelectedItem piece not equal to tile`() {
        assertNotEquals(
            SelectedItem.PieceItem("0") as SelectedItem,
            SelectedItem.TileIndexItem(0) as SelectedItem,
        )
    }

    // MARK: - GamePhase

    @Test
    fun `GamePhase rawValues match API format`() {
        assertEquals("move_token", GamePhase.MOVE_TOKEN.value)
        assertEquals("move_tile", GamePhase.MOVE_TILE.value)
        assertEquals("ended", GamePhase.ENDED.value)
        assertEquals("waiting", GamePhase.WAITING.value)
    }

    // MARK: - GameStatus

    @Test
    fun `GameStatus rawValues match API format`() {
        assertEquals("WAITING", GameStatus.WAITING.value)
        assertEquals("PLAYING", GameStatus.PLAYING.value)
        assertEquals("FINISHED", GameStatus.FINISHED.value)
        assertEquals("ABANDONED", GameStatus.ABANDONED.value)
    }

    // MARK: - Tile

    @Test
    fun `Tile coordsKey format`() {
        val tile = Tile(q = 3, r = -2)
        assertEquals("3,-2", tile.coordsKey)
    }

    @Test
    fun `Tile coordsKey uniqueness`() {
        val t1 = Tile(q = 1, r = 2)
        val t2 = Tile(q = 2, r = 1)
        assertNotEquals(t1.coordsKey, t2.coordsKey)
    }

    // MARK: - Piece

    @Test
    fun `Piece coordsKey matches tile at same position`() {
        val piece = Piece(id = "r1", player = PlayerColor.RED, q = 3, r = -1)
        val tile = Tile(q = 3, r = -1)
        assertEquals(piece.coordsKey, tile.coordsKey)
    }

    @Test
    fun `Piece tile property creates correct Tile`() {
        val piece = Piece(id = "b1", player = PlayerColor.BLUE, q = 2, r = -1)
        assertEquals(2, piece.tile.q)
        assertEquals(-1, piece.tile.r)
    }

    // MARK: - ApiError

    @Test
    fun `ApiError errorDescription is non-nil for all cases`() {
        val cases: List<ApiError> = listOf(
            ApiError.InvalidURL,
            ApiError.InvalidResponse,
            ApiError.HttpError(statusCode = 500, errorMessage = "server"),
            ApiError.DecodingError(errorMessage = "bad json"),
            ApiError.NetworkError(errorMessage = "timeout"),
            ApiError.GameNotFound,
            ApiError.GameAlreadyStarted,
            ApiError.NotYourTurn,
            ApiError.InvalidMove,
        )
        for (error in cases) {
            assertNotNull("$error should have errorDescription", error.errorDescription)
            assertTrue(
                "$error errorDescription should not be empty",
                error.errorDescription.isNotEmpty(),
            )
        }
    }

    @Test
    fun `ApiError httpError includes status code`() {
        val error = ApiError.HttpError(statusCode = 503, errorMessage = "unavailable")
        assertTrue(error.errorDescription.contains("503"))
    }

    @Test
    fun `ApiError message is same as errorDescription`() {
        val cases: List<ApiError> = listOf(
            ApiError.InvalidURL,
            ApiError.GameNotFound,
            ApiError.NotYourTurn,
            ApiError.InvalidMove,
        )
        for (error in cases) {
            assertEquals(error.message, error.errorDescription)
        }
    }
}
