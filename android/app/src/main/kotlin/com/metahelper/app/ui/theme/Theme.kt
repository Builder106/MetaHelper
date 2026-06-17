package com.metahelper.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette (matches the banner/app-icon): indigo accent on a light surface.
// Light-only by design — no dark scheme. Colors chosen for WCAG-AA text contrast.
private val BrandColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),            // brand indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0B0F1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B0F1A),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF3F4356),   // ~8:1 on surfaceVariant
    outline = Color(0xFF565B6E),
)

@Composable
fun MetaHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandColorScheme,
        content = content
    )
}
