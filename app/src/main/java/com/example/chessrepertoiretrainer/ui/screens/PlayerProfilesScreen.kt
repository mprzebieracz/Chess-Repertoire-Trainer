package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import com.example.chessrepertoiretrainer.ui.viewmodels.PlayerProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfilesScreen(
  viewModel: PlayerProfilesViewModel,
  onOpenProfileTree: (profileId: Long, color: String, timeControl: String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  val (username, setUsername) = remember { mutableStateOf("") }
  val (platform, setPlatform) = remember { mutableStateOf("lichess") }
  val (colorFilter, setColorFilter) = remember { mutableStateOf("both") }
  val (timeControlFilter, setTimeControlFilter) = remember { mutableStateOf("") }
  val (maxGamesText, setMaxGamesText) = remember { mutableStateOf("200") }

    Scaffold(
        topBar = {
                  TopAppBar(title = { Text("Your games") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
                  Text(
                    text = "Download your games for opening analysis",
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                  Spacer(modifier = Modifier.height(8.dp))

                  Text(text = "Color", style = MaterialTheme.typography.labelMedium)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                      selected = colorFilter == "both",
                      onClick = { setColorFilter("both") }
                    )
                    Text(text = "Both")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                      selected = colorFilter == "white",
                      onClick = { setColorFilter("white") }
                    )
                    Text(text = "White")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                      selected = colorFilter == "black",
                      onClick = { setColorFilter("black") }
                    )
                    Text(text = "Black")
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  OutlinedTextField(
                    value = timeControlFilter,
                    onValueChange = setTimeControlFilter,
                    label = { Text("Time control filter (optional)") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  OutlinedTextField(
                    value = maxGamesText,
                    onValueChange = { newValue ->
                      // Allow only digits or empty string
                      if (newValue.all { it.isDigit() } ) {
                        setMaxGamesText(newValue)
                      }
                    },
                    label = { Text("Max games to download (empty = all)") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Button(
                    onClick = {
                      val maxGames = maxGamesText.toIntOrNull()
                      viewModel.syncGamesForUsername(
                        username = username,
                        platform = platform,
                        maxGames = maxGames
                      ) { profileId ->
                        onOpenProfileTree(profileId, colorFilter, timeControlFilter)
                      }
                    },
                    enabled = username.isNotBlank() && !uiState.isSyncing
                  ) {
                    Text(if (uiState.isSyncing) "Syncing..." else "Download games and open tree")
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

                  if (uiState.isSyncing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Syncing games...",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
        }
    }
}
