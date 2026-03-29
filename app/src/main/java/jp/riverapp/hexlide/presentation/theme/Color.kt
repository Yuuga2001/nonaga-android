package jp.riverapp.hexlide.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * iOS版 Constants.Colors の完全移植。
 * 全38色を同一の16進カラーコードで定義。
 */
object HexlideColors {
    // Pieces
    val PieceRed = Color(0xFFf43f5e)
    val PieceBlue = Color(0xFF6366f1)

    // Tile
    val TileFill = Color.White
    val TileStroke = Color(0xFFcbd5e1)

    // Selection
    val SelectedGlow = Color(0xFFfbbf24)
    val ValidDest = Color(0xFF34d399)
    val ValidDestFill = Color(0xFFf0fdf4)
    val SelectedOriginFill = Color(0xFFfee2e2)

    // Background
    val Background = Color(0xFFeef2f7)

    // Text
    val TextPrimary = Color(0xFF1e293b)
    val TextSecondary = Color(0xFF64748b)
    val TextTertiary = Color(0xFF94a3b8)

    // Victory
    val VictoryRedBg = Color(0xFFfff1f2)
    val VictoryBlueBg = Color(0xFFeef2ff)
    val VictoryRedStroke = Color(0xFFfda4af)
    val VictoryBlueStroke = Color(0xFFa5b4fc)

    // Goal
    val GoalBg = Color(0xFFfffbeb)
    val GoalBorder = Color(0xFFfde68a)
    val GoalText = Color(0xFF78350f)

    // Mode badges
    val ModeAI = Color(0xFFf59e0b)
    val ModeOnline = Color(0xFF10b981)
    val ModeOnlineBg = Color(0xFFd1fae5)
    val ModeOnlineSelectedBg = Color(0xFFecfdf5)
    val ModeOfflineBg = Color(0xFFf1f5f9)

    // Gradients
    val GradientPurple = Color(0xFF8b5cf6)
    val GradientGreen = Color(0xFF22c55e)

    // Button
    val ButtonText = Color(0xFF475569)

    // Status bar indicator
    val StatusRedBg = Color(0xFFffe4e6)
    val StatusBlueBg = Color(0xFFe0e7ff)
}
