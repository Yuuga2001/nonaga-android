package jp.riverapp.hexlide.domain.logic

import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.Tile
import kotlin.math.abs

/**
 * iOS版 AIEngine.swift の完全移植。
 *
 * AI がピース移動・タイル移動を選択するためのエンジン。
 * ヒューリスティック評価関数でスコアリングし、最もスコアの高い手を選択する。
 */
data class PieceMove(
    val pieceId: String,
    val destination: Tile,
)

data class TileMove(
    val tileIndex: Int,
    val destination: Tile,
)

object AIEngine {

    /**
     * AI のピース移動を選択する。
     * 自分のピースの全スライド先を評価し、最高スコアの手を返す。
     * 移動可能な手がなければ null を返す。
     */
    fun choosePieceMove(
        pieces: List<Piece>,
        tiles: List<Tile>,
        aiColor: PlayerColor,
    ): PieceMove? {
        val aiPieces = pieces.filter { it.player == aiColor }
        val enemyColor = aiColor.opposite

        var bestMove: PieceMove? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for (piece in aiPieces) {
            val dests = GameLogic.getSlideDestinations(piece = piece, tiles = tiles, pieces = pieces)
            for (dest in dests) {
                val newPieces = pieces.toMutableList()
                val idx = newPieces.indexOfFirst { it.id == piece.id }
                if (idx != -1) {
                    newPieces[idx] = Piece(id = piece.id, player = piece.player, q = dest.q, r = dest.r)
                }
                val score = evaluatePiecePosition(
                    pieces = newPieces,
                    aiColor = aiColor,
                    enemyColor = enemyColor,
                )
                if (score > bestScore) {
                    bestScore = score
                    bestMove = PieceMove(pieceId = piece.id, destination = dest)
                }
            }
        }

        return bestMove
    }

    /**
     * AI のタイル移動を選択する。
     * ピースが乗っていないタイルを全て評価し、最高スコアの手を返す。
     * 移動可能な手がなければ null を返す。
     */
    fun chooseTileMove(
        pieces: List<Piece>,
        tiles: List<Tile>,
        aiColor: PlayerColor,
    ): TileMove? {
        val enemyColor = aiColor.opposite
        val pieceSet = pieces.map { it.coordsKey }.toSet()

        var bestMove: TileMove? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for ((i, tile) in tiles.withIndex()) {
            val key = tile.coordsKey
            if (pieceSet.contains(key)) continue
            if (!GameLogic.isBoardConnected(tiles = tiles, excludeIndex = i)) continue

            val dests = GameLogic.getValidTileDestinations(selectedIndex = i, tiles = tiles)
            for (dest in dests) {
                val newTiles = tiles.toMutableList()
                newTiles[i] = dest

                if (!GameLogic.isBoardConnected(tiles = newTiles)) continue

                val score = evaluateTilePosition(
                    pieces = pieces,
                    tiles = newTiles,
                    aiColor = aiColor,
                    enemyColor = enemyColor,
                    movedTileIndex = i,
                    originalTile = tile,
                )

                if (score > bestScore) {
                    bestScore = score
                    bestMove = TileMove(tileIndex = i, destination = dest)
                }
            }
        }

        return bestMove
    }

