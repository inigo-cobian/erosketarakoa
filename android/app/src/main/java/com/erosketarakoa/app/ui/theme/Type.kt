package com.erosketarakoa.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.erosketarakoa.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val firaSans = FontFamily(
    Font(GoogleFont("Fira Sans"), provider),
)

val AppTypography = Typography().run {
    val d = FontFamily(Font(GoogleFont("Fira Sans"), provider))
    copy(
        displayLarge = displayLarge.copy(fontFamily = d),
        displayMedium = displayMedium.copy(fontFamily = d),
        displaySmall = displaySmall.copy(fontFamily = d),
        headlineLarge = headlineLarge.copy(fontFamily = d),
        headlineMedium = headlineMedium.copy(fontFamily = d),
        headlineSmall = headlineSmall.copy(fontFamily = d),
        titleLarge = titleLarge.copy(fontFamily = d),
        titleMedium = titleMedium.copy(fontFamily = d),
        titleSmall = titleSmall.copy(fontFamily = d),
        bodyLarge = bodyLarge.copy(fontFamily = d, fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(fontFamily = d, fontSize = 16.sp, lineHeight = 22.sp),
        bodySmall = bodySmall.copy(fontFamily = d),
        labelLarge = labelLarge.copy(fontFamily = d),
        labelMedium = labelMedium.copy(fontFamily = d),
        labelSmall = labelSmall.copy(fontFamily = d),
    )
}
