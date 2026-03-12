package jp.riverapp.hexlide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlayerColor {
    @SerialName("red")
    RED,

    @SerialName("blue")
    BLUE;

    val opposite: PlayerColor
        get() = when (this) {
            RED -> BLUE
            BLUE -> RED
        }
}
