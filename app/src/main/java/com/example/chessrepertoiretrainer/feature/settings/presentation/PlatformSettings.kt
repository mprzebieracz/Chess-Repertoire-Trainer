package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DefaultPlatformSection(selectedPlatform: String, onPlatformChange: (String) -> Unit) {
    SectionHeader(text = "Default online platform", modifier = Modifier.padding(top = 8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selectedPlatform == "lichess",
            onClick = { onPlatformChange("lichess") },
        )
        Text(text = "Lichess")

        Spacer(modifier = Modifier.padding(start = 16.dp))

        RadioButton(
            selected = selectedPlatform == "chess.com",
            onClick = { onPlatformChange("chess.com") },
        )
        Text(text = "Chess.com")
    }
}
