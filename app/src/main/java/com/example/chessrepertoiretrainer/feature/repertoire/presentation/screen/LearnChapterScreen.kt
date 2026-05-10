package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LearnChapterViewModel

@Composable
fun LearnChapterScreen(
    viewModel: LearnChapterViewModel,
    onBackClick: () -> Unit,
    onStartChapterTraining: (chapterId: Int) -> Unit,
    onStartLineTraining: (lineId: Int) -> Unit,
    onOpenInAnalysis: (fen: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController

    val isComplete = uiState.phase == LearnChapterViewModel.LearnPhase.LINE_COMPLETE
    val isChapterDone = uiState.phase == LearnChapterViewModel.LearnPhase.CHAPTER_COMPLETE

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        topBar = {
            ChessTopBar(
                title = uiState.chapterName.ifBlank { "Learn" },
                onBackClick = onBackClick,
                actions = {
                    if (!uiState.isLoading && !uiState.hasNoLines) {
                        TextButton(onClick = { onOpenInAnalysis(chessCtrl.boardState) }) {
                            Text("Analysis")
                        }
                    }
                },
            )
        },
        contentBar = {
            LearnContentArea(
                uiState = uiState,
                onStartChapterTraining = onStartChapterTraining,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            when {
                uiState.isLoading || uiState.hasNoLines || isChapterDone -> {}
                isComplete -> ChessBottomBar {
                    val lineId = uiState.currentLineId
                    BottomBarButton(Icons.Filled.FitnessCenter,
                                    "Train",
                                    { lineId?.let { onStartLineTraining(it) } },
                                    enabled = lineId != null)
                    BottomBarButton(Icons.Filled.SkipNext,
                                    "Skip",
                                    viewModel::skipTrainingForCurrentLine)
                }

                else -> ChessBottomBar {
                    BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    "Prev",
                                    viewModel::onPreviousMove,
                                    enabled = !uiState.isAtLineStart)
                    BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    "Next",
                                    viewModel::onNextMove,
                                    enabled = !uiState.isAtLineEnd)
                }
            }
        },
    )
}

@Composable
private fun LearnContentArea(
    uiState: LearnChapterViewModel.LearnChapterUiState,
    onStartChapterTraining: (chapterId: Int) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when {
            uiState.isLoading -> Text("Loading chapter…",
                                      style = MaterialTheme.typography.bodyMedium)

            uiState.hasNoLines -> Text(
                text = uiState.statusMessage
                    ?: "No lines in this chapter. Use edit mode to add lines.",
                style = MaterialTheme.typography.bodyMedium,
            )

            uiState.phase == LearnChapterViewModel.LearnPhase.CHAPTER_COMPLETE -> {
                Text(
                    text = uiState.statusMessage
                        ?: "You have gone through all lines in this chapter.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onStartChapterTraining(uiState.chapterId) }) { Text("Train Chapter") }
                OutlinedButton(onClick = onBackClick,
                               modifier = Modifier.padding(top = 8.dp)) { Text("Finish") }
            }

            else -> {
                if (uiState.totalLines > 0) {
                    Text(
                        text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                uiState.myColor?.let {
                    Text(text = "You play $it",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.statusMessage?.let {
                            Text(text = it,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                        }
                        val comment = uiState.currentMoveComment
                        if (comment.isNullOrBlank()) {
                            Text("No comment for this move.",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else {
                            Text(text = comment, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (uiState.phase == LearnChapterViewModel.LearnPhase.LINE_COMPLETE) {
                            Spacer(Modifier.height(8.dp))
                            Text("Line complete — train or skip below.",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.primary,
                                 fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}