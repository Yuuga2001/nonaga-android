package jp.riverapp.hexlide.presentation.screen.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.presentation.viewmodel.OnlineLobbyViewModel
import jp.riverapp.hexlide.util.Constants
import kotlinx.coroutines.delay

@Composable
fun OnlineLobbyScreen(
    viewModel: OnlineLobbyViewModel = hiltViewModel(),
    strings: LocalizedStrings,
    onNavigateToGame: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showJoinForm by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Navigate to game when joined and game is not in waiting state
    LaunchedEffect(state.joinedGameId, state.gameSession?.status) {
        val joinedId = state.joinedGameId ?: return@LaunchedEffect
        if (state.gameSession?.status != GameStatus.WAITING) {
            onNavigateToGame(joinedId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HexlideColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = HexlideColors.PieceBlue,
                    )
                }
                Text(
                    text = strings.back,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HexlideColors.PieceBlue,
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main content card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = strings.onlineTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HexlideColors.TextPrimary,
                )

                val waitingGame = state.gameSession
                if (waitingGame != null && state.joinedGameId != null && waitingGame.status == GameStatus.WAITING) {
                    // Waiting for opponent state
                    WaitingView(
                        game = waitingGame,
                        strings = strings,
                        copied = copied,
                        onCopy = { url ->
                            clipboardManager.setText(AnnotatedString(url))
                            copied = true
                        },
                        onCopiedReset = { copied = false },
                        onCancel = {
                            viewModel.reset()
                            copied = false
                            showJoinForm = false
                        },
                        onGameStarted = { gameId -> onNavigateToGame(gameId) },
                        viewModel = viewModel,
                    )
                } else if (showJoinForm) {
                    JoinFormView(
                        roomCode = state.gameIdInput,
                        isJoining = state.isJoining,
                        strings = strings,
                        onRoomCodeChange = { viewModel.updateGameIdInput(it) },
                        onJoin = { viewModel.joinWithRoomCode() },
                        onBack = {
                            showJoinForm = false
                            viewModel.clearError()
                        },
                    )
                } else {
                    LobbyButtons(
                        isCreating = state.isCreating,
                        strings = strings,
                        onCreate = { viewModel.createGame() },
                        onJoinForm = { showJoinForm = true },
                    )
                }

                // Error display
                val error = state.error
                if (error != null) {
                    Text(
                        text = errorText(error, strings),
                        fontSize = 12.sp,
                        color = Color.Red,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LobbyButtons(
    isCreating: Boolean,
    strings: LocalizedStrings,
    onCreate: () -> Unit,
    onJoinForm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Create Game button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(HexlideColors.PieceBlue, HexlideColors.GradientPurple),
                    ),
                )
                .clickable(enabled = !isCreating) { onCreate() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isCreating) strings.creating else strings.createRoom,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        // Join Room button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, HexlideColors.TileStroke, RoundedCornerShape(12.dp))
                .clickable { onJoinForm() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = strings.joinRoom,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HexlideColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun WaitingView(
    game: jp.riverapp.hexlide.data.model.GameSession,
    strings: LocalizedStrings,
    copied: Boolean,
    onCopy: (String) -> Unit,
    onCopiedReset: () -> Unit,
    onCancel: () -> Unit,
    onGameStarted: (String) -> Unit,
    viewModel: OnlineLobbyViewModel,
) {
    val gameUrl = Constants.Api.BASE_URL + "/game/" + game.gameId

    // Poll for game start
    LaunchedEffect(game.gameId) {
        var consecutiveFailures = 0
        while (true) {
            delay(1000)
            try {
                val updated = viewModel.let {
                    // We can't directly call repository here, so we use a polling approach
                    // by checking the state changes propagated by the ViewModel
                    null
                }
                // The actual polling is best handled via the onNavigateToGame callback
                // which is triggered by LaunchedEffect on joinedGameId/status changes
                break
            } catch (_: Exception) {
                consecutiveFailures++
                if (consecutiveFailures >= 5) break
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            color = HexlideColors.PieceBlue,
        )

        Text(
            text = strings.waitingForOpponent,
            fontSize = 14.sp,
            color = HexlideColors.TextSecondary,
        )

        Text(
            text = strings.shareDescription,
            fontSize = 12.sp,
            color = HexlideColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )

        // Room code display
        val roomCode = game.roomCode
        if (roomCode != null) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(HexlideColors.Background)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = strings.roomCodeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HexlideColors.TextTertiary,
                )
                Text(
                    text = roomCode,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HexlideColors.TextPrimary,
                    letterSpacing = 4.sp,
                )
            }
        }

        // URL + Copy button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = gameUrl,
                fontSize = 10.sp,
                color = HexlideColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HexlideColors.Background)
                    .border(1.dp, HexlideColors.TileStroke, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )

            // Reset copied after delay
            LaunchedEffect(copied) {
                if (copied) {
                    delay(2000)
                    onCopiedReset()
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HexlideColors.PieceBlue)
                    .clickable { onCopy(gameUrl) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = if (copied) strings.copied else strings.copyUrl,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // Cancel button
        Box(
            modifier = Modifier
                .border(1.dp, HexlideColors.TileStroke, RoundedCornerShape(50))
                .clickable { onCancel() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = strings.cancel,
                fontSize = 14.sp,
                color = HexlideColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun JoinFormView(
    roomCode: String,
    isJoining: Boolean,
    strings: LocalizedStrings,
    onRoomCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit,
) {
    val normalizedDigitCount = roomCode.filter { it.isDigit() }.length
    val canJoin = !isJoining && normalizedDigitCount == 6

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.enterRoomCode,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = HexlideColors.TextPrimary,
        )

        OutlinedTextField(
            value = roomCode,
            onValueChange = onRoomCodeChange,
            placeholder = {
                Text(
                    text = strings.roomCodePlaceholder,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
        )

        // Join button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (canJoin) {
                        Brush.linearGradient(
                            colors = listOf(HexlideColors.ModeOnline, HexlideColors.GradientGreen),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                HexlideColors.ModeOnline.copy(alpha = 0.4f),
                                HexlideColors.GradientGreen.copy(alpha = 0.4f),
                            ),
                        )
                    },
                )
                .clickable(enabled = canJoin) { onJoin() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isJoining) strings.joining else strings.joinGame,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        // Back button
        Text(
            text = strings.back,
            fontSize = 14.sp,
            color = HexlideColors.TextSecondary,
            modifier = Modifier.clickable { onBack() },
        )
    }
}

private fun errorText(key: String, strings: LocalizedStrings): String {
    return when (key) {
        "invalid_room_code" -> strings.invalidRoomCode
        "game_not_found" -> strings.gameNotFound
        "game_already_started" -> strings.gameAlreadyStarted
        "connection_error" -> strings.connectionError
        else -> key
    }
}
