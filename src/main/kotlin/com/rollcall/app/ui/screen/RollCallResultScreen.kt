package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
        // 保持窗口置顶
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    window.isMinimized = false
                    window.isAlwaysOnTop = true
                    kotlinx.coroutines.delay(1000)
                }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(AppState.accentColorNamed)),
            contentAlignment = Alignment.Center
        ) {
            // 幸运学生星星动画
            val angle by animateFloatAsState(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, delayMillis = 500),
                    repeatMode = RepeatMode.Restart
                )
            )

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
                                Text("🎱", fontSize = 650.sp, modifier = Modifier.rotate(45f))
                            }
                        }
                    }
                }
            }

            // 提示文字和学生姓名
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 提示语
                AnimatedVisibility(
                    visible = isReadyVisible,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(1000))
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                        Text(
                            randomTips, fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(
                                start = 200.dp,
                                top = if (!driveIsLongPressed) 100.dp else 50.dp
                            )
                        )
                    }
                }

                // 学生姓名显示
                if (!driveIsLongPressed) {
                    SingleStudentDisplay(selectedStudent, isLastNameVisible, isFirstNameVisible)
                } else {
                    ThreeStudentsDisplay(isFirstNameVisible, student1, student2, student3)
                }
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
    Row(
        modifier = Modifier.padding(bottom = 100.dp).fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 姓氏
        AnimatedVisibility(
            visible = isLastNameVisible,
            enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                    fadeIn(tween(700)) + expandVertically(tween(500)) +
                    slideInHorizontally(tween(500))
        ) {
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    student?.first ?: "", fontSize = 300.sp,
                    color = Color.Red,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }
        }
        // 名字
        AnimatedVisibility(
            visible = isFirstNameVisible,
            enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                    fadeIn(tween(700)) + expandVertically(tween(500)) +
                    slideInHorizontally(tween(500))
        ) {
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    student?.second ?: "某某", fontSize = 300.sp,
                    color = Color.Red,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }
        }
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
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            for (name in listOf(s1, s2, s3)) {
                Text(
                    name, fontSize = 150.sp,
                    color = Color.Red,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = if (name == s3) 100.dp else 25.dp)
                )
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
