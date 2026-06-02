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

// 皮肤枚举
enum class Skin(val label: String, val isDark: Boolean) {
    DEFAULT_DARK("默认深色", true),
    PEARL_WHITE("珍珠白", false)
}

// 原有深色配色
private val DefaultDarkColorScheme = darkColorScheme(
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

// 原有浅色配色
private val DefaultLightColorScheme = lightColorScheme(
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

// 珍珠白配色
private val PearlWhiteColorScheme = lightColorScheme(
    primary = PearlPrimary,
    onPrimary = Color.White,
    primaryContainer = PearlPrimaryDark,
    background = PearlBackground,
    surface = PearlSurface,
    surfaceVariant = PearlSurfaceVariant,
    onBackground = PearlOnBackground,
    onSurface = PearlOnSurface,
    onSurfaceVariant = PearlOnSurfaceVariant,
    outline = PearlBorder,
    error = PearlPrimary
)

@Composable
fun LeChenMusicTheme(
    darkTheme: Boolean = true,
    skinName: String = "default",
    content: @Composable () -> Unit
) {
    val colorScheme = when (skinName) {
        "pearl_white" -> PearlWhiteColorScheme
        else -> if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                // 珍珠白是浅色皮肤，状态栏用深色图标
                val isLightSkin = skinName == "pearl_white" || !darkTheme
                isAppearanceLightStatusBars = isLightSkin
                isAppearanceLightNavigationBars = isLightSkin
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
