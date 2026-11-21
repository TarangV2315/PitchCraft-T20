package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = TurfEmerald80,
    onPrimary = OnTurfEmerald20,
    primaryContainer = TurfEmeraldContainer30,
    onPrimaryContainer = OnTurfEmeraldContainer90,
    secondary = FloodlightGold80,
    onSecondary = OnFloodlightGold20,
    secondaryContainer = FloodlightGoldContainer30,
    onSecondaryContainer = OnFloodlightGoldContainer90,
    tertiary = TelemetryCyan80,
    onTertiary = OnTelemetryCyan20,
    tertiaryContainer = TelemetryCyanContainer30,
    onTertiaryContainer = OnTelemetryCyanContainer90,
    error = WicketCoral80,
    onError = OnWicketCoral20,
    errorContainer = WicketCoralContainer30,
    onErrorContainer = OnWicketCoralContainer90,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = TurfEmerald40,
    onPrimary = OnTurfEmerald100,
    primaryContainer = TurfEmeraldContainer90,
    onPrimaryContainer = OnTurfEmeraldContainer10,
    secondary = FloodlightGold40,
    onSecondary = OnFloodlightGold100,
    secondaryContainer = FloodlightGoldContainer90,
    onSecondaryContainer = OnFloodlightGoldContainer10,
    tertiary = TelemetryCyan40,
    onTertiary = OnTelemetryCyan100,
    tertiaryContainer = TelemetryCyanContainer90,
    onTertiaryContainer = OnTelemetryCyanContainer10,
    error = WicketCoral40,
    onError = OnWicketCoral100,
    errorContainer = WicketCoralContainer90,
    onErrorContainer = OnWicketCoralContainer10,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = OnTurfEmerald100,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val PitchCraftShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PitchCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        typography = PitchCraftTypography,
        shapes = PitchCraftShapes,
        content = content
    )
}
