package jp.riverapp.hexlide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Tile(
    val q: Int,
    val r: Int,
) {
    val coordsKey: String
        get() = "$q,$r"
}
