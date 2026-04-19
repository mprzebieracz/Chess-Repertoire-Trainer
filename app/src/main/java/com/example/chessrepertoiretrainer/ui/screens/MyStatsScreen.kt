package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.UserSettings
import com.example.chessrepertoiretrainer.domain.stats.GameStatsSummary
import com.example.chessrepertoiretrainer.ui.viewmodels.MyStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsScreen(
    viewModel: MyStatsViewModel,
    settings: UserSettings,
    onOpenProfileTree: (profileId: Long, color: String, timeControl: String, maxGames: Int?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(settings.lichessUsername, settings.chessComUsername) {
        viewModel.onUserSettingsChanged(
            lichessUsername = settings.lichessUsername,
            chessComUsername = settings.chessComUsername
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My stats") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (settings.lichessUsername.isBlank() && settings.chessComUsername.isBlank()) {
                Text(
                    text = "Set your Lichess and/or Chess.com usernames in Settings to see your stats.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val hasLichessAccount = settings.lichessUsername.isNotBlank()
                val hasChessComAccount = settings.chessComUsername.isNotBlank()

                // Simple per-platform statistics derived from stored games.
                val lichessStats = uiState.lichessStats
                val chessComStats = uiState.chessComStats

                if (lichessStats != null || chessComStats != null) {
                    Text(
                        text = "Quick stats from your games",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (lichessStats != null && hasLichessAccount) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PlatformStatsSection(platformLabel = "Lichess", summary = lichessStats)
                    }

                    if (chessComStats != null && hasChessComAccount) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PlatformStatsSection(platformLabel = "Chess.com", summary = chessComStats)
                    }
                } else {
                    Text(
                        text = "No stats yet. Download your games below to see a summary.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                var showTreeSetup by rememberSaveable { mutableStateOf(false) }

                Button(onClick = { showTreeSetup = !showTreeSetup }) {
                    Text(
                        text = if (showTreeSetup) {
                            "Hide opening tree filters"
                        } else {
                            "Open my opening tree"
                        }
                    )
                }

                if (showTreeSetup) {
                    Spacer(modifier = Modifier.height(16.dp))

                    var useLichess by rememberSaveable { mutableStateOf(hasLichessAccount) }
                    var useChessCom by rememberSaveable { mutableStateOf(hasChessComAccount) }

                    val anyPlatformSelected =
                        (useLichess && hasLichessAccount) || (useChessCom && hasChessComAccount)

                    Text(
                        text = "Build an opening tree from your online games",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Platform selection
                    Text(text = "Platform", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasLichessAccount) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = useLichess,
                                    onCheckedChange = { useLichess = it }
                                )
                                Text(text = "Lichess")
                            }
                        }
                        if (hasChessComAccount) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = useChessCom,
                                    onCheckedChange = { useChessCom = it }
                                )
                                Text(text = "Chess.com")
                            }
                        }
                    }

                    if (!anyPlatformSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select at least one platform above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shared filters
                    var colorFilter by rememberSaveable { mutableStateOf("white") }
                    var bulletEnabled by rememberSaveable { mutableStateOf(true) }
                    var blitzEnabled by rememberSaveable { mutableStateOf(true) }
                    var rapidEnabled by rememberSaveable { mutableStateOf(true) }
                    var classicalEnabled by rememberSaveable { mutableStateOf(true) }
                    var maxGamesText by rememberSaveable { mutableStateOf("500") }

                    Text(text = "Color", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = colorFilter == "white",
                            onClick = { colorFilter = "white" }
                        )
                        Text(text = "White")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = colorFilter == "black",
                            onClick = { colorFilter = "black" }
                        )
                        Text(text = "Black")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Time controls", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = bulletEnabled,
                            onCheckedChange = { bulletEnabled = it }
                        )
                        Text(text = "Bullet")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = blitzEnabled,
                            onCheckedChange = { blitzEnabled = it }
                        )
                        Text(text = "Blitz")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rapidEnabled,
                            onCheckedChange = { rapidEnabled = it }
                        )
                        Text(text = "Rapid")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = classicalEnabled,
                            onCheckedChange = { classicalEnabled = it }
                        )
                        Text(text = "Classical/Daily")
                    }

                    OutlinedTextField(
                        value = maxGamesText,
                        onValueChange = { newValue ->
                            if (newValue.isBlank() || newValue.all { it.isDigit() }) {
                                maxGamesText = newValue
                            }
                        },
                        label = { Text("Use last N games (empty = all)") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val maxGames = maxGamesText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                            val selectedCategories = buildList {
                                if (bulletEnabled) add("bullet")
                                if (blitzEnabled) add("blitz")
                                if (rapidEnabled) add("rapid")
                                if (classicalEnabled) add("classical")
                            }
                            val timeControlFilter = if (selectedCategories.size == 4) {
                                ""
                            } else {
                                selectedCategories.joinToString(",")
                            }

                            viewModel.syncAndPrepareTreeForSelection(
                                useLichess = useLichess,
                                useChessCom = useChessCom,
                                color = colorFilter,
                                timeControlFilter = timeControlFilter,
                                maxGamesForTree = maxGames
                            ) { profileId ->
                                onOpenProfileTree(profileId, colorFilter, timeControlFilter, maxGames)
                            }
                        },
                        enabled = uiState.isAnySyncing.not() && anyPlatformSelected
                    ) {
                        Text("Download my games and open tree")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.refreshGamesForSelection(
                                useLichess = useLichess,
                                useChessCom = useChessCom
                            )
                        },
                        enabled = uiState.isAnySyncing.not() && anyPlatformSelected
                    ) {
                        Text("Refresh my games from online")
                    }

                    if (uiState.isAnySyncing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Syncing games and building your opening tree…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformStatsSection(platformLabel: String, summary: GameStatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = platformLabel,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Games: ${summary.totalGames} · Winrate: ${summary.winPercent}%",
            style = MaterialTheme.typography.bodyMedium
        )

        if (summary.byTimeCategory.isNotEmpty()) {
            val byTimeText = summary.byTimeCategory
                .filter { it.value > 0 }
                .entries
                .sortedByDescending { it.value }
                .joinToString("   ·   ") { (cat, count) ->
                    "${cat.replaceFirstChar { c -> c.uppercase() }}: $count"
                }
            if (byTimeText.isNotBlank()) {
                Text(
                    text = "By time control: $byTimeText",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (summary.topOpenings.isNotEmpty()) {
            Text(
                text = "Top openings:",
                style = MaterialTheme.typography.labelMedium
            )
            summary.topOpenings.forEach { opening ->
                Text(
                    text = "• ${shortenOpeningKey(opening.openingKey)} – ${opening.games} games, ${opening.scorePercent}% score",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun shortenOpeningKey(key: String, maxChars: Int = 40): String {
    // Display at most the first two SAN moves (e.g. "e4 e5", "e4 c5") and
    // keep the label reasonably short for the UI.
    val tokens = key.split(" ").filter { it.isNotBlank() }
    val trimmed = tokens.take(2).joinToString(" ")
    return if (trimmed.length <= maxChars) trimmed else trimmed.take(maxChars).trimEnd() + "…"
}
