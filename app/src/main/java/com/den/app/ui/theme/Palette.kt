package com.den.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

data class AppPalette(
    val id: Int,
    val name: String,
    val seed: Color,
)

val PALETTES = listOf(
    AppPalette(0, "Indigo", Color(0xFF4F46E5)),
    AppPalette(1, "Midnight", Color(0xFF1E3A6F)),
    AppPalette(2, "Sky", Color(0xFF0E7490)),
    AppPalette(3, "Ocean", Color(0xFF0D5C75)),
    AppPalette(4, "Forest", Color(0xFF2E5E3B)),
    AppPalette(5, "Meadow", Color(0xFF3F7A44)),
    AppPalette(6, "Sunset", Color(0xFFC2410C)),
    AppPalette(7, "Flame", Color(0xFFD0342C)),
    AppPalette(8, "Rose", Color(0xFFB0245A)),
    AppPalette(9, "Plum", Color(0xFF7C3AED)),
    AppPalette(10, "Berry", Color(0xFF9D174D)),
    AppPalette(11, "Chocolate", Color(0xFF8B5E3C)),
    AppPalette(12, "Slate", Color(0xFF49566B)),
    AppPalette(13, "Pixel", Color(0xFFFFC400)),
)

private val Mint = Color(0xFF6BD6A7)
private val Lavender = Color(0xFFB3A7E8)
private val Amber = Color(0xFFE6A23C)
private val Danger = Color(0xFFE2564F)

private fun lighten(c: Color, f: Float) = lerp(c, Color.White, f)
private fun darken(c: Color, f: Float) = lerp(c, Color.Black, f)
private fun blend(c: Color, target: Color, f: Float) = lerp(c, target, f)

/** Brand primary leans toward soft mint while keeping the chosen theme seed's hue. */
fun brandPrimary(seed: Color): Color = lighten(blend(seed, Mint, 0.45f), 0.14f)

private object DenNeutrals {
    // Dark (spec)
    const val DARK_BACKGROUND = 0xFF0B0E12
    const val DARK_SURFACE = 0xFF11161D
    const val DARK_SURFACE_HIGH = 0xFF171D25
    const val DARK_SURFACE_VARIANT = 0xFF1D2530
    const val DARK_TEXT = 0xFFF4F6F8
    const val DARK_TEXT_SECONDARY = 0xFFAAB3C0
    const val DARK_TEXT_TERTIARY = 0xFF737E8C
    const val DARK_DIVIDER = 0xFF242C36
    const val DARK_OUTLINE = 0xFF4A5662

    // Light
    const val LIGHT_BACKGROUND = 0xFFF7F8FA
    const val LIGHT_SURFACE = 0xFFFFFFFF
    const val LIGHT_SURFACE_VARIANT = 0xFFEEF1F4
    const val LIGHT_TEXT = 0xFF181B21
    const val LIGHT_TEXT_SECONDARY = 0xFF5C6672
    const val LIGHT_DIVIDER = 0xFFE3E7EC
    const val LIGHT_OUTLINE = 0xFFC9CFD7
}

