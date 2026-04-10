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
                    screenContent = screenContent,
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
    screenContent: String,
    wordList: List<WordItem>?,
    onClose: () -> Unit
) {
    val colors = AppTheme.colors
    val totalWords = wordList?.size ?: 0
    val newWordCount = wordList?.count { it.category == "new_word" } ?: 0
    val familiarMeaningCount = wordList?.count { it.category == "familiar_new_meaning" } ?: 0
    val excerptLength = screenContent.trim().length

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
                        listOf(colors.primary, colors.primaryVariant, colors.accent.copy(alpha = 0.92f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LearningSummaryChip(
                        label = "识别片段",
                        value = if (excerptLength > 0) "${excerptLength}字" else "--",
                        accent = Color.White.copy(alpha = 0.22f),
                        textColor = colors.onPrimary
                    )
                    LearningSummaryChip(
                        label = "待学条目",
                        value = totalWords.toString(),
                        accent = Color(0xFFDCFCE7).copy(alpha = 0.35f),
                        textColor = colors.onPrimary
                    )
                    LearningSummaryChip(
                        label = "生词/熟义",
                        value = "$newWordCount/$familiarMeaningCount",
                        accent = Color(0xFFFFF1C2).copy(alpha = 0.38f),
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
                    colors = colors
                )
                !hasAnalysisResult -> MessageState(
                    title = "暂无结果",
                    message = "没有拿到有效分析结果，请重试。",
                    colors = colors
                )
                else -> WordListPanel(
                    wordList = wordList,
                    screenContent = screenContent,
                    colors = colors
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
            .clip(RoundedCornerShape(28.dp))
            .background(colors.cardBackground.copy(alpha = 0.96f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 28.dp, vertical = 36.dp),
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
            .background(colors.cardBackground.copy(alpha = 0.98f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
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
    screenContent: String,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.cardBackground.copy(alpha = 0.98f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.primary.copy(alpha = 0.12f),
                                        colors.accent.copy(alpha = 0.08f),
                                        colors.success.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "学习摘要",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = excerptPreview,
                            style = TextStyle(fontSize = 14.sp),
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(12.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .background(accent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 10.sp, color = textColor.copy(alpha = 0.88f))
        Spacer(Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 11.sp, color = accent)
        Spacer(Modifier.height(3.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF24324A))
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
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, colors.border.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word.ifBlank { "未命名条目" },
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (word.type.isBlank()) "未标注词性" else word.type,
                    style = TextStyle(fontSize = 13.sp),
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = formatWordCategory(word.category),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = word.meaning.ifBlank { "暂无释义" },
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            color = colors.textPrimary
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
