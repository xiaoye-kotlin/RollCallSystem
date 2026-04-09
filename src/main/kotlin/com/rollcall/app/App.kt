package com.rollcall.app

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.audio.AudioManager
import com.rollcall.app.network.NetworkHelper
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.screen.*
import com.rollcall.app.util.FileHelper
import com.rollcall.app.data.model.parseTimeJsonResponse
import kotlinx.coroutines.*
import java.awt.*
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * 应用程序主入口
 * 点名系统 - 适用于学校智能大屏幕的课堂管理工具
 */
fun main() = application {
    // ==================== 全局状态收集 ====================
    val isOpen = AppState.isOpen.collectAsState()
    AppState.isInternetAvailable.collectAsState()
    val buttonState = AppState.buttonState.collectAsState()
    val isLoading = AppState.isLoading.collectAsState()
    val isMinimize = AppState.isMinimize.collectAsState()
    val isVoiceIdentify = AppState.isVoiceIdentify.collectAsState()
    val isLongPressed = AppState.isLongPressed.collectAsState()
    val isEasterEgg = AppState.isEasterEgg.collectAsState()
    var isRun by remember { mutableStateOf(false) }
    var mainWindowVisible by remember { mutableStateOf(true) }
    var floatingWindowVisible by remember { mutableStateOf(false) }
    var isReadyVisible by remember { mutableStateOf(false) }
    var isFirstNameVisible by remember { mutableStateOf(false) }
    var isLastNameVisible by remember { mutableStateOf(false) }
    var randomCounter by remember { mutableStateOf(0) }
    var isFirstInitialized by remember { mutableStateOf(false) }

    // 随机选择学生
    val selectedStudent = remember(randomCounter) {
        if (randomCounter % 2 == 0 && isFirstInitialized) {
            AppState.getRandomStudent()
        } else {
            isFirstInitialized = true
            null
        }
    }

    var isOpenHtml by remember { mutableStateOf(false) }
    val isTime = AppState.isTime.collectAsState()
    val isCountDownDayOpen = AppState.isCountDownDayOpen.collectAsState()
    val isCountDownOpen = AppState.isCountDownOpen.collectAsState()
    val week = AppState.week.collectAsState()
    val time = AppState.time.collectAsState()
    val date = AppState.date.collectAsState()
    val luckyGuy = AppState.luckyGuy.collectAsState()
    val poolGuy = AppState.poolGuy.collectAsState()
    var operating by remember { mutableStateOf(0) }

    // ==================== 心跳与版本上报 ====================
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                operating++
                // 检查更新标记
                if (FileHelper.readFromFile("D:/Xiaoye/Updating") == "The System is updating...") {
                    exitProcess(0)
                }
                FileHelper.writeToFile("D:/Xiaoye/Version", AppState.VERSION.toString())
                FileHelper.writeToFile("D:/Xiaoye/Operating", operating.toString())
                val runningTime = FileHelper.readFromFile("D:/Xiaoye/RunningTime").toIntOrNull() ?: 0
                FileHelper.writeToFile("D:/Xiaoye/RunningTime", (runningTime + 1).toString())
                delay(1000)
            }
        }
    }

    // ==================== 远程配置轮询 ====================
    var lastTrueTimestamp = 0L

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                AppState.url = NetworkHelper.getUrl()
                println("网络状态: ${NetworkHelper.isInternetAvailable()}")
                if (AppState.url != "No Wifi" && AppState.url.contains("http")) {
                    AppState.setIsOpen(NetworkHelper.getIsOpen().toBoolean())
                    AppState.setIsVoiceIdentify(NetworkHelper.getIsVoiceIdentifyOpen().toBoolean())
                    AppState.setIsTime(NetworkHelper.getIsTimeOpen().toBoolean())
                    AppState.setIsCountDownDayOpen(NetworkHelper.getCountDownDaySwitch().toBoolean())
                    AppState.setIsCountDownOpen(NetworkHelper.getCountDownSwitch().toBoolean())
                    AppState.setIsWallpaper(NetworkHelper.getWallpaperSwitch().toBoolean())
                    AppState.setIsDeleteWallpaper(NetworkHelper.getDeleteWallpaperSwitch().toBoolean())

                    if (!isOpen.value) exitProcess(0)

                    // 彩蛋检测（每100秒最多触发一次）
                    val now = System.currentTimeMillis()
                    if (now - lastTrueTimestamp > 100000) {
                        if (NetworkHelper.getEasterEggSwitch().toBooleanStrictOrNull() == true) {
                            AppState.setIsEasterEgg(true)
                            lastTrueTimestamp = now
                        } else {
                            AppState.setIsEasterEgg(false)
                        }
                    }

                    // 更新幸运学生和倒霉学生列表
                    updateSpecialStudentList(NetworkHelper.getLuckyGuy(), "LuckyGuy") { AppState.setLuckyGuy(it) }
                    updateSpecialStudentList(NetworkHelper.getPoolGuy(), "PoolGuy") { AppState.setPoolGuy(it) }

                    // 更新时间信息
                    if (isTime.value && AppState.timeApi != "" && AppState.timeApi.contains("http")) {
                        val timeData = NetworkHelper.getTimeData()
                        AppState.setWeek(parseTimeJsonResponse(timeData).weekday)
                        val dateStr = parseTimeJsonResponse(timeData).date
                        if (dateStr.length >= 16) {
                            AppState.setTime(dateStr.substring(11, 16))
                        }
                    }

                    // 更新日期（倒数日使用）
                    if (isCountDownDayOpen.value && AppState.timeApi != "" && AppState.timeApi.contains("http")) {
                        val rawDate = parseTimeJsonResponse(NetworkHelper.getTimeData()).date
                        rawDate.split(" ").getOrNull(0)?.let { AppState.setDate(it) }
                    }
                }
                delay(3000)
            }
        }
    }

    // ==================== 在线状态上报 ====================
    LaunchedEffect(Unit) {
        while (true) {
            if (AppState.url != "" && AppState.url.contains("http")) {
                NetworkHelper.getOnline()
            }
            delay(10000)
        }
    }

    // ==================== 启动动画延迟 ====================
    LaunchedEffect(Unit) {
        delay(500)
        isRun = true
    }

    // ==================== 加载完成后切换窗口 ====================
    LaunchedEffect(isLoading.value) {
        if (!isLoading.value) {
            mainWindowVisible = false
            floatingWindowVisible = true
        }
    }

    // ==================== 长按动画逻辑 ====================
    var isLongPressedAnimation by remember { mutableStateOf(false) }
    val driveIsLongPressed = remember { mutableStateOf(false) }
    var isFirstDriveIsLongPressed by remember { mutableStateOf(true) }
    var isGetStudent by remember { mutableStateOf(false) }

    LaunchedEffect(isLongPressed.value) {
        if (isLongPressed.value) {
            isLongPressedAnimation = true
            delay(1000)
            driveIsLongPressed.value = true
            isLongPressedAnimation = false
        }
    }

    LaunchedEffect(driveIsLongPressed.value) {
        if (driveIsLongPressed.value && !isFirstDriveIsLongPressed) {
            isReadyVisible = true
            delay(500)
            isOpenHtml = false
            delay(500)
            isFirstNameVisible = true
            delay(6000)
            driveIsLongPressed.value = false
            floatingWindowVisible = true
            isReadyVisible = false
            isLastNameVisible = false
            isFirstNameVisible = false
            isGetStudent = false
        } else if (isFirstDriveIsLongPressed) {
            isFirstDriveIsLongPressed = false
        }
    }

    // ==================== 点击点名逻辑 ====================
    var isIgnore by remember { mutableStateOf(false) }

    LaunchedEffect(buttonState.value) {
        if (floatingWindowVisible && buttonState.value == "关闭") {
            if (isIgnore) isIgnore = false
            randomCounter += 1
            isReadyVisible = true
            delay(500)
            isLastNameVisible = true
            delay(1000)
            isOpenHtml = false
            delay(500)
            isFirstNameVisible = true
            delay(2000)
            floatingWindowVisible = true
            AppState.setButtonState("点名")
        } else if (buttonState.value == "点名" && !isIgnore) {
            randomCounter += 1
            isReadyVisible = false
            isLastNameVisible = false
            isFirstNameVisible = false
        } else if (buttonState.value == "^_^" || buttonState.value == "＠_＠") {
            isIgnore = true
        }
    }

    // ==================== 语音识别处理 ====================
    var voiceResult by remember { mutableStateOf("") }

    LaunchedEffect(isVoiceIdentify.value) {
        if (floatingWindowVisible && isVoiceIdentify.value) {
            voice { result ->
                voiceResult = result
                if (result != "没有可用的识别结果" && result != "") {
                    if (isRollCallCommand(result)) {
                        floatingWindowVisible = true
                        AppState.setButtonState("关闭")
                    }
                }
            }
        }
    }

    // ==================== 随机颜色方案 ====================
    if (!AppState.isRandomColor) {
        initRandomColorScheme()
    }

    // ==================== 长按倒计时窗口 ====================
    if (isLongPressed.value) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "点名程序",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(400.dp, 400.dp)
            ),
            undecorated = true, transparent = true,
            alwaysOnTop = true, resizable = false
        ) {
            setWindowIcon()
            AnimatedVisibility(
                visible = isLongPressedAnimation,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource("images/pane.png"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().size(400.dp)
                    )
                    Text("3", fontSize = 300.sp,
                        style = TextStyle(fontWeight = FontWeight.Bold),
                        color = Color.Red, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // ==================== 彩蛋窗口 ====================
    if (isEasterEgg.value) {
        easterEgg()
    }

    // ==================== 启动加载页面 ====================
    if (mainWindowVisible && !isEasterEgg.value) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "点名程序",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(700.dp, 700.dp)
            ),
            undecorated = true, transparent = true, alwaysOnTop = true
        ) {
            rememberWindowState()
            LaunchedEffect(isLoading.value) {
                if (!isLoading.value) window.extendedState = Frame.ICONIFIED
            }
            LaunchedEffect(isMinimize.value) {
                if (isMinimize.value) {
                    window.isMinimized = true
                    AppState.setIsMinimize(false)
                }
            }
            setWindowIcon()

            AnimatedVisibility(
                visible = isRun,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(500)),
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                    },
                    color = Color(AppState.accentColorMain)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        title()
                    }
                }
            }
        }
    }

    // ==================== 悬浮窗系统 ====================
    val isChangeFace = AppState.isChangeFace.collectAsState()
    var isRunCountdownDay by remember { mutableStateOf(false) }

    if (floatingWindowVisible && !isEasterEgg.value) {
        // 倒数日窗口
        if (date.value != "无" && isCountDownDayOpen.value &&
            AppState.countdownName != "无" && AppState.countdownTime != "无"
        ) {
            LaunchedEffect(Unit) { delay(500); isRunCountdownDay = true }
            CountdownDayWindow(isRunCountdownDay)
        } else {
            isRunCountdownDay = false
        }

        // 倒计时窗口
        val countDownType = AppState.countDownType.collectAsState()
        var isRunCountdown by remember { mutableStateOf(false) }

        if (isCountDownOpen.value && countDownType.value != 0) {
            LaunchedEffect(Unit) { delay(500); isRunCountdown = true }
            CountdownTimerWindow(isRunCountdown)
        } else {
            isRunCountdown = false
        }

        // 更多选项窗口（拖拽触发）
        val isDragging = AppState.isDragging.collectAsState()
        if (isCountDownOpen.value && countDownType.value == 0 && isDragging.value) {
            LaunchedEffect(Unit) { delay(500); isRunCountdown = true }
            MoreOptionsWindow(isRunCountdown)
        } else if (!(isCountDownOpen.value && countDownType.value != 0)) {
            isRunCountdown = false
        }

        // 悬浮窗主窗口
        FloatingWindow(
            isCountDownOpen = isCountDownOpen.value,
            countDownType = countDownType.value,
            isChangeFace = isChangeFace.value
        )
    }

    // ==================== 点名结果页面 ====================
    var student1 by remember { mutableStateOf("某某某") }
    var student2 by remember { mutableStateOf("某某某") }
    var student3 by remember { mutableStateOf("某某某") }

    if (driveIsLongPressed.value && !isGetStudent) {
        isGetStudent = true
        AppState.getRandomStudent()?.let { student1 = it.first + it.second }
        AppState.getRandomStudent()?.let { student2 = it.first + it.second }
        AppState.getRandomStudent()?.let { student3 = it.first + it.second }
    }

    if (buttonState.value == "关闭" || driveIsLongPressed.value) {
        AppState.setCountDownType(0)
        RollCallResultWindow(
            selectedStudent = selectedStudent,
            driveIsLongPressed = driveIsLongPressed.value,
            isReadyVisible = isReadyVisible,
            isFirstNameVisible = isFirstNameVisible,
            isLastNameVisible = isLastNameVisible,
            isOpenHtml = isOpenHtml,
            onOpenHtmlChanged = { isOpenHtml = it },
            student1 = student1, student2 = student2, student3 = student3,
            luckyGuyJson = luckyGuy.value,
            poolGuyJson = poolGuy.value
        )
    }
}