fun colorSchemeFor(seed: Color, dark: Boolean): ColorScheme {
    return if (dark) {
        val primary = brandPrimary(seed)
        darkColorScheme().copy(
            background = Color(DenNeutrals.DARK_BACKGROUND),
            onBackground = Color(DenNeutrals.DARK_TEXT),
            surface = Color(DenNeutrals.DARK_SURFACE),
            onSurface = Color(DenNeutrals.DARK_TEXT),
            surfaceTint = Color(DenNeutrals.DARK_SURFACE),
            surfaceVariant = Color(DenNeutrals.DARK_SURFACE_VARIANT),
            onSurfaceVariant = Color(DenNeutrals.DARK_TEXT_SECONDARY),
            surfaceContainerLowest = Color(DenNeutrals.DARK_BACKGROUND),
            surfaceContainerLow = Color(0xFF0E1319),
            surfaceContainer = Color(DenNeutrals.DARK_SURFACE),
            surfaceContainerHigh = Color(DenNeutrals.DARK_SURFACE_HIGH),
            surfaceContainerHighest = Color(DenNeutrals.DARK_SURFACE_VARIANT),
            outline = Color(DenNeutrals.DARK_OUTLINE),
            outlineVariant = Color(DenNeutrals.DARK_DIVIDER),
            primary = primary,
            onPrimary = Color(0xFF06281C),
            primaryContainer = blend(Color(DenNeutrals.DARK_SURFACE), Mint, 0.16f),
            onPrimaryContainer = lighten(Mint, 0.62f),
            inversePrimary = lighten(blend(seed, Mint, 0.45f), 0.32f),
            secondary = lighten(blend(seed, Lavender, 0.55f), 0.14f),
            onSecondary = Color(0xFF211A3A),
            secondaryContainer = blend(Color(DenNeutrals.DARK_SURFACE), Lavender, 0.16f),
            onSecondaryContainer = lighten(Lavender, 0.6f),
            tertiary = Amber,
            onTertiary = Color(0xFF3A2500),
            tertiaryContainer = Color(0xFF3E2E12),
            onTertiaryContainer = lighten(Amber, 0.55f),
            error = Danger,
            onError = Color(0xFF390A08),
            errorContainer = Color(0xFF4A2323),
            onErrorContainer = Color(0xFFF3B9B5),
            inverseSurface = Color(DenNeutrals.DARK_TEXT),
            inverseOnSurface = Color(DenNeutrals.DARK_SURFACE),
        )
    } else {
        val primary = darken(blend(seed, Mint, 0.4f), 0.1f)
        lightColorScheme().copy(
            background = Color(DenNeutrals.LIGHT_BACKGROUND),
            onBackground = Color(DenNeutrals.LIGHT_TEXT),
            surface = Color(DenNeutrals.LIGHT_SURFACE),
            onSurface = Color(DenNeutrals.LIGHT_TEXT),
            surfaceTint = Color(DenNeutrals.LIGHT_SURFACE),
            surfaceVariant = Color(DenNeutrals.LIGHT_SURFACE_VARIANT),
            onSurfaceVariant = Color(DenNeutrals.LIGHT_TEXT_SECONDARY),
            surfaceContainerLowest = Color(DenNeutrals.LIGHT_SURFACE),
            surfaceContainerLow = Color(0xFFF2F4F6),
            surfaceContainer = Color(DenNeutrals.LIGHT_BACKGROUND),
            surfaceContainerHigh = Color(DenNeutrals.LIGHT_SURFACE_VARIANT),
            surfaceContainerHighest = Color(0xFFE6EAEF),
            outline = Color(DenNeutrals.LIGHT_OUTLINE),
            outlineVariant = Color(DenNeutrals.LIGHT_DIVIDER),
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lighten(blend(seed, Mint, 0.4f), 0.82f),
            onPrimaryContainer = darken(blend(seed, Mint, 0.4f), 0.4f),
            inversePrimary = brandPrimary(seed),
            secondary = darken(blend(seed, Lavender, 0.55f), 0.12f),
            onSecondary = Color.White,
            secondaryContainer = lighten(Lavender, 0.72f),
            onSecondaryContainer = darken(Lavender, 0.5f),
            tertiary = darken(Amber, 0.08f),
            onTertiary = Color.White,
            tertiaryContainer = lighten(Amber, 0.74f),
            onTertiaryContainer = darken(Amber, 0.48f),
            error = Color(0xFFC1423B),
            onError = Color.White,
            errorContainer = Color(0xFFF8DAD8),
            onErrorContainer = Color(0xFF761711),
            inverseSurface = Color(0xFF1E232B),
            inverseOnSurface = Color(0xFFE9EDF2),
        )
    }
}

fun colorForIndex(index: Int?): Color {
    if (index == null) return Color.Transparent
    val palette = PALETTES.getOrNull(index) ?: return Color.Transparent
    return palette.seed
}