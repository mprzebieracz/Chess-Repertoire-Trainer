package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingSelectionViewModel

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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Training") }, navigationIcon = if (onBackClick != null) {
                {
                    IconButton(onClick = onBackClick) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                }
            }
            else {
                {}
            })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            SectionBanner(text = "Choose repertoire", icon = {
                Icon(
                    AppIcons.Repertoire,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            })
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(repertoires) { repertoire ->
                    RepertoireItem(
                        repertoire = repertoire,
                        selected = repertoire.id == selectedRepertoireId,
                        onClick = { viewModel.selectRepertoire(repertoire.id) })
                }
            }

            HorizontalDivider()

            SectionBanner(text = "Choose chapter to train", icon = {
                Icon(
                    AppIcons.Train,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            })
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(chapters) { chapter ->
                    ChapterItem(chapter = chapter, onClick = { onStartTraining(chapter.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionBanner(text: String, icon: @Composable () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RepertoireItem(repertoire: Repertoire, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = repertoire.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "Side: ${repertoire.color}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (selected) {
            Text(
                text = "Selected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ChapterItem(chapter: Chapter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = chapter.name, style = MaterialTheme.typography.bodyLarge)
        Icon(
            AppIcons.Train,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}