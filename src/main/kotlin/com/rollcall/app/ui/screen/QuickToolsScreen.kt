package com.rollcall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * 快捷工具面板
 * 提供常用工具的快速入口
 *
 * @param onClose 关闭面板回调
 * @param onOpenStatistics 打开统计面板回调
 * @param onOpenGroupGenerator 打开分组面板回调
 * @param onOpenSchedule 打开课程表回调
 * @param onOpenQuiz 打开抽题器回调
 * @param onOpenSeatMap 打开座位表回调
 * @param onOpenNoiseMeter 打开噪音检测回调
 */
@Composable
fun QuickToolsPanel(
    onClose: () -> Unit,
    onOpenStatistics: () -> Unit = {},
    onOpenGroupGenerator: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    onOpenQuiz: () -> Unit = {},
    onOpenSeatMap: () -> Unit = {},
    onOpenNoiseMeter: () -> Unit = {}
) {
    Window(
        onCloseRequest = onClose,
        title = "快捷工具",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.CenterEnd),
            size = DpSize(360.dp, 600.dp)
        ),
    ) {
        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(colors.gradient1, colors.gradient2)
                        )
                    )
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🛠 快捷工具",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 今日信息卡片
                TodayInfoCard(colors)

                Spacer(Modifier.height(16.dp))

                // 工具网格
                Text(
                    "常用工具",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textHint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 第一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        icon = "📊",
                        label = "点名统计",
                        accentColor = colors.primary,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenStatistics() }
                    )
                    ToolButton(
                        icon = "🎲",
                        label = "随机分组",
                        accentColor = colors.accent,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenGroupGenerator() }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 第二行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        icon = "🎯",
                        label = "随机抽题",
                        accentColor = colors.success,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenQuiz() }
                    )
                    ToolButton(
                        icon = "⏱",
                        label = "倒计时",
                        accentColor = colors.warning,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 第三行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        icon = "💺",
                        label = "随机座位",
                        accentColor = Color(0xFF9B59B6),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenSeatMap() }
                    )
                    ToolButton(
                        icon = "📅",
                        label = "今日课表",
                        accentColor = Color(0xFF1ABC9C),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenSchedule() }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 第四行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolButton(
                        icon = "🔊",
                        label = "噪音检测",
                        accentColor = Color(0xFFE74C3C),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onClick = { onClose(); onOpenNoiseMeter() }
                    )
                    ToolButton(
                        icon = "🔢",
                        label = "随机数",
                        accentColor = Color(0xFF3498DB),
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.weight(1f))

                // 底部版本信息
                Text(
                    "v${AppState.VERSION} · 智能点名系统",
                    fontSize = 12.sp,
                    color = colors.textHint.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 今日信息卡片
 * 显示今天的日期、星期和对应课程概览
 */
@Composable
private fun TodayInfoCard(colors: com.rollcall.app.ui.theme.AppColors) {
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.CHINESE)
    val dateStr = "${today.monthValue}月${today.dayOfMonth}日"

    // 从AppState获取今日课表
    val todayKey = when (today.dayOfWeek.value) {
        1 -> "星期一"
        2 -> "星期二"
        3 -> "星期三"
        4 -> "星期四"
        5 -> "星期五"
        6 -> "星期六"
        7 -> "星期日"
        else -> ""
    }
    val todaySchedule = AppState.subjectList[todayKey]
    val classCount = todaySchedule?.schedule?.size ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期圆圈
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.primary, colors.accent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${today.dayOfMonth}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "$dateStr $dayOfWeek",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (classCount > 0) "今日共 $classCount 节课" else "今日无课程安排",
                    fontSize = 14.sp,
                    color = colors.textHint
                )
            }
        }
    }
}

/**
 * 工具按钮组件
 */
@Composable
private fun ToolButton(
    icon: String,
    label: String,
    accentColor: Color,
    colors: com.rollcall.app.ui.theme.AppColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}
