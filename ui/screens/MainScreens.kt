package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessrepertoiretrainer.ui.components.chess.ChessViewModel
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen() {
    val chessViewModel: ChessViewModel = viewModel()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Analysis") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // PGN / Move History Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 80.dp, max = 150.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (chessViewModel.pgnState.isEmpty()) "Make moves to see analysis..." else chessViewModel.pgnState,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (chessViewModel.pgnState.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box(modifier = Modifier.padding(16.dp)) {
                ChessboardUI(state = chessViewModel)
            }
            
            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { chessViewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(32.dp))
                IconButton(onClick = { chessViewModel.navigateForward() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Forward")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { chessViewModel.resetBoard() }, modifier = Modifier.weight(1f)) {
                    Text("Reset")
                }
                Button(onClick = { chessViewModel.flipBoard() }, modifier = Modifier.weight(1f)) {
                    Text("Flip")
                }
            }
        }
    }
}
