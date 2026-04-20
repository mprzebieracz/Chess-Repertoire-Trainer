package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PuzzlesScreen(
    viewModel: PuzzlesViewModel,
    onStartTraining: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    PuzzlesScaffold {
        PuzzlesStateContent(
            uiState = uiState,
            onStartTraining = onStartTraining
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzlesScaffold(content: @Composable () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Puzzles") }) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun PuzzlesStateContent(
    uiState: PuzzlesUiState,
    onStartTraining: () -> Unit
) {
    when {
        uiState.isLoading -> LoadingState()
        uiState.errorMessage != null -> ErrorState(errorMessage = uiState.errorMessage)
        else -> ReadyState(
            unsolvedCount = uiState.unsolvedCount,
            onStartTraining = onStartTraining
        )
    }
}

@Composable
private fun LoadingState() {
    CircularProgressIndicator()
}

@Composable
private fun ErrorState(errorMessage: String?) {
    Text(
        text = "Error loading puzzles: $errorMessage",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun ReadyState(
    unsolvedCount: Int,
    onStartTraining: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PuzzleAvailabilityMessage(unsolvedCount = unsolvedCount)

        Spacer(modifier = Modifier.height(16.dp))

        StartDailyPuzzleButton(
            unsolvedCount = unsolvedCount,
            onStartTraining = onStartTraining
        )

        if (unsolvedCount == 0) {
            EmptyHintMessage()
        }
    }
}

@Composable
private fun PuzzleAvailabilityMessage(unsolvedCount: Int) {
    val message = if (unsolvedCount > 0) {
        "Today's Lichess daily puzzle is ready."
    } else {
        "No daily puzzle is currently available."
    }

    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun StartDailyPuzzleButton(
    unsolvedCount: Int,
    onStartTraining: () -> Unit
) {
    Button(
        onClick = onStartTraining,
        enabled = unsolvedCount > 0
    ) {
        Text("Play today's daily puzzle")
    }
}

@Composable
private fun EmptyHintMessage() {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "You may have already solved today's puzzle or it failed to download.",
        style = MaterialTheme.typography.bodySmall
    )
}
