package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.chess.ui.ChessboardUI
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.ReviewChapterViewModel

/**
 * Simple chapter review screen: lets you step through moves of each line and
 * switch between lines (previous/next) without affecting learning progress.
 */
@Composable
fun ReviewChapterScreen(
    viewModel: ReviewChapterViewModel, onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chessCtrl = viewModel.chessController

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top: back button and basic info
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Back", style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.hasNoLines) {
                Text(
                    text = "No lines in this chapter yet.", style = MaterialTheme.typography.bodyMedium
                )
            }
            else if (!uiState.isLoading && uiState.totalLines > 0) {
                Text(
                    text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                uiState.currentLineName?.let { name ->
                    Text(
                        text = name, style = MaterialTheme.typography.bodyMedium
                    )
                }
                uiState.myColor?.let { color ->
                    Text(
                        text = "You play $color", style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading chapter...", style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.hasNoLines -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.statusMessage ?: "No lines in this chapter. Use edit mode to add lines.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> {
                // Board in the middle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ChessboardUI(state = chessCtrl)
                }

                // Bottom section: comment box + move navigation + line navigation
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Comment / status card
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        ) {
                            uiState.statusMessage?.let { message ->
                                Text(
                                    text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            val comment = uiState.currentMoveComment
                            if (comment.isNullOrBlank()) {
                                Text(
                                    text = "No comment for this move.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else {
                                Text(
                                    text = comment, style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Move navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onPreviousMove() }, modifier = Modifier.weight(1f), enabled = !uiState.isAtLineStart
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = { viewModel.onNextMove() }, modifier = Modifier.weight(1f), enabled = !uiState.isAtLineEnd
                        ) {
                            Text("Next")
                        }

                        OutlinedButton(
                            onClick = { viewModel.restartCurrentLine() }, modifier = Modifier.weight(1f)
                        ) {
                            Text("Restart")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Line navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.goToPreviousLine() }, modifier = Modifier.weight(1f)
                        ) {
                            Text("Previous line")
                        }
                        OutlinedButton(
                            onClick = { viewModel.goToNextLine() }, modifier = Modifier.weight(1f)
                        ) {
                            Text("Next line")
                        }
                    }
                }
            }
        }
    }
}

