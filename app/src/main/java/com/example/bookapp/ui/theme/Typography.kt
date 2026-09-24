package com.example.bookapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bookapp.R

val TaziehFontFamily = FontFamily(
    Font(R.font.b_titr_bold, FontWeight.Normal),
    Font(R.font.b_titr_bold, FontWeight.Bold)
)

val FontChoices = mapOf(
    "titr" to TaziehFontFamily,
    "serif" to FontFamily.Serif,
    "sans" to FontFamily.SansSerif,
    "cursive" to FontFamily.Cursive,
)

val FontChoiceLabels = mapOf(
    "titr" to "B Titr (اختصاصی)",
    "serif" to "سریف",
    "sans" to "بدون سریف",
    "cursive" to "شکسته",
)

/**
 * تایپوگرافی سراسری برنامه. fontScale روی متن‌های استاندارد Material اعمال می‌شود
 * تا تنظیم اندازه متن فقط محدود به صفحه مطالعه نباشد.
 */
fun typographyFor(fontChoice: String, fontScale: Float = 1f): Typography {
    val family = FontChoices[fontChoice] ?: TaziehFontFamily
    val scale = fontScale.coerceIn(0.8f, 2.0f)
    fun style(size: Float, line: Float, weight: FontWeight? = null) =
        TextStyle(
            fontFamily = family,
            fontSize = (size * scale).sp,
            lineHeight = (line * scale).sp,
            fontWeight = weight
        )

    return Typography(
        displayLarge = style(57f, 64f),
        displayMedium = style(45f, 52f),
        displaySmall = style(36f, 44f),
        headlineLarge = style(32f, 40f),
        headlineMedium = style(28f, 36f),
        headlineSmall = style(24f, 32f),
        titleLarge = style(22f, 28f),
        titleMedium = style(16f, 24f, FontWeight.Medium),
        titleSmall = style(14f, 20f, FontWeight.Medium),
        bodyLarge = style(18f, 32f),
        bodyMedium = style(16f, 24f),
        bodySmall = style(14f, 20f),
        labelLarge = style(14f, 20f, FontWeight.Medium),
        labelMedium = style(12f, 16f, FontWeight.Medium),
        labelSmall = style(11f, 16f, FontWeight.Medium)
    )
}
