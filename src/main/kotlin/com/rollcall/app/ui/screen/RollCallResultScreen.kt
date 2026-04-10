package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.audio.AudioManager
import com.rollcall.app.drawStar
import com.rollcall.app.setWindowIcon
import com.rollcall.app.state.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import javax.swing.JFrame

/**
 * 点名结果展示窗口
 * 全屏显示被点到的学生姓名，带有动画效果
 *
 * @param selectedStudent 选中的学生（姓, 名）
 * @param driveIsLongPressed 是否是长按模式（选3个学生）
 * @param isReadyVisible 提示文字是否可见
 * @param isFirstNameVisible 名字是否可见
 * @param isLastNameVisible 姓氏是否可见
 * @param isOpenHtml 是否已播放语音
 * @param onOpenHtmlChanged 语音播放状态变更回调
 * @param student1/2/3 长按模式下选中的3个学生
 * @param luckyGuyJson 幸运学生JSON
 * @param poolGuyJson 倒霉学生JSON
 */
@Composable
fun ApplicationScope.RollCallResultWindow(
    selectedStudent: Pair<String, String>?,
    driveIsLongPressed: Boolean,
    isReadyVisible: Boolean,
    isFirstNameVisible: Boolean,
    isLastNameVisible: Boolean,
    isOpenHtml: Boolean,
    onOpenHtmlChanged: (Boolean) -> Unit,
    student1: String, student2: String, student3: String,
    luckyGuyJson: String, poolGuyJson: String
) {
    // 随机选择提示语
    val tips = if (!driveIsLongPressed) {
        listOf("我选好了！", "你猜猜是谁？", "就你了！", "幸运儿是",
            "掌声有请", "你是全班最靓的仔", "让我挑选一个学霸...", "我看好你！")
    } else {
        listOf("我选好了！", "你猜猜是谁？", "就你们了！", "幸运儿是",
            "掌声有请", "你们是全班最靓的仔", "让我挑选一些学霸...", "我看好你们！")
    }
    val randomTips = remember { tips.random() }

    // 解析幸运/倒霉学生列表
    val luckyGuyList = parseSafeJsonList(luckyGuyJson)
    val poolGuyList = parseSafeJsonList(poolGuyJson)

    Window(
        onCloseRequest = ::exitApplication,
        title = "点名系统",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        setWindowIcon()

        // 保持窗口置顶
        LaunchedEffect(Unit) {
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                kotlinx.coroutines.delay(1000)
            }
        }

        // 全屏显示
        LaunchedEffect(window) {
            val screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            window.setSize(screenSize.width, screenSize.height)
            window.extendedState = JFrame.MAXIMIZED_BOTH
        }

        // 播放语音
        LaunchedEffect(isOpenHtml) {
            if (!isOpenHtml) {
                onOpenHtmlChanged(true)
                val audioName = if (!driveIsLongPressed) {
                    selectedStudent?.let { it.first + it.second } ?: ""
                } else {
                    "$student1,$student2,$student3"
                }
                if (audioName.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        AudioManager.playNameAudio(
                            audioName, AppState.url,
                            AppState.isInternetAvailable.value
                        )
                    }
                }
            }
        }

        // 结果展示区域
        val baseAccent = Color(AppState.accentColorNamed)
        val infiniteTransition = rememberInfiniteTransition()
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val floatingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseAccent.copy(alpha = 0.94f),
                            baseAccent.copy(red = 0.12f + baseAccent.red * 0.6f, green = 0.16f + baseAccent.green * 0.55f, blue = 0.25f + baseAccent.blue * 0.5f),
                            Color(0xFF101727)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.28f,
                    center = center.copy(x = size.width * 0.18f, y = size.height * 0.16f)
                )
                drawCircle(
                    color = Color(0xFFFFF1C7).copy(alpha = 0.08f),
                    radius = size.minDimension * 0.24f,
                    center = center.copy(x = size.width * 0.86f, y = size.height * 0.18f)
                )
                drawCircle(
                    color = Color(0xFFBEE3FF).copy(alpha = 0.09f),
                    radius = size.minDimension * 0.22f,
                    center = center.copy(x = size.width * 0.76f, y = size.height * 0.82f)
                )
            }

            if (!driveIsLongPressed) {
                AnimatedVisibility(
                    visible = isFirstNameVisible,
                    enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                            fadeIn(tween(700)) + expandVertically(tween(500)) +
                            slideInHorizontally(tween(500))
                ) {
                    selectedStudent?.let {
                        when {
                            luckyGuyList.contains(it.second) -> {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawStar(size = 1600f, center = center, rotationAngle = angle)
                                }
                            }
                            poolGuyList.contains(it.second) -> {
                                Text(
                                    "🎱",
                                    fontSize = 600.sp,
                                    modifier = Modifier.rotate(25f),
                                    color = Color.White.copy(alpha = 0.16f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp, vertical = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedVisibility(
                    visible = isReadyVisible,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(1000))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        ResultFloatingTag(
                            title = "本轮提示",
                            value = randomTips,
                            alpha = floatingAlpha
                        )
                        ResultFloatingTag(
                            title = if (driveIsLongPressed) "模式" else "状态",
                            value = if (driveIsLongPressed) "三人连抽" else resolveSingleStudentStatus(selectedStudent, luckyGuyList, poolGuyList),
                            alpha = floatingAlpha
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 1500.dp)
                            .clip(RoundedCornerShape(42.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(42.dp))
                            .padding(horizontal = 40.dp, vertical = 34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (driveIsLongPressed) "本轮点到" else "请看大屏",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                        Spacer(Modifier.height(14.dp))

                        if (!driveIsLongPressed) {
                            SingleStudentDisplay(selectedStudent, isLastNameVisible, isFirstNameVisible)
                        } else {
                            ThreeStudentsDisplay(isFirstNameVisible, student1, student2, student3)
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ResultMetaCard(
                                title = "语音播报",
                                value = if (isOpenHtml) "已触发" else "准备中"
                            )
                            ResultMetaCard(
                                title = "网络",
                                value = if (AppState.isInternetAvailable.value) "在线" else "离线"
                            )
                            ResultMetaCard(
                                title = "来源",
                                value = if (driveIsLongPressed) "连抽模式" else "随机点名"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

/**
 * 单人点名显示
 */
@Composable
private fun SingleStudentDisplay(
    student: Pair<String, String>?,
    isLastNameVisible: Boolean,
    isFirstNameVisible: Boolean
) {
    val surname = student?.first ?: ""
    val name = student?.second ?: "某某"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isLastNameVisible,
            enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                    fadeIn(tween(700)) + expandVertically(tween(500)) +
                    slideInHorizontally(tween(500))
        ) {
            NameBlock(
                text = surname.ifBlank { "某" },
                accent = Color(0xFFFFF1C7),
                modifier = Modifier.weight(0.9f)
            )
        }

        Spacer(Modifier.width(22.dp))

        AnimatedVisibility(
            visible = isFirstNameVisible,
            enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                    fadeIn(tween(700)) + expandVertically(tween(500)) +
                    slideInHorizontally(tween(500))
        ) {
            NameBlock(
                text = name,
                accent = Color(0xFFCAE8FF),
                modifier = Modifier.weight(1.35f)
            )
        }
    }
}

@Composable
private fun NameBlock(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(320.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(38.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length <= 1) 230.sp else 210.sp,
            color = Color.White,
            style = TextStyle(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultFloatingTag(
    title: String,
    value: String,
    alpha: Float
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = alpha))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.78f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ResultMetaCard(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.72f))
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun resolveSingleStudentStatus(
    student: Pair<String, String>?,
    luckyGuyList: List<String>,
    poolGuyList: List<String>
): String {
    val name = student?.second ?: return "等待揭晓"
    return when {
        luckyGuyList.contains(name) -> "幸运位"
        poolGuyList.contains(name) -> "焦点位"
        else -> "已锁定"
    }
}

/**
 * 三人点名显示（长按模式）
 */
@Composable
private fun ThreeStudentsDisplay(
    visible: Boolean,
    s1: String, s2: String, s3: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                fadeIn(tween(700)) + expandVertically(tween(500)) +
                slideInHorizontally(tween(500))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            listOf(s1, s2, s3).forEachIndexed { index, name ->
                Text(
                    text = name,
                    fontSize = 120.sp,
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            when (index) {
                                0 -> Color(0xFFFFF1C7).copy(alpha = 0.16f)
                                1 -> Color(0xFFCAE8FF).copy(alpha = 0.16f)
                                else -> Color(0xFFD7F8D7).copy(alpha = 0.16f)
                            }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(32.dp))
                        .padding(vertical = 22.dp),
                    textAlign = TextAlign.Center
                )
                if (index != 2) Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/**
 * 安全解析JSON字符串列表
 */
private fun parseSafeJsonList(json: String): List<String> {
    return try {
        if (json.isNotBlank() && json.trim().startsWith("[")) {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}
