package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 上下课提示通知类型
 */
enum class ClassNotificationType {
    CLASS_STARTING,   // 即将上课
    CLASS_ENDING,     // 即将下课
    BREAK_TIME,       // 课间休息
    SCHOOL_OVER       // 放学
}

/**
 * 通知数据
 */
data class ClassNotification(
    val type: ClassNotificationType,
    val title: String,
    val message: String,
    val icon: String,
    val accentColor: Color
)

/**
 * 上下课提示通知管理器
 * 在屏幕右上角弹出通知，自动消失
 */
@Composable
fun ClassNotificationHost() {
    val week = AppState.week.collectAsState()
    val time = AppState.time.collectAsState()
    val isTime = AppState.isTime.collectAsState()
    val subjectList = AppState.subjectList

    // 通知状态
    var currentNotification by remember { mutableStateOf<ClassNotification?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var lastNotificationKey by remember { mutableStateOf("") }

    // 每秒检查通知触发条件
    LaunchedEffect(Unit) {
        while (isActive) {
            if (isTime.value && week.value != "无" && time.value != "无") {
                val todaySchedule = subjectList[week.value]?.schedule
                if (todaySchedule != null) {
                    val notification = checkNotificationTrigger(time.value, todaySchedule)
                    if (notification != null) {
                        val notificationKey = "${notification.type}_${notification.title}_${time.value}"
                        if (notificationKey != lastNotificationKey) {
                            lastNotificationKey = notificationKey
                            currentNotification = notification
                            isVisible = true
                            // 8秒后自动消失
                            delay(8000)
                            isVisible = false
                            delay(500) // 等待退出动画完成
                            currentNotification = null
                        }
                    }
                }
            }
            delay(10000) // 每10秒检查一次
        }
    }

    // 显示通知窗口
    if (currentNotification != null) {
        NotificationPopupWindow(
            notification = currentNotification!!,
            isVisible = isVisible
        )
    }
}

/**
 * 通知弹出窗口
 * 在屏幕右上角显示，带有入场/出场动画
 */
@Composable
private fun NotificationPopupWindow(
    notification: ClassNotification,
    isVisible: Boolean
) {
    Window(
        onCloseRequest = { },
        title = "课程提醒",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        focusable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.TopEnd),
            size = DpSize(380.dp, 120.dp)
        )
    ) {
        setWindowIcon()

        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(tween(300)) + slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(400)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colors.cardBackground,
                                    colors.surface
                                )
                            )
                        )
                        .border(
                            1.dp,
                            notification.accentColor.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 图标
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(notification.accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(notification.icon, fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        // 文字内容
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                notification.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                notification.message,
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // 进度条（倒计时指示）
                        val infiniteTransition = rememberInfiniteTransition()
                        val progressAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                tween(1500),
                                RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(notification.accentColor.copy(alpha = progressAlpha))
                        )
                    }
                }
            }
        }
    }
}

/**
 * 检查是否需要触发通知
 * - 上课前2分钟提醒
 * - 下课前1分钟提醒
 * - 最后一节课结束后放学提醒
 */
private fun checkNotificationTrigger(
    currentTimeStr: String,
    schedule: List<Subject>
): ClassNotification? {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val formattedTime = if (currentTimeStr.length == 4) "0$currentTimeStr" else currentTimeStr
    val now = try {
        LocalTime.parse(formattedTime, formatter)
    } catch (_: Exception) {
        return null
    }

    for (i in schedule.indices) {
        val subject = schedule[i]
        val startTime = parseTimeForNotification(subject.startTime, formatter) ?: continue
        val endTime = parseTimeForNotification(subject.endTime, formatter) ?: continue

        // 上课前2分钟
        val minutesBefore = Duration.between(now, startTime).toMinutes()
        if (minutesBefore in 0..2 && now.isBefore(startTime)) {
            return ClassNotification(
                type = ClassNotificationType.CLASS_STARTING,
                title = "📚 即将上课",
                message = "第${subject.period}节「${subject.subject}」${minutesBefore}分钟后开始",
                icon = "🔔",
                accentColor = Color(0xFF5B8DEF)
            )
        }

        // 下课前1分钟
        val minutesBeforeEnd = Duration.between(now, endTime).toMinutes()
        if (minutesBeforeEnd in 0..1 && !now.isBefore(startTime) && now.isBefore(endTime)) {
            return ClassNotification(
                type = ClassNotificationType.CLASS_ENDING,
                title = "⏰ 即将下课",
                message = "「${subject.subject}」还有${minutesBeforeEnd}分钟下课",
                icon = "🎉",
                accentColor = Color(0xFFFBBF24)
            )
        }

        // 刚下课
        val minutesAfterEnd = Duration.between(endTime, now).toMinutes()
        if (minutesAfterEnd in 0..0 && !now.isBefore(endTime)) {
            if (i < schedule.size - 1) {
                val nextSubject = schedule[i + 1]
                return ClassNotification(
                    type = ClassNotificationType.BREAK_TIME,
                    title = "🎊 课间休息",
                    message = "下节课是「${nextSubject.subject}」，好好休息一下吧",
                    icon = "☕",
                    accentColor = Color(0xFF2ECC71)
                )
            }
        }
    }

    // 检查放学
    if (schedule.isNotEmpty()) {
        val lastSubject = schedule.last()
        val lastEndTime = parseTimeForNotification(lastSubject.endTime, formatter)
        if (lastEndTime != null) {
            val minutesAfterSchool = Duration.between(lastEndTime, now).toMinutes()
            if (minutesAfterSchool in 0..1 && !now.isBefore(lastEndTime)) {
                return ClassNotification(
                    type = ClassNotificationType.SCHOOL_OVER,
                    title = "🏠 放学啦！",
                    message = "今天的课程全部结束了，辛苦了！",
                    icon = "🌟",
                    accentColor = Color(0xFFFF7E6B)
                )
            }
        }
    }

    return null
}

private fun parseTimeForNotification(str: String, formatter: DateTimeFormatter): LocalTime? {
    return try {
        val formatted = if (str.length == 4) "0$str" else str
        LocalTime.parse(formatted, formatter)
    } catch (_: Exception) {
        null
    }
}
