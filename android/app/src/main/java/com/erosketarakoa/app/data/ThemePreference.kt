package com.erosketarakoa.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Selectable app themes. Add new entries here to grow the theme list. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Selectable font sizes, applied as a global text scale multiplier. */
enum class FontSize(val scale: Float) { SMALL(0.85f), NORMAL(1f), LARGE(1.15f), HUGE(1.3f) }

/** Persists user settings in SharedPreferences and exposes them as flows. */
@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _fontSize = MutableStateFlow(readFontSize())
    val fontSize: StateFlow<FontSize> = _fontSize.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _mode.value = mode
    }

    fun set(fontSize: FontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, fontSize.name).apply()
        _fontSize.value = fontSize
    }

    private fun readMode(): ThemeMode =
        prefs.getString(KEY_THEME, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    private fun readFontSize(): FontSize =
        prefs.getString(KEY_FONT_SIZE, null)?.let { runCatching { FontSize.valueOf(it) }.getOrNull() }
            ?: FontSize.NORMAL

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_FONT_SIZE = "font_size"
    }
}
