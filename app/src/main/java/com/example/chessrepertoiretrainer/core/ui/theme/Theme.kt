package com.example.chessrepertoiretrainer.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.chessrepertoiretrainer.core.ui.theme.themes.ForestGreenDarkColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.ForestGreenLightColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.VelvetPinkDarkColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.VelvetPinkLightColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.WarmBrownDarkColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.WarmBrownLightColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.WarmCreamDarkColorScheme
import com.example.chessrepertoiretrainer.core.ui.theme.themes.WarmCreamLightColorScheme
import com.example.chessrepertoiretrainer.feature.settings.data.AppColorTheme

@Composable
fun ChessRepertoireTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appColorTheme: AppColorTheme = AppColorTheme.WARM_BROWN,
    content: @Composable () -> Unit,
) {
    val useDynamicColors =
        dynamicColor && appColorTheme == AppColorTheme.WARM_BROWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColors -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> when (appColorTheme) {
            AppColorTheme.WARM_BROWN -> if (darkTheme) WarmBrownDarkColorScheme else WarmBrownLightColorScheme
            AppColorTheme.FOREST_GREEN -> if (darkTheme) ForestGreenDarkColorScheme else ForestGreenLightColorScheme
            AppColorTheme.WARM_CREAM -> if (darkTheme) WarmCreamDarkColorScheme else WarmCreamLightColorScheme
            AppColorTheme.VELVET_PINK -> if (darkTheme) VelvetPinkDarkColorScheme else VelvetPinkLightColorScheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}