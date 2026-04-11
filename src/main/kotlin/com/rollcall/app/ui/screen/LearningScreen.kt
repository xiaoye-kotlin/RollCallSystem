package com.rollcall.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.rollcall.app.data.model.VocabularyBookEntry
import com.rollcall.app.data.model.WordItem
import com.rollcall.app.network.ZhipuAIClient
import com.rollcall.app.ocr.ScreenshotHelper
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import com.rollcall.app.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val WORD_BOOK_FILE_PATH = "D:/Xiaoye/Learning/WordBook.json"

/**
 * 英语单词识别与学习界面
 * 自动截屏 -> OCR识别 -> AI分析 -> 展示生词
 */
@Composable
fun recognizeWord() {
    val isLearning = AppState.isLearning.collectAsState()
    val learningTriggerMode = AppState.learningTriggerMode.collectAsState()
    val aiPrompt = AppState.aiPrompt.ifBlank { AppState.DEFAULT_AI_PROMPT }
    val service = remember { ZhipuAIClient() }
    val isAutoTrigger = learningTriggerMode.value == AppState.LearningTriggerMode.AUTO

    var screenContent by remember { mutableStateOf("") }
    var wordList by remember { mutableStateOf<List<WordItem>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasAnalysisResult by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("准备开始 OCR 识别") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var captureAttemptNonce by remember { mutableStateOf(0) }
    var analyzeAttemptNonce by remember { mutableStateOf(0) }
    var latestScreenshotFile by remember { mutableStateOf<File?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    fun resetLearningState() {
        screenContent = ""
        wordList = null
        isLoading = false
        hasAnalysisResult = false
        statusMessage = "准备开始 OCR 识别"
        errorMessage = null
        saveMessage = null
    }

    fun releaseLatestScreenshot() {
        try {
            latestScreenshotFile?.takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
        } finally {
            latestScreenshotFile = null
        }
    }

    fun closeLearningWindow() {
        AppState.finishLearning()
        releaseLatestScreenshot()
        resetLearningState()
    }

    fun retryCapture() {
        if (isLoading) {
            return
        }

        resetLearningState()
        if (isLearning.value) {
            captureAttemptNonce++
        } else {
            AppState.startLearning(AppState.LearningTriggerMode.MANUAL)
        }
    }

    fun retryAnalyze() {
        if (isLoading || screenContent.isBlank()) {
            return
        }
        errorMessage = null
        hasAnalysisResult = false
        wordList = null
        statusMessage = "正在重新分析生词..."
        analyzeAttemptNonce++
    }

    LaunchedEffect(isLearning.value, learningTriggerMode.value, captureAttemptNonce) {
        if (!isLearning.value) {
            return@LaunchedEffect
        }

        screenContent = ""
        wordList = null
        hasAnalysisResult = false
        errorMessage = null
        isLoading = true
        statusMessage = if (AppState.aiModelSupportsImage) "正在静默截图..." else "正在截取屏幕内容..."

        try {
            releaseLatestScreenshot()
            val screenshotFile = withContext(Dispatchers.IO) {
                ScreenshotHelper.takeSilentScreenshot()
            }
            latestScreenshotFile = screenshotFile
            println("OCR capture -> supportsImage=${AppState.aiModelSupportsImage}, file=${screenshotFile.absolutePath}, size=${screenshotFile.length()}")

            if (!AppState.aiModelSupportsImage) {
                val ocrResult = withContext(Dispatchers.IO) {
                    com.rollcall.app.ocr.OcrHelper().recognizeImage(screenshotFile)
                }
                val rawContent = ocrResult.trim()
                screenContent = extractEnglishLearningContent(rawContent).ifBlank { rawContent }
                println("OCR text ready -> rawLength=${rawContent.length}, filteredLength=${screenContent.length}")
            }
        } catch (_: CancellationException) {
            return@LaunchedEffect
        } catch (e: Exception) {
            println("OCR capture failed -> ${e.message}")
            if (isAutoTrigger) {
                closeLearningWindow()
            } else {
                errorMessage = e.message ?: "OCR 识别失败"
                statusMessage = "OCR 失败"
            }
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = false
    }

    LaunchedEffect(isLearning.value, learningTriggerMode.value, analyzeAttemptNonce, screenContent, latestScreenshotFile) {
        val hasImageInput = AppState.aiModelSupportsImage && latestScreenshotFile != null
        if (!isLearning.value || (!hasImageInput && screenContent.isBlank())) {
            return@LaunchedEffect
        }

        if (hasAnalysisResult && errorMessage == null && analyzeAttemptNonce == 0) {
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            if (!hasImageInput && screenContent.isBlank()) {
                if (isAutoTrigger) closeLearningWindow() else {
                    errorMessage = "未识别到可用文字，请切到包含清晰英文内容的页面后重试。"
                    statusMessage = "OCR 未识别到文字"
                }
                return@LaunchedEffect
            }

            if (!hasImageInput && !looksLikeEnglishLearningPage(screenContent)) {
                if (isAutoTrigger) {
                    closeLearningWindow()
                    return@LaunchedEffect
                }
                statusMessage = "检测到中英混排页面，继续尝试分析..."
            }

            if (isAutoTrigger && shouldSkipAutoLearningContent(screenContent)) {
                closeLearningWindow()
                return@LaunchedEffect
            }

            var usedVisionPath = false
            val aiAnswer = if (AppState.aiModelSupportsImage) {
                val imageFile = latestScreenshotFile
                    ?: throw IllegalStateException("截图文件不存在")

                if (isAutoTrigger && shouldSkipAutoLearningImage(imageFile)) {
                    closeLearningWindow()
                    return@LaunchedEffect
                }

                statusMessage = "截图完成，正在提交图片分析..."
                try {
                    usedVisionPath = true
                    println("OCR flow -> trying vision path")
                    askAiVisionQuestion(service, imageFile, buildVisionPrompt(aiPrompt))
                } catch (visionError: Exception) {
                    println("OCR flow -> vision failed, message=${visionError.message}")
                    if (!shouldFallbackToOcr(visionError)) {
                        throw visionError
                    }
                    statusMessage = "图片直传失败，回退 OCR 分析..."
                    val ocrText = withTimeout(45_000L) {
                        withContext(Dispatchers.IO) {
                            com.rollcall.app.ocr.OcrHelper().recognizeImage(imageFile)
                        }
                    }.trim()
                    screenContent = extractEnglishLearningContent(ocrText).ifBlank { ocrText }
                    println("OCR flow -> fallback OCR ready, rawLength=${ocrText.length}, filteredLength=${screenContent.length}")
                    statusMessage = "图片分析失败，已回退 OCR 文本分析..."
                    withTimeout(90_000L) {
                        askAiQuestion(service, buildEnhancedAnalysisPrompt(aiPrompt, screenContent))
                    }
                }
            } else {
                statusMessage = "OCR 完成，正在分析生词..."
                println("OCR flow -> using OCR text path directly, textLength=${screenContent.length}")
                withTimeout(90_000L) {
                    askAiQuestion(service, buildEnhancedAnalysisPrompt(aiPrompt, screenContent))
                }
            }
            var processedWords = processAiResponse(aiAnswer, skipIfSimilar = isAutoTrigger)
            println("OCR flow -> initial parse result count=${processedWords?.size ?: -1}, usedVision=$usedVisionPath")
            if (processedWords == null && usedVisionPath) {
                val imageFile = latestScreenshotFile
                if (imageFile != null) {
                    statusMessage = "图片结果不可用，已回退 OCR 文本分析..."
                    println("OCR flow -> vision response parse failed, retrying with OCR text fallback")
                    val ocrText = withTimeout(45_000L) {
                        withContext(Dispatchers.IO) {
                            com.rollcall.app.ocr.OcrHelper().recognizeImage(imageFile)
                        }
                    }.trim()
                    screenContent = extractEnglishLearningContent(ocrText).ifBlank { ocrText }
                    val fallbackAnswer = withTimeout(90_000L) {
                        askAiQuestion(service, buildEnhancedAnalysisPrompt(aiPrompt, screenContent))
                    }
                    processedWords = processAiResponse(fallbackAnswer, skipIfSimilar = isAutoTrigger)
                    println("OCR flow -> fallback parse result count=${processedWords?.size ?: -1}")
                    if (processedWords != null) {
                        statusMessage = "图片模式已回退 OCR 分析完成"
                    }
                }
            }
            wordList = processedWords
            hasAnalysisResult = true

            if (isAutoTrigger) {
                persistAutoLearningResult(screenContent, processedWords, latestScreenshotFile)
                closeLearningWindow()
                return@LaunchedEffect
            }

            statusMessage = "分析完成"
            println("OCR flow -> completed, finalCount=${processedWords?.size ?: -1}")
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            println("OCR flow failed -> ${e.message}")
            if (isAutoTrigger) {
                closeLearningWindow()
            } else {
                errorMessage = when {
                    e.message?.contains("The coroutine scope left the composition") == true -> "OCR 窗口已被新的操作中断，请再试一次。"
                    isAiTimeoutError(e) -> "云端分析响应超时，请稍后重试，或点击“重新分析”再次尝试。"
                    e.message?.contains("Timed out waiting for") == true -> "回退分析超时，请点击“重新截图”再试一次。"
                    isRateLimitError(e) -> "云端模型额度已用完，请稍后再试或更换可用的 AI Key。"
                    else -> e.message ?: "OCR 识别失败"
                }
                statusMessage = "处理失败"
            }
        } finally {
            isLoading = false
        }
    }

    val shouldShowWindow = isLearning.value && !isAutoTrigger

    if (shouldShowWindow) {
        Window(
            onCloseRequest = ::closeLearningWindow,
            title = "英语生词识别结果",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = rememberWindowState(
                position = WindowPosition(Alignment.BottomEnd),
                size = DpSize(540.dp, 800.dp)
            ),
        ) {
            AppTheme {
                LearningPanel(
                    isLoading = isLoading,
                    statusMessage = statusMessage,
                    errorMessage = errorMessage,
                    hasAnalysisResult = hasAnalysisResult,
                    screenContent = screenContent,
                    wordList = wordList,
                    saveMessage = saveMessage,
                    onClose = ::closeLearningWindow,
                    onRetryCapture = ::retryCapture,
                    onRetryAnalyze = ::retryAnalyze,
                    onSaveWordBook = {
                        val savedCount = saveWordsToWordBook(wordList)
                        saveMessage = if (savedCount > 0) {
                            "已加入生词本 $savedCount 个"
                        } else {
                            "生词本里已有这些词"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LearningPanel(
    isLoading: Boolean,
    statusMessage: String,
    errorMessage: String?,
    hasAnalysisResult: Boolean,
    screenContent: String,
    wordList: List<WordItem>?,
    saveMessage: String?,
    onClose: () -> Unit,
    onRetryCapture: () -> Unit,
    onRetryAnalyze: () -> Unit,
    onSaveWordBook: () -> Unit
) {
    val colors = AppTheme.colors
    val totalWords = wordList?.size ?: 0
    val newWordCount = wordList?.count { it.category == "new_word" } ?: 0
    val familiarMeaningCount = wordList?.count { it.category == "familiar_new_meaning" } ?: 0
    val excerptLength = screenContent.trim().length
    val isShowingResult = hasAnalysisResult && errorMessage == null && !isLoading
    val progressValue = when {
        isShowingResult -> 1f
        statusMessage.contains("重新分析") || statusMessage.contains("提交图片") || statusMessage.contains("分析") -> 0.78f
        statusMessage.contains("截图") || statusMessage.contains("OCR") -> 0.42f
        else -> 0.12f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surface,
                        colors.surfaceVariant,
                        colors.background
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF4C85F6),
                            Color(0xFF5A79F2),
                            Color(0xFFFF8B7B)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OCR 生词识别",
                    style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.onPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = statusMessage,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    color = colors.onPrimary.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LearningSummaryChip(
                        label = "识别片段",
                        value = if (excerptLength > 0) "${excerptLength}字" else "--",
                        accent = Color.White.copy(alpha = 0.18f),
                        textColor = colors.onPrimary
                    )
                    LearningSummaryChip(
                        label = "待学条目",
                        value = totalWords.toString(),
                        accent = Color(0xFFDCEBFF).copy(alpha = 0.3f),
                        textColor = colors.onPrimary
                    )
                    LearningSummaryChip(
                        label = "生词/熟义",
                        value = "$newWordCount/$familiarMeaningCount",
                        accent = Color(0xFFFFF1C2).copy(alpha = 0.28f),
                        textColor = colors.onPrimary
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = progressValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = Color.White.copy(alpha = 0.92f),
            backgroundColor = Color.White.copy(alpha = 0.18f)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> LoadingState(statusMessage = statusMessage, colors = colors)
                errorMessage != null -> MessageState(
                    title = "识别失败",
                    message = errorMessage,
                    colors = colors,
                    canReanalyze = screenContent.isNotBlank(),
                    onRetryCapture = onRetryCapture,
                    onRetryAnalyze = onRetryAnalyze
                )
                !hasAnalysisResult -> MessageState(
                    title = "暂无结果",
                    message = "没有拿到有效分析结果，请重试。",
                    colors = colors,
                    canReanalyze = screenContent.isNotBlank(),
                    onRetryCapture = onRetryCapture,
                    onRetryAnalyze = onRetryAnalyze
                )
                else -> WordListPanel(
                    wordList = wordList,
                    screenContent = screenContent,
                    colors = colors,
                    onRetryCapture = onRetryCapture,
                    onRetryAnalyze = onRetryAnalyze,
                    onSaveWordBook = onSaveWordBook,
                    saveMessage = saveMessage
                )
            }
        }

        Box(
            modifier = Modifier
                .height(92.dp)
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.primaryVariant, colors.primary, colors.accent.copy(alpha = 0.92f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "完成",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    statusMessage: String,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.8f), RoundedCornerShape(30.dp))
            .padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = colors.primary,
            strokeWidth = 5.dp,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = statusMessage,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "保持页面静止几秒，系统会自动完成截图、OCR 与生词分析。",
            style = TextStyle(fontSize = 16.sp),
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MessageState(
    title: String,
    message: String,
    colors: com.rollcall.app.ui.theme.AppColors,
    canReanalyze: Boolean = false,
    onRetryCapture: (() -> Unit)? = null,
    onRetryAnalyze: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.78f), RoundedCornerShape(30.dp))
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetryCapture != null) {
            Spacer(Modifier.height(22.dp))
            RetryActionRow(
                colors = colors,
                canReanalyze = canReanalyze,
                onRetryCapture = onRetryCapture,
                onRetryAnalyze = onRetryAnalyze
            )
        }
    }
}

@Composable
private fun WordListPanel(
    wordList: List<WordItem>?,
    screenContent: String,
    colors: com.rollcall.app.ui.theme.AppColors,
    onRetryCapture: () -> Unit,
    onRetryAnalyze: () -> Unit,
    onSaveWordBook: () -> Unit,
    saveMessage: String?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, colors.cardBorder.copy(alpha = 0.78f), RoundedCornerShape(30.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            wordList == null -> {
                item {
                    MessageState(
                        title = "未识别到有效文段",
                        message = "OCR 结果不是适合学习的英文段落，或 AI 返回格式异常。",
                        colors = colors,
                        canReanalyze = screenContent.isNotBlank(),
                        onRetryCapture = onRetryCapture,
                        onRetryAnalyze = onRetryAnalyze
                    )
                }
            }
            wordList.isEmpty() -> {
                item {
                    MessageState(
                        title = "未发现生词",
                        message = "当前截图中的英文内容较基础，暂时没有需要重点学习的单词。",
                        colors = colors,
                        canReanalyze = screenContent.isNotBlank(),
                        onRetryCapture = onRetryCapture,
                        onRetryAnalyze = onRetryAnalyze
                    )
                }
            }
            else -> {
                item {
                    val newWordCount = wordList.count { it.category == "new_word" }
                    val familiarMeaningCount = wordList.count { it.category == "familiar_new_meaning" }
                    val excerptPreview = screenContent
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(90)
                        .ifBlank { "当前识别片段为空" }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFEAF2FF),
                                        Color(0xFFFFF4F0),
                                        Color(0xFFEAFBEF)
                                    )
                                )
                            )
                            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "学习摘要",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = excerptPreview,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RetryActionButton(
                                label = "加入生词本",
                                colors = colors,
                                outline = false,
                                onClick = onSaveWordBook,
                                modifier = Modifier.weight(1f)
                            )
                            RetryActionButton(
                                label = "重新分析",
                                colors = colors,
                                outline = true,
                                onClick = onRetryAnalyze,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!saveMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = saveMessage,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.success
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LearningStatCard(
                                title = "总条目",
                                value = wordList.size.toString(),
                                accent = colors.primary,
                                modifier = Modifier.weight(1f)
                            )
                            LearningStatCard(
                                title = "生词",
                                value = newWordCount.toString(),
                                accent = colors.error,
                                modifier = Modifier.weight(1f)
                            )
                            LearningStatCard(
                                title = "熟词生义",
                                value = familiarMeaningCount.toString(),
                                accent = colors.success,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = wordList,
                    key = { index, word -> "${word.word}_${word.type}_${word.category}_$index" }
                ) { index, word ->
                    WordCard(index = index, word = word, colors = colors)
                }
            }
        }
    }
}

@Composable
private fun LearningSummaryChip(
    label: String,
    value: String,
    accent: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.88f))
        Spacer(Modifier.height(3.dp))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
    }
}

