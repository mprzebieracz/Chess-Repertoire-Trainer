package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.chess.ui.BoardNavigationControls
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LineEditorViewModel

@Composable
fun LineEditorScreen(
    viewModel: LineEditorViewModel,
    onBackClick: () -> Unit
) {
    val moves by viewModel.dbMoves.collectAsStateWithLifecycle()
    val editingComment by viewModel.editingComment.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) {
        showExitDialog = true
    }

    Scaffold(
        bottomBar = {
            LineEditorBottomBar(
                chessCtrl = viewModel.chessController,
                onResetToStart = { viewModel.resetToStart() },
                onDeleteLast = { showDeleteConfirm = true }
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
                    LineEditorTopContent(onBackClick = {
                        if (hasChanges) showExitDialog = true else onBackClick()
                    })
                },
                bottomContent = {
                    LineEditorCommentSection(
                        moves = moves,
                        boardFen = viewModel.chessController.boardState,
                        editingComment = editingComment,
                        onStartEditing = { viewModel.startEditingComment(it) },
                        onCommentTextChange = { viewModel.onCommentTextChange(it) },
                        onSave = { viewModel.saveComment(it) },
                        onCancel = { viewModel.cancelEditingComment() }
                    )
                }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete last move?") },
            text = { Text("This will permanently remove the last move from the line.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; viewModel.deleteLastMove() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave editor?") },
            text = { Text("You have made changes to this line. They are saved automatically — leave anyway?") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBackClick() }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Stay") }
            }
        )
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
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "Back to lines", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LineEditorBottomBar(
    chessCtrl: com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController,
    onResetToStart: () -> Unit,
    onDeleteLast: () -> Unit
) {
    Column {
        BoardNavigationControls(
            onBack = { chessCtrl.navigateBack() },
            onForward = { chessCtrl.navigateForward() }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onResetToStart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { Text("To Start") }

            Button(
                onClick = onDeleteLast,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete last") }
        }
    }
}

@Composable
private fun LineEditorCommentSection(
    moves: List<LineMove>,
    boardFen: String,
    editingComment: String?,
    onStartEditing: (currentComment: String?) -> Unit,
    onCommentTextChange: (String) -> Unit,
    onSave: (fen: String) -> Unit,
    onCancel: () -> Unit
) {
    val currentMove = moves.firstOrNull { it.fen == boardFen }

    if (editingComment != null) {
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
            OutlinedTextField(
                value = editingComment,
                onValueChange = onCommentTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a comment for this position…") },
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                TextButton(onClick = { onSave(boardFen) }) { Text("Save") }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val comment = currentMove?.comment
            Text(
                text = if (!comment.isNullOrBlank()) comment else "No comment — tap to add",
                style = if (!comment.isNullOrBlank()) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.bodySmall,
                color = if (!comment.isNullOrBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onStartEditing(currentMove?.comment) },
                enabled = currentMove != null
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit comment")
            }
        }
    }
}
