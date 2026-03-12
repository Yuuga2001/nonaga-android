package jp.riverapp.hexlide.presentation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.data.model.GameMode
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors

@Composable
fun ShuffleAnimation(
    firstPlayer: PlayerColor,
    mode: GameMode,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shuffle")

    val bounce1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce1",
    )

    val bounce2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce2",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .shadow(20.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.98f))
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.decidingFirst,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HexlideColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mode == GameMode.AI) {
                    AvatarBubble(
                        icon = "\uD83D\uDC64", // person emoji
                        label = strings.you,
                        gradientColors = listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)),
                        offsetY = bounce1,
                    )
                    AvatarBubble(
                        icon = "\uD83E\uDD16", // robot emoji
                        label = strings.ai,
                        gradientColors = listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
                        offsetY = bounce2,
                    )
                } else {
                    AvatarBubble(
                        icon = "\uD83D\uDC64",
                        label = strings.playerRed,
                        gradientColors = listOf(
                            HexlideColors.PieceRed,
                            HexlideColors.PieceRed.copy(alpha = 0.8f),
                        ),
                        offsetY = bounce1,
                    )
                    AvatarBubble(
                        icon = "\uD83D\uDC64",
                        label = strings.playerBlue,
                        gradientColors = listOf(
                            HexlideColors.PieceBlue,
                            HexlideColors.PieceBlue.copy(alpha = 0.8f),
                        ),
                        offsetY = bounce2,
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarBubble(
    icon: String,
    label: String,
    gradientColors: List<Color>,
    offsetY: Float,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offsetY * density }
                .size(48.dp)
                .shadow(8.dp, CircleShape, ambientColor = gradientColors.first().copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                fontSize = 22.sp,
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = HexlideColors.TextSecondary,
        )
    }
}
