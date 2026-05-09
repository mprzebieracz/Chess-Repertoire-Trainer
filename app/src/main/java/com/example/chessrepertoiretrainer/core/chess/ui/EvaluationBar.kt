package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val colorWhite = Color(0xFFEEEEEE)
private val colorBlack = Color(0xFF1A1A1A)

/**
 * Vertical evaluation bar that animates smoothly between values.
 *
 * [fraction] is white's advantage in 0..1 (0.5 = equal, 1.0 = white wins, 0.0 = black wins).
 * [isFlipped] swaps which colour is at the bottom so the bar matches the board orientation.
 */
@Composable
fun EvaluationBar(fraction: Float, isFlipped: Boolean = false, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(targetValue = fraction,
                                                animationSpec = tween(durationMillis = 500,
                                                                      easing = FastOutSlowInEasing),
                                                label = "eval_bar")

    val base = modifier.clip(RoundedCornerShape(4.dp))

    when {
        animatedFraction >= 1f -> Box(modifier = base
            .fillMaxSize()
            .background(colorWhite))
        animatedFraction <= 0f -> Box(modifier = base
            .fillMaxSize()
            .background(colorBlack))
        else -> {
            val whiteFrac = animatedFraction
            val blackFrac = 1f - animatedFraction
            Column(modifier = base) {
                if (!isFlipped) {
                    Box(Modifier
                            .fillMaxWidth()
                            .weight(blackFrac)
                            .background(colorBlack))
                    Box(Modifier
                            .fillMaxWidth()
                            .weight(whiteFrac)
                            .background(colorWhite))
                }
                else {
                    Box(Modifier
                            .fillMaxWidth()
                            .weight(whiteFrac)
                            .background(colorWhite))
                    Box(Modifier
                            .fillMaxWidth()
                            .weight(blackFrac)
                            .background(colorBlack))
                }
            }
        }
    }
}

/**
 * Horizontal evaluation bar shown above the board.
 *
 * [fraction] is white's advantage 0..1 (0.5 = equal).
 * [isFlipped] reverses which side fills from the left so the bar matches the board orientation
 *   (when black is at the bottom, black's portion fills from the left).
 * [scoreLabel] is displayed to the left of the bar, e.g. "+1.2" or "+M3".
 */
@Composable
fun HorizontalEvaluationBar(
    fraction: Float,
    isFlipped: Boolean,
    scoreLabel: String,
    modifier: Modifier = Modifier,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "eval_bar_h",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = scoreLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp),
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            val (leadingColor, trailingColor, leadingFrac) = if (!isFlipped) {
                Triple(colorWhite, colorBlack, animatedFraction)
            } else {
                Triple(colorBlack, colorWhite, 1f - animatedFraction)
            }
            Box(Modifier.fillMaxSize().background(trailingColor))
            Box(Modifier.fillMaxHeight().fillMaxWidth(leadingFrac.coerceIn(0f, 1f)).background(leadingColor))
        }
    }
}
