package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.CourseProgress
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.RepertoiresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoiresScreen(viewModel: RepertoiresViewModel, onNavigateToChapters: (Int) -> Unit) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val isRebuildingIndex by viewModel.isRebuildingIndex.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        RepertoiresTopBar(
            isRebuildingIndex = isRebuildingIndex,
            onRebuildIndex = viewModel::rebuildComplianceIndex
        )
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showAddDialog = true }) {
            Icon(AppIcons.AddRepertoire, contentDescription = "Add course")
        }
    }) { paddingValues ->
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No courses yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(courses) { course ->
                    RepertoireCard(
                        course = course,
                        onClick = { onNavigateToChapters(course.repertoire.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddRepertoireDialog(onDismiss = { showAddDialog = false }, onConfirm = { name, color ->
            viewModel.addRepertoire(name, color)
            showAddDialog = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepertoiresTopBar(isRebuildingIndex: Boolean, onRebuildIndex: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(title = { Text("Your Courses") }, actions = {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(text = {
                if (isRebuildingIndex) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Rebuilding…")
                    }
                } else {
                    Text("Rebuild Compliance Index")
                }
            }, onClick = {
                menuExpanded = false
                onRebuildIndex()
            }, enabled = !isRebuildingIndex)
        }
    })
}

@Composable
private fun RepertoireCard(course: CourseProgress, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        RepertoireCardContent(
            course = course, modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun RepertoireCardContent(course: CourseProgress, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = course.repertoire.name, style = MaterialTheme.typography.titleLarge)
            ColorBadge(color = course.repertoire.color)
        }

        val total = course.totalLines
        val learned = course.learnedLines
        if (total > 0) {
            val percent = (learned * 100 / total)
            Text(
                text = "$learned / $total lines learned ($percent%)",
                style = MaterialTheme.typography.bodySmall
            )
            LinearProgressIndicator(
                progress = { course.learnedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        } else {
            Text(text = "No lines yet", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ColorBadge(color: String) {
    val isWhite = color.equals("White", ignoreCase = true)
    val bgColor = if (isWhite) Color(0xFFF5F5F5) else Color(0xFF212121)
    val textColor = if (isWhite) Color(0xFF212121) else Color(0xFFF5F5F5)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center
    ) {
        Text(text = color, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRepertoireDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("White") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Repertoire") }, text = {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Repertoire Name (e.g. Sicilian)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Side:", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedColor == "White",
                    onClick = { selectedColor = "White" },
                    label = { Text("White") })
                FilterChip(
                    selected = selectedColor == "Black",
                    onClick = { selectedColor = "Black" },
                    label = { Text("Black") })
            }
        }
    }, confirmButton = {
        Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) }) {
            Text("Add")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancel") }
    })
}