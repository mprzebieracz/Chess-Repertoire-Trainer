package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.Repertoire
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepertoireScreen(
    viewModel: RepertoireViewModel,
    onRepertoireClick: (Int) -> Unit
) {
    val repertoires by viewModel.repertoires.collectAsState()
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("White") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Add New Repertoire", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Repertoire Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            FilterChip(
                selected = selectedColor == "White",
                onClick = { selectedColor = "White" },
                label = { Text("White") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = selectedColor == "Black",
                onClick = { selectedColor = "Black" },
                label = { Text("Black") }
            )
        }

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.addRepertoire(name, selectedColor)
                    name = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Repertoire")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Your Repertoires", style = MaterialTheme.typography.headlineSmall)
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(repertoires) { repertoire ->
                RepertoireItem(
                    repertoire = repertoire,
                    onDelete = { viewModel.deleteRepertoire(repertoire) },
                    onClick = { onRepertoireClick(repertoire.id) }
                )
            }
        }
    }
}

@Composable
fun RepertoireItem(repertoire: Repertoire, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = repertoire.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Color: ${repertoire.color}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
