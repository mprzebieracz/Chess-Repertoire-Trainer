package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ChapterStats
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.CourseOverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseOverviewScreen(
    viewModel: CourseOverviewViewModel,
    onBackClick: () -> Unit,
    onEditCourse: (repertoireId: Int) -> Unit,
    onOpenChapterLearn: (chapterId: Int) -> Unit,
    onOpenChapterTrain: (chapterId: Int) -> Unit,
    onOpenChapterReview: (chapterId: Int) -> Unit,
    onStartMultiChapterTraining: (List<Int>) -> Unit,
) {
    val repertoire by viewModel.repertoire.collectAsStateWithLifecycle()
    val chaptersWithStats by viewModel.chaptersWithStats.collectAsStateWithLifecycle()
    val showChapterSelection by viewModel.showChapterSelection.collectAsStateWithLifecycle()
    val selectedChapterIds by viewModel.selectedChapterIds.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text(repertoire?.name ?: "Course") }, navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(AppIcons.Back, contentDescription = "Back")
            }
        }, actions = {
            val repId = repertoire?.id
            if (repId != null) {
                IconButton(onClick = { viewModel.openChapterSelection() }) {
                    Icon(AppIcons.TrainCourse, contentDescription = "Train selection")
                }
                IconButton(onClick = { onEditCourse(repId) }) {
                    Icon(AppIcons.EditCourse, contentDescription = "Edit course")
                }
            }
        })
    }) { padding ->
        CourseOverviewContent(
            padding = padding,
            chaptersWithStats = chaptersWithStats,
            onOpenChapterLearn = onOpenChapterLearn,
            onOpenChapterTrain = onOpenChapterTrain,
            onOpenChapterReview = onOpenChapterReview
        )
    }

    if (showChapterSelection) {
        ChapterSelectionBottomSheet(
            chaptersWithStats = chaptersWithStats,
            selectedChapterIds = selectedChapterIds,
            onToggle = { viewModel.toggleChapterSelection(it) },
            onConfirm = {
                val ids = selectedChapterIds.toList()
                viewModel.dismissChapterSelection()
                if (ids.isNotEmpty()) onStartMultiChapterTraining(ids)
            },
            onDismiss = { viewModel.dismissChapterSelection() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelectionBottomSheet(
    chaptersWithStats: List<CourseOverviewViewModel.ChapterWithStats>,
    selectedChapterIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Select chapters to train",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn {
            items(chaptersWithStats) { item ->
                ListItem(headlineContent = { Text(item.chapter.name) }, leadingContent = {
                    Checkbox(
                        checked = item.chapter.id in selectedChapterIds,
                        onCheckedChange = { onToggle(item.chapter.id) })
                })
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = onConfirm,
                enabled = selectedChapterIds.isNotEmpty(),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Train Selected")
            }
        }
    }
}

@Composable
private fun CourseOverviewContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    chaptersWithStats: List<CourseOverviewViewModel.ChapterWithStats>,
    onOpenChapterLearn: (Int) -> Unit,
    onOpenChapterTrain: (Int) -> Unit,
    onOpenChapterReview: (Int) -> Unit
) {
    if (chaptersWithStats.isEmpty()) {
        CourseOverviewEmptyState(padding = padding)
    }
    else {
        CourseOverviewList(
            padding = padding,
            chaptersWithStats = chaptersWithStats,
            onOpenChapterLearn = onOpenChapterLearn,
            onOpenChapterTrain = onOpenChapterTrain,
            onOpenChapterReview = onOpenChapterReview
        )
    }
}

@Composable
private fun CourseOverviewEmptyState(padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "No chapters yet.", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Use the edit button to add chapters and lines.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CourseOverviewList(
    padding: androidx.compose.foundation.layout.PaddingValues,
    chaptersWithStats: List<CourseOverviewViewModel.ChapterWithStats>,
    onOpenChapterLearn: (Int) -> Unit,
    onOpenChapterTrain: (Int) -> Unit,
    onOpenChapterReview: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        items(chaptersWithStats) { item ->
            CourseChapterCard(
                item = item,
                onOpenChapterLearn = onOpenChapterLearn,
                onOpenChapterTrain = onOpenChapterTrain,
                onOpenChapterReview = onOpenChapterReview
            )
        }
    }
}

@Composable
private fun CourseChapterCard(
    item: CourseOverviewViewModel.ChapterWithStats,
    onOpenChapterLearn: (Int) -> Unit,
    onOpenChapterTrain: (Int) -> Unit,
    onOpenChapterReview: (Int) -> Unit
) {
    val chapter = item.chapter
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = chapter.name, style = MaterialTheme.typography.titleMedium)
            CourseChapterProgress(totalLines = item.totalLines, learnedLines = item.learnedLines)
            if (item.gameStats != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ChapterGameStatsRow(stats = item.gameStats)
            }
            CourseChapterActionsRow(
                chapterId = chapter.id,
                onOpenChapterLearn = onOpenChapterLearn,
                onOpenChapterTrain = onOpenChapterTrain,
                onOpenChapterReview = onOpenChapterReview
            )
        }
    }
}

@Composable
private fun CourseChapterProgress(totalLines: Int, learnedLines: Int) {
    val percent = if (totalLines == 0) 0 else (learnedLines * 100 / totalLines)
    Text(
        text = if (totalLines > 0) "$learnedLines / $totalLines lines learned ($percent%)"
        else "No lines yet", style = MaterialTheme.typography.bodySmall
    )
    if (totalLines > 0) {
        LinearProgressIndicator(
            progress = { learnedLines.toFloat() / totalLines.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChapterGameStatsRow(stats: ChapterStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stats.played} games",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${(stats.winPct * 100).toInt()}% win",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${stats.wins}W ${stats.losses}L ${stats.draws}D",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (stats.played > 0) {
            val inBookPct = (stats.inBookPct * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "In book",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$inBookPct%  (${stats.gamesInBook}/${stats.played})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (inBookPct >= 70) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CourseChapterActionsRow(
    chapterId: Int,
    onOpenChapterLearn: (Int) -> Unit,
    onOpenChapterTrain: (Int) -> Unit,
    onOpenChapterReview: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = { onOpenChapterLearn(chapterId) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Learn", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            onClick = { onOpenChapterReview(chapterId) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Review", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
            onClick = { onOpenChapterTrain(chapterId) },
            modifier = Modifier.weight(1f),
        ) {
            Text("Train", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}