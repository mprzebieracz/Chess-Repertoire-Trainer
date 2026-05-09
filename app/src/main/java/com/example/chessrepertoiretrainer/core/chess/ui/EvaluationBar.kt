package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EvaluationBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val whiteFraction = fraction.coerceIn(0f, 1f)
    val blackFraction = 1f - whiteFraction

    Column(
        modifier = modifier.clip(RoundedCornerShape(4.dp))
    ) {
        // Black portion (top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(blackFraction.coerceAtLeast(0.03f))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.TopCenter
        ) {}

        // White portion (bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(whiteFraction.coerceAtLeast(0.03f))
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.BottomCenter
        ) {}
    }
}

@Composable
fun EvaluationScore(
    scoreLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = scoreLabel,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 2.dp)
    )
}
