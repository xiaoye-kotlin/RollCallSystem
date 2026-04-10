package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.data.model.Subject
import com.rollcall.app.setWindowIcon
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.awt.GraphicsEnvironment
import java.awt.Point
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
            size = DpSize(760.dp, 840.dp)
        )
    ) {
        setWindowIcon()

        AppTheme {
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
            val todayKey = resolveWeekKey(week.value)
            val todaySchedule = subjectList[todayKey]?.schedule
            val currentTimeStr = resolveCurrentTimeStr(time.value)
            val currentInfo = remember(tick, currentTimeStr, todaySchedule) {
                computeCurrentClassInfo(currentTimeStr, todaySchedule)
            }
            val currentSubject = todaySchedule?.getOrNull(currentInfo.currentIndex)
            val nextSubject = todaySchedule?.getOrNull(currentInfo.nextIndex)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.gradient1,
                                colors.gradient2,
                                colors.surfaceVariant
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.16f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    ScheduleHeader(
                        colors = colors,
                        todayKey = todayKey,
                        currentInfo = currentInfo,
                        onClose = onClose
                    )

                    Spacer(Modifier.height(12.dp))

                    ScheduleFocusBoard(
                        todayKey = todayKey,
                        currentInfo = currentInfo,
                        currentSubject = currentSubject,
                        nextSubject = nextSubject,
                        currentTimeStr = currentTimeStr,
                        colors = colors
                    )

                    Spacer(Modifier.height(12.dp))

                    if (todaySchedule != null && todaySchedule.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 18.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(colors.cardBackground.copy(alpha = 0.9f))
                                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "今日时间轴",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${todaySchedule.size} 节课 · ${todaySchedule.first().startTime} - ${todaySchedule.last().endTime}",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                currentInfo.currentIndex >= 0 -> "正在进行"
                                                currentInfo.nextIndex >= 0 -> "等待上课"
                                                else -> "今日结束"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                    }
                                }
                            }

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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .background(colors.cardBackground.copy(alpha = 0.92f))
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 60.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (todayKey == "无") "暂未获取到星期信息" else "今天没有课程安排",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (todayKey == "无") "请先等待网络时间与星期同步。" else "可以去处理别的功能，不用盯着课表。",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleWidgetWindow() {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.TopCenter),
        size = DpSize(560.dp, 820.dp)
    )

    Window(
        onCloseRequest = { },
        title = "课程表小组件",
        undecorated = true,
        transparent = true,
        alwaysOnTop = false,
        resizable = false,
        focusable = false,
        state = windowState
    ) {
        setWindowIcon()

        AppTheme {
            val composeWindow = remember { window }
            val week = AppState.week.collectAsState()
            val time = AppState.time.collectAsState()
            val subjectList = AppState.subjectList
            var tick by remember { mutableStateOf(0) }
            var isDraggingWidget by remember { mutableStateOf(false) }
            var lastMouseLocation by remember { mutableStateOf<Pair<Int, Int>?>(null) }

            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(1000)
                    tick++
                }
            }

            LaunchedEffect(composeWindow) {
                delay(300)
                javax.swing.SwingUtilities.invokeLater {
                    composeWindow.location = readScheduleWidgetPosition()
                        ?: defaultScheduleWidgetPosition(composeWindow)
                    composeWindow.isAlwaysOnTop = false
                    composeWindow.toBack()
                }
            }

            DisposableEffect(composeWindow) {
                val headerDragHeightPx = 72
                val mouseListener = object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(e: java.awt.event.MouseEvent?) {
                        if (e == null || !javax.swing.SwingUtilities.isLeftMouseButton(e)) return
                        if (e.y > headerDragHeightPx) return
                        isDraggingWidget = true
                        lastMouseLocation = e.locationOnScreen.let { Pair(it.x, it.y) }
                    }

                    override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                        isDraggingWidget = false
                        saveScheduleWidgetPosition(composeWindow.location)
                        lastMouseLocation = null
                    }
                }
                val mouseMotionListener = object : java.awt.event.MouseMotionAdapter() {
                    override fun mouseDragged(e: java.awt.event.MouseEvent?) {
                        if (e == null || !isDraggingWidget) return
                        lastMouseLocation?.let { lastLoc ->
                            val dx = e.locationOnScreen.x - lastLoc.first
                            val dy = e.locationOnScreen.y - lastLoc.second
                            if (dx != 0 || dy != 0) {
                                val newLocation = composeWindow.location.apply {
                                    x += dx
                                    y += dy
                                }
                                javax.swing.SwingUtilities.invokeLater {
                                    composeWindow.location = newLocation
                                }
                                lastMouseLocation = e.locationOnScreen.let { Pair(it.x, it.y) }
                            }
                        }
                    }
                }

                composeWindow.addMouseListener(mouseListener)
                composeWindow.addMouseMotionListener(mouseMotionListener)
                onDispose {
                    composeWindow.removeMouseListener(mouseListener)
                    composeWindow.removeMouseMotionListener(mouseMotionListener)
                }
            }

            val todayKey = resolveWeekKey(week.value)
            val currentTimeStr = resolveCurrentTimeStr(time.value)
            val todaySchedule = subjectList[todayKey]?.schedule.orEmpty()
            val currentInfo = remember(tick, currentTimeStr, todaySchedule) {
                computeCurrentClassInfo(currentTimeStr, todaySchedule)
            }
            val weekDays = remember {
                listOf(
                    "星期一" to "周一",
                    "星期二" to "周二",
                    "星期三" to "周三",
                    "星期四" to "周四",
                    "星期五" to "周五"
                )
            }
            val totalClasses = weekDays.sumOf { (dayKey, _) -> subjectList[dayKey]?.schedule?.size ?: 0 }
            val maxPeriods = remember(subjectList) {
                weekDays.maxOfOrNull { (dayKey, _) -> subjectList[dayKey]?.schedule?.size ?: 0 } ?: 0
            }
            val timeTemplate = remember(subjectList, weekDays) {
                buildTimeTemplate(subjectList, weekDays.map { it.first }, maxPeriods)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFDFBFF).copy(alpha = 0.94f),
                                Color(0xFFF6FAFF).copy(alpha = 0.94f),
                                Color(0xFFFFFBF6).copy(alpha = 0.94f)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFE3DCF1).copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFE4EC),
                                    Color(0xFFE3F2FF),
                                    Color(0xFFE8F8E8)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFF1D0DD), RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "课表",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF584B6B)
                        )
                        Text(
                            text = "$todayKey · $currentTimeStr · ${totalClasses}节",
                            fontSize = 11.sp,
                            color = Color(0xFF7C748C)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.72f))
                            .border(1.dp, Color(0xFFE6D8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "拖动顶部",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7A6690)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WidgetStatusChip(
                        label = "当前",
                        value = currentInfo.currentClassName,
                        accentColor = Color(0xFF70C9A9),
                        modifier = Modifier.weight(1f)
                    )
                    WidgetStatusChip(
                        label = "下节",
                        value = currentInfo.nextClassName,
                        accentColor = Color(0xFF7EB6FF),
                        modifier = Modifier.weight(1f)
                    )
                    WidgetStatusChip(
                        label = "剩余",
                        value = currentInfo.remainingTime,
                        accentColor = if (currentInfo.remainingMinutes in 1..5) Color(0xFFFFB36D) else Color(0xFFA389F4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        WidgetHeaderCell(
                            title = "节次",
                            subtitle = summarizeDayRange(todaySchedule),
                            background = Color(0xFFFDF1C9),
                            textColor = Color(0xFF7A5E1F),
                            isToday = false,
                            modifier = Modifier.width(72.dp)
                        )
                        weekDays.forEachIndexed { index, (dayKey, dayLabel) ->
                            WidgetHeaderCell(
                                title = dayLabel,
                                subtitle = if (dayKey == todayKey) "今天" else "${subjectList[dayKey]?.schedule?.size ?: 0} 节",
                                background = macaronColorForIndex(index),
                                textColor = Color(0xFF5E5470),
                                isToday = dayKey == todayKey,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (maxPeriods == 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "还没有课程表数据",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8A819A)
                            )
                        }
                    } else {
                        for (rowIndex in 0 until maxPeriods) {
                            val slot = timeTemplate.getOrNull(rowIndex)
                            val isSessionStart = rowIndex == 0 ||
                                timeTemplate.getOrNull(rowIndex - 1)?.session != slot?.session
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                PeriodTimeCell(
                                    period = rowIndex + 1,
                                    slot = slot,
                                    isSessionStart = isSessionStart && rowIndex != 0,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .fillMaxHeight()
                                )
                                weekDays.forEachIndexed { dayIndex, (dayKey, _) ->
                                    val subjects = subjectList[dayKey]?.schedule.orEmpty()
                                    val subject = subjects.getOrNull(rowIndex)
                                    val isCurrent = dayKey == todayKey && currentInfo.currentIndex == rowIndex
                                    val isNext = dayKey == todayKey && currentInfo.nextIndex == rowIndex
                                    WeeklyCourseCell(
                                        subject = subject,
                                        isCurrent = isCurrent,
                                        isNext = isNext,
                                        isToday = dayKey == todayKey,
                                        background = blendCellColor(macaronColorForIndex(dayIndex), slot?.session),
                                        isSessionStart = isSessionStart && rowIndex != 0,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.cardBackground.copy(alpha = 0.9f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "今日课程",
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

        Spacer(Modifier.height(6.dp))
        Text(
            text = formatTodayHeadline(todayKey, currentInfo),
            fontSize = 14.sp,
            color = colors.textSecondary
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoChip(
                icon = "当前",
                label = "当前",
                value = currentInfo.currentClassName,
                accentColor = colors.success,
                bgColor = Color(0xFFE9F9F2),
                borderColor = colors.success.copy(alpha = 0.3f),
                textColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            InfoChip(
                icon = "下节",
                label = "下节",
                value = currentInfo.nextClassName,
                accentColor = colors.primary,
                bgColor = Color(0xFFEDF4FF),
                borderColor = colors.primary.copy(alpha = 0.3f),
                textColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            InfoChip(
                icon = "剩余",
                label = "剩余",
                value = currentInfo.remainingTime,
                accentColor = if (currentInfo.remainingMinutes in 1..5) colors.warning else colors.accent,
                bgColor = Color(0xFFFFF5EA),
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
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(icon, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = accentColor.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScheduleFocusBoard(
    todayKey: String,
    currentInfo: ClassInfo,
    currentSubject: Subject?,
    nextSubject: Subject?,
    currentTimeStr: String,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBackground.copy(alpha = 0.92f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        todayKey == "无" -> "等待星期信息"
                        currentSubject != null -> "当前进度"
                        nextSubject != null -> "下一节课"
                        else -> "今日已结束"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        currentSubject != null -> "${currentSubject.subject} · ${currentSubject.startTime} - ${currentSubject.endTime}"
                        nextSubject != null -> "${nextSubject.subject} · ${nextSubject.startTime} 开始"
                        todayKey == "无" -> "网络时间还没有同步成功"
                        else -> "今天的课程已经全部结束"
                    },
                    fontSize = 15.sp,
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            currentSubject != null -> colors.success.copy(alpha = 0.12f)
                            nextSubject != null -> colors.primary.copy(alpha = 0.12f)
                            else -> colors.accent.copy(alpha = 0.12f)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = currentTimeStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val progress = remember(currentTimeStr, currentSubject) {
            computeClassProgress(currentTimeStr, currentSubject)
        }
        MiniTimelineBar(
            progress = progress,
            accent = when {
                currentSubject != null -> colors.success
                nextSubject != null -> colors.primary
                else -> colors.accent
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusMetaCard(
                title = "状态",
                value = when {
                    currentSubject != null -> "正在上课"
                    nextSubject != null -> "等待上课"
                    todayKey == "无" -> "未同步"
                    else -> "放学中"
                },
                accent = when {
                    currentSubject != null -> colors.success
                    nextSubject != null -> colors.primary
                    todayKey == "无" -> colors.warning
                    else -> colors.accent
                },
                modifier = Modifier.weight(1f)
            )
            FocusMetaCard(
                title = "剩余",
                value = currentInfo.remainingTime,
                accent = if (currentInfo.remainingMinutes in 1..5) colors.warning else colors.accent,
                modifier = Modifier.weight(1f)
            )
            FocusMetaCard(
                title = "下节",
                value = nextSubject?.subject ?: currentInfo.nextClassName,
                accent = colors.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniTimelineBar(
    progress: Float,
    accent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE9EEF8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.7f), accent)
                    )
                )
        )
    }
}

@Composable
private fun FocusMetaCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(text = title, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF29334D),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(
                width = if (isCurrent) 3.dp else if (isNext) 2.dp else 1.dp,
                color = borderColor.let { if (isCurrent) it.copy(alpha = pulseAlpha) else it },
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "第${subject.period}节",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${subject.startTime}\n${subject.endTime}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject.subject,
                    fontSize = 22.sp,
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
                        Text("当前", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.success)
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
                        Text("下一节", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${subject.startTime} - ${subject.endTime} · ${resolveCourseSessionLabel(subject.startTime)}",
                fontSize = 14.sp,
                color = colors.textSecondary
            )
            if (subject.dismissalTime.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "下课时间 ${subject.dismissalTime}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textHint
                )
            }
        }
    }
}

private fun formatTodayHeadline(todayKey: String, currentInfo: ClassInfo): String = when {
    todayKey == "无" -> "正在等待网络时间同步，课表会在识别到星期后自动切换。"
    currentInfo.currentIndex >= 0 -> "今天是 $todayKey，当前处于上课阶段，时间轴会自动高亮当前节次。"
    currentInfo.nextIndex >= 0 -> "今天是 $todayKey，当前还没到上课时间或处于课间。"
    else -> "今天是 $todayKey，课程已结束。"
}

private fun computeClassProgress(currentTimeStr: String, subject: Subject?): Float {
    if (subject == null) return 0f
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = parseTime(currentTimeStr, formatter) ?: return 0f
    val start = parseTime(subject.startTime, formatter) ?: return 0f
    val end = parseTime(subject.endTime, formatter) ?: return 0f
    val total = Duration.between(start, end).toMinutes().coerceAtLeast(1)
    val elapsed = Duration.between(start, now).toMinutes()
    return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun resolveCourseSessionLabel(startTime: String): String = when (resolveDaySession(startTime, 0)) {
    DaySession.MORNING -> "上午"
    DaySession.AFTERNOON -> "下午"
    DaySession.EVENING -> "晚上"
}

@Composable
private fun WidgetHeaderCell(
    title: String,
    subtitle: String,
    background: Color,
    textColor: Color,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isToday) Color(0xFF8D7DFF) else Color.White.copy(alpha = 0.8f),
        animationSpec = tween(240)
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isToday) 1.02f else 1f,
        animationSpec = tween(240)
    )

    Column(
        modifier = modifier
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isToday) Color(0xFFF0E6FF) else background.copy(alpha = 0.9f))
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 9.sp, color = textColor.copy(alpha = 0.8f), maxLines = 1)
    }
}

@Composable
private fun WidgetStatusChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, accentColor.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E5B57),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PeriodTimeCell(
    period: Int,
    slot: PeriodTimeSlot?,
    isSessionStart: Boolean,
    modifier: Modifier = Modifier
) {
    val sessionStyle = sessionStyle(slot?.session ?: DaySession.MORNING)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(sessionStyle.softBackground)
            .border(1.dp, sessionStyle.borderColor, RoundedCornerShape(16.dp))
            .drawBehind {
                if (isSessionStart) {
                    drawRoundRect(
                        color = sessionStyle.borderColor.copy(alpha = 0.95f),
                        topLeft = Offset.Zero,
                        size = size.copy(height = 4.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("第${period}节", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = sessionStyle.textColor)
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${slot?.startTime ?: "--:--"}-${slot?.endTime ?: "--:--"}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5D576A),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeeklyCourseCell(
    subject: Subject?,
    isCurrent: Boolean,
    isNext: Boolean,
    isToday: Boolean,
    background: Color,
    isSessionStart: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = when {
        isCurrent -> Color(0xFF49B98A)
        isNext -> Color(0xFF7EB6FF)
        isToday -> Color(0xFF8D7DFF)
        else -> Color(0xFFD8D2E8)
    }
    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(220)
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isCurrent) 1.03f else if (isToday) 1.015f else 1f,
        animationSpec = tween(220)
    )

    Column(
        modifier = modifier
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isCurrent -> Color(0xFFE3FAEF)
                    isToday -> Color(0xFFF4ECFF)
                    subject == null -> Color(0xFFF8F6FB).copy(alpha = 0.72f)
                    else -> background.copy(alpha = 0.84f)
                }
            )
            .border(
                width = if (isCurrent) 3.dp else if (isToday) 2.dp else 1.dp,
                color = animatedAccentColor.copy(alpha = if (subject == null) 0.35f else if (isCurrent) 0.95f else 0.85f),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                if (isSessionStart) {
                    drawRoundRect(
                        color = animatedAccentColor.copy(alpha = 0.55f),
                        topLeft = Offset.Zero,
                        size = size.copy(height = 3.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 3.dp, vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (subject == null) {
            Text(
                text = "空课",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA69DB8)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = subject.subject,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF554C67),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent || isNext) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    ) {
                    }
                }
            }
            if (isCurrent) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "正在上课",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedAccentColor
                )
            } else if (isToday) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "今天",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = animatedAccentColor
                )
            }
        }
    }
}

