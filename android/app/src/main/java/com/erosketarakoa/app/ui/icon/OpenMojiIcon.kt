package com.erosketarakoa.app.ui.icon

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Renders an emoji by OpenMoji hexcode. Prefers the device's own emoji font (so icons match the
 * rest of the phone) and falls back to the bundled OpenMoji SVG only for glyphs the system font
 * can't draw. A missing/unknown hexcode falls back to the amphora SVG.
 */
@Composable
fun OpenMojiIcon(
    hexcode: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    // Assets/glyph support are fixed at runtime; resolve once per hexcode.
    val resolved = remember(hexcode) {
        val candidate = hexcode.ifBlank { DEFAULT_ICON }.uppercase()
        val emoji = hexToEmoji(candidate)
        when {
            emoji != null && SYSTEM_EMOJI_PAINT.hasGlyph(emoji) -> Resolved.System(emoji)
            assetExists(context, candidate) -> Resolved.Svg(candidate)
            else -> Resolved.Svg(DEFAULT_ICON)
        }
    }
    when (resolved) {
        is Resolved.System -> {
            // Font at 0.8x the box so the glyph's line metrics (ascent+descent) fit without the
            // bottom being clipped; lineHeight pinned to fontSize keeps it vertically centered.
            val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }
            Box(modifier.size(size), contentAlignment = Alignment.Center) {
                Text(
                    text = resolved.emoji,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }
        }
        is Resolved.Svg -> AsyncImage(
            model = "file:///android_asset/openmoji/${resolved.hex}.svg",
            contentDescription = contentDescription,
            modifier = modifier.size(size),
        )
    }
}

private sealed interface Resolved {
    data class System(val emoji: String) : Resolved
    data class Svg(val hex: String) : Resolved
}

/** Shared paint for glyph checks; used only from the UI thread. */
private val SYSTEM_EMOJI_PAINT = Paint()

private fun assetExists(context: Context, hex: String): Boolean =
    runCatching { context.assets.open("openmoji/$hex.svg").close() }.isSuccess

/** "1F345" or "1F344-200D-1F7EB" -> the actual emoji string, or null if the hex is malformed. */
private fun hexToEmoji(hex: String): String? = runCatching {
    buildString {
        hex.split('-').forEach { appendCodePoint(it.toInt(16)) }
    }
}.getOrNull()
