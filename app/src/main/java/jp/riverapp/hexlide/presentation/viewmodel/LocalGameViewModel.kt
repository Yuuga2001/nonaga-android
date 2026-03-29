package jp.riverapp.hexlide.presentation.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.AIEngine
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalGameUiState(
    val tiles: List<Tile> = GameConstants.initialTiles,
    val pieces: List<Piece> = GameConstants.initialPieces,
    val turn: PlayerColor = PlayerColor.RED,
    val phase: GamePhase = GamePhase.MOVE_TOKEN,
    val selectedItem: SelectedItem? = null,
    val validDests: List<Tile> = emptyList(),
    val winner: PlayerColor? = null,
    val victoryLine: List<String> = emptyList(),
    val mode: GameMode = GameMode.AI,
    val myColor: PlayerColor = PlayerColor.RED,
    val isAnimating: Boolean = false,
    val isAIThinking: Boolean = false,
    val showModeSelector: Boolean = false,
    val showShuffle: Boolean = false,
) {
    val isMyTurn: Boolean
        get() = when (mode) {
            GameMode.AI -> turn == myColor
            GameMode.PVP -> true
            GameMode.ONLINE -> false
        }

    val currentPlayerColor: PlayerColor?
        get() = when (mode) {
            GameMode.AI -> myColor
            GameMode.PVP -> turn
            GameMode.ONLINE -> null
        }

    val movableTileIndices: Set<Int>
        get() = GameLogic.getMovableTileIndices(
            tiles = tiles,
            pieces = pieces,
            winner = winner,
            phase = phase,
        )
}

