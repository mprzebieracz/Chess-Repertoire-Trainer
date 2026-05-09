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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.ui.ChessboardUI
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LearnChapterViewModel

@Composable
fun LearnChapterScreen(viewModel: LearnChapterViewModel,
                       onBackClick: () -> Unit,
                       onStartChapterTraining: (chapterId: Int) -> Unit,
                       onStartLineTraining: (lineId: Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LearnChapterScaffold(uiState = uiState,
                         chessCtrl = viewModel.chessController,
                         onBackClick = onBackClick,
                         onStartChapterTraining = onStartChapterTraining,
                         onStartLineTraining = onStartLineTraining,
                         onPreviousMove = viewModel::onPreviousMove,
                         onNextMove = viewModel::onNextMove,
                         onSkipTrainingForCurrentLine = viewModel::skipTrainingForCurrentLine)
}

@Composable
private fun LearnChapterScaffold(uiState: LearnChapterViewModel.LearnChapterUiState,
                                 chessCtrl: ChessBoardController,
                                 onBackClick: () -> Unit,
                                 onStartChapterTraining: (chapterId: Int) -> Unit,
                                 onStartLineTraining: (lineId: Int) -> Unit,
                                 onPreviousMove: () -> Unit,
                                 onNextMove: () -> Unit,
                                 onSkipTrainingForCurrentLine: () -> Unit) {
    Scaffold(bottomBar = {
        if (!uiState.isLoading && !uiState.hasNoLines && uiState.phase != LearnChapterViewModel.LearnPhase.CHAPTER_COMPLETE) {
            LearnChapterBottomBar(uiState = uiState,
                                  onStartLineTraining = onStartLineTraining,
                                  onPreviousMove = onPreviousMove,
                                  onNextMove = onNextMove,
                                  onSkipTrainingForCurrentLine = onSkipTrainingForCurrentLine)
        }
    }) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            LearnChapterTopSection(uiState = uiState, onBackClick = onBackClick)

            when {
                uiState.isLoading -> LearnChapterLoadingState()
                uiState.hasNoLines -> LearnChapterEmptyState(uiState = uiState)
                uiState.phase == LearnChapterViewModel.LearnPhase.CHAPTER_COMPLETE -> LearnChapterCompleteState(
                    uiState = uiState,
                    onStartChapterTraining = onStartChapterTraining,
                    onBackClick = onBackClick)

                else -> LearnChapterPlayState(uiState = uiState,
                                              chessCtrl = chessCtrl,
                                              onStartLineTraining = onStartLineTraining)
            }
        }
    }
}

@Composable
private fun LearnChapterTopSection(uiState: LearnChapterViewModel.LearnChapterUiState,
                                   onBackClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)) {
        LearnChapterBackRow(onBackClick = onBackClick)
        LearnChapterHeader(uiState = uiState)
    }
}

