package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout

@Composable
fun PuzzleTrainingScreen(
    viewModel: PuzzleTrainingViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChessScreenLayout(
        title = "Daily Puzzle",
        chessCtrl = viewModel.chessController,
        showNavigationControls = false,
        showBoardActionButtons = false,
        allowPgnNavigation = false,
        topContent = {
            PuzzleTrainingTopContent(uiState = uiState, onBackClick = onBackClick)
        },
        bottomContent = {
            PuzzleTrainingBottomContent(
                uiState = uiState,
                onShowSolution = viewModel::showSolution,
                onShowHint = viewModel::showHint
            )
        }
    )
}

@Composable
private fun PuzzleTrainingTopContent(
    uiState: PuzzleTrainingUiState,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        PuzzleTrainingBackRow(onBackClick = onBackClick)
        PuzzleTrainingHeaderDetails(uiState = uiState)
    }
}

@Composable
private fun PuzzleTrainingBackRow(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "Back", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PuzzleTrainingHeaderDetails(uiState: PuzzleTrainingUiState) {
    if (uiState.isSessionComplete && !uiState.isLoading) {
        Text(
            text = uiState.statusMessage ?: "Daily puzzle complete!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        return
    }

    if (uiState.currentRating != null) {
        Text(
            text = "Rating: ${uiState.currentRating}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
    if (!uiState.currentThemes.isNullOrBlank()) {
        Text(text = "Themes: ${uiState.currentThemes}", style = MaterialTheme.typography.bodySmall)
    }
    if (uiState.userSideLabel != null) {
        Text(text = "${uiState.userSideLabel} to move.", style = MaterialTheme.typography.bodySmall)
    }
    if (uiState.attemptsForCurrent > 0) {
        Text(
            text = "Attempts: ${uiState.attemptsForCurrent}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PuzzleTrainingBottomContent(
    uiState: PuzzleTrainingUiState,
    onShowSolution: () -> Unit,
    onShowHint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        PuzzleTrainingStatusMessage(uiState = uiState)

        if (!uiState.isLoading && !uiState.isSessionComplete) {
            PuzzleTrainingActionButtons(
                onShowSolution = onShowSolution,
                onShowHint = onShowHint
            )
        }
    }
}

@Composable
private fun PuzzleTrainingStatusMessage(uiState: PuzzleTrainingUiState) {
    when {
        uiState.isLoading -> Text(
            text = "Loading puzzle...",
            style = MaterialTheme.typography.bodyMedium
        )

        uiState.isSessionComplete -> Text(
            text = uiState.statusMessage ?: "Daily puzzle complete!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        uiState.lastMoveWasCorrect == true -> Text(
            text = "Correct!",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )

        uiState.lastMoveWasCorrect == false -> Text(
            text = "Incorrect, try again",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        else -> Text(
            text = uiState.statusMessage ?: if (uiState.isWaitingForUserMove) {
                "Your turn: find the best move."
            }
            else {
                "..."
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PuzzleTrainingActionButtons(
    onShowSolution: () -> Unit,
    onShowHint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onShowSolution) {
            Text("Solution")
        }
        OutlinedButton(onClick = onShowHint) {
            Text("Hint")
        }
    }
}
