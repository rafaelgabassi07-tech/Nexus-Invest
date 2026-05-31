package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// Global theme state for dynamic color getters (automatically triggers recomposition)
var isDarkThemeGlobal by mutableStateOf(true)

// Premium Luxury Slate & Gold Theme Color definitions
val DarkBackground: Color
    get() = if (isDarkThemeGlobal) Color(0xFF0F1115) else Color(0xFFF9FAFB)

val DarkSurface: Color
    get() = if (isDarkThemeGlobal) Color(0xFF191D24) else Color(0xFFFFFFFF)

val DarkSurfaceElevated: Color
    get() = if (isDarkThemeGlobal) Color(0xFF222731) else Color(0xFFF3F4F6)

val GoldPrimary = Color(0xFFFBBF24) // Gold / Amber
val GoldSecondary = Color(0xFFF59E0B) // Dark Amber
val GoldTertiary = Color(0xFFD97706)
val GoldPale = Color(0xFFFEF08A) // Light Pale Yellow Gold
val GoldBronze = Color(0xFFB45309) // Bronze Gold
val GoldDeep = Color(0xFF92400E) // Deep Earth Gold

val SuccessGreen = Color(0xFF10B981) // Emerald Green for positive numbers
val DangerRed = Color(0xFFEF4444) // Red for negative numbers
val WarningOrange = Color(0xFFF97316) // Orange for warnings

val TextPrimary: Color
    get() = if (isDarkThemeGlobal) Color(0xFFF3F4F6) else Color(0xFF111827)

val TextSecondary: Color
    get() = if (isDarkThemeGlobal) Color(0xFF9CA3AF) else Color(0xFF4B5563)

val BorderColor: Color
    get() = if (isDarkThemeGlobal) Color(0xFF2D3748) else Color(0xFFE5E7EB)

val DarkGray: Color
    get() = if (isDarkThemeGlobal) Color(0xFF4A5568) else Color(0xFF9CA3AF)

// Light Mode equivalents
val LightBackground = Color(0xFFF9FAFB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF3F4F6)
val TextPrimaryLight = Color(0xFF111827)
val TextSecondaryLight = Color(0xFF4B5563)
val BorderColorLight = Color(0xFFE5E7EB)
