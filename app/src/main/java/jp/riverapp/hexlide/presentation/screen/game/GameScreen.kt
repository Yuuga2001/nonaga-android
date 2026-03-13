package jp.riverapp.hexlide.presentation.screen.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.presentation.component.GameStatusBar
import jp.riverapp.hexlide.presentation.component.HexBoard
import jp.riverapp.hexlide.presentation.component.ModeSelectorSheet
import jp.riverapp.hexlide.presentation.component.RulesSection
import jp.riverapp.hexlide.presentation.component.ShuffleAnimation
import jp.riverapp.hexlide.presentation.component.VictoryOverlay
import jp.riverapp.hexlide.presentation.localization.LocalizationManager
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.presentation.viewmodel.LocalGameViewModel

@Composable
fun GameScreen(
    viewModel: LocalGameViewModel = hiltViewModel(),
    localizationManager: LocalizationManager,
    onNavigateToOnline: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = localizationManager.strings

    var hasAppearedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasAppearedOnce) {
            hasAppearedOnce = true
            viewModel.startNewGame()
        }
    }

    val backgroundColor = when {
        uiState.winner == PlayerColor.RED -> HexlideColors.VictoryRedBg
        uiState.winner == PlayerColor.BLUE -> HexlideColors.VictoryBlueBg
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
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = HexlideColors.TextSecondary,
                    )
                }
            }

            // Status / Victory
            if (uiState.winner != null) {
                VictoryOverlay(
                    winner = uiState.winner!!,
                    mode = uiState.mode,
                    myColor = uiState.myColor,
                    strings = strings,
                    onPlayAgain = { viewModel.startNewGame() },
                )
            } else {
                GameStatusBar(
                    turn = uiState.turn,
                    phase = uiState.phase,
                    mode = uiState.mode,
                    myColor = uiState.currentPlayerColor,
                    isAIThinking = uiState.isAIThinking,
                    strings = strings,
                )
            }

            // Board
            HexBoard(
                tiles = uiState.tiles,
                pieces = uiState.pieces,
                selectedItem = uiState.selectedItem,
                validDests = uiState.validDests,
                winner = uiState.winner,
                victoryLine = uiState.victoryLine,
                phase = uiState.phase,
                turn = uiState.turn,
                isInteractive = uiState.isMyTurn && !uiState.isAnimating,
                movableTileIndices = uiState.movableTileIndices,
                myColor = uiState.currentPlayerColor,
                onPieceTap = viewModel::handlePieceTap,
                onTileTap = viewModel::handleTileTap,
                onDestinationTap = viewModel::handleDestinationTap,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )

            // Rules
            RulesSection(strings = strings)

            Spacer(modifier = Modifier.height(8.dp))

            // Mode badge + Change mode button
            val modeDisabled = uiState.isAnimating || uiState.isAIThinking

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val modeBadgeColor = when (uiState.mode) {
                    GameMode.AI -> HexlideColors.ModeAI
                    GameMode.PVP -> HexlideColors.PieceBlue
                    GameMode.ONLINE -> HexlideColors.ModeOnline
                }
                val modeLabelText = when (uiState.mode) {
                    GameMode.AI -> strings.aiMode
                    GameMode.PVP -> strings.pvpMode
                    GameMode.ONLINE -> strings.onlineMode
                }

                Text(
                    text = modeLabelText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = modeBadgeColor.copy(alpha = 0.3f),
                        )
                        .clip(RoundedCornerShape(50))
                        .background(modeBadgeColor)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = strings.changeMode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HexlideColors.ButtonText,
                    modifier = Modifier
                        .alpha(if (modeDisabled) 0.4f else 1f)
                        .clip(RoundedCornerShape(50))
                        .background(HexlideColors.TileStroke)
                        .clickable(enabled = !modeDisabled) {
                            viewModel.setShowModeSelector(true)
                        }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }

            // Ad space placeholder (将来の広告バナー用スペース)
            Spacer(modifier = Modifier.height(60.dp))
        }

        // ----- Overlays -----

        // Mode selector overlay
        AnimatedVisibility(
            visible = uiState.showModeSelector,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ModeSelectorSheet(
                currentMode = uiState.mode,
                strings = strings,
                onModeSelected = { mode ->
                    if (mode == GameMode.ONLINE) {
                        viewModel.switchMode(GameMode.ONLINE)
                        onNavigateToOnline()
                    } else {
                        viewModel.switchMode(mode)
                    }
                },
                onRestart = {
                    viewModel.setShowModeSelector(false)
                    viewModel.startNewGame()
                },
                onDismiss = { viewModel.setShowModeSelector(false) },
            )
        }

        // Shuffle overlay
        AnimatedVisibility(
            visible = uiState.showShuffle,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                ShuffleAnimation(
                    firstPlayer = uiState.turn,
                    mode = uiState.mode,
                    strings = strings,
                )
            }
        }
    }
}
