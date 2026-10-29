package com.jamiecarl.droneside.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

private val DronesideColorScheme = darkColorScheme(
    primary = DsCyan,
    onPrimary = DsOnCyan,
    background = DsBase,
    onBackground = DsInk,
    surface = DsSurface,
    onSurface = DsInk,
    surfaceVariant = DsSurface2,
    onSurfaceVariant = DsMuted,
    outline = DsLine,
    error = DsDnf
)

private fun TextStyle.inter() = copy(fontFamily = Inter)

private val DronesideTypography = Typography().run {
    copy(
        displayLarge = displayLarge.inter(), displayMedium = displayMedium.inter(), displaySmall = displaySmall.inter(),
        headlineLarge = headlineLarge.inter(), headlineMedium = headlineMedium.inter(), headlineSmall = headlineSmall.inter(),
        titleLarge = titleLarge.inter(), titleMedium = titleMedium.inter(), titleSmall = titleSmall.inter(),
        bodyLarge = bodyLarge.inter(), bodyMedium = bodyMedium.inter(), bodySmall = bodySmall.inter(),
        labelLarge = labelLarge.inter(), labelMedium = labelMedium.inter(), labelSmall = labelSmall.inter()
    )
}

@Composable
fun DronesideTheme(content: @Composable () -> Unit) {
    // The app is always dark — matches the original design, which never offered a light mode.
    MaterialTheme(
        colorScheme = DronesideColorScheme,
        typography = DronesideTypography,
        content = content
    )
}
