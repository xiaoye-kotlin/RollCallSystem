package com.rollcall.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import com.rollcall.app.network.NetworkHelper
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.network.NetworkHelper.getCountDownName
import com.rollcall.app.network.NetworkHelper.getCountDownTime
import com.rollcall.app.network.NetworkHelper.getDownloadUrl
import com.rollcall.app.network.NetworkHelper.getIsOpen
import com.rollcall.app.network.NetworkHelper.getIsVoiceIdentifyOpen
import com.rollcall.app.network.NetworkHelper.getNameList
import com.rollcall.app.network.NetworkHelper.getSubjectList
import com.rollcall.app.network.NetworkHelper.getTimeApi
import com.rollcall.app.network.NetworkHelper.getUrl
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper
import com.rollcall.app.util.readFromFile
import com.rollcall.app.util.writeToFile
import com.rollcall.app.util.deleteFileOrDirectory
import kotlin.system.exitProcess

@Composable
fun title() {
    /*
    核心数据：
    1、全局域名（用于所有url访问的前段部分） - AppState.url
    2、文件下载域名（用于下载语音转文字模型包） - AppState.downloadUrl
    3、程序远程开关（用于远控程序） - AppState.isOpen
    4、检测是否存在模型文件包 - isModelExists
    5、获取名单列表（用于点名的名单列表） -  jsonData
    */
    var countdown by remember { mutableStateOf(5) }
    var tips by remember { mutableStateOf("") }
    val isLoading = AppState.isLoading.collectAsState()
    var jsonData by remember { mutableStateOf("无") }
    var subjectData by remember { mutableStateOf("无") }
    var luckyGuyData by remember { mutableStateOf("无") }
    var poolGuyData by remember { mutableStateOf("无") }
    var countdownName by remember { mutableStateOf("无") }
    var countdownTime by remember { mutableStateOf("无") }
    val isInternetAvailable = AppState.isInternetAvailable.collectAsState()
    var isModelExists by remember { mutableStateOf(false) }
    val isVoiceIdentify = AppState.isVoiceIdentify.collectAsState()

    val targetDir = File("D:/")
    val testDir = File("D:/vosk-model-small-cn-0.22")

    val jsonNameListFilePath = "D:/Xiaoye/NameList.json"
    val jsonSubjectListFilePath = "D:/Xiaoye/SubjectList.json"
    val jsonCountDownNameFilePath = "D:/Xiaoye/CountDownName.json"
    val jsonCountDownTimeFilePath = "D:/Xiaoye/CountDownTime.json"
    val jsonLuckyGuyFilePath = "D:/Xiaoye/LuckyGuy.json"
    val jsonPoolGuyFilePath = "D:/Xiaoye/PoolGuy.json"

    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isLoading.value) {
            println("Countdown: $countdown")
            countdown--
            delay(1000)
        }
    }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            println("isDownloading is touched")
            withContext(Dispatchers.IO) {

                if (!tips.contains("9/10") && !tips.contains("10/10") && tips != "整理本地文件...") {
                    tips = "加载必要数据(8/10)"
                }
                while (!isModelExists) {
                    isModelExists = checkAndCopyModel(AppState.downloadUrl, targetDir, testDir)
                    delay(1000)
                }
                println("isModelExists is touched")

                if (!tips.contains("10/10") && tips != "整理本地文件...") {
                    tips = "加载必要数据(9/10)"
                }
                while (jsonData == "无") {
                    jsonData = getNameList()
                    delay(3000)
                }

                if (tips != "整理本地文件...") {
                    tips = "加载必要数据(10/10)"
                }
                while (subjectData == "无") {
                    subjectData = getSubjectList()
                    delay(3000)
                }
                println("Data is touched")

                if (tips != "整理本地文件...") {
                    tips = "整理本地文件..."
                }
                delay(1000)

                AppState.updateStudentListFromJson(jsonData)
                AppState.updateSubjectListFromJson(subjectData)
                countdownName = AppState.countdownName
                countdownTime = AppState.countdownTime
                if (readFromFile("D:/Xiaoye/LuckyGuy.json") != "404") {
                    if (FileHelper.isValidJson(readFromFile("D:/Xiaoye/LuckyGuy.json"))) {
                        AppState.setLuckyGuy(readFromFile("D:/Xiaoye/LuckyGuy.json"))
                    }
                }
                if (countdown <= 0 && countdownName != "无") {
                    writeToFile(jsonCountDownNameFilePath, countdownName)
                }
                if (countdown <= 0 && countdownTime != "无") {
                    writeToFile(jsonCountDownTimeFilePath, countdownTime)
                }
                if (countdown <= 0 && jsonData != "无") {
                    writeToFile(jsonNameListFilePath, jsonData)
                }
                if (countdown <= 0 && subjectData != "无") {
                    writeToFile(jsonSubjectListFilePath, subjectData)
                    AppState.setIsLoading(false)
                }
            }
        }
    }

    val isOpen = AppState.isOpen.collectAsState()

    LaunchedEffect(Unit) {
        while (!isDownloading) {
            if (NetworkHelper.isInternetAvailable()) {
                withContext(Dispatchers.IO) {
                    println("start to get data")

                    if (!tips.contains("1/10") &&
                        !tips.contains("2/10") &&
                        !tips.contains("3/10") &&
                        !tips.contains("4/10") &&
                        !tips.contains("5/10") &&
                        !tips.contains("6/10") &&
                        !tips.contains("7/10") &&
                        !tips.contains("8/10")
                    ) {
                        tips = "加载必要数据..."
                    }

                    if (NetworkHelper.isInternetAvailable()) {
                        deleteFileOrDirectory("D:/Xiaoye/CountDownName.json")
                        deleteFileOrDirectory("D:/Xiaoye/CountDownTime.json")
                        AppState.setIsOpen(getIsOpen().toBoolean())
                        if (!isOpen.value) {
                            exitProcess(0)
                        }

                        if (!tips.contains("1/10") &&
                            !tips.contains("2/10") &&
                            !tips.contains("3/10") &&
                            !tips.contains("4/10") &&
                            !tips.contains("5/10") &&
                            !tips.contains("6/10") &&
                            !tips.contains("7/10")
                        ) {
                            tips = "加载必要数据(1/10)"
                        }
                        AppState.url = getUrl()

                        if (tips == "加载必要数据(1/10)") {
                            tips = "加载必要数据(2/10)"
                        }
                        AppState.setIsVoiceIdentify(getIsVoiceIdentifyOpen().toBooleanStrictOrNull() == true)

                        if (tips == "加载必要数据(2/10)") {
                            tips = "加载必要数据(3/10)"
                        }
                        AppState.downloadUrl = getDownloadUrl()

                        if (tips == "加载必要数据(3/10)") {
                            tips = "加载必要数据(4/10)"
                        }
                        AppState.timeApi = getTimeApi()

                        if (tips == "加载必要数据(4/10)") {
                            tips = "加载必要数据(5/10)"
                        }
                        AppState.countdownName = getCountDownName()

                        if (tips == "加载必要数据(5/10)") {
                            tips = "加载必要数据(6/10)"
                        }
                        AppState.countdownTime = getCountDownTime()

                        if (tips == "加载必要数据(6/10)") {
                            tips = "加载必要数据(7/10)"
                        }
                        println("Student read result: ${readFromFile(jsonNameListFilePath)}")
                        println("Subject read result: ${readFromFile(jsonSubjectListFilePath)}")
                        println("isInternetAvailable: ${isInternetAvailable.value}")
                        delay(1000)
                        if (!isDownloading && tips != "数据不完整,重新尝试...") {
                            tips = "数据不完整,重新尝试..."
                        }
                    }
                }
            } else {
                if (!NetworkHelper.isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404" &&
                    readFromFile(jsonSubjectListFilePath) != "404" &&
                    readFromFile(jsonLuckyGuyFilePath) != "404"
                ) {
                    println("has been readData")
                    jsonData = readFromFile(jsonNameListFilePath)
                    subjectData = readFromFile(jsonSubjectListFilePath)
                    luckyGuyData = readFromFile(jsonLuckyGuyFilePath)
                    poolGuyData = readFromFile(jsonPoolGuyFilePath)
                    if (jsonData != "无") {
                        AppState.updateStudentListFromJson(jsonData)
                        println("Student Data has been written")
                    }
                    if (subjectData != "无") {
                        AppState.updateSubjectListFromJson(subjectData)
                        println("Subject Data has been written")
                    }
                    if (luckyGuyData != "无") {
                        AppState.setLuckyGuy(luckyGuyData)
                        println("luckyGuy Data has been written")
                    }
                    if (poolGuyData != "无") {
                        AppState.setPoolGuy(poolGuyData)
                        println("poolGuy Data has been written")
                    }
                } else if (!NetworkHelper.isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404") {
                    jsonData = readFromFile(jsonNameListFilePath)
                    if (jsonData != "无") {
                        AppState.updateStudentListFromJson(jsonData)
                        println("Student Data has been written")
                    }
                }
            }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (isInternetAvailable.value) {
            if ((AppState.url != "" && AppState.url.contains("http") && AppState.timeApi != "" && AppState.timeApi.contains(
                    "http"
                ) && AppState.downloadUrl != "" && AppState.downloadUrl.contains("http") && isOpen.value)
            ) {
                isDownloading = true
            }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        println("AppState.url: ${AppState.url}, AppState.timeApi: ${AppState.timeApi}, AppState.isOpen: ${AppState.isOpen}, isModelExists: $isModelExists, AppState.downloadUrl: ${AppState.downloadUrl}, isVoiceIdentify: ${isVoiceIdentify.value}")
        if (!NetworkHelper.isInternetAvailable()) {
            AppState.setIsInternetAvailable(false)
            if (readFromFile(jsonNameListFilePath) != "404" || readFromFile(jsonSubjectListFilePath) != "404") {
                tips = "当前无网络连接，即将进入离线模式"
                delay(900)
                AppState.setIsLoading(false)
            } else {
                tips = "联网后将自动运行，5秒后最小化程序。"
                delay(4900)
                AppState.setIsLoading(false)
            }
        }
    }

    // ==================== 现代化加载界面 ====================
    val colors = AppTheme.colors

    // 旋转加载动画
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 进度条计算（基于tips阶段）
    val progress = remember(tips) {
        when {
            tips.contains("1/10") -> 0.1f
            tips.contains("2/10") -> 0.2f
            tips.contains("3/10") -> 0.3f
            tips.contains("4/10") -> 0.4f
            tips.contains("5/10") -> 0.5f
            tips.contains("6/10") -> 0.6f
            tips.contains("7/10") -> 0.7f
            tips.contains("8/10") -> 0.8f
            tips.contains("9/10") -> 0.9f
            tips.contains("10/10") -> 0.95f
            tips.contains("整理") -> 0.98f
            else -> 0.05f
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

    // 微光动画
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.gradient1, colors.gradient2)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // 右上角最小化按钮
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = { AppState.setIsMinimize(true) }) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource("/images/minimize.png"),
                    contentDescription = "最小化",
                    tint = colors.textSecondary.copy(alpha = 0.7f)
                )
            }
        }

        // 主内容区
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 应用图标 - 带旋转光环
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // 外圈光环
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    colors.primary.copy(alpha = 0.6f),
                                    colors.accent.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    colors.primary.copy(alpha = 0.6f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
                val iconImage: Painter = painterResource("images/callTheRoll.png")
                Image(
                    painter = iconImage, contentDescription = "应用图标",
                    modifier = Modifier.size(100.dp)
                )
            }

            // 应用名称
            Text(
                text = "智能点名系统",
                fontSize = 36.sp,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "v${AppState.VERSION} · 高${2026}届${AppState.CLASS}班",
                fontSize = 15.sp,
                color = colors.textHint,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // 进度条
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(colors.primary, colors.accent)
                            )
                        )
                )
                // 微光效果
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .offset(x = shimmerOffset.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.shimmer,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 状态提示
            if (tips.isNotEmpty() && tips.isNotBlank()) {
                val isProgress = tips.contains("加载") || tips.contains("整理")
                val bgColor = if (isProgress) {
                    colors.primary.copy(alpha = 0.12f)
                } else {
                    colors.error.copy(alpha = 0.12f)
                }
                val textColor = if (isProgress) colors.primary else colors.error

                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .background(bgColor, RoundedCornerShape(20.dp))
                        .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tips,
                        fontSize = 20.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }

            // 欢迎图片
            val titleImage: Painter = painterResource("images/welcome.png")
            Image(painter = titleImage, contentDescription = "欢迎")
        }

        // 底部版权信息
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Powered by Compose Desktop",
                fontSize = 13.sp,
                color = colors.textHint.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
