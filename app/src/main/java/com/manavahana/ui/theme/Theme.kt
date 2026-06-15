package com.manavahana.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Custom Themes visual profiles matching our template images
private val DarkTechColorScheme = darkColorScheme(
    primary = Color(0xFFFF5722), // Neon tech orange active line
    secondary = Color(0xFF00E5FF), // Cyber digital blue element
    tertiary = Color(0xFFFFD600), // Alert warning instrument yellow
    background = Color(0xFF0C0C0E), // Stealth steel black slate 
    surface = Color(0xFF141419), // Steel card capsule
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFECEFF1), // Silver metallic grey text
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF1F2026),
    onSurfaceVariant = Color(0xFF90A4AE),
    error = Color(0xFFFF1744),
    errorContainer = Color(0xFF2C0B0E),
    onErrorContainer = Color(0xFFFF8A80)
)

private val EcoMinimalColorScheme = lightColorScheme(
    primary = Color(0xFFB45309), // Earth terracotta clay premium warmth
    secondary = Color(0xFF14532D), // Sage deep forest organic green
    tertiary = Color(0xFFF59E0B), // Sunflower yellow active markers
    background = Color(0xFFFAF7F2), // Soft ivory cream clay base
    surface = Color(0xFFFFFFFF), // Pill white surfaces
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1917), // Soft earthy stone text
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF3EFE9),
    onSurfaceVariant = Color(0xFF78716C),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B)
)

private val RetroCruiseColorScheme = lightColorScheme(
    primary = Color(0xFFFF6D00), // Tangerine old school octane orange
    secondary = Color(0xFF1A237E), // Gas garage retro deep navy blue
    tertiary = Color(0xFF00C853), // Neon retro lane route green
    background = Color(0xFFF1EDE4), // Vellum paper parchment board
    surface = Color(0xFFFFFDF9), // Vintage white fuel sheets
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0A0F1D), // Inked blueprint letter text
    onSurface = Color(0xFF0A0F1D),
    surfaceVariant = Color(0xFFE5DECE), // Craft cardboard folder cream
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFD50000),
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C)
)

private val SportCarbonColorScheme = darkColorScheme(
    primary = Color(0xFFD32F2F), // Racing crimson red
    secondary = Color(0xFF90A4AE), // Metal mechanical steel grey
    tertiary = Color(0xFF00E676), // Nitrous green
    background = Color(0xFF09090B), // Carbon pit black
    surface = Color(0xFF121216), // Metal brake pad slate
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF1C1C22),
    onSurfaceVariant = Color(0xFFA1A1AA),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val RoyalGoldColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700), // Rich royal liquid gold
    secondary = Color(0xFF1E293B), // Premium midnight indigo slate
    tertiary = Color(0xFFD4AF37), // Heritage metal brass gold
    background = Color(0xFF0F172A), // Royal dark navy
    surface = Color(0xFF1E293B), // Luxury plush panel
    onPrimary = Color(0xFF12110D), // Solid ink black text
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

private val EcoMinimalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

private val RetroCruiseShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp)
)

private val CyberDarkTechShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp)
)

private val SportCarbonShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

private val RoyalGoldShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp)
)

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
    themeMode: String = "system",
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to enforce Telugu aesthetics
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "dark_tech" -> DarkTechColorScheme
        "eco_minimal" -> EcoMinimalColorScheme
        "retro_cruise" -> RetroCruiseColorScheme
        "sport_carbon" -> SportCarbonColorScheme
        "royal_gold" -> RoyalGoldColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val shapes = when (themeMode) {
        "dark_tech" -> CyberDarkTechShapes
        "eco_minimal" -> EcoMinimalShapes
        "retro_cruise" -> RetroCruiseShapes
        "sport_carbon" -> SportCarbonShapes
        "royal_gold" -> RoyalGoldShapes
        else -> MaterialTheme.shapes // uses default
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
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
