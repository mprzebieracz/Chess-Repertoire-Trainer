package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.data.RepertoireMove
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI
import com.example.chessrepertoiretrainer.ui.components.chess.RepertoireEditorViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepertoireEditorScreen(viewModel: RepertoireEditorViewModel, onBackClick: () -> Unit) {
    val currentPath by viewModel.currentPath.collectAsState()
    val availableMoves by viewModel.availableMoves.collectAsState()
    val moveTree by viewModel.moveTree.collectAsState()
    val selectedMoveId = currentPath.lastOrNull()?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edytor Repertuaru") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    if (selectedMoveId != null) {
                        IconButton(onClick = { viewModel.deleteCurrentMove() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń ruch", tint = MaterialTheme.colorScheme.error)
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
            // Chessboard
            Box(modifier = Modifier.padding(16.dp)) {
                ChessboardUI(state = viewModel)
            }

            // Interactive PGN-style Move List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Twoja linia:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Start button
                        PgnMoveChip(
                            text = "Start",
                            isSelected = selectedMoveId == null,
                            onClick = { viewModel.selectMove(null) }
                        )

                        currentPath.forEachIndexed { index, move ->
                            if (index % 2 == 0) {
                                Text(
                                    text = "${(index / 2) + 1}.",
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            PgnMoveChip(
                                text = move.moveSan,
                                isSelected = move.id == selectedMoveId,
                                onClick = { viewModel.selectMove(move) }
                            )
                        }
                    }
                }
            }

            // Variations / Next Moves
            if (availableMoves.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Alternatywy / Kontynuacje:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start).padding(horizontal = 16.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableMoves.forEach { move ->
                        SuggestionChip(
                            onClick = { viewModel.selectMove(move) },
                            label = { Text(move.moveSan) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PgnMoveChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
