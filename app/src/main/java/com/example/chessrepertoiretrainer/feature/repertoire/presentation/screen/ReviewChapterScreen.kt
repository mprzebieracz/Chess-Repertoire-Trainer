package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.chess.ui.ContentBarScaffold
import com.example.chessrepertoiretrainer.core.chess.ui.DeeperButton
import com.example.chessrepertoiretrainer.core.chess.ui.EmptyStatePlaceholder
import com.example.chessrepertoiretrainer.core.chess.ui.EngineSection
import com.example.chessrepertoiretrainer.core.chess.ui.EngineToggleButton
import com.example.chessrepertoiretrainer.core.chess.ui.LineProgressHeader
import com.example.chessrepertoiretrainer.core.chess.ui.LoadingPlaceholder
import com.example.chessrepertoiretrainer.core.chess.ui.MoveCommentCard
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.ReviewChapterViewModel

@Composable
fun ReviewChapterScreen(
    viewModel: ReviewChapterViewModel,
    onBackClick: () -> Unit,
    onFindMasterGames: (fen: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        annotations = viewModel.annotations,
        topBar = {
            ChessTopBar(
                title = uiState.chapterName.ifBlank { "Review" },
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = viewModel::goToPreviousLine) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous line"
                        )
                    }
                    Text(
                        text = if (uiState.totalLines > 0) "${uiState.currentLineNumber}/${uiState.totalLines}" else "",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(onClick = viewModel::goToNextLine) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next line"
                        )
                    }
                    engineAnalysis?.depth?.let { depth ->
                        if (isEngineEnabled) DeeperButton(
                            searchState = engineSearchState,
                            depth = depth,
                            onClick = viewModel::analyzeDeeper
                        )
                    }
                    EngineToggleButton(
                        isEnabled = isEngineEnabled,
                        onClick = viewModel::toggleEngine
                    )
                    IconButton(onClick = { onFindMasterGames(chessCtrl.getBoard().fen) }) {
                        Icon(Icons.Filled.ManageSearch, contentDescription = "Find master games")
                    }
                },
            )
        },
        engineSection = {
            if (!engineError.isNullOrBlank()) {
                Text(
                    text = engineError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            if (isEngineEnabled) {
                EngineSection(
                    analysis = engineAnalysis,
                    searchState = engineSearchState,
                    isFlipped = chessCtrl.isFlipped,
                )
            }
        },
        contentBar = { ReviewContentArea(uiState = uiState) },
        bottomBar = {
            if (!uiState.isLoading && !uiState.hasNoLines) {
                ChessBottomBar {
                    BottomBarButton(Icons.Filled.Refresh, "Restart", viewModel::restartCurrentLine)
                    BottomBarButton(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Prev",
                        viewModel::onPreviousMove
                    )
                    BottomBarButton(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Next",
                        viewModel::onNextMove
                    )
                }
            }
        },
    )
}

@Composable
private fun ReviewContentArea(uiState: ReviewChapterViewModel.ReviewChapterUiState) {
    ContentBarScaffold {
        when {
            uiState.isLoading -> LoadingPlaceholder("Loading chapter…")

            uiState.hasNoLines -> EmptyStatePlaceholder(
                uiState.statusMessage ?: "No lines in this chapter yet."
            )

            else -> {
                if (uiState.totalLines > 0) {
                    LineProgressHeader(lineName = uiState.currentLineName)
                }
                val label = uiState.currentMoveLabel
                val comment = uiState.currentMoveComment
                if (!label.isNullOrBlank() || !comment.isNullOrBlank()) {
                    MoveCommentCard {
                        Text(
                            text = buildAnnotatedString {
                                if (!label.isNullOrBlank()) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                        append(label)
                                    }
                                }
                                if (!comment.isNullOrBlank()) {
                                    if (!label.isNullOrBlank()) append("  ")
                                    append(comment)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                uiState.statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}