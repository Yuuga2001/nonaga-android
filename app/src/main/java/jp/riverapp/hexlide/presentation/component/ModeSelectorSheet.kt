package jp.riverapp.hexlide.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors

@Composable
fun ModeSelectorSheet(
    currentMode: GameMode,
    strings: LocalizedStrings,
    onModeSelected: (GameMode) -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRestartConfirm by remember { mutableStateOf(false) }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text(strings.confirmRestart) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    onRestart()
                }) {
                    Text(strings.restart, color = HexlideColors.PieceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Scrim - tap to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.001f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ),
        )

        // Bottom sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .shadow(30.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = strings.selectMode,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HexlideColors.TextPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Close",
                        tint = HexlideColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI mode option
            ModeOption(
                icon = "\uD83E\uDD16",
                label = strings.aiMode,
                isOnline = false,
                isSelected = currentMode == GameMode.AI,
                onClick = {
                    if (currentMode == GameMode.AI) {
                        showRestartConfirm = true
                    } else {
                        onModeSelected(GameMode.AI)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PVP mode option
            ModeOption(
                icon = "\uD83D\uDC65",
                label = strings.pvpMode,
                isOnline = false,
                isSelected = currentMode == GameMode.PVP,
                onClick = {
                    if (currentMode == GameMode.PVP) {
                        showRestartConfirm = true
                    } else {
                        onModeSelected(GameMode.PVP)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Online mode option
            ModeOption(
                icon = "\uD83C\uDF10",
                label = strings.onlineMode,
                isOnline = true,
                isSelected = currentMode == GameMode.ONLINE,
                onClick = {
                    if (currentMode == GameMode.ONLINE) {
                        showRestartConfirm = true
                    } else {
                        onModeSelected(GameMode.ONLINE)
                    }
                },
            )
        }
    }
}

@Composable
private fun ModeOption(
    icon: String,
    label: String,
    isOnline: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isSelected && isOnline -> HexlideColors.ModeOnline
        isSelected -> HexlideColors.PieceBlue
        else -> HexlideColors.TileStroke
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = when {
        isSelected && isOnline -> HexlideColors.ModeOnlineSelectedBg
        isSelected -> HexlideColors.VictoryBlueBg
        else -> Color.White
    }
    val iconBgColor = if (isOnline) HexlideColors.ModeOnlineBg else HexlideColors.ModeOfflineBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                fontSize = 22.sp,
            )
        }

        // Label
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = HexlideColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        // Checkmark
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = if (isOnline) HexlideColors.ModeOnline else HexlideColors.PieceBlue,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
