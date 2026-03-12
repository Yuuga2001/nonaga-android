package jp.riverapp.hexlide.viewmodel

import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameUiState
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * LocalGameViewModel のユニットテスト。
 * iOS版 LocalGameViewModelTests.swift に対応。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalGameViewModelTests {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LocalGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LocalGameViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** PvP mode default state helper */
    private fun setupPvPMode() {
        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.PVP,
                myColor = PlayerColor.RED,
                tiles = GameConstants.initialTiles,
                pieces = GameConstants.initialPieces,
                turn = PlayerColor.RED,
                phase = GamePhase.MOVE_TOKEN,
                selectedItem = null,
                validDests = emptyList(),
                winner = null,
                victoryLine = emptyList(),
                isAnimating = false,
                isAIThinking = false,
                showShuffle = false,
            )
        }
    }

    // ----------------------------------------------------------------
    // 1. initialState
    // ----------------------------------------------------------------

    @Test
    fun `initialState - PvP mode has correct defaults`() {
        setupPvPMode()
        val state = viewModel.uiState.value
        assertEquals(19, state.tiles.size)
        assertEquals(6, state.pieces.size)
        assertEquals(PlayerColor.RED, state.turn)
        assertEquals(GamePhase.MOVE_TOKEN, state.phase)
        assertNull(state.winner)
        assertNull(state.selectedItem)
        assertTrue(state.validDests.isEmpty())
        assertTrue(state.victoryLine.isEmpty())
        assertFalse(state.isAnimating)
        assertFalse(state.isAIThinking)
        assertFalse(state.showShuffle)
    }

    // ----------------------------------------------------------------
    // 2. selectPiece
    // ----------------------------------------------------------------

    @Test
    fun `selectPiece - tapping red piece selects it with valid destinations`() {
        setupPvPMode()
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedItem)
        assertTrue(state.selectedItem is SelectedItem.PieceItem)
        assertEquals(redPiece.id, (state.selectedItem as SelectedItem.PieceItem).pieceId)
        assertTrue(state.validDests.isNotEmpty())
    }

    // ----------------------------------------------------------------
    // 3. cannotSelectOpponent
    // ----------------------------------------------------------------

    @Test
    fun `cannotSelectOpponent - tapping blue piece during red turn does nothing`() {
        setupPvPMode()
        val bluePiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.BLUE }
        viewModel.handlePieceTap(bluePiece)

        val state = viewModel.uiState.value
        assertNull(state.selectedItem)
    }

    // ----------------------------------------------------------------
    // 4. deselectPiece
    // ----------------------------------------------------------------

    @Test
    fun `deselectPiece - tapping same piece twice deselects`() {
        setupPvPMode()
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        // Confirm selected
        assertNotNull(viewModel.uiState.value.selectedItem)

        // Tap again to deselect
        viewModel.handlePieceTap(redPiece)

        val state = viewModel.uiState.value
        assertNull(state.selectedItem)
        assertTrue(state.validDests.isEmpty())
    }

    // ----------------------------------------------------------------
    // 5. modeSwitchResetsGame
    // ----------------------------------------------------------------

    @Test
    fun `modeSwitchResetsGame - switching to PvP resets state`() {
        // Start in AI mode and select something
        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.AI,
                myColor = PlayerColor.RED,
                winner = PlayerColor.RED,
            )
        }

        viewModel.switchMode(GameMode.PVP)

        val state = viewModel.uiState.value
        assertEquals(GameMode.PVP, state.mode)
        assertEquals(19, state.tiles.size)
        assertNull(state.winner)
    }

    // ----------------------------------------------------------------
    // 6. isMyTurnPvP
    // ----------------------------------------------------------------

    @Test
    fun `isMyTurnPvP - PvP mode always returns true`() {
        setupPvPMode()
        assertTrue(viewModel.uiState.value.isMyTurn)

        // Even when it's BLUE's turn
        viewModel.setStateForTesting { it.copy(turn = PlayerColor.BLUE) }
        assertTrue(viewModel.uiState.value.isMyTurn)
    }

    // ----------------------------------------------------------------
    // 7. aiModeInit
    // ----------------------------------------------------------------

    @Test
    fun `aiModeInit - AI mode startNewGame sets myColor and showShuffle`() {
        viewModel.setStateForTesting { it.copy(mode = GameMode.AI) }
        viewModel.startNewGame()

        val state = viewModel.uiState.value
        assertTrue(
            "myColor should be RED or BLUE",
            state.myColor == PlayerColor.RED || state.myColor == PlayerColor.BLUE,
        )
        assertTrue(state.showShuffle)
    }

    // ----------------------------------------------------------------
    // 8. restartClearsState
    // ----------------------------------------------------------------

    @Test
    fun `restartClearsState - starting new game resets everything`() {
        setupPvPMode()
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        // Confirm selected
        assertNotNull(viewModel.uiState.value.selectedItem)

        viewModel.startNewGame()

        val state = viewModel.uiState.value
        assertNull(state.selectedItem)
        assertTrue(state.validDests.isEmpty())
        assertNull(state.winner)
        assertEquals(GamePhase.MOVE_TOKEN, state.phase)
    }

    // ----------------------------------------------------------------
    // 9. cannotInteractAfterWin
    // ----------------------------------------------------------------

    @Test
    fun `cannotInteractAfterWin - piece tap ignored when winner exists`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(winner = PlayerColor.RED) }

        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        assertNull(viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 10. switchModeToPvp
    // ----------------------------------------------------------------

    @Test
    fun `switchModeToPvp - switching to PvP sets correct state`() {
        viewModel.setStateForTesting { it.copy(mode = GameMode.AI) }
        viewModel.switchMode(GameMode.PVP)

        val state = viewModel.uiState.value
        assertEquals(GameMode.PVP, state.mode)
        assertEquals(PlayerColor.RED, state.myColor)
        assertFalse(state.showShuffle)
    }

    // ----------------------------------------------------------------
    // 11. handleTileTapWithPieceOnTile
    // ----------------------------------------------------------------

    @Test
    fun `handleTileTapWithPieceOnTile - tile with piece cannot be selected`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(phase = GamePhase.MOVE_TILE) }

        // Find a tile that has a piece on it
        val state = viewModel.uiState.value
        val pieceOnTile = state.pieces.first()
        val tileIndex = state.tiles.indexOfFirst { it.coordsKey == pieceOnTile.coordsKey }
        assertTrue("Should find a tile with a piece", tileIndex >= 0)

        viewModel.handleTileTap(state.tiles[tileIndex], tileIndex)

        assertNull(viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 12. cannotSelectPieceInTilePhase
    // ----------------------------------------------------------------

    @Test
    fun `cannotSelectPieceInTilePhase - piece tap ignored in MOVE_TILE phase`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(phase = GamePhase.MOVE_TILE) }

        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        assertNull(viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 13. cannotSelectTileInTokenPhase
    // ----------------------------------------------------------------

    @Test
    fun `cannotSelectTileInTokenPhase - tile tap ignored in MOVE_TOKEN phase`() {
        setupPvPMode()
        // phase is MOVE_TOKEN by default

        // Find a tile without a piece
        val state = viewModel.uiState.value
        val pieceKeys = state.pieces.map { it.coordsKey }.toSet()
        val tileIndex = state.tiles.indexOfFirst { !pieceKeys.contains(it.coordsKey) }
        assertTrue("Should find an empty tile", tileIndex >= 0)

        viewModel.handleTileTap(state.tiles[tileIndex], tileIndex)

        assertNull(viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 14. deselectTile
    // ----------------------------------------------------------------

    @Test
    fun `deselectTile - tapping same tile twice deselects`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(phase = GamePhase.MOVE_TILE) }

        // Find a movable tile (no piece, keeps board connected)
        val state = viewModel.uiState.value
        val movableIndices = state.movableTileIndices
        assertTrue("Should have movable tiles", movableIndices.isNotEmpty())

        val tileIndex = movableIndices.first()
        val tile = state.tiles[tileIndex]

        // First tap: select
        viewModel.handleTileTap(tile, tileIndex)
        assertNotNull(viewModel.uiState.value.selectedItem)

        // Second tap: deselect
        viewModel.handleTileTap(tile, tileIndex)
        assertNull(viewModel.uiState.value.selectedItem)
        assertTrue(viewModel.uiState.value.validDests.isEmpty())
    }

    // ----------------------------------------------------------------
    // 15. cannotInteractWhileAnimating
    // ----------------------------------------------------------------

    @Test
    fun `cannotInteractWhileAnimating - piece tap ignored when animating`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(isAnimating = true) }

        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        assertNull(viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 16. currentPlayerColorPvP
    // ----------------------------------------------------------------

    @Test
    fun `currentPlayerColorPvP - returns current turn in PvP mode`() {
        setupPvPMode()
        assertEquals(PlayerColor.RED, viewModel.uiState.value.currentPlayerColor)

        viewModel.setStateForTesting { it.copy(turn = PlayerColor.BLUE) }
        assertEquals(PlayerColor.BLUE, viewModel.uiState.value.currentPlayerColor)
    }

    // ----------------------------------------------------------------
    // 17. currentPlayerColorAI
    // ----------------------------------------------------------------

    @Test
    fun `currentPlayerColorAI - returns myColor in AI mode`() {
        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.AI,
                myColor = PlayerColor.BLUE,
                turn = PlayerColor.RED,
            )
        }
        assertEquals(PlayerColor.BLUE, viewModel.uiState.value.currentPlayerColor)
    }
}
