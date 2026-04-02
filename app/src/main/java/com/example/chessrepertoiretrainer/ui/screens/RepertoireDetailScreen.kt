package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.Chapter
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoireDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoireDetailScreen(
    viewModel: RepertoireDetailViewModel,
    onChapterClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapters") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
            }
        }
    ) { padding ->
        if (chapters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No chapters yet. Click + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(chapters) { chapter ->
                    ChapterItem(chapter) { onChapterClick(chapter.id) }
                }
            }
        }

        if (showAddDialog) {
            AddChapterDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    viewModel.addChapter(name)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ChapterItem(chapter: Chapter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(text = chapter.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AddChapterDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Chapter") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Chapter Name") }
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
