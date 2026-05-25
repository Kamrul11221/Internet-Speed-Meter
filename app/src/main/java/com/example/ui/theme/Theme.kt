package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = SleekPrimary,
        secondary = SleekSecondary,
        background = SleekBg,
        surface = SleekSurface,
        onBackground = SleekTextMain,
        onSurface = SleekTextMain,
        onSurfaceVariant = SleekTextSub,
        outline = SleekBorder
    )

private val LightColorScheme =
    darkColorScheme( // Force dark theme globally since design requires dark mode + low battery consumption
        primary = SleekPrimary,
        secondary = SleekSecondary,
        background = SleekBg,
        surface = SleekSurface,
        onBackground = SleekTextMain,
        onSurface = SleekTextMain,
        onSurfaceVariant = SleekTextSub,
        outline = SleekBorder
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // default true for sleek dark style energy conservation
    dynamicColor: Boolean = false, // Disable dynamic color to enforce exact designed theme branding
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
