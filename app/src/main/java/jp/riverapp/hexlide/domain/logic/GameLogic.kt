package jp.riverapp.hexlide.domain.logic

import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.Tile

/**
 * iOS版 GameLogic.swift の完全移植。
 *
 * ボード接続判定、スライド先計算、勝利判定、有効タイル移動先計算、
 * 移動可能タイルインデックス、プレイヤー色取得、ビュー境界計算を提供する。
 */
data class ViewBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val width: Float,
    val height: Float,
)

object GameLogic {

    /**
     * 指定されたピースが6方向にスライドした場合の到達先タイル一覧を返す。
     * ピースはタイルが途切れるか、他のピースにぶつかるまで一直線に滑る。
     */
    fun getSlideDestinations(piece: Piece, tiles: List<Tile>, pieces: List<Piece>): List<Tile> {
        val tileSet = tiles.map { it.coordsKey }.toSet()
        val pieceSet = pieces.map { it.coordsKey }.toSet()

        val destinations = mutableListOf<Tile>()

        for (dir in HexMath.directions) {
            var q = piece.q
            var r = piece.r
            var lastValid: Tile? = null

            while (true) {
                val nextQ = q + dir.q
                val nextR = r + dir.r
                val nextKey = HexMath.coordsKey(nextQ, nextR)

                if (!tileSet.contains(nextKey)) break
                if (pieceSet.contains(nextKey)) break

                q = nextQ
                r = nextR
                lastValid = Tile(q = q, r = r)
            }

            val valid = lastValid
            if (valid != null && (valid.q != piece.q || valid.r != piece.r)) {
                destinations.add(valid)
            }
        }

        return destinations
    }

    /**
     * BFS でボード上のタイルが全て接続されているかを判定する。
     * excludeIndex を指定すると、そのインデックスのタイルを除外して判定する。
     */
    fun isBoardConnected(tiles: List<Tile>, excludeIndex: Int? = null): Boolean {
        val filteredTiles: List<Tile> = if (excludeIndex != null) {
            tiles.filterIndexed { i, _ -> i != excludeIndex }
        } else {
            tiles
        }

        if (filteredTiles.isEmpty()) return true

        val tileSet = filteredTiles.map { it.coordsKey }.toSet()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Tile>()

        queue.add(filteredTiles[0])
        visited.add(filteredTiles[0].coordsKey)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (dir in HexMath.directions) {
                val neighborKey = HexMath.coordsKey(current.q + dir.q, current.r + dir.r)
                if (tileSet.contains(neighborKey) && !visited.contains(neighborKey)) {
                    visited.add(neighborKey)
                    queue.add(Tile(q = current.q + dir.q, r = current.r + dir.r))
                }
            }
        }

        return visited.size == filteredTiles.size
    }

    /**
     * 選択されたタイルを移動する際の有効な移動先タイルを返す。
     * 移動先は残りのタイルのうち2つ以上に隣接している空き位置でなければならない。
     */
    fun getValidTileDestinations(selectedIndex: Int, tiles: List<Tile>): List<Tile> {
        if (selectedIndex !in tiles.indices) return emptyList()

        val remaining = tiles.filterIndexed { i, _ -> i != selectedIndex }
        val selectedTile = tiles[selectedIndex]
        val remainingSet = remaining.map { it.coordsKey }.toSet()
        val selectedKey = selectedTile.coordsKey
        val candidates = mutableMapOf<String, Triple<Int, Int, Int>>() // key -> (q, r, count)

        for (t in remaining) {
            for (d in HexMath.directions) {
                val nQ = t.q + d.q
                val nR = t.r + d.r
                val key = HexMath.coordsKey(nQ, nR)

                if (remainingSet.contains(key) || key == selectedKey) continue

                val existing = candidates[key]
                if (existing != null) {
                    candidates[key] = Triple(existing.first, existing.second, existing.third + 1)
                } else {
                    candidates[key] = Triple(nQ, nR, 1)
                }
            }
        }

        return candidates.values
            .filter { it.third >= 2 }
            .map { Tile(q = it.first, r = it.second) }
    }

    /**
     * 勝利判定: 同色の3つのピースのうち、隣接ペアが2つ以上あれば勝利。
     * 勝利していればピースの座標キーリストを返し、勝利していなければ null を返す。
     */
    fun getVictoryCoords(pieces: List<Piece>, player: PlayerColor): List<String>? {
        val playerPieces = pieces.filter { it.player == player }
        if (playerPieces.size != 3) return null

        val p1 = playerPieces[0]
        val p2 = playerPieces[1]
        val p3 = playerPieces[2]

        val adj12 = HexMath.isAdjacent(p1.tile, p2.tile)
        val adj23 = HexMath.isAdjacent(p2.tile, p3.tile)
        val adj13 = HexMath.isAdjacent(p1.tile, p3.tile)

        val adjCount = listOf(adj12, adj23, adj13).count { it }
        return if (adjCount >= 2) {
            playerPieces.map { it.coordsKey }
        } else {
            null
        }
    }

    /**
     * 移動可能なタイルのインデックス集合を返す。
     * 勝者がいる場合や moveTile フェーズでない場合は空集合。
     * ピースが乗っているタイル、除去するとボードが分断されるタイルは除外。
     */
    fun getMovableTileIndices(
        tiles: List<Tile>,
        pieces: List<Piece>,
        winner: PlayerColor?,
        phase: GamePhase,
    ): Set<Int> {
        if (winner != null || phase != GamePhase.MOVE_TILE) return emptySet()

        val result = mutableSetOf<Int>()
        for (i in tiles.indices) {
            val key = HexMath.coordsKey(tiles[i].q, tiles[i].r)
            if (pieces.any { HexMath.coordsKey(it.q, it.r) == key }) continue
            if (isBoardConnected(tiles, excludeIndex = i)) {
                result.add(i)
            }
        }
        return result
    }

    /**
     * ゲームセッションとプレイヤーIDから、そのプレイヤーの色を返す。
     */
    fun getPlayerColor(game: GameSession, playerId: String): PlayerColor? {
        if (game.hostPlayerId == playerId) {
            return game.hostColor
        }
        if (game.guestPlayerId == playerId) {
            return if (game.hostColor == PlayerColor.RED) PlayerColor.BLUE else PlayerColor.RED
        }
        return null
    }

    /**
     * タイル一覧からビューの表示範囲を計算する。
     */
    fun calculateViewBounds(tiles: List<Tile>): ViewBounds {
        val padding = jp.riverapp.hexlide.util.Constants.Game.BOARD_PADDING
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (t in tiles) {
            val p = HexMath.hexToPixel(q = t.q, r = t.r)
            minX = minOf(minX, p.first)
            maxX = maxOf(maxX, p.first)
            minY = minOf(minY, p.second)
            maxY = maxOf(maxY, p.second)
        }

        return ViewBounds(
            minX = minX - padding,
            minY = minY - padding,
            maxX = maxX + padding,
            maxY = maxY + padding,
            width = maxX - minX + padding * 2,
            height = maxY - minY + padding * 2,
        )
    }
}
