package com.example.chessrepertoiretrainer.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessScreenLayout
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel

@Composable
fun TrainScreen(
    viewModel: TrainingViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    ChessScreenLayout(
        title = "Train",
        chessCtrl = viewModel.chessController,
        showNavigationControls = false,
        showBoardActionButtons = false,
        topContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (onBackClick != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (uiState.isSessionEmpty) {
                    Text(
                        text = "No lines to train. Create lines in your repertoire first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    val currentLineName = uiState.currentLineName
                    if (currentLineName != null && uiState.totalLines > 0) {
                        Text(
                            text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentLineName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (uiState.myColor != null) {
                        Text(
                            text = "You play ${uiState.myColor}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when {
                    uiState.isSessionComplete -> {
                        Text(
                            text = uiState.statusMessage ?: "Training complete",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    uiState.lastMoveWasCorrect == true -> {
                        Text(
                            text = "Correct move",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    uiState.lastMoveWasCorrect == false -> {
                        Text(
                            text = "Incorrect move",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    !uiState.isSessionEmpty && !uiState.isSessionComplete -> {
                        Text(
                            text = if (uiState.isWaitingForUserMove) {
                                "Your turn: follow the repertoire moves."
                            } else {
                                "Waiting for training session..."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    )
}
