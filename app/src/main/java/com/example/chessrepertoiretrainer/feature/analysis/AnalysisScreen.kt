package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.chess.ui.DeeperButton
import com.example.chessrepertoiretrainer.core.chess.ui.EngineSection
import com.example.chessrepertoiretrainer.core.chess.ui.PgnTextViewer
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel, onBackClick: () -> Unit) {
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        topBar = {
            ChessTopBar(
                title = "Analysis",
                onBackClick = onBackClick,
                actions = {
                    engineAnalysis?.depth?.let { depth ->
                        if (isEngineEnabled) DeeperButton(searchState = engineSearchState,
                                                          depth = depth,
                                                          onClick = viewModel::analyzeDeeper)
                    }
                    EngineToggleButton(isEnabled = isEngineEnabled,
                                       onClick = viewModel::toggleEngine)
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
        contentBar = {
            PgnTextViewer(
                sanHistory = chessCtrl.sanHistory,
                currentMoveIndex = chessCtrl.currentMoveIndex,
                onMoveClick = { chessCtrl.navigateToMoveIndex(it) },
            )
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(AppIcons.FlipBoard, "Flip", { chessCtrl.flipBoard() })
                BottomBarButton(AppIcons.ResetBoard, "Reset", { chessCtrl.resetBoard() })
                BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                "Prev",
                                { chessCtrl.navigateBack() })
                BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                "Next",
                                { chessCtrl.navigateForward() })
            }
        },
    )
}

@Composable
fun EngineToggleButton(isEnabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = AppIcons.Engine,
            contentDescription = if (isEnabled) "Disable engine" else "Enable engine",
            tint = if (isEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}