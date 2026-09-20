package com.erosketarakoa.app.ui.settings

import androidx.lifecycle.ViewModel
import com.erosketarakoa.app.data.FontSize
import com.erosketarakoa.app.data.ThemeMode
import com.erosketarakoa.app.data.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreference: ThemePreference,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreference.mode
    val fontSize: StateFlow<FontSize> = themePreference.fontSize

    fun setThemeMode(mode: ThemeMode) = themePreference.set(mode)
    fun setFontSize(fontSize: FontSize) = themePreference.set(fontSize)
}
