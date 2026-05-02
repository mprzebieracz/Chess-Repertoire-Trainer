package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
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

    TrainSelectionScaffold(onBackClick = onBackClick) { padding ->
        TrainSelectionContent(
            padding = padding,
            repertoires = repertoires,
            chapters = chapters,
            selectedRepertoireId = selectedRepertoireId,
            onSelectRepertoire = viewModel::selectRepertoire,
            onStartTraining = onStartTraining
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainSelectionScaffold(
    onBackClick: (() -> Unit)?,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TrainSelectionTopBar(onBackClick = onBackClick)
        }) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainSelectionTopBar(onBackClick: (() -> Unit)?) {
    TopAppBar(
        title = { Text("Training") }, navigationIcon = if (onBackClick != null) {
        {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
    else {
        {}
    })
}

@Composable
private fun TrainSelectionContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    repertoires: List<Repertoire>,
    chapters: List<Chapter>,
    selectedRepertoireId: Int?,
    onSelectRepertoire: (Int) -> Unit,
    onStartTraining: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        SectionHeader(text = "Choose repertoire")

        TrainSelectionRepertoireList(
            repertoires = repertoires,
            selectedRepertoireId = selectedRepertoireId,
            onSelectRepertoire = onSelectRepertoire,
            modifier = Modifier.weight(1f)
        )

        SectionHeader(text = "Choose chapter to train")

        TrainSelectionChapterList(
            chapters = chapters, onStartTraining = onStartTraining, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TrainSelectionRepertoireList(
    repertoires: List<Repertoire>,
    selectedRepertoireId: Int?,
    onSelectRepertoire: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(repertoires) { repertoire ->
            TrainSelectionRepertoireItem(
                repertoire = repertoire,
                selected = repertoire.id == selectedRepertoireId,
                onClick = { onSelectRepertoire(repertoire.id) })
        }
    }
}

@Composable
private fun TrainSelectionRepertoireItem(
    repertoire: Repertoire, selected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = repertoire.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Side: ${repertoire.color}", style = MaterialTheme.typography.bodySmall
            )
        }

        if (selected) {
            SelectedLabel()
        }
    }
    HorizontalDivider()
}

@Composable
private fun SelectedLabel() {
    Text(
        text = "Selected",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TrainSelectionChapterList(
    chapters: List<Chapter>, onStartTraining: (Int) -> Unit, modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(chapters) { chapter ->
            TrainSelectionChapterItem(
                chapter = chapter, onClick = { onStartTraining(chapter.id) })
        }
    }
}

@Composable
private fun TrainSelectionChapterItem(
    chapter: Chapter, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = chapter.name, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider()
}

