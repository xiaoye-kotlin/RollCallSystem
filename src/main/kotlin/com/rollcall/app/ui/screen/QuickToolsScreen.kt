package com.rollcall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

@Composable
fun QuickToolsPanel(
    onClose: () -> Unit,
    onOpenStatistics: () -> Unit = {},
    onOpenGroupGenerator: () -> Unit = {},
    onOpenQuiz: () -> Unit = {},
    onOpenNoiseMeter: () -> Unit = {},
    onOpenLearning: () -> Unit = {},
    onOpenCountdown: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Window(
        onCloseRequest = onClose,
        title = "快捷工具",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.CenterEnd),
            size = DpSize(356.dp, 540.dp)
        )
    ) {
        AppTheme {
            val colors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDFBFF),
                                Color(0xFFF7FBFF),
                                Color(0xFFFFFBF5)
                            )
                        )
                    )
                    .border(1.dp, colors.cardBorder.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("快捷工具", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("保留常用功能，倒计时支持点击选择时长。", fontSize = 12.sp, color = colors.textHint)
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    TodayInfoCard(colors)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "教学常用",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textHint,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val toolRows = listOf(
                        listOf(
                            ToolItem("📊", "点名统计", colors.primary, onOpenStatistics),
                            ToolItem("🎲", "随机分组", colors.accent, onOpenGroupGenerator)
                        ),
                        listOf(
                            ToolItem("🎯", "随机抽题", colors.success, onOpenQuiz),
                            ToolItem("📝", "OCR识词", Color(0xFF8E44AD), onOpenLearning)
                        ),
                        listOf(
                            ToolItem("🔊", "噪音检测", Color(0xFFE45858), onOpenNoiseMeter),
                            ToolItem("⏱", "倒计时", colors.warning, onOpenCountdown)
                        )
                    )

                    toolRows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { item ->
                                ToolButton(
                                    icon = item.icon,
                                    label = item.label,
                                    accentColor = item.accentColor,
                                    colors = colors,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onClose()
                                    item.onClick()
                                }
                            }
                        }
                        if (rowIndex != toolRows.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HelperCard(colors)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "v${AppState.VERSION} · 智能点名系统",
                    fontSize = 12.sp,
                    color = colors.textHint.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CountdownQuickPickerWindow(onClose: () -> Unit) {
    val isEnabled = AppState.isCountDownOpen.collectAsState()

    Window(
        onCloseRequest = onClose,
        title = "倒计时",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(320.dp, 280.dp)
        )
    ) {
        AppTheme {
            val colors = AppTheme.colors
            val options = listOf(
                Triple("1分钟", Color(0xFF79A8FF), 1),
                Triple("3分钟", Color(0xFF8D7DFF), 2),
                Triple("5分钟", Color(0xFF63C7A3), 3),
                Triple("10分钟", Color(0xFFFFB067), 4)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDFBFF),
                                Color(0xFFF8FBFF),
                                Color(0xFFFFFBF5)
                            )
                        )
                    )
                    .border(1.dp, colors.cardBorder.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("倒计时", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(
                            if (isEnabled.value) "点击时长后立即开始" else "当前倒计时开关未开启",
                            fontSize = 12.sp,
                            color = colors.textHint
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { (label, accent, value) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isEnabled.value) Color.White.copy(alpha = 0.84f) else Color(0xFFF5F3F8))
                                        .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                                        .clickable(enabled = isEnabled.value) {
                                            AppState.setCountDownType(value)
                                            onClose()
                                        }
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent)
                                        Spacer(Modifier.height(4.dp))
                                        Text("开始", fontSize = 12.sp, color = colors.textHint)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelperCard(colors: com.rollcall.app.ui.theme.AppColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text("操作提示", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "左键单击悬浮球开始点名，长按触发多人抽取，拖拽仍然用于倒计时投放。",
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "拖拽悬浮球到右侧面板顶部的“快捷工具”区域后松手即可打开。",
            fontSize = 13.sp,
            color = colors.textHint,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun TodayInfoCard(colors: com.rollcall.app.ui.theme.AppColors) {
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.CHINESE)
    val dateStr = "${today.monthValue}月${today.dayOfMonth}日"

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
            .background(Color.White.copy(alpha = 0.76f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text("${today.dayOfMonth}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("$dateStr $dayOfWeek", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(if (classCount > 0) "今日共 $classCount 节课" else "今日无课程安排", fontSize = 14.sp, color = colors.textHint)
            }
        }
    }
}

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
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary, textAlign = TextAlign.Center)
    }
}

private data class ToolItem(
    val icon: String,
    val label: String,
    val accentColor: Color,
    val onClick: () -> Unit
)
