package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.CourseProgress
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.RepertoiresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoiresScreen(
    viewModel: RepertoiresViewModel,
    onNavigateToChapters: (Int) -> Unit
) {
    val courses by viewModel.courses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    RepertoiresScaffold(
        onAddClick = { showAddDialog = true }
    ) { paddingValues ->
        RepertoiresContent(
            courses = courses,
            paddingValues = paddingValues,
            onNavigateToChapters = onNavigateToChapters,
            onDeleteCourse = { viewModel.deleteRepertoire(it) }
        )
    }

    if (showAddDialog) {
        AddRepertoireDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.addRepertoire(name, color)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepertoiresScaffold(
    onAddClick: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Courses") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(AppIcons.AddRepertoire, contentDescription = "Add course")
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
private fun RepertoiresContent(
    courses: List<CourseProgress>,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onNavigateToChapters: (Int) -> Unit,
    onDeleteCourse: (com.example.chessrepertoiretrainer.core.database.entity.Repertoire) -> Unit
) {
    if (courses.isEmpty()) {
        RepertoiresEmptyState(paddingValues = paddingValues)
    } else {
        RepertoiresList(
            courses = courses,
            paddingValues = paddingValues,
            onNavigateToChapters = onNavigateToChapters,
            onDeleteCourse = onDeleteCourse
        )
    }
}

@Composable
private fun RepertoiresEmptyState(paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text("No courses yet. Tap + to add one.")
    }
}

@Composable
private fun RepertoiresList(
    courses: List<CourseProgress>,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onNavigateToChapters: (Int) -> Unit,
    onDeleteCourse: (com.example.chessrepertoiretrainer.core.database.entity.Repertoire) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        items(courses) { course ->
            RepertoireCard(
                course = course,
                onClick = { onNavigateToChapters(course.repertoire.id) },
                onDelete = { onDeleteCourse(course.repertoire) }
            )
        }
    }
}

@Composable
private fun RepertoireCard(
    course: CourseProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RepertoireCardContent(course = course)
            RepertoireDeleteButton(onDelete = onDelete)
        }
    }
}

@Composable
private fun RowScope.RepertoireCardContent(course: CourseProgress) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = course.repertoire.name,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Side: ${course.repertoire.color}",
            style = MaterialTheme.typography.bodyMedium
        )

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
            Text(
                text = "No lines yet",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RepertoireDeleteButton(onDelete: () -> Unit) {
    IconButton(onClick = onDelete) {
        Icon(
            AppIcons.DeleteRepertoire,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRepertoireDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("White") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Repertoire") },
        text = {
            AddRepertoireDialogContent(
                name = name,
                selectedColor = selectedColor,
                onNameChange = { name = it },
                onSelectedColorChange = { selectedColor = it }
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) }) {
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

@Composable
private fun AddRepertoireDialogContent(
    name: String,
    selectedColor: String,
    onNameChange: (String) -> Unit,
    onSelectedColorChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
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
            ColorChip(
                label = "White",
                selected = selectedColor == "White",
                onClick = { onSelectedColorChange("White") }
            )
            ColorChip(
                label = "Black",
                selected = selectedColor == "Black",
                onClick = { onSelectedColorChange("Black") }
            )
        }
    }
}

@Composable
private fun ColorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
