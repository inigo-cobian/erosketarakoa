package com.erosketarakoa.app.data

import androidx.compose.ui.graphics.Color

/**
 * The fixed palette a list can be tagged with. Stored by [name] in the DB.
 * WHITE is the default (renders as "no color" / theme default in the header).
 */
enum class ListColor(val label: String, val swatch: Color) {
    WHITE("White", Color(0xFFFFFFFF)),
    GREY("Grey", Color(0xFF9E9E9E)),
    BLACK("Black", Color(0xFF000000)),
    YELLOW("Yellow", Color(0xFFF9A825)),
    RED("Red", Color(0xFFD32F2F)),
    BLUE("Blue", Color(0xFF1976D2)),
    GREEN("Green", Color(0xFF388E3C)),
    PURPLE("Purple", Color(0xFF7B1FA2)),
    ORANGE("Orange", Color(0xFFF57C00));

    companion object {
        val DEFAULT = WHITE

        /** Parse a stored name, falling back to the default for unknown/blank values. */
        fun from(name: String?): ListColor =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}
