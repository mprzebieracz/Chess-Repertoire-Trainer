package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.CategoryStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.StatsTimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatsScreen(viewModel: AccountStatsViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(viewModel.username) }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatsTimeRange.entries) { range ->
                    FilterChip(selected = uiState.selectedTimeRange == range,
                               onClick = { viewModel.setTimeRange(range) },
                               label = { Text(range.label) })
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else {
                val periodLabel = when (uiState.selectedTimeRange) {
                    StatsTimeRange.DAYS_7 -> "last 7 days"
                    StatsTimeRange.DAYS_30 -> "last 30 days"
                    StatsTimeRange.DAYS_90 -> "last 90 days"
                    StatsTimeRange.DAYS_365 -> "last year"
                    StatsTimeRange.ALL_TIME -> "all time"
                }
                Text(text = "${uiState.totalGames} games · $periodLabel",
                     style = MaterialTheme.typography.bodyMedium,
                     modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.categoryStats.isNotEmpty()) {
                    TabRow(selectedTabIndex = selectedCategoryIndex.coerceAtMost(uiState.categoryStats.size - 1)) {
                        uiState.categoryStats.forEachIndexed { index, cs ->
                            Tab(selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                       modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(text = categoryDisplayName(cs.category,
                                                                    viewModel.platform),
                                         style = MaterialTheme.typography.labelMedium)
                                    val displayRating = cs.currentRating ?: cs.peakRating
                                    if (displayRating != null) {
                                        Text(text = displayRating.toString(),
                                             style = MaterialTheme.typography.bodySmall,
                                             fontWeight = FontWeight.Bold)
                                        if (cs.ratingDiff != null && cs.ratingDiff != 0) {
                                            val sign = if (cs.ratingDiff > 0) "+" else ""
                                            Text(text = "$sign${cs.ratingDiff}",
                                                 style = MaterialTheme.typography.labelSmall,
                                                 color = if (cs.ratingDiff > 0) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val currentStats =
                        uiState.categoryStats.getOrNull(selectedCategoryIndex.coerceAtMost(uiState.categoryStats.size - 1))
                    if (currentStats != null) {
                        CategoryStatsContent(stats = currentStats, platform = viewModel.platform)
                    }
                }
            }
        }
    }
}

private fun categoryDisplayName(category: String, platform: String): String =
    if (category == "classical" && platform == "chess.com") "Daily"
    else category.replaceFirstChar { it.uppercase() }

@Composable
private fun CategoryStatsContent(stats: CategoryStats, platform: String) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (stats.allStats.played == 0) {
            Text("No games in this period", style = MaterialTheme.typography.bodyMedium)
        }
        else {
            OverallStatsRow(stats.allStats)
            HorizontalDivider()
            ColorBreakdownTable(stats)
        }

        HorizontalDivider()
        ExtraInfoSection(stats)
    }
}

@Composable
private fun OverallStatsRow(stats: GameStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCell(label = "Played", value = stats.played.toString())
        StatCell(label = "Won",
                 value = "${stats.wins}  (${(stats.winPct * 100).toInt()}%)",
                 color = MaterialTheme.colorScheme.primary)
        StatCell(label = "Lost",
                 value = "${stats.losses}  (${(stats.lossPct * 100).toInt()}%)",
                 color = MaterialTheme.colorScheme.error)
        StatCell(label = "Drawn", value = "${stats.draws}  (${(stats.drawPct * 100).toInt()}%)")
    }
}

@Composable
private fun StatCell(label: String,
                     value: String,
                     color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value,
             style = MaterialTheme.typography.bodyMedium,
             fontWeight = FontWeight.Bold,
             color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ColorBreakdownTable(stats: CategoryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("By color",
             style = MaterialTheme.typography.labelMedium,
             fontWeight = FontWeight.SemiBold)
        ColorRow("All", stats.allStats)
        ColorRow("White ♔", stats.whiteStats)
        ColorRow("Black ♚", stats.blackStats)
    }
}

@Composable
private fun ColorRow(label: String, stats: GameStats) {
    if (stats.played == 0) return
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "${stats.wins}W  ${stats.losses}L  ${stats.draws}D",
             style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "${(stats.winPct * 100).toInt()}%",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.primary,
             fontWeight = FontWeight.SemiBold)
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
private fun ExtraInfoSection(stats: CategoryStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text("Highest rating:  ", style = MaterialTheme.typography.bodySmall)
            if (stats.peakRating != null) {
                Text(stats.peakRating.toString(),
                     style = MaterialTheme.typography.bodySmall,
                     fontWeight = FontWeight.Bold)
                val dateStr = stats.peakRatingDate?.let { dateFormat.format(Date(it)) }
                if (!dateStr.isNullOrBlank()) {
                    Text("  ·  $dateStr", style = MaterialTheme.typography.bodySmall)
                }
            }
            else {
                Text("—", style = MaterialTheme.typography.bodySmall)
            }
        }
        Row {
            Text("Avg opponent:  ", style = MaterialTheme.typography.bodySmall)
            Text(text = stats.avgOpponentRating?.toString() ?: "—",
                 style = MaterialTheme.typography.bodySmall,
                 fontWeight = FontWeight.Bold)
        }
    }
}