package com.example.chessrepertoiretrainer.feature.stats.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings
import com.example.chessrepertoiretrainer.feature.stats.domain.GameStatsSummary

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

    val showTreeSetup = rememberSaveable { mutableStateOf(false) }
    val useLichess = rememberSaveable { mutableStateOf(settings.lichessUsername.isNotBlank()) }
    val useChessCom = rememberSaveable { mutableStateOf(settings.chessComUsername.isNotBlank()) }
    val colorFilter = rememberSaveable { mutableStateOf("white") }
    val bulletEnabled = rememberSaveable { mutableStateOf(true) }
    val blitzEnabled = rememberSaveable { mutableStateOf(true) }
    val rapidEnabled = rememberSaveable { mutableStateOf(true) }
    val classicalEnabled = rememberSaveable { mutableStateOf(true) }
    val maxGamesText = rememberSaveable { mutableStateOf("500") }

    MyStatsScaffold {
        MyStatsContent(
            settings = settings,
            uiState = uiState,
            showTreeSetup = showTreeSetup.value,
            useLichess = useLichess.value,
            useChessCom = useChessCom.value,
            colorFilter = colorFilter.value,
            bulletEnabled = bulletEnabled.value,
            blitzEnabled = blitzEnabled.value,
            rapidEnabled = rapidEnabled.value,
            classicalEnabled = classicalEnabled.value,
            maxGamesText = maxGamesText.value,
            onToggleTreeSetup = { showTreeSetup.value = !showTreeSetup.value },
            onUseLichessChange = { useLichess.value = it },
            onUseChessComChange = { useChessCom.value = it },
            onColorFilterChange = { colorFilter.value = it },
            onBulletChange = { bulletEnabled.value = it },
            onBlitzChange = { blitzEnabled.value = it },
            onRapidChange = { rapidEnabled.value = it },
            onClassicalChange = { classicalEnabled.value = it },
            onMaxGamesTextChange = { maxGamesText.value = it },
            onOpenProfileTree = onOpenProfileTree,
            onSyncAndPrepareTree = {
                val maxGames = maxGamesText.value.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                val selectedCategories = buildList {
                    if (bulletEnabled.value) add("bullet")
                    if (blitzEnabled.value) add("blitz")
                    if (rapidEnabled.value) add("rapid")
                    if (classicalEnabled.value) add("classical")
                }
                val timeControlFilter = if (selectedCategories.size == 4) "" else selectedCategories.joinToString(",")

                viewModel.syncAndPrepareTreeForSelection(
                    useLichess = useLichess.value,
                    useChessCom = useChessCom.value,
                    color = colorFilter.value,
                    timeControlFilter = timeControlFilter,
                    maxGamesForTree = maxGames
                ) { profileId ->
                    onOpenProfileTree(profileId, colorFilter.value, timeControlFilter, maxGames)
                }
            },
            onRefreshGames = {
                viewModel.refreshGamesForSelection(
                    useLichess = useLichess.value,
                    useChessCom = useChessCom.value
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyStatsScaffold(content: @Composable () -> Unit) {
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
            content()
        }
    }
}

@Composable
private fun MyStatsContent(
    settings: UserSettings,
    uiState: MyStatsViewModel.MyStatsUiState,
    showTreeSetup: Boolean,
    useLichess: Boolean,
    useChessCom: Boolean,
    colorFilter: String,
    bulletEnabled: Boolean,
    blitzEnabled: Boolean,
    rapidEnabled: Boolean,
    classicalEnabled: Boolean,
    maxGamesText: String,
    onToggleTreeSetup: () -> Unit,
    onUseLichessChange: (Boolean) -> Unit,
    onUseChessComChange: (Boolean) -> Unit,
    onColorFilterChange: (String) -> Unit,
    onBulletChange: (Boolean) -> Unit,
    onBlitzChange: (Boolean) -> Unit,
    onRapidChange: (Boolean) -> Unit,
    onClassicalChange: (Boolean) -> Unit,
    onMaxGamesTextChange: (String) -> Unit,
    onOpenProfileTree: (profileId: Long, color: String, timeControl: String, maxGames: Int?) -> Unit,
    onSyncAndPrepareTree: () -> Unit,
    onRefreshGames: () -> Unit
) {
    if (settings.lichessUsername.isBlank() && settings.chessComUsername.isBlank()) {
        EmptyStatsState()
        return
    }

    val hasLichessAccount = settings.lichessUsername.isNotBlank()
    val hasChessComAccount = settings.chessComUsername.isNotBlank()

    QuickStatsSection(
        uiState = uiState,
        hasLichessAccount = hasLichessAccount,
        hasChessComAccount = hasChessComAccount
    )

    OpenTreeSetupToggle(
        showTreeSetup = showTreeSetup,
        onToggleTreeSetup = onToggleTreeSetup
    )

    if (showTreeSetup) {
        OpeningTreeSetupSection(
            uiState = uiState,
            hasLichessAccount = hasLichessAccount,
            hasChessComAccount = hasChessComAccount,
            useLichess = useLichess,
            useChessCom = useChessCom,
            colorFilter = colorFilter,
            bulletEnabled = bulletEnabled,
            blitzEnabled = blitzEnabled,
            rapidEnabled = rapidEnabled,
            classicalEnabled = classicalEnabled,
            maxGamesText = maxGamesText,
            onUseLichessChange = onUseLichessChange,
            onUseChessComChange = onUseChessComChange,
            onColorFilterChange = onColorFilterChange,
            onBulletChange = onBulletChange,
            onBlitzChange = onBlitzChange,
            onRapidChange = onRapidChange,
            onClassicalChange = onClassicalChange,
            onMaxGamesTextChange = onMaxGamesTextChange,
            onSyncAndPrepareTree = onSyncAndPrepareTree,
            onRefreshGames = onRefreshGames
        )
    }
}

@Composable
private fun EmptyStatsState() {
    Text(
        text = "Set your Lichess and/or Chess.com usernames in Settings to see your stats.",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun QuickStatsSection(
    uiState: MyStatsViewModel.MyStatsUiState,
    hasLichessAccount: Boolean,
    hasChessComAccount: Boolean
) {
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
}

@Composable
private fun OpenTreeSetupToggle(
    showTreeSetup: Boolean,
    onToggleTreeSetup: () -> Unit
) {
    Button(onClick = onToggleTreeSetup) {
        Text(
            text = if (showTreeSetup) "Hide opening tree filters" else "Open my opening tree"
        )
    }
}

@Composable
private fun OpeningTreeSetupSection(
    uiState: MyStatsViewModel.MyStatsUiState,
    hasLichessAccount: Boolean,
    hasChessComAccount: Boolean,
    useLichess: Boolean,
    useChessCom: Boolean,
    colorFilter: String,
    bulletEnabled: Boolean,
    blitzEnabled: Boolean,
    rapidEnabled: Boolean,
    classicalEnabled: Boolean,
    maxGamesText: String,
    onUseLichessChange: (Boolean) -> Unit,
    onUseChessComChange: (Boolean) -> Unit,
    onColorFilterChange: (String) -> Unit,
    onBulletChange: (Boolean) -> Unit,
    onBlitzChange: (Boolean) -> Unit,
    onRapidChange: (Boolean) -> Unit,
    onClassicalChange: (Boolean) -> Unit,
    onMaxGamesTextChange: (String) -> Unit,
    onSyncAndPrepareTree: () -> Unit,
    onRefreshGames: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Build an opening tree from your online games",
        style = MaterialTheme.typography.titleMedium
    )

    PlatformSelectionSection(
        hasLichessAccount = hasLichessAccount,
        hasChessComAccount = hasChessComAccount,
        useLichess = useLichess,
        useChessCom = useChessCom,
        onUseLichessChange = onUseLichessChange,
        onUseChessComChange = onUseChessComChange
    )

    if (!anyPlatformSelected(useLichess, useChessCom, hasLichessAccount, hasChessComAccount)) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select at least one platform above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    SharedFiltersSection(
        colorFilter = colorFilter,
        bulletEnabled = bulletEnabled,
        blitzEnabled = blitzEnabled,
        rapidEnabled = rapidEnabled,
        classicalEnabled = classicalEnabled,
        maxGamesText = maxGamesText,
        onColorFilterChange = onColorFilterChange,
        onBulletChange = onBulletChange,
        onBlitzChange = onBlitzChange,
        onRapidChange = onRapidChange,
        onClassicalChange = onClassicalChange,
        onMaxGamesTextChange = onMaxGamesTextChange
    )

    Spacer(modifier = Modifier.height(8.dp))

    TreeActionButtonsSection(
        isSyncing = uiState.isAnySyncing,
        anyPlatformSelected = anyPlatformSelected(useLichess, useChessCom, hasLichessAccount, hasChessComAccount),
        onSyncAndPrepareTree = onSyncAndPrepareTree,
        onRefreshGames = onRefreshGames
    )

    if (uiState.isAnySyncing) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Syncing games and building your opening tree…",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PlatformSelectionSection(
    hasLichessAccount: Boolean,
    hasChessComAccount: Boolean,
    useLichess: Boolean,
    useChessCom: Boolean,
    onUseLichessChange: (Boolean) -> Unit,
    onUseChessComChange: (Boolean) -> Unit
) {
    Text(text = "Platform", style = MaterialTheme.typography.labelMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (hasLichessAccount) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useLichess, onCheckedChange = onUseLichessChange)
                Text(text = "Lichess")
            }
        }
        if (hasChessComAccount) {
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useChessCom, onCheckedChange = onUseChessComChange)
                Text(text = "Chess.com")
            }
        }
    }
}

