package com.rollcall.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用颜色方案 - 现代化配色
 * 采用柔和的渐变色系，适合学校智能大屏幕显示
 */
data class AppColors(
    // 主色调
    val primary: Color = Color(0xFF4A90D9),
    val primaryVariant: Color = Color(0xFF3A7BD5),
    val onPrimary: Color = Color.White,

    // 背景色
    val background: Color = Color(0xFFF5F7FA),
    val surface: Color = Color.White,
    val surfaceVariant: Color = Color(0xFFF0F2F5),
    val onBackground: Color = Color(0xFF1A1A2E),
    val onSurface: Color = Color(0xFF2D2D3F),

    // 强调色
    val accent: Color = Color(0xFFFF6B6B),
    val accentVariant: Color = Color(0xFFEE5A24),
    val success: Color = Color(0xFF27AE60),
    val warning: Color = Color(0xFFF39C12),
    val error: Color = Color(0xFFE74C3C),

    // 文字色
    val textPrimary: Color = Color(0xFF1A1A2E),
    val textSecondary: Color = Color(0xFF6B7280),
    val textHint: Color = Color(0xFF9CA3AF),

    // 分割线和边框
    val divider: Color = Color(0xFFE5E7EB),
    val border: Color = Color(0xFFD1D5DB),

    // 卡片阴影色
    val shadowLight: Color = Color(0x1A000000),
    val shadowDark: Color = Color(0x33000000),

    // 悬浮窗颜色
    val floatingBackground: Color = Color(0xFFFFFFFF),
    val floatingBorder: Color = Color(0xFFE8EDF3),

    // 点名结果页面
    val resultBackground: Color = Color(0xFFF8F9FE),
    val resultHighlight: Color = Color(0xFFFF4757),
    val resultText: Color = Color(0xFF2D3436),
)

/**
 * 应用文字样式
 */
data class AppTypography(
    val displayLarge: TextStyle = TextStyle(
        fontSize = 300.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFF4757)
    ),
    val displayMedium: TextStyle = TextStyle(
        fontSize = 150.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFF4757)
    ),
    val headlineLarge: TextStyle = TextStyle(
        fontSize = 100.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1A2E)
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1A2E)
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A1A2E)
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1A1A2E)
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF2D2D3F)
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF6B7280)
    ),
    val labelLarge: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF6B7280)
    ),
)

/**
 * 应用形状系统
 */
object AppShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val circle = RoundedCornerShape(50)
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val dialog = RoundedCornerShape(20.dp)
    val floating = RoundedCornerShape(32.dp)
}

/**
 * 应用间距系统
 */
object AppSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
}

// CompositionLocal 用于主题传递
val LocalAppColors = staticCompositionLocalOf { AppColors() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/**
 * 应用主题入口
 * 包裹整个应用，提供统一的颜色和字体样式
 */
@Composable
fun AppTheme(
    colors: AppColors = AppColors(),
    typography: AppTypography = AppTypography(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content
    )
}

/**
 * 主题访问器 - 方便在任何地方获取当前主题
 */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable get() = LocalAppTypography.current
}
