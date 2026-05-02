package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings
import com.example.chessrepertoiretrainer.feature.settings.presentation.state.SettingsScreenState
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    var lichessInput by remember { mutableStateOf("") }
    var chessComInput by remember { mutableStateOf("") }

    LaunchedEffect(settings.lichessUsername, settings.chessComUsername) {
        lichessInput = settings.lichessUsername
        chessComInput = settings.chessComUsername
    }

    LaunchedEffect(screenState.saveSuccessMessage, screenState.saveErrorMessage) {
        if (screenState.saveSuccessMessage != null || screenState.saveErrorMessage != null) {
            delay(2000)
            viewModel.clearTransientMessages()
        }
    }

    SettingsScaffold {
        SettingsContent(
            settings = settings,
            screenState = screenState,
            lichessInput = lichessInput,
            chessComInput = chessComInput,
            onLichessInputChange = { lichessInput = it },
            onChessComInputChange = { chessComInput = it },
            onSaveUsernames = {
                viewModel.updateLichessUsername(lichessInput)
                viewModel.updateChessComUsername(chessComInput)
            },
            onThemeModeChange = viewModel::updateAppThemeMode,
            onBoardThemeChange = viewModel::updateBoardTheme,
            onPlatformChange = viewModel::updateDefaultOnlinePlatform,
            onDynamicColorsChange = viewModel::updateUseDynamicColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsContent(
    settings: UserSettings,
    screenState: SettingsScreenState,
    lichessInput: String,
    chessComInput: String,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onSaveUsernames: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
    onPlatformChange: (String) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit
) {
    AccountsSection(
        lichessInput = lichessInput,
        chessComInput = chessComInput,
        screenState = screenState,
        onLichessInputChange = onLichessInputChange,
        onChessComInputChange = onChessComInputChange,
        onSaveUsernames = onSaveUsernames
    )

    AppearanceSection(
        settings = settings,
        onThemeModeChange = onThemeModeChange,
        onBoardThemeChange = onBoardThemeChange
    )

    DefaultPlatformSection(
        selectedPlatform = settings.defaultOnlinePlatform, onPlatformChange = onPlatformChange
    )

    DynamicColorsSection(
        useDynamicColors = settings.useDynamicColors, onDynamicColorsChange = onDynamicColorsChange
    )

    AdvancedSection()
}

@Composable
private fun AccountsSection(
    lichessInput: String,
    chessComInput: String,
    screenState: SettingsScreenState,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit,
    onSaveUsernames: () -> Unit
) {
    SectionHeader(text = "Accounts")

    AccountsInputFields(
        lichessInput = lichessInput,
        chessComInput = chessComInput,
        onLichessInputChange = onLichessInputChange,
        onChessComInputChange = onChessComInputChange
    )

    SaveUsernamesRow(
        screenState = screenState, onSaveUsernames = onSaveUsernames
    )
}

@Composable
private fun AccountsInputFields(
    lichessInput: String,
    chessComInput: String,
    onLichessInputChange: (String) -> Unit,
    onChessComInputChange: (String) -> Unit
) {
    OutlinedTextField(
        value = lichessInput,
        onValueChange = onLichessInputChange,
        label = { Text("Lichess username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = chessComInput,
        onValueChange = onChessComInputChange,
        label = { Text("Chess.com username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SaveUsernamesRow(
    screenState: SettingsScreenState, onSaveUsernames: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onSaveUsernames) {
            Text("Save usernames")
        }

        SaveStatusMessage(screenState = screenState)
    }
}

@Composable
private fun SaveStatusMessage(screenState: SettingsScreenState) {
    if (screenState.isSaving) {
        Text(
            text = "Saving...",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall
        )
    }
    else if (screenState.saveSuccessMessage != null) {
        Text(
            text = "Saved",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall
        )
    }
    else if (screenState.saveErrorMessage != null) {
        Text(
            text = screenState.saveErrorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AppearanceSection(
    settings: UserSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit
) {
    SectionHeader(text = "Appearance", modifier = Modifier.padding(top = 8.dp))

    ThemeModeSelector(current = settings.appThemeMode, onChange = onThemeModeChange)
    BoardThemeSelector(current = settings.boardTheme, onChange = onBoardThemeChange)
}

@Composable
private fun DefaultPlatformSection(
    selectedPlatform: String, onPlatformChange: (String) -> Unit
) {
    SectionHeader(text = "Default online platform", modifier = Modifier.padding(top = 8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selectedPlatform == "lichess", onClick = { onPlatformChange("lichess") })
        Text(text = "Lichess")

        Spacer(modifier = Modifier.padding(start = 16.dp))

        RadioButton(
            selected = selectedPlatform == "chess.com", onClick = { onPlatformChange("chess.com") })
        Text(text = "Chess.com")
    }
}

@Composable
private fun DynamicColorsSection(
    useDynamicColors: Boolean, onDynamicColorsChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dynamic Material colors")
            Text(
                text = "Use system-derived colors on Android 12+",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = useDynamicColors,
            onCheckedChange = onDynamicColorsChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun AdvancedSection() {
    SectionHeader(text = "Advanced", modifier = Modifier.padding(top = 8.dp))

    Text(
        text = "Here you could later tweak training speed, default learn mode options, PGN import filters, etc.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
private fun ThemeModeSelector(current: AppThemeMode, onChange: (AppThemeMode) -> Unit) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkChecked = when (current) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemIsDark
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dark theme")
            Text(
                text = "Toggle between light and dark app theme",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = isDarkChecked, onCheckedChange = { checked ->
                onChange(if (checked) AppThemeMode.DARK else AppThemeMode.LIGHT)
            }, colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun BoardThemeSelector(current: BoardTheme, onChange: (BoardTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Board theme")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (current) {
                    BoardTheme.CLASSIC -> "Classic green"
                    BoardTheme.BLUE -> "Blue"
                    BoardTheme.BROWN -> "Brown"
                }
            )
            IconButton(onClick = { expanded = true }) {
                Icon(AppIcons.TrainChapter, contentDescription = "Change board theme")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Classic green") }, onClick = {
                onChange(BoardTheme.CLASSIC)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Blue") }, onClick = {
                onChange(BoardTheme.BLUE)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Brown") }, onClick = {
                onChange(BoardTheme.BROWN)
                expanded = false
            })
        }
    }
}
