package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.chess.ui.BoardBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit
) {
    Scaffold(
        bottomBar = { BoardBottomBar(chessCtrl = viewModel.chessController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChessScreenLayout(
                title = "Game",
                chessCtrl = viewModel.chessController,
                showNavigationControls = true,
                showBoardActionButtons = false,
                topContent = {
                    GameDetailHeader(
                        viewModel = viewModel,
                        onBackClick = onBackClick
                    )
                }
            )
        }
    }
}

@Composable
private fun GameDetailHeader(viewModel: GameDetailViewModel, onBackClick: () -> Unit) {
    val game = viewModel.game
    val resultColor = when (game.playerResult) {
        "win" -> MaterialTheme.colorScheme.primary
        "loss" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = game.opponentName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = game.playerResult.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = resultColor
                    )
                }
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${if (game.isPlayerWhite) "White" else "Black"} · ${game.platform.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (game.timeCategory != null) {
                        Text(
                            text = "· ${game.timeCategory.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "· ${dateFormat.format(Date(game.playedAt))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!game.opening.isNullOrBlank()) {
                    Text(text = game.opening, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
