package jp.riverapp.hexlide.logic

import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.HexMath
import jp.riverapp.hexlide.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class HexMathTests {

    @Test
    fun `coordsKey formats correctly`() {
        assertEquals("0,0", HexMath.coordsKey(0, 0))
        assertEquals("2,-1", HexMath.coordsKey(2, -1))
        assertEquals("-3,4", HexMath.coordsKey(-3, 4))
    }

    @Test
    fun `hexToPixel converts correctly`() {
        val origin = HexMath.hexToPixel(q = 0, r = 0)
        assertEquals(0f, origin.first, 0.001f)
        assertEquals(0f, origin.second, 0.001f)

        val p = HexMath.hexToPixel(q = 1, r = 0, hexSize = 38f)
        assertEquals(57.0f, p.first, 0.001f) // 38 * 1.5
        assertEquals(38f * sqrt(3f) / 2f, p.second, 0.001f)
    }

    @Test
    fun `isAdjacent detects adjacent tiles`() {
        val a = Tile(q = 0, r = 0)
        // All 6 neighbors should be adjacent
        for (dir in HexMath.directions) {
            val b = Tile(q = dir.q, r = dir.r)
            assertTrue("Direction (${ dir.q}, ${dir.r}) should be adjacent", HexMath.isAdjacent(a, b))
        }
    }

    @Test
    fun `isAdjacent rejects non-adjacent tiles`() {
        val a = Tile(q = 0, r = 0)
        val b = Tile(q = 2, r = 0) // 2 steps away
        assertFalse(HexMath.isAdjacent(a, b))

        val c = Tile(q = 0, r = 0) // same position
        assertFalse(HexMath.isAdjacent(a, c))
    }

    @Test
    fun `directions has 6 elements`() {
        assertEquals(6, HexMath.directions.size)
    }

    @Test
    fun `hexagonPoints generates 6 points`() {
        val points = HexMath.hexagonPoints(size = 38f)
        assertEquals(6, points.size)
    }

    @Test
    fun `distance calculates hex distance`() {
        val a = Tile(q = 0, r = 0)
        val b = Tile(q = 2, r = -1)
        assertEquals(2, HexMath.distance(a, b))

        val c = Tile(q = 0, r = 0)
        assertEquals(0, HexMath.distance(a, c))

        val d = Tile(q = 1, r = 0)
        assertEquals(1, HexMath.distance(a, d))
    }

    // MARK: - Edge cases

    @Test
    fun `coordsKey with negative coordinates`() {
        assertEquals("-5,-10", HexMath.coordsKey(-5, -10))
        assertEquals("0,-1", HexMath.coordsKey(0, -1))
    }

    @Test
    fun `hexToPixel with zero size returns origin`() {
        val p = HexMath.hexToPixel(q = 5, r = 3, hexSize = 0f)
        assertEquals(0f, p.first, 0.001f)
        assertEquals(0f, p.second, 0.001f)
    }

    @Test
    fun `hexToPixel with negative coordinates`() {
        val p = HexMath.hexToPixel(q = -1, r = -1, hexSize = 38f)
        assertTrue(p.first < 0)
        assertTrue(p.second < 0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = Tile(q = 2, r = -1)
        val b = Tile(q = -1, r = 2)
        assertEquals(HexMath.distance(a, b), HexMath.distance(b, a))
    }

    @Test
    fun `isAdjacent is symmetric`() {
        val a = Tile(q = 0, r = 0)
        val b = Tile(q = 1, r = 0)
        assertEquals(HexMath.isAdjacent(a, b), HexMath.isAdjacent(b, a))
    }

    // MARK: - Grid spacing

    @Test
    fun `Adjacent hex pixel distance equals hexSize times sqrt3`() {
        val hexSize = Constants.Game.HEX_SIZE
        val expectedDistance = hexSize * sqrt(3.0f)

        val origin = HexMath.hexToPixel(q = 0, r = 0, hexSize = hexSize)
        val neighbor = HexMath.hexToPixel(q = 0, r = 1, hexSize = hexSize)
        val dx = neighbor.first - origin.first
        val dy = neighbor.second - origin.second
        val distance = sqrt(dx * dx + dy * dy)

        assertTrue(
            "Distance $distance should equal hexSize * sqrt(3) = $expectedDistance",
            abs(distance - expectedDistance) < 0.001f,
        )
    }

    @Test
    fun `All 6 adjacent directions produce equal pixel distance`() {
        val hexSize = Constants.Game.HEX_SIZE
        val origin = HexMath.hexToPixel(q = 0, r = 0, hexSize = hexSize)
        val distances = mutableListOf<Float>()

        for (dir in HexMath.directions) {
            val neighbor = HexMath.hexToPixel(q = dir.q, r = dir.r, hexSize = hexSize)
            val dx = neighbor.first - origin.first
            val dy = neighbor.second - origin.second
            distances.add(sqrt(dx * dx + dy * dy))
        }

        val first = distances[0]
        for ((i, d) in distances.withIndex()) {
            assertTrue(
                "Direction $i distance $d differs from first $first",
                abs(d - first) < 0.001f,
            )
        }
    }
}
