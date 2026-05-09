package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.ui.ChessUiConstants
import com.example.chessrepertoiretrainer.core.chess.ui.ChessboardUI
import com.example.chessrepertoiretrainer.core.chess.ui.EnginePanel
import com.example.chessrepertoiretrainer.core.chess.ui.EvaluationBar
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState
import com.example.chessrepertoiretrainer.feature.analysis.EngineToggleButton
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.ReviewChapterViewModel

@Composable
fun ReviewChapterScreen(
    viewModel: ReviewChapterViewModel, onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    ReviewChapterScaffold(
        uiState = uiState,
        chessCtrl = viewModel.chessController,
        isEngineEnabled = isEngineEnabled,
        engineAnalysis = engineAnalysis,
        engineSearchState = engineSearchState,
        engineError = engineError,
        onBackClick = onBackClick,
        onPreviousMove = viewModel::onPreviousMove,
        onNextMove = viewModel::onNextMove,
        onRestartCurrentLine = viewModel::restartCurrentLine,
        onGoToPreviousLine = viewModel::goToPreviousLine,
        onGoToNextLine = viewModel::goToNextLine,
        onToggleEngine = viewModel::toggleEngine,
        onDeeperClick = viewModel::analyzeDeeper
    )
}

@Composable
private fun ReviewChapterScaffold(
    uiState: ReviewChapterViewModel.UiState,
    chessCtrl: ChessBoardController,
    isEngineEnabled: Boolean,
    engineAnalysis: EngineAnalysis?,
    engineSearchState: EngineSearchState,
    engineError: String?,
    onBackClick: () -> Unit,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onRestartCurrentLine: () -> Unit,
    onGoToPreviousLine: () -> Unit,
    onGoToNextLine: () -> Unit,
    onToggleEngine: () -> Unit,
    onDeeperClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            if (!uiState.isLoading && !uiState.hasNoLines) {
                Column {
                    engineError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    ReviewMoveBottomBar(
                        uiState = uiState,
                        onPreviousMove = onPreviousMove,
                        onNextMove = onNextMove,
                        onRestartCurrentLine = onRestartCurrentLine
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ReviewChapterTopSection(
                uiState = uiState,
                isEngineEnabled = isEngineEnabled,
                onBackClick = onBackClick,
                onGoToPreviousLine = onGoToPreviousLine,
                onGoToNextLine = onGoToNextLine,
                onToggleEngine = onToggleEngine
            )

            when {
                uiState.isLoading -> ReviewChapterLoadingState()
                uiState.hasNoLines -> ReviewChapterEmptyState(uiState = uiState)
                else -> ReviewChapterBody(
                    uiState = uiState,
                    chessCtrl = chessCtrl,
                    isEngineEnabled = isEngineEnabled,
                    engineAnalysis = engineAnalysis,
                    engineSearchState = engineSearchState,
                    onDeeperClick = onDeeperClick
                )
            }
        }
    }
}

@Composable
private fun ReviewChapterTopSection(
    uiState: ReviewChapterViewModel.UiState,
    isEngineEnabled: Boolean,
    onBackClick: () -> Unit,
    onGoToPreviousLine: () -> Unit,
    onGoToNextLine: () -> Unit,
    onToggleEngine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text(text = "Back", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.weight(1f))

            // Line navigation
            IconButton(onClick = onGoToPreviousLine) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous line")
            }
            Text(
                text = if (uiState.totalLines > 0) "${uiState.currentLineNumber}/${uiState.totalLines}" else "",
                style = MaterialTheme.typography.bodySmall
            )
            IconButton(onClick = onGoToNextLine) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next line")
            }

            // Engine toggle — top right
            EngineToggleButton(isEnabled = isEngineEnabled, onClick = onToggleEngine)
        }

        ReviewChapterHeader(uiState = uiState)
    }
}

@Composable
private fun ReviewChapterHeader(uiState: ReviewChapterViewModel.UiState) {
    if (uiState.hasNoLines) {
        Text("No lines in this chapter yet.", style = MaterialTheme.typography.bodyMedium)
    } else if (!uiState.isLoading) {
        uiState.currentLineName?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        uiState.myColor?.let {
            Text(text = "You play $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewChapterLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading chapter...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReviewChapterEmptyState(uiState: ReviewChapterViewModel.UiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = uiState.statusMessage ?: "No lines in this chapter. Use edit mode to add lines.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ReviewChapterBody(
    uiState: ReviewChapterViewModel.UiState,
    chessCtrl: ChessBoardController,
    isEngineEnabled: Boolean = false,
    engineAnalysis: EngineAnalysis? = null,
    engineSearchState: EngineSearchState = EngineSearchState.IDLE,
    onDeeperClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isEngineEnabled) {
            EnginePanel(
                analysis = engineAnalysis,
                searchState = engineSearchState,
                onDeeperClick = onDeeperClick
            )
        }

        val boardPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        val barFraction = if (isEngineEnabled) (engineAnalysis?.evaluationBarFraction ?: 0.5f) else null

        if (barFraction != null) {
            BoxWithConstraints(modifier = boardPadding.fillMaxWidth()) {
                val boardSize = maxWidth - ChessUiConstants.ScreenChrome.EvalBar.width - ChessUiConstants.ScreenChrome.EvalBar.spacing
                Row(
                    modifier = Modifier.fillMaxWidth().height(boardSize),
                    verticalAlignment = Alignment.Top
                ) {
                    EvaluationBar(
                        fraction = barFraction,
                        isFlipped = chessCtrl.isFlipped,
                        modifier = Modifier
                            .width(ChessUiConstants.ScreenChrome.EvalBar.width)
                            .fillMaxHeight()
                    )
                    Spacer(Modifier.width(ChessUiConstants.ScreenChrome.EvalBar.spacing))
                    Box(modifier = Modifier.weight(1f)) {
                        ChessboardUI(state = chessCtrl)
                    }
                }
            }
        } else {
            Box(modifier = boardPadding) {
                ChessboardUI(state = chessCtrl)
            }
        }
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)) {
            ReviewChapterCommentCard(uiState = uiState)
        }
    }
}

@Composable
private fun ReviewMoveBottomBar(
    uiState: ReviewChapterViewModel.UiState,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onRestartCurrentLine: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onPreviousMove, enabled = !uiState.isAtLineStart) { Text("Back") }
        Button(onClick = onNextMove, enabled = !uiState.isAtLineEnd) { Text("Next") }
        OutlinedButton(onClick = onRestartCurrentLine) { Text("Restart") }
    }
}

@Composable
private fun ReviewChapterCommentCard(uiState: ReviewChapterViewModel.UiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)) {
            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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
            } else {
                Text(text = comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
