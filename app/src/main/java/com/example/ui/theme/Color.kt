package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Unified Core Neon Color Palette
// Primary Accents: Electric Cyan & Cyber Magenta
// ==========================================
val ElectricCyan = Color(0xFF00E5FF)
val CyberMagenta = Color(0xFFFF00FF)

// Semantic Theme Accents & Aliases
val NeonCyan = ElectricCyan
val NeonPink = CyberMagenta
val ElectricBlue = Color(0xFF00B0FF)
val CyberPurple = Color(0xFFD500F9)

// ==========================================
// Smooth Unified Neon Linear Gradients
// ==========================================
val GradientCyanToMagenta = Brush.horizontalGradient(
    colors = listOf(ElectricCyan, CyberMagenta)
)

val GradientMagentaToCyan = Brush.horizontalGradient(
    colors = listOf(CyberMagenta, ElectricCyan)
)

// Legacy alias pointing to unified gradient
val GradientCosmicHero = GradientCyanToMagenta

// ==========================================
// Liquid Glass Surfaces - Light Mode (Translucent)
// ==========================================
val BackgroundLight = Color(0xFFF1F5F9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE2E8F0)
val OutlineLight = Color(0xFFCBD5E1)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// ==========================================
// Liquid Glass Surfaces - Dark Mode (Translucent)
// ==========================================
val BackgroundDark = Color(0xFF050811)
val SurfaceDark = Color(0xFF0B132B)
val SurfaceVariantDark = Color(0xFF111D38)
val OutlineDark = Color(0xFF1E293B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
