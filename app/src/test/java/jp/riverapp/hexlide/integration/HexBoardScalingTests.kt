package jp.riverapp.hexlide.integration

import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.logic.HexMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * HexBoard の描画に関連するスケーリング・座標変換の統合テスト。
 * GameLogic.calculateViewBounds と HexMath を統合的に検証する。
 */
class HexBoardScalingTests {

    // ----------------------------------------------------------------
    // 1. calculateViewBoundsForInitialBoard
    // ----------------------------------------------------------------

    @Test
    fun `calculateViewBoundsForInitialBoard - 19 tiles produce valid bounds`() {
        val bounds = GameLogic.calculateViewBounds(GameConstants.initialTiles)

        assertTrue("幅が正", bounds.width > 0)
        assertTrue("高さが正", bounds.height > 0)
        assertTrue("minX < maxX", bounds.minX < bounds.maxX)
        assertTrue("minY < maxY", bounds.minY < bounds.maxY)

        // 19タイルの六角形ボードは概ね対称的なので、
        // 幅と高さが極端に偏っていないことを確認
        val aspectRatio = bounds.width / bounds.height
        assertTrue("アスペクト比が 0.5 ~ 2.0 の範囲", aspectRatio in 0.5f..2.0f)
    }

    // ----------------------------------------------------------------
    // 2. calculateViewBoundsForReducedBoard
    // ----------------------------------------------------------------

    @Test
    fun `calculateViewBoundsForReducedBoard - fewer tiles produce smaller bounds`() {
        val fullBounds = GameLogic.calculateViewBounds(GameConstants.initialTiles)

        // 中央の7タイルだけを使う（内輪）
        val innerTiles = GameConstants.initialTiles.take(7)
        val reducedBounds = GameLogic.calculateViewBounds(innerTiles)

        assertTrue(
            "タイルが少ないとバウンズ幅が小さくなる",
            reducedBounds.width <= fullBounds.width,
        )
        assertTrue(
            "タイルが少ないとバウンズ高さが小さくなる",
            reducedBounds.height <= fullBounds.height,
        )
    }

    // ----------------------------------------------------------------
    // 3. hexToPixelCenter
    // ----------------------------------------------------------------

    @Test
    fun `hexToPixelCenter - center tile converts to origin pixel position`() {
        val (px, py) = HexMath.hexToPixel(q = 0, r = 0)
        assertEquals("中央タイルの x は 0", 0f, px, 0.001f)
        assertEquals("中央タイルの y は 0", 0f, py, 0.001f)
    }

    // ----------------------------------------------------------------
    // 4. hexToPixelSymmetry
    // ----------------------------------------------------------------

    @Test
    fun `hexToPixelSymmetry - symmetric tiles produce symmetric pixel positions`() {
        // (1,0) と (-1,0) は q 軸で対称
        val pos1 = HexMath.hexToPixel(q = 1, r = 0)
        val neg1 = HexMath.hexToPixel(q = -1, r = 0)

        // x 座標は符号が反転（対称）
        assertEquals("対称タイルの x 座標は符号反転", pos1.first, -neg1.first, 0.001f)

        // y 座標も符号が反転（対称）
        assertEquals("対称タイルの y 座標は符号反転", pos1.second, -neg1.second, 0.001f)

        // (0,1) と (0,-1) は r 軸で対称
        val posR = HexMath.hexToPixel(q = 0, r = 1)
        val negR = HexMath.hexToPixel(q = 0, r = -1)

        assertEquals("r軸対称タイルの x 座標は同じ (=0)", posR.first, negR.first, 0.001f)
        assertEquals("r軸対称タイルの y 座標は符号反転", posR.second, -negR.second, 0.001f)
    }

    // ----------------------------------------------------------------
    // 5. hexagonPointsCount
    // ----------------------------------------------------------------

    @Test
    fun `hexagonPointsCount - hexagonPoints returns exactly 6 points`() {
        val points = HexMath.hexagonPoints(size = 36f)
        assertEquals("六角形は6頂点", 6, points.size)

        // 全頂点が原点から等距離（= size）であることを確認
        for ((x, y) in points) {
            val dist = sqrt(x * x + y * y)
            assertEquals("頂点は原点から size の距離にある", 36f, dist, 0.01f)
        }
    }

    // ----------------------------------------------------------------
    // 追加: ボード全体のピクセル座標の一貫性
    // ----------------------------------------------------------------

    @Test
    fun `allTilesWithinViewBounds - every tile pixel position is within calculated bounds`() {
        val bounds = GameLogic.calculateViewBounds(GameConstants.initialTiles)

        for (tile in GameConstants.initialTiles) {
            val (px, py) = HexMath.hexToPixel(tile.q, tile.r)
            assertTrue(
                "タイル (${tile.q},${tile.r}) の x=$px が bounds 内: [${bounds.minX}, ${bounds.maxX}]",
                px >= bounds.minX && px <= bounds.maxX,
            )
            assertTrue(
                "タイル (${tile.q},${tile.r}) の y=$py が bounds 内: [${bounds.minY}, ${bounds.maxY}]",
                py >= bounds.minY && py <= bounds.maxY,
            )
        }
    }
}
