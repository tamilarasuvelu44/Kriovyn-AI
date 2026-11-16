package com.orbita.personalagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.orbita.personalagent.data.preferences.KriovynThemeMode

private val DeepNavyColorScheme = darkColorScheme(
    primary = KriovynBlueAccent,
    onPrimary = KriovynNavyBlack,
    primaryContainer = KriovynNavyElevated,
    onPrimaryContainer = KriovynTextPrimary,
    secondary = KriovynPurpleAccent,
    onSecondary = KriovynNavyBlack,
    secondaryContainer = ColorTokens.PurpleContainer,
    onSecondaryContainer = KriovynTextPrimary,
    tertiary = KriovynCyanMint,
    background = KriovynNavyBlack,
    onBackground = KriovynTextPrimary,
    surface = KriovynNavySurface,
    onSurface = KriovynTextPrimary,
    surfaceVariant = KriovynNavyCard,
    onSurfaceVariant = KriovynTextSecondary,
    outline = KriovynOutline,
    error = KriovynError,
    onError = KriovynNavyBlack
)

private val ObsidianColorScheme = DeepNavyColorScheme.copy(
    background = KriovynObsidian,
    surface = ColorTokens.ObsidianSurface,
    surfaceVariant = ColorTokens.ObsidianCard
)

private object ColorTokens {
    val PurpleContainer = Color(0xFF281F4A)
    val ObsidianSurface = Color(0xFF0A0D16)
    val ObsidianCard = Color(0xFF101524)
}

@Composable
fun KriovynTheme(
    themeMode: KriovynThemeMode = KriovynThemeMode.DEEP_NAVY,
    content: @Composable () -> Unit
) {
    val scheme = when (themeMode) {
        KriovynThemeMode.DEEP_NAVY -> DeepNavyColorScheme
        KriovynThemeMode.OBSIDIAN_OLED -> ObsidianColorScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = KriovynTypography,
        content = content
    )
}
