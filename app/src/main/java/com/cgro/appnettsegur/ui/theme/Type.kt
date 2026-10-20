package com.cgro.appnettsegur.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Plus Jakarta Sans debe ir empaquetada en res/font/ (no se usan fuentes descargables
 * porque requieren Internet). Cuando estén los .ttf, reemplazar FontFamily.Default por:
 *
 * FontFamily(
 *     Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
 *     Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
 *     Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
 *     Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
 * )
 */
val PlusJakartaSans: FontFamily = FontFamily.Default

private val base = Typography()

val Typography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
)
