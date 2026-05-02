package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningTreeSearchScreen(
    viewModel: OpeningTreeSearchViewModel,
    defaultLichessUsername: String,
    defaultChessComUsername: String,
    defaultPlatform: String,
    onOpenTree: (username: String, platform: String, color: String, timeControl: String, maxGames: Int?) -> Unit // <- THIS LINE
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val initialState = remember(defaultPlatform, defaultLichessUsername, defaultChessComUsername) {
        OpeningTreeSearchFormState.initial(
            defaultPlatform = defaultPlatform,
            defaultLichessUsername = defaultLichessUsername,
            defaultChessComUsername = defaultChessComUsername
        )
    }
    var formState by remember(defaultPlatform, defaultLichessUsername, defaultChessComUsername) {
        mutableStateOf(initialState)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Opening tree search") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchIntro()
            UsernameSection(
                formState = formState, onFormStateChange = { formState = it })
            PlatformSection(
                formState = formState,
                defaultLichessUsername = defaultLichessUsername,
                defaultChessComUsername = defaultChessComUsername,
                onFormStateChange = { formState = it })
            ColorSection(
                formState = formState, onFormStateChange = { formState = it })
            TimeControlsSection(
                formState = formState, onFormStateChange = { formState = it })
            MaxGamesSection(
                formState = formState, onFormStateChange = { formState = it })
            DownloadSection(
                isEnabled = formState.username.isNotBlank() && !uiState.isSyncing,
                isSyncing = uiState.isSyncing,
                onDownloadGames = {
                    val maxGames = formState.maxGamesOrNull()
                    val timeControlFilter = formState.timeControlFilter()

                    viewModel.searchAndPrepareOpeningTree(
                        username = formState.username.trim(),
                        platform = formState.platform,
                        maxGamesForTree = maxGames,
                        color = formState.colorFilter,
                        timeControlFilter = timeControlFilter
                    ) { readyUsername, readyPlatform ->
                        onOpenTree(
                            readyUsername,
                            readyPlatform,
                            formState.colorFilter,
                            timeControlFilter,
                            maxGames
                        )
                    }
                })
            SearchFeedbackSection(uiState = uiState)
        }
    }
}

data class OpeningTreeSearchFormState(
    val platform: String,
    val username: String,
    val colorFilter: String = "white",
    val bulletEnabled: Boolean = true,
    val blitzEnabled: Boolean = true,
    val rapidEnabled: Boolean = true,
    val classicalEnabled: Boolean = true,
    val maxGamesText: String = "200"
) {
    fun withPlatform(
        newPlatform: String, defaultLichessUsername: String, defaultChessComUsername: String
    ): OpeningTreeSearchFormState {
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
        fun initial(
            defaultPlatform: String, defaultLichessUsername: String, defaultChessComUsername: String
        ): OpeningTreeSearchFormState {
            val platform = if (defaultPlatform == "chess.com") "chess.com" else "lichess"
            val username = if (platform == "chess.com") {
                defaultChessComUsername
            }
            else {
                defaultLichessUsername
            }

            return OpeningTreeSearchFormState(
                platform = platform, username = username
            )
        }
    }
}

@Composable
private fun SearchIntro() {
    Text(
        text = "Build an opening tree from any player's online games",
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun UsernameSection(
    formState: OpeningTreeSearchFormState, onFormStateChange: (OpeningTreeSearchFormState) -> Unit
) {
    OutlinedTextField(
        value = formState.username,
        onValueChange = { onFormStateChange(formState.copy(username = it)) },
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlatformSection(
    formState: OpeningTreeSearchFormState,
    defaultLichessUsername: String,
    defaultChessComUsername: String,
    onFormStateChange: (OpeningTreeSearchFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Platform", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformOption(
                label = "Lichess", selected = formState.platform == "lichess", onClick = {
                    onFormStateChange(
                        formState.withPlatform(
                            newPlatform = "lichess",
                            defaultLichessUsername = defaultLichessUsername,
                            defaultChessComUsername = defaultChessComUsername
                        )
                    )
                })
            Spacer(modifier = Modifier.width(16.dp))
            PlatformOption(
                label = "Chess.com", selected = formState.platform == "chess.com", onClick = {
                    onFormStateChange(
                        formState.withPlatform(
                            newPlatform = "chess.com",
                            defaultLichessUsername = defaultLichessUsername,
                            defaultChessComUsername = defaultChessComUsername
                        )
                    )
                })
        }
    }
}

@Composable
private fun PlatformOption(
    label: String, selected: Boolean, onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label)
    }
}

@Composable
private fun ColorSection(
    formState: OpeningTreeSearchFormState, onFormStateChange: (OpeningTreeSearchFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Color", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = formState.colorFilter == "white",
                onClick = { onFormStateChange(formState.copy(colorFilter = "white")) })
            Text(text = "White")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = formState.colorFilter == "black",
                onClick = { onFormStateChange(formState.copy(colorFilter = "black")) })
            Text(text = "Black")
        }
    }
}

@Composable
private fun TimeControlsSection(
    formState: OpeningTreeSearchFormState, onFormStateChange: (OpeningTreeSearchFormState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Time controls", style = MaterialTheme.typography.labelMedium)
        TimeControlRow(
            firstLabel = "Bullet",
            firstChecked = formState.bulletEnabled,
            onFirstChange = { onFormStateChange(formState.copy(bulletEnabled = it)) },
            secondLabel = "Blitz",
            secondChecked = formState.blitzEnabled,
            onSecondChange = { onFormStateChange(formState.copy(blitzEnabled = it)) })
        TimeControlRow(
            firstLabel = "Rapid",
            firstChecked = formState.rapidEnabled,
            onFirstChange = { onFormStateChange(formState.copy(rapidEnabled = it)) },
            secondLabel = "Classical/Daily",
            secondChecked = formState.classicalEnabled,
            onSecondChange = { onFormStateChange(formState.copy(classicalEnabled = it)) })
    }
}

@Composable
private fun TimeControlRow(
    firstLabel: String,
    firstChecked: Boolean,
    onFirstChange: (Boolean) -> Unit,
    secondLabel: String,
    secondChecked: Boolean,
    onSecondChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = firstChecked, onCheckedChange = onFirstChange)
        Text(text = firstLabel)
        Spacer(modifier = Modifier.width(12.dp))
        Checkbox(checked = secondChecked, onCheckedChange = onSecondChange)
        Text(text = secondLabel)
    }
}

@Composable
private fun MaxGamesSection(
    formState: OpeningTreeSearchFormState, onFormStateChange: (OpeningTreeSearchFormState) -> Unit
) {
    OutlinedTextField(
        value = formState.maxGamesText,
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() }) {
                onFormStateChange(formState.copy(maxGamesText = newValue))
            }
        },
        label = { Text("Max games to download (empty = all)") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DownloadSection(
    onDownloadGames: () -> Unit, isEnabled: Boolean, isSyncing: Boolean
) {
    Button(
        onClick = onDownloadGames, enabled = isEnabled && !isSyncing
    ) {
        Text("Download games and open tree")
    }
}

@Composable
private fun SearchFeedbackSection(uiState: OpeningTreeSearchViewModel.SearchUiState) {
    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }

    uiState.lastSyncSummary?.let { summary ->
        Text(
            text = summary, style = MaterialTheme.typography.bodySmall
        )
    }

    uiState.statusMessage?.let { status ->
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

