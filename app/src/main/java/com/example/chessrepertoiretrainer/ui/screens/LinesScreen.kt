package com.example.chessrepertoiretrainer.ui.screens

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
import com.example.chessrepertoiretrainer.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel

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


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lines") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTraining(viewModel.chapterId) }) {
                        Icon(AppIcons.TrainChapter, contentDescription = "Train Chapter")
                    }
                    IconButton(onClick = { pgnLauncher.launch(arrayOf("*/*")) }) {
                        Icon(AppIcons.ImportPgn, contentDescription = "Import PGN")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(AppIcons.AddLine, contentDescription = "Add Line")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(lines) { line ->
                ListItem(
                    headlineContent = { Text(line.name) },
                    supportingContent = {
                        // Opcjonalnie: pokazujemy kiedy następna powtórka
                        Text("New Line")
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteLine(line) }) {
                            Icon(AppIcons.DeleteLine, contentDescription = "Delete")
                        }
                    },
                    modifier = Modifier.clickable { onNavigateToLineEditor(line.id) }
                )
                HorizontalDivider()
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Create New Line") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Line Name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.addLine(name)
                        name = ""
                        showDialog = false
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}