    /**
     * ピース配置の評価関数。
     *
     * スコアリング重み:
     * - 自分の隣接ペア * 500
     * - 最短ペア距離 * -30
     * - コンパクトさ(重心からの距離合計) * -20
     * - 敵の隣接ペア * -200
     * - 中央からの距離合計 * -5
     * - ノイズ 0~3
     * - 勝利判定: 隣接ペア >= 2 なら 10000
     */
    private fun evaluatePiecePosition(
        pieces: List<Piece>,
        aiColor: PlayerColor,
        enemyColor: PlayerColor,
    ): Double {
        val aiPieces = pieces.filter { it.player == aiColor }
        val enemyPieces = pieces.filter { it.player == enemyColor }

        // 勝利判定
        val aiAdjPairs = countAdjacentPairs(aiPieces)
        if (aiAdjPairs >= 2) return 10000.0

        // 敵の状態
        val enemyAdjPairs = countAdjacentPairs(enemyPieces)

        // 距離計算
        var minDist = Double.MAX_VALUE
        var totalDist = 0.0
        for (i in aiPieces.indices) {
            for (j in (i + 1) until aiPieces.size) {
                val d = HexMath.distance(aiPieces[i].tile, aiPieces[j].tile).toDouble()
                minDist = minOf(minDist, d)
                totalDist += d
            }
        }

        // コンパクトさ (重心からの距離)
        val cx = aiPieces.sumOf { it.q }.toDouble() / 3.0
        val cr = aiPieces.sumOf { it.r }.toDouble() / 3.0
        val compactness = aiPieces.sumOf { p ->
            abs(p.q.toDouble() - cx) + abs(p.r.toDouble() - cr)
        }

        // 中央バイアス
        val centerDist = aiPieces.sumOf { p ->
            (abs(p.q) + abs(p.r) + abs(p.q + p.r)).toDouble() / 2.0
        }

        val noise = Math.random() * 3.0

        return aiAdjPairs.toDouble() * 500 -
            minDist * 30 -
            compactness * 20 -
            enemyAdjPairs.toDouble() * 200 -
            centerDist * 5 +
            noise
    }

    /**
     * タイル配置の評価関数。
     *
     * スコアリング重み:
     * - 敵が次のターンで勝てる場合: -5000
     * - 自分の隣接ペア * 300
     * - 敵の隣接ペア * -200
     * - 最短ペア距離 * -15
     * - ノイズ 0~2
     */
    @Suppress("UNUSED_PARAMETER")
    private fun evaluateTilePosition(
        pieces: List<Piece>,
        tiles: List<Tile>,
        aiColor: PlayerColor,
        enemyColor: PlayerColor,
        movedTileIndex: Int,
        originalTile: Tile,
    ): Double {
        val enemyPieces = pieces.filter { it.player == enemyColor }
        val aiPieces = pieces.filter { it.player == aiColor }

        // 敵が次のターンで勝てるか確認
        var enemyCanWinScore = 0.0
        run enemyWinCheck@{
            for (ep in enemyPieces) {
                val dests = GameLogic.getSlideDestinations(piece = ep, tiles = tiles, pieces = pieces)
                for (dest in dests) {
                    val testPieces = pieces.toMutableList()
                    val idx = testPieces.indexOfFirst { it.id == ep.id }
                    if (idx != -1) {
                        testPieces[idx] = Piece(id = ep.id, player = ep.player, q = dest.q, r = dest.r)
                    }
                    if (countAdjacentPairs(testPieces.filter { it.player == enemyColor }) >= 2) {
                        enemyCanWinScore = -5000.0
                        return@enemyWinCheck
                    }
                }
            }
        }

        val enemyAdjPairs = countAdjacentPairs(enemyPieces)
        val aiAdjPairs = countAdjacentPairs(aiPieces)

        // 距離最適化
        var minDist = Double.MAX_VALUE
        for (i in aiPieces.indices) {
            for (j in (i + 1) until aiPieces.size) {
                val d = HexMath.distance(aiPieces[i].tile, aiPieces[j].tile).toDouble()
                minDist = minOf(minDist, d)
            }
        }

        val noise = Math.random() * 2.0

        return enemyCanWinScore +
            aiAdjPairs.toDouble() * 300 -
            enemyAdjPairs.toDouble() * 200 -
            minDist * 15 +
            noise
    }

    /**
     * ピースリスト内の隣接ペア数をカウントする。
     */
    internal fun countAdjacentPairs(pieces: List<Piece>): Int {
        var count = 0
        for (i in pieces.indices) {
            for (j in (i + 1) until pieces.size) {
                if (HexMath.isAdjacent(pieces[i].tile, pieces[j].tile)) {
                    count++
                }
            }
        }
        return count
    }
}
