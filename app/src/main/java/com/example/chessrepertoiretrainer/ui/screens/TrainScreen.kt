package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.Repertoire
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScreen(
    viewModel: RepertoireViewModel,
    onRepertoireClick: (Int) -> Unit
) {
    val repertoires by viewModel.repertoires.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose Repertoire to Train") }) }
    ) { padding ->
        if (repertoires.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No repertoires found. Go to Repertoire tab to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(repertoires) { repertoire ->
                    RepertoireTrainItem(repertoire) {
                        onRepertoireClick(repertoire.id)
                    }
                }
            }
        }
    }
}

@Composable
fun RepertoireTrainItem(repertoire: Repertoire, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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
                Text(text = "Color: ${repertoire.color}", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Start Training")
        }
    }
}
