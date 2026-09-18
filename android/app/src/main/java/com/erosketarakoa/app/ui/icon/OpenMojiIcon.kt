package com.erosketarakoa.app.ui.icon

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Renders a bundled OpenMoji SVG by hexcode, fully offline (Coil + SvgDecoder registered on the
 * Application). A missing/unknown hexcode falls back to the amphora.
 */
@Composable
fun OpenMojiIcon(
    hexcode: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    // Assets are fixed at build time; resolve once per hexcode.
    val hex = remember(hexcode) {
        val candidate = hexcode.ifBlank { DEFAULT_ICON }.uppercase()
        val exists = runCatching {
            context.assets.open("openmoji/$candidate.svg").close()
        }.isSuccess
        if (exists) candidate else DEFAULT_ICON
    }
    AsyncImage(
        model = "file:///android_asset/openmoji/$hex.svg",
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}
