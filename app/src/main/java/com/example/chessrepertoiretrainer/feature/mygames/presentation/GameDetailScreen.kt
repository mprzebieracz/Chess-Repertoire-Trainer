package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.chessrepertoiretrainer.core.chess.ui.BoardNavigationControls
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.EnginePanel
import com.example.chessrepertoiretrainer.feature.analysis.EngineToggleButton
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
    onViewInCourse: (chapterId: Int, lineId: Int) -> Unit
) {
    val complianceEnabled by viewModel.complianceEnabled.collectAsStateWithLifecycle()
    val currentAnnotation by viewModel.currentAnnotation.collectAsStateWithLifecycle()
    val isLoadingCompliance by viewModel.isLoadingCompliance.collectAsStateWithLifecycle()
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            Column {
                engineError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                CompliancePanel(
                    enabled = complianceEnabled,
                    annotation = currentAnnotation,
                    isLoading = isLoadingCompliance,
                    onViewInCourse = onViewInCourse
                )
                BoardNavigationControls(
                    onBack = { viewModel.chessController.navigateBack() },
                    onForward = { viewModel.chessController.navigateForward() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChessScreenLayout(
                title = "Game",
                chessCtrl = viewModel.chessController,
                showNavigationControls = false,
                showBoardActionButtons = false,
                evaluationBarFraction = if (isEngineEnabled) engineAnalysis?.evaluationBarFraction else null,
                titleEndContent = {
                    EngineToggleButton(isEnabled = isEngineEnabled, onClick = viewModel::toggleEngine)
                },
                topContent = {
                    GameDetailHeader(
                        viewModel = viewModel,
                        complianceEnabled = complianceEnabled,
                        onBackClick = onBackClick,
                        onToggleCompliance = viewModel::toggleCompliance
                    )
                },
                midContent = {
                    if (isEngineEnabled) {
                        EnginePanel(
                            analysis = engineAnalysis,
                            searchState = engineSearchState,
                            onDeeperClick = viewModel::analyzeDeeper
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun GameDetailHeader(
    viewModel: GameDetailViewModel,
    complianceEnabled: Boolean,
    onBackClick: () -> Unit,
    onToggleCompliance: () -> Unit
) {
    val game = viewModel.game
    val resultColor = when (game.playerResult) {
        "win" -> MaterialTheme.colorScheme.primary
        "loss" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = game.opponentName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = game.playerResult.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = resultColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${if (game.isPlayerWhite) "White" else "Black"} · ${game.platform.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (game.timeCategory != null) {
                        Text(
                            text = "· ${game.timeCategory.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "· ${dateFormat.format(Date(game.playedAt))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!game.opening.isNullOrBlank()) {
                    Text(text = game.opening, style = MaterialTheme.typography.bodySmall)
                }
            }
            FilterChip(
                selected = complianceEnabled,
                onClick = onToggleCompliance,
                label = { Text("Repertoire") }
            )
        }
    }
}

@Composable
private fun CompliancePanel(
    enabled: Boolean,
    annotation: MoveAnnotation?,
    isLoading: Boolean,
    onViewInCourse: (chapterId: Int, lineId: Int) -> Unit
) {
    // Always reserve the same height so nav buttons never shift
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!enabled) return@Row

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
                    text = "✓ ${annotation.playedSan} — in repertoire",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (annotation.chapterIdForNavigation != null && annotation.lineIdForNavigation != null) {
                    TextButton(
                        onClick = { onViewInCourse(annotation.chapterIdForNavigation, annotation.lineIdForNavigation) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("View →", style = MaterialTheme.typography.bodySmall) }
                }
            }
            ComplianceStatus.DEVIATION -> {
                Text(
                    text = "✗ ${annotation.playedSan} — deviation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (annotation.chapterIdForNavigation != null && annotation.lineIdForNavigation != null) {
                    TextButton(
                        onClick = { onViewInCourse(annotation.chapterIdForNavigation, annotation.lineIdForNavigation) },
                        modifier = Modifier.height(28.dp)
                    ) { Text("View →", style = MaterialTheme.typography.bodySmall) }
                }
            }
            ComplianceStatus.OUT_OF_BOOK -> Text(
                text = "○ Out of book",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            ComplianceStatus.OPPONENT_IN_BOOK -> Text(
                text = "Opponent followed expected lines",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            ComplianceStatus.OPPONENT_DEVIATION -> Text(
                text = "Opponent deviated — ${annotation.playedSan}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
