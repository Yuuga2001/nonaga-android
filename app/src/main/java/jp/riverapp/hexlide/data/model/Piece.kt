package jp.riverapp.hexlide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Piece(
    val id: String,
    val player: PlayerColor,
    val q: Int,
    val r: Int,
) {
    val coordsKey: String
        get() = "$q,$r"

    val tile: Tile
        get() = Tile(q = q, r = r)
}
