package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.rollcall.app.setWindowIcon
import com.rollcall.app.state.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO

/**
 * 倒数日窗口
 * 在屏幕顶部显示距离重要日期的倒数天数
 */
@Composable
fun CountdownDayWindow(isVisible: Boolean) {
    Window(
        onCloseRequest = { },
        title = "倒数日",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.TopCenter),
            size = DpSize(400.dp, 50.dp)
        ),
        undecorated = true, transparent = true,
        alwaysOnTop = true, resizable = false
    ) {
        setWindowIcon()

        // 保持窗口置顶
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    window.isMinimized = false
                    window.isAlwaysOnTop = true
                    delay(1000)
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    shape = RoundedCornerShape(16.dp)
                    clip = true
                    alpha = 0.7f
                },
                color = Color(AppState.accentColorFloating)
            ) {
                Box(
                    Modifier.padding(10.dp).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    countdownDay()
                }
            }
        }
    }
}

/**
 * 倒计时窗口
 * 在屏幕中央显示计时器
 */
@Composable
fun CountdownTimerWindow(isVisible: Boolean) {
    Window(
        onCloseRequest = { },
        title = "倒计时",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(500.dp, 300.dp)
        ),
        undecorated = true, transparent = true,
        alwaysOnTop = true, resizable = false
    ) {
        setWindowIcon()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    window.isMinimized = false
                    window.isAlwaysOnTop = true
                    delay(1000)
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    shape = RoundedCornerShape(16.dp)
                    clip = true
                },
                color = Color(AppState.accentColorFloating)
            ) {
                Box(
                    Modifier.padding(10.dp).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    countdown()
                }
            }
        }
    }
}

/**
 * 更多选项窗口
 * 拖动悬浮窗时显示在右侧
 */
@Composable
fun MoreOptionsWindow(isVisible: Boolean) {
    Window(
        onCloseRequest = { },
        title = "更多选项",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.CenterEnd),
            size = DpSize(300.dp, 600.dp)
        ),
        undecorated = true, transparent = true,
        alwaysOnTop = true, resizable = false
    ) {
        setWindowIcon()

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    window.isMinimized = false
                    window.isAlwaysOnTop = true
                    delay(1000)
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    shape = RoundedCornerShape(16.dp)
                    clip = true
                    alpha = 0.7f
                },
                color = Color(AppState.accentColorFloating)
            ) {
                Box(
                    Modifier.padding(10.dp).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    moreFunction()
                }
            }
        }
    }
}

/**
 * 悬浮窗主窗口
 * 可拖动的点名按钮，支持点击和长按操作
 */
@Composable
fun ApplicationScope.FloatingWindow(
    isCountDownOpen: Boolean,
    countDownType: Int,
    isChangeFace: Boolean
) {
    Window(
        onCloseRequest = { },
        title = "点名系统",
        state = if (isChangeFace) {
            rememberWindowState(
                position = WindowPosition(Alignment.TopStart),
                size = DpSize(300.dp, 230.dp)
            )
        } else {
            rememberWindowState(
                position = WindowPosition.Aligned(Alignment.BottomEnd),
                size = DpSize(100.dp, 100.dp)
            )
        },
        undecorated = true, transparent = true,
        alwaysOnTop = true, resizable = false
    ) {
        setWindowIcon()
        val isDragging = AppState.isDragging.collectAsState()

        // 保持置顶
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                while (true) {
                    window.isMinimized = false
                    window.isAlwaysOnTop = true
                    delay(1000)
                }
            }
        }

        val window = remember { this.window }
        var isDraggingBox by remember { mutableStateOf(false) }
        var lastMouseLocation by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var pressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        Box(Modifier.width(100.dp).height(100.dp)) {
            val dragThreshold = 5
            val clickThreshold = 200L

            // 设置鼠标事件监听
            LaunchedEffect(Unit) {
                var pressTime: Long = 0
                var isClick = false

                window.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(e: java.awt.event.MouseEvent?) {
                        pressTime = System.currentTimeMillis()
                        isClick = true
                        lastMouseLocation = e?.locationOnScreen?.let { Pair(it.x, it.y) }
                        isDraggingBox = true
                        AppState.setIsDragging(true)

                        pressJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(1000)
                            if (isDraggingBox && isClick) {
                                AppState.setIsLongPressed(true)
                            }
                        }
                    }

                    override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                        val clickDuration = System.currentTimeMillis() - pressTime
                        if (clickDuration < clickThreshold && isClick) {
                            AppState.setButtonState("关闭")
                            AppState.setIsLongPressed(false)
                            pressJob?.cancel()
                        }

                        isDraggingBox = false
                        lastMouseLocation = null
                        AppState.setIsDragging(false)
                        pressJob?.cancel()

                        e?.locationOnScreen?.let { mouseLocation ->
                            val loc = window.location.apply {
                                x = mouseLocation.x - window.width / 2
                                y = mouseLocation.y - window.height / 2
                            }
                            javax.swing.SwingUtilities.invokeLater { window.location = loc }
                            checkWindowPosition(loc, isCountDownOpen, countDownType)
                        }
                    }
                })

                window.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
                    override fun mouseDragged(e: java.awt.event.MouseEvent?) {
                        if (isDraggingBox) {
                            lastMouseLocation?.let { lastLoc ->
                                val dx = e?.locationOnScreen?.x?.minus(lastLoc.first) ?: 0
                                val dy = e?.locationOnScreen?.y?.minus(lastLoc.second) ?: 0
                                if (kotlin.math.abs(dx) > dragThreshold || kotlin.math.abs(dy) > dragThreshold) {
                                    pressJob?.cancel()
                                    AppState.setIsLongPressed(false)
                                    val newLoc = window.location.apply { x += dx; y += dy }
                                    javax.swing.SwingUtilities.invokeLater { window.location = newLoc }
                                    lastMouseLocation = e?.locationOnScreen?.let { Pair(it.x, it.y) }
                                }
                            }
                        }
                    }
                })
            }

            // 动态壁纸和悬浮窗UI
            videoWallpaper()
            dragWindow()
        }
    }
}

/**
 * 根据悬浮窗位置判断倒计时类型
 */
private fun checkWindowPosition(location: java.awt.Point, isCountDownOpen: Boolean, countDownType: Int) {
    if (isCountDownOpen && countDownType == 0) {
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        val sw = screenSize.width.toDouble()
        val sh = screenSize.height.toDouble()

        val type = when {
            location.x.toDouble() in sw * 0.7518..sw * 0.9569 &&
                    location.y.toDouble() in sh * 0.2188..sh * 0.2954 -> 1
            location.x.toDouble() in sw * 0.7518..sw * 0.9569 &&
                    location.y.toDouble() in sh * 0.3063..sh * 0.4595 -> 2
            location.x.toDouble() in sw * 0.7518..sw * 0.9569 &&
                    location.y.toDouble() in sh * 0.4923..sh * 0.6017 -> 3
            location.x.toDouble() in sw * 0.7518..sw * 0.9569 &&
                    location.y.toDouble() in sh * 0.6017..sh * 0.7330 -> 4
            else -> 0
        }
        AppState.setCountDownType(type)
    }
}
