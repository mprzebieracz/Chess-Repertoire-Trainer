package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import kotlin.math.sqrt

private val arrowColor = Color(0xFF1E88E5)

@Composable
internal fun ArrowsLayer(
    arrows: List<Arrow>,
    squareSizePx: Float,
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
) {
    if (arrows.isEmpty() || squareSizePx <= 0f) return
    Canvas(modifier = modifier) {
        for (arrow in arrows) {
            if (arrow.isHighlight) drawHighlight(arrow, squareSizePx, isFlipped)
            else drawArrow(arrow, squareSizePx, isFlipped)
        }
    }
}

private fun DrawScope.drawHighlight(arrow: Arrow, squareSizePx: Float, isFlipped: Boolean) {
    val vp = visualPosition(arrow.from, isFlipped)
    drawRect(
        color = arrowColor.copy(alpha = (arrow.alpha * 0.5f).coerceIn(0f, 1f)),
        topLeft = Offset(vp.file * squareSizePx, vp.rank * squareSizePx),
        size = Size(squareSizePx, squareSizePx)
    )
}

private fun DrawScope.drawArrow(arrow: Arrow, squareSizePx: Float, isFlipped: Boolean) {
    val fromCenter = squareCenterPx(arrow.from, squareSizePx, isFlipped)
    val toCenter = squareCenterPx(arrow.to, squareSizePx, isFlipped)

    val dx = toCenter.x - fromCenter.x
    val dy = toCenter.y - fromCenter.y
    val length = sqrt(dx * dx + dy * dy)
    if (length == 0f) return

    val ux = dx / length
    val uy = dy / length
    // perpendicular unit vector
    val px = -uy
    val py = ux

    val shaftHalfWidth = squareSizePx * 0.11f
    val headHalfWidth = squareSizePx * 0.26f
    val headLength = squareSizePx * 0.44f
    val shaftInset = squareSizePx * 0.30f

    val color = arrowColor.copy(alpha = (arrow.alpha * 0.85f).coerceIn(0f, 1f))

    val shaftStart = Offset(fromCenter.x + ux * shaftInset, fromCenter.y + uy * shaftInset)
    val headBase = Offset(toCenter.x - ux * headLength, toCenter.y - uy * headLength)

    // Single path for shaft + head so no region is painted twice (which would darken alpha).
    val path = Path().apply {
        moveTo(shaftStart.x + px * shaftHalfWidth, shaftStart.y + py * shaftHalfWidth)
        lineTo(headBase.x + px * shaftHalfWidth, headBase.y + py * shaftHalfWidth)
        lineTo(headBase.x + px * headHalfWidth, headBase.y + py * headHalfWidth)
        lineTo(toCenter.x, toCenter.y)
        lineTo(headBase.x - px * headHalfWidth, headBase.y - py * headHalfWidth)
        lineTo(headBase.x - px * shaftHalfWidth, headBase.y - py * shaftHalfWidth)
        lineTo(shaftStart.x - px * shaftHalfWidth, shaftStart.y - py * shaftHalfWidth)
        close()
    }
    drawPath(path, color)

    // Separate circle for the rounded shaft start (no overlap with the path above).
    drawCircle(color = color, radius = shaftHalfWidth, center = shaftStart)
}
