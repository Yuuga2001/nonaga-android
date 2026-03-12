package jp.riverapp.hexlide.logic

import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.AIEngine
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.logic.HexMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * iOS版 AIEngineTests.swift の完全移植。
 * AIEngine の各メソッドを JUnit 4 でテストする。
 */
class AIEngineTests {

    @Test
    fun `AI finds a piece move from initial position`() {
        val move = AIEngine.choosePieceMove(
            pieces = GameConstants.initialPieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        assertNotNull(move)
    }

    @Test
    fun `AI finds a tile move from initial position`() {
        val move = AIEngine.chooseTileMove(
            pieces = GameConstants.initialPieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        assertNotNull(move)
    }

    @Test
    fun `AI detects winning piece move`() {
        // Set up near-win position for red
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 2, r = -2), // needs to slide to (0,1) area
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 1),
            Piece(id = "b3", player = PlayerColor.BLUE, q = -2, r = 2),
        )
        val move = AIEngine.choosePieceMove(
            pieces = pieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        assertNotNull(move)
        // AI should prioritize winning or getting closer
    }

    @Test
    fun `AI tile move produces valid state`() {
        val move = AIEngine.chooseTileMove(
            pieces = GameConstants.initialPieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.BLUE,
        )
        if (move == null) {
            fail("Expected tile move")
            return
        }

        // Verify board remains connected after AI's move
        val newTiles = GameConstants.initialTiles.toMutableList()
        newTiles[move.tileIndex] = move.destination
        assertTrue(GameLogic.isBoardConnected(tiles = newTiles))
    }

    @Test
    fun `AI piece move produces valid slide`() {
        val move = AIEngine.choosePieceMove(
            pieces = GameConstants.initialPieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        if (move == null) {
            fail("Expected piece move")
            return
        }

        // Verify the destination is a valid slide for that piece
        val piece = GameConstants.initialPieces.first { it.id == move.pieceId }
        val validDests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = GameConstants.initialTiles,
            pieces = GameConstants.initialPieces,
        )
        assertTrue(validDests.any { it.q == move.destination.q && it.r == move.destination.r })
    }

    // ----------------------------------------------------------------
    // Edge cases
    // ----------------------------------------------------------------

    @Test
    fun `AI takes winning move when available`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 2),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 1),
            Piece(id = "b3", player = PlayerColor.BLUE, q = -2, r = 2),
        )
        val move = AIEngine.choosePieceMove(
            pieces = pieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        assertNotNull(move)
        if (move != null) {
            val newPieces = pieces.toMutableList()
            val idx = newPieces.indexOfFirst { it.id == move.pieceId }
            if (idx != -1) {
                newPieces[idx] = Piece(
                    id = move.pieceId,
                    player = PlayerColor.RED,
                    q = move.destination.q,
                    r = move.destination.r,
                )
            }
            val victory = GameLogic.getVictoryCoords(pieces = newPieces, player = PlayerColor.RED)
            assertNotNull(victory)
        }
    }

    @Test
    fun `AI finds defensive tile move when opponent threatens`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = -2, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = -2, r = 1),
            Piece(id = "r3", player = PlayerColor.RED, q = -2, r = 2),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 1, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = 2, r = 0),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 2, r = -2),
        )
        val move = AIEngine.chooseTileMove(
            pieces = pieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.RED,
        )
        assertNotNull(move)
    }

    // ----------------------------------------------------------------
    // Nil return / stuck scenarios
    // ----------------------------------------------------------------

    @Test
    fun `AI returns nil for piece move when all pieces are blocked`() {
        val tiles = listOf(Tile(q = 0, r = 0))
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 0, r = 0),
        )
        val move = AIEngine.choosePieceMove(pieces = pieces, tiles = tiles, aiColor = PlayerColor.RED)
        assertNull(move)
    }

    @Test
    fun `AI works for blue as well as red`() {
        val move = AIEngine.choosePieceMove(
            pieces = GameConstants.initialPieces,
            tiles = GameConstants.initialTiles,
            aiColor = PlayerColor.BLUE,
        )
        assertNotNull(move)
        if (move != null) {
            val piece = GameConstants.initialPieces.first { it.id == move.pieceId }
            assertEquals(PlayerColor.BLUE, piece.player)
        }
    }

    @Test
    fun `AI never moves opponent's piece`() {
        repeat(10) {
            val move = AIEngine.choosePieceMove(
                pieces = GameConstants.initialPieces,
                tiles = GameConstants.initialTiles,
                aiColor = PlayerColor.RED,
            )
            if (move != null) {
                val piece = GameConstants.initialPieces.first { it.id == move.pieceId }
                assertEquals(PlayerColor.RED, piece.player)
            }
        }
    }

    @Test
    fun `AI never moves tile with piece on it`() {
        repeat(10) {
            val move = AIEngine.chooseTileMove(
                pieces = GameConstants.initialPieces,
                tiles = GameConstants.initialTiles,
                aiColor = PlayerColor.RED,
            )
            if (move != null) {
                val tile = GameConstants.initialTiles[move.tileIndex]
                val hasPiece = GameConstants.initialPieces.any { it.coordsKey == tile.coordsKey }
                assertFalse(hasPiece)
            }
        }
    }
}
