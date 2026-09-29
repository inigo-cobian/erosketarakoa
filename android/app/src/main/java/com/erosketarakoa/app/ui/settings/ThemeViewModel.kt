package com.erosketarakoa.app.ui.settings

import androidx.lifecycle.ViewModel
import com.erosketarakoa.app.data.FontSize
import com.erosketarakoa.app.data.ItemDetail
import com.erosketarakoa.app.data.ThemeMode
import com.erosketarakoa.app.data.ThemePreference
import com.erosketarakoa.app.notification.BargainScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreference: ThemePreference,
    private val bargainScheduler: BargainScheduler,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreference.mode
    val fontSize: StateFlow<FontSize> = themePreference.fontSize
    val visibleDetails: StateFlow<Set<ItemDetail>> = themePreference.visibleDetails
    val title: StateFlow<String> = themePreference.title
    val bargainNotifyEnabled: StateFlow<Boolean> = themePreference.bargainNotifyEnabled
    val bargainNotifyMinuteOfDay: StateFlow<Int> = themePreference.bargainNotifyMinuteOfDay

    fun setThemeMode(mode: ThemeMode) = themePreference.set(mode)
    fun setFontSize(fontSize: FontSize) = themePreference.set(fontSize)
    fun setDetailVisible(detail: ItemDetail, visible: Boolean) = themePreference.setDetailVisible(detail, visible)
    fun setTitle(title: String) = themePreference.setTitle(title)

    fun setBargainNotifyEnabled(enabled: Boolean) {
        themePreference.setBargainNotifyEnabled(enabled)
        bargainScheduler.apply(enabled, themePreference.bargainNotifyMinuteOfDay.value)
    }

    fun setBargainNotifyTime(hour: Int, minute: Int) {
        themePreference.setBargainNotifyTime(hour, minute)
        if (themePreference.bargainNotifyEnabled.value) {
            bargainScheduler.schedule(themePreference.bargainNotifyMinuteOfDay.value)
        }
    }
}
