package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.database.entities.Repertoire
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoiresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoiresScreen(
    viewModel: RepertoiresViewModel,
    onNavigateToChapters: (Int) -> Unit
) {
    val repertoires by viewModel.repertoires.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Repertoires") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Repertoire")
            }
        }
    ) { paddingValues ->
        if (repertoires.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No repertoires found. Click + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(repertoires) { repertoire ->
                    RepertoireItem(
                        repertoire = repertoire,
                        onClick = { onNavigateToChapters(repertoire.id) },
                        onDelete = { viewModel.deleteRepertoire(repertoire) }
                    )
                }
            }
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
}

@Composable
fun RepertoireItem(repertoire: Repertoire, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = repertoire.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "Side: ${repertoire.color}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepertoireDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("White") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Repertoire") },
        text = {
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
                        label = { Text("White") }
                    )
                    FilterChip(
                        selected = selectedColor == "Black",
                        onClick = { selectedColor = "Black" },
                        label = { Text("Black") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) }
            ) {
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
