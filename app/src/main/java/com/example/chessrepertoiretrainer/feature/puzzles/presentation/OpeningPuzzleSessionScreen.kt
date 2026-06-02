package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun OpeningPuzzleSessionScreen(
    viewModel: OpeningPuzzleSessionViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading && uiState.solvedInSession == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val title = "Puzzle ${uiState.solvedInSession + 1}"

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        annotations = viewModel.annotations,
        topBar = {
            ChessTopBar(
                title = title,
                onBackClick = onBackClick,
            )
        },
        contentBar = { SessionContentBar(uiState = uiState) },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(
                    AppIcons.Hint,
                    "Hint",
                    viewModel::showHint,
                    enabled = !uiState.isLoading && !uiState.isError,
                )
                BottomBarButton(
                    AppIcons.Solution,
                    "Solution",
                    viewModel::showSolution,
                    enabled = !uiState.isLoading && !uiState.isError,
                )
            }
        },
    )
}

@Composable
private fun SessionContentBar(uiState: OpeningPuzzleSessionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when {
            uiState.isError -> Text(
                text = "Could not load puzzles. Check your connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            uiState.isLoading -> Text("Loading next puzzle…", style = MaterialTheme.typography.bodyMedium)

            else -> {
                uiState.currentOpeningFamily?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                uiState.currentRating?.let {
                    Text(
                        "★ $it",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                uiState.userSideLabel?.let {
                    Text(
                        "$it to move",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                val statusText = when (uiState.lastMoveWasCorrect) {
                    true -> "Correct!"
                    false -> "Incorrect, try again"
                    null -> if (uiState.isWaitingForUserMove) "Find the best move" else uiState.statusMessage ?: ""
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
