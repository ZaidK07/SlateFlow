package com.zaid01.slateflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zaid01.slateflow.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Inter = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
private val base = Typography()
private fun TextStyle.inter(weight: FontWeight = fontWeight ?: FontWeight.Normal) = copy(fontFamily = Inter, fontWeight = weight)
val Typography = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.Bold),
    displayMedium = base.displayMedium.inter(FontWeight.Bold),
    displaySmall = base.displaySmall.inter(FontWeight.Bold),
    headlineLarge = base.headlineLarge.inter(FontWeight.Bold),
    headlineMedium = base.headlineMedium.inter(FontWeight.Bold),
    headlineSmall = base.headlineSmall.inter(FontWeight.Bold),
    titleLarge = base.titleLarge.inter(FontWeight.Bold).copy(letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold),
    titleSmall = base.titleSmall.inter(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.inter().copy(letterSpacing = 0.sp),
    bodyMedium = base.bodyMedium.inter().copy(letterSpacing = 0.sp),
    bodySmall = base.bodySmall.inter().copy(letterSpacing = 0.sp),
    labelLarge = base.labelLarge.inter(FontWeight.SemiBold),
    labelMedium = base.labelMedium.inter(FontWeight.Medium),
    labelSmall = base.labelSmall.inter(FontWeight.Medium),
)
