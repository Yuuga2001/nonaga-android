package jp.riverapp.hexlide.viewmodel

import io.mockk.coEvery
import io.mockk.mockk
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.repository.GameRepository
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.domain.service.GamePollingService
import jp.riverapp.hexlide.presentation.viewmodel.OnlineLobbyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineLobbyViewModelTests {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: GameRepository
    private lateinit var pollingService: GamePollingService
    private lateinit var viewModel: OnlineLobbyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        pollingService = mockk(relaxed = true)
        viewModel = OnlineLobbyViewModel(repository, pollingService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ----------------------------------------------------------------
    // 1. initialState
    // ----------------------------------------------------------------

    @Test
    fun `initialState - defaults are correct`() {
        val state = viewModel.uiState.value
        assertNull(state.gameSession)
        assertFalse(state.isCreating)
        assertFalse(state.isJoining)
        assertNull(state.error)
        assertEquals("", state.gameIdInput)
        assertNull(state.joinedGameId)
    }

    // ----------------------------------------------------------------
    // 2. updateGameIdInput
    // ----------------------------------------------------------------

    @Test
    fun `updateGameIdInput - updates input string`() {
        viewModel.updateGameIdInput("123456")
        assertEquals("123456", viewModel.uiState.value.gameIdInput)
    }

    // ----------------------------------------------------------------
    // 3. clearError
    // ----------------------------------------------------------------

    @Test
    fun `clearError - resets error to null`() {
        viewModel.setStateForTesting { it.copy(error = "some error") }
        assertEquals("some error", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ----------------------------------------------------------------
    // 4. createGameSetsLoading
    // ----------------------------------------------------------------

    @Test
    fun `createGameSetsLoading - isCreating becomes true`() = runTest {
        val testSession = createTestSession()
        coEvery { repository.createGame() } coAnswers {
            testDispatcher.scheduler.advanceTimeBy(1000)
            testSession
        }

        viewModel.createGame()

        // After calling createGame, isCreating should be true immediately
        assertTrue(viewModel.uiState.value.isCreating)
    }

    // ----------------------------------------------------------------
    // 5. joinWithRoomCodeRejectsShortCode
    // ----------------------------------------------------------------

    @Test
    fun `joinWithRoomCode - rejects short code`() {
        viewModel.updateGameIdInput("123")
        viewModel.joinWithRoomCode()

        val state = viewModel.uiState.value
        assertEquals("invalid_room_code", state.error)
        assertFalse(state.isJoining)
    }

    // ----------------------------------------------------------------
    // 6. joinWithRoomCodeRejectsEmptyCode
    // ----------------------------------------------------------------

    @Test
    fun `joinWithRoomCode - rejects empty code`() {
        viewModel.updateGameIdInput("")
        viewModel.joinWithRoomCode()

        val state = viewModel.uiState.value
        assertEquals("invalid_room_code", state.error)
    }

    // ----------------------------------------------------------------
    // 7. joinWithRoomCodeFiltersNonNumeric
    // ----------------------------------------------------------------

    @Test
    fun `joinWithRoomCode - filters non-numeric characters`() {
        viewModel.updateGameIdInput("abc12")
        viewModel.joinWithRoomCode()

        val state = viewModel.uiState.value
        assertEquals("invalid_room_code", state.error)
    }

    // ----------------------------------------------------------------
    // 8. resetClearsState
    // ----------------------------------------------------------------

    @Test
    fun `reset - clears all state`() {
        viewModel.updateGameIdInput("123456")
        viewModel.setStateForTesting { it.copy(error = "some_error") }

        viewModel.reset()

        val state = viewModel.uiState.value
        assertEquals("", state.gameIdInput)
        assertFalse(state.isCreating)
        assertFalse(state.isJoining)
        assertNull(state.error)
        assertNull(state.gameSession)
        assertNull(state.joinedGameId)
    }

    // ----------------------------------------------------------------
    // 9. createGameSuccess
    // ----------------------------------------------------------------

    @Test
    fun `createGame - success sets gameSession and joinedGameId`() = runTest {
        val testSession = createTestSession()
        coEvery { repository.createGame() } returns testSession

        viewModel.createGame()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreating)
        assertEquals(testSession, state.gameSession)
        assertEquals("test-game-id", state.joinedGameId)
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private fun createTestSession(): GameSession {
        return GameSession(
            gameId = "test-game-id",
            roomCode = "123456",
            status = GameStatus.WAITING,
            hostPlayerId = "player-1",
            guestPlayerId = null,
            hostColor = PlayerColor.RED,
            tiles = GameConstants.initialTiles,
            pieces = GameConstants.initialPieces,
            turn = PlayerColor.RED,
            phase = GamePhase.MOVE_TOKEN,
            winner = null,
            victoryLine = null,
            lastMoveAt = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            ttl = null,
        )
    }
}