@Composable
private fun LearnChapterBackRow(onBackClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "Back", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LearnChapterHeader(uiState: LearnChapterViewModel.LearnChapterUiState) {
    if (uiState.hasNoLines) {
        Text("No lines in this chapter yet.", style = MaterialTheme.typography.bodyMedium)
    }
    else if (!uiState.isLoading && uiState.totalLines > 0) {
        Text(text = "Line ${uiState.currentLineNumber} of ${uiState.totalLines}",
             style = MaterialTheme.typography.bodyMedium,
             fontWeight = FontWeight.SemiBold)
        uiState.currentLineName?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        uiState.myColor?.let {
            Text(text = "You play $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LearnChapterLoadingState() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text("Loading chapter...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LearnChapterEmptyState(uiState: LearnChapterViewModel.LearnChapterUiState) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text = uiState.statusMessage
            ?: "No lines in this chapter. Use edit mode to add lines.",
             style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LearnChapterCompleteState(uiState: LearnChapterViewModel.LearnChapterUiState,
                                      onStartChapterTraining: (chapterId: Int) -> Unit,
                                      onBackClick: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            Text(text = uiState.statusMessage ?: "You have gone through all lines in this chapter.",
                 style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onStartChapterTraining(uiState.chapterId) }) { Text("Final chapter training") }
                OutlinedButton(onClick = onBackClick) { Text("Finish") }
            }
        }
    }
}

@Composable
private fun LearnChapterPlayState(uiState: LearnChapterViewModel.LearnChapterUiState,
                                  chessCtrl: ChessBoardController,
                                  onStartLineTraining: (lineId: Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ChessboardUI(state = chessCtrl)
        LearnChapterBottomSection(uiState = uiState, onStartLineTraining = onStartLineTraining)
    }
}

@Composable
private fun LearnChapterBottomSection(uiState: LearnChapterViewModel.LearnChapterUiState,
                                      onStartLineTraining: (lineId: Int) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        LearnChapterCommentCard(uiState = uiState)
        if (uiState.phase == LearnChapterViewModel.LearnPhase.LINE_COMPLETE && uiState.currentLineId != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = "Line complete - use bottom actions to train this line or skip test.",
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LearnChapterBottomBar(uiState: LearnChapterViewModel.LearnChapterUiState,
                                  onStartLineTraining: (lineId: Int) -> Unit,
                                  onPreviousMove: () -> Unit,
                                  onNextMove: () -> Unit,
                                  onSkipTrainingForCurrentLine: () -> Unit) {
    val isComplete = uiState.phase == LearnChapterViewModel.LearnPhase.LINE_COMPLETE
    val lineId = uiState.currentLineId

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPreviousMove,
                       enabled = !uiState.isAtLineStart,
                       modifier = Modifier.weight(1f)) { Text("Back") }

        Button(onClick = onNextMove,
               enabled = !uiState.isAtLineEnd && !isComplete,
               modifier = Modifier.weight(1f)) { Text("Next") }

        // Third slot: empty until line complete, then shows train/skip
        Box(modifier = Modifier.weight(1f)) {
            if (isComplete) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp),
                       horizontalAlignment = Alignment.CenterHorizontally,
                       modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { if (lineId != null) onStartLineTraining(lineId) },
                           enabled = lineId != null,
                           modifier = Modifier.fillMaxWidth()) { Text("Train", maxLines = 1) }
                    TextButton(onClick = onSkipTrainingForCurrentLine,
                               modifier = Modifier.fillMaxWidth()) { Text("Skip", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun LearnChapterCommentCard(uiState: LearnChapterViewModel.LearnChapterUiState) {
    Card(modifier = Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)) {
            uiState.statusMessage?.let {
                Text(text = it,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }

            val comment = uiState.currentMoveComment
            if (comment.isNullOrBlank()) {
                Text(text = "No comment for this move.",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else {
                Text(text = comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LearnChapterMoveNavigationRow(uiState: LearnChapterViewModel.LearnChapterUiState,
                                          onPreviousMove: () -> Unit,
                                          onNextMove: () -> Unit,
                                          onRestartCurrentLine: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPreviousMove, enabled = !uiState.isAtLineStart) { Text("Back") }
        Button(onClick = onNextMove, enabled = !uiState.isAtLineEnd) { Text("Next") }
        OutlinedButton(onClick = onRestartCurrentLine) { Text("Restart") }
    }
}

@Composable
private fun LearnChapterLineActionsRow(uiState: LearnChapterViewModel.LearnChapterUiState,
                                       onStartLineTraining: (lineId: Int) -> Unit,
                                       onSkipTrainingForCurrentLine: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val lineId = uiState.currentLineId
        Button(onClick = { if (lineId != null) onStartLineTraining(lineId) },
               enabled = lineId != null) { Text("Train this line") }
        TextButton(onClick = onSkipTrainingForCurrentLine) { Text("Skip test") }
    }
}