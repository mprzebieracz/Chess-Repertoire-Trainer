package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AdvancedSection(
    depth: Int,
    movetime: Int,
    threads: Int,
    onDepthChange: (Int) -> Unit,
    onMoveTimeChange: (Int) -> Unit,
    onThreadsChange: (Int) -> Unit,
) {
    SectionHeader(text = "Advanced", modifier = Modifier.padding(top = 8.dp))
    EngineSection(
        depth = depth,
        movetime = movetime,
        threads = threads,
        onDepthChange = onDepthChange,
        onMoveTimeChange = onMoveTimeChange,
        onThreadsChange = onThreadsChange,
    )
}

@Composable
private fun EngineSection(
    depth: Int,
    movetime: Int,
    threads: Int,
    onDepthChange: (Int) -> Unit,
    onMoveTimeChange: (Int) -> Unit,
    onThreadsChange: (Int) -> Unit,
) {
    Text(
        text = "Engine",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Depth: $depth", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = depth.toFloat(),
            onValueChange = { onDepthChange(it.toInt()) },
            valueRange = 10f..30f,
            steps = 19,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Time per move: ${movetime / 1000}s", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = movetime.toFloat(),
            onValueChange = { onMoveTimeChange(it.toInt()) },
            valueRange = 500f..10000f,
            steps = 19,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Threads: $threads", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = threads.toFloat(),
            onValueChange = { onThreadsChange(it.toInt()) },
            valueRange = 1f..4f,
            steps = 2,
        )
    }
}