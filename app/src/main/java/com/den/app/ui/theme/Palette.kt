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

private const val BG_LIGHT = 0xFFF6F6F9
private const val BG_DARK = 0xFF101014
private const val SURFACE_LIGHT = 0xFFFFFFFF
private const val SURFACE_DARK = 0xFF17171C
private const val ON_BG_LIGHT = 0xFF1B1B20
private const val ON_BG_DARK = 0xFFE9E9F0

private fun lighten(c: Color, f: Float) = lerp(c, Color.White, f)
private fun darken(c: Color, f: Float) = lerp(c, Color.Black, f)

fun colorSchemeFor(seed: Color, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return if (dark) {
        base.copy(
            background = Color(BG_DARK),
            onBackground = Color(ON_BG_DARK),
            surface = Color(BG_DARK),
            onSurface = Color(ON_BG_DARK),
            surfaceVariant = Color(SURFACE_DARK),
            onSurfaceVariant = lighten(Color(ON_BG_DARK), 0.35f),
            primary = lighten(seed, 0.28f),
            onPrimary = darken(seed, 0.55f),
            primaryContainer = darken(seed, 0.42f),
            onPrimaryContainer = lighten(seed, 0.45f),
            secondary = lerp(seed, Color(0xFFB0B7CE), 0.55f).let { lighten(it, 0.15f) },
            onSecondary = darken(seed, 0.35f),
            secondaryContainer = darken(seed, 0.28f),
            onSecondaryContainer = lighten(seed, 0.55f),
            tertiary = lighten(lerp(seed, Color(0xFF7FB4A9), 0.4f), 0.12f),
            onTertiary = darken(seed, 0.3f),
            tertiaryContainer = darken(lerp(seed, Color(0xFF7FB4A9), 0.4f), 0.3f),
            onTertiaryContainer = lighten(seed, 0.6f),
        )
    } else {
        base.copy(
            background = Color(BG_LIGHT),
            onBackground = Color(ON_BG_LIGHT),
            surface = Color(SURFACE_LIGHT),
            onSurface = Color(ON_BG_LIGHT),
            surfaceVariant = lighten(Color(BG_LIGHT), 0.55f),
            onSurfaceVariant = darken(Color(ON_BG_LIGHT), 0.45f),
            primary = darken(seed, 0.08f),
            onPrimary = Color.White,
            primaryContainer = lighten(seed, 0.82f),
            onPrimaryContainer = darken(seed, 0.5f),
            secondary = lerp(seed, Color(0xFF6D7283), 0.5f),
            onSecondary = Color.White,
            secondaryContainer = lighten(lerp(seed, Color(0xFF6D7283), 0.5f), 0.75f),
            onSecondaryContainer = darken(seed, 0.45f),
            tertiary = lerp(seed, Color(0xFF317158), 0.45f),
            onTertiary = Color.White,
            tertiaryContainer = lighten(lerp(seed, Color(0xFF317158), 0.45f), 0.8f),
            onTertiaryContainer = darken(seed, 0.42f),
        )
    }
}

fun colorForIndex(index: Int?): Color {
    if (index == null) return Color.Transparent
    val palette = PALETTES.getOrNull(index) ?: return Color.Transparent
    return palette.seed
}