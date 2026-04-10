package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.data.model.Subject
import com.rollcall.app.setWindowIcon
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 桌面课程表窗口
 * 显示本周每日课程安排，高亮当前课程，显示下节课和剩余时间
 *
 * @param onClose 关闭窗口的回调
 */
@Composable
fun ScheduleWindow(onClose: () -> Unit) {
    Window(
        onCloseRequest = onClose,
        title = "课程表",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(680.dp, 760.dp)
        )
    ) {
        setWindowIcon()

        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors
            val week = AppState.week.collectAsState()
            val time = AppState.time.collectAsState()
            val subjectList = AppState.subjectList

            // 每秒刷新一次当前时间状态
            var tick by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(1000)
                    tick++
                }
            }

            // 计算当前课程信息
            val todayKey = week.value
            val todaySchedule = subjectList[todayKey]?.schedule
            val currentTimeStr = time.value
            val currentInfo = remember(tick, currentTimeStr, todaySchedule) {
                computeCurrentClassInfo(currentTimeStr, todaySchedule)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(colors.gradient1, colors.gradient2)
                        )
                    )
            ) {
                // 顶部标题栏
                ScheduleHeader(
                    colors = colors,
                    todayKey = todayKey,
                    currentInfo = currentInfo,
                    onClose = onClose
                )

                // 当日课程列表
                if (todaySchedule != null && todaySchedule.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        itemsIndexed(
                            items = todaySchedule,
                            key = { index, _ -> "class_$index" }
                        ) { index, subject ->
                            ScheduleClassCard(
                                subject = subject,
                                isCurrent = currentInfo.currentIndex == index,
                                isNext = currentInfo.nextIndex == index,
                                colors = colors
                            )
                        }
                    }
                } else {
                    // 无课程提示
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 60.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (todayKey == "无") "暂未获取到星期信息" else "今天没有课程安排",
                                fontSize = 20.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课程表顶部信息区
 * 包含标题、当前课/下节课信息、剩余时间指示
 */
@Composable
private fun ScheduleHeader(
    colors: com.rollcall.app.ui.theme.AppColors,
    todayKey: String,
    currentInfo: ClassInfo,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "今日课表",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (todayKey != "无") {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(todayKey, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    }
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 当前/下节课信息卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 当前课程卡片
            InfoChip(
                icon = "📖",
                label = "当前",
                value = currentInfo.currentClassName,
                accentColor = colors.success,
                bgColor = colors.cardBackground,
                borderColor = colors.success.copy(alpha = 0.3f),
                textColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            // 下节课卡片
            InfoChip(
                icon = "➡️",
                label = "下节",
                value = currentInfo.nextClassName,
                accentColor = colors.primary,
                bgColor = colors.cardBackground,
                borderColor = colors.primary.copy(alpha = 0.3f),
                textColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            // 剩余时间卡片
            InfoChip(
                icon = "⏱",
                label = "剩余",
                value = currentInfo.remainingTime,
                accentColor = if (currentInfo.remainingMinutes in 1..5) colors.warning else colors.accent,
                bgColor = colors.cardBackground,
                borderColor = if (currentInfo.remainingMinutes in 1..5) colors.warning.copy(alpha = 0.3f)
                else colors.accent.copy(alpha = 0.3f),
                textColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 信息指示卡片组件
 */
@Composable
private fun InfoChip(
    icon: String,
    label: String,
    value: String,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 单节课程卡片
 * 当前课程高亮显示，下节课带蓝色指示
 */
@Composable
private fun ScheduleClassCard(
    subject: Subject,
    isCurrent: Boolean,
    isNext: Boolean,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    val borderColor = when {
        isCurrent -> colors.success
        isNext -> colors.primary.copy(alpha = 0.5f)
        else -> colors.cardBorder
    }
    val bgColor = when {
        isCurrent -> colors.success.copy(alpha = 0.06f)
        isNext -> colors.primary.copy(alpha = 0.04f)
        else -> colors.cardBackground
    }
    val badgeColor = when {
        isCurrent -> colors.success
        isNext -> colors.primary
        else -> colors.textHint
    }

    // 脉冲动画 - 当前课程
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = borderColor.let { if (isCurrent) it.copy(alpha = pulseAlpha) else it },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 节次指示
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${subject.period}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
        Spacer(Modifier.width(12.dp))

        // 课程名和时间
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject.subject,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrent) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.success.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("进行中", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.success)
                    }
                }
                if (isNext) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("下节课", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${subject.startTime} — ${subject.endTime}",
                fontSize = 14.sp,
                color = colors.textHint
            )
        }

        // 下课时间
        if (subject.dismissalTime.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text("下课", fontSize = 11.sp, color = colors.textHint)
                Text(
                    subject.dismissalTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// ==================== 课程信息数据类 ====================

/**
 * 当前课程状态信息
 */
data class ClassInfo(
    val currentClassName: String = "无课程",
    val nextClassName: String = "无",
    val remainingTime: String = "--",
    val remainingMinutes: Int = -1,
    val currentIndex: Int = -1,
    val nextIndex: Int = -1
)

/**
 * 根据当前时间和今日课程表，计算当前正在上课、下节课、剩余时间等信息
 */
/**
 * 将 "H:mm" 或 "HH:mm" 格式的时间字符串规范化为 "HH:mm"
 */
private fun normalizeTimeStr(time: String): String =
    if (time.length == 4) "0$time" else time

private fun computeCurrentClassInfo(currentTimeStr: String, schedule: List<Subject>?): ClassInfo {
    if (schedule.isNullOrEmpty() || currentTimeStr == "无") return ClassInfo()

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = try { LocalTime.parse(normalizeTimeStr(currentTimeStr), formatter) } catch (_: Exception) { return ClassInfo() }

    var currentIndex = -1
    var nextIndex = -1

    for (i in schedule.indices) {
        val s = schedule[i]
        val start = parseTime(s.startTime, formatter) ?: continue
        val end = parseTime(s.endTime, formatter) ?: continue

        if (!now.isBefore(start) && now.isBefore(end)) {
            currentIndex = i
        }
    }

    // 找下节课
    if (currentIndex >= 0 && currentIndex + 1 < schedule.size) {
        nextIndex = currentIndex + 1
    } else if (currentIndex < 0) {
        // 还没有开始上课或课间，找最近的下节课
        for (i in schedule.indices) {
            val start = parseTime(schedule[i].startTime, formatter) ?: continue
            if (now.isBefore(start)) {
                nextIndex = i
                break
            }
        }
    }

    val currentName = if (currentIndex >= 0) schedule[currentIndex].subject else "课间/未上课"
    val nextName = if (nextIndex >= 0) schedule[nextIndex].subject else "无"

    // 计算剩余时间
    val remainingMinutes: Int
    val remainingStr: String

    if (currentIndex >= 0) {
        val endTime = parseTime(schedule[currentIndex].endTime, formatter)
        if (endTime != null) {
            val mins = Duration.between(now, endTime).toMinutes().toInt()
            remainingMinutes = mins
            remainingStr = if (mins > 0) "${mins}分钟" else "即将下课"
        } else {
            remainingMinutes = -1
            remainingStr = "--"
        }
    } else if (nextIndex >= 0) {
        val startTime = parseTime(schedule[nextIndex].startTime, formatter)
        if (startTime != null) {
            val mins = Duration.between(now, startTime).toMinutes().toInt()
            remainingMinutes = mins
            remainingStr = if (mins > 0) "${mins}分钟后上课" else "即将上课"
        } else {
            remainingMinutes = -1
            remainingStr = "--"
        }
    } else {
        remainingMinutes = -1
        remainingStr = "已放学"
    }

    return ClassInfo(
        currentClassName = currentName,
        nextClassName = nextName,
        remainingTime = remainingStr,
        remainingMinutes = remainingMinutes,
        currentIndex = currentIndex,
        nextIndex = nextIndex
    )
}

private fun parseTime(str: String, formatter: DateTimeFormatter): LocalTime? {
    return try {
        LocalTime.parse(normalizeTimeStr(str), formatter)
    } catch (_: Exception) {
        null
    }
}
