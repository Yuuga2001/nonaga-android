package jp.riverapp.hexlide.presentation.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.data.repository.GameRepository
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.service.GamePollingService
import jp.riverapp.hexlide.domain.service.PlayerIdentityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineGameUiState(
    val gameSession: GameSession? = null,
    val tiles: List<Tile> = GameConstants.initialTiles,
    val pieces: List<Piece> = GameConstants.initialPieces,
    val turn: PlayerColor = PlayerColor.RED,
    val phase: GamePhase = GamePhase.MOVE_TOKEN,
    val selectedItem: SelectedItem? = null,
    val validDests: List<Tile> = emptyList(),
    val winner: PlayerColor? = null,
    val victoryLine: List<String> = emptyList(),
    val myColor: PlayerColor? = null,
    val isAnimating: Boolean = false,
    val error: String? = null,
    val isPolling: Boolean = false,
    val isAbandoned: Boolean = false,
    val connectionError: Boolean = false,
    val isLoading: Boolean = true,
    val isReconnecting: Boolean = false,
    val showEndConfirm: Boolean = false,
    val shouldNavigateBack: Boolean = false,
) {
    val isMyTurn: Boolean
        get() {
            val session = gameSession ?: return false
            val color = myColor ?: return false
            return session.turn == color && session.status == GameStatus.PLAYING
        }

    val movableTileIndices: Set<Int>
        get() {
            val session = gameSession ?: return emptySet()
            return GameLogic.getMovableTileIndices(
                tiles = session.tiles,
                pieces = session.pieces,
                winner = session.winner,
                phase = session.phase,
            )
        }
}

