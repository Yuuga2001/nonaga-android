package jp.riverapp.hexlide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase(val value: String) {
    @SerialName("move_token")
    MOVE_TOKEN("move_token"),

    @SerialName("move_tile")
    MOVE_TILE("move_tile"),

    @SerialName("waiting")
    WAITING("waiting"),

    @SerialName("ended")
    ENDED("ended");
}
