package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.chessrepertoiretrainer.core.chess.ui.BottomBarButton
import com.example.chessrepertoiretrainer.core.chess.ui.ChessBottomBar
import com.example.chessrepertoiretrainer.core.chess.ui.ChessScreenLayout
import com.example.chessrepertoiretrainer.core.chess.ui.ChessTopBar
import com.example.chessrepertoiretrainer.core.chess.ui.DeeperButton
import com.example.chessrepertoiretrainer.core.chess.ui.EngineSection
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.feature.analysis.EngineToggleButton
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LineEditorViewModel

@Composable
fun LineEditorScreen(viewModel: LineEditorViewModel, onBackClick: () -> Unit) {
    val moves by viewModel.dbMoves.collectAsStateWithLifecycle()
    val editingComment by viewModel.editingComment.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val isEngineEnabled by viewModel.isEngineEnabled.collectAsStateWithLifecycle()
    val engineAnalysis by viewModel.engineAnalysis.collectAsStateWithLifecycle()
    val engineSearchState by viewModel.engineSearchState.collectAsStateWithLifecycle()
    val engineError by viewModel.engineError.collectAsStateWithLifecycle()
    val chessCtrl = viewModel.chessController

    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = hasChanges) { showExitDialog = true }

    ChessScreenLayout(
        chessCtrl = chessCtrl,
        topBar = {
            ChessTopBar(
                title = "Edit Line",
                onBackClick = { if (hasChanges) showExitDialog = true else onBackClick() },
                actions = {
                    engineAnalysis?.depth?.let { depth ->
                        if (isEngineEnabled) DeeperButton(searchState = engineSearchState, depth = depth, onClick = viewModel::analyzeDeeper)
                    }
                    EngineToggleButton(isEnabled = isEngineEnabled, onClick = viewModel::toggleEngine)
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
                    onDeeperClick = viewModel::analyzeDeeper,
                )
            }
        },
        contentBar = {
            LineEditorCommentSection(
                moves = moves,
                boardFen = chessCtrl.boardState,
                editingComment = editingComment,
                onStartEditing = { viewModel.startEditingComment(it) },
                onCommentTextChange = { viewModel.onCommentTextChange(it) },
                onSave = { viewModel.saveComment(it) },
                onCancel = { viewModel.cancelEditingComment() },
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            )
        },
        bottomBar = {
            ChessBottomBar {
                BottomBarButton(Icons.Filled.FirstPage, "Start", { viewModel.resetToStart() })
                BottomBarButton(Icons.Filled.Delete, "Delete", { showDeleteConfirm = true })
                BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", { chessCtrl.navigateBack() })
                BottomBarButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", { chessCtrl.navigateForward() })
            }
        },
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete last move?") },
            text = { Text("This will permanently remove the last move from the line.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; viewModel.deleteLastMove() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave editor?") },
            text = { Text("You have made changes to this line. They are saved automatically — leave anyway?") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onBackClick() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Stay") } },
        )
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
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentMove = moves.firstOrNull { it.fen == boardFen }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (editingComment != null) {
            Text("Comment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(value = editingComment, onValueChange = onCommentTextChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Add a comment for this position…") }, maxLines = 4)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                TextButton(onClick = { onSave(boardFen) }) { Text("Save") }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val comment = currentMove?.comment
                Text(
                    text = if (!comment.isNullOrBlank()) comment else "No comment — tap to add",
                    style = if (!comment.isNullOrBlank()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = if (!comment.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onStartEditing(currentMove?.comment) }, enabled = currentMove != null) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit comment")
                }
            }
        }
    }
}
