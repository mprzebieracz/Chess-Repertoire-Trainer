package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.chessrepertoiretrainer.feature.settings.presentation.state.SettingsUiState

@Composable
internal fun AccountsSection(
    lichessInput: String,
    chessComInput: String,
    tokenInput: String,
    screenState: SettingsUiState,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onSaveUsernames: () -> Unit,
) {
    SectionHeader(text = "Accounts")

    AccountsInputFields(
        lichessInput = lichessInput,
        chessComInput = chessComInput,
        tokenInput = tokenInput,
        onLichessInputChange = onLichessInputChange,
        onChessComInputChange = onChessComInputChange,
        onTokenInputChange = onTokenInputChange,
    )

    SaveUsernamesRow(screenState = screenState, onSaveUsernames = onSaveUsernames)
}

@Composable
private fun AccountsInputFields(
    lichessInput: String,
    chessComInput: String,
    tokenInput: String,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onTokenInputChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = lichessInput,
        onValueChange = onLichessInputChange,
        label = { Text("Lichess username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = chessComInput,
        onValueChange = onChessComInputChange,
        label = { Text("Chess.com username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = tokenInput,
        onValueChange = onTokenInputChange,
        label = { Text("Lichess API token (for Opening Explorer)") },
        supportingText = { Text("Get one at lichess.org/account/oauth/token") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SaveUsernamesRow(screenState: SettingsUiState, onSaveUsernames: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onSaveUsernames) {
            Text("Save usernames")
        }

        SaveStatusMessage(screenState = screenState)
    }
}

@Composable
private fun SaveStatusMessage(screenState: SettingsUiState) {
    if (screenState.isSaving) {
        Text(
            text = "Saving...",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (screenState.saveSuccessMessage != null) {
        Text(
            text = "Saved",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (screenState.saveErrorMessage != null) {
        Text(
            text = screenState.saveErrorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
