package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import com.example.chessrepertoiretrainer.feature.settings.data.AppColorTheme
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings

@Composable
internal fun AppearanceSection(
    settings: UserSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onColorThemeChange: (AppColorTheme) -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
) {
    SectionHeader(text = "Appearance", modifier = Modifier.padding(top = 8.dp))

    ThemeModeSelector(current = settings.appThemeMode, onChange = onThemeModeChange)
    ColorThemeSelector(current = settings.appColorTheme, onChange = onColorThemeChange)
    BoardThemeSelector(current = settings.boardTheme, onChange = onBoardThemeChange)
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Dark theme")
            Text(
                text = "Toggle between light and dark app theme",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = isDarkChecked,
            onCheckedChange = { checked ->
                onChange(if (checked) AppThemeMode.DARK else AppThemeMode.LIGHT)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ColorThemeSelector(current: AppColorTheme, onChange: (AppColorTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Color theme")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (current) {
                    AppColorTheme.WARM_BROWN -> "Warm Brown"
                    AppColorTheme.FOREST_GREEN -> "Forest Green"
                    AppColorTheme.WARM_CREAM -> "Warm Cream"
                    AppColorTheme.VELVET_PINK -> "Velvet Pink"
                },
            )
            IconButton(onClick = { expanded = true }) {
                Icon(AppIcons.Settings, contentDescription = "Change color theme")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Warm Brown") }, onClick = {
                onChange(AppColorTheme.WARM_BROWN)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Forest Green") }, onClick = {
                onChange(AppColorTheme.FOREST_GREEN)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Warm Cream") }, onClick = {
                onChange(AppColorTheme.WARM_CREAM)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Velvet Pink") }, onClick = {
                onChange(AppColorTheme.VELVET_PINK)
                expanded = false
            })
        }
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (current) {
                    BoardTheme.CLASSIC -> "Classic green"
                    BoardTheme.BLUE -> "Blue"
                    BoardTheme.BROWN -> "Brown"
                    BoardTheme.RED -> "Tournament (red)"
                    BoardTheme.NIGHT -> "Night (slate blue)"
                },
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
            DropdownMenuItem(text = { Text("Tournament (red)") }, onClick = {
                onChange(BoardTheme.RED)
                expanded = false
            })
            DropdownMenuItem(text = { Text("Night (slate blue)") }, onClick = {
                onChange(BoardTheme.NIGHT)
                expanded = false
            })
        }
    }
}
