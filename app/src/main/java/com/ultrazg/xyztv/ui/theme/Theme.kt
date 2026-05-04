package com.ultrazg.xyztv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF0F160F),
    secondary = PrimaryDarkVariant,
    tertiary = PrimaryDarkVariant,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    border = OutlineDark
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LightColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    secondary = PrimaryLightVariant,
    tertiary = PrimaryLightVariant,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    border = OutlineLight
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XyzTvTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = com.ultrazg.xyztv.data.DarkModeManager.isDarkMode
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
