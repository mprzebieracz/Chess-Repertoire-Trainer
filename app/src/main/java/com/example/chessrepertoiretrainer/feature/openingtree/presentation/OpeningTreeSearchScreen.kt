package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningTreeSearchScreen(viewModel: OpeningTreeSearchViewModel,
                            defaultLichessUsername: String,
                            defaultChessComUsername: String,
                            defaultPlatform: String,
                            onOpenTree: (OpeningTree) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialState = remember(defaultPlatform, defaultLichessUsername, defaultChessComUsername) {
        OpeningTreeSearchFormState.initial(defaultPlatform = defaultPlatform,
                                           defaultLichessUsername = defaultLichessUsername,
                                           defaultChessComUsername = defaultChessComUsername)
    }
    var formState by remember(defaultPlatform, defaultLichessUsername, defaultChessComUsername) {
        mutableStateOf(initialState)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Opening Tree") }) }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
               verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Spacer(Modifier.height(4.dp))

            Text(text = "Build an opening tree from your online games",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

            OutlinedTextField(value = formState.username,
                              onValueChange = { formState = formState.copy(username = it) },
                              label = { Text("Username") },
                              modifier = Modifier.fillMaxWidth(),
                              singleLine = true)

            ChipGroupSection(label = "Platform") {
                FilterChip(selected = formState.platform == "lichess",
                           onClick = { formState = formState.withPlatform("lichess", defaultLichessUsername, defaultChessComUsername) },
                           label = { Text("Lichess") })
                FilterChip(selected = formState.platform == "chess.com",
                           onClick = { formState = formState.withPlatform("chess.com", defaultLichessUsername, defaultChessComUsername) },
                           label = { Text("Chess.com") })
            }

            ChipGroupSection(label = "Color") {
                FilterChip(selected = formState.colorFilter == "white",
                           onClick = { formState = formState.copy(colorFilter = "white") },
                           label = { Text("White") })
                FilterChip(selected = formState.colorFilter == "black",
                           onClick = { formState = formState.copy(colorFilter = "black") },
                           label = { Text("Black") })
            }

            ChipGroupSection(label = "Time controls") {
                FilterChip(selected = formState.bulletEnabled,
                           onClick = { formState = formState.copy(bulletEnabled = !formState.bulletEnabled) },
                           label = { Text("Bullet") })
                FilterChip(selected = formState.blitzEnabled,
                           onClick = { formState = formState.copy(blitzEnabled = !formState.blitzEnabled) },
                           label = { Text("Blitz") })
                FilterChip(selected = formState.rapidEnabled,
                           onClick = { formState = formState.copy(rapidEnabled = !formState.rapidEnabled) },
                           label = { Text("Rapid") })
                FilterChip(selected = formState.classicalEnabled,
                           onClick = { formState = formState.copy(classicalEnabled = !formState.classicalEnabled) },
                           label = { Text("Classical") })
            }

            OutlinedTextField(value = formState.maxGamesText,
                              onValueChange = { newValue ->
                                  if (newValue.all { it.isDigit() }) formState = formState.copy(maxGamesText = newValue)
                              },
                              label = { Text("Max games (leave empty for all)") },
                              modifier = Modifier.fillMaxWidth(),
                              singleLine = true)

            Button(onClick = {
                val maxGames = formState.maxGamesOrNull()
                val timeControlFilter = formState.timeControlFilter()
                viewModel.searchAndPrepareOpeningTree(username = formState.username.trim(),
                                                      platform = formState.platform,
                                                      maxGames = maxGames,
                                                      color = formState.colorFilter,
                                                      timeControlFilter = timeControlFilter,
                                                      onTreeReady = { tree -> onOpenTree(tree) })
            },
                   enabled = formState.username.isNotBlank() && !uiState.isSyncing,
                   modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.OpeningTree, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Build opening tree", fontWeight = FontWeight.SemiBold)
            }

            if (uiState.isSyncing) {
                val statusText = when (uiState.syncPhase) {
                    SyncPhase.FetchingGames -> "Fetching games… (${uiState.fetchedGameCount} fetched)"
                    SyncPhase.BuildingTree  -> "Building tree from ${uiState.fetchedGameCount} games…"
                    SyncPhase.Idle          -> ""
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            uiState.errorMessage?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            uiState.lastSyncSummary?.let { summary ->
                Text(text = summary,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChipGroupSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label,
             style = MaterialTheme.typography.labelLarge,
             fontWeight = FontWeight.SemiBold,
             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

data class OpeningTreeSearchFormState(val platform: String,
                                      val username: String,
                                      val colorFilter: String = "white",
                                      val bulletEnabled: Boolean = true,
                                      val blitzEnabled: Boolean = true,
                                      val rapidEnabled: Boolean = true,
                                      val classicalEnabled: Boolean = true,
                                      val maxGamesText: String = "200") {
    fun withPlatform(newPlatform: String,
                     defaultLichessUsername: String,
                     defaultChessComUsername: String): OpeningTreeSearchFormState {
        val nextUsername = when (newPlatform) {
            "chess.com" -> defaultChessComUsername.takeIf { it.isNotBlank() } ?: username
            else -> defaultLichessUsername.takeIf { it.isNotBlank() } ?: username
        }
        return copy(platform = newPlatform, username = nextUsername)
    }

    fun timeControlFilter(): String {
        val selectedCategories = buildList {
            if (bulletEnabled) add("bullet")
            if (blitzEnabled) add("blitz")
            if (rapidEnabled) add("rapid")
            if (classicalEnabled) add("classical")
        }
        return if (selectedCategories.size == 4) "" else selectedCategories.joinToString(",")
    }

    fun maxGamesOrNull(): Int? = maxGamesText.toIntOrNull()

    companion object {
        fun initial(defaultPlatform: String,
                    defaultLichessUsername: String,
                    defaultChessComUsername: String): OpeningTreeSearchFormState {
            val platform = if (defaultPlatform == "chess.com") "chess.com" else "lichess"
            val username = if (platform == "chess.com") defaultChessComUsername else defaultLichessUsername
            return OpeningTreeSearchFormState(platform = platform, username = username)
        }
    }
}