@Composable
private fun SharedFiltersSection(
    colorFilter: String,
    bulletEnabled: Boolean,
    blitzEnabled: Boolean,
    rapidEnabled: Boolean,
    classicalEnabled: Boolean,
    maxGamesText: String,
    onColorFilterChange: (String) -> Unit,
    onBulletChange: (Boolean) -> Unit,
    onBlitzChange: (Boolean) -> Unit,
    onRapidChange: (Boolean) -> Unit,
    onClassicalChange: (Boolean) -> Unit,
    onMaxGamesTextChange: (String) -> Unit
) {
    Text(text = "Color", style = MaterialTheme.typography.labelMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = colorFilter == "white", onClick = { onColorFilterChange("white") })
        Text(text = "White")
        Spacer(modifier = Modifier.width(16.dp))
        RadioButton(selected = colorFilter == "black", onClick = { onColorFilterChange("black") })
        Text(text = "Black")
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(text = "Time controls", style = MaterialTheme.typography.labelMedium)
    SingleTimeControlRow(label = "Bullet", checked = bulletEnabled, onCheckedChange = onBulletChange)
    SingleTimeControlRow(label = "Blitz", checked = blitzEnabled, onCheckedChange = onBlitzChange)
    SingleTimeControlRow(label = "Rapid", checked = rapidEnabled, onCheckedChange = onRapidChange)
    SingleTimeControlRow(label = "Classical/Daily", checked = classicalEnabled, onCheckedChange = onClassicalChange)

    OutlinedTextField(
        value = maxGamesText,
        onValueChange = { newValue ->
            if (newValue.isBlank() || newValue.all { it.isDigit() }) {
                onMaxGamesTextChange(newValue)
            }
        },
        label = { Text("Use last N games (empty = all)") },
        singleLine = true
    )
}

