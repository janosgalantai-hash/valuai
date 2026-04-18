package com.valuai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ValuAIDarkColorScheme = darkColorScheme(
    primary          = GoldPrimary,
    onPrimary        = BackgroundDark,
    secondary        = GoldLight,
    onSecondary      = BackgroundDark,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDark,
    onSurfaceVariant = TextSecondary,
    error            = StatusBad,
    onError          = TextPrimary,
)

@Composable
fun ValuAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ValuAIDarkColorScheme,
        typography  = ValuAITypography,
        content     = content
    )
}