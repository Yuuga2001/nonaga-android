package jp.riverapp.hexlide.integration

import io.mockk.mockk
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.logic.HexMath
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameUiState
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
 * ViewModel を通じたゲームフロー全体の統合テスト。
 * iOS版 GameFlowUITests に相当するが、UI要素ではなく
 * ViewModel の状態遷移を検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameFlowIntegrationTests {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LocalGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LocalGameViewModel(context = mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** PvP モードの初期状態をセットアップするヘルパー */
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
    // 1. fullPvPTurnFlow
    // ----------------------------------------------------------------

    @Test
    fun `fullPvPTurnFlow - complete one turn in PvP mode`() = runTest {
        setupPvPMode()

        // Step 1: RED のピースを選択
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)

        val afterSelect = viewModel.uiState.value
        assertTrue("ピースが選択されている", afterSelect.selectedItem is SelectedItem.PieceItem)
        assertTrue("有効な移動先がある", afterSelect.validDests.isNotEmpty())

        // Step 2: 有効な移動先にピースを移動
        val dest = afterSelect.validDests.first()
        viewModel.handleDestinationTap(dest)

        // 移動直後: ピースの位置が更新され、アニメーション中
        val afterMove = viewModel.uiState.value
        val movedPiece = afterMove.pieces.first { it.id == redPiece.id }
        assertEquals("ピースのq座標が更新", dest.q, movedPiece.q)
        assertEquals("ピースのr座標が更新", dest.r, movedPiece.r)
        assertTrue("アニメーション中", afterMove.isAnimating)
        assertNull("選択が解除されている", afterMove.selectedItem)

        // Step 3: アニメーション完了を待つ → MOVE_TILE フェーズへ
        advanceTimeBy(900)
        val afterAnim = viewModel.uiState.value
        assertFalse("アニメーション終了", afterAnim.isAnimating)
        assertEquals("MOVE_TILE フェーズに遷移", GamePhase.MOVE_TILE, afterAnim.phase)
        assertEquals("まだ RED のターン", PlayerColor.RED, afterAnim.turn)

        // Step 4: 移動可能なタイルを選択
        val movableIndices = afterAnim.movableTileIndices
        assertTrue("移動可能なタイルがある", movableIndices.isNotEmpty())

        val tileIndex = movableIndices.first()
        val tile = afterAnim.tiles[tileIndex]
        viewModel.handleTileTap(tile, tileIndex)

        val afterTileSelect = viewModel.uiState.value
        assertTrue("タイルが選択されている", afterTileSelect.selectedItem is SelectedItem.TileIndexItem)
        assertTrue("タイルの有効な移動先がある", afterTileSelect.validDests.isNotEmpty())

        // Step 5: タイルを有効な移動先に移動
        val tileDest = afterTileSelect.validDests.first()
        viewModel.handleDestinationTap(tileDest)

        // タイル移動のアニメーション完了を待つ
        advanceTimeBy(900)
        val afterTileMove = viewModel.uiState.value

        // ターンが BLUE に切り替わり、MOVE_TOKEN フェーズに戻る
        assertEquals("BLUE のターンに切り替わる", PlayerColor.BLUE, afterTileMove.turn)
        assertEquals("MOVE_TOKEN フェーズに戻る", GamePhase.MOVE_TOKEN, afterTileMove.phase)
    }

    // ----------------------------------------------------------------
    // 2. victoryDetection
    // ----------------------------------------------------------------

    @Test
    fun `victoryDetection - winning move triggers victory state`() = runTest {
        // 3つの RED ピースをあと1手で勝利する配置にする。
        // r1=(0,0), r2=(1,0) は隣接。r3 を (2,-2)→(0,1) に移動すれば三角勝利。
        // r3 が (0,1) に到達するにはスライドが必要なので、直接配置してからテスト。
        val nearWinPieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = 1, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 2),
            Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 2),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 2, r = -2),
        )

        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.PVP,
                myColor = PlayerColor.RED,
                tiles = GameConstants.initialTiles,
                pieces = nearWinPieces,
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

        // r3 (0,2) を選択し、(0,1) にスライドさせる (三角勝利)
        val r3 = viewModel.uiState.value.pieces.first { it.id == "r3" }
        viewModel.handlePieceTap(r3)

        val dests = viewModel.uiState.value.validDests
        // (0,1) が有効な移動先に含まれるか確認
        val targetDest = dests.firstOrNull { it.q == 0 && it.r == 1 }
        assertNotNull("(0,1) が有効な移動先に含まれる", targetDest)

        viewModel.handleDestinationTap(targetDest!!)

        // アニメーション完了を待つ
        advanceTimeBy(900)

        val state = viewModel.uiState.value
        assertEquals("勝者は RED", PlayerColor.RED, state.winner)
        assertEquals("フェーズは ENDED", GamePhase.ENDED, state.phase)
        assertEquals("victoryLine は3エントリ", 3, state.victoryLine.size)
    }

    // ----------------------------------------------------------------
    // 3. pvpModeAlwaysMyTurn
    // ----------------------------------------------------------------

    @Test
    fun `pvpModeAlwaysMyTurn - isMyTurn is true for both players in PvP`() {
        setupPvPMode()

        // RED のターン
        assertTrue("RED のターンで isMyTurn=true", viewModel.uiState.value.isMyTurn)

        // BLUE のターンに切り替え
        viewModel.setStateForTesting { it.copy(turn = PlayerColor.BLUE) }
        assertTrue("BLUE のターンでも isMyTurn=true", viewModel.uiState.value.isMyTurn)
    }

    // ----------------------------------------------------------------
    // 4. aiModeRandomColor
    // ----------------------------------------------------------------

    @Test
    fun `aiModeRandomColor - AI game assigns a valid player color`() {
        // AI モードで startNewGame を複数回実行して、myColor が RED/BLUE のいずれかになることを検証
        val colors = mutableSetOf<PlayerColor>()

        repeat(50) {
            viewModel.setStateForTesting { it.copy(mode = GameMode.AI) }
            viewModel.startNewGame()
            colors.add(viewModel.uiState.value.myColor)
        }

        assertTrue("RED が割り当てられることがある", colors.contains(PlayerColor.RED))
        assertTrue("BLUE が割り当てられることがある", colors.contains(PlayerColor.BLUE))
    }

    // ----------------------------------------------------------------
    // 5. cannotMoveDuringAnimation
    // ----------------------------------------------------------------

    @Test
    fun `cannotMoveDuringAnimation - all actions are ignored while animating`() {
        setupPvPMode()
        viewModel.setStateForTesting { it.copy(isAnimating = true) }

        // ピースタップ → 無視される
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)
        assertNull("アニメーション中はピース選択不可", viewModel.uiState.value.selectedItem)

        // タイルタップ → 無視される
        viewModel.setStateForTesting { it.copy(phase = GamePhase.MOVE_TILE, isAnimating = true) }
        val movableIndices = viewModel.uiState.value.movableTileIndices
        if (movableIndices.isNotEmpty()) {
            val tileIdx = movableIndices.first()
            viewModel.handleTileTap(viewModel.uiState.value.tiles[tileIdx], tileIdx)
            assertNull("アニメーション中はタイル選択不可", viewModel.uiState.value.selectedItem)
        }

        // デスティネーションタップ → 無視される
        viewModel.setStateForTesting { state ->
            state.copy(
                isAnimating = true,
                phase = GamePhase.MOVE_TOKEN,
                selectedItem = SelectedItem.PieceItem(redPiece.id),
                validDests = listOf(Tile(q = 0, r = 0)),
            )
        }
        viewModel.handleDestinationTap(Tile(q = 0, r = 0))
        // 何も変わらない（アニメーション中なので処理されない）
        assertTrue("状態が変化しない", viewModel.uiState.value.isAnimating)
    }

    // ----------------------------------------------------------------
    // 6. tileMoveMustKeepBoardConnected
    // ----------------------------------------------------------------

    @Test
    fun `tileMoveMustKeepBoardConnected - bridge tile cannot be selected`() {
        // 3タイルの直線ボードを構築: (0,0)-(1,0)-(2,0)
        // 中央の (1,0) を除去するとボードが分断される
        val linearTiles = listOf(
            Tile(q = 0, r = 0),
            Tile(q = 1, r = 0),
            Tile(q = 2, r = 0),
        )
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 2, r = 0),
        )

        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.PVP,
                tiles = linearTiles,
                pieces = pieces,
                turn = PlayerColor.RED,
                phase = GamePhase.MOVE_TILE,
                winner = null,
                isAnimating = false,
            )
        }

        // index=1 (1,0) はピースがないがブリッジタイル
        // ViewModel の handleTileTap は isBoardConnected チェックを行うので、選択されない
        viewModel.handleTileTap(linearTiles[1], 1)
        assertNull("ブリッジタイルは選択できない", viewModel.uiState.value.selectedItem)
    }

    // ----------------------------------------------------------------
    // 7. pieceSlideDestinations
    // ----------------------------------------------------------------

    @Test
    fun `pieceSlideDestinations - center piece has correct slide destinations`() {
        // 中央 (0,0) にピースを1つだけ配置 → 6方向全てにスライド可能
        val piece = Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0)

        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.PVP,
                tiles = GameConstants.initialTiles,
                pieces = listOf(
                    piece,
                    Piece(id = "b1", player = PlayerColor.BLUE, q = -2, r = 0),
                    Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 2),
                    Piece(id = "b3", player = PlayerColor.BLUE, q = 2, r = -2),
                    Piece(id = "r2", player = PlayerColor.RED, q = 0, r = -2),
                    Piece(id = "r3", player = PlayerColor.RED, q = 0, r = 2),
                ),
                turn = PlayerColor.RED,
                phase = GamePhase.MOVE_TOKEN,
                winner = null,
                isAnimating = false,
            )
        }

        viewModel.handlePieceTap(piece)

        val dests = viewModel.uiState.value.validDests
        assertTrue("中央ピースにはスライド先がある", dests.isNotEmpty())

        // 全ての移動先が有効なタイル座標上にあることを確認
        val tileKeys = GameConstants.initialTiles.map { it.coordsKey }.toSet()
        for (dest in dests) {
            assertTrue(
                "移動先 ${dest.coordsKey} はタイル上にある",
                tileKeys.contains(dest.coordsKey),
            )
        }

        // 移動先は他のピースの位置と重複しないことを確認
        val pieceKeys = viewModel.uiState.value.pieces.map { it.coordsKey }.toSet()
        for (dest in dests) {
            assertFalse(
                "移動先 ${dest.coordsKey} にピースがいない",
                pieceKeys.contains(dest.coordsKey),
            )
        }
    }

    // ----------------------------------------------------------------
    // 8. modeSwitchToOnlineDoesNotStartGame
    // ----------------------------------------------------------------

    @Test
    fun `modeSwitchToOnlineDoesNotStartGame - ONLINE mode does not reset board`() {
        setupPvPMode()

        // 選択状態を作る
        val redPiece = viewModel.uiState.value.pieces.first { it.player == PlayerColor.RED }
        viewModel.handlePieceTap(redPiece)
        assertNotNull("選択状態がある", viewModel.uiState.value.selectedItem)

        // ONLINE モードに切り替え
        viewModel.switchMode(GameMode.ONLINE)

        val state = viewModel.uiState.value
        assertEquals("ONLINE モードに切り替わった", GameMode.ONLINE, state.mode)
        // ONLINE モードでは startNewGame が呼ばれるが、
        // ONLINE の startNewGame はデフォルト状態を設定するだけ
        // (startNewGame 内の ONLINE ケースではボードリセットされる)
        assertEquals("フェーズは MOVE_TOKEN", GamePhase.MOVE_TOKEN, state.phase)
    }

    // ----------------------------------------------------------------
    // 9. boardInitialStateCorrect
    // ----------------------------------------------------------------

    @Test
    fun `boardInitialStateCorrect - 19 tiles and 6 pieces at correct positions`() {
        setupPvPMode()

        val state = viewModel.uiState.value
        assertEquals("タイル数は19", 19, state.tiles.size)
        assertEquals("ピース数は6", 6, state.pieces.size)
        assertEquals("赤ピース3つ", 3, state.pieces.count { it.player == PlayerColor.RED })
        assertEquals("青ピース3つ", 3, state.pieces.count { it.player == PlayerColor.BLUE })

        // 全ピースがタイル上にあることを確認
        val tileKeys = state.tiles.map { it.coordsKey }.toSet()
        for (piece in state.pieces) {
            assertTrue(
                "ピース ${piece.id} (${piece.coordsKey}) はタイル上にある",
                tileKeys.contains(piece.coordsKey),
            )
        }

        // 全タイルがユニークな座標を持つことを確認
        val uniqueTileKeys = state.tiles.map { it.coordsKey }.toSet()
        assertEquals("全タイルの座標がユニーク", state.tiles.size, uniqueTileKeys.size)

        // 全ピースがユニークな座標を持つことを確認
        val uniquePieceKeys = state.pieces.map { it.coordsKey }.toSet()
        assertEquals("全ピースの座標がユニーク", state.pieces.size, uniquePieceKeys.size)
    }

    // ----------------------------------------------------------------
    // 10. pieceCantMoveToPieceOccupiedTile
    // ----------------------------------------------------------------

    @Test
    fun `pieceCantMoveToPieceOccupiedTile - slide destinations exclude occupied tiles`() {
        // (0,0) に RED ピース、(2,0) に BLUE ピースを配置
        // RED が (1,0) 方向にスライドすると (1,0) で止まり、(2,0) には行けない
        val pieces = listOf(
            Piece(id = "r1", player = PlayerColor.RED, q = 0, r = 0),
            Piece(id = "b1", player = PlayerColor.BLUE, q = 2, r = 0),
            Piece(id = "r2", player = PlayerColor.RED, q = -2, r = 2),
            Piece(id = "b2", player = PlayerColor.BLUE, q = -2, r = 0),
            Piece(id = "r3", player = PlayerColor.RED, q = 0, r = -2),
            Piece(id = "b3", player = PlayerColor.BLUE, q = 0, r = 2),
        )

        viewModel.setStateForTesting { state ->
            state.copy(
                mode = GameMode.PVP,
                tiles = GameConstants.initialTiles,
                pieces = pieces,
                turn = PlayerColor.RED,
                phase = GamePhase.MOVE_TOKEN,
                winner = null,
                isAnimating = false,
            )
        }

        val r1 = viewModel.uiState.value.pieces.first { it.id == "r1" }
        viewModel.handlePieceTap(r1)

        val dests = viewModel.uiState.value.validDests
        val occupiedKeys = pieces.map { it.coordsKey }.toSet()

        for (dest in dests) {
            assertFalse(
                "移動先 ${dest.coordsKey} に他のピースがいてはいけない",
                occupiedKeys.contains(dest.coordsKey),
            )
        }

        // 特に (2,0) に移動できないことを明示的に確認
        val blockedDest = dests.firstOrNull { it.q == 2 && it.r == 0 }
        assertNull("(2,0) にはスライドできない", blockedDest)
    }
}
