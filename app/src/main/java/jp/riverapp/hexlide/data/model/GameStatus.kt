package jp.riverapp.hexlide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameStatus(val value: String) {
    @SerialName("WAITING")
    WAITING("WAITING"),

    @SerialName("PLAYING")
    PLAYING("PLAYING"),

    @SerialName("FINISHED")
    FINISHED("FINISHED"),

    @SerialName("ABANDONED")
    ABANDONED("ABANDONED");
}
