package com.zaid01.slateflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val Signature = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF152D50),
    primaryContainer = Color(0xFF263E5D), onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFFB6C5D9), secondaryContainer = Color(0xFF303D4F),
    background = Color(0xFF171A20), onBackground = Color(0xFFE2E5EC),
    surface = Color(0xFF1F232C), onSurface = Color(0xFFE2E5EC),
    surfaceVariant = Color(0xFF2C323E), onSurfaceVariant = Color(0xFFB2BBCB),
    outline = Color(0xFF798494), outlineVariant = Color(0xFF404956)
)
@Composable
fun SlateFlowTheme(dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (dynamicColor) dynamicDarkColorScheme(LocalContext.current).copy(
        background = Signature.background, surface = Signature.surface
    ) else Signature
    MaterialTheme(colorScheme = scheme, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp)), content = content)
}
