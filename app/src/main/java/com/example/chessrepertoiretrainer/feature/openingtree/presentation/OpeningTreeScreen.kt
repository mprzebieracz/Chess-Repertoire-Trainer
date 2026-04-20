package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout

@Composable
fun OpeningTreeScreen(
    viewModel: OpeningTreeViewModel, onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.onGoBack() }, enabled = uiState.pathMoves.isNotEmpty(), modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = { viewModel.onGoRoot() }, modifier = Modifier.weight(1f)
                ) {
                    Text("Root")
                }
            }
        }) { padding ->
        if (uiState.isLoading) {
            // While the opening tree is being prepared, show a simple
            // full-screen progress view instead of a half-populated board.
            Box(
                modifier = Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preparing opening tree...", style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = uiState.statusMessage ?: "", style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        else {
            Column(modifier = Modifier.padding(padding)) {
                ChessScreenLayout(
                    title = "Opening Tree",
                    chessCtrl = viewModel.chessController,
                    showPgnBar = false,
                    showNavigationControls = false,
                    showBoardActionButtons = false,
                    topContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                                )
                            }
                        }
                    },
                    bottomContent = {
                        if (uiState.moves.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Next moves", style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.moves) { move ->
                                        OpeningTreeMoveCard(move = move) {
                                            viewModel.onMoveSelected(move.toFen)
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            Text(
                                text = uiState.statusMessage ?: "No further moves from this position",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    })
            }
        }
    }
}

@Composable
private fun OpeningTreeMoveCard(
    move: OpeningTreeViewModel.OpeningTreeMoveUi, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = move.moveSan, style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "W ${move.winPercent}%  D ${move.drawPercent}%  L ${move.lossPercent}%  (${move.games} games)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


