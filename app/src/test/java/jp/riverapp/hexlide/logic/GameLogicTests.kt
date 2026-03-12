package jp.riverapp.hexlide.logic

import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.logic.HexMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * iOS版 GameLogicTests.swift の完全移植。
 * GameLogic の各メソッドを JUnit 4 でテストする。
 */
class GameLogicTests {

    // ----------------------------------------------------------------
    // Board connectivity
    // ----------------------------------------------------------------

    @Test
    fun `initial board is connected`() {
        assertTrue(GameLogic.isBoardConnected(tiles = GameConstants.initialTiles))
    }

    @Test
    fun `board connectivity after removing center tile`() {
        // Removing center tile (0,0) at index 0 should still leave board connected
        assertTrue(GameLogic.isBoardConnected(tiles = GameConstants.initialTiles, excludeIndex = 0))
    }

    @Test
    fun `single tile board is connected`() {
        val tiles = listOf(Tile(q = 0, r = 0))
        assertTrue(GameLogic.isBoardConnected(tiles = tiles))
    }

    @Test
    fun `disconnected board detected`() {
        val tiles = listOf(Tile(q = 0, r = 0), Tile(q = 5, r = 5))
        assertFalse(GameLogic.isBoardConnected(tiles = tiles))
    }

    @Test
    fun `removing bridge tile disconnects linear board`() {
        val tiles = listOf(Tile(q = 0, r = 0), Tile(q = 1, r = 0), Tile(q = 2, r = 0))
        assertFalse(GameLogic.isBoardConnected(tiles = tiles, excludeIndex = 1))
    }

    @Test
    fun `three separate clusters detected as disconnected`() {
        val tiles = listOf(Tile(q = 0, r = 0), Tile(q = 5, r = 0), Tile(q = 0, r = 5))
        assertFalse(GameLogic.isBoardConnected(tiles = tiles))
    }

    @Test
    fun `L-shaped board is connected`() {
        val tiles = listOf(
            Tile(q = 0, r = 0), Tile(q = 1, r = 0), Tile(q = 2, r = 0),
            Tile(q = 0, r = 1), Tile(q = 0, r = 2),
        )
        assertTrue(GameLogic.isBoardConnected(tiles = tiles))
    }

    // ----------------------------------------------------------------
    // Slide destinations
    // ----------------------------------------------------------------

    @Test
    fun `slide destinations from initial position`() {
        val tiles = GameConstants.initialTiles
        val pieces = GameConstants.initialPieces

        // Red piece at (2, -2) - should have slide destinations
        val r1 = pieces.first { it.id == "r1" }
        val dests = GameLogic.getSlideDestinations(piece = r1, tiles = tiles, pieces = pieces)
        assertTrue(dests.isNotEmpty())
    }

