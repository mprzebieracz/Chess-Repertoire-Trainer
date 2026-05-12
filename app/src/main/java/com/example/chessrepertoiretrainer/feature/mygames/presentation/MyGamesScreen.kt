package com.example.chessrepertoiretrainer.feature.mygames.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGamesScreen(
    viewModel: MyGamesViewModel,
    onOpenLichess: (username: String) -> Unit,
    onOpenChessCom: (username: String) -> Unit,
    onOpenGamesList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text("My Games") }, actions = {
            if (uiState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = viewModel::sync) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Sync")
                }
            }
        })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            val hasAnyAccount =
                uiState.lichessUsername.isNotBlank() || uiState.chessComUsername.isNotBlank()

            if (!hasAnyAccount) {
                Text(
                    text = "Add usernames in Settings to view your stats and sync games.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (uiState.lichessUsername.isNotBlank()) {
                AccountCard(
                    platform = "Lichess",
                    username = uiState.lichessUsername,
                    gameCount = uiState.lichessGameCount,
                    lastSyncAt = uiState.lichessLastSyncAt,
                    onClick = { onOpenLichess(uiState.lichessUsername) })
            }

            if (uiState.chessComUsername.isNotBlank()) {
                AccountCard(
                    platform = "Chess.com",
                    username = uiState.chessComUsername,
                    gameCount = uiState.chessComGameCount,
                    lastSyncAt = uiState.chessComLastSyncAt,
                    onClick = { onOpenChessCom(uiState.chessComUsername) })
            }

            if (hasAnyAccount) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = onOpenGamesList, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        AppIcons.MyGames,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Browse all games")
                }
            }

            uiState.syncProgress?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.syncError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    platform: String,
    username: String,
    gameCount: Int,
    lastSyncAt: Long,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    AppIcons.MyGames,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = platform,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (gameCount > 0) "$gameCount games" else "No games",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatLastSync(lastSyncAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatLastSync(timestamp: Long): String {
    if (timestamp == 0L) return "Never synced"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Synced just now"
        diff < 3_600_000L -> "Synced ${diff / 60_000}m ago"
        diff < 86_400_000L -> "Synced ${diff / 3_600_000}h ago"
        diff < 172_800_000L -> "Synced yesterday"
        else -> "Synced ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))}"
    }
}