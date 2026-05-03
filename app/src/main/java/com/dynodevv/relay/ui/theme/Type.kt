package com.dynodevv.relay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dynodevv.relay.R

val GoogleSansFlex = FontFamily(
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
    )
)

val GoogleSansCode = FontFamily(
    Font(
        R.font.googlesanscode_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("MONO", 1f))
    ),
    Font(
        R.font.googlesanscode_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.Setting("MONO", 1f))
    ),
    Font(
        R.font.googlesanscode_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.Setting("MONO", 1f))
    )
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
