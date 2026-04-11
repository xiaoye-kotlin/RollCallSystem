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
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Toolkit
import javax.imageio.ImageIO
import kotlin.math.max

private const val OPTIONS_WINDOW_WIDTH_DP = 340
private const val OPTIONS_WINDOW_HEIGHT_DP = 860
private const val OPTIONS_WINDOW_MARGIN_RIGHT_PX = 180
private const val DRAG_BALL_SIZE_PX = 100
private const val DRAG_TRIGGER_DISTANCE_PX = 5
private const val QUICK_TOOLS_TARGET_TOP_PX = 120
private const val QUICK_TOOLS_TARGET_BOTTOM_PX = 250
private const val COUNTDOWN_TARGET_ONE_TOP_PX = 332
private const val COUNTDOWN_TARGET_FOUR_BOTTOM_PX = 764
private const val COUNTDOWN_TARGET_HEIGHT_PX = 82
private const val COUNTDOWN_TARGET_GAP_PX = 12

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
        alwaysOnTop = true, resizable = false,
        focusable = false
    ) {
        setWindowIcon()

        // 保持窗口置顶
        LaunchedEffect(Unit) {
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                delay(1000)
            }
        }

        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                        alpha = 0.92f
                    },
                    color = colors.surface
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
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                delay(1000)
            }
        }

        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors
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
                    color = colors.surface
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
}

/**
 * 更多选项窗口
 * 拖动悬浮窗时显示在右侧
 */
