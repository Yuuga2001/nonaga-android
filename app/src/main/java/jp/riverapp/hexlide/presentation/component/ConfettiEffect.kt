package jp.riverapp.hexlide.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.util.Constants
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val startY: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val rotation: Float,
    val rotationEnd: Float,
    val delay: Long,
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    val containerSize = remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    val confettiColors = remember {
        listOf(
            HexlideColors.PieceRed,
            HexlideColors.PieceBlue,
            HexlideColors.SelectedGlow,
            HexlideColors.ValidDest,
            Color(0xFFFF9800), // orange
            Color(0xFFE91E63), // pink
            Color(0xFF9C27B0), // purple
            Color(0xFF00BCD4), // cyan
        )
    }

    val particles = remember(containerSize.value) {
        val size = containerSize.value
        if (size.width == 0 || size.height == 0) return@remember emptyList()
        (0 until 40).map { i ->
            val particleWidth = with(density) { Random.nextFloat() * 6f + 6f } // 6-12dp equivalent
            ConfettiParticle(
                x = Random.nextFloat() * size.width,
                startY = -20f,
                width = particleWidth * density.density,
                height = particleWidth * density.density * 0.6f,
                color = confettiColors[i % confettiColors.size],
                rotation = Random.nextFloat() * 360f,
                rotationEnd = Random.nextFloat() * 540f + 180f,
                delay = (Random.nextFloat() * 1000).toLong(),
            )
        }
    }

    // Each particle has its own animatable for staggered animation
    val particleProgresses = remember(particles.size) {
        List(particles.size) { Animatable(0f) }
    }

    LaunchedEffect(particles) {
        if (particles.isEmpty()) return@LaunchedEffect
        particles.forEachIndexed { index, particle ->
            launch {
                kotlinx.coroutines.delay(particle.delay)
                particleProgresses[index].animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = Constants.Timing.CONFETTI_DURATION_MS.toInt(),
                        easing = LinearEasing,
                    )
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize.value = it }
    ) {
        val canvasHeight = size.height
        particles.forEachIndexed { index, particle ->
            if (index >= particleProgresses.size) return@forEachIndexed
            val p = particleProgresses[index].value
            if (p <= 0f) return@forEachIndexed

            val currentY = particle.startY + (canvasHeight + 70f) * p
            val currentRotation = particle.rotation + (particle.rotationEnd - particle.rotation) * p
            val alpha = (1f - p).coerceIn(0f, 1f)

            rotate(
                degrees = currentRotation,
                pivot = Offset(particle.x, currentY),
            ) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(
                        particle.x - particle.width / 2f,
                        currentY - particle.height / 2f,
                    ),
                    size = Size(particle.width, particle.height),
                )
            }
        }
    }
}
