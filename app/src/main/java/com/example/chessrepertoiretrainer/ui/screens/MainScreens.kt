package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.background
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

@Composable
fun HomeScreen() { CenterText("Home Screen") }

@Composable
fun TrainScreen() { CenterText("Train Screen") }

@Composable
fun YourGamesScreen() { CenterText("Your Games Screen") }

@Composable
fun AnalysisScreen() {
    val chessViewModel: ChessViewModel = viewModel()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Chess Analysis", 
            fontSize = 24.sp, 
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        // PGN / Move History Display - Scrollable and potentially expanding
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 80.dp, max = 200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (chessViewModel.pgnState.isEmpty()) "Start making moves to see analysis..." else chessViewModel.pgnState,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = if (chessViewModel.pgnState.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Box(modifier = Modifier.padding(16.dp)) {
            ChessboardUI(state = chessViewModel)
        }
        
        // Navigation Buttons (Back/Forward)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { chessViewModel.navigateBack() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Move Back",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            
            IconButton(
                onClick = { chessViewModel.navigateForward() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Move Forward",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { chessViewModel.resetBoard() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("Reset Board")
            }
            Button(
                onClick = { chessViewModel.flipBoard() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Flip Board")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BluetoothScreen() { CenterText("Bluetooth Screen") }

@Composable
fun SettingsScreen() { CenterText("Settings Screen") }

@Composable
fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 24.sp)
    }
}