// ==================== 辅助函数 ====================

/**
 * 更新特殊学生列表（幸运/倒霉）
 */
private fun updateSpecialStudentList(rawData: String, fileTag: String, setter: (String) -> Unit) {
    if (rawData.contains("|")) {
        try {
            val items = rawData.split("|")
            val json = Gson().toJson(items)
            setter(json)
            FileHelper.writeToFile("D:/Xiaoye/$fileTag.json", json)
        } catch (e: Exception) {
            println("转换失败: ${e.message}")
        }
    }
}

/**
 * 判断语音识别结果是否为点名指令
 */
private fun isRollCallCommand(text: String): Boolean {
    val patterns = listOf(
        "点.*名", "抽.*人", "选.*人", "选.*同学", "抽.*同学",
        "叫.*人", "叫.*同学", "挑.*人", "挑.*同学", "随便.*点",
        "随便.*抽", "随机.*选", "随机.*抽", "请.*发言", "叫.*回答",
        "点.*同学"
    )
    return patterns.any { Regex(it).containsMatchIn(text) }
}

/**
 * 初始化随机颜色方案
 * 从50种预设配色方案中随机选择一种
 */
private fun initRandomColorScheme() {
    val colorSchemes = listOf(
        Triple(0xFF37474F.toInt(), 0xFFCFD8DC.toInt(), 0xFFFFF59D.toInt()),
        Triple(0xFF263238.toInt(), 0xFFB0BEC5.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF004D40.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF1B5E20.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt()),
        Triple(0xFF3E2723.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFF01579B.toInt(), 0xFFBBDEFB.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFF4E342E.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFCDD2.toInt()),
        Triple(0xFF1565C0.toInt(), 0xFFB3E5FC.toInt(), 0xFFFF8A65.toInt()),
        Triple(0xFF283593.toInt(), 0xFFC5CAE9.toInt(), 0xFFFFCCBC.toInt()),
        Triple(0xFF006064.toInt(), 0xFFB2EBF2.toInt(), 0xFFFFE082.toInt()),
        Triple(0xFF5D4037.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFECB3.toInt()),
        Triple(0xFF3F51B5.toInt(), 0xFFE8EAF6.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFF303F9F.toInt(), 0xFFE3F2FD.toInt(), 0xFFFFF59D.toInt()),
        Triple(0xFF1976D2.toInt(), 0xFFBBDEFB.toInt(), 0xFFFFCDD2.toInt()),
        Triple(0xFF0288D1.toInt(), 0xFFB3E5FC.toInt(), 0xFFFF8A65.toInt()),
        Triple(0xFF512DA8.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF673AB7.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFECB3.toInt()),
        Triple(0xFF7B1FA2.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF880E4F.toInt(), 0xFFFCE4EC.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFFAD1457.toInt(), 0xFFF48FB1.toInt(), 0xFFFFF59D.toInt()),
        Triple(0xFF0D47A1.toInt(), 0xFF82B1FF.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF01579B.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFF59D.toInt()),
        Triple(0xFF00695C.toInt(), 0xFF4DB6AC.toInt(), 0xFFFFEB3B.toInt()),
        Triple(0xFF1B5E20.toInt(), 0xFF66BB6A.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF33691E.toInt(), 0xFF9CCC65.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFF827717.toInt(), 0xFFDCE775.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFFF57F17.toInt(), 0xFFFFF59D.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFFBF360C.toInt(), 0xFFFFCCBC.toInt(), 0xFFFF8A65.toInt()),
        Triple(0xFF4A148C.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCCBC.toInt()),
        Triple(0xFF006064.toInt(), 0xFFB2EBF2.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFFD84315.toInt(), 0xFFFFCCBC.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFFBF360C.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFF1A237E.toInt(), 0xFFC5CAE9.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF311B92.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFECB3.toInt()),
        Triple(0xFF004D40.toInt(), 0xFF80CBC4.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF880E4F.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFEB3B.toInt()),
        Triple(0xFFAD1457.toInt(), 0xFFF48FB1.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFF4A148C.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFAB91.toInt()),
        Triple(0xFFD81B60.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFF59D.toInt()),
        Triple(0xFF8E24AA.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF4E342E.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFF01579B.toInt(), 0xFFB3E5FC.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF2E7D32.toInt(), 0xFFA5D6A7.toInt(), 0xFFFFEB3B.toInt()),
        Triple(0xFF558B2F.toInt(), 0xFFDCEDC8.toInt(), 0xFFFFF9C4.toInt()),
        Triple(0xFF6A1B9A.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCC80.toInt()),
        Triple(0xFF880E4F.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFA726.toInt()),
        Triple(0xFFC2185B.toInt(), 0xFFF48FB1.toInt(), 0xFFFF8A65.toInt()),
        Triple(0xFF009688.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFF176.toInt()),
        Triple(0xFF00796B.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFCCBC.toInt()),
        Triple(0xFFD32F2F.toInt(), 0xFFFFCDD2.toInt(), 0xFFFFA726.toInt())
    )
    val selected = colorSchemes[Random.nextInt(colorSchemes.size)]
    AppState.accentColorMain = selected.first
    AppState.accentColorFloating = selected.second
    AppState.accentColorNamed = selected.third
    AppState.isRandomColor = true
}

/**
 * 设置窗口图标
 */
@Composable
fun FrameWindowScope.setWindowIcon() {
    val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
    window.iconImage = icon
}

/**
 * 绘制五角星（用于幸运学生标记）
 */
fun DrawScope.drawStar(size: Float, center: Offset, rotationAngle: Float) {
    val path = Path()
    val numPoints = 5
    val radius = size / 2
    val angleStep = 2 * PI / numPoints

    for (i in 0 until numPoints) {
        val outerAngle = i * angleStep
        val outerX = (center.x + radius * cos(outerAngle)).toFloat()
        val outerY = (center.y + radius * sin(outerAngle)).toFloat()
        if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)

        val innerAngle = outerAngle + angleStep / 2
        val innerX = (center.x + radius / 2 * cos(innerAngle)).toFloat()
        val innerY = (center.y + radius / 2 * sin(innerAngle)).toFloat()
        path.lineTo(innerX, innerY)
    }
    path.close()

    rotate(rotationAngle, pivot = center) {
        drawPath(path = path, color = Color.Yellow)
    }
}
