package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.EditChapterViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChapterScreen(viewModel: EditChapterViewModel,
                      onNavigateToLineEditor: (lineId: Int) -> Unit,
                      onBackClick: () -> Unit) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    var localLines by remember(lines) { mutableStateOf(lines) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localLines = localLines.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    var showDialog by remember { mutableStateOf(false) }
    var newLineName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val pgnLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importPgn(context, it) }
        }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Edit Chapter") }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(AppIcons.Back, contentDescription = "Back")
            }
        }, actions = {
            IconButton(onClick = { pgnLauncher.launch(arrayOf("*/*")) }) {
                Icon(AppIcons.ImportPgn, contentDescription = "Import PGN")
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showDialog = true }) {
            Icon(AppIcons.AddLine, contentDescription = "Add Line")
        }
    }) { padding ->
        LazyColumn(state = lazyListState, modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            items(localLines, key = { it.id }) { line ->
                ReorderableItem(reorderState, key = line.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp,
                                                      label = "elevation")
                    Surface(shadowElevation = elevation) {
                        ListItem(modifier = Modifier.clickable { onNavigateToLineEditor(line.id) },
                                 leadingContent = {
                                     Icon(Icons.Default.Menu,
                                          contentDescription = "Drag to reorder",
                                          modifier = Modifier.draggableHandle(onDragStopped = {
                                              viewModel.persistLineOrder(localLines)
                                          }),
                                          tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                 },
                                 headlineContent = { Text(line.name) },
                                 trailingContent = {
                                     IconButton(onClick = { viewModel.deleteLine(line) }) {
                                         Icon(Icons.Default.Delete, contentDescription = "Delete")
                                     }
                                 })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false; newLineName = "" },
                    title = { Text("Create New Line") },
                    text = {
                        OutlinedTextField(value = newLineName,
                                          onValueChange = { newLineName = it },
                                          label = { Text("Line Name (optional)") },
                                          modifier = Modifier.fillMaxWidth())
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.addLine(newLineName)
                            newLineName = ""
                            showDialog = false
                        }) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false; newLineName = ""
                        }) { Text("Cancel") }
                    })
    }
}