@Composable
fun MoreOptionsWindow(isVisible: Boolean, isCountDownEnabled: Boolean, currentDropTarget: Int) {
    val state = rememberWindowState(
        size = DpSize(OPTIONS_WINDOW_WIDTH_DP.dp, OPTIONS_WINDOW_HEIGHT_DP.dp)
    )
    Window(
        onCloseRequest = { },
        title = "更多选项",
        state = state,
        undecorated = true, transparent = true,
        alwaysOnTop = true, resizable = false,
        focusable = false
    ) {
        setWindowIcon()

        LaunchedEffect(Unit) {
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                delay(1000)
            }
        }

        LaunchedEffect(window) {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val marginRight = OPTIONS_WINDOW_MARGIN_RIGHT_PX
            val y = max(40, (screenSize.height - window.height) / 2)
            window.setLocation(screenSize.width - window.width - marginRight, y)
        }

        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors
            if (isVisible) {
                Surface(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                        alpha = 0.95f
                    },
                    color = colors.surface
                ) {
                    Box(
                        Modifier.padding(10.dp).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        moreFunction(isCountDownEnabled, currentDropTarget)
                    }
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
    countDownType: Int,
    isChangeFace: Boolean,
    onDropTargetChanged: (Int) -> Unit,
    onOpenQuickTools: () -> Unit
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

        // 保持置顶
        LaunchedEffect(Unit) {
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                delay(1000)
            }
        }

        val window = remember { this.window }
        var isDraggingBox by remember { mutableStateOf(false) }
        var lastMouseLocation by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var pressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        fun clearDraggingState() {
            isDraggingBox = false
            lastMouseLocation = null
            AppState.setIsDragging(false)
            AppState.setIsLongPressed(false)
            onDropTargetChanged(DROP_TARGET_NONE)
            pressJob?.cancel()
            pressJob = null
        }

        Box(Modifier.width(100.dp).height(100.dp)) {
            val clickThreshold = 200L

            // 设置鼠标事件监听
            DisposableEffect(window) {
                var pressTime: Long = 0
                var isClick = false
                var didDrag = false

                val mouseListener = object : java.awt.event.MouseAdapter() {
                    override fun mousePressed(e: java.awt.event.MouseEvent?) {
                        if (e == null || !javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                            isClick = false
                            clearDraggingState()
                            return
                        }
                        pressTime = System.currentTimeMillis()
                        isClick = true
                        didDrag = false
                        lastMouseLocation = e.locationOnScreen.let { Pair(it.x, it.y) }
                        isDraggingBox = true
                        AppState.setIsDragging(false)
                        onDropTargetChanged(DROP_TARGET_NONE)
                        javax.swing.SwingUtilities.invokeLater { window.toFront() }

                        pressJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(1000)
                            if (isDraggingBox && isClick) {
                                AppState.setIsLongPressed(true)
                            }
                        }
                    }

                    override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                        if (e == null || !javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                            clearDraggingState()
                            return
                        }

                        val clickDuration = System.currentTimeMillis() - pressTime
                        if (clickDuration < clickThreshold && isClick) {
                            AppState.setButtonState("关闭")
                        }

                        if (didDrag) {
                            e.locationOnScreen.let { mouseLocation ->
                                val loc = window.location.apply {
                                    x = mouseLocation.x - window.width / 2
                                    y = mouseLocation.y - window.height / 2
                                }
                                javax.swing.SwingUtilities.invokeLater { window.location = loc }
                                handleWindowDrop(loc, countDownType, onOpenQuickTools)
                            }
                        }
                        clearDraggingState()
                    }
                }

                val mouseMotionListener = object : java.awt.event.MouseMotionAdapter() {
                    override fun mouseDragged(e: java.awt.event.MouseEvent?) {
                        if (e != null && javax.swing.SwingUtilities.isLeftMouseButton(e) && isDraggingBox) {
                            lastMouseLocation?.let { lastLoc ->
                                val dx = e.locationOnScreen.x - lastLoc.first
                                val dy = e.locationOnScreen.y - lastLoc.second
                                if (
                                    kotlin.math.abs(dx) > DRAG_TRIGGER_DISTANCE_PX ||
                                    kotlin.math.abs(dy) > DRAG_TRIGGER_DISTANCE_PX
                                ) {
                                    pressJob?.cancel()
                                    AppState.setIsLongPressed(false)
                                    isClick = false
                                    didDrag = true
                                    AppState.setIsDragging(true)
                                    val newLoc = window.location.apply { x += dx; y += dy }
                                    javax.swing.SwingUtilities.invokeLater {
                                        window.location = newLoc
                                        window.toFront()
                                    }
                                    onDropTargetChanged(
                                        detectDropTarget(newLoc, countDownType)
                                    )
                                    lastMouseLocation = e.locationOnScreen.let { Pair(it.x, it.y) }
                                }
                            }
                        }
                    }
                }

                val windowFocusListener = object : java.awt.event.WindowAdapter() {
                    override fun windowDeactivated(e: java.awt.event.WindowEvent?) {
                        clearDraggingState()
                    }
                }

                window.addMouseListener(mouseListener)
                window.addMouseMotionListener(mouseMotionListener)
                window.addWindowListener(windowFocusListener)

                onDispose {
                    clearDraggingState()
                    window.removeMouseListener(mouseListener)
                    window.removeMouseMotionListener(mouseMotionListener)
                    window.removeWindowListener(windowFocusListener)
                }
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
private fun handleWindowDrop(
    location: java.awt.Point,
    countDownType: Int,
    onOpenQuickTools: () -> Unit
) {
    val dropTarget = detectDropTarget(location, countDownType)
    when (dropTarget) {
        DROP_TARGET_QUICK_TOOLS -> {
            onOpenQuickTools()
            return
        }
        1, 2, 3, 4 -> {
            AppState.setCountDownType(dropTarget)
            return
        }
    }
}

private fun detectDropTarget(
    location: java.awt.Point,
    countDownType: Int
): Int {
    val panelBounds = getOptionsPanelBounds()
    val pointerX = location.x + DRAG_BALL_SIZE_PX / 2
    val pointerY = location.y + DRAG_BALL_SIZE_PX / 2

    val inPanelX = pointerX in (panelBounds.x + 18)..(panelBounds.x + panelBounds.width - 18)
    if (!inPanelX) return DROP_TARGET_NONE

    val localY = pointerY - panelBounds.y
    if (localY in QUICK_TOOLS_TARGET_TOP_PX..QUICK_TOOLS_TARGET_BOTTOM_PX) {
        return DROP_TARGET_QUICK_TOOLS
    }

    if (countDownType == 0) {
        if (localY !in COUNTDOWN_TARGET_ONE_TOP_PX..COUNTDOWN_TARGET_FOUR_BOTTOM_PX) {
            return DROP_TARGET_NONE
        }

        val step = COUNTDOWN_TARGET_HEIGHT_PX + COUNTDOWN_TARGET_GAP_PX
        val relativeY = localY - COUNTDOWN_TARGET_ONE_TOP_PX
        val slotIndex = relativeY / step
        val slotOffset = relativeY % step
        if (slotIndex in 0..3 && slotOffset <= COUNTDOWN_TARGET_HEIGHT_PX) {
            return slotIndex + 1
        }
    }

    return DROP_TARGET_NONE
}

private fun getOptionsPanelBounds(): java.awt.Rectangle {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val width = OPTIONS_WINDOW_WIDTH_DP
    val height = OPTIONS_WINDOW_HEIGHT_DP
    val x = screenSize.width - width - OPTIONS_WINDOW_MARGIN_RIGHT_PX
    val y = max(40, (screenSize.height - height) / 2)
    return java.awt.Rectangle(x, y, width, height)
}
