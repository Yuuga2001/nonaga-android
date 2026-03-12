package jp.riverapp.hexlide.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = HexlideColors.PieceBlue,
    secondary = HexlideColors.PieceRed,
    background = HexlideColors.Background,
    surface = HexlideColors.Background,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = HexlideColors.TextPrimary,
    onSurface = HexlideColors.TextPrimary,
)

@Composable
fun HexlideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
