package com.hoosha.examai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3155A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE2FF),
    onPrimaryContainer = Color(0xFF001A42),
    secondary = Color(0xFF575E71),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1B1B1F),
    onSurface = Color(0xFF1B1B1F),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    onPrimary = Color(0xFF002C6B),
    primaryContainer = Color(0xFF17448E),
    onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFFBFC6DC),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    onBackground = Color(0xFFE3E2E9),
    onSurface = Color(0xFFE3E2E9),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ExamAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}