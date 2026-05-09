package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun EnginePanel(analysis: EngineAnalysis?,
                searchState: EngineSearchState,
                onDeeperClick: () -> Unit,
                modifier: Modifier = Modifier) {

    val isSearching = searchState == EngineSearchState.SEARCHING
    val scrollState = rememberScrollState()

    Row(modifier = modifier
        .fillMaxWidth()
        .height(36.dp)
        .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isSearching && analysis == null) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Text(text = "Calculating…",
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        else if (analysis != null) {
            Text(text = analysis.scoreLabel,
                 style = MaterialTheme.typography.labelMedium,
                 fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.width(44.dp))
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
            }
            else {
                Text(text = "d${analysis.depth}",
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                     fontSize = 10.sp)
            }
            Text(text = analysis.line,
                 style = MaterialTheme.typography.bodySmall,
                 fontFamily = FontFamily.Monospace,
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onSurface,
                 maxLines = 1,
                 softWrap = false,
                 modifier = Modifier
                     .weight(1f)
                     .horizontalScroll(scrollState))
            FilledTonalButton(onClick = onDeeperClick,
                              enabled = searchState == EngineSearchState.COMPLETE,
                              modifier = Modifier.height(28.dp),
                              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                Text("Deeper", fontSize = 11.sp)
            }
        }
        else {
            Spacer(Modifier.weight(1f))
        }
    }
}