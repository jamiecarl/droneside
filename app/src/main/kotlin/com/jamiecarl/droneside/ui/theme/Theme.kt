package com.jamiecarl.droneside.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val DronesideColorScheme = darkColorScheme(
    primary = DsCyan,
    onPrimary = DsBase,
    background = DsBase,
    onBackground = DsInk,
    surface = DsSurface,
    onSurface = DsInk,
    surfaceVariant = DsSurface2,
    onSurfaceVariant = DsMuted,
    outline = DsLine,
    error = DsDnf
)

private val DronesideTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, color = DsInk),
    bodyMedium = TextStyle(fontSize = 14.sp, color = DsInk2),
    bodySmall = TextStyle(fontSize = 12.sp, color = DsMuted)
)

@Composable
fun DronesideTheme(content: @Composable () -> Unit) {
    // The app is always dark — matches the original design, which never offered a light mode.
    MaterialTheme(
        colorScheme = DronesideColorScheme,
        typography = DronesideTypography,
        content = content
    )
}
