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
import androidx.compose.material3.Slider
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

    SyncAccountInputsEffect(lichessUsername = settings.lichessUsername,
                            chessComUsername = settings.chessComUsername,
                            onLichessInputChange = { lichessInput = it },
                            onChessComInputChange = { chessComInput = it })

    ClearSaveMessagesEffect(saveSuccessMessage = screenState.saveSuccessMessage,
                            saveErrorMessage = screenState.saveErrorMessage,
                            onClearMessages = viewModel::clearTransientMessages)

    SettingsScaffold {
        SettingsContent(settings = settings,
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
                        onColorThemeChange = viewModel::updateAppColorTheme,
                        onBoardThemeChange = viewModel::updateBoardTheme,
                        onPlatformChange = viewModel::updateDefaultOnlinePlatform,
                        onDynamicColorsChange = viewModel::updateUseDynamicColors,
                        onEngineDepthChange = viewModel::updateEngineDepth,
                        onEngineMoveTimeChange = viewModel::updateEngineMovetime,
                        onEngineThreadsChange = viewModel::updateEngineThreads)
    }
}

@Composable
private fun SyncAccountInputsEffect(lichessUsername: String,
                                    chessComUsername: String,
                                    onLichessInputChange: (String) -> Unit,
                                    onChessComInputChange: (String) -> Unit) {
    LaunchedEffect(lichessUsername, chessComUsername) {
        onLichessInputChange(lichessUsername)
        onChessComInputChange(chessComUsername)
    }
}

