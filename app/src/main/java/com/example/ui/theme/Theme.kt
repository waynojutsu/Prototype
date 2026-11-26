package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VarsityDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepCyanNavy,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = IceCyanText,
    secondary = KalahariAmber,
    onSecondary = DeepAmberInk,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = WarmAmberText,
    tertiary = EmeraldPula,
    onTertiary = DeepEmeraldInk,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = MintPulaText,
    background = ObsidianVoid,
    onBackground = CrispSnow,
    surface = MidnightSlate,
    onSurface = CrispSnow,
    surfaceVariant = CarbonSurface,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = MidnightSlate,
    surfaceContainer = CarbonSurface,
    surfaceContainerHigh = ElevatedCarbon,
    surfaceContainerHighest = HighestCarbon,
    outline = HairlineBorder,
    outlineVariant = SubtleSlateText,
    error = CoralCrimson,
    onError = DeepCrimsonInk,
    errorContainer = CrimsonContainer,
    onErrorContainer = SoftRoseText
)

private val VarsityShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun VarsityBwTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VarsityDarkColorScheme,
        typography = VarsityTypography,
        shapes = VarsityShapes,
        content = content
    )
}
