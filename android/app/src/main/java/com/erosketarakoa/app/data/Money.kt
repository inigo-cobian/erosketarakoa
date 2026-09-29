package com.erosketarakoa.app.data

/** Money helpers. Prices are integer cents everywhere; formatting/parsing lives here only. */
object Money {

    /** "1.99" or "1,99" or "2" -> 199 / 199 / 200. Null/blank/garbage -> null. */
    fun parseEurosToCents(input: String?): Long? {
        val cleaned = input?.trim()?.replace(',', '.')?.ifBlank { null } ?: return null
        val euros = cleaned.toDoubleOrNull() ?: return null
        if (euros < 0) return null
        return Math.round(euros * 100)
    }

    /** 199 -> "1.99". */
    fun centsToEuros(cents: Long?): String {
        if (cents == null) return ""
        return String.format(java.util.Locale.US, "%.2f", cents / 100.0)
    }
}
