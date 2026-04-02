package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.Line
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinesScreen(
    viewModel: LinesViewModel,
    onLineClick: (Int) -> Unit,
    onTrainChapterClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val lines by viewModel.lines.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lines") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (lines.isNotEmpty()) {
                        Button(
                            onClick = { onTrainChapterClick(viewModel.chapterId) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Train Chapter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Line")
            }
        }
    ) { padding ->
        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No lines yet. Click + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(lines) { line ->
                    LineItem(line) { onLineClick(line.id) }
                }
            }
        }

        if (showAddDialog) {
            AddLineDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    viewModel.addLine(name)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun LineItem(line: Line, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(text = line.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AddLineDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Line") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Line Name (Optional)") },
                placeholder = { Text("Leave blank for default") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
