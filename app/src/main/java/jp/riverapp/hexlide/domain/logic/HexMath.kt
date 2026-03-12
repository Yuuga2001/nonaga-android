package jp.riverapp.hexlide.domain.logic

import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.util.Constants
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object HexMath {
    val directions: List<Tile> = listOf(
        Tile(q = 1, r = 0),
        Tile(q = 1, r = -1),
        Tile(q = 0, r = -1),
        Tile(q = -1, r = 0),
        Tile(q = -1, r = 1),
        Tile(q = 0, r = 1),
    )

    fun coordsKey(q: Int, r: Int): String = "$q,$r"

    fun hexToPixel(
        q: Int,
        r: Int,
        hexSize: Float = Constants.Game.HEX_SIZE,
    ): Pair<Float, Float> {
        val x = hexSize * (3.0f / 2.0f * q.toFloat())
        val y = hexSize * (sqrt(3.0f) / 2.0f * q.toFloat() + sqrt(3.0f) * r.toFloat())
        return Pair(x, y)
    }

    fun isAdjacent(a: Tile, b: Tile): Boolean {
        return directions.any { d ->
            a.q + d.q == b.q && a.r + d.r == b.r
        }
    }

    fun distance(a: Tile, b: Tile): Int {
        val dq = abs(a.q - b.q)
        val dr = abs(a.r - b.r)
        val ds = abs((a.q + a.r) - (b.q + b.r))
        return maxOf(dq, dr, ds)
    }

    fun hexagonPoints(size: Float): List<Pair<Float, Float>> {
        return (0 until 6).map { i ->
            val angleDeg = 60.0 * i.toDouble() - 90.0
            val angleRad = angleDeg * Math.PI / 180.0
            Pair(
                (size * cos(angleRad)).toFloat(),
                (size * sin(angleRad)).toFloat(),
            )
        }
    }
}
