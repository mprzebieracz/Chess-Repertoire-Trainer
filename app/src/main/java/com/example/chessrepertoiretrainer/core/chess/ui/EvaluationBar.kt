package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val colorWhite = Color(0xFFEEEEEE)
private val colorBlack = Color(0xFF1A1A1A)

/**
 * Horizontal evaluation bar.
 *
 * The caller controls the size via [modifier] — typically [Modifier.weight(1f)] inside a Row.
 * Score label is drawn on the bar itself on whichever side is winning. The label is suppressed
 * during cross-side transitions so it never appears on the wrong colour.
 *
 * [fraction] is white's advantage 0..1 (0.5 = equal).
 * [isFlipped] flips which side fills from the left to match the board orientation.
 * [scoreLabel] e.g. "+1.2" or "+M3".
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

    val leadingColor = if (!isFlipped) colorWhite else colorBlack
    val trailingColor = if (!isFlipped) colorBlack else colorWhite
    val leadingFrac = (if (!isFlipped) animatedFraction else 1f - animatedFraction).coerceIn(0f, 1f)

    // Determine which side the score sits on based on the animated position.
    val scoreOnLeft = leadingFrac >= 0.5f

    // Suppress the label while the bar is mid-transition to the opposite side.
    // This prevents the new value appearing on the wrong colour during animation.
    val targetLeadingFrac = (if (!isFlipped) fraction else 1f - fraction).coerceIn(0f, 1f)
    val targetScoreOnLeft = targetLeadingFrac >= 0.5f
    val displayLabel = if (scoreOnLeft == targetScoreOnLeft) scoreLabel else ""

    val scoreBackground = if (scoreOnLeft) leadingColor else trailingColor
    val scoreTextColor = if (scoreBackground == colorWhite) colorBlack else colorWhite

    Box(modifier = modifier) {
        // Trailing colour fills the entire bar.
        Box(Modifier
                .fillMaxSize()
                .background(trailingColor))
        // Leading colour fills from the left.
        Box(Modifier
                .fillMaxHeight()
                .fillMaxWidth(leadingFrac)
                .background(leadingColor))
        // Score text overlaid on the dominant side.
        if (displayLabel.isNotEmpty()) {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = scoreTextColor,
                modifier = Modifier
                    .align(if (scoreOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 6.dp),
            )
        }
    }
}