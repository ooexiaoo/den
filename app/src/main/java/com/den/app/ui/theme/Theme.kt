package com.den.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.den.app.settings.Settings

/** Core spacing scale used across the app. */
object DenSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Elevation scale. The app leans on surface tones over shadows. */
object DenElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
}

val DenShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Base = Typography()

val DenTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    displayMedium = Base.displayMedium.copy(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    displaySmall = Base.displaySmall.copy(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineLarge = Base.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = Base.headlineMedium.copy(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = Base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleLarge = Base.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = Base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = Base.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
)

@Composable
fun DenTheme(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seed = PALETTES.getOrNull(settings.themeColorIndex)?.seed ?: Color(0xFF4F46E5)
            colorSchemeFor(seed, darkTheme)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DenTypography,
        shapes = DenShapes,
        content = content,
    )
}