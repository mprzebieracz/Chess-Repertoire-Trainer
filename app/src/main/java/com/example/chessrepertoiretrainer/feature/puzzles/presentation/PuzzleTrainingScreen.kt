package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun PuzzleTrainingScreen(viewModel: PuzzleTrainingViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        topBar = { ChessTopBar(title = "Daily Puzzle", onBackClick = onBackClick) },
        contentBar = { PuzzleContentBar(uiState = uiState) },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(AppIcons.Hint,
                                "Hint",
                                viewModel::showHint,
                                enabled = !uiState.isLoading && !uiState.isSessionComplete)
                BottomBarButton(AppIcons.Solution,
                                "Solution",
                                viewModel::showSolution,
                                enabled = !uiState.isLoading && !uiState.isSessionComplete)
            }
        },
    )
}

@Composable
private fun PuzzleContentBar(uiState: PuzzleTrainingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when {
            uiState.isLoading -> Text("Loading puzzle…",
                                      style = MaterialTheme.typography.bodyMedium)

            uiState.isSessionComplete -> Text(
                text = uiState.statusMessage ?: "Daily puzzle complete!",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )

            else -> {
                uiState.currentRating?.let {
                    Text("Rating: $it",
                         style = MaterialTheme.typography.bodyMedium,
                         fontWeight = FontWeight.SemiBold)
                }
                uiState.currentThemes?.takeIf { it.isNotBlank() }?.let {
                    Text("Themes: $it",
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(top = 2.dp))
                }
                uiState.userSideLabel?.let {
                    Text("$it to move",
                         style = MaterialTheme.typography.bodySmall,
                         modifier = Modifier.padding(top = 2.dp))
                }
                if (uiState.attemptsForCurrent > 0) Text("Attempts: ${uiState.attemptsForCurrent}",
                                                         style = MaterialTheme.typography.bodySmall,
                                                         modifier = Modifier.padding(top = 2.dp))
                val statusText = when {
                    uiState.lastMoveWasCorrect == true -> "Correct!"
                    uiState.lastMoveWasCorrect == false -> "Incorrect, try again"
                    uiState.isWaitingForUserMove -> "Your turn: find the best move."
                    else -> uiState.statusMessage ?: ""
                }
                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (uiState.lastMoveWasCorrect != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = when (uiState.lastMoveWasCorrect) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}