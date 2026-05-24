package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun MasterGameViewerScreen(
    viewModel: MasterGameViewerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChessScreenLayout(
        chessCtrl = viewModel.chessController,
        topBar = { ChessTopBar(title = "Master Game", onBackClick = onBackClick) },
        contentBar = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.currentMoveLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } ?: Text(
                    text = "Starting position",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(
                    icon = AppIcons.Back,
                    label = "Prev",
                    onClick = viewModel::onPrevious,
                    enabled = !uiState.isAtStart
                )
                BottomBarButton(
                    icon = AppIcons.SkipLine,
                    label = "Next",
                    onClick = viewModel::onNext,
                    enabled = !uiState.isAtEnd
                )
            }
        }
    )
}