private enum class DaySession {
    MORNING,
    AFTERNOON,
    EVENING
}

private data class PeriodTimeSlot(
    val startTime: String,
    val endTime: String,
    val session: DaySession
)

private data class SessionStyle(
    val label: String,
    val softBackground: Color,
    val chipColor: Color,
    val borderColor: Color,
    val textColor: Color
)

private fun buildTimeTemplate(
    subjectList: Map<String, com.rollcall.app.data.model.DailySchedule>,
    weekDays: List<String>,
    maxPeriods: Int
): List<PeriodTimeSlot> {
    val template = MutableList<PeriodTimeSlot?>(maxPeriods) { null }
    for (dayKey in weekDays) {
        val schedule = subjectList[dayKey]?.schedule.orEmpty()
        schedule.forEachIndexed { index, subject ->
            if (index in template.indices && template[index] == null) {
                template[index] = PeriodTimeSlot(
                    startTime = subject.startTime,
                    endTime = subject.endTime,
                    session = resolveDaySession(subject.startTime, index)
                )
            }
        }
    }
    return template.map {
        it ?: PeriodTimeSlot("--:--", "--:--", resolveDaySession("--:--", 0))
    }
}

private fun macaronColorForIndex(index: Int): Color {
    val palette = listOf(
        Color(0xFFFFE2EC),
        Color(0xFFE1F0FF),
        Color(0xFFE5F8E7),
        Color(0xFFFFF2D9),
        Color(0xFFECE6FF),
        Color(0xFFFFEAD8),
        Color(0xFFE4F7F7)
    )
    return palette[index % palette.size]
}

