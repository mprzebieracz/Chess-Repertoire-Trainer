package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI
import com.example.chessrepertoiretrainer.ui.components.chess.DefaultChessBoardController
import com.example.chessrepertoiretrainer.ui.viewmodels.YourGamesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourGamesScreen(viewModel: YourGamesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val gamesCount by viewModel.gamesCount.collectAsState()
    val treeState by viewModel.treeState.collectAsState()

    val (username, setUsername) = remember { mutableStateOf("") }
    val (platform, setPlatform) = remember { mutableStateOf("lichess") }

    val chessController = remember { DefaultChessBoardController() }

    LaunchedEffect(treeState.currentFen) {
        val fen = treeState.currentFen
        if (fen != null) {
            chessController.loadPositionFromFen(fen)
        } else {
            chessController.resetBoard()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Games") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Download your games from Lichess or Chess.com",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = setUsername,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Platform", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = platform == "lichess",
                        onClick = { setPlatform("lichess") }
                    )
                    Text(text = "Lichess")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = platform == "chess.com",
                        onClick = { setPlatform("chess.com") }
                    )
                    Text(text = "Chess.com")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.syncGamesForUser(username, platform, maxGames = 200)
                    },
                    enabled = username.isNotBlank() && !uiState.isSyncing
                ) {
                    Text(if (uiState.isSyncing) "Syncing..." else "Download games")
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                uiState.lastSyncSummary?.let { summary ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val labelUsername = uiState.lastProfileUsername
                val labelPlatform = uiState.lastProfilePlatform

                if (labelUsername != null && labelPlatform != null) {
                    Text(
                        text = "Stored locally: $gamesCount games for $labelUsername on $labelPlatform",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No games downloaded yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (treeState.currentFen != null) {
                    Text(
                        text = if (treeState.pathMoves.isEmpty()) "Opening tree from starting position" else
                            "Path: " + treeState.pathMoves.joinToString(" "),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChessboardUI(state = chessController)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.onTreeGoRoot() },
                            enabled = treeState.pathMoves.isNotEmpty()
                        ) {
                            Text("Root")
                        }

                        Button(
                            onClick = { viewModel.onTreeGoBack() },
                            enabled = treeState.pathMoves.isNotEmpty()
                        ) {
                            Text("Back")
                        }

                        treeState.moves.forEach { moveUi ->
                            Button(onClick = { viewModel.onTreeMoveSelected(moveUi.toFen) }) {
                                Text(
                                    text = buildString {
                                        append(moveUi.moveSan)
                                        append("  ")
                                        append("W ")
                                        append(moveUi.winPercent)
                                        append("% D ")
                                        append(moveUi.drawPercent)
                                        append("% L ")
                                        append(moveUi.lossPercent)
                                        append("%")
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
