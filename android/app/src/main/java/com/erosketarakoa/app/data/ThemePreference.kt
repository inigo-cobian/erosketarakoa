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

/** Item qualities that can be shown or hidden in the item subtitle. */
enum class ItemDetail(val label: String) {
    QUANTITY("Quantity"),
    SUPERMARKETS("Supermarkets"),
    CATEGORY("Category"),
    NOTES("Notes"),
}

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

    private val _visibleDetails = MutableStateFlow(readVisibleDetails())
    val visibleDetails: StateFlow<Set<ItemDetail>> = _visibleDetails.asStateFlow()

    fun setDetailVisible(detail: ItemDetail, visible: Boolean) {
        val updated = if (visible) _visibleDetails.value + detail else _visibleDetails.value - detail
        prefs.edit().putStringSet(KEY_VISIBLE_DETAILS, updated.map { it.name }.toSet()).apply()
        _visibleDetails.value = updated
    }

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

    private fun readVisibleDetails(): Set<ItemDetail> =
        prefs.getStringSet(KEY_VISIBLE_DETAILS, null)
            ?.mapNotNull { runCatching { ItemDetail.valueOf(it) }.getOrNull() }?.toSet()
            ?: ItemDetail.entries.toSet()

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_VISIBLE_DETAILS = "visible_details"
    }
}
