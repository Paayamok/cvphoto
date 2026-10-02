package com.fushengce.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val FuShengCeColors = lightColorScheme(
    primary = Color(0xFF9B3028),
    onPrimary = Color(0xFFFFFBF4),
    primaryContainer = Color(0xFFF2D9D2),
    onPrimaryContainer = Color(0xFF3D0A07),
    background = Color(0xFFF5EFE2),
    onBackground = Color(0xFF241C15),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF241C15),
    surfaceVariant = Color(0xFFE9E0D0),
    onSurfaceVariant = Color(0xFF51483D),
    outline = Color(0xFF817466),
)

private val FuShengWenKai = FontFamily(
    Font(R.font.fusheng_wenkai, FontWeight.Normal),
)

private val FuShengCeTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FuShengWenKai,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FuShengWenKai,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FuShengWenKai,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FuShengWenKai,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FuShengWenKai,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun FuShengCeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FuShengCeColors,
        typography = FuShengCeTypography,
        content = content,
    )
}
