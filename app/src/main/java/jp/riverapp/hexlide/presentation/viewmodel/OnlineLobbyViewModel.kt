package jp.riverapp.hexlide.presentation.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.repository.GameRepository
import jp.riverapp.hexlide.domain.service.GamePollingService
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineLobbyUiState(
    val gameSession: GameSession? = null,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null,
    val gameIdInput: String = "",
    val joinedGameId: String? = null,
)

@HiltViewModel
class OnlineLobbyViewModel @Inject constructor(
    private val repository: GameRepository,
    private val pollingService: GamePollingService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineLobbyUiState())
    val uiState: StateFlow<OnlineLobbyUiState> = _uiState.asStateFlow()

    private fun updateState(block: OnlineLobbyUiState.() -> OnlineLobbyUiState) {
        _uiState.value = _uiState.value.block()
    }

    @VisibleForTesting
    internal fun setStateForTesting(modifier: (OnlineLobbyUiState) -> OnlineLobbyUiState) {
        _uiState.value = modifier(_uiState.value)
    }

    fun createGame() {
        updateState { copy(isCreating = true, error = null) }
        viewModelScope.launch {
            try {
                val game = repository.createGame()
                updateState {
                    copy(
                        gameSession = game,
                        joinedGameId = game.gameId,
                        isCreating = false,
                    )
                }
                // ルーム作成後、対戦相手の参加を検出するためポーリング開始
                startWaitingPolling(game.gameId)
            } catch (e: Exception) {
                updateState {
                    copy(
                        error = e.message ?: "Unknown error",
                        isCreating = false,
                    )
                }
            }
        }
    }

    private fun startWaitingPolling(gameId: String) {
        pollingService.startPolling(
            scope = viewModelScope,
            gameId = gameId,
            onUpdate = { updatedGame ->
                updateState { copy(gameSession = updatedGame) }
                // ゲームが開始されたらポーリング停止（ナビゲーションはUIのLaunchedEffectが処理）
                if (updatedGame.status != GameStatus.WAITING) {
                    pollingService.stopPolling()
                }
            },
            onError = {
                updateState { copy(error = "connection_error") }
            },
        )
    }

    fun stopPolling() {
        pollingService.stopPolling()
    }

    fun joinWithRoomCode() {
        val normalized = _uiState.value.gameIdInput.filter { it.isDigit() }
        if (normalized.length != 6) {
            updateState { copy(error = "invalid_room_code") }
            return
        }

        updateState { copy(isJoining = true, error = null) }
        viewModelScope.launch {
            try {
                val game = repository.getGameByRoomCode(normalized)

                if (game.status != GameStatus.WAITING) {
                    updateState {
                        copy(
                            error = "game_already_started",
                            isJoining = false,
                        )
                    }
                    return@launch
                }

                val joined = repository.joinGame(game.gameId)
                updateState {
                    copy(
                        joinedGameId = joined.gameId,
                        isJoining = false,
                    )
                }
            } catch (e: HttpException) {
                val errorKey = when (e.code()) {
                    404 -> "game_not_found"
                    409 -> "game_already_started"
                    else -> "connection_error"
                }
                updateState { copy(error = errorKey, isJoining = false) }
            } catch (e: Exception) {
                updateState {
                    copy(
                        error = "connection_error",
                        isJoining = false,
                    )
                }
            }
        }
    }

    fun joinGame(gameId: String) {
        updateState { copy(isJoining = true, error = null) }
        viewModelScope.launch {
            try {
                val game = repository.joinGame(gameId)
                updateState {
                    copy(
                        gameSession = game,
                        joinedGameId = game.gameId,
                        isJoining = false,
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        error = e.message ?: "Unknown error",
                        isJoining = false,
                    )
                }
            }
        }
    }

    fun joinGameFromDeepLink(gameId: String) {
        joinGame(gameId)
    }

    fun clearError() {
        updateState { copy(error = null) }
    }

    fun updateGameIdInput(input: String) {
        updateState { copy(gameIdInput = input) }
    }

    fun reset() {
        pollingService.stopPolling()
        _uiState.value = OnlineLobbyUiState()
    }

    override fun onCleared() {
        super.onCleared()
        pollingService.stopPolling()
    }
}
