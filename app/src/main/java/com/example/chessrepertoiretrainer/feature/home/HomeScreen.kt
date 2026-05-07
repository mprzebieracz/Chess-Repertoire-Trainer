package com.example.chessrepertoiretrainer.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAnalysis: () -> Unit,
    onPlayDailyPuzzle: () -> Unit
) {
    val dailyPuzzleState by viewModel.dailyPuzzleState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DailyPuzzleCard(
                state = dailyPuzzleState,
                onPlay = onPlayDailyPuzzle,
                onRetry = viewModel::retry
            )

            OutlinedButton(onClick = onOpenAnalysis) {
                Text("Open analysis board")
            }
        }
    }
}

@Composable
private fun DailyPuzzleCard(
    state: DailyPuzzleState,
    onPlay: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is DailyPuzzleState.Solved -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Daily Puzzle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when (state) {
                DailyPuzzleState.Loading -> {
                    CircularProgressIndicator()
                }

                DailyPuzzleState.Fetching -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Fetching today's puzzle…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is DailyPuzzleState.Available -> {
                    Text(
                        text = "Rating: ${state.rating}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.themes.isNotBlank()) {
                        Text(text = state.themes, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onPlay) {
                        Text("Play")
                    }
                }

                DailyPuzzleState.Solved -> {
                    Text(
                        text = "✓ Completed today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is DailyPuzzleState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