@HiltViewModel
class LocalGameViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LocalGameUiState())
    val uiState: StateFlow<LocalGameUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null
    private var initialized = false

    private fun updateState(block: LocalGameUiState.() -> LocalGameUiState) {
        _uiState.value = _uiState.value.block()
    }

    @VisibleForTesting
    internal fun setStateForTesting(modifier: (LocalGameUiState) -> LocalGameUiState) {
        _uiState.value = modifier(_uiState.value)
    }

    fun initializeIfNeeded() {
        if (!initialized) {
            initialized = true
            startNewGame()
        }
    }

    fun startNewGame() {
        currentJob?.cancel()
        currentJob = null

        val state = _uiState.value
        val mode = state.mode

        when (mode) {
            GameMode.AI -> {
                val myColor = if (Math.random() < 0.5) PlayerColor.RED else PlayerColor.BLUE
                _uiState.value = LocalGameUiState(
                    mode = GameMode.AI,
                    myColor = myColor,
                    showShuffle = true,
                )
                currentJob = viewModelScope.launch {
                    delay(1500)
                    ensureActive()
                    updateState { copy(showShuffle = false) }
                    if (_uiState.value.turn != _uiState.value.myColor) {
                        performAIMove()
                    }
                }
            }
            GameMode.PVP -> {
                _uiState.value = LocalGameUiState(
                    mode = GameMode.PVP,
                    myColor = PlayerColor.RED,
                )
            }
            GameMode.ONLINE -> {
                _uiState.value = LocalGameUiState(
                    mode = GameMode.ONLINE,
                )
            }
        }
    }

    fun handlePieceTap(piece: Piece) {
        val state = _uiState.value
        if (state.isAnimating) return
        if (state.winner != null) return
        if (state.phase != GamePhase.MOVE_TOKEN) return

        when (state.mode) {
            GameMode.AI -> if (piece.player != state.myColor) return
            GameMode.PVP -> if (piece.player != state.turn) return
            GameMode.ONLINE -> return
        }

        val currentSelected = state.selectedItem
        if (currentSelected is SelectedItem.PieceItem && currentSelected.pieceId == piece.id) {
            // Deselect
            updateState {
                copy(selectedItem = null, validDests = emptyList())
            }
        } else {
            // Select + calculate valid destinations
            val dests = GameLogic.getSlideDestinations(
                piece = piece,
                tiles = state.tiles,
                pieces = state.pieces,
            )
            updateState {
                copy(
                    selectedItem = SelectedItem.PieceItem(piece.id),
                    validDests = dests,
                )
            }
        }
    }

    fun handleTileTap(tile: Tile, index: Int) {
        val state = _uiState.value
        if (state.isAnimating) return
        if (state.winner != null) return
        if (state.phase != GamePhase.MOVE_TILE) return

        when (state.mode) {
            GameMode.AI -> if (state.turn != state.myColor) return
            GameMode.PVP -> { /* always allowed */ }
            GameMode.ONLINE -> return
        }

        // Check if a piece is on this tile
        val tileKey = tile.coordsKey
        if (state.pieces.any { it.coordsKey == tileKey }) return

        // Check if removing this tile keeps the board connected
        if (!GameLogic.isBoardConnected(state.tiles, excludeIndex = index)) return

        val currentSelected = state.selectedItem
        if (currentSelected is SelectedItem.TileIndexItem && currentSelected.index == index) {
            // Deselect
            updateState {
                copy(selectedItem = null, validDests = emptyList())
            }
        } else {
            // Select + calculate valid destinations
            val dests = GameLogic.getValidTileDestinations(
                selectedIndex = index,
                tiles = state.tiles,
            )
            updateState {
                copy(
                    selectedItem = SelectedItem.TileIndexItem(index),
                    validDests = dests,
                )
            }
        }
    }

    fun handleDestinationTap(dest: Tile) {
        val state = _uiState.value
        if (state.isAnimating) return
        if (state.winner != null) return

        val selected = state.selectedItem ?: return

        when {
            selected is SelectedItem.PieceItem && state.phase == GamePhase.MOVE_TOKEN -> {
                executePieceMove(selected.pieceId, dest)
            }
            selected is SelectedItem.TileIndexItem && state.phase == GamePhase.MOVE_TILE -> {
                executeTileMove(selected.index, dest)
            }
        }
    }

    private fun executePieceMove(pieceId: String, dest: Tile) {
        val state = _uiState.value
        val pieceIndex = state.pieces.indexOfFirst { it.id == pieceId }
        if (pieceIndex == -1) return

        currentJob?.cancel()

        val currentTurn = state.turn
        val updatedPieces = state.pieces.toMutableList()
        val oldPiece = updatedPieces[pieceIndex]
        updatedPieces[pieceIndex] = Piece(
            id = oldPiece.id,
            player = oldPiece.player,
            q = dest.q,
            r = dest.r,
        )

        updateState {
            copy(
                pieces = updatedPieces,
                selectedItem = null,
                validDests = emptyList(),
                isAnimating = true,
            )
        }

        currentJob = viewModelScope.launch {
            delay(850)
            ensureActive()
            updateState { copy(isAnimating = false) }

            // Victory check
            val victoryCoords = GameLogic.getVictoryCoords(
                pieces = _uiState.value.pieces,
                player = currentTurn,
            )
            if (victoryCoords != null) {
                updateState {
                    copy(
                        victoryLine = victoryCoords,
                        winner = currentTurn,
                        phase = GamePhase.ENDED,
                    )
                }
                return@launch
            }

            updateState { copy(phase = GamePhase.MOVE_TILE) }

            // AI mode: if it's AI's turn, perform AI tile move
            val currentState = _uiState.value
            if (currentState.mode == GameMode.AI && currentState.turn != currentState.myColor) {
                performAITileMove()
            }
        }
    }

    private fun executeTileMove(tileIndex: Int, dest: Tile) {
        val state = _uiState.value
        currentJob?.cancel()

        val updatedTiles = state.tiles.toMutableList()
        updatedTiles[tileIndex] = dest

        updateState {
            copy(
                tiles = updatedTiles,
                selectedItem = null,
                validDests = emptyList(),
                isAnimating = true,
            )
        }

        currentJob = viewModelScope.launch {
            delay(850)
            ensureActive()
            updateState { copy(isAnimating = false) }

            val nextTurn = _uiState.value.turn.opposite
            updateState {
                copy(
                    turn = nextTurn,
                    phase = GamePhase.MOVE_TOKEN,
                )
            }

            // AI mode: if next turn is AI's, perform AI move
            val currentState = _uiState.value
            if (currentState.mode == GameMode.AI && currentState.turn != currentState.myColor) {
                performAIMove()
            }
        }
    }

    private suspend fun performAIMove() {
        updateState { copy(isAIThinking = true) }
        delay(500)

        val state = _uiState.value
        val aiMove = AIEngine.choosePieceMove(
            pieces = state.pieces,
            tiles = state.tiles,
            aiColor = state.turn,
        )

        if (aiMove != null) {
            updateState { copy(isAIThinking = false) }
            executePieceMove(aiMove.pieceId, aiMove.destination)
            return
        }

        // Fallback: try all AI pieces for any valid move
        val aiPieces = state.pieces.filter { it.player == state.turn }
        for (piece in aiPieces) {
            val dests = GameLogic.getSlideDestinations(
                piece = piece,
                tiles = state.tiles,
                pieces = state.pieces,
            )
            if (dests.isNotEmpty()) {
                updateState { copy(isAIThinking = false) }
                executePieceMove(piece.id, dests.first())
                return
            }
        }

        // All stuck: skip to tile phase
        updateState {
            copy(
                isAIThinking = false,
                phase = GamePhase.MOVE_TILE,
            )
        }
        performAITileMove()
    }

    private suspend fun performAITileMove() {
        updateState { copy(isAIThinking = true) }
        delay(500)

        val state = _uiState.value
        val tileMove = AIEngine.chooseTileMove(
            pieces = state.pieces,
            tiles = state.tiles,
            aiColor = state.turn,
        )

        if (tileMove != null) {
            updateState { copy(isAIThinking = false) }
            executeTileMove(tileMove.tileIndex, tileMove.destination)
            return
        }

        // Fallback: try all movable tiles
        val movableIndices = state.movableTileIndices
        for (index in movableIndices) {
            val dests = GameLogic.getValidTileDestinations(
                selectedIndex = index,
                tiles = state.tiles,
            )
            if (dests.isNotEmpty()) {
                updateState { copy(isAIThinking = false) }
                executeTileMove(index, dests.first())
                return
            }
        }

        // All stuck: force turn switch
        updateState {
            copy(
                isAIThinking = false,
                turn = turn.opposite,
                phase = GamePhase.MOVE_TOKEN,
            )
        }
    }

    fun setShowModeSelector(show: Boolean) {
        updateState { copy(showModeSelector = show) }
    }

    fun switchMode(newMode: GameMode) {
        updateState {
            copy(
                mode = newMode,
                showModeSelector = false,
            )
        }
        if (newMode != GameMode.ONLINE) {
            startNewGame()
        }
    }
}
