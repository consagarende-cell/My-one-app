package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SafaDarkColorScheme = darkColorScheme(
    primary = SafaEmerald,
    onPrimary = Color.White,
    primaryContainer = SafaForest,
    onPrimaryContainer = SafaMintLight,
    secondary = SafaGold,
    onSecondary = Color(0xFF1F1400),
    secondaryContainer = Color(0xFF4A3100),
    onSecondaryContainer = SafaGoldContainer,
    tertiary = SafaTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0A3D38),
    onTertiaryContainer = SafaTealContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF3F0A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = SafaRedContainer,
)

private val SafaLightColorScheme = lightColorScheme(
    primary = SafaEmerald,
    onPrimary = Color.White,
    primaryContainer = SafaMintLight,
    onPrimaryContainer = SafaDeepPine,
    secondary = SafaGold,
    onSecondary = Color(0xFF231600),
    secondaryContainer = SafaGoldContainer,
    onSecondaryContainer = Color(0xFF452800),
    tertiary = SafaTeal,
    onTertiary = Color.White,
    tertiaryContainer = SafaTealContainer,
    onTertiaryContainer = Color(0xFF042F2E),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = SafaRed,
    onError = Color.White,
    errorContainer = SafaRedContainer,
    onErrorContainer = Color(0xFF7F1D1D),
)

val SafaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SafaOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SafaDarkColorScheme else SafaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SafaTypography,
        shapes = SafaShapes,
        content = content
    )
}
