package jp.riverapp.hexlide.domain.logic

import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.Tile

object GameConstants {
    val initialTiles: List<Tile> = listOf(
        Tile(q = 0, r = 0),
        Tile(q = 1, r = 0),
        Tile(q = 1, r = -1),
        Tile(q = 0, r = -1),
        Tile(q = -1, r = 0),
        Tile(q = -1, r = 1),
        Tile(q = 0, r = 1),
        Tile(q = 2, r = 0),
        Tile(q = 2, r = -1),
        Tile(q = 2, r = -2),
        Tile(q = 1, r = -2),
        Tile(q = 0, r = -2),
        Tile(q = -1, r = -1),
        Tile(q = -2, r = 0),
        Tile(q = -2, r = 1),
        Tile(q = -2, r = 2),
        Tile(q = -1, r = 2),
        Tile(q = 0, r = 2),
        Tile(q = 1, r = 1),
    )

    val initialPieces: List<Piece> = listOf(
        Piece(id = "r1", player = PlayerColor.RED, q = 2, r = -2),
        Piece(id = "b1", player = PlayerColor.BLUE, q = 2, r = 0),
        Piece(id = "r2", player = PlayerColor.RED, q = 0, r = 2),
        Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 2),
        Piece(id = "r3", player = PlayerColor.RED, q = -2, r = 0),
        Piece(id = "b3", player = PlayerColor.BLUE, q = 0, r = -2),
    )
}
