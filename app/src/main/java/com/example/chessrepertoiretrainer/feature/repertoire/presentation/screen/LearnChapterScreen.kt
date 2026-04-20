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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.ChessboardUI
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LearnChapterViewModel

@Composable
fun LearnChapterScreen(
    viewModel: LearnChapterViewModel,
    onBackClick: () -> Unit,
    onStartChapterTraining: (chapterId: Int) -> Unit,
    onStartLineTraining: (lineId: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

            uiState.phase == LearnChapterViewModel.LearnPhase.CHAPTER_COMPLETE -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = uiState.statusMessage ?: "You have gone through all lines in this chapter.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartChapterTraining(uiState.chapterId) }, modifier = Modifier.weight(1f)
                            ) {
                                Text("Final chapter training")
                            }
                            OutlinedButton(
                                onClick = onBackClick, modifier = Modifier.weight(1f)
                            ) {
                                Text("Finish")
                            }
                        }
                    }
                }
            }

            else -> {
                // Board in the middle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ChessboardUI(state = chessCtrl)
                }

                // Bottom section: comment box (scrollable) + navigation + train/skip
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Comment box
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(12.dp)
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

                    // Navigation row
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

                    if (uiState.phase == LearnChapterViewModel.LearnPhase.LINE_COMPLETE) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val lineId = uiState.currentLineId
                            Button(
                                onClick = {
                                    if (lineId != null) {
                                        onStartLineTraining(lineId)
                                    }
                                }, modifier = Modifier.weight(1f), enabled = lineId != null
                            ) {
                                Text("Train this line")
                            }
                            TextButton(
                                onClick = { viewModel.skipTrainingForCurrentLine() }, modifier = Modifier.weight(1f)
                            ) {
                                Text("Skip test")
                            }
                        }
                    }
                }
            }
        }
    }
}
