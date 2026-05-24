package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun OpeningTreeScreen(viewModel: OpeningTreeViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Preparing opening tree…", style = MaterialTheme.typography.titleMedium)
                uiState.statusMessage?.takeIf { it.isNotBlank() }
                    ?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        return
    }

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        annotations = viewModel.annotations,
        topBar = { ChessTopBar(title = "Opening Tree", onBackClick = onBackClick) },
        contentBar = {
            OpeningTreeMovesPanel(uiState = uiState, onMoveSelected = viewModel::onMoveSelected)
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(
                    AppIcons.Back,
                    "Back",
                    { viewModel.onGoBack() },
                    enabled = uiState.canGoBack
                )
                BottomBarButton(AppIcons.Home, "Root", { viewModel.onGoRoot() })
            }
        },
    )
}

@Composable
private fun OpeningTreeMovesPanel(uiState: OpeningTreeUiState, onMoveSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        uiState.statusMessage?.let { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        if (uiState.moves.isNotEmpty()) {
            Text(text = "Next moves", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.size(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.moves) { move ->
                    OpeningTreeMoveCard(move = move) { onMoveSelected(move.moveSan) }
                }
            }
        } else {
            Text("No further moves from this position.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OpeningTreeMoveCard(move: OpeningTreeMoveUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = move.moveSan, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "W ${move.winPercent}%  D ${move.drawPercent}%  L ${move.lossPercent}%  (${move.games})",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
