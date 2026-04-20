package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.ChaptersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    viewModel: ChaptersViewModel,
    onNavigateToLines: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    ChaptersScaffold(
        onBackClick = onBackClick,
        onAddClick = { showDialog = true }
    ) { padding ->
        ChaptersList(
            chapters = chapters,
            padding = padding,
            onNavigateToLines = onNavigateToLines,
            onDeleteChapter = { viewModel.deleteChapter(it) }
        )
    }

    if (showDialog) {
        AddChapterDialog(
            name = name,
            onNameChange = { name = it },
            onDismiss = { showDialog = false },
            onConfirm = {
                if (name.isNotBlank()) {
                    viewModel.addChapter(name)
                    name = ""
                    showDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChaptersScaffold(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            ChaptersTopBar(onBackClick = onBackClick)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
            }
        }
    ) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChaptersTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Chapters") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
private fun ChaptersList(
    chapters: List<Chapter>,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onNavigateToLines: (Int) -> Unit,
    onDeleteChapter: (Chapter) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        items(chapters) { chapter ->
            ChapterListItem(
                chapter = chapter,
                onNavigateToLines = onNavigateToLines,
                onDeleteChapter = onDeleteChapter
            )
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    onNavigateToLines: (Int) -> Unit,
    onDeleteChapter: (Chapter) -> Unit
) {
    ListItem(
        headlineContent = { Text(chapter.name) },
        trailingContent = {
            IconButton(onClick = { onDeleteChapter(chapter) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        modifier = Modifier.clickable { onNavigateToLines(chapter.id) }
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChapterDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Chapter Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
