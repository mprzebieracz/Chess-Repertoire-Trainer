package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BoardBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LineEditorViewModel

@Composable
fun LineEditorScreen(
    viewModel: LineEditorViewModel,
    onBackClick: () -> Unit
) {
    val moves by viewModel.dbMoves.collectAsStateWithLifecycle()

    LineEditorScaffold(
        viewModel = viewModel,
        moves = moves,
        onBackClick = onBackClick
    )
}

@Composable
private fun LineEditorScaffold(
    viewModel: LineEditorViewModel,
    moves: List<LineMove>,
    onBackClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            LineEditorBottomBar(
                viewModel = viewModel
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChessScreenLayout(
                title = "Edit Line",
                chessCtrl = viewModel.chessController,
                showNavigationControls = false,
                showBoardActionButtons = false,
                topContent = {
                    LineEditorTopContent(onBackClick = onBackClick)
                },
                bottomContent = {
                    LineEditorCommentSection(
                        moves = moves,
                        boardFen = viewModel.chessController.boardState
                    )
                }
            )
        }
    }
}

@Composable
private fun LineEditorTopContent(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LineEditorBackButton(onBackClick = onBackClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Back to lines",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LineEditorBackButton(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}

@Composable
private fun LineEditorCommentSection(
    moves: List<LineMove>,
    boardFen: String
) {
    val currentComment = moves.firstOrNull { it.fen == boardFen }?.comment

    if (!currentComment.isNullOrBlank()) {
        LineEditorCommentCard(comment = currentComment)
    }
}

@Composable
private fun LineEditorCommentCard(comment: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Comment",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun LineEditorBottomBar(viewModel: LineEditorViewModel) {
    BoardBottomBar(chessCtrl = viewModel.chessController) {
        LineEditorUndoButton(onUndo = viewModel::undoDbMove)
    }
}

@Composable
private fun RowScope.LineEditorUndoButton(onUndo: () -> Unit) {
    Button(
        onClick = onUndo,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Undo",
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text("Undo")
    }
}