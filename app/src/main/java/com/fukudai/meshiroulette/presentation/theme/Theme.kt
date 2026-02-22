package com.fukudai.meshiroulette.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// iOS と統一したアプリカラー定数
private val AppPrimary = Color(0xFFFF8000)          // オレンジ (#FF8000)
private val AppPrimaryDark = Color(0xFFFFAB5E)       // ダークモード用オレンジ
private val AppPrimaryContainer = Color(0xFFFFE0C0)  // オレンジ薄め
private val AppOnPrimaryContainer = Color(0xFF4A1800)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    secondary = Color(0xFF775A40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDBB),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF5C6023),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E59C),
    onTertiaryContainer = Color(0xFF1A1D00),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3E0CC),
    onSurfaceVariant = Color(0xFF52443A)
)

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimaryDark,
    onPrimary = Color(0xFF4A1800),
    primaryContainer = Color(0xFF6B2E00),
    onPrimaryContainer = AppPrimaryContainer,
    secondary = Color(0xFFE6BF9A),
    onSecondary = Color(0xFF432B13),
    secondaryContainer = Color(0xFF5C4128),
    onSecondaryContainer = Color(0xFFFFDDBB),
    tertiary = Color(0xFFC5C982),
    onTertiary = Color(0xFF2E3100),
    tertiaryContainer = Color(0xFF44480B),
    onTertiaryContainer = Color(0xFFE2E59C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF52443A),
    onSurfaceVariant = Color(0xFFD7C3B5)
)

@Composable
fun MeshiRouletteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
