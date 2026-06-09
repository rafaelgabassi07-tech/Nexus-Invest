package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

import com.example.data.FontScale
import com.example.data.CornerStyle
import com.example.data.DarkVariant
import com.example.data.LightVariant

private fun createDarkColorScheme(darkVariant: DarkVariant) = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldPrimary.copy(alpha = 0.8f),
    tertiary = GoldPrimary.copy(alpha = 0.6f),
    background = when (darkVariant) {
        DarkVariant.OLED -> Color.Black
        DarkVariant.DEEP_SEA -> Color(0xFF020914)
        else -> DarkBackground
    },
    surface = when (darkVariant) {
        DarkVariant.OLED -> Color.Black
        DarkVariant.DEEP_SEA -> Color(0xFF061426)
        else -> DarkSurface
    },
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceVariant = when (darkVariant) {
        DarkVariant.OLED -> Color(0xFF101010)
        DarkVariant.DEEP_SEA -> Color(0xFF0C2038)
        else -> DarkSurfaceElevated
    },
    primaryContainer = when (darkVariant) {
        DarkVariant.OLED -> Color(0xFF121212)
        DarkVariant.DEEP_SEA -> Color(0xFF0C2038)
        else -> DarkSurfaceElevated
    },
    onPrimaryContainer = TextPrimary,
    outline = GoldPrimary.copy(alpha = 0.2f),
    error = DangerRed
)

private fun createLightColorScheme(lightVariant: LightVariant) = lightColorScheme(
    primary = GoldPrimary,
    secondary = GoldPrimary.copy(alpha = 0.8f),
    tertiary = GoldPrimary.copy(alpha = 0.6f),
    background = when(lightVariant) {
        LightVariant.CLASSIC -> LightBackground
        LightVariant.VALOR_GOLD -> Color(0xFFFFFDF5)
        LightVariant.IVORY_CREAM -> Color(0xFFFAF7F2)
    },
    surface = when(lightVariant) {
        LightVariant.CLASSIC -> LightSurface
        LightVariant.VALOR_GOLD -> Color(0xFFFFFBEA)
        LightVariant.IVORY_CREAM -> Color(0xFFFFFBF7)
    },
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceVariant = when(lightVariant) {
        LightVariant.CLASSIC -> LightSurfaceElevated
        LightVariant.VALOR_GOLD -> Color(0xFFFEF3C7)
        LightVariant.IVORY_CREAM -> Color(0xFFEFECE6)
    },
    primaryContainer = when(lightVariant) {
        LightVariant.CLASSIC -> LightSurfaceElevated
        LightVariant.VALOR_GOLD -> Color(0xFFFEF3C7)
        LightVariant.IVORY_CREAM -> Color(0xFFEFECE6)
    },
    onPrimaryContainer = TextPrimaryLight,
    outline = GoldPrimary.copy(alpha = 0.15f),
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: FontScale = FontScale.MEDIUM,
    cornerStyle: CornerStyle = CornerStyle.MODERN,
    darkVariant: DarkVariant = DarkVariant.CARBON,
    lightVariant: LightVariant = LightVariant.CLASSIC,
    content: @Composable () -> Unit,
) {
    isDarkThemeGlobal = darkTheme
    activeDarkVariantGlobal = darkVariant
    val colorScheme = if (darkTheme) createDarkColorScheme(darkVariant) else createLightColorScheme(lightVariant)

    val scaledTypography = Typography(
        displayLarge = Typography().displayLarge.scale(fontScale.scale),
        displayMedium = Typography().displayMedium.scale(fontScale.scale),
        displaySmall = Typography().displaySmall.scale(fontScale.scale),
        headlineLarge = Typography().headlineLarge.scale(fontScale.scale),
        headlineMedium = Typography().headlineMedium.scale(fontScale.scale),
        headlineSmall = Typography().headlineSmall.scale(fontScale.scale),
        titleLarge = Typography().titleLarge.scale(fontScale.scale),
        titleMedium = Typography().titleMedium.scale(fontScale.scale),
        titleSmall = Typography().titleSmall.scale(fontScale.scale),
        bodyLarge = Typography().bodyLarge.scale(fontScale.scale),
        bodyMedium = Typography().bodyMedium.scale(fontScale.scale),
        bodySmall = Typography().bodySmall.scale(fontScale.scale),
        labelLarge = Typography().labelLarge.scale(fontScale.scale),
        labelMedium = Typography().labelMedium.scale(fontScale.scale),
        labelSmall = Typography().labelSmall.scale(fontScale.scale)
    )

    val shapes = Shapes(
        small = RoundedCornerShape(cornerStyle.radius.dp / 2),
        medium = RoundedCornerShape(cornerStyle.radius.dp),
        large = RoundedCornerShape(cornerStyle.radius.dp * 1.5f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        shapes = shapes,
        content = content
    )
}

private fun TextStyle.scale(scaleFactor: Float): TextStyle {
    return this.copy(
        fontSize = if (this.fontSize.isSp) this.fontSize * scaleFactor else this.fontSize,
        lineHeight = if (this.lineHeight.isSp) this.lineHeight * scaleFactor else this.lineHeight,
        letterSpacing = if (this.letterSpacing.isSp) this.letterSpacing * scaleFactor else this.letterSpacing
    )
}