private fun summarizeDayRange(schedule: List<Subject>): String {
    if (schedule.isEmpty()) return "无"
    val first = schedule.first()
    val last = schedule.last()
    return "${first.startTime}-${last.endTime}"
}

private fun resolveDaySession(startTime: String, index: Int): DaySession {
    val hour = normalizeTimeStr(startTime).takeIf { it.contains(":") }
        ?.substringBefore(":")
        ?.toIntOrNull()

    return when {
        hour != null && hour < 12 -> DaySession.MORNING
        hour != null && hour < 18 -> DaySession.AFTERNOON
        hour != null -> DaySession.EVENING
        index < 4 -> DaySession.MORNING
        index < 8 -> DaySession.AFTERNOON
        else -> DaySession.EVENING
    }
}

private fun sessionStyle(session: DaySession): SessionStyle = when (session) {
    DaySession.MORNING -> SessionStyle(
        label = "上午",
        softBackground = Color(0xFFFFF8E9),
        chipColor = Color(0xFFFFE4A8),
        borderColor = Color(0xFFF3D489),
        textColor = Color(0xFF86631D)
    )

    DaySession.AFTERNOON -> SessionStyle(
        label = "下午",
        softBackground = Color(0xFFFBEFFF),
        chipColor = Color(0xFFE8CCFF),
        borderColor = Color(0xFFDDB8F8),
        textColor = Color(0xFF77538D)
    )

    DaySession.EVENING -> SessionStyle(
        label = "晚上",
        softBackground = Color(0xFFEEF6FF),
        chipColor = Color(0xFFCDE6FF),
        borderColor = Color(0xFFB3D8FF),
        textColor = Color(0xFF456B91)
    )
}

