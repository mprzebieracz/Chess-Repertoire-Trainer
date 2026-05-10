package com.example.chessrepertoiretrainer.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.chessrepertoiretrainer.feature.settings.data.AppColorTheme

// ---------------------------------------------------------------------------
// DARK_WOOD schemes
// ---------------------------------------------------------------------------

private val DarkWoodDarkColorScheme = darkColorScheme(
    primary = DarkWood_Primary,
    onPrimary = DarkWood_OnPrimary,
    primaryContainer = DarkWood_PrimaryContainer,
    onPrimaryContainer = DarkWood_OnPrimaryContainer,
    secondary = DarkWood_Secondary,
    onSecondary = DarkWood_OnSecondary,
    secondaryContainer = DarkWood_SecondaryContainer,
    onSecondaryContainer = DarkWood_OnSecondaryContainer,
    tertiary = DarkWood_Tertiary,
    onTertiary = DarkWood_OnTertiary,
    tertiaryContainer = DarkWood_TertiaryContainer,
    onTertiaryContainer = DarkWood_OnTertiaryContainer,
    background = DarkWood_Background,
    onBackground = DarkWood_OnBackground,
    surface = DarkWood_Surface,
    onSurface = DarkWood_OnSurface,
    surfaceVariant = DarkWood_SurfaceVariant,
    onSurfaceVariant = DarkWood_OnSurfaceVariant,
    outline = DarkWood_Outline,
    error = DarkWood_Error,
    onError = DarkWood_OnError,
    errorContainer = DarkWood_ErrorContainer,
    onErrorContainer = DarkWood_OnErrorContainer,
)

private val DarkWoodLightColorScheme = lightColorScheme(
    primary = DarkWoodLight_Primary,
    onPrimary = DarkWoodLight_OnPrimary,
    primaryContainer = DarkWoodLight_PrimaryContainer,
    onPrimaryContainer = DarkWoodLight_OnPrimaryContainer,
    secondary = DarkWoodLight_Secondary,
    onSecondary = DarkWoodLight_OnSecondary,
    secondaryContainer = DarkWoodLight_SecondaryContainer,
    onSecondaryContainer = DarkWoodLight_OnSecondaryContainer,
    tertiary = DarkWoodLight_Tertiary,
    onTertiary = DarkWoodLight_OnTertiary,
    tertiaryContainer = DarkWoodLight_TertiaryContainer,
    onTertiaryContainer = DarkWoodLight_OnTertiaryContainer,
    background = DarkWoodLight_Background,
    onBackground = DarkWoodLight_OnBackground,
    surface = DarkWoodLight_Surface,
    onSurface = DarkWoodLight_OnSurface,
    surfaceVariant = DarkWoodLight_SurfaceVariant,
    onSurfaceVariant = DarkWoodLight_OnSurfaceVariant,
    outline = DarkWoodLight_Outline,
    error = DarkWoodLight_Error,
    onError = DarkWoodLight_OnError,
    errorContainer = DarkWoodLight_ErrorContainer,
    onErrorContainer = DarkWoodLight_OnErrorContainer,
)

// ---------------------------------------------------------------------------
// LICHESS schemes
// ---------------------------------------------------------------------------

private val LichessDarkColorScheme = darkColorScheme(
    primary = Lichess_Primary,
    onPrimary = Lichess_OnPrimary,
    primaryContainer = Lichess_PrimaryContainer,
    onPrimaryContainer = Lichess_OnPrimaryContainer,
    secondary = Lichess_Secondary,
    onSecondary = Lichess_OnSecondary,
    secondaryContainer = Lichess_SecondaryContainer,
    onSecondaryContainer = Lichess_OnSecondaryContainer,
    tertiary = Lichess_Tertiary,
    onTertiary = Lichess_OnTertiary,
    tertiaryContainer = Lichess_TertiaryContainer,
    onTertiaryContainer = Lichess_OnTertiaryContainer,
    background = Lichess_Background,
    onBackground = Lichess_OnBackground,
    surface = Lichess_Surface,
    onSurface = Lichess_OnSurface,
    surfaceVariant = Lichess_SurfaceVariant,
    onSurfaceVariant = Lichess_OnSurfaceVariant,
    outline = Lichess_Outline,
    error = Lichess_Error,
    onError = Lichess_OnError,
    errorContainer = Lichess_ErrorContainer,
    onErrorContainer = Lichess_OnErrorContainer,
)