    @Test
    fun `slide destinations blocked by pieces`() {
        val tiles = GameConstants.initialTiles
        // Place pieces to block all directions
        val piece = Piece(id = "test", player = PlayerColor.RED, q = 0, r = 0)
        val blockers: List<Piece> = HexMath.directions.mapIndexed { i, dir ->
            Piece(id = "b$i", player = PlayerColor.BLUE, q = dir.q, r = dir.r)
        }
        val dests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = tiles,
            pieces = listOf(piece) + blockers,
        )
        assertTrue(dests.isEmpty())
    }

    @Test
    fun `slide on single-tile board yields no destinations`() {
        val piece = Piece(id = "test", player = PlayerColor.RED, q = 0, r = 0)
        val dests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = listOf(Tile(q = 0, r = 0)),
            pieces = listOf(piece),
        )
        assertTrue(dests.isEmpty())
    }

    @Test
    fun `piece at center has multiple slide directions`() {
        val piece = Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0)
        val dests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = GameConstants.initialTiles,
            pieces = listOf(piece),
        )
        assertTrue("Center piece should have multiple slide directions", dests.size >= 4)
    }

    @Test
    fun `slide destinations are always valid tile positions`() {
        val piece = Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0)
        val dests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = GameConstants.initialTiles,
            pieces = listOf(piece),
        )
        val tileKeys = GameConstants.initialTiles.map { it.coordsKey }.toSet()
        for (dest in dests) {
            assertTrue(tileKeys.contains(dest.coordsKey))
        }
    }

    @Test
    fun `slide stops one before blocking piece`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 2, r = 0),
        )
        val dests = GameLogic.getSlideDestinations(
            piece = pieces[0],
            tiles = GameConstants.initialTiles,
            pieces = pieces,
        )
        val hasQ1R0 = dests.any { it.q == 1 && it.r == 0 }
        val hasQ2R0 = dests.any { it.q == 2 && it.r == 0 }
        assertTrue("Should stop one before blocking piece", hasQ1R0)
        assertFalse("Should not overlap with blocking piece", hasQ2R0)
    }

    // ----------------------------------------------------------------
    // Victory detection
    // ----------------------------------------------------------------

    @Test
    fun `victory detection - line of 3`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 2, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 1),
            Piece(id = "b3", player = PlayerColor.BLUE, q = -2, r = 2),
        )
        val result = GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.RED)
        assertNotNull(result)
        assertEquals(3, result!!.size)
    }

    @Test
    fun `victory detection - triangle`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 1),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 1),
            Piece(id = "b3", player = PlayerColor.BLUE, q = -2, r = 2),
        )
        val result = GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.RED)
        assertNotNull(result)
    }

    @Test
    fun `no victory when pieces separated`() {
        val result = GameLogic.getVictoryCoords(
            pieces = GameConstants.initialPieces,
            player = PlayerColor.RED,
        )
        assertNull(result)
    }

    @Test
    fun `no victory with only 1 adjacent pair`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = -2, r = 2), // far away
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 1),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 0, r = -2),
        )
        val result = GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.RED)
        assertNull(result)
    }

    @Test
    fun `victory with fewer than 3 same-color pieces returns nil`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -1, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b3", player = PlayerColor.BLUE, q = -2, r = 1),
        )
        assertNull(GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.RED))
    }

    @Test
    fun `blue victory detected correctly`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = -2, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 2, r = -2),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 2),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 0, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = 1, r = 0),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 0, r = 1),
        )
        assertNotNull(GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.BLUE))
        assertNull(GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.RED))
    }

    @Test
    fun `victory not detected for wrong player`() {
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 1),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = 2, r = -2),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 0, r = 2),
        )
        assertNull(GameLogic.getVictoryCoords(pieces = pieces, player = PlayerColor.BLUE))
    }

    // ----------------------------------------------------------------
    // Valid tile destinations
    // ----------------------------------------------------------------

    @Test
    fun `valid tile destinations require 2+ adjacent tiles`() {
        val tiles = GameConstants.initialTiles
        // Select an edge tile
        val dests = GameLogic.getValidTileDestinations(selectedIndex = 9, tiles = tiles) // (2,-2) edge tile
        for (dest in dests) {
            // Verify each dest is adjacent to at least 2 remaining tiles
            val remaining = tiles.filterIndexed { i, _ -> i != 9 }
            val adjacentCount = remaining.count { HexMath.isAdjacent(it, dest) }
            assertTrue(adjacentCount >= 2)
        }
    }

    @Test
    fun `valid tile destinations with out-of-bounds index returns empty`() {
        val dests = GameLogic.getValidTileDestinations(
            selectedIndex = 999,
            tiles = GameConstants.initialTiles,
        )
        assertTrue(dests.isEmpty())
    }

    @Test
    fun `valid tile destinations minimal board`() {
        val tiles = listOf(Tile(q = 0, r = 0), Tile(q = 1, r = 0), Tile(q = 0, r = 1))
        val dests = GameLogic.getValidTileDestinations(selectedIndex = 2, tiles = tiles)
        for (dest in dests) {
            val remaining = listOf(tiles[0], tiles[1])
            val adjCount = remaining.count { HexMath.isAdjacent(it, dest) }
            assertTrue(adjCount >= 2)
        }
    }

    // ----------------------------------------------------------------
    // getMovableTileIndices
    // ----------------------------------------------------------------

    @Test
    fun `getMovableTileIndices returns empty when winner exists`() {
        val result = GameLogic.getMovableTileIndices(
            tiles = GameConstants.initialTiles,
            pieces = GameConstants.initialPieces,
            winner = PlayerColor.RED,
            phase = GamePhase.MOVE_TILE,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMovableTileIndices returns empty in moveToken phase`() {
        val result = GameLogic.getMovableTileIndices(
            tiles = GameConstants.initialTiles,
            pieces = GameConstants.initialPieces,
            winner = null,
            phase = GamePhase.MOVE_TOKEN,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMovableTileIndices excludes tiles with pieces`() {
        val tiles = GameConstants.initialTiles
        val pieces = GameConstants.initialPieces
        val result = GameLogic.getMovableTileIndices(
            tiles = tiles,
            pieces = pieces,
            winner = null,
            phase = GamePhase.MOVE_TILE,
        )

        // Verify no index in result has a piece on it
        for (i in result) {
            val key = HexMath.coordsKey(tiles[i].q, tiles[i].r)
            val hasPiece = pieces.any { HexMath.coordsKey(it.q, it.r) == key }
            assertFalse(hasPiece)
        }
    }

    @Test
    fun `getMovableTileIndices only includes tiles that keep board connected`() {
        val tiles = GameConstants.initialTiles
        val pieces = GameConstants.initialPieces
        val result = GameLogic.getMovableTileIndices(
            tiles = tiles,
            pieces = pieces,
            winner = null,
            phase = GamePhase.MOVE_TILE,
        )

        // Verify each movable tile keeps the board connected when removed
        for (i in result) {
            assertTrue(GameLogic.isBoardConnected(tiles = tiles, excludeIndex = i))
        }
    }

    // ----------------------------------------------------------------
    // Initial counts
    // ----------------------------------------------------------------

    @Test
    fun `initial tile count is 19`() {
        assertEquals(19, GameConstants.initialTiles.size)
    }

    @Test
    fun `initial piece count is 6`() {
        assertEquals(6, GameConstants.initialPieces.size)
        assertEquals(3, GameConstants.initialPieces.count { it.player == PlayerColor.RED })
        assertEquals(3, GameConstants.initialPieces.count { it.player == PlayerColor.BLUE })
    }

    // ----------------------------------------------------------------
    // getPlayerColor
    // ----------------------------------------------------------------

    @Test
    fun `getPlayerColor returns correct colors`() {
        val game = GameSession(
            gameId = "test",
            roomCode = null,
            status = GameStatus.PLAYING,
            hostPlayerId = "host-id",
            guestPlayerId = "guest-id",
            hostColor = PlayerColor.RED,
            tiles = GameConstants.initialTiles,
            pieces = GameConstants.initialPieces,
            turn = PlayerColor.RED,
            phase = GamePhase.MOVE_TOKEN,
            winner = null,
            victoryLine = null,
            lastMoveAt = null,
            createdAt = "",
            updatedAt = "",
            ttl = null,
        )
        assertEquals(PlayerColor.RED, GameLogic.getPlayerColor(game = game, playerId = "host-id"))
        assertEquals(PlayerColor.BLUE, GameLogic.getPlayerColor(game = game, playerId = "guest-id"))
        assertNull(GameLogic.getPlayerColor(game = game, playerId = "unknown"))
    }

    // ----------------------------------------------------------------
    // calculateViewBounds
    // ----------------------------------------------------------------

    @Test
    fun `calculateViewBounds returns valid rect`() {
        val bounds = GameLogic.calculateViewBounds(tiles = GameConstants.initialTiles)
        assertTrue(bounds.width > 0)
        assertTrue(bounds.height > 0)
    }
}
