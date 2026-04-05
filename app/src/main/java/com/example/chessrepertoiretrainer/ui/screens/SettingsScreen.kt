package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.data.AppThemeMode
import com.example.chessrepertoiretrainer.data.BoardTheme
import com.example.chessrepertoiretrainer.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()

    var lichessInput by remember { mutableStateOf("") }
    var chessComInput by remember { mutableStateOf("") }
    var showUsernamesSaved by remember { mutableStateOf(false) }

    LaunchedEffect(settings.lichessUsername, settings.chessComUsername) {
        lichessInput = settings.lichessUsername
        chessComInput = settings.chessComUsername
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = lichessInput,
                onValueChange = { lichessInput = it },
                label = { Text("Lichess username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = chessComInput,
                onValueChange = { chessComInput = it },
                label = { Text("Chess.com username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.updateLichessUsername(lichessInput)
                        viewModel.updateChessComUsername(chessComInput)
                        showUsernamesSaved = true
                    }
                ) {
                    Text("Save usernames")
                }

                if (showUsernamesSaved) {
                    Text(
                        text = "Saved",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LaunchedEffect(showUsernamesSaved) {
                if (showUsernamesSaved) {
                    kotlinx.coroutines.delay(2000)
                    showUsernamesSaved = false
                }
            }

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            ThemeModeSelector(
                current = settings.appThemeMode,
                onChange = { viewModel.updateAppThemeMode(it) }
            )

            BoardThemeSelector(
                current = settings.boardTheme,
                onChange = { viewModel.updateBoardTheme(it) }
            )

            Text(
                text = "Default online platform",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = settings.defaultOnlinePlatform == "lichess",
                    onClick = { viewModel.updateDefaultOnlinePlatform("lichess") }
                )
                Text(text = "Lichess")

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 16.dp))

                androidx.compose.material3.RadioButton(
                    selected = settings.defaultOnlinePlatform == "chess.com",
                    onClick = { viewModel.updateDefaultOnlinePlatform("chess.com") }
                )
                Text(text = "Chess.com")
            }

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
                    checked = settings.useDynamicColors,
                    onCheckedChange = { viewModel.updateUseDynamicColors(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Text(
                text = "Advanced",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Here you could later tweak training speed, default learn mode options, PGN import filters, etc.",
                style = MaterialTheme.typography.bodySmall
            )
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
            checked = isDarkChecked,
            onCheckedChange = { checked ->
                onChange(if (checked) AppThemeMode.DARK else AppThemeMode.LIGHT)
            },
            colors = SwitchDefaults.colors(
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
            modifier = Modifier
                .fillMaxWidth(),
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
            DropdownMenuItem(
                text = { Text("Classic green") },
                onClick = {
                    onChange(BoardTheme.CLASSIC)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Blue") },
                onClick = {
                    onChange(BoardTheme.BLUE)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Brown") },
                onClick = {
                    onChange(BoardTheme.BROWN)
                    expanded = false
                }
            )
        }
    }
}
