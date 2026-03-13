package jp.riverapp.hexlide.presentation.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import jp.riverapp.hexlide.data.model.GamePhase
import jp.riverapp.hexlide.data.model.Piece
import jp.riverapp.hexlide.data.model.PlayerColor
import jp.riverapp.hexlide.data.model.SelectedItem
import jp.riverapp.hexlide.data.model.Tile
import jp.riverapp.hexlide.domain.logic.GameLogic
import jp.riverapp.hexlide.domain.logic.HexMath
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.util.Constants

private val HexSlideEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Composable
fun HexBoard(
    tiles: List<Tile>,
    pieces: List<Piece>,
    selectedItem: SelectedItem?,
    validDests: List<Tile>,
    winner: PlayerColor?,
    victoryLine: List<String>,
    phase: GamePhase,
    turn: PlayerColor,
    isInteractive: Boolean,
    movableTileIndices: Set<Int>,
    myColor: PlayerColor?,
    onPieceTap: (Piece) -> Unit,
    onTileTap: (Tile, Int) -> Unit,
    onDestinationTap: (Tile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tileSize = 36f // matching iOS tileSize
    val hexSize = Constants.Game.HEX_SIZE
    val pieceRadius = Constants.Game.PIECE_RADIUS
    val pieceTouchRadius = Constants.Game.PIECE_TOUCH_RADIUS

    // Compute bounds from tiles
    val bounds = remember(tiles) { GameLogic.calculateViewBounds(tiles) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val scaleX = canvasWidth / bounds.width
        val scaleY = canvasHeight / bounds.height
        val scale = minOf(scaleX, scaleY, 2.5f)

        val midX = (bounds.minX + bounds.maxX) / 2f
        val midY = (bounds.minY + bounds.maxY) / 2f
        val offsetX = canvasWidth / 2f - midX * scale
        val offsetY = canvasHeight / 2f - midY * scale

        // Hex points for tile drawing (scaled by tileSize)
        val hexPoints = remember { HexMath.hexagonPoints(tileSize) }
        // Smaller hex points for dest guides
        val hexPointsSmall = remember { HexMath.hexagonPoints(tileSize * 0.85f) }

        // Piece animation data - animate pixel positions
        data class PieceAnimData(val id: String, val targetX: Float, val targetY: Float)

        val pieceAnimTargets = remember(pieces, scale, offsetX, offsetY) {
            pieces.map { piece ->
                val (px, py) = HexMath.hexToPixel(piece.q, piece.r, hexSize)
                PieceAnimData(
                    id = piece.id,
                    targetX = px * scale + offsetX,
                    targetY = py * scale + offsetY,
                )
            }
        }

        // Create animated values for each piece
        val animatedPositions = pieces.mapIndexed { index, piece ->
            val target = pieceAnimTargets.getOrNull(index)
            val animX by animateFloatAsState(
                targetValue = target?.targetX ?: 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = HexSlideEasing,
                ),
                label = "pieceX_${piece.id}",
            )
            val animY by animateFloatAsState(
                targetValue = target?.targetY ?: 0f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = HexSlideEasing,
                ),
                label = "pieceY_${piece.id}",
            )
            Pair(animX, animY)
        }

        // Pre-compute valid dest set for quick lookup
        val validDestSet = remember(validDests) {
            validDests.map { it.coordsKey }.toSet()
        }

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(tiles, pieces, selectedItem, validDests, phase, winner, isInteractive) {
                    detectTapGestures { tapOffset ->
                        if (winner != null && phase == GamePhase.ENDED) return@detectTapGestures

                        val tapX = tapOffset.x
                        val tapY = tapOffset.y
                        val tapRadiusSq = (pieceTouchRadius * scale) * (pieceTouchRadius * scale)

                        // 1. Check if tapped a destination
                        if (selectedItem != null && validDests.isNotEmpty()) {
                            for (dest in validDests) {
                                val (dx, dy) = HexMath.hexToPixel(dest.q, dest.r, hexSize)
                                val screenX = dx * scale + offsetX
                                val screenY = dy * scale + offsetY
                                val distSq =
                                    (tapX - screenX) * (tapX - screenX) + (tapY - screenY) * (tapY - screenY)
                                if (distSq < tapRadiusSq) {
                                    onDestinationTap(dest)
                                    return@detectTapGestures
                                }
                            }
                        }

                        // 2. Check if tapped a piece
                        if (phase == GamePhase.MOVE_TOKEN && isInteractive) {
                            for (piece in pieces) {
                                val (px, py) = HexMath.hexToPixel(piece.q, piece.r, hexSize)
                                val screenX = px * scale + offsetX
                                val screenY = py * scale + offsetY
                                val distSq =
                                    (tapX - screenX) * (tapX - screenX) + (tapY - screenY) * (tapY - screenY)
                                if (distSq < tapRadiusSq) {
                                    onPieceTap(piece)
                                    return@detectTapGestures
                                }
                            }
                        }

                        // 3. Check if tapped a tile
                        if (phase == GamePhase.MOVE_TILE && isInteractive) {
                            var closestIndex = -1
                            var closestDistSq = Float.MAX_VALUE
                            val tileTapRadiusSq = (tileSize * scale) * (tileSize * scale)

                            for ((index, tile) in tiles.withIndex()) {
                                val (tx, ty) = HexMath.hexToPixel(tile.q, tile.r, hexSize)
                                val screenX = tx * scale + offsetX
                                val screenY = ty * scale + offsetY
                                val distSq =
                                    (tapX - screenX) * (tapX - screenX) + (tapY - screenY) * (tapY - screenY)
                                if (distSq < tileTapRadiusSq && distSq < closestDistSq) {
                                    closestDistSq = distSq
                                    closestIndex = index
                                }
                            }

                            if (closestIndex >= 0) {
                                onTileTap(tiles[closestIndex], closestIndex)
                                return@detectTapGestures
                            }
                        }
                    }
                },
        ) {
            // ----- Layer 1: Tiles -----
            for ((index, tile) in tiles.withIndex()) {
                val (px, py) = HexMath.hexToPixel(tile.q, tile.r, hexSize)
                val screenX = px * scale + offsetX
                val screenY = py * scale + offsetY
                val tileKey = tile.coordsKey
                val isVictoryTile = victoryLine.contains(tileKey)
                val isSelectedTile = phase == GamePhase.MOVE_TILE &&
                    selectedItem is SelectedItem.TileIndexItem &&
                    selectedItem.index == index
                val pieceOnTile = pieces.any { it.coordsKey == tileKey }
                val isMovableTile = winner == null &&
                    phase == GamePhase.MOVE_TILE &&
                    !pieceOnTile &&
                    isInteractive &&
                    movableTileIndices.contains(index)
                val isFaded = winner != null && !isVictoryTile

                // Determine fill color
                val fillColor = when {
                    isVictoryTile && winner == PlayerColor.RED -> HexlideColors.VictoryRedBg
                    isVictoryTile && winner == PlayerColor.BLUE -> HexlideColors.VictoryBlueBg
                    isSelectedTile -> HexlideColors.SelectedOriginFill
                    else -> HexlideColors.TileFill
                }

                // Determine stroke color
                val strokeColor = when {
                    isVictoryTile && winner == PlayerColor.RED -> HexlideColors.VictoryRedStroke
                    isVictoryTile && winner == PlayerColor.BLUE -> HexlideColors.VictoryBlueStroke
                    isSelectedTile -> HexlideColors.PieceRed
                    isMovableTile -> HexlideColors.SelectedGlow
                    else -> HexlideColors.TileStroke
                }

                val strokeWidth = when {
                    isVictoryTile -> 3f
                    isSelectedTile || isMovableTile -> 2f
                    else -> 1f
                }

                val alpha = if (isFaded) 0.2f else 1f

                drawHexagon(
                    center = Offset(screenX, screenY),
                    hexPoints = hexPoints,
                    scale = scale,
                    fillColor = fillColor,
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth * scale,
                    alpha = alpha,
                )
            }

            // ----- Layer 2: Tile destination guides -----
            if (phase == GamePhase.MOVE_TILE && selectedItem is SelectedItem.TileIndexItem) {
                for (dest in validDests) {
                    val (dx, dy) = HexMath.hexToPixel(dest.q, dest.r, hexSize)
                    val screenX = dx * scale + offsetX
                    val screenY = dy * scale + offsetY

                    drawHexagon(
                        center = Offset(screenX, screenY),
                        hexPoints = hexPointsSmall,
                        scale = scale,
                        fillColor = HexlideColors.ValidDestFill.copy(alpha = 0.8f),
                        strokeColor = HexlideColors.ValidDest,
                        strokeWidth = 2f * scale,
                        alpha = 1f,
                        dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4f * scale, 4f * scale),
                            0f,
                        ),
                    )
                }
            }

            // ----- Layer 3: Piece destination hints -----
            if (phase == GamePhase.MOVE_TOKEN && selectedItem is SelectedItem.PieceItem) {
                for (dest in validDests) {
                    val (dx, dy) = HexMath.hexToPixel(dest.q, dest.r, hexSize)
                    val screenX = dx * scale + offsetX
                    val screenY = dy * scale + offsetY
                    val hintRadius = pieceRadius * 0.75f * scale

                    drawCircle(
                        color = HexlideColors.ValidDest.copy(alpha = 0.3f),
                        radius = hintRadius,
                        center = Offset(screenX, screenY),
                    )
                    drawCircle(
                        color = HexlideColors.ValidDest,
                        radius = hintRadius,
                        center = Offset(screenX, screenY),
                        style = Stroke(width = 2f * scale),
                    )
                }
            }

            // ----- Layer 4: Pieces -----
            for ((index, piece) in pieces.withIndex()) {
                val (animX, animY) = animatedPositions.getOrElse(index) {
                    val (px, py) = HexMath.hexToPixel(piece.q, piece.r, hexSize)
                    Pair(px * scale + offsetX, py * scale + offsetY)
                }

                val isVictoryPiece = victoryLine.contains(piece.coordsKey)
                val isMyPiece = piece.player == myColor
                val canSelect = winner == null && isMyPiece && phase == GamePhase.MOVE_TOKEN && isInteractive
                val isSelected = selectedItem is SelectedItem.PieceItem && selectedItem.pieceId == piece.id
                val isFaded = winner != null && !isVictoryPiece

                val pieceColor = if (piece.player == PlayerColor.RED)
                    HexlideColors.PieceRed else HexlideColors.PieceBlue

                val drawRadius = pieceRadius * scale * (if (isSelected) 1.15f else if (isVictoryPiece) 1.1f else 1f)
                val alpha = if (isFaded) 0.2f else 1f

                // Selection glow
                if (isSelected) {
                    drawCircle(
                        color = HexlideColors.SelectedGlow.copy(alpha = 0.6f * alpha),
                        radius = drawRadius + 6f * scale,
                        center = Offset(animX, animY),
                    )
                }

                // Main piece circle
                drawCircle(
                    color = pieceColor.copy(alpha = alpha),
                    radius = drawRadius,
                    center = Offset(animX, animY),
                )

                // Inner subtle shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.05f * alpha),
                    radius = 14f * scale * (if (isSelected) 1.15f else 1f),
                    center = Offset(animX, animY),
                )

                // Selection / selectable border
                if (isSelected) {
                    drawCircle(
                        color = HexlideColors.SelectedGlow.copy(alpha = alpha),
                        radius = drawRadius + 4f * scale,
                        center = Offset(animX, animY),
                        style = Stroke(width = 6f * scale),
                    )
                } else if (canSelect) {
                    drawCircle(
                        color = HexlideColors.SelectedGlow.copy(alpha = 0.6f * alpha),
                        radius = drawRadius + 2f * scale,
                        center = Offset(animX, animY),
                        style = Stroke(width = 2f * scale),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHexagon(
    center: Offset,
    hexPoints: List<Pair<Float, Float>>,
    scale: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
    alpha: Float,
    dashEffect: PathEffect? = null,
) {
    if (hexPoints.isEmpty()) return

    val path = Path().apply {
        val first = hexPoints.first()
        moveTo(center.x + first.first * scale, center.y + first.second * scale)
        for (i in 1 until hexPoints.size) {
            val pt = hexPoints[i]
            lineTo(center.x + pt.first * scale, center.y + pt.second * scale)
        }
        close()
    }

    drawPath(
        path = path,
        color = fillColor.copy(alpha = fillColor.alpha * alpha),
        style = Fill,
    )

    drawPath(
        path = path,
        color = strokeColor.copy(alpha = strokeColor.alpha * alpha),
        style = Stroke(
            width = strokeWidth,
            pathEffect = dashEffect,
        ),
    )
}
