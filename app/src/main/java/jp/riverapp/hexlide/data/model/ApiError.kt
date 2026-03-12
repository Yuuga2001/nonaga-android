package jp.riverapp.hexlide.data.model

sealed class ApiError(override val message: String) : Exception(message) {
    data object InvalidURL : ApiError("Invalid URL")
    data object InvalidResponse : ApiError("Invalid server response")
    data class HttpError(val statusCode: Int, val errorMessage: String?) :
        ApiError("HTTP $statusCode: ${errorMessage ?: "Unknown error"}")
    data class DecodingError(val errorMessage: String) :
        ApiError("Decoding error: $errorMessage")
    data class NetworkError(val errorMessage: String) :
        ApiError("Network error: $errorMessage")
    data object GameNotFound : ApiError("Game not found")
    data object GameAlreadyStarted : ApiError("Game already started")
    data object NotYourTurn : ApiError("Not your turn")
    data object InvalidMove : ApiError("Invalid move")

    val errorDescription: String
        get() = message
}
