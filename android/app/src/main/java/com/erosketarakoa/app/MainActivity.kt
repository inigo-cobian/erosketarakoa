package com.erosketarakoa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.notification.BargainNotifier
import com.erosketarakoa.app.ui.navigation.AppNavigation
import com.erosketarakoa.app.ui.settings.ThemeViewModel
import com.erosketarakoa.app.ui.theme.ErosketarakoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openBargains = intent?.dataString == BargainNotifier.DEEP_LINK
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val fontSize by themeViewModel.fontSize.collectAsStateWithLifecycle()
            ErosketarakoTheme(themeMode = themeMode) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale * fontSize.scale,
                    ),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        RootContent(openBargains = openBargains)
                    }
                }
            }
        }
    }
}

@Composable
private fun RootContent(openBargains: Boolean) {
    AppNavigation(openBargains = openBargains)
}
