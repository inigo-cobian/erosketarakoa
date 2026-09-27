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

private val titleFont = FontFamily(Font(GoogleFont("Limelight"), provider))
private val bodyFont = FontFamily(Font(GoogleFont("Montserrat"), provider))
private val labelFont = FontFamily(Font(GoogleFont("Lato"), provider))

val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = titleFont),
        displayMedium = displayMedium.copy(fontFamily = titleFont),
        displaySmall = displaySmall.copy(fontFamily = titleFont),
        headlineLarge = headlineLarge.copy(fontFamily = titleFont),
        headlineMedium = headlineMedium.copy(fontFamily = titleFont),
        headlineSmall = headlineSmall.copy(fontFamily = titleFont),
        titleLarge = titleLarge.copy(fontFamily = titleFont),
        titleMedium = titleMedium.copy(fontFamily = titleFont),
        titleSmall = titleSmall.copy(fontFamily = titleFont),
        bodyLarge = bodyLarge.copy(fontFamily = bodyFont),
        bodyMedium = bodyMedium.copy(fontFamily = bodyFont),
        bodySmall = bodySmall.copy(fontFamily = bodyFont),
        labelLarge = labelLarge.copy(fontFamily = labelFont),
        labelMedium = labelMedium.copy(fontFamily = labelFont),
        labelSmall = labelSmall.copy(fontFamily = labelFont),
    )
}
