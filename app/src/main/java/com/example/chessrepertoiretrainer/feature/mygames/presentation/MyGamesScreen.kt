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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGamesScreen(viewModel: MyGamesViewModel,
                  onOpenLichess: (username: String) -> Unit,
                  onOpenChessCom: (username: String) -> Unit,
                  onOpenGamesList: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text("My Games") }, actions = {
            if (uiState.isSyncing) {
                CircularProgressIndicator(modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp))
            }
            else {
                IconButton(onClick = viewModel::sync) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Sync")
                }
            }
        })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
               horizontalAlignment = Alignment.CenterHorizontally) {
            val hasAnyAccount =
                uiState.lichessUsername.isNotBlank() || uiState.chessComUsername.isNotBlank()

            if (!hasAnyAccount) {
                Text(text = "Add usernames in Settings to view your stats",
                     style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.lichessUsername.isNotBlank()) {
                AccountButton(platform = "Lichess",
                              username = uiState.lichessUsername,
                              gameCount = uiState.lichessGameCount,
                              lastSyncAt = uiState.lichessLastSyncAt,
                              onClick = { onOpenLichess(uiState.lichessUsername) })
            }

            if (uiState.chessComUsername.isNotBlank()) {
                AccountButton(platform = "Chess.com",
                              username = uiState.chessComUsername,
                              gameCount = uiState.chessComGameCount,
                              lastSyncAt = uiState.chessComLastSyncAt,
                              onClick = { onOpenChessCom(uiState.chessComUsername) })
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = onOpenGamesList, modifier = Modifier.fillMaxWidth()) {
                Text("Browse games")
            }


            uiState.syncProgress?.let {
                Text(it,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.primary)
            }
            uiState.syncError?.let {
                Text(it,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AccountButton(platform: String,
                          username: String,
                          gameCount: Int,
                          lastSyncAt: Long,
                          onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(text = platform, style = MaterialTheme.typography.labelMedium)
                Text(text = username,
                     style = MaterialTheme.typography.bodyMedium,
                     fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (gameCount > 0) "$gameCount games" else "No games synced",
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                Text(text = formatLastSync(lastSyncAt),
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
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