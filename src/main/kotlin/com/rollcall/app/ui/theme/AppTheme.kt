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
import java.awt.Toolkit

/**
 * 应用颜色方案 - 现代极光/玻璃拟态配色
 * 采用柔和的渐变色系，适合学校智能大屏幕显示
 * 支持亮色模式与暗色模式自动切换
 */
data class AppColors(
    // 主色调
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,

    // 背景色
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,

    // 强调色
    val accent: Color,
    val accentVariant: Color,
    val success: Color,
    val warning: Color,
    val error: Color,

    // 文字色
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,

    // 分割线和边框
    val divider: Color,
    val border: Color,

    // 卡片阴影色
    val shadowLight: Color,
    val shadowDark: Color,

    // 悬浮窗颜色
    val floatingBackground: Color,
    val floatingBorder: Color,

    // 点名结果页面
    val resultBackground: Color,
    val resultHighlight: Color,
    val resultText: Color,

    // —— 新增：渐变背景、卡片、微光动画 ——

    // 渐变背景色（用于全屏或大面积渐变）
    val gradient1: Color,
    val gradient2: Color,

    // 卡片专用背景与边框（玻璃拟态风格）
    val cardBackground: Color,
    val cardBorder: Color,

    // 微光/骨架屏加载动画高光色
    val shimmer: Color,
) {
    companion object {

        /**
         * 亮色方案 —— 柔白底色 + 蓝灰点缀 + 暖珊瑚/日落高亮
         */
        fun lightColors(): AppColors = AppColors(
            // 主色调：柔和的天空蓝
            primary = Color(0xFF5B8DEF),
            primaryVariant = Color(0xFF4678DB),
            onPrimary = Color.White,

            // 背景色：米白与浅灰蓝
            background = Color(0xFFF6F8FC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF0F3F8),
            onBackground = Color(0xFF1B2A4A),
            onSurface = Color(0xFF2C3E5A),

            // 强调色：暖珊瑚 / 日落橙
            accent = Color(0xFFFF7E6B),
            accentVariant = Color(0xFFF56042),
            success = Color(0xFF2ECC71),
            warning = Color(0xFFFBBF24),
            error = Color(0xFFEF4444),

            // 文字色
            textPrimary = Color(0xFF1B2A4A),
            textSecondary = Color(0xFF64748B),
            textHint = Color(0xFF94A3B8),

            // 分割线和边框
            divider = Color(0xFFE2E8F0),
            border = Color(0xFFCBD5E1),

            // 卡片阴影色
            shadowLight = Color(0x14000000),
            shadowDark = Color(0x29000000),

            // 悬浮窗颜色
            floatingBackground = Color(0xFFFFFFFF),
            floatingBorder = Color(0xFFE2E8F0),

            // 点名结果页面
            resultBackground = Color(0xFFF8FAFF),
            resultHighlight = Color(0xFFFF6B6B),
            resultText = Color(0xFF1E293B),

            // 渐变背景：浅粉蓝 → 浅薰衣草
            gradient1 = Color(0xFFE0ECFF),
            gradient2 = Color(0xFFF5E6FF),

            // 卡片：半透明白 + 浅蓝灰边框（玻璃拟态）
            cardBackground = Color(0xCCFFFFFF),   // ~80% 不透明白
            cardBorder = Color(0x33A0B4D0),

            // 微光高光
            shimmer = Color(0x33FFFFFF),
        )

        /**
         * 暗色方案 —— 深炭/藏青底色 + 电光蓝/青绿点缀 + 暖琥珀高亮
         */
        fun darkColors(): AppColors = AppColors(
            // 主色调：电光蓝
            primary = Color(0xFF60A5FA),
            primaryVariant = Color(0xFF3B82F6),
            onPrimary = Color(0xFF0F172A),

            // 背景色：深藏青与炭灰
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF283548),
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFCBD5E1),

            // 强调色：暖琥珀 / 柔橙
            accent = Color(0xFFFBBF24),
            accentVariant = Color(0xFFF59E0B),
            success = Color(0xFF34D399),
            warning = Color(0xFFFCD34D),
            error = Color(0xFFF87171),

            // 文字色
            textPrimary = Color(0xFFE2E8F0),
            textSecondary = Color(0xFF94A3B8),
            textHint = Color(0xFF64748B),

            // 分割线和边框
            divider = Color(0xFF334155),
            border = Color(0xFF475569),

            // 卡片阴影色
            shadowLight = Color(0x33000000),
            shadowDark = Color(0x66000000),

            // 悬浮窗颜色
            floatingBackground = Color(0xFF1E293B),
            floatingBorder = Color(0xFF334155),

            // 点名结果页面
            resultBackground = Color(0xFF162032),
            resultHighlight = Color(0xFFFBBF24),
            resultText = Color(0xFFE2E8F0),

            // 渐变背景：深藏青 → 深紫墨
            gradient1 = Color(0xFF0F172A),
            gradient2 = Color(0xFF1A1040),

            // 卡片：半透明深蓝 + 青蓝边框（玻璃拟态）
            cardBackground = Color(0xBB1E293B),   // ~73% 不透明深蓝
            cardBorder = Color(0x4460A5FA),

            // 微光高光
            shimmer = Color(0x1AFFFFFF),
        )
    }
}

/**
 * 应用文字样式
 * 颜色会根据当前主题在 AppTheme composable 内自动匹配
 */
