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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.rollcall.app.network.NetworkHelper
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.network.NetworkHelper.getNameList
import com.rollcall.app.network.NetworkHelper.getSubjectList
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper
import com.rollcall.app.util.deleteFileOrDirectory
import com.rollcall.app.util.readFromFile
import com.rollcall.app.util.writeToFile
import kotlin.system.exitProcess

private val startupBackgroundScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)

private data class StartupLocalCache(
    val studentJson: String,
    val subjectJson: String,
    val countdownName: String,
    val countdownTime: String,
    val luckyGuy: String,
    val poolGuy: String
)

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
    var tips by remember { mutableStateOf("") }

    val targetDir = File("D:/")
    val testDir = File("D:/vosk-model-small-cn-0.22")

    val jsonNameListFilePath = "D:/Xiaoye/NameList.json"
    val jsonSubjectListFilePath = "D:/Xiaoye/SubjectList.json"
    val jsonCountDownNameFilePath = "D:/Xiaoye/CountDownName.json"
    val jsonCountDownTimeFilePath = "D:/Xiaoye/CountDownTime.json"
    val jsonLuckyGuyFilePath = "D:/Xiaoye/LuckyGuy.json"
    val jsonPoolGuyFilePath = "D:/Xiaoye/PoolGuy.json"

    fun isValidStudentJson(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("[") && FileHelper.isValidJson(trimmed)
    }

    fun isValidSubjectJson(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("{") && FileHelper.isValidJson(trimmed)
    }

    suspend fun loadStudentDataFast(): String = coroutineScope {
        val remoteDeferred = async { getNameList() }
        val localDeferred = async { readFromFile(jsonNameListFilePath) }
        val remote = remoteDeferred.await()
        if (isValidStudentJson(remote)) {
            return@coroutineScope remote
        }
        val local = localDeferred.await()
        if (isValidStudentJson(local)) {
            println("Using cached student data")
            return@coroutineScope local
        }
        "无"
    }

    suspend fun loadSubjectDataFast(): String = coroutineScope {
        val remoteDeferred = async { getSubjectList() }
        val localDeferred = async { readFromFile(jsonSubjectListFilePath) }
        val remote = remoteDeferred.await()
        if (isValidSubjectJson(remote)) {
            return@coroutineScope remote
        }
        val local = localDeferred.await()
        if (isValidSubjectJson(local)) {
            println("Using cached subject data")
            return@coroutineScope local
        }
        "无"
    }

    suspend fun loadLocalCacheFast(): StartupLocalCache = coroutineScope {
        val studentDeferred = async { readFromFile(jsonNameListFilePath) }
        val subjectDeferred = async { readFromFile(jsonSubjectListFilePath) }
        val countdownNameDeferred = async { readFromFile(jsonCountDownNameFilePath) }
        val countdownTimeDeferred = async { readFromFile(jsonCountDownTimeFilePath) }
        val luckyGuyDeferred = async { readFromFile(jsonLuckyGuyFilePath) }
        val poolGuyDeferred = async { readFromFile(jsonPoolGuyFilePath) }

        StartupLocalCache(
            studentJson = studentDeferred.await(),
            subjectJson = subjectDeferred.await(),
            countdownName = countdownNameDeferred.await(),
            countdownTime = countdownTimeDeferred.await(),
            luckyGuy = luckyGuyDeferred.await(),
            poolGuy = poolGuyDeferred.await()
        )
    }

    fun applyCoreData(studentJson: String, subjectJson: String) {
        AppState.updateStudentListFromJson(studentJson)
        AppState.updateSubjectListFromJson(subjectJson)
    }

    fun applyOptionalLocalData(cache: StartupLocalCache) {
        if (cache.luckyGuy != "404" && FileHelper.isValidJson(cache.luckyGuy)) {
            AppState.setLuckyGuy(cache.luckyGuy)
        }
        if (cache.poolGuy != "404" && FileHelper.isValidJson(cache.poolGuy)) {
            AppState.setPoolGuy(cache.poolGuy)
        }
    }

    suspend fun persistCoreData(studentJson: String, subjectJson: String, countdownName: String, countdownTime: String) =
        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
            if (countdownName != "无") {
                jobs += async { writeToFile(jsonCountDownNameFilePath, countdownName) }
            }
            if (countdownTime != "无") {
                jobs += async { writeToFile(jsonCountDownTimeFilePath, countdownTime) }
            }
            if (isValidStudentJson(studentJson)) {
                jobs += async { writeToFile(jsonNameListFilePath, studentJson) }
            }
            if (isValidSubjectJson(subjectJson)) {
                jobs += async { writeToFile(jsonSubjectListFilePath, subjectJson) }
            }
            jobs.awaitAll()
        }

    suspend fun <T> retryFast(
        attempts: Int = 3,
        retryDelayMs: Long = 180,
        block: suspend () -> T,
        isSuccess: (T) -> Boolean
    ): T {
        var lastValue = block()
        repeat(attempts - 1) {
            if (isSuccess(lastValue)) {
                return lastValue
            }
            delay(retryDelayMs)
            lastValue = block()
        }
        return lastValue
    }

    LaunchedEffect(Unit) {
        tips = "读取本地缓存..."
        val localCache = withContext(Dispatchers.IO) { loadLocalCacheFast() }
        val hasLocalCoreData =
            isValidStudentJson(localCache.studentJson) && isValidSubjectJson(localCache.subjectJson)

        if (!NetworkHelper.isInternetAvailable()) {
            AppState.setIsInternetAvailable(false)
            if (hasLocalCoreData) {
                tips = "离线模式启动中..."
                applyCoreData(localCache.studentJson, localCache.subjectJson)
                applyOptionalLocalData(localCache)
                delay(120)
            } else {
                tips = "联网后将自动运行，稍后最小化程序。"
                delay(800)
            }
            AppState.setIsLoading(false)
            return@LaunchedEffect
        }

        tips = "并行拉取远程配置..."
        deleteFileOrDirectory(jsonCountDownNameFilePath)
        deleteFileOrDirectory(jsonCountDownTimeFilePath)

        val remoteConfig = withContext(Dispatchers.IO) {
            NetworkHelper.getStartupRemoteConfig()
        }

        AppState.setIsOpen(remoteConfig.isOpen)
        if (!remoteConfig.isOpen) {
            exitProcess(0)
        }
        AppState.url = remoteConfig.url
        AppState.downloadUrl = remoteConfig.downloadUrl
        AppState.timeApi = remoteConfig.timeApi
        AppState.setIsVoiceIdentify(remoteConfig.isVoiceIdentifyOpen)
        AppState.countdownName = remoteConfig.countdownName
        AppState.countdownTime = remoteConfig.countdownTime

        tips = "并行加载课程和名单..."
        val studentDeferred = async(Dispatchers.IO) {
            retryFast(
                block = { loadStudentDataFast() },
                isSuccess = ::isValidStudentJson
            )
        }
        val subjectDeferred = async(Dispatchers.IO) {
            retryFast(
                block = { loadSubjectDataFast() },
                isSuccess = ::isValidSubjectJson
            )
        }

        if (AppState.isVoiceIdentify.value) {
            startupBackgroundScope.launch {
                if (!checkAndCopyModel(AppState.downloadUrl, targetDir, testDir)) {
                    delay(300)
                    checkAndCopyModel(AppState.downloadUrl, targetDir, testDir)
                }
            }
        }

        val loadedStudentJson = studentDeferred.await()
        val loadedSubjectJson = subjectDeferred.await()
        val finalStudentJson = if (isValidStudentJson(loadedStudentJson)) loadedStudentJson else localCache.studentJson
        val finalSubjectJson = if (isValidSubjectJson(loadedSubjectJson)) loadedSubjectJson else localCache.subjectJson

        if (isValidStudentJson(finalStudentJson) && isValidSubjectJson(finalSubjectJson)) {
            tips = "整理本地文件..."
            applyCoreData(finalStudentJson, finalSubjectJson)
            applyOptionalLocalData(localCache)
            withContext(Dispatchers.IO) {
                persistCoreData(
                    studentJson = finalStudentJson,
                    subjectJson = finalSubjectJson,
                    countdownName = AppState.countdownName,
                    countdownTime = AppState.countdownTime
                )
            }
            AppState.setIsLoading(false)
        } else if (hasLocalCoreData) {
            tips = "远程较慢，使用本地缓存启动..."
            applyCoreData(localCache.studentJson, localCache.subjectJson)
            applyOptionalLocalData(localCache)
            delay(120)
            AppState.setIsLoading(false)
        } else {
            tips = "数据加载失败，请检查网络。"
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
            tips.contains("读取本地缓存") -> 0.15f
            tips.contains("离线模式启动中") -> 0.55f
            tips.contains("并行拉取远程配置") -> 0.35f
            tips.contains("并行加载课程和名单") -> 0.72f
            tips.contains("远程较慢，使用本地缓存启动") -> 0.88f
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
