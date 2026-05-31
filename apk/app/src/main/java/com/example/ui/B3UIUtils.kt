package com.example.ui

import java.util.Locale

object B3UIUtils {
    
    /**
     * Formats a double value with a suffix. 
     * Returns a fallback string (default "--") if the value is 0.0 or NaN.
     */
    fun formatValue(
        value: Double, 
        suffix: String = "", 
        prefix: String = "", 
        precision: Int = 2,
        fallback: String = "--"
    ): String {
        if (value == 0.0 || value.isNaN()) return fallback
        val format = "%,.${precision}f"
        return "$prefix${String.format(Locale.US, format, value)}$suffix"
    }

    /**
     * Specialized formatter for large financial numbers (Millions, Billions)
     */
    fun formatLargeNumber(value: Double, fallback: String = "--"): String {
        if (value == 0.0 || value.isNaN()) return fallback
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "R$ %.2f B", value / 1_000_000_000)
            value >= 1_000_000 -> String.format(Locale.US, "R$ %.2f M", value / 1_000_000)
            else -> String.format(Locale.US, "R$ %,.0f", value)
        }
    }

    /**
     * Returns a text value or fallback if empty
     */
    fun formatText(value: String?, fallback: String = "--"): String {
        return if (value.isNullOrBlank()) fallback else value
    }
}
