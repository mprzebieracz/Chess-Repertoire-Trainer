package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SoundSection(
    soundsEnabled: Boolean,
    onSoundsEnabledChange: (Boolean) -> Unit,
) {
    SectionHeader(text = "Sound", modifier = Modifier.padding(top = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Move sounds", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = soundsEnabled, onCheckedChange = onSoundsEnabledChange)
    }
}
