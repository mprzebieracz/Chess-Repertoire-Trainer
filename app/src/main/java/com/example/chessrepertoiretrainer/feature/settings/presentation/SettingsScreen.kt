package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.feature.settings.data.AppColorTheme
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings
import com.example.chessrepertoiretrainer.feature.settings.presentation.state.SettingsUiState
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    var lichessInput by remember { mutableStateOf("") }
    var chessComInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }

    SyncAccountInputsEffect(
        lichessUsername = settings.lichessUsername,
        chessComUsername = settings.chessComUsername,
        lichessApiToken = settings.lichessApiToken,
        onLichessInputChange = { lichessInput = it },
        onChessComInputChange = { chessComInput = it },
        onTokenInputChange = { tokenInput = it },
    )

    ClearSaveMessagesEffect(
        saveSuccessMessage = screenState.saveSuccessMessage,
        saveErrorMessage = screenState.saveErrorMessage,
        onClearMessages = viewModel::clearTransientMessages,
    )

    SettingsScaffold {
        SettingsContent(
            settings = settings,
            screenState = screenState,
            lichessInput = lichessInput,
            chessComInput = chessComInput,
            tokenInput = tokenInput,
            onLichessInputChange = { lichessInput = it },
            onChessComInputChange = { chessComInput = it },
            onTokenInputChange = { tokenInput = it },
            onSaveUsernames = {
                viewModel.updateLichessUsername(lichessInput)
                viewModel.updateChessComUsername(chessComInput)
                viewModel.updateLichessApiToken(tokenInput)
            },
            onThemeModeChange = viewModel::updateAppThemeMode,
            onColorThemeChange = viewModel::updateAppColorTheme,
            onBoardThemeChange = viewModel::updateBoardTheme,
            onPlatformChange = viewModel::updateDefaultOnlinePlatform,
            onEngineDepthChange = viewModel::updateEngineDepth,
            onEngineMoveTimeChange = viewModel::updateEngineMovetime,
            onEngineThreadsChange = viewModel::updateEngineThreads,
        )
    }
}

@Composable
private fun SyncAccountInputsEffect(
    lichessUsername: String,
    chessComUsername: String,
    lichessApiToken: String,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onTokenInputChange: (String) -> Unit,
) {
    LaunchedEffect(lichessUsername, chessComUsername, lichessApiToken) {
        onLichessInputChange(lichessUsername)
        onChessComInputChange(chessComUsername)
        onTokenInputChange(lichessApiToken)
    }
}

@Composable
private fun ClearSaveMessagesEffect(
    saveSuccessMessage: String?,
    saveErrorMessage: String?,
    onClearMessages: () -> Unit,
) {
    LaunchedEffect(saveSuccessMessage, saveErrorMessage) {
        if (saveSuccessMessage != null || saveErrorMessage != null) {
            delay(2000)
            onClearMessages()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(content: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettings,
    screenState: SettingsUiState,
    lichessInput: String,
    chessComInput: String,
    tokenInput: String,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onSaveUsernames: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onColorThemeChange: (AppColorTheme) -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
    onPlatformChange: (String) -> Unit,
    onEngineDepthChange: (Int) -> Unit,
    onEngineMoveTimeChange: (Int) -> Unit,
    onEngineThreadsChange: (Int) -> Unit,
) {
    AccountsSection(
        lichessInput = lichessInput,
        chessComInput = chessComInput,
        tokenInput = tokenInput,
        screenState = screenState,
        onLichessInputChange = onLichessInputChange,
        onChessComInputChange = onChessComInputChange,
        onTokenInputChange = onTokenInputChange,
        onSaveUsernames = onSaveUsernames,
    )

    AppearanceSection(
        settings = settings,
        onThemeModeChange = onThemeModeChange,
        onColorThemeChange = onColorThemeChange,
        onBoardThemeChange = onBoardThemeChange,
    )

    DefaultPlatformSection(
        selectedPlatform = settings.defaultOnlinePlatform,
        onPlatformChange = onPlatformChange,
    )

    AdvancedSection(
        depth = settings.engineDepth,
        movetime = settings.engineMovetime,
        threads = settings.engineThreads,
        onDepthChange = onEngineDepthChange,
        onMoveTimeChange = onEngineMoveTimeChange,
        onThreadsChange = onEngineThreadsChange,
    )
}

@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}
