package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Standardized Dark ColorScheme built with our exact palette
private val PremiumDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    secondary = EmeraldSecondary,
    onSecondary = Color.Black,
    background = DarkBaseBg,
    onBackground = TextPureWhite,
    surface = DarkSurface,
    onSurface = TextPureWhite,
    error = DarkAccentAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force our glorious dark fintech look
    dynamicColor: Boolean = false, // Enforce brand identity colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
