package jp.riverapp.hexlide.presentation.screen.online

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.GameStatus
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.domain.logic.GameConstants
import jp.riverapp.hexlide.presentation.component.GameStatusBar
import jp.riverapp.hexlide.presentation.component.HexBoard
import jp.riverapp.hexlide.presentation.component.VictoryOverlay
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.presentation.viewmodel.OnlineGameViewModel

@Composable
fun OnlineGameScreen(
    gameId: String,
    viewModel: OnlineGameViewModel = hiltViewModel(),
    strings: LocalizedStrings,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        viewModel.startGame(gameId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    // Handle navigation back
    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            onBack()
        }
    }

    // End game confirmation dialog
    if (state.showEndConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowEndConfirm(false) },
            title = { Text(strings.endGame) },
            text = { Text(strings.confirmEndGame) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setShowEndConfirm(false)
                        viewModel.endGame()
                    },
                ) {
                    Text(strings.confirm, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowEndConfirm(false) }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    val displayTiles = state.gameSession?.tiles ?: GameConstants.initialTiles
    val displayPieces = state.gameSession?.pieces ?: GameConstants.initialPieces

    val backgroundColor = when {
        state.winner == PlayerColor.RED && state.gameSession?.status != GameStatus.ABANDONED -> HexlideColors.VictoryRedBg
        state.winner == PlayerColor.BLUE && state.gameSession?.status != GameStatus.ABANDONED -> HexlideColors.VictoryBlueBg
        else -> HexlideColors.Background
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "HEXLIDE",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = HexlideColors.TextPrimary,
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.setShowEndConfirm(true) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = strings.endGame,
                        tint = HexlideColors.TextSecondary,
                    )
                }
            }

            // Status / Victory
            val session = state.gameSession
            if (session != null && session.winner != null && session.status != GameStatus.ABANDONED) {
                VictoryOverlay(
                    winner = session.winner!!,
                    mode = GameMode.ONLINE,
                    myColor = state.myColor,
                    strings = strings,
                    onPlayAgain = { viewModel.requestRematch() },
                )
            } else {
                GameStatusBar(
                    turn = state.gameSession?.turn ?: PlayerColor.RED,
                    phase = state.gameSession?.phase ?: GamePhase.MOVE_TOKEN,
                    mode = GameMode.ONLINE,
                    myColor = state.myColor,
                    isAIThinking = false,
                    strings = strings,
                )
            }

            // Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                HexBoard(
                    tiles = displayTiles,
                    pieces = displayPieces,
                    selectedItem = state.selectedItem,
                    validDests = state.validDests,
                    winner = state.winner,
                    victoryLine = state.victoryLine,
                    phase = state.gameSession?.phase ?: GamePhase.MOVE_TOKEN,
                    turn = state.gameSession?.turn ?: PlayerColor.RED,
                    isInteractive = state.isMyTurn && !state.isAnimating,
                    movableTileIndices = state.movableTileIndices,
                    myColor = state.myColor,
                    onPieceTap = viewModel::handlePieceTap,
                    onTileTap = viewModel::handleTileTap,
                    onDestinationTap = viewModel::handleDestinationTap,
                    modifier = Modifier.fillMaxSize(),
                )

                // Overlays on top of board
                when {
                    state.isLoading -> {
                        LoadingOverlay()
                    }
                    session?.status == GameStatus.WAITING -> {
                        WaitingOverlay(game = session, strings = strings)
                    }
                    state.error != null && session == null -> {
                        ErrorOverlay(
                            message = state.error ?: "",
                            strings = strings,
                            onBack = onBack,
                        )
                    }
                    session?.status == GameStatus.ABANDONED -> {
                        AbandonedOverlay(
                            strings = strings,
                            onBack = onBack,
                        )
                    }
                    state.isReconnecting -> {
                        ReconnectingOverlay(
                            strings = strings,
                            onRetry = { viewModel.retryPolling() },
                        )
                    }
                }
            }

            // Mode badge + End game button
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.onlineTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = HexlideColors.ModeOnline.copy(alpha = 0.3f),
                        )
                        .clip(RoundedCornerShape(50))
                        .background(HexlideColors.ModeOnline)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )

                Text(
                    text = strings.endGame,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HexlideColors.ButtonText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HexlideColors.TileStroke)
                        .clickable { viewModel.setShowEndConfirm(true) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HexlideColors.PieceBlue)
    }
}

@Composable
private fun WaitingOverlay(
    game: jp.riverapp.hexlide.data.model.GameSession,
    strings: LocalizedStrings,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = HexlideColors.PieceBlue)
            Text(
                text = strings.waitingForOpponent,
                fontSize = 14.sp,
                color = HexlideColors.TextSecondary,
            )
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
                        letterSpacing = 4.sp,
                        color = HexlideColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    strings: LocalizedStrings,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.Red,
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HexlideColors.PieceBlue,
                ),
            ) {
                Text(strings.back)
            }
        }
    }
}

@Composable
private fun AbandonedOverlay(
    strings: LocalizedStrings,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = null,
                tint = HexlideColors.TextSecondary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = strings.abandoned,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HexlideColors.TextPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HexlideColors.PieceBlue)
                    .clickable { onBack() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = strings.backToLobby,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ReconnectingOverlay(
    strings: LocalizedStrings,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = null,
                tint = HexlideColors.TextSecondary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = strings.reconnecting,
                fontSize = 12.sp,
                color = HexlideColors.TextSecondary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HexlideColors.PieceBlue)
                    .clickable { onRetry() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = strings.restart,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
