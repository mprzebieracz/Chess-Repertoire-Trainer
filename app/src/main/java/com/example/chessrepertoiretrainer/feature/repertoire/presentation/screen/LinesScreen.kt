package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinesScreen(
    viewModel: LinesViewModel,
    onNavigateToLineEditor: (Int) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToTraining: (Int) -> Unit
) {
    val lines by viewModel.lines.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val context = LocalContext.current

    val pgnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importPgn(context, it) }
    }

    LinesScaffold(
        onBackClick = onBackClick,
        onTrainClick = { onNavigateToTraining(viewModel.chapterId) },
        onImportClick = { pgnLauncher.launch(arrayOf("*/*")) },
        onAddClick = { showDialog = true }
    ) { padding ->
        LinesList(
            lines = lines,
            padding = padding,
            onNavigateToLineEditor = onNavigateToLineEditor,
            onDeleteLine = viewModel::deleteLine
        )
    }

    if (showDialog) {
        AddLineDialog(
            name = name,
            onNameChange = { name = it },
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.addLine(name)
                name = ""
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinesScaffold(
    onBackClick: () -> Unit,
    onTrainClick: () -> Unit,
    onImportClick: () -> Unit,
    onAddClick: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            LinesTopBar(
                onBackClick = onBackClick,
                onTrainClick = onTrainClick,
                onImportClick = onImportClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(AppIcons.AddLine, contentDescription = "Add Line")
            }
        }
    ) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinesTopBar(
    onBackClick: () -> Unit,
    onTrainClick: () -> Unit,
    onImportClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Lines") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(AppIcons.Back, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onTrainClick) {
                Icon(AppIcons.TrainChapter, contentDescription = "Train Chapter")
            }
            IconButton(onClick = onImportClick) {
                Icon(AppIcons.ImportPgn, contentDescription = "Import PGN")
            }
        }
    )
}

@Composable
private fun LinesList(
    lines: List<Line>,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onNavigateToLineEditor: (Int) -> Unit,
    onDeleteLine: (Line) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        items(lines) { line ->
            LineListItem(
                line = line,
                onNavigateToLineEditor = onNavigateToLineEditor,
                onDeleteLine = onDeleteLine
            )
        }
    }
}

@Composable
private fun LineListItem(
    line: Line,
    onNavigateToLineEditor: (Int) -> Unit,
    onDeleteLine: (Line) -> Unit
) {
    ListItem(
        headlineContent = { Text(line.name) },
        supportingContent = {
            Text("New Line")
        },
        trailingContent = {
            IconButton(onClick = { onDeleteLine(line) }) {
                Icon(AppIcons.DeleteLine, contentDescription = "Delete")
            }
        },
        modifier = Modifier.clickable { onNavigateToLineEditor(line.id) }
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLineDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Line") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Line Name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}