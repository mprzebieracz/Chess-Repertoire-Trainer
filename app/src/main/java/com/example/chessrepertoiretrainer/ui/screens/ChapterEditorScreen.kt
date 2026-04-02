package com.example.chessrepertoiretrainer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.ui.components.chess.ChessboardUI
import com.example.chessrepertoiretrainer.ui.viewmodels.ChapterEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditorScreen(
    viewModel: ChapterEditorViewModel,
    onBackClick: () -> Unit,
    onTrainClick: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPgn(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapter Editor") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = onTrainClick, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Train")
                    }
                    IconButton(onClick = { launcher.launch("*/*") }) {
                        Icon(Icons.Default.Menu, contentDescription = "Import PGN")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ChessboardUI(state = viewModel)
            }
            
            // Move tree visualization could go here
            Text(
                "PGN Import Button in Top Bar. Tap to select file.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
