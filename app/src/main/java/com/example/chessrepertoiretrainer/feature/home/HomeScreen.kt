package com.example.chessrepertoiretrainer.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAnalysis: () -> Unit,
    onOpenMyStats: () -> Unit
) {
    HomeScaffold {
        HomeContent(
            onOpenAnalysis = onOpenAnalysis,
            onOpenMyStats = onOpenMyStats
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(content: @Composable () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun HomeContent(
    onOpenAnalysis: () -> Unit,
    onOpenMyStats: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HomeTitle()
        HomeActionButtons(
            onOpenAnalysis = onOpenAnalysis,
            onOpenMyStats = onOpenMyStats
        )
    }
}

@Composable
private fun HomeTitle() {
    Text("Welcome to Chess Repertoire Trainer")
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun HomeActionButtons(
    onOpenAnalysis: () -> Unit,
    onOpenMyStats: () -> Unit
) {
    Button(onClick = onOpenAnalysis) {
        Text("Open analysis board")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onOpenMyStats) {
        Text("My stats")
    }
}
