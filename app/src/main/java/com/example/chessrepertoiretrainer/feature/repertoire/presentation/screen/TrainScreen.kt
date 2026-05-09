package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingUiState
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingViewModel

@Composable
fun TrainScreen(viewModel: TrainingViewModel,
                onBackClick: (() -> Unit)? = null,
                onSessionComplete: (() -> Unit)? = null) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete && onSessionComplete != null) {
            onSessionComplete()
        }
    }

    ChessScreenLayout(title = "Train",
                      chessCtrl = viewModel.chessController,
                      showNavigationControls = false,
                      showBoardActionButtons = false,
                      allowPgnNavigation = false,
                      topContent = {
                          TrainTopContent(uiState = uiState, onBackClick = onBackClick)
                      },
                      bottomContent = {
                          TrainBottomContent(uiState = uiState)
                      })
}

@Composable
private fun TrainTopContent(uiState: TrainingUiState, onBackClick: (() -> Unit)?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (onBackClick != null) {
            TrainBackRow(onBackClick = onBackClick)
        }

        TrainHeader(uiState = uiState)
    }
}

@Composable
private fun TrainBackRow(onBackClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(Modifier.width(8.dp))

        Text(text = "Back", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TrainHeader(uiState: TrainingUiState) {
    if (uiState.isSessionEmpty) {
        Text(text = "No lines to train. Create lines in your repertoire first.",
             style = MaterialTheme.typography.bodyMedium)
        return
    }

    uiState.currentLineName?.let { currentLineName ->
        if (uiState.totalLines > 0) {
            Text(text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
                 style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.SemiBold)
        }

        Text(text = currentLineName, style = MaterialTheme.typography.bodyMedium)
    }

    uiState.myColor?.let { color ->
        Text(text = "You play $color", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TrainBottomContent(uiState: TrainingUiState) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        TrainStatusMessage(uiState = uiState)
    }
}

@Composable
private fun TrainStatusMessage(uiState: TrainingUiState) {
    when {
        uiState.isSessionComplete -> {
            Text(text = uiState.statusMessage ?: "Training complete",
                 style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.SemiBold)
        }

        uiState.lastMoveWasCorrect == true -> {
            Text(text = "Correct move",
                 color = MaterialTheme.colorScheme.primary,
                 style = MaterialTheme.typography.bodyMedium)
        }

        uiState.lastMoveWasCorrect == false -> {
            Text(text = "Incorrect move",
                 color = MaterialTheme.colorScheme.error,
                 style = MaterialTheme.typography.bodyMedium)
        }

        !uiState.isSessionEmpty && !uiState.isSessionComplete -> {
            Text(text = if (uiState.isWaitingForUserMove) {
                "Your turn: follow the repertoire moves."
            }
            else {
                "Waiting for training session..."
            }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}