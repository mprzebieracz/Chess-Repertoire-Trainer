package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
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
import com.example.chessrepertoiretrainer.ui.viewmodels.PlayerProfilesViewModel

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
 fun PlayerProfilesScreen(
   viewModel: PlayerProfilesViewModel,
   onOpenProfileTree: (profileId: Long, color: String, timeControl: String, maxGames: Int?) -> Unit
 ) {
  val uiState by viewModel.uiState.collectAsState()

  val (username, setUsername) = remember { mutableStateOf("") }
  val (platform, setPlatform) = remember { mutableStateOf("lichess") }
  val (colorFilter, setColorFilter) = remember { mutableStateOf("white") }
  val (bulletEnabled, setBulletEnabled) = remember { mutableStateOf(true) }
  val (blitzEnabled, setBlitzEnabled) = remember { mutableStateOf(true) }
  val (rapidEnabled, setRapidEnabled) = remember { mutableStateOf(true) }
  val (classicalEnabled, setClassicalEnabled) = remember { mutableStateOf(true) }
  val (maxGamesText, setMaxGamesText) = remember { mutableStateOf("200") }

     Scaffold(
         topBar = {
                   TopAppBar(title = { Text("Opening tree") })
         }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
                   Text(
                     text = "Prepare your opening tree from your online games",
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

                  Text(text = "Time controls", style = MaterialTheme.typography.labelMedium)

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                      checked = bulletEnabled,
                      onCheckedChange = { setBulletEnabled(it) }
                    )
                    Text(text = "Bullet")
                    Spacer(modifier = Modifier.width(12.dp))
                    Checkbox(
                      checked = blitzEnabled,
                      onCheckedChange = { setBlitzEnabled(it) }
                    )
                    Text(text = "Blitz")
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                      checked = rapidEnabled,
                      onCheckedChange = { setRapidEnabled(it) }
                    )
                    Text(text = "Rapid")
                    Spacer(modifier = Modifier.width(12.dp))
                    Checkbox(
                      checked = classicalEnabled,
                      onCheckedChange = { setClassicalEnabled(it) }
                    )
                    Text(text = "Classical/Daily")
                  }

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
                       // Build a comma-separated list of selected time-control categories.
                       val selectedCategories = buildList {
                         if (bulletEnabled) add("bullet")
                         if (blitzEnabled) add("blitz")
                         if (rapidEnabled) add("rapid")
                         if (classicalEnabled) add("classical")
                       }
                       // If all categories are selected, treat as no explicit filter.
                       val timeControlFilter = if (selectedCategories.size == 4) {
                         ""
                       } else {
                         selectedCategories.joinToString(",")
                       }
                       viewModel.syncGamesForUsername(
                         username = username,
                         platform = platform,
                         maxGamesForTree = maxGames,
                         color = colorFilter,
                         timeControlFilter = timeControlFilter
                       ) { profileId ->
                         onOpenProfileTree(profileId, colorFilter, timeControlFilter, maxGames)
                       }
                     },
                     enabled = username.isNotBlank() && !uiState.isSyncing
                   ) {
                     Text("Download games and open tree")
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

                  uiState.statusMessage?.let { status ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = status,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
        }
    }
}
