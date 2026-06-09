package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesListScreen(
    viewModel: GamesListViewModel,
    onOpenGame: (SavedGame) -> Unit,
    onBackClick: () -> Unit
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Games") }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterPanel(
                filter = filter,
                onFilterChange = viewModel::setFilter,
                onToggleTimeCategory = viewModel::toggleTimeCategory,
                onToggleResult = viewModel::toggleResult
            )

            HorizontalDivider()

            if (games.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No games found", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(games) { game ->
                        GameListCard(game = game, onClick = { onOpenGame(game) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    filter: GameFilter,
    onFilterChange: (GameFilter) -> Unit,
    onToggleTimeCategory: (String) -> Unit,
    onToggleResult: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val activeSummary = buildList {
        filter.platform?.let { add(it.replaceFirstChar { c -> c.uppercase() }) }
        if (filter.isPlayerWhite == true) add("White")
        else if (filter.isPlayerWhite == false) add("Black")
        filter.timeCategories.forEach { add(it.replaceFirstChar { c -> c.uppercase() }) }
        filter.selectedResults.forEach { add(it.replaceFirstChar { c -> c.uppercase() }) }
        if (filter.ratedOnly) add("Rated")
    }.joinToString(" · ")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (activeSummary.isBlank()) "Filters" else "Filters: $activeSummary",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterSection(label = "Platform") {
                    OptionChip("Lichess", filter.platform == "lichess") {
                        onFilterChange(filter.copy(platform = if (filter.platform == "lichess") null else "lichess"))
                    }
                    OptionChip("Chess.com", filter.platform == "chess.com") {
                        onFilterChange(filter.copy(platform = if (filter.platform == "chess.com") null else "chess.com"))
                    }
                }

                FilterSection(label = "Color") {
                    OptionChip("White ♔", filter.isPlayerWhite == true) {
                        onFilterChange(filter.copy(isPlayerWhite = if (filter.isPlayerWhite == true) null else true))
                    }
                    OptionChip("Black ♚", filter.isPlayerWhite == false) {
                        onFilterChange(filter.copy(isPlayerWhite = if (filter.isPlayerWhite == false) null else false))
                    }
                }

                FilterSection(label = "Game type") {
                    OptionChip("Rated", filter.ratedOnly) {
                        onFilterChange(filter.copy(ratedOnly = !filter.ratedOnly))
                    }
                }

                FilterSection(label = "Time control") {
                    listOf("bullet", "blitz", "rapid", "classical").forEach { cat ->
                        OptionChip(
                            label = cat.replaceFirstChar { it.uppercase() },
                            selected = cat in filter.timeCategories,
                            onClick = { onToggleTimeCategory(cat) })
                    }
                }

                FilterSection(label = "Result") {
                    listOf(
                        "win" to "Win",
                        "loss" to "Loss",
                        "draw" to "Draw"
                    ).forEach { (key, label) ->
                        OptionChip(label, key in filter.selectedResults) {
                            onToggleResult(key)
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(88.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private val gameDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
private fun GameListCard(game: SavedGame, onClick: () -> Unit) {
    val resultColor = when (game.playerResult) {
        "win" -> MaterialTheme.colorScheme.primary
        "loss" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (game.isPlayerWhite) "W" else "B",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "vs ${game.opponentName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    game.playerRating?.let {
                        Text("($it)", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (game.timeCategory != null) {
                        Text(
                            text = game.timeCategory.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (!game.opening.isNullOrBlank()) {
                        Text(
                            text = "· ${game.opening}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = game.playerResult.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = resultColor
                )
                Text(
                    text = gameDateFormat.format(Date(game.playedAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}