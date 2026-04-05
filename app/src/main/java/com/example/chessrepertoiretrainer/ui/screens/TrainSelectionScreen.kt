package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainSelectionScreen(
    viewModel: TrainingSelectionViewModel,
    onStartTraining: (Int) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val repertoires by viewModel.repertoires.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val selectedRepertoireId by viewModel.selectedRepertoireId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training") },
                navigationIcon = if (onBackClick != null) {
                    {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = AppIcons.Back,
                                contentDescription = "Back"
                            )
                        }
                    }
                } else {
                    {}
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Choose repertoire",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(repertoires) { rep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRepertoire(rep.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = rep.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Side: ${rep.color}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (rep.id == selectedRepertoireId) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            Text(
                text = "Choose chapter to train",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(chapters) { chapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartTraining(chapter.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = chapter.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}