@Composable
private fun LearningStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
        Spacer(Modifier.height(4.dp))
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF24324A))
    }
}

@Composable
private fun WordCard(
    index: Int,
    word: WordItem,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    val categoryColor = when (word.category) {
        "new_word" -> colors.error
        "familiar_new_meaning" -> colors.primary
        else -> colors.textSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, colors.border.copy(alpha = 0.78f), RoundedCornerShape(24.dp))
            .animateContentSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = categoryColor
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word.ifBlank { "未命名条目" },
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (word.type.isBlank()) "未标注词性" else word.type,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = formatWordCategory(word.category),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = categoryColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = word.meaning.ifBlank { "暂无释义" },
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
        if (word.root.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "词根词缀  ${word.root}",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = colors.primary
            )
        }
        if (word.example.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = word.example,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun RetryActionRow(
    colors: com.rollcall.app.ui.theme.AppColors,
    canReanalyze: Boolean,
    onRetryCapture: () -> Unit,
    onRetryAnalyze: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canReanalyze && onRetryAnalyze != null) {
            RetryActionButton(
                label = "重新分析",
                colors = colors,
                outline = true,
                onClick = onRetryAnalyze,
                modifier = Modifier.weight(1f)
            )
        }
        RetryActionButton(
            label = "重新截图",
            colors = colors,
            outline = false,
            onClick = onRetryCapture,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RetryActionButton(
    label: String,
    colors: com.rollcall.app.ui.theme.AppColors,
    outline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (outline) Color.White else colors.primary,
            contentColor = if (outline) colors.primary else colors.onPrimary
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

private fun formatWordCategory(category: String): String = when (category) {
    "new_word" -> "生词"
    "familiar_new_meaning" -> "熟词生义"
    else -> category.ifBlank { "待确认" }
}

private suspend fun askAiQuestion(service: ZhipuAIClient, question: String): String =
    suspendCancellableCoroutine { continuation ->
        service.askQuestionAsync(
            question = question,
            onSuccess = {
                if (continuation.isActive) {
                    continuation.resume(it)
                }
            },
            onError = {
                if (continuation.isActive) {
                    continuation.resumeWithException(it)
                }
            }
        )
    }

private suspend fun askAiVisionQuestion(
    service: ZhipuAIClient,
    imageFile: File,
    prompt: String
): String = suspendCancellableCoroutine { continuation ->
    service.askVisionQuestionAsync(
        imageFile = imageFile,
        prompt = prompt,
        onSuccess = {
            if (continuation.isActive) {
                continuation.resume(it)
            }
        },
        onError = {
            if (continuation.isActive) {
                continuation.resumeWithException(it)
            }
        }
    )
}

private fun looksLikeEnglishLearningPage(text: String): Boolean {
    val normalizedText = text.replace('\u3000', ' ')
    val englishWords = Regex("[A-Za-z]{2,}").findAll(normalizedText)
        .map { it.value.lowercase(Locale.getDefault()) }
        .toList()
    if (englishWords.size < 10) return false

    val uniqueWords = englishWords.toSet().size
    if (uniqueWords < 6) return false

    val englishChars = normalizedText.count { it.isLetter() && it.code < 128 }
    val englishRatio = englishChars.toDouble() / normalizedText.length.coerceAtLeast(1)

    val englishLines = normalizedText
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .count { line ->
            Regex("[A-Za-z]{2,}").findAll(line).count() >= 6
        }

    val hasEnglishParagraph = Regex("([A-Za-z][A-Za-z'\",;:.!?()\\- ]{60,})")
        .containsMatchIn(normalizedText)
    val longestEnglishRunWords = Regex(
        "(?is)(?:\\b[A-Za-z][A-Za-z'\\-]*\\b(?:\\s+|[,.!?;:()\"'])+){11,}\\b[A-Za-z][A-Za-z'\\-]*\\b"
    )
        .findAll(normalizedText)
        .maxOfOrNull { matchResult ->
            Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b").findAll(matchResult.value).count()
        } ?: 0

    return when {
        englishRatio >= 0.22 -> true
        englishWords.size >= 24 && englishLines >= 2 -> true
        englishWords.size >= 32 && hasEnglishParagraph -> true
        longestEnglishRunWords >= 12 -> true
        else -> false
    }
}

private fun extractEnglishLearningContent(text: String): String {
    val cleanedText = text.replace('\u3000', ' ')
    val lines = cleanedText.lines().map { it.trim() }
    val blocks = mutableListOf<String>()
    val currentBlock = mutableListOf<String>()

    fun flushBlock() {
        if (currentBlock.isNotEmpty()) {
            blocks += currentBlock.joinToString("\n")
            currentBlock.clear()
        }
    }

    for (line in lines) {
        if (line.isBlank()) {
            flushBlock()
            continue
        }

        val englishWordCount = Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b").findAll(line).count()
        val englishCharCount = line.count { it.isLetter() && it.code < 128 }
        val englishRatio = englishCharCount.toDouble() / line.length.coerceAtLeast(1)
        val looksEnglishLine = englishWordCount >= 4 || (englishWordCount >= 2 && englishRatio >= 0.55)

        if (looksEnglishLine) {
            currentBlock += line
        } else if (currentBlock.isNotEmpty()) {
            flushBlock()
        }
    }
    flushBlock()

    val bestBlock = blocks.maxByOrNull { block ->
        Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b").findAll(block).count()
    }.orEmpty()

    val bestBlockWordCount = Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b").findAll(bestBlock).count()
    if (bestBlockWordCount >= 20) {
        return bestBlock
    }

    val longEnglishRun = Regex(
        "(?is)(?:\\b[A-Za-z][A-Za-z'\\-]*\\b(?:\\s+|[,.!?;:()\"'\\-])+){19,}\\b[A-Za-z][A-Za-z'\\-]*\\b"
    ).find(cleanedText)?.value.orEmpty()

    return if (longEnglishRun.isNotBlank()) longEnglishRun.trim() else cleanedText
}

private fun persistAutoLearningResult(
    screenContent: String,
    wordList: List<WordItem>?,
    screenshotFile: File?
) {
    if (wordList.isNullOrEmpty()) {
        return
    }
    val gson = Gson()
    FileHelper.writeToFile("D:/Xiaoye/Learning/LatestAutoText.txt", screenContent)
    FileHelper.writeToFile("D:/Xiaoye/Learning/LatestAutoWords.json", gson.toJson(wordList))
    screenshotFile?.let {
        FileHelper.writeToFile("D:/Xiaoye/Learning/LatestAutoImageHash.txt", calculateFileHash(it))
    }
}

private fun shouldSkipAutoLearningContent(screenContent: String): Boolean {
    val lastAutoText = FileHelper.readFromFile("D:/Xiaoye/Learning/LatestAutoText.txt")
    if (lastAutoText == "404" || lastAutoText.isBlank()) {
        return false
    }

    val currentWords = Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b")
        .findAll(screenContent.lowercase(Locale.getDefault()))
        .map { it.value }
        .toSet()
    val lastWords = Regex("\\b[A-Za-z][A-Za-z'\\-]*\\b")
        .findAll(lastAutoText.lowercase(Locale.getDefault()))
        .map { it.value }
        .toSet()

    if (currentWords.isEmpty() || lastWords.isEmpty()) {
        return false
    }

    val intersection = currentWords.intersect(lastWords).size
    val union = currentWords.union(lastWords).size.coerceAtLeast(1)
    return intersection.toDouble() / union.toDouble() >= 0.9
}

private fun shouldSkipAutoLearningImage(imageFile: File): Boolean {
    val lastHash = FileHelper.readFromFile("D:/Xiaoye/Learning/LatestAutoImageHash.txt")
    if (lastHash == "404" || lastHash.isBlank()) {
        return false
    }
    return calculateFileHash(imageFile) == lastHash.trim()
}

private fun calculateFileHash(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun buildVisionPrompt(aiPrompt: String): String {
    return buildString {
        append("请直接阅读这张截图中的内容并完成英语生词分析。")
        append("如果图片里没有适合学习的英文正文，返回'未识别到有效文段'。")
        append("输出要求保持和原提示一致，并且每个条目尽量补充 example(超短英文例句) 和 root(词根/词缀或构词提示) 字段。")
        append(aiPrompt.removeSuffix("OCR文本："))
    }
}

private fun buildEnhancedAnalysisPrompt(aiPrompt: String, screenContent: String): String {
    return buildString {
        append(aiPrompt.removeSuffix("OCR文本："))
        append("额外要求：输出JSON时，若能提供信息，请为每个单词补充 example(超短英文例句) 与 root(词根/词缀或构词提示) 字段。")
        append("若没有把握可留空字符串。")
        append("OCR文本：")
        append(screenContent)
    }
}

private fun shouldFallbackToOcr(error: Exception): Boolean {
    val message = error.message.orEmpty().lowercase(Locale.getDefault())
    return "image" in message ||
        "vision" in message ||
        "unsupported" in message ||
        "invalid" in message ||
        "content" in message && "array" in message ||
        "400" in message ||
        "500" in message ||
        "502" in message ||
        "503" in message ||
        "504" in message ||
        "bad gateway" in message ||
        "service unavailable" in message ||
        "gateway timeout" in message ||
        "timeout" in message ||
        "stream was reset" in message ||
        "unexpected end of stream" in message
}

private fun isAiTimeoutError(error: Exception): Boolean {
    val message = error.message.orEmpty().lowercase(Locale.getDefault())
    return "timeout" in message ||
        "stream was reset" in message ||
        "unexpected end of stream" in message ||
        "connection reset" in message ||
        "bad gateway" in message ||
        "gateway timeout" in message
}

private fun isRateLimitError(error: Exception): Boolean {
    val message = error.message.orEmpty().lowercase(Locale.getDefault())
    return "rate_limit" in message || "429" in message || "usage limit exceeded" in message
}

// ==================== AI回复处理函数 ====================

/**
 * 处理AI回复，提取单词列表
 * 支持JSON对象和JSON数组两种格式
 */
private fun processAiResponse(aiResponse: String, skipIfSimilar: Boolean = false): List<WordItem>? {
    return try {
        var clean = aiResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        clean = clean.replace(Regex("^```(json)?"), "").replace(Regex("```$"), "").trim()

        if (!clean.startsWith("{") && !clean.startsWith("[")) {
            return parseLooseWordItems(clean)
        }

        if (skipIfSimilar) {
            val lastWords = FileHelper.readFromFile("D:/Xiaoye/Learning/LastWords.json")
            if (lastWords != "404" && calculateSimilarity(clean, lastWords) >= 0.2) {
                return null
            }
            FileHelper.writeToFile("D:/Xiaoye/Learning/LastWords.json", clean)
        }

        val gson = Gson()
        val wordList = mutableListOf<WordItem>()

        val wordsArray = if (clean.startsWith("{")) {
            gson.fromJson(clean, JsonObject::class.java).getAsJsonArray("words") ?: return emptyList()
        } else {
            gson.fromJson(clean, JsonArray::class.java)
        }

        wordsArray.forEach { element ->
            val obj = element.asJsonObject
            wordList.add(
                WordItem(
                    word = obj.get("word")?.asString ?: "",
                    type = obj.get("type")?.asString ?: "",
                    meaning = obj.get("meaning")?.asString ?: "",
                    category = obj.get("category")?.asString ?: "",
                    example = obj.get("example")?.asString ?: "",
                    root = obj.get("root")?.asString ?: ""
                )
            )
        }
        deduplicateWordItems(wordList)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun deduplicateWordItems(items: List<WordItem>): List<WordItem> {
    val merged = linkedMapOf<String, WordItem>()
    items.forEach { item ->
        val normalizedWord = item.word.trim().lowercase(Locale.getDefault())
        if (normalizedWord.isBlank() || !normalizedWord.any { it.isLetter() }) {
            return@forEach
        }
        val key = buildString {
            append(normalizedWord)
            append('|')
            append(item.category.trim())
            append('|')
            append(item.meaning.trim())
        }
        val previous = merged[key]
        merged[key] = if (previous == null) {
            item.copy(word = item.word.trim())
        } else {
            previous.copy(
                type = previous.type.ifBlank { item.type },
                example = previous.example.ifBlank { item.example },
                root = previous.root.ifBlank { item.root }
            )
        }
    }
    return merged.values.toList()
}

private fun saveWordsToWordBook(wordList: List<WordItem>?): Int {
    if (wordList.isNullOrEmpty()) return 0

    val gson = Gson()
    val existingJson = FileHelper.readFromFile(WORD_BOOK_FILE_PATH)
    val existingEntries: MutableList<VocabularyBookEntry> =
        if (existingJson != "404" && existingJson.isNotBlank()) {
            runCatching {
                gson.fromJson<MutableList<VocabularyBookEntry>>(
                    existingJson,
                    object : TypeToken<MutableList<VocabularyBookEntry>>() {}.type
                ) ?: mutableListOf()
            }.getOrElse { mutableListOf() }
        } else {
            mutableListOf()
        }

    val existingKeys = existingEntries.associateBy {
        "${it.word.trim().lowercase(Locale.getDefault())}|${it.meaning.trim()}|${it.category.trim()}"
    }.toMutableMap()

    var addedCount = 0
    wordList.forEach { word ->
        val key = "${word.word.trim().lowercase(Locale.getDefault())}|${word.meaning.trim()}|${word.category.trim()}"
        val old = existingKeys[key]
        if (old == null) {
            val entry = VocabularyBookEntry(
                word = word.word.trim(),
                type = word.type.trim(),
                meaning = word.meaning.trim(),
                category = word.category.trim(),
                example = word.example.trim(),
                root = word.root.trim()
            )
            existingEntries += entry
            existingKeys[key] = entry
            addedCount++
        } else if (old.example.isBlank() || old.root.isBlank()) {
            val updated = old.copy(
                type = old.type.ifBlank { word.type.trim() },
                example = old.example.ifBlank { word.example.trim() },
                root = old.root.ifBlank { word.root.trim() }
            )
            val index = existingEntries.indexOfFirst {
                it.word == old.word && it.meaning == old.meaning && it.category == old.category
            }
            if (index >= 0) {
                existingEntries[index] = updated
                existingKeys[key] = updated
            }
        }
    }

    FileHelper.writeToFile(WORD_BOOK_FILE_PATH, gson.toJson(existingEntries))
    return addedCount
}

/**
 * 计算两个JSON的单词相似度
 * 用于判断是否重复显示相同结果
 */
private fun calculateSimilarity(json1: String, json2: String): Double {
    return try {
        val gson = Gson()
        val words1 = extractWordsFromJson(json1, gson)
        val words2 = extractWordsFromJson(json2, gson)
        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        intersection.toDouble() / union.toDouble()
    } catch (e: Exception) {
        0.0
    }
}

/**
 * 从JSON中提取所有单词
 */
private fun extractWordsFromJson(json: String, gson: Gson): Set<String> {
    return try {
        val words = mutableSetOf<String>()
        val array = if (json.startsWith("{")) {
            gson.fromJson(json, JsonObject::class.java).getAsJsonArray("words") ?: return emptySet()
        } else {
            gson.fromJson(json, JsonArray::class.java)
        }
        array.forEach { element ->
            element.asJsonObject.get("word")?.asString?.let {
                words.add(it.lowercase(Locale.getDefault()).trim())
            }
        }
        words
    } catch (e: Exception) {
        emptySet()
    }
}

private fun parseLooseWordItems(text: String): List<WordItem>? {
    val normalized = text
        .replace(Regex("[•·]"), "\n")
        .replace(Regex("\\r\\n?"), "\n")
        .trim()
    if (normalized.isBlank()) {
        return null
    }

    val candidates = mutableListOf<WordItem>()
    val lines = normalized.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    lines.forEach { line ->
        val match = Regex(
            """(?i)^([A-Za-z][A-Za-z'\- ]{1,30})\s*(?:\(([^)]{1,16})\)|[-,:：]\s*([^,:：]{1,16}))?\s*[-:：]\s*(.+)$"""
        ).find(line) ?: return@forEach

        val word = match.groupValues[1].trim()
        val type = listOf(match.groupValues[2], match.groupValues[3]).firstOrNull { it.isNotBlank() }.orEmpty().trim()
        val meaning = match.groupValues[4].trim()
        if (word.count { it.isLetter() } < 2 || meaning.length < 2) {
            return@forEach
        }

        candidates += WordItem(
            word = word,
            type = type,
            meaning = meaning,
            category = "new_word"
        )
    }

    return deduplicateWordItems(candidates).takeIf { it.isNotEmpty() }
}
