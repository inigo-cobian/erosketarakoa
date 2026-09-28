package com.erosketarakoa.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.data.FontSize
import com.erosketarakoa.app.data.ItemDetail
import com.erosketarakoa.app.data.ThemeMode

const val SETTINGS_SCREEN_TAG = "settings_screen"
const val SETTINGS_TITLE_FIELD_TAG = "settings_title_field"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val currentTheme by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val currentFontSize by themeViewModel.fontSize.collectAsStateWithLifecycle()
    val visibleDetails by themeViewModel.visibleDetails.collectAsStateWithLifecycle()
    val title by themeViewModel.title.collectAsStateWithLifecycle()
    var titleDraft by remember(title) { mutableStateOf(title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { themeViewModel.setTitle(titleDraft); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag(SETTINGS_SCREEN_TAG),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Displayed title", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = titleDraft,
                onValueChange = { titleDraft = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) themeViewModel.setTitle(titleDraft) }
                    .testTag(SETTINGS_TITLE_FIELD_TAG),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Theme", style = MaterialTheme.typography.titleSmall)
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    SettingOption(
                        label = mode.label(),
                        selected = mode == currentTheme,
                        onSelect = { themeViewModel.setThemeMode(mode) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Font size", style = MaterialTheme.typography.titleSmall)
            Column(modifier = Modifier.selectableGroup()) {
                FontSize.entries.forEach { size ->
                    SettingOption(
                        label = size.label(),
                        selected = size == currentFontSize,
                        onSelect = { themeViewModel.setFontSize(size) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Item details", style = MaterialTheme.typography.titleSmall)
            ItemDetail.entries.forEach { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(detail.label, modifier = Modifier.weight(1f))
                    Switch(
                        checked = detail in visibleDetails,
                        onCheckedChange = { themeViewModel.setDetailVisible(detail, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun FontSize.label(): String = when (this) {
    FontSize.SMALL -> "Small"
    FontSize.NORMAL -> "Normal"
    FontSize.LARGE -> "Large"
    FontSize.HUGE -> "Huge"
}
