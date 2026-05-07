package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameFilter
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.StatsTimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGamesScreen(
    viewModel: MyGamesViewModel,
    onOpenGame: (SavedGame) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Games") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SyncRow(
                uiState = uiState,
                onSync = viewModel::sync,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stats") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Games") })
            }

            when (selectedTab) {
                0 -> StatsTab(
                    uiState = uiState,
                    onTimeRangeSelected = viewModel::setTimeRange
                )
                1 -> GamesTab(
                    games = games,
                    activeFilter = uiState.activeFilter,
                    onFilterChange = viewModel::setFilter,
                    onOpenGame = onOpenGame
                )
            }
        }
    }
}

@Composable
private fun SyncRow(
    uiState: MyGamesUiState,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (uiState.gameCounts.isNotEmpty()) {
                uiState.gameCounts.entries.forEach { (platform, count) ->
                    Text(
                        text = "${platform.replaceFirstChar { it.uppercase() }}: $count games",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else {
                Text(text = "No games stored yet", style = MaterialTheme.typography.bodySmall)
            }
            uiState.syncProgress?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            uiState.syncError?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (uiState.isSyncing) {
            CircularProgressIndicator()
        }
        else {
            Button(onClick = onSync) { Text("Sync") }
        }
    }
}

@Composable
private fun StatsTab(
    uiState: MyGamesUiState,
    onTimeRangeSelected: (StatsTimeRange) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StatsTimeRange.entries) { range ->
                FilterChip(
                    selected = uiState.selectedTimeRange == range,
                    onClick = { onTimeRangeSelected(range) },
                    label = { Text(range.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.statsRows) { (label, stats) ->
                StatsCard(label = label, stats = stats)
            }
        }
    }
}

@Composable
private fun StatsCard(label: String, stats: GameStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            if (stats.played == 0) {
                Text(text = "No games", style = MaterialTheme.typography.bodySmall)
            }
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCell(label = "Played", value = stats.played.toString())
                    StatCell(label = "W", value = "${stats.wins} (${(stats.winPct * 100).toInt()}%)",
                        color = MaterialTheme.colorScheme.primary)
                    StatCell(label = "L", value = "${stats.losses} (${(stats.lossPct * 100).toInt()}%)",
                        color = MaterialTheme.colorScheme.error)
                    StatCell(label = "D", value = "${stats.draws} (${(stats.drawPct * 100).toInt()}%)")
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun GamesTab(
    games: List<SavedGame>,
    activeFilter: GameFilter,
    onFilterChange: (GameFilter) -> Unit,
    onOpenGame: (SavedGame) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterRow(activeFilter = activeFilter, onFilterChange = onFilterChange)

        if (games.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "No games found", style = MaterialTheme.typography.bodyMedium)
            }
        }
        else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(games) { game ->
                    GameCard(game = game, onClick = { onOpenGame(game) })
                }
            }
        }
    }
}

@Composable
private fun FilterRow(activeFilter: GameFilter, onFilterChange: (GameFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = activeFilter.platform == "lichess",
                onClick = {
                    onFilterChange(activeFilter.copy(
                        platform = if (activeFilter.platform == "lichess") null else "lichess"
                    ))
                },
                label = { Text("Lichess") }
            )
        }
        item {
            FilterChip(
                selected = activeFilter.platform == "chess.com",
                onClick = {
                    onFilterChange(activeFilter.copy(
                        platform = if (activeFilter.platform == "chess.com") null else "chess.com"
                    ))
                },
                label = { Text("Chess.com") }
            )
        }
        items(listOf("bullet", "blitz", "rapid", "classical")) { cat ->
            FilterChip(
                selected = activeFilter.timeCategory == cat,
                onClick = {
                    onFilterChange(activeFilter.copy(
                        timeCategory = if (activeFilter.timeCategory == cat) null else cat
                    ))
                },
                label = { Text(cat.replaceFirstChar { it.uppercase() }) }
            )
        }
        items(listOf("win" to "Win", "loss" to "Loss", "draw" to "Draw")) { (key, label) ->
            FilterChip(
                selected = activeFilter.playerResult == key,
                onClick = {
                    onFilterChange(activeFilter.copy(
                        playerResult = if (activeFilter.playerResult == key) null else key
                    ))
                },
                label = { Text(label) }
            )
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
private fun GameCard(game: SavedGame, onClick: () -> Unit) {
    val resultColor = when (game.playerResult) {
        "win" -> MaterialTheme.colorScheme.primary
        "loss" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = game.platform.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (game.isPlayerWhite) "W" else "B",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = game.opponentName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (game.timeCategory != null) {
                        Text(
                            text = game.timeCategory.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!game.opening.isNullOrBlank()) {
                        Text(text = "· ${game.opening}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = game.playerResult.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = resultColor
                )
                Text(
                    text = dateFormat.format(Date(game.playedAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
