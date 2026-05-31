package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Natural Tones color mappings matching the design vibe
private val NaturalTerracotta = Color(0xFFB45309) // Warm terracotta / amber orange
private val NaturalForestGreen = Color(0xFF065F46) // Elegant deep forest green
private val NaturalSaddleBrown = Color(0xFF8B4513) // Saddle brown display tone

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B), // Warm deep amber
    secondary = Color(0xFF10B981), // Vivid forest emerald
    tertiary = Color(0xFFFDBA74), // Sand beach peach
    background = Color(0xFF12110E), // Safe warm earthy dark bark
    surface = Color(0xFF1D1C18), // Warm dark slate-bark surface
    onPrimary = Color(0xFF451A03),
    onSecondary = Color(0xFF022C22),
    onTertiary = Color(0xFF431407),
    onBackground = Color(0xFFFAF9F6),
    onSurface = Color(0xFFFAF9F6),
    surfaceVariant = Color(0xFF2E2A25),
    onSurfaceVariant = Color(0xFFC8BDB3),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalTerracotta, // #B45309 - Clay Turmeric Warmth
    secondary = NaturalForestGreen, // #065F46 - Deep Forest Jade
    tertiary = NaturalSaddleBrown, // #8B4513 - Saddle Wood Brown
    background = Color(0xFFFAF9F6), // Warm Ivory Alabaster Base
    surface = Color(0xFFFFFFFF), // Crisp Clean White Surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A), // Soft dark slate text
    onSurface = Color(0xFF0F172A), // Soft dark slate text
    surfaceVariant = Color(0xFFF1F5F9), // Light cold clay / elegant grey
    onSurfaceVariant = Color(0xFF475569), // Intermediate grey
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun ManaVahanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to enforce Telugu aesthetics
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep the alias, just in case some generated files or tests use it
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ManaVahanaTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
