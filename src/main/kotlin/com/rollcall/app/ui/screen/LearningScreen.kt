package com.rollcall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.rollcall.app.data.model.WordItem
import com.rollcall.app.network.ZhipuAIClient
import com.rollcall.app.ocr.ScreenshotHelper
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    fun closeLearningWindow() {
        AppState.finishLearning()
        screenContent = ""
        wordList = null
        isLoading = false
        hasAnalysisResult = false
        statusMessage = "准备开始 OCR 识别"
        errorMessage = null
    }

    LaunchedEffect(isLearning.value, learningTriggerMode.value) {
        if (!isLearning.value) {
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        screenContent = ""
        wordList = null
        hasAnalysisResult = false
        statusMessage = "正在截取屏幕内容..."

        try {
            val (screenshotFile, ocrResult) = withContext(Dispatchers.IO) {
                ScreenshotHelper.takeSilentScreenshotAndRecognize()
            }
            try {
                screenContent = ocrResult.trim()
            } finally {
                if (screenshotFile.exists()) {
                    screenshotFile.delete()
                }
            }

            if (screenContent.isBlank()) {
                if (isAutoTrigger) {
                    closeLearningWindow()
                } else {
                    errorMessage = "未识别到可用文字，请切到包含清晰英文内容的页面后重试。"
                    statusMessage = "OCR 未识别到文字"
                }
                return@LaunchedEffect
            }

            if (!looksLikeEnglishLearningPage(screenContent)) {
                if (isAutoTrigger) {
                    closeLearningWindow()
                } else {
                    errorMessage = "当前页面不像英文阅读内容，请切到英文文章、题目或单词页后再试。"
                    statusMessage = "未检测到目标页面"
                }
                return@LaunchedEffect
            }

            statusMessage = "OCR 完成，正在分析生词..."
            val aiAnswer = askAiQuestion(service, aiPrompt + screenContent)
            val processedWords = processAiResponse(aiAnswer)
            wordList = processedWords
            hasAnalysisResult = true

            if (isAutoTrigger) {
                persistAutoLearningResult(screenContent, processedWords)
                closeLearningWindow()
                return@LaunchedEffect
            }

            statusMessage = "分析完成"
        } catch (e: Exception) {
            if (isAutoTrigger) {
                println("自动OCR处理失败: ${e.message}")
                closeLearningWindow()
            } else {
                errorMessage = e.message ?: "OCR 识别失败"
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
                    wordList = wordList,
                    onClose = ::closeLearningWindow
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
    wordList: List<WordItem>?,
    onClose: () -> Unit
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OCR 生词识别",
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    color = colors.onPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    style = TextStyle(fontSize = 14.sp),
                    color = colors.onPrimary.copy(alpha = 0.85f)
                )
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> LoadingState(statusMessage = statusMessage, colors = colors)
                errorMessage != null -> MessageState(
                    title = "识别失败",
                    message = errorMessage,
                    colors = colors
                )
                !hasAnalysisResult -> MessageState(
                    title = "暂无结果",
                    message = "没有拿到有效分析结果，请重试。",
                    colors = colors
                )
                else -> WordListPanel(wordList = wordList, colors = colors)
            }
        }

        Box(
            modifier = Modifier
                .height(88.dp)
                .fillMaxWidth()
                .background(colors.primary),
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = colors.primary)
        Spacer(Modifier.height(18.dp))
        Text(
            text = statusMessage,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请保持目标页面静止几秒，系统会自动完成截图、OCR 与生词分析。",
            style = TextStyle(fontSize = 14.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun MessageState(
    title: String,
    message: String,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = TextStyle(fontSize = 16.sp),
            color = colors.textSecondary
        )
    }
}

@Composable
private fun WordListPanel(
    wordList: List<WordItem>?,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            wordList == null -> {
                item {
                    MessageState(
                        title = "未识别到有效文段",
                        message = "OCR 结果不是适合学习的英文段落，或 AI 返回格式异常。",
                        colors = colors
                    )
                }
            }
            wordList.isEmpty() -> {
                item {
                    MessageState(
                        title = "未发现生词",
                        message = "当前截图中的英文内容较基础，暂时没有需要重点学习的单词。",
                        colors = colors
                    )
                }
            }
            else -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (header in listOf("单词", "词性", "释义", "类型")) {
                            Text(
                                text = header,
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
                items(
                    count = wordList.size,
                    key = { "${wordList[it].word}_${wordList[it].type}_$it" }
                ) { index ->
                    val word = wordList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = word.word,
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = word.type,
                            style = TextStyle(fontSize = 20.sp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = word.meaning,
                            style = TextStyle(fontSize = 20.sp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when (word.category) {
                                "new_word" -> "生词"
                                "familiar_new_meaning" -> "熟词生义"
                                else -> word.category
                            },
                            style = TextStyle(
                                fontSize = 20.sp,
                                color = when (word.category) {
                                    "new_word" -> colors.error
                                    "familiar_new_meaning" -> colors.primary
                                    else -> colors.textPrimary
                                }
                            ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
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

private fun looksLikeEnglishLearningPage(text: String): Boolean {
    val englishWords = Regex("[A-Za-z]{3,}").findAll(text)
        .map { it.value.lowercase(Locale.getDefault()) }
        .toList()
    if (englishWords.size < 8) return false

    val uniqueWords = englishWords.toSet().size
    val englishChars = text.count { it.isLetter() && it.code < 128 }
    val englishRatio = englishChars.toDouble() / text.length.coerceAtLeast(1)
    return uniqueWords >= 5 && englishRatio >= 0.25
}

private fun persistAutoLearningResult(
    screenContent: String,
    wordList: List<WordItem>?
) {
    if (wordList.isNullOrEmpty()) {
        return
    }
    val gson = Gson()
    FileHelper.writeToFile("D:/Xiaoye/Learning/LatestAutoText.txt", screenContent)
    FileHelper.writeToFile("D:/Xiaoye/Learning/LatestAutoWords.json", gson.toJson(wordList))
}

// ==================== AI回复处理函数 ====================

/**
 * 处理AI回复，提取单词列表
 * 支持JSON对象和JSON数组两种格式
 */
private fun processAiResponse(aiResponse: String): List<WordItem>? {
    return try {
        var clean = aiResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        clean = clean.replace(Regex("^```(json)?"), "").replace(Regex("```$"), "").trim()

        if (!clean.startsWith("{") && !clean.startsWith("[")) return null

        val lastWords = FileHelper.readFromFile("D:/Xiaoye/Learning/LastWords.json")
        if (lastWords != "404" && calculateSimilarity(clean, lastWords) >= 0.2) {
            return null
        }
        FileHelper.writeToFile("D:/Xiaoye/Learning/LastWords.json", clean)

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
                    category = obj.get("category")?.asString ?: ""
                )
            )
        }
        wordList
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
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
