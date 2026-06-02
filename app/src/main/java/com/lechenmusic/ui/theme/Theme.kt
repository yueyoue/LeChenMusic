package com.lechenmusic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    error = DarkPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryDark,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    error = LightPrimary
)

/** 琉璃幻境 - 毛玻璃主题 */
private val GlassColorScheme = darkColorScheme(
    primary = GlassPrimary,
    onPrimary = Color.White,
    primaryContainer = GlassPrimaryDark,
    background = GlassBackground,
    surface = GlassSurface,
    surfaceVariant = GlassSurfaceVariant,
    onBackground = GlassOnBackground,
    onSurface = GlassOnSurface,
    onSurfaceVariant = GlassOnSurfaceVariant,
    outline = GlassBorder,
    error = GlassAccent
)

/**
 * 主题入口
 * @param themeMode "dark" | "light" | "glass"
 */
@Composable
fun LeChenMusicTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "light" -> LightColorScheme
        "glass" -> GlassColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                // glass 和 dark 都用深色状态栏
                isAppearanceLightStatusBars = themeMode == "light"
                isAppearanceLightNavigationBars = themeMode == "light"
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