private val LichessLightColorScheme = lightColorScheme(
    primary = LichessLight_Primary,
    onPrimary = LichessLight_OnPrimary,
    primaryContainer = LichessLight_PrimaryContainer,
    onPrimaryContainer = LichessLight_OnPrimaryContainer,
    secondary = LichessLight_Secondary,
    onSecondary = LichessLight_OnSecondary,
    secondaryContainer = LichessLight_SecondaryContainer,
    onSecondaryContainer = LichessLight_OnSecondaryContainer,
    tertiary = LichessLight_Tertiary,
    onTertiary = LichessLight_OnTertiary,
    tertiaryContainer = LichessLight_TertiaryContainer,
    onTertiaryContainer = LichessLight_OnTertiaryContainer,
    background = LichessLight_Background,
    onBackground = LichessLight_OnBackground,
    surface = LichessLight_Surface,
    onSurface = LichessLight_OnSurface,
    surfaceVariant = LichessLight_SurfaceVariant,
    onSurfaceVariant = LichessLight_OnSurfaceVariant,
    outline = LichessLight_Outline,
    error = LichessLight_Error,
    onError = LichessLight_OnError,
    errorContainer = LichessLight_ErrorContainer,
    onErrorContainer = LichessLight_OnErrorContainer,
)

// ---------------------------------------------------------------------------
// WARM_LIGHT schemes
// ---------------------------------------------------------------------------

private val WarmLightLightColorScheme = lightColorScheme(
    primary = WarmLight_Primary,
    onPrimary = WarmLight_OnPrimary,
    primaryContainer = WarmLight_PrimaryContainer,
    onPrimaryContainer = WarmLight_OnPrimaryContainer,
    secondary = WarmLight_Secondary,
    onSecondary = WarmLight_OnSecondary,
    secondaryContainer = WarmLight_SecondaryContainer,
    onSecondaryContainer = WarmLight_OnSecondaryContainer,
    tertiary = WarmLight_Tertiary,
    onTertiary = WarmLight_OnTertiary,
    tertiaryContainer = WarmLight_TertiaryContainer,
    onTertiaryContainer = WarmLight_OnTertiaryContainer,
    background = WarmLight_Background,
    onBackground = WarmLight_OnBackground,
    surface = WarmLight_Surface,
    onSurface = WarmLight_OnSurface,
    surfaceVariant = WarmLight_SurfaceVariant,
    onSurfaceVariant = WarmLight_OnSurfaceVariant,
    outline = WarmLight_Outline,
    error = WarmLight_Error,
    onError = WarmLight_OnError,
    errorContainer = WarmLight_ErrorContainer,
    onErrorContainer = WarmLight_OnErrorContainer,
)

private val WarmLightDarkColorScheme = darkColorScheme(
    primary = WarmLightDark_Primary,
    onPrimary = WarmLightDark_OnPrimary,
    primaryContainer = WarmLightDark_PrimaryContainer,
    onPrimaryContainer = WarmLightDark_OnPrimaryContainer,
    secondary = WarmLightDark_Secondary,
    onSecondary = WarmLightDark_OnSecondary,
    secondaryContainer = WarmLightDark_SecondaryContainer,
    onSecondaryContainer = WarmLightDark_OnSecondaryContainer,
    tertiary = WarmLightDark_Tertiary,
    onTertiary = WarmLightDark_OnTertiary,
    tertiaryContainer = WarmLightDark_TertiaryContainer,
    onTertiaryContainer = WarmLightDark_OnTertiaryContainer,
    background = WarmLightDark_Background,
    onBackground = WarmLightDark_OnBackground,
    surface = WarmLightDark_Surface,
    onSurface = WarmLightDark_OnSurface,
    surfaceVariant = WarmLightDark_SurfaceVariant,
    onSurfaceVariant = WarmLightDark_OnSurfaceVariant,
    outline = WarmLightDark_Outline,
    error = WarmLightDark_Error,
    onError = WarmLightDark_OnError,
    errorContainer = WarmLightDark_ErrorContainer,
    onErrorContainer = WarmLightDark_OnErrorContainer,
)

// ---------------------------------------------------------------------------
// Theme entry point
// ---------------------------------------------------------------------------

@Composable
fun ChessRepertoireTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appColorTheme: AppColorTheme = AppColorTheme.DARK_WOOD,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (appColorTheme) {
            AppColorTheme.DARK_WOOD  -> if (darkTheme) DarkWoodDarkColorScheme  else DarkWoodLightColorScheme
            AppColorTheme.LICHESS    -> if (darkTheme) LichessDarkColorScheme   else LichessLightColorScheme
            AppColorTheme.WARM_LIGHT -> if (darkTheme) WarmLightDarkColorScheme else WarmLightLightColorScheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