@Composable
private fun ClearSaveMessagesEffect(saveSuccessMessage: String?,
                                    saveErrorMessage: String?,
                                    onClearMessages: () -> Unit) {
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
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
               verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsContent(settings: UserSettings,
                            screenState: SettingsUiState,
                            lichessInput: String,
                            chessComInput: String,
                            onLichessInputChange: (String) -> Unit,
                            onChessComInputChange: (String) -> Unit,
                            onSaveUsernames: () -> Unit,
                            onThemeModeChange: (AppThemeMode) -> Unit,
                            onColorThemeChange: (AppColorTheme) -> Unit,
                            onBoardThemeChange: (BoardTheme) -> Unit,
                            onPlatformChange: (String) -> Unit,
                            onDynamicColorsChange: (Boolean) -> Unit,
                            onEngineDepthChange: (Int) -> Unit,
                            onEngineMoveTimeChange: (Int) -> Unit,
                            onEngineThreadsChange: (Int) -> Unit) {
    AccountsSection(lichessInput = lichessInput,
                    chessComInput = chessComInput,
                    screenState = screenState,
                    onLichessInputChange = onLichessInputChange,
                    onChessComInputChange = onChessComInputChange,
                    onSaveUsernames = onSaveUsernames)

    AppearanceSection(settings = settings,
                      onThemeModeChange = onThemeModeChange,
                      onColorThemeChange = onColorThemeChange,
                      onBoardThemeChange = onBoardThemeChange)

    DefaultPlatformSection(selectedPlatform = settings.defaultOnlinePlatform,
                           onPlatformChange = onPlatformChange)

    DynamicColorsSection(useDynamicColors = settings.useDynamicColors,
                         onDynamicColorsChange = onDynamicColorsChange)

    EngineSection(depth = settings.engineDepth,
                  movetime = settings.engineMovetime,
                  threads = settings.engineThreads,
                  onDepthChange = onEngineDepthChange,
                  onMoveTimeChange = onEngineMoveTimeChange,
                  onThreadsChange = onEngineThreadsChange)

    AdvancedSection()
}

@Composable
private fun AccountsSection(lichessInput: String,
                            chessComInput: String,
                            screenState: SettingsUiState,
                            onLichessInputChange: (String) -> Unit,
                            onChessComInputChange: (String) -> Unit,
                            onSaveUsernames: () -> Unit) {
    SectionHeader(text = "Accounts")

    AccountsInputFields(lichessInput = lichessInput,
                        chessComInput = chessComInput,
                        onLichessInputChange = onLichessInputChange,
                        onChessComInputChange = onChessComInputChange)

    SaveUsernamesRow(screenState = screenState, onSaveUsernames = onSaveUsernames)
}

@Composable
private fun AccountsInputFields(lichessInput: String,
                                chessComInput: String,
                                onLichessInputChange: (String) -> Unit,
                                onChessComInputChange: (String) -> Unit) {
    OutlinedTextField(value = lichessInput,
                      onValueChange = onLichessInputChange,
                      label = { Text("Lichess username") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())

    OutlinedTextField(value = chessComInput,
                      onValueChange = onChessComInputChange,
                      label = { Text("Chess.com username") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SaveUsernamesRow(screenState: SettingsUiState, onSaveUsernames: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSaveUsernames) {
            Text("Save usernames")
        }

        SaveStatusMessage(screenState = screenState)
    }
}

@Composable
private fun SaveStatusMessage(screenState: SettingsUiState) {
    if (screenState.isSaving) {
        Text(text = "Saving...",
             color = MaterialTheme.colorScheme.primary,
             style = MaterialTheme.typography.bodySmall)
    }
    else if (screenState.saveSuccessMessage != null) {
        Text(text = "Saved",
             color = MaterialTheme.colorScheme.primary,
             style = MaterialTheme.typography.bodySmall)
    }
    else if (screenState.saveErrorMessage != null) {
        Text(text = screenState.saveErrorMessage,
             color = MaterialTheme.colorScheme.error,
             style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppearanceSection(settings: UserSettings,
                              onThemeModeChange: (AppThemeMode) -> Unit,
                              onColorThemeChange: (AppColorTheme) -> Unit,
                              onBoardThemeChange: (BoardTheme) -> Unit) {
    SectionHeader(text = "Appearance", modifier = Modifier.padding(top = 8.dp))

    ThemeModeSelector(current = settings.appThemeMode, onChange = onThemeModeChange)
    ColorThemeSelector(current = settings.appColorTheme, onChange = onColorThemeChange)
    BoardThemeSelector(current = settings.boardTheme, onChange = onBoardThemeChange)
}

@Composable
private fun DefaultPlatformSection(selectedPlatform: String, onPlatformChange: (String) -> Unit) {
    SectionHeader(text = "Default online platform", modifier = Modifier.padding(top = 8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedPlatform == "lichess",
                    onClick = { onPlatformChange("lichess") })
        Text(text = "Lichess")

        Spacer(modifier = Modifier.padding(start = 16.dp))

        RadioButton(selected = selectedPlatform == "chess.com",
                    onClick = { onPlatformChange("chess.com") })
        Text(text = "Chess.com")
    }
}

@Composable
private fun DynamicColorsSection(useDynamicColors: Boolean,
                                 onDynamicColorsChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dynamic Material colors")
            Text(text = "Use system-derived colors on Android 12+ (overrides Color theme above)",
                 style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = useDynamicColors,
               onCheckedChange = onDynamicColorsChange,
               colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun EngineSection(depth: Int,
                          movetime: Int,
                          threads: Int,
                          onDepthChange: (Int) -> Unit,
                          onMoveTimeChange: (Int) -> Unit,
                          onThreadsChange: (Int) -> Unit) {
    SectionHeader(text = "Engine (Stockfish)", modifier = Modifier.padding(top = 8.dp))
    Text(text = "Place libstockfish.so in jniLibs/arm64-v8a/ to enable analysis.",
         style = MaterialTheme.typography.bodySmall,
         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Depth: $depth", style = MaterialTheme.typography.bodyMedium)
        Slider(value = depth.toFloat(),
               onValueChange = { onDepthChange(it.toInt()) },
               valueRange = 10f..30f,
               steps = 19)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Time per move: ${movetime / 1000}s", style = MaterialTheme.typography.bodyMedium)
        Slider(value = movetime.toFloat(),
               onValueChange = { onMoveTimeChange(it.toInt()) },
               valueRange = 500f..10000f,
               steps = 19)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Threads: $threads", style = MaterialTheme.typography.bodyMedium)
        Text(text = "1 = battery-friendly, 4 = strongest (heats device)",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Slider(value = threads.toFloat(),
               onValueChange = { onThreadsChange(it.toInt()) },
               valueRange = 1f..4f,
               steps = 2)
    }
}

@Composable
private fun AdvancedSection() {
    SectionHeader(text = "Advanced", modifier = Modifier.padding(top = 8.dp))

    Text(text = "Here you could later tweak training speed, default learn mode options, PGN import filters, etc.",
         style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text,
         style = MaterialTheme.typography.titleMedium,
         fontWeight = FontWeight.SemiBold,
         modifier = modifier)
}

@Composable
private fun ColorThemeSelector(current: AppColorTheme, onChange: (AppColorTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Color theme")
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = when (current) {
                AppColorTheme.DARK_WOOD  -> "Dark Wood (chess.com)"
                AppColorTheme.LICHESS    -> "Lichess Green"
                AppColorTheme.WARM_LIGHT -> "Warm Parchment"
            })
            IconButton(onClick = { expanded = true }) {
                Icon(AppIcons.Settings, contentDescription = "Change color theme")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Dark Wood (chess.com)") }, onClick = {
                onChange(AppColorTheme.DARK_WOOD)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Lichess Green") }, onClick = {
                onChange(AppColorTheme.LICHESS)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Warm Parchment") }, onClick = {
                onChange(AppColorTheme.WARM_LIGHT)
                expanded = false
            })
        }
    }
}

@Composable
private fun ThemeModeSelector(current: AppThemeMode, onChange: (AppThemeMode) -> Unit) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkChecked = when (current) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemIsDark
    }

    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dark theme")
            Text(text = "Toggle between light and dark app theme",
                 style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = isDarkChecked, onCheckedChange = { checked ->
            onChange(if (checked) AppThemeMode.DARK else AppThemeMode.LIGHT)
        }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun BoardThemeSelector(current: BoardTheme, onChange: (BoardTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Board theme")
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = when (current) {
                BoardTheme.CLASSIC    -> "Classic green"
                BoardTheme.BLUE       -> "Blue"
                BoardTheme.BROWN      -> "Brown"
                BoardTheme.TOURNAMENT -> "Tournament (red)"
                BoardTheme.NIGHT      -> "Night (slate blue)"
            })
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
            DropdownMenuItem(text = { Text("Tournament (red)") }, onClick = {
                onChange(BoardTheme.TOURNAMENT)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Night (slate blue)") }, onClick = {
                onChange(BoardTheme.NIGHT)
                expanded = false
            })
        }
    }
}