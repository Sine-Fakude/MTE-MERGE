package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ThemeMode

// ==========================================
// Material 3 Dark Color Scheme (Electric Cyan & Cyber Magenta)
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF050811),
    primaryContainer = Color(0xFF042F38),
    onPrimaryContainer = ElectricCyan,
    secondary = CyberMagenta,
    onSecondary = Color(0xFF050811),
    secondaryContainer = Color(0xFF4A004A),
    onSecondaryContainer = CyberMagenta,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF003865),
    onTertiaryContainer = ElectricBlue,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

// ==========================================
// Material 3 Light Color Scheme (Liquid Glass)
// ==========================================
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0091EA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = Color(0xFFC51162),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color(0xFF880E4F),
    tertiary = Color(0xFF6200EA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF311B92),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight
)

@Composable
fun MteMergeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ==========================================
// Liquid Glass & Refined Neon Glow Modifiers
// ==========================================

/**
 * Applies a true Liquid Glass effect with high translucency (0.4f light / 0.5f dark)
 * so background content and wallpaper cleanly shine through.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    isDark: Boolean? = null,
    borderAlpha: Float = 0.20f,
    surfaceAlpha: Float? = null
): Modifier = composed {
    val darkMode = isDark ?: isSystemInDarkTheme()

    val surfaceColor = if (darkMode) {
        Color(0xFF0B132B).copy(alpha = surfaceAlpha ?: 0.50f)
    } else {
        Color.White.copy(alpha = surfaceAlpha ?: 0.40f)
    }

    val borderBrush = if (darkMode) {
        Brush.linearGradient(
            colors = listOf(
                ElectricCyan.copy(alpha = borderAlpha * 0.9f),
                Color.White.copy(alpha = borderAlpha * 0.3f),
                CyberMagenta.copy(alpha = borderAlpha * 0.6f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                ElectricCyan.copy(alpha = borderAlpha * 0.8f),
                Color.White.copy(alpha = 0.8f),
                CyberMagenta.copy(alpha = borderAlpha * 0.5f)
            )
        )
    }

    this
        .clip(shape)
        .background(surfaceColor, shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}

/**
 * Refined edge-lighting glow modifier.
 * Applies a delicate, low-alpha shadow (0.15f) and a crisp 1.dp colored border to simulate edge-lighting
 * without harsh blurred opaque boxes.
 */
fun Modifier.neonGlow(
    color: Color = ElectricCyan,
    radius: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.dp
): Modifier = this
    .shadow(
        elevation = radius,
        shape = shape,
        ambientColor = color.copy(alpha = 0.15f),
        spotColor = color.copy(alpha = 0.15f)
    )
    .border(
        width = borderWidth,
        color = color.copy(alpha = 0.35f),
        shape = shape
    )
