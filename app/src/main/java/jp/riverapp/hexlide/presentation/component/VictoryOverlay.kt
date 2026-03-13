package jp.riverapp.hexlide.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors

@Composable
fun VictoryOverlay(
    winner: PlayerColor,
    mode: GameMode,
    myColor: PlayerColor?,
    strings: LocalizedStrings,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBadge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showBadge = true
    }

    val victoryText = when (mode) {
        GameMode.AI -> if (winner == myColor) strings.youWin else strings.aiWin
        GameMode.PVP -> if (winner == PlayerColor.RED) strings.redWin else strings.blueWin
        GameMode.ONLINE -> if (winner == myColor) strings.youWin else strings.opponentWin
    }

    val badgeColor = if (winner == PlayerColor.RED) HexlideColors.PieceRed else HexlideColors.PieceBlue

    AnimatedVisibility(
        visible = showBadge,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { it },
        ) + fadeIn(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Victory badge
            Text(
                text = victoryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .shadow(20.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(badgeColor)
                    .border(4.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )

            // Play again button
            Text(
                text = strings.playAgain,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HexlideColors.TextPrimary)
                    .clickable(onClick = onPlayAgain)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
