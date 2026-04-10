package com.rollcall.app.ui.screen

import com.rollcall.app.state.AppState.buttonState
import com.rollcall.app.state.AppState.subjectList
import com.rollcall.app.state.AppState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// 自定义倒置三角形形状
object TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            // 将顶点向右拉伸，比如拉伸到宽度的 3/4 处
            moveTo(size.width * 0.75f, size.height)
            // 右边顶点
            lineTo(size.width, 0f)
            // 左边顶点
            lineTo(0f, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}


/**
 * 悬浮窗主体UI
 * 显示圆形浮动按钮，支持课程提醒功能
 * 按钮有动画效果，定期显示表情
 */
@Composable
fun dragWindow() {
    val displayText = buttonState.collectAsState()
    var isClick by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf(false) }

    if (isClick && displayText.value == "点名") {
        isClick = false
        AppState.setButtonState("关闭")
    }

    if (isClick && displayText.value == "关闭") {
        isClick = false
        AppState.setButtonState("点名")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Transparent)
    ) {
        var subject = ""
        val isChangeFace = AppState.isChangeFace.collectAsState()
        val isTime = AppState.isTime.collectAsState()
        val week = AppState.week.collectAsState()
        val time = AppState.time.collectAsState()

        // 判断当前是否课间，显示下节课提醒
        if (isTime.value && week.value != "无" && time.value != "无") {
            val todaySchedule = subjectList[week.value]
            if (todaySchedule != null) {
                val nextClass = getNextClassIfDismissalTime(time.value, todaySchedule.schedule)
                if (nextClass != null && nextClass != "未下课") {
                    subject = nextClass
                    AppState.setIsChangeFace(true)
                } else {
                    AppState.setIsChangeFace(false)
                }
            }
        } else {
            AppState.setIsChangeFace(false)
        }

        var isAnimated by remember { mutableStateOf(false) }
        var isChange by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { delay(1000); isAnimated = true }
        LaunchedEffect(isChangeFace.value) {
            isAnimated = false; delay(500); isAnimated = true
        }
        LaunchedEffect(isChangeFace.value) {
            delay(1000); isChange = isChangeFace.value
        }
        LaunchedEffect(longPressed) {
            if (longPressed) {
                delay(2000)
                longPressed = false
                AppState.setIsLongPressed(false)
            }
        }

        AnimatedVisibility(
            visible = isAnimated,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)) + slideInVertically(
                initialOffsetY = { -80 },
                animationSpec = tween(durationMillis = 500)
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // 课程提醒气泡
                if (isChange) {
                    Surface(
                        modifier = Modifier.background(Color.Transparent)
                            .height(100.dp).width(300.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        Box(
                            modifier = Modifier.background(Color(0xFF2D2D3F).copy(alpha = 0.85f))
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                                    text = "下节课是",
                                    fontSize = 35.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${subject}课",
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                                    fontSize = 35.sp,
                                    style = TextStyle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFF6B6B),
                                                Color(0xFFFF8E8E),
                                                Color(0xFFFF6B6B)
                                            )
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    modifier = Modifier.padding(end = 10.dp, top = 10.dp, bottom = 10.dp),
                                    text = "~",
                                    fontSize = 35.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    // 三角形指示器
                    Surface(
                        modifier = Modifier.background(Color.Transparent)
                            .size(20.dp).clip(TriangleShape)
                    ) {
                        Box(modifier = Modifier.background(Color(0xFF2D2D3F).copy(alpha = 0.85f)))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 悬浮按钮动画
                var rotation by remember { mutableStateOf(0f) }
                var scale by remember { mutableStateOf(1f) }

                val animatedRotation by animateFloatAsState(
                    targetValue = rotation,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing),
                    label = ""
                )
                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing),
                    label = ""
                )

                var isFirstRun by remember { mutableStateOf(false) }

                // 定期表情动画
                LaunchedEffect(Unit) {
                    while (isActive) {
                        if (isFirstRun) {
                            val randomNumber = (1..2).random()
                            if (randomNumber == 1 && buttonState.value != "关闭") {
                                AppState.setButtonState("^_^")
                                delay(3000)
                                if (buttonState.value != "关闭") AppState.setButtonState("点名")
                            } else if (buttonState.value != "关闭") {
                                scale = 1.3f; rotation = 360f
                                delay(1200)
                                scale = 1f; rotation = 0f
                                delay(1200)
                                if (buttonState.value != "关闭") {
                                    AppState.setButtonState("＠_＠")
                                    delay(3000)
                                    if (buttonState.value != "关闭") AppState.setButtonState("点名")
                                }
                            }
                        } else {
                            isFirstRun = true
                        }
                        delay((1..10000).random().toLong() + 60000)
                    }
                }

                // 现代化浮动按钮
                Surface(
                    modifier = Modifier.background(Color.Transparent)
                        .height(100.dp).width(100.dp)
                        .clip(CircleShape)
                        .rotate(animatedRotation)
                        .scale(animatedScale)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(AppState.accentColorFloating),
                                        Color(AppState.accentColorFloating).copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = displayText.value,
                            fontSize = 30.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2D2D3F)
                        )
                    }
                }
            }
        }
    }
}