@HiltViewModel
class OnlineGameViewModel @Inject constructor(
    private val repository: GameRepository,
    private val pollingService: GamePollingService,
    private val playerIdentityService: PlayerIdentityService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineGameUiState())
    val uiState: StateFlow<OnlineGameUiState> = _uiState.asStateFlow()

    private var gameId: String = ""
    private val playerId: String
        get() = playerIdentityService.getPlayerId()
    private var lastUpdatedAt: String? = null

    private fun updateState(block: OnlineGameUiState.() -> OnlineGameUiState) {
        _uiState.value = _uiState.value.block()
    }

    @VisibleForTesting
    internal fun setStateForTesting(modifier: (OnlineGameUiState) -> OnlineGameUiState) {
        _uiState.value = modifier(_uiState.value)
    }

    fun startGame(gameId: String) {
        this.gameId = gameId
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            try {
                var fetched = repository.getGame(gameId)

                // If waiting and we're guest, join
                if (fetched.status == GameStatus.WAITING && fetched.hostPlayerId != playerId) {
                    fetched = repository.joinGame(gameId)
                }

                lastUpdatedAt = fetched.updatedAt
                applyGameSession(fetched)
                updateState { copy(isLoading = false) }
            } catch (e: Exception) {
                updateState {
                    copy(
                        error = e.message ?: "Unknown error",
                        isLoading = false,
                    )
                }
            }

            startPolling()
        }
    }

    private fun applyGameSession(session: GameSession) {
        val color = GameLogic.getPlayerColor(session, playerId)
        updateState {
            copy(
                gameSession = session,
                tiles = session.tiles,
                pieces = session.pieces,
                turn = session.turn,
                phase = session.phase,
                winner = session.winner,
                victoryLine = session.victoryLine ?: emptyList(),
                myColor = color,
                isAbandoned = session.status == GameStatus.ABANDONED,
            )
        }
    }

    private fun startPolling() {
        pollingService.startPolling(
            scope = viewModelScope,
            gameId = gameId,
            onUpdate = { updatedGame -> handlePollingUpdate(updatedGame) },
            onError = {
                updateState { copy(isReconnecting = true) }
            },
        )
        updateState { copy(isPolling = true) }
    }

    private fun handlePollingUpdate(updatedGame: GameSession) {
        if (updatedGame.updatedAt == lastUpdatedAt) return
        lastUpdatedAt = updatedGame.updatedAt

        updateState { copy(isReconnecting = false) }

        if (updatedGame.status == GameStatus.ABANDONED) {
            applyGameSession(updatedGame)
            return
        }

        // Skip polling updates while animating (optimistic update in progress)
        if (_uiState.value.isAnimating) return

        applyGameSession(updatedGame)
    }

    fun handlePieceTap(piece: Piece) {
        val state = _uiState.value
        if (state.isAnimating) return
        val session = state.gameSession ?: return
        if (session.winner != null) return
        if (session.phase != GamePhase.MOVE_TOKEN) return
        if (!state.isMyTurn) return
        if (piece.player != state.myColor) return

        val currentSelected = state.selectedItem
        if (currentSelected is SelectedItem.PieceItem && currentSelected.pieceId == piece.id) {
            updateState { copy(selectedItem = null, validDests = emptyList()) }
            return
        }

        val dests = GameLogic.getSlideDestinations(
            piece = piece,
            tiles = session.tiles,
            pieces = session.pieces,
        )
        updateState {
            copy(
                selectedItem = SelectedItem.PieceItem(piece.id),
                validDests = dests,
            )
        }
    }

    fun handleTileTap(tile: Tile, index: Int) {
        val state = _uiState.value
        if (state.isAnimating) return
        val session = state.gameSession ?: return
        if (session.winner != null) return
        if (session.phase != GamePhase.MOVE_TILE) return
        if (!state.isMyTurn) return

        val tileKey = tile.coordsKey
        if (session.pieces.any { it.coordsKey == tileKey }) return

        val currentSelected = state.selectedItem
        if (currentSelected is SelectedItem.TileIndexItem && currentSelected.index == index) {
            updateState { copy(selectedItem = null, validDests = emptyList()) }
            return
        }

        if (!GameLogic.isBoardConnected(session.tiles, excludeIndex = index)) return

        val dests = GameLogic.getValidTileDestinations(
            selectedIndex = index,
            tiles = session.tiles,
        )
        updateState {
            copy(
                selectedItem = SelectedItem.TileIndexItem(index),
                validDests = dests,
            )
        }
    }

    fun handleDestinationTap(dest: Tile) {
        val state = _uiState.value
        if (state.isAnimating) return

        when (val selected = state.selectedItem) {
            is SelectedItem.PieceItem -> sendPieceMove(selected.pieceId, dest)
            is SelectedItem.TileIndexItem -> sendTileMove(selected.index, dest)
            null -> return
        }
    }

    private fun sendPieceMove(pieceId: String, dest: Tile) {
        updateState {
            copy(
                isAnimating = true,
                selectedItem = null,
                validDests = emptyList(),
            )
        }

        // Optimistic update
        val currentPieces = _uiState.value.pieces.toMutableList()
        val idx = currentPieces.indexOfFirst { it.id == pieceId }
        if (idx >= 0) {
            val old = currentPieces[idx]
            currentPieces[idx] = Piece(id = pieceId, player = old.player, q = dest.q, r = dest.r)
            updateState { copy(pieces = currentPieces) }
        }

        viewModelScope.launch {
            try {
                val updated = repository.movePiece(
                    gameId = gameId,
                    pieceId = pieceId,
                    toQ = dest.q,
                    toR = dest.r,
                )
                delay(500)
                if (lastUpdatedAt == null || updated.updatedAt >= (lastUpdatedAt ?: "")) {
                    lastUpdatedAt = updated.updatedAt
                    applyGameSession(updated)
                }
            } catch (_: Exception) {
                // Refresh on error
                try {
                    val refreshed = repository.getGame(gameId)
                    lastUpdatedAt = refreshed.updatedAt
                    applyGameSession(refreshed)
                } catch (_: Exception) {
                    // Ignore refresh failure
                }
            }
            updateState { copy(isAnimating = false) }
        }
    }

    private fun sendTileMove(tileIndex: Int, dest: Tile) {
        updateState {
            copy(
                isAnimating = true,
                selectedItem = null,
                validDests = emptyList(),
            )
        }

        // Optimistic update
        val currentTiles = _uiState.value.tiles.toMutableList()
        if (tileIndex < currentTiles.size) {
            currentTiles[tileIndex] = dest
            updateState { copy(tiles = currentTiles) }
        }

        viewModelScope.launch {
            try {
                val updated = repository.moveTile(
                    gameId = gameId,
                    tileIndex = tileIndex,
                    toQ = dest.q,
                    toR = dest.r,
                )
                delay(500)
                if (lastUpdatedAt == null || updated.updatedAt >= (lastUpdatedAt ?: "")) {
                    lastUpdatedAt = updated.updatedAt
                    applyGameSession(updated)
                }
            } catch (_: Exception) {
                try {
                    val refreshed = repository.getGame(gameId)
                    lastUpdatedAt = refreshed.updatedAt
                    applyGameSession(refreshed)
                } catch (_: Exception) {
                    // Ignore refresh failure
                }
            }
            updateState { copy(isAnimating = false) }
        }
    }

    fun abandonGame() {
        updateState { copy(shouldNavigateBack = true) }
        viewModelScope.launch {
            try {
                repository.abandonGame(gameId)
            } catch (_: Exception) {
                // Ignore - navigation already triggered
            }
        }
    }

    fun endGame() {
        abandonGame()
    }

    fun requestRematch() {
        viewModelScope.launch {
            try {
                val newGame = repository.rematch(gameId)
                lastUpdatedAt = newGame.updatedAt
                updateState {
                    copy(selectedItem = null, validDests = emptyList())
                }
                applyGameSession(newGame)
            } catch (e: Exception) {
                updateState { copy(error = e.message) }
            }
        }
    }

    fun shareGameLink(): String {
        return "https://hexlide.riverapp.jp/game/$gameId"
    }

    fun stopPolling() {
        pollingService.stopPolling()
        updateState { copy(isPolling = false) }
    }

    fun retryPolling() {
        updateState { copy(isReconnecting = false) }
        startPolling()
    }

    fun setShowEndConfirm(show: Boolean) {
        updateState { copy(showEndConfirm = show) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
