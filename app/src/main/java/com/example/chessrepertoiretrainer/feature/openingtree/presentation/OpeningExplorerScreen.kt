package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.network.explorer.MasterGameEntry
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningExplorerScreen(
    viewModel: OpeningExplorerViewModel,
    onBackClick: () -> Unit,
    onOpenMasterGame: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        annotations = viewModel.annotations,
        topBar = { ChessTopBar(title = "Opening Explorer", onBackClick = onBackClick) },
        contentBar = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ExplorerSource.entries.forEachIndexed { index, source ->
                        SegmentedButton(
                            selected = uiState.source == source,
                            onClick = { viewModel.setSource(source) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index,
                                ExplorerSource.entries.size
                            ),
                            label = {
                                Text(
                                    text = if (source == ExplorerSource.LICHESS_PLAYERS) "Players" else "Masters",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                if (uiState.isFetching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                uiState.statusMessage?.let { msg ->
                    Text(
                        msg, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }

                if (uiState.moves.isNotEmpty() || uiState.topGames.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.moves.isNotEmpty()) {
                            item {
                                Text("Next moves", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.size(4.dp))
                            }
                            items(uiState.moves) { move ->
                                ExplorerMoveCard(move = move) { viewModel.onMoveSelected(move.moveSan) }
                            }
                        }
                        if (uiState.topGames.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.size(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    "Top master games",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                            }
                            items(uiState.topGames) { game ->
                                MasterGameCard(game = game) {
                                    viewModel.openMasterGame(game.id) { pgn -> onOpenMasterGame(pgn) }
                                }
                            }
                        }
                    }
                }
                else if (!uiState.isFetching) {
                    Text(
                        "No moves found for this position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
        }
    )
}

@Composable
private fun ExplorerMoveCard(move: OpeningTreeMoveUi, onClick: () -> Unit) {
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
            Text(move.moveSan, style = MaterialTheme.typography.bodyMedium)
            Text(
                "W ${move.winPercent}%  D ${move.drawPercent}%  B ${move.lossPercent}%  (${move.games})",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MasterGameCard(game: MasterGameEntry, onClick: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${game.white} vs ${game.black}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    game.year.toString(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (game.winner) {
                    "white" -> "1-0"; "black" -> "0-1"; else -> "½-½"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}