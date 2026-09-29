package com.cssm.desktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

// ServPilot v0.2 原创深色主题（Tony 已批准的 UI 草稿配色）：
// 底 #0B0E14 / 卡片 #141A23 / 边框 #232C3B / 文字 #EAF0F7 / 次要 #8A94A6
// 点缀：薄荷绿 #3DDC97 / 天蓝 #38BDF8 / 琥珀 #FBBF24 / 红 #F87171
private val PilotDarkColors = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF06281C),
    primaryContainer = Color(0xFF1D4438),
    onPrimaryContainer = Color(0xFFD9FBEF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF08222E),
    secondaryContainer = Color(0xFF1E3A4C),
    onSecondaryContainer = Color(0xFFD6F0FF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF2E2004),
    tertiaryContainer = Color(0xFF4A3A10),
    onTertiaryContainer = Color(0xFFFFF3D6),
    error = Color(0xFFF87171),
    onError = Color(0xFF3A0D0B),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFEAF0F7),
    surface = Color(0xFF141A23),
    onSurface = Color(0xFFEAF0F7),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceVariant = Color(0xFF8A94A6),
    outline = Color(0xFF232C3B),
    outlineVariant = Color(0xFF232C3B)
)

// ServPilot 浅色主题（v0.5.7 新增）：底 #F1F4F8 / 卡片白 / 文字深；点缀色在浅底上加深保证对比度
private val PilotLightColors = lightColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF1F4F8),
    onBackground = Color(0xFF182230),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182230),
    surfaceVariant = Color(0xFFE9EEF4),
    onSurfaceVariant = Color(0xFF687385),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFE2E8F0)
)

/** 外观模式：跟随系统 / 深色 / 浅色 */
enum class ThemeMode { FOLLOW_SYSTEM, DARK, LIGHT }

private val PilotTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Composable
fun CssmTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) PilotDarkColors else PilotLightColors,
        typography = PilotTypography,
        content = content
    )
}
