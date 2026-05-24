package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.chess.ui.DeeperButton
import com.example.chessrepertoiretrainer.core.chess.ui.EngineSection
import com.example.chessrepertoiretrainer.core.chess.ui.EngineToggleButton
import com.example.chessrepertoiretrainer.core.chess.ui.PgnTextViewer
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    onViewInCourse: (chapterId: Int, lineId: Int) -> Unit,
) {
    val currentAnnotation by viewModel.currentAnnotation.collectAsStateWithLifecycle()
    val isLoadingCompliance by viewModel.isLoadingCompliance.collectAsStateWithLifecycle()
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    val moveEvals by viewModel.moveEvals.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController
    val game = viewModel.game

    val resultColor = when (game.playerResult) {
        "win" -> MaterialTheme.colorScheme.primary
        "loss" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        annotations = viewModel.annotations,
        topBar = {
            ChessTopBar(
                title = "${game.opponentName} — ${game.playerResult.replaceFirstChar { it.uppercase() }}",
                onBackClick = onBackClick,
                actions = {
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
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (moveEvals.isEmpty()) {
                        IconButton(onClick = viewModel::startAnalysis) {
                            Icon(Icons.Filled.Analytics, contentDescription = "Analyze game")
                        }
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            if (isEngineEnabled) {
                EngineSection(
                    analysis = engineAnalysis,
                    searchState = engineSearchState,
                    isFlipped = chessCtrl.isFlipped
                )
            }
        },
        contentBar = {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            game.opponentName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            game.playerResult.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = resultColor
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${if (game.isPlayerWhite) "White" else "Black"} · ${game.platform.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        game.timeCategory?.let {
                            Text(
                                "· ${it.replaceFirstChar { c -> c.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "· ${dateFormat.format(Date(game.playedAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    game.opening?.takeIf { it.isNotBlank() }
                        ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                CompliancePanel(
                    annotation = currentAnnotation,
                    isLoading = isLoadingCompliance,
                    onViewInCourse = onViewInCourse,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                PgnTextViewer(
                    navigator = viewModel.navigator,
                    onMoveClick = { viewModel.navigator.goTo(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "Prev",
                    { viewModel.navigator.goPrevious() })
                BottomBarButton(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Next",
                    { viewModel.navigator.goNext() })
            }
        },
    )
}

@Composable
private fun CompliancePanel(
    annotation: MoveAnnotation?,
    isLoading: Boolean,
    onViewInCourse: (chapterId: Int, lineId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Text("Building index…", style = MaterialTheme.typography.bodySmall)
            return@Row
        }
        when (annotation?.status) {
            null -> Text(
                "Navigate to see repertoire compliance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            ComplianceStatus.IN_BOOK -> {
                Text(
                    "✓ ${annotation.playedSan} — in repertoire",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (annotation.chapterIdForNavigation != null && annotation.lineIdForNavigation != null) {
                    TextButton(onClick = {
                        onViewInCourse(
                            annotation.chapterIdForNavigation,
                            annotation.lineIdForNavigation
                        )
                    }, modifier = Modifier.height(28.dp)) {
                        Text("View →", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            ComplianceStatus.DEVIATION -> {
                Text(
                    "✗ ${annotation.playedSan} — deviation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (annotation.chapterIdForNavigation != null && annotation.lineIdForNavigation != null) {
                    TextButton(onClick = {
                        onViewInCourse(
                            annotation.chapterIdForNavigation,
                            annotation.lineIdForNavigation
                        )
                    }, modifier = Modifier.height(28.dp)) {
                        Text("View →", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            ComplianceStatus.OUT_OF_BOOK -> Text(
                "○ Out of book",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            ComplianceStatus.OPPONENT_IN_BOOK -> Text(
                "Opponent followed expected lines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            ComplianceStatus.OPPONENT_DEVIATION -> Text(
                "Opponent deviated — ${annotation.playedSan}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
