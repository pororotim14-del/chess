package com.chessassistant.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = PastelGreen,
    secondary = Baige,
    error = EvilRed,
)

private val LightScheme = lightColorScheme(
    primary = ChessGreenDark,
    secondary = EvilRed,
    error = EvilRed,
)

@Composable
fun ChessAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}