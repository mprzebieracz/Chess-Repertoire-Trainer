package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.CourseOverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseOverviewScreen(
    viewModel: CourseOverviewViewModel,
    onBackClick: () -> Unit,
    onEditCourse: (repertoireId: Int) -> Unit,
    onTrainCourse: (repertoireId: Int) -> Unit,
    onOpenChapterLearn: (chapterId: Int) -> Unit,
    onOpenChapterTrain: (chapterId: Int) -> Unit,
    onOpenChapterReview: (chapterId: Int) -> Unit
) {
    val repertoire by viewModel.repertoire.collectAsState()
    val chaptersWithStats by viewModel.chaptersWithStats.collectAsState()

    CourseOverviewScaffold(
        title = repertoire?.name ?: "Course",
        repertoireId = repertoire?.id,
        onBackClick = onBackClick,
        onEditCourse = onEditCourse,
        onTrainCourse = onTrainCourse
    ) { padding ->
        CourseOverviewContent(
            padding = padding,
            chaptersWithStats = chaptersWithStats,
            onOpenChapterLearn = onOpenChapterLearn,
            onOpenChapterTrain = onOpenChapterTrain,
            onOpenChapterReview = onOpenChapterReview
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseOverviewScaffold(
    title: String,
    repertoireId: Int?,
    onBackClick: () -> Unit,
    onEditCourse: (Int) -> Unit,
    onTrainCourse: (Int) -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CourseOverviewTopBar(
                title = title,
                repertoireId = repertoireId,
                onBackClick = onBackClick,
                onEditCourse = onEditCourse,
                onTrainCourse = onTrainCourse
            )
        }
    ) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseOverviewTopBar(
    title: String,
    repertoireId: Int?,
    onBackClick: () -> Unit,
    onEditCourse: (Int) -> Unit,
    onTrainCourse: (Int) -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (repertoireId != null) {
                CourseOverviewActionButtons(
                    repertoireId = repertoireId,
                    onTrainCourse = onTrainCourse,
                    onEditCourse = onEditCourse
                )
            }
        }
    )
}

@Composable
private fun CourseOverviewActionButtons(
    repertoireId: Int,
    onTrainCourse: (Int) -> Unit,
    onEditCourse: (Int) -> Unit
) {
    IconButton(onClick = { onTrainCourse(repertoireId) }) {
        Icon(
            imageVector = AppIcons.TrainCourse,
            contentDescription = "Train course"
        )
    }
    IconButton(onClick = { onEditCourse(repertoireId) }) {
        Icon(
            imageVector = AppIcons.EditCourse,
            contentDescription = "Edit course"
        )
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
    } else {
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
        Text(
            text = "No chapters yet.",
            style = MaterialTheme.typography.bodyMedium
        )
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
    val totalLines = item.totalLines
    val learnedLines = item.learnedLines

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenChapterLearn(chapter.id) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            CourseChapterHeader(chapterName = chapter.name)
            CourseChapterProgress(
                totalLines = totalLines,
                learnedLines = learnedLines
            )
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
private fun CourseChapterHeader(chapterName: String) {
    Text(
        text = chapterName,
        style = MaterialTheme.typography.titleMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseChapterProgress(
    totalLines: Int,
    learnedLines: Int
) {
    val percent = if (totalLines == 0) 0 else (learnedLines * 100 / totalLines)
    Text(
        text = if (totalLines > 0) {
            "$learnedLines / $totalLines lines learned ($percent%)"
        } else {
            "No lines yet"
        },
        style = MaterialTheme.typography.bodySmall
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
        CourseActionButton(label = "Learn", modifier = Modifier.weight(1f)) {
            onOpenChapterLearn(chapterId)
        }
        CourseActionOutlinedButton(label = "Review", modifier = Modifier.weight(1f)) {
            onOpenChapterReview(chapterId)
        }
        CourseActionOutlinedButton(label = "Train", modifier = Modifier.weight(1f)) {
            onOpenChapterTrain(chapterId)
        }
    }
}

@Composable
private fun RowScope.CourseActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier) {
        Text(label)
    }
}

@Composable
private fun RowScope.CourseActionOutlinedButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(label)
    }
}
