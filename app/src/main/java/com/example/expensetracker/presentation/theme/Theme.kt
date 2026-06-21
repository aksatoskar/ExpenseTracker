package com.example.expensetracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFFEA580C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF9A3412),
    background = Color(0xFFF8F5FF),
    onBackground = Color(0xFF1E1B2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B2E),
    surfaceVariant = Color(0xFFEDE8F5),
    onSurfaceVariant = Color(0xFF5B5470),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF9A3412),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF0F0D17),
    onBackground = Color(0xFFF3EEFF),
    surface = Color(0xFF1A1628),
    onSurface = Color(0xFFF3EEFF),
    surfaceVariant = Color(0xFF2D2640),
    onSurfaceVariant = Color(0xFFC9BFD9),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D)
)

/** App-wide Material 3 theme with a colorful purple/cyan palette and light/dark variants. */
@Composable
fun ExpenseTrackerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
