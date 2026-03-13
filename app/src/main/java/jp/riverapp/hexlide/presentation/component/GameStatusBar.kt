package jp.riverapp.hexlide.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors

@Composable
fun GameStatusBar(
    turn: PlayerColor,
    phase: GamePhase,
    mode: GameMode,
    myColor: PlayerColor?,
    isAIThinking: Boolean,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Player indicators row in capsule
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                )
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIndicator(
                color = PlayerColor.RED,
                isActive = turn == PlayerColor.RED,
                label = playerLabel(PlayerColor.RED, mode, myColor, strings),
            )
            PlayerIndicator(
                color = PlayerColor.BLUE,
                isActive = turn == PlayerColor.BLUE,
                label = playerLabel(PlayerColor.BLUE, mode, myColor, strings),
            )
        }

        // Phase text
        if (isAIThinking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = HexlideColors.TextTertiary,
                )
                Text(
                    text = strings.aiThinking,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HexlideColors.TextTertiary,
                )
            }
        } else if (phase == GamePhase.MOVE_TOKEN || phase == GamePhase.MOVE_TILE) {
            Text(
                text = if (phase == GamePhase.MOVE_TOKEN) strings.phaseMoveToken else strings.phaseMoveTile,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = HexlideColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun PlayerIndicator(
    color: PlayerColor,
    isActive: Boolean,
    label: String,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(300),
        label = "playerAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(300),
        label = "playerScale",
    )

    Row(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (color == PlayerColor.RED) HexlideColors.PieceRed
                    else HexlideColors.PieceBlue
                ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = HexlideColors.TextPrimary,
        )
    }
}

private fun playerLabel(
    color: PlayerColor,
    mode: GameMode,
    myColor: PlayerColor?,
    strings: LocalizedStrings,
): String {
    return when (mode) {
        GameMode.AI -> {
            if (color == myColor) strings.you else strings.ai
        }
        GameMode.PVP -> {
            if (color == PlayerColor.RED) strings.playerRed else strings.playerBlue
        }
        GameMode.ONLINE -> {
            if (color == myColor) strings.you else strings.opponent
        }
    }
}
