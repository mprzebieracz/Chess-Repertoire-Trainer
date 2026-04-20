package com.example.chessrepertoiretrainer.feature.analysis

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.chess.ui.BoardBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel, onBackClick: () -> Unit
) {
    AnalysisScaffold(
        viewModel = viewModel,
        onBackClick = onBackClick
    )
}

@Composable
private fun AnalysisScaffold(
    viewModel: AnalysisViewModel,
    onBackClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BoardBottomBar(chessCtrl = viewModel.chessController)
        }) { padding ->
        AnalysisBoardContent(
            modifier = Modifier.padding(padding),
            viewModel = viewModel,
            onBackClick = onBackClick
        )
    }
}

@Composable
private fun AnalysisBoardContent(
    modifier: Modifier,
    viewModel: AnalysisViewModel,
    onBackClick: () -> Unit
) {
    Column(modifier = modifier) {
        ChessScreenLayout(
            title = "Analysis",
            chessCtrl = viewModel.chessController,
            showNavigationControls = false,
            showBoardActionButtons = false,
            topContent = {
                AnalysisTopContent(onBackClick = onBackClick)
            }
        )
    }
}

@Composable
private fun AnalysisTopContent(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnalysisBackButton(onBackClick = onBackClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Back",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AnalysisBackButton(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}