private fun blendCellColor(dayColor: Color, session: DaySession?): Color {
    val sessionBackground = sessionStyle(session ?: DaySession.MORNING).softBackground
    return Color(
        red = (dayColor.red + sessionBackground.red) / 2f,
        green = (dayColor.green + sessionBackground.green) / 2f,
        blue = (dayColor.blue + sessionBackground.blue) / 2f,
        alpha = 0.88f
    )
}

private fun readScheduleWidgetPosition(): Point? {
    val raw = FileHelper.readFromFile(SCHEDULE_WIDGET_POSITION_FILE).trim()
    if (raw == "404" || raw.isEmpty()) return null
    val parts = raw.split(",")
    if (parts.size != 2) return null
    val x = parts[0].trim().toIntOrNull() ?: return null
    val y = parts[1].trim().toIntOrNull() ?: return null
    return Point(x, y)
}

private fun saveScheduleWidgetPosition(point: Point) {
    FileHelper.writeToFile(SCHEDULE_WIDGET_POSITION_FILE, "${point.x},${point.y}")
}

private fun defaultScheduleWidgetPosition(window: java.awt.Window): Point {
    val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    return Point(
        bounds.x + bounds.width - window.width - SCHEDULE_WIDGET_EDGE_MARGIN,
        bounds.y + SCHEDULE_WIDGET_EDGE_MARGIN
    )
}

private const val SCHEDULE_WIDGET_POSITION_FILE = "D:/Xiaoye/ScheduleWidgetPosition.txt"
private const val SCHEDULE_WIDGET_EDGE_MARGIN = 24

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
