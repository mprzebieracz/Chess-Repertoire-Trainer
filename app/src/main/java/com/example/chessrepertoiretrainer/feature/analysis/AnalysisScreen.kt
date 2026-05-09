package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BoardBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.EnginePanel

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel, onBackClick: () -> Unit) {
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            Column {
                engineError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                BoardBottomBar(chessCtrl = viewModel.chessController)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChessScreenLayout(
                title = "Analysis",
                chessCtrl = viewModel.chessController,
                showNavigationControls = false,
                showBoardActionButtons = false,
                evaluationBarFraction = if (isEngineEnabled) engineAnalysis?.evaluationBarFraction else null,
                titleEndContent = {
                    EngineToggleButton(isEnabled = isEngineEnabled, onClick = viewModel::toggleEngine)
                },
                topContent = { AnalysisTopBar(onBackClick = onBackClick) },
                midContent = {
                    if (isEngineEnabled) {
                        EnginePanel(
                            analysis = engineAnalysis,
                            searchState = engineSearchState,
                            onDeeperClick = viewModel::analyzeDeeper
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun EngineToggleButton(isEnabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = if (isEnabled) "Disable engine" else "Enable engine",
            tint = if (isEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun AnalysisTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "Back", style = MaterialTheme.typography.bodyMedium)
    }
}