data class AppTypography(
    val displayLarge: TextStyle = TextStyle(
        fontSize = 300.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Unspecified
    ),
    val displayMedium: TextStyle = TextStyle(
        fontSize = 150.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Unspecified
    ),
    val headlineLarge: TextStyle = TextStyle(
        fontSize = 100.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Unspecified
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Unspecified
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Unspecified
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Unspecified
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified
    ),
    val labelLarge: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Unspecified
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

// ======== 系统暗色模式检测（桌面端 / java.awt） ========

/**
 * 检测当前操作系统是否处于暗色主题
 *
 * 检测策略（按优先级）：
 * 1. Windows：读取注册表 AppsUseLightTheme（0 = 暗色）
 * 2. macOS：读取 defaults read -g AppleInterfaceStyle
 * 3. Linux/GTK：读取 gtk-theme-name 或 gsettings 中的 color-scheme
 * 4. 通用回退：AWT Desktop hints（部分 JDK 支持）
 * 5. 若全部失败，默认返回 false（亮色模式）
 */
fun isDarkTheme(): Boolean {
    return try {
        detectSystemDarkMode()
    } catch (_: Exception) {
        false
    }
}

private fun detectSystemDarkMode(): Boolean {
    val osName = System.getProperty("os.name", "").lowercase()

    // —— Windows 注册表检测 ——
    if (osName.contains("win")) {
        return detectWindowsDarkMode()
    }

    // —— macOS defaults 检测 ——
    if (osName.contains("mac") || osName.contains("darwin")) {
        return detectMacOsDarkMode()
    }

    // —— Linux / 其他：GTK / GNOME / FreeDesktop 检测 ——
    if (osName.contains("nux") || osName.contains("nix") || osName.contains("bsd")) {
        return detectLinuxDarkMode()
    }

    // 通用回退：AWT Desktop hints
    return detectAwtDarkHint()
}

/** Windows：注册表读取 AppsUseLightTheme（值为 0 表示暗色） */
private fun detectWindowsDarkMode(): Boolean {
    return try {
        val process = ProcessBuilder(
            "reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        // 输出示例：... AppsUseLightTheme    REG_DWORD    0x00000000
        // 值为 0 表示暗色，非零表示亮色；用正则精确匹配 "0x" 后全为 0 的情况
        Regex("""0x0+\s*$""", RegexOption.MULTILINE).containsMatchIn(output)
    } catch (_: Exception) {
        false
    }
}

/** macOS：通过 defaults 读取 AppleInterfaceStyle */
private fun detectMacOsDarkMode(): Boolean {
    return try {
        val process = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.equals("Dark", ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}

/** Linux：尝试 gsettings / GTK 主题名称关键字 */
private fun detectLinuxDarkMode(): Boolean {
    // 优先尝试 FreeDesktop color-scheme（GNOME 42+）
    try {
        val process = ProcessBuilder(
            "gsettings", "get", "org.gnome.desktop.interface", "color-scheme"
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim().lowercase()
        process.waitFor()
        if (output.contains("dark")) return true
        if (output.contains("light") || output.contains("default")) return false
    } catch (_: Exception) { /* 继续尝试其他方式 */ }

    // 尝试 gtk-theme-name 关键字
    try {
        val process = ProcessBuilder(
            "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim().lowercase()
        process.waitFor()
        if (output.contains("dark")) return true
    } catch (_: Exception) { /* 忽略 */ }

    // 环境变量回退
    val gtkTheme = System.getenv("GTK_THEME") ?: ""
    if (gtkTheme.lowercase().contains("dark")) return true

    return detectAwtDarkHint()
}

/** AWT Desktop hints 通用回退 */
private fun detectAwtDarkHint(): Boolean {
    return try {
        val toolkit = Toolkit.getDefaultToolkit()
        val value = toolkit.getDesktopProperty("awt.desktop.isDarkMode")
        value == true
    } catch (_: Exception) {
        false
    }
}

// ======== CompositionLocal 用于主题传递 ========

val LocalAppColors = staticCompositionLocalOf { AppColors.lightColors() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

/**
 * 根据主题色方案生成匹配的排版样式
 * display 系列使用 accent 高亮色，其他使用文字主色/次色
 */
private fun typographyForColors(colors: AppColors): AppTypography = AppTypography(
    displayLarge = TextStyle(fontSize = 300.sp, fontWeight = FontWeight.Bold, color = colors.accent),
    displayMedium = TextStyle(fontSize = 150.sp, fontWeight = FontWeight.Bold, color = colors.accent),
    headlineLarge = TextStyle(fontSize = 100.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
    headlineMedium = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
    titleLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
    titleMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary),
    bodyLarge = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Normal, color = colors.onSurface),
    bodyMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, color = colors.textSecondary),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary),
)

/**
 * 应用主题入口
 * 包裹整个应用，提供统一的颜色和字体样式
 *
 * @param isDarkTheme 是否使用暗色主题；为 null 时自动检测系统设置
 * @param colors      自定义颜色方案（覆盖自动检测）
 * @param typography  自定义排版样式（覆盖自动生成）
 */
@Composable
fun AppTheme(
    isDarkTheme: Boolean? = null,
    colors: AppColors? = null,
    typography: AppTypography? = null,
    content: @Composable () -> Unit
) {
    val dark = isDarkTheme ?: isDarkTheme()
    val resolvedColors = colors ?: if (dark) AppColors.darkColors() else AppColors.lightColors()
    val resolvedTypography = typography ?: typographyForColors(resolvedColors)

    CompositionLocalProvider(
        LocalAppColors provides resolvedColors,
        LocalAppTypography provides resolvedTypography,
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
