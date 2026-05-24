package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
fun RepertoirePuzzlesScreen(
    viewModel: RepertoirePuzzlesViewModel,
    onBackClick: () -> Unit,
    onStartSession: (List<String>) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCount = uiState.openings.count { it.isSelected }
    val unsolvedInSelection = viewModel.totalUnsolvedInSelection()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opening Puzzles") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = viewModel::manualRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh openings")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.openings.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { onStartSession(viewModel.selectedFamilies()) },
                        enabled = selectedCount > 0 && unsolvedInSelection > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (selectedCount == 0) {
                            Text("Select openings to train")
                        } else if (unsolvedInSelection == 0) {
                            Text("No unsolved puzzles in selection")
                        } else {
                            Text("Train selected ($unsolvedInSelection puzzle${if (unsolvedInSelection != 1) "s" else ""})")
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.openings.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Text(
                        "No openings found in your repertoire.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Add some lines to your repertoire first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.openings, key = { it.family }) { item ->
                    OpeningRow(
                        item = item,
                        onToggle = { viewModel.toggleSelection(item.family) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun OpeningRow(
    item: RepertoirePuzzlesViewModel.OpeningUiItem,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.family,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                item.eco?.let { eco ->
                    Badge { Text(eco, style = MaterialTheme.typography.labelSmall) }
                }
            }
            Text(
                text = when (item.unsolvedCount) {
                    0 -> "No unsolved puzzles"
                    1 -> "1 puzzle available"
                    else -> "${item.unsolvedCount} puzzles available"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (item.unsolvedCount > 0)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.error,
            )
        }
    }
}
