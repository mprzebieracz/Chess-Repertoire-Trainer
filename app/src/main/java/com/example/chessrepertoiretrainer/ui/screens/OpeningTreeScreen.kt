package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessScreenLayout
import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeViewModel

@Composable
fun OpeningTreeScreen(
    viewModel: OpeningTreeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.onGoBack() },
                    enabled = uiState.pathMoves.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = { viewModel.onGoRoot() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Root")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChessScreenLayout(
                title = "Opening Tree",
                chessCtrl = viewModel.chessController,
                showNavigationControls = false,
                showBoardActionButtons = false,
                topContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (uiState.pathMoves.isEmpty()) {
                                "From starting position"
                            } else {
                                "Path: " + uiState.pathMoves.joinToString(" ")
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                bottomContent = {
                    if (uiState.moves.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Next moves",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.moves) { move ->
                                    OpeningTreeMoveCard(move = move) {
                                        viewModel.onMoveSelected(move.toFen)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No further moves from this position",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OpeningTreeMoveCard(
    move: OpeningTreeViewModel.OpeningTreeMoveUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = move.moveSan, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.size(4.dp))
            // Simple colored bar representation: W / D / L
            val winWeight = move.winPercent.coerceIn(0, 100)
            val drawWeight = move.drawPercent.coerceIn(0, 100)
            val lossWeight = move.lossPercent.coerceIn(0, 100)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (winWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(winWeight.toFloat())
                            .background(Color(0xFF4CAF50))
                    )
                }
                if (drawWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(drawWeight.toFloat())
                            .background(Color(0xFF9E9E9E))
                    )
                }
                if (lossWeight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(lossWeight.toFloat())
                            .background(Color(0xFFF44336))
                    )
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "W ${move.winPercent}%  D ${move.drawPercent}%  L ${move.lossPercent}%  (${move.games} games)",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.size(4.dp))

            Spacer(modifier = Modifier.size(4.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("Go to position")
            }
        }
    }
}


