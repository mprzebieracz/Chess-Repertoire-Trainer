package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Standard scrollable column used by all chessboard screen content bars.
 */
@Composable
fun ContentBarScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
fun LoadingPlaceholder(text: String = "Loading…") {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/**
 * Flat card for displaying a move comment.
 * Uses the background color + rectangle shape consistent across Learn/Review.
 */
@Composable
fun MoveCommentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RectangleShape,
        content = { Column(content = content) },
    )
}

/**
 * Shows "Line X of Y" progress or a named-line header.
 *
 * - [currentLineNumber] and [totalLines]: show "Line X of Y" when > 0.
 * - [lineName]: when non-null, show the line name instead of (or in addition to) the counter.
 * - Pass both to show name on top and counter below, or just one.
 */
@Composable
fun LineProgressHeader(
    currentLineNumber: Int = 0,
    totalLines: Int = 0,
    lineName: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        lineName?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (totalLines > 0 && lineName == null) {
            Text(
                text = "Line $currentLineNumber of $totalLines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}