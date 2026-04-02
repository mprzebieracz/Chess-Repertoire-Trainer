package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI
import com.example.chessrepertoiretrainer.ui.viewmodels.LineEditorViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LineEditorScreen(
    viewModel: LineEditorViewModel,
    onBackClick: () -> Unit
) {
    val moves by viewModel.moves.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Line Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (moves.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteLastMove() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete last move", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ChessboardUI(state = viewModel)
            }

            // Linear Move List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Moves:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        moves.forEachIndexed { index, move ->
                            if (index % 2 == 0) {
                                Text(
                                    text = "${(index / 2) + 1}.",
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = move.moveSan,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