@Composable
private fun SingleTimeControlRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label)
    }
}

@Composable
private fun TreeActionButtonsSection(
    isSyncing: Boolean,
    anyPlatformSelected: Boolean,
    onSyncAndPrepareTree: () -> Unit,
    onRefreshGames: () -> Unit
) {
    Button(
        onClick = onSyncAndPrepareTree,
        enabled = !isSyncing && anyPlatformSelected
    ) {
        Text("Download my games and open tree")
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
        onClick = onRefreshGames,
        enabled = !isSyncing && anyPlatformSelected
    ) {
        Text("Refresh my games from online")
    }
}

private fun anyPlatformSelected(
    useLichess: Boolean,
    useChessCom: Boolean,
    hasLichessAccount: Boolean,
    hasChessComAccount: Boolean
): Boolean {
    return (useLichess && hasLichessAccount) || (useChessCom && hasChessComAccount)
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
            val byTimeText =
                summary.byTimeCategory.filter { it.value > 0 }.entries.sortedByDescending { it.value }.joinToString("   ·   ") { (cat, count) ->
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
    val tokens = key.split(" ").filter { it.isNotBlank() }
    val trimmed = tokens.take(2).joinToString(" ")
    return if (trimmed.length <= maxChars) trimmed else trimmed.take(maxChars).trimEnd() + "…"
}
