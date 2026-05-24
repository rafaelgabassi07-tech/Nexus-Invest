package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class FontScale(val scale: Float) { SMALL(0.85f), MEDIUM(1.0f), LARGE(1.15f), EXTRA_LARGE(1.3f) }
enum class CornerStyle(val radius: Int) { SHARP(0), MODERN(12), ROUNDED(24) }
enum class DarkVariant { CARBON, OLED, DEEP_SEA }
enum class LightVariant { CLASSIC, VALOR_GOLD, IVORY_CREAM }

class ThemePreferences(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val FONT_SCALE_KEY = stringPreferencesKey("font_scale")
    private val CORNER_STYLE_KEY = stringPreferencesKey("corner_style")
    private val DARK_VARIANT_KEY = stringPreferencesKey("dark_variant")
    private val LIGHT_VARIANT_KEY = stringPreferencesKey("light_variant")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val PIN_ENABLED_KEY = booleanPreferencesKey("pin_enabled")
    private val HIDE_VALUES_KEY = booleanPreferencesKey("hide_values")
    private val PIN_CODE_KEY = stringPreferencesKey("pin_code")
    private val FAVORITES_KEY = stringPreferencesKey("favorite_tickers")

    val theme: Flow<AppTheme> = context.dataStore.data.map { pref ->
        AppTheme.valueOf(pref[THEME_KEY] ?: AppTheme.SYSTEM.name)
    }

    val favoriteTickers: Flow<List<String>> = context.dataStore.data.map { pref ->
        val raw = pref[FAVORITES_KEY] ?: ""
        if (raw.isEmpty()) {
            emptyList()
        } else {
            raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        }
    }

    suspend fun toggleFavorite(ticker: String) {
        context.dataStore.edit { pref ->
            val cleanTicker = ticker.trim().uppercase()
            if (cleanTicker.isEmpty()) return@edit
            val raw = pref[FAVORITES_KEY] ?: ""
            val current = if (raw.isEmpty()) emptyList() else raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            val updated = if (current.contains(cleanTicker)) {
                current.filter { it != cleanTicker }
            } else {
                current + cleanTicker
            }
            pref[FAVORITES_KEY] = updated.joinToString(",")
        }
    }

    val fontScale: Flow<FontScale> = context.dataStore.data.map { pref ->
        FontScale.valueOf(pref[FONT_SCALE_KEY] ?: FontScale.MEDIUM.name)
    }

    val cornerStyle: Flow<CornerStyle> = context.dataStore.data.map { pref ->
        CornerStyle.valueOf(pref[CORNER_STYLE_KEY] ?: CornerStyle.MODERN.name)
    }

    val darkVariant: Flow<DarkVariant> = context.dataStore.data.map { pref ->
        DarkVariant.valueOf(pref[DARK_VARIANT_KEY] ?: DarkVariant.CARBON.name)
    }

    val lightVariant: Flow<LightVariant> = context.dataStore.data.map { pref ->
        LightVariant.valueOf(pref[LIGHT_VARIANT_KEY] ?: LightVariant.CLASSIC.name)
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { it[PIN_ENABLED_KEY] ?: false }
    val hideValues: Flow<Boolean> = context.dataStore.data.map { it[HIDE_VALUES_KEY] ?: false }
    val pinCode: Flow<String?> = context.dataStore.data.map { it[PIN_CODE_KEY] }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setFontScale(scale: FontScale) {
        context.dataStore.edit { it[FONT_SCALE_KEY] = scale.name }
    }

    suspend fun setCornerStyle(style: CornerStyle) {
        context.dataStore.edit { it[CORNER_STYLE_KEY] = style.name }
    }

    suspend fun setDarkVariant(variant: DarkVariant) {
        context.dataStore.edit { it[DARK_VARIANT_KEY] = variant.name }
    }

    suspend fun setLightVariant(variant: LightVariant) {
        context.dataStore.edit { it[LIGHT_VARIANT_KEY] = variant.name }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PIN_ENABLED_KEY] = enabled }
    }

    suspend fun setHideValues(hide: Boolean) {
        context.dataStore.edit { it[HIDE_VALUES_KEY] = hide }
    }

    suspend fun setPinCode(pin: String?) {
        context.dataStore.edit { 
            if (pin == null) it.remove(PIN_CODE_KEY)
            else it[PIN_CODE_KEY] = pin 
        }
    }
}
