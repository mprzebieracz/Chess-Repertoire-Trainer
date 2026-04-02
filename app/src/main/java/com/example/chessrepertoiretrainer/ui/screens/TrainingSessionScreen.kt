package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingEvent
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionScreen(
    viewModel: TrainingViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val errorAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TrainingEvent.Mistake -> {
                    scope.launch {
                        errorAlpha.animateTo(0.4f, animationSpec = tween(100, easing = LinearEasing))
                        errorAlpha.animateTo(0f, animationSpec = tween(300, easing = LinearEasing))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapter Training") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress
                Text(
                    text = "Line ${uiState.completedLinesCount + 1} of ${uiState.totalLines}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                LinearProgressIndicator(
                    progress = if (uiState.totalLines > 0) 
                        (uiState.completedLinesCount.toFloat() / uiState.totalLines.toFloat()) 
                        else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chessboard
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ChessboardUI(state = viewModel)
                    
                    // Error flash overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Red.copy(alpha = errorAlpha.value))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            uiState.status.contains("Correct") -> Color(0xFFE8F5E9)
                            uiState.status.contains("Mistake") -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.status,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                uiState.status.contains("Correct") -> Color(0xFF2E7D32)
                                uiState.status.contains("Mistake") -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Completion Overlay
            AnimatedVisibility(visible = uiState.isComplete) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "Congratulations!",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "You have completed all lines in this chapter.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(onClick = onBackClick) {
                            Text("Finish Training")
                        }
                    }
                }
            }
        }
    }
}
