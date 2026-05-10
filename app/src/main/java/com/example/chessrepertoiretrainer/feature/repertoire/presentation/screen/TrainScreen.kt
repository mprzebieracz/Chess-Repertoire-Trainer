package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingUiState
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingViewModel

@Composable
fun TrainScreen(
    viewModel: TrainingViewModel,
    onBackClick: (() -> Unit)? = null,
    onSessionComplete: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete && onSessionComplete != null) onSessionComplete()
    }

    val title = buildString {
        uiState.currentLineName?.let { append(it) }
        uiState.currentChapterName?.let { name ->
            if (isNotEmpty()) append(" · ")
            append(name)
        }
        if (isEmpty()) append("Train")
    }

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        topBar = {
            ChessTopBar(title = title, onBackClick = onBackClick ?: {})
        },
        contentBar = {
            TrainContentBar(uiState = uiState)
        },
        bottomBar = {
            val canAct = !uiState.isLoading && !uiState.isSessionComplete && !uiState.isSessionEmpty
            ChessBottomBar {
                BottomBarButton(AppIcons.Hint, "Hint", { viewModel.showHint() }, enabled = canAct && uiState.isWaitingForUserMove)
                BottomBarButton(AppIcons.Solution, "Solution", { viewModel.showSolution() }, enabled = canAct && uiState.isWaitingForUserMove)
            }
        },
    )
}

@Composable
private fun TrainContentBar(uiState: TrainingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (uiState.totalLines > 0) {
            Text(
                text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        uiState.myColor?.let {
            Text(
                text = "You play $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (uiState.lastMoveWasCorrect) {
                    true -> MaterialTheme.colorScheme.primaryContainer
                    false -> MaterialTheme.colorScheme.errorContainer
                    null -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val message = when {
                    uiState.isSessionEmpty -> "No lines to train. Create lines in your repertoire first."
                    uiState.isSessionComplete -> uiState.statusMessage ?: "Training complete!"
                    uiState.lastMoveWasCorrect == true -> "Correct move"
                    uiState.lastMoveWasCorrect == false -> "Incorrect — try again"
                    uiState.isWaitingForUserMove -> "Your turn: follow the repertoire moves."
                    else -> "…"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (uiState.lastMoveWasCorrect) {
                        true -> MaterialTheme.colorScheme.onPrimaryContainer
                        false -> MaterialTheme.colorScheme.onErrorContainer
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (uiState.lastMoveWasCorrect == false) {
                    uiState.lastExpectedSan?.let {
                        Text(text = "Expected: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
