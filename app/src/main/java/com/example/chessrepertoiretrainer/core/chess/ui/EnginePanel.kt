package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState

/**
 * Engine analysis panel: score label and principal variation on the same row.
 */
@Composable
fun EnginePanel(
    analysis: EngineAnalysis?,
    searchState: EngineSearchState,
    modifier: Modifier = Modifier,
) {
    val isSearching = searchState == EngineSearchState.SEARCHING

    when {
        isSearching && analysis == null -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    text = "Calculating…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        analysis != null -> {
            val lineScrollState = rememberScrollState()
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = analysis.scoreLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(33.dp),
                )
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = analysis.line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(lineScrollState),
                )
            }
        }

        else -> {}
    }
}

/**
 * Combined engine section shown above the board.
 *
 * Layout:
 *   [eval bar — full screen width]
 *   [score  …continuation moves…]
 *
 * The "Deeper" button and engine toggle live in the title bar.
 */
@Composable
fun EngineSection(
    analysis: EngineAnalysis?,
    searchState: EngineSearchState,
    isFlipped: Boolean,
    onDeeperClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalEvaluationBar(
            fraction = analysis?.evaluationBarFraction ?: 0.5f,
            isFlipped = isFlipped,
            scoreLabel = analysis?.scoreLabel ?: "0.0",
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        )
        EnginePanel(
            analysis = analysis,
            searchState = searchState,
        )
    }
}

/**
 * Title-bar button showing the current engine depth.
 * Label is e.g. "d20"; only enabled when the search is complete.
 */
@Composable
fun DeeperButton(
    searchState: EngineSearchState,
    depth: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = searchState == EngineSearchState.COMPLETE,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Text("d$depth", fontSize = 11.sp)
    }
}