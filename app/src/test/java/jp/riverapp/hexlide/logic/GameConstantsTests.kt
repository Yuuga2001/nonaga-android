package jp.riverapp.hexlide.logic

import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.HexMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameConstantsTests {

    @Test
    fun `Initial board has exactly 19 tiles`() {
        assertEquals(19, GameConstants.initialTiles.size)
    }

    @Test
    fun `Initial board has 3 red and 3 blue pieces`() {
        assertEquals(6, GameConstants.initialPieces.size)
        assertEquals(3, GameConstants.initialPieces.count { it.player == PlayerColor.RED })
        assertEquals(3, GameConstants.initialPieces.count { it.player == PlayerColor.BLUE })
    }

    @Test
    fun `Initial board is connected`() {
        // Board connectivity check: BFS from the first tile should reach all tiles
        val tiles = GameConstants.initialTiles
        val tileSet = tiles.map { it.coordsKey }.toMutableSet()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()

        queue.add(tiles.first().coordsKey)
        visited.add(tiles.first().coordsKey)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val parts = current.split(",")
            val q = parts[0].toInt()
            val r = parts[1].toInt()
            for (dir in HexMath.directions) {
                val neighborKey = HexMath.coordsKey(q + dir.q, r + dir.r)
                if (neighborKey in tileSet && neighborKey !in visited) {
                    visited.add(neighborKey)
                    queue.add(neighborKey)
                }
            }
        }

        assertEquals(
            "All tiles should be reachable from the first tile",
            tiles.size,
            visited.size,
        )
    }

    @Test
    fun `All initial pieces are on valid tiles`() {
        val tileKeys = GameConstants.initialTiles.map { it.coordsKey }.toSet()
        for (piece in GameConstants.initialPieces) {
            assertTrue(
                "Piece ${piece.id} at ${piece.coordsKey} is not on a tile",
                tileKeys.contains(piece.coordsKey),
            )
        }
    }

    @Test
    fun `No duplicate tile positions`() {
        val keys = GameConstants.initialTiles.map { it.coordsKey }
        assertEquals(keys.toSet().size, keys.size)
    }

    @Test
    fun `No duplicate piece positions`() {
        val keys = GameConstants.initialPieces.map { it.coordsKey }
        assertEquals(keys.toSet().size, keys.size)
    }

    @Test
    fun `All pieces have unique IDs`() {
        val ids = GameConstants.initialPieces.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun `Same-color pieces are not adjacent initially`() {
        val redPieces = GameConstants.initialPieces.filter { it.player == PlayerColor.RED }
        for (i in redPieces.indices) {
            for (j in (i + 1) until redPieces.size) {
                assertFalse(
                    "Red pieces ${redPieces[i].id} and ${redPieces[j].id} should not be adjacent",
                    HexMath.isAdjacent(redPieces[i].tile, redPieces[j].tile),
                )
            }
        }
        val bluePieces = GameConstants.initialPieces.filter { it.player == PlayerColor.BLUE }
        for (i in bluePieces.indices) {
            for (j in (i + 1) until bluePieces.size) {
                assertFalse(
                    "Blue pieces ${bluePieces[i].id} and ${bluePieces[j].id} should not be adjacent",
                    HexMath.isAdjacent(bluePieces[i].tile, bluePieces[j].tile),
                )
            }
        }
    }
}
