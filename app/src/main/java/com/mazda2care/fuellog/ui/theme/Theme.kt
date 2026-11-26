package com.mazda2care.fuellog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ngôn ngữ thiết kế Mazda KODO Cockpit & High-Contrast Outdoor Readability
val FuelOrange = Color(0xFFD9381E) // Mazda Soul Red / Sporty Amber-Red có độ tương phản cao dưới nắng
val PetrolBlue = Color(0xFF0284C7) // Cockpit Digital Blue sắc nét
val EcoGreen = Color(0xFF15803D)   // SkyActiv Eco Emerald Green
val CostPurple = Color(0xFF6D28D9) // Deep Royal Purple
val CockpitDark = Color(0xFF0F172A) // Carbon Slate Táp-lô Mazda Connect

private val LightColors = lightColorScheme(
    primary = FuelOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = PetrolBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF334155)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF1E3A8A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun Mazda2FuelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
