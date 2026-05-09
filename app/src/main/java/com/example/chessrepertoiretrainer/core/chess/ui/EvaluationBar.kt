package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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