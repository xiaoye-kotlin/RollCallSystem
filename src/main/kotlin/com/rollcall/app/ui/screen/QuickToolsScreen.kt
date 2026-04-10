package com.rollcall.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay
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
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60)
        showContent = true
    }

    Window(
        onCloseRequest = onClose,
        title = "快捷工具",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.CenterEnd),
            size = DpSize(368.dp, 660.dp)
        )
    ) {
        AppTheme {
            val colors = AppTheme.colors
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDFBFF),
                                Color(0xFFF7FBFF),
                                Color(0xFFFFFBF5)
                            )
                        )
                    )
                    .border(1.dp, colors.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 6 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("快捷工具", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text("全部功能一屏可见", fontSize = 12.sp, color = colors.textHint)
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
                }

                Spacer(Modifier.height(14.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(260, delayMillis = 40)) + scaleIn(
                        animationSpec = tween(260, delayMillis = 40, easing = FastOutSlowInEasing),
                        initialScale = 0.96f
                    )
                ) {
                    TodayInfoCard(colors)
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(260, delayMillis = 80))
                ) {
                    Text(
                        "教学常用",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textHint,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                toolRows.forEachIndexed { rowIndex, row ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(240, delayMillis = 110 + rowIndex * 60)) +
                            slideInVertically(tween(260, delayMillis = 110 + rowIndex * 60)) { it / 6 }
                    ) {
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
                    }
                    if (rowIndex != toolRows.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.weight(1f))

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(260, delayMillis = 240))
                ) {
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
}

@Composable
fun CountdownQuickPickerWindow(onClose: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(40)
        showContent = true
    }

    Window(
        onCloseRequest = onClose,
        title = "倒计时",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(328.dp, 296.dp)
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
                    .animateContentSize()
            ) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 6 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("倒计时", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text(
                                "点击时长后立即开始",
                                fontSize = 12.sp,
                                color = colors.textHint
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                options.chunked(2).forEachIndexed { rowIndex, row ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(230, delayMillis = 70 + rowIndex * 70)) +
                            scaleIn(animationSpec = tween(240, delayMillis = 70 + rowIndex * 70), initialScale = 0.95f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { (label, accent, value) ->
                                CountdownOptionCard(
                                    label = label,
                                    accent = accent,
                                    enabled = true,
                                    onClick = {
                                        AppState.setCountDownType(value)
                                        onClose()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (rowIndex != 1) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownOptionCard(
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.98f,
        animationSpec = tween(180)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Color.White.copy(alpha = 0.84f) else Color(0xFFF5F3F8))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp)
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(if (enabled) "开始" else "未开启", fontSize = 12.sp, color = Color(0xFF8A8298))
        }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .animateContentSize()
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
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(220))

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, accentColor.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
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
