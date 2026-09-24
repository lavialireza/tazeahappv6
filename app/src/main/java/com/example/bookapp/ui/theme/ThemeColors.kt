package com.example.bookapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// تم پیش‌فرض (طلایی) — حالت شب واقعی: پس‌زمینه مشکی خالص و متن کرم‌رنگ
private val DefaultDark = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEFE0C0),
    onSurface = Color(0xFFEFE0C0),
    onSurfaceVariant = Color(0xFFC9BFA6),
    primary = Color(0xFFD4A94A),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A230F),
    onPrimaryContainer = Color(0xFFEFE0C0)
)
private val DefaultLight = lightColorScheme(
    primary = Color(0xFF8A6D1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3B8),
    onPrimaryContainer = Color(0xFF2A2000),
    secondary = Color(0xFF9C8449)
)

// تم سبز-طلایی
private val GreenDark = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF16211A),
    onBackground = Color(0xFFE3F0E5),
    onSurface = Color(0xFFE3F0E5),
    onSurfaceVariant = Color(0xFFB9CBBB),
    primary = Color(0xFF3E8E5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF163322),
    onPrimaryContainer = Color(0xFFE3F0E5)
)
private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E8D2),
    onPrimaryContainer = Color(0xFF0B2E17),
    secondary = Color(0xFF4E7C63)
)

// تم قرمز تیره (حس عزاداری)
private val RedDark = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF241414),
    onBackground = Color(0xFFF0DEDE),
    onSurface = Color(0xFFF0DEDE),
    onSurfaceVariant = Color(0xFFCBB0B0),
    primary = Color(0xFFA33B3B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF331414),
    onPrimaryContainer = Color(0xFFF0DEDE)
)
private val RedLight = lightColorScheme(
    primary = Color(0xFF8C2F2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D3D3),
    onPrimaryContainer = Color(0xFF3A0E0E),
    secondary = Color(0xFF8A5A5A)
)

fun colorSchemeFor(themeChoice: String, darkMode: Boolean): ColorScheme {
    return when (themeChoice) {
        "green" -> if (darkMode) GreenDark else GreenLight
        "red" -> if (darkMode) RedDark else RedLight
        else -> if (darkMode) DefaultDark else DefaultLight
    }
}
