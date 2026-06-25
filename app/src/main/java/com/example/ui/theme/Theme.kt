package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentPureWhite,
    secondary = PrimarySilver,
    tertiary = SecondaryGray,
    background = TrueBlack,
    surface = CardBackground,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = PrimarySilver,
    onSurface = PrimarySilver,
    surfaceVariant = DividerColor,
    onSurfaceVariant = SecondaryGray
)

@Composable
fun RoozAraTheme(
    content: @Composable () -> Unit
) {
    // RoozAra is an ultra-minimal true dark theme app by design.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
