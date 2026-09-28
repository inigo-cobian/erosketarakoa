package com.erosketarakoa.app.data

import androidx.compose.ui.graphics.Color

/**
 * The fixed palette a list can be tagged with. Stored by [name] in the DB.
 * WHITE is the default (renders as "no color" / theme default in the header).
 */
enum class ListColor(val label: String, val swatch: Color) {
    WHITE("White", Color(0xFFFFFFFF)),
    GREY("Grey", Color(0xFF9E9E9E)),
    DARK_GREY("Dark grey", Color(0xFF424242)),
    BLACK("Black", Color(0xFF000000)),
    YELLOW("Yellow", Color(0xFFFFD600)),
    GOLD("Gold", Color(0xFFC9A227)),
    ORANGE("Orange", Color(0xFFF57C00)),
    RED("Red", Color(0xFFD32F2F)),
    MAROON("Maroon", Color(0xFF6D1B1B)),
    BURGUNDY("Burgundy", Color(0xFF800020)),
    PINK("Pink", Color(0xFFEC407A)),
    MAGENTA("Magenta", Color(0xFFC2185B)),
    BILBAO_BLUE("Blue", Color(0xFF0F4C81)),
    BLUE("Blue", Color(0xFF1976D2)),
    CYAN("Cyan", Color(0xFF00ACC1)),
    GREEN("Green", Color(0xFF388E3C)),
    DARK_GREEN("Dark green", Color(0xFF1B5E20)),
    PURPLE("Purple", Color(0xFF7B1FA2));

    /** True for colors whose text should follow the theme's font color instead of tinting. */
    val usesThemeText: Boolean get() = this == WHITE || this == BLACK || this == DARK_GREY

    companion object {
        val DEFAULT = WHITE

        /** Parse a stored name, falling back to the default for unknown/blank values. */
        fun from(name: String?): ListColor =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}
