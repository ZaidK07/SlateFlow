package com.zaid01.slateflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaid01.slateflow.data.ThemeMode

val NexusLight = lightColorScheme(
    primary = Color(0xFF2E7D32), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4E9), onPrimaryContainer = Color(0xFF256629),
    secondary = Color(0xFF0A6EBD), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F0FE), onSecondaryContainer = Color(0xFF08497D),
    tertiary = Color(0xFF7142A0), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E8F8), onTertiaryContainer = Color(0xFF563078),
    background = Color(0xFFF5F6FA), onBackground = Color(0xFF1F2A3C),
    surface = Color.White, onSurface = Color(0xFF1F2A3C),
    surfaceVariant = Color(0xFFEEF0F4), onSurfaceVariant = Color(0xFF5A6675),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8F9FC),
    surfaceContainer = Color(0xFFEEF0F4), surfaceContainerHigh = Color(0xFFE7EAF0),
    surfaceContainerHighest = Color(0xFFDEE3EC),
    outline = Color(0xFF748095), outlineVariant = Color(0xFFD8DEE8),
    error = Color(0xFFC72D4E), onError = Color.White,
    errorContainer = Color(0xFFFDE6EA), onErrorContainer = Color(0xFFA6203D),
)

val NexusDark = darkColorScheme(
    primary = Color(0xFF99D5A0), onPrimary = Color(0xFF13391C),
    primaryContainer = Color(0xFF233E2B), onPrimaryContainer = Color(0xFFC7EDCD),
    secondary = Color(0xFF9DCDFA), onSecondary = Color(0xFF073352),
    secondaryContainer = Color(0xFF213B53), onSecondaryContainer = Color(0xFFD0E7FF),
    tertiary = Color(0xFFD3B5EC), onTertiary = Color(0xFF402458),
    tertiaryContainer = Color(0xFF3C2E4C), onTertiaryContainer = Color(0xFFEEDAFF),
    background = Color(0xFF171C24), onBackground = Color(0xFFE3E9F2),
    surface = Color(0xFF202732), onSurface = Color(0xFFE3E9F2),
    surfaceVariant = Color(0xFF2C3543), onSurfaceVariant = Color(0xFFBAC5D5),
    surfaceContainerLowest = Color(0xFF141920), surfaceContainerLow = Color(0xFF1C232D),
    surfaceContainer = Color(0xFF242C38), surfaceContainerHigh = Color(0xFF2C3543),
    surfaceContainerHighest = Color(0xFF354050),
    outline = Color(0xFF8D9AAE), outlineVariant = Color(0xFF465367),
    error = Color(0xFFFFB1C1), onError = Color(0xFF64152A),
    errorContainer = Color(0xFF532633), onErrorContainer = Color(0xFFFFD9E1),
)

@Composable
fun isReaderDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SlateFlowTheme(dynamicColor: Boolean = false, themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = isReaderDark(themeMode)
    val base = if (dark) NexusDark else NexusLight
    val scheme = if (dynamicColor) {
        val dynamic = if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        // Keep Nexus's neutral surfaces; system colors supply the accents.
        base.copy(
            primary = dynamic.primary, onPrimary = dynamic.onPrimary,
            primaryContainer = dynamic.primaryContainer, onPrimaryContainer = dynamic.onPrimaryContainer,
            secondary = dynamic.secondary, onSecondary = dynamic.onSecondary,
            secondaryContainer = dynamic.secondaryContainer, onSecondaryContainer = dynamic.onSecondaryContainer,
            tertiary = dynamic.tertiary, onTertiary = dynamic.onTertiary,
            tertiaryContainer = dynamic.tertiaryContainer, onTertiaryContainer = dynamic.onTertiaryContainer,
        )
    } else base
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp),
        ), content = content,
    )
}
