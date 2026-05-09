package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.EditCourseViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseScreen(viewModel: EditCourseViewModel,
                     onNavigateToEditChapter: (chapterId: Int) -> Unit,
                     onCourseDeleted: () -> Unit,
                     onBackClick: () -> Unit) {
    val repertoire by viewModel.repertoire.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val renamingChapterId by viewModel.renamingChapterId.collectAsStateWithLifecycle()

    var localChapters by remember(chapters) { mutableStateOf(chapters) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localChapters = localChapters.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newChapterName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameRepertoire by remember { mutableStateOf(false) }
    var newRepertoireName by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Edit Course") }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }, actions = {
            IconButton(onClick = {
                newRepertoireName = repertoire?.name ?: ""
                showRenameRepertoire = true
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Rename course")
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete course")
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add Chapter")
        }
    }) { padding ->
        LazyColumn(state = lazyListState, modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            items(localChapters, key = { it.id }) { chapter ->
                ReorderableItem(reorderState, key = chapter.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp,
                                                      label = "elevation")
                    Surface(shadowElevation = elevation) {
                        if (renamingChapterId == chapter.id) {
                            RenamingChapterItem(chapter = chapter, onConfirm = { name ->
                                viewModel.renameChapter(chapter, name)
                            }, onCancel = { viewModel.cancelRename() })
                        }
                        else {
                            ListItem(modifier = Modifier.clickable { onNavigateToEditChapter(chapter.id) },
                                     leadingContent = {
                                         Icon(Icons.Default.Menu,
                                              contentDescription = "Drag to reorder",
                                              modifier = Modifier.draggableHandle(onDragStopped = {
                                                  viewModel.persistChapterOrder(localChapters)
                                              }),
                                              tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                     },
                                     headlineContent = { Text(chapter.name) },
                                     trailingContent = {
                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                             IconButton(onClick = { viewModel.startRename(chapter.id) }) {
                                                 Icon(Icons.Default.Edit,
                                                      contentDescription = "Rename")
                                             }
                                             IconButton(onClick = { viewModel.deleteChapter(chapter) }) {
                                                 Icon(Icons.Default.Delete,
                                                      contentDescription = "Delete")
                                             }
                                         }
                                     })
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false; newChapterName = "" },
                    title = { Text("Add Chapter") },
                    text = {
                        OutlinedTextField(value = newChapterName,
                                          onValueChange = { newChapterName = it },
                                          label = { Text("Chapter Name") },
                                          modifier = Modifier.fillMaxWidth())
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newChapterName.isNotBlank()) {
                                viewModel.addChapter(newChapterName)
                                newChapterName = ""
                                showAddDialog = false
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false; newChapterName = "" }) {
                            Text("Cancel")
                        }
                    })
    }

    if (showRenameRepertoire) {
        AlertDialog(onDismissRequest = { showRenameRepertoire = false },
                    title = { Text("Rename Course") },
                    text = {
                        OutlinedTextField(value = newRepertoireName,
                                          onValueChange = { newRepertoireName = it },
                                          label = { Text("Course Name") },
                                          modifier = Modifier.fillMaxWidth(),
                                          singleLine = true)
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newRepertoireName.isNotBlank()) {
                                viewModel.renameRepertoire(newRepertoireName)
                                showRenameRepertoire = false
                            }
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameRepertoire = false }) { Text("Cancel") }
                    })
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Course") },
                    text = { Text("Delete \"${repertoire?.name}\" and all its chapters and lines? This cannot be undone.") },
                    confirmButton = {
                        Button(onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteRepertoire(onDeleted = onCourseDeleted)
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                    })
    }
}

@Composable
private fun RenamingChapterItem(chapter: Chapter,
                                onConfirm: (String) -> Unit,
                                onCancel: () -> Unit) {
    var text by remember(chapter.id) { mutableStateOf(chapter.name) }
    ListItem(headlineContent = {
        OutlinedTextField(value = text,
                          onValueChange = { text = it },
                          modifier = Modifier.fillMaxWidth(),
                          singleLine = true)
    }, trailingContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Save") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    })
}