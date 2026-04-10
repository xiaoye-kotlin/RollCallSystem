package com.rollcall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.util.*

/**
 * 英语单词识别与学习界面
 * 自动截屏→OCR识别→AI分析→展示生词
 * 使用RapidOCR本地引擎，不依赖外部程序
 */
@Composable
fun recognizeWord() {
    val isLearning = AppState.isLearning.collectAsState()
    var screenContent by remember { mutableStateOf("") }

    // AI提示词：让AI分析截图中的英语生词
    val aiPrompt by remember {
        mutableStateOf(
            "你是一个专业的英语词汇分析助手。工作流程：" +
            "1.如果OCR文本英文单词少于5个或包含大量专业术语或是系统界面，返回'未识别到有效文段'；" +
            "2.只分析英文单词（单个单词），忽略中文和英文短语；找出大一学生不认识的单个单词，" +
            "包括完全生词和常见单词的不常见含义；排除基础词汇和专有名词；" +
            "3.输出JSON数组格式：[{'word':'单个单词','type':'词性(n./v./adj./adv.等)'," +
            "'meaning':'中文释义','category':'类型(new_word/familiar_new_meaning)'}]，" +
            "完全陌生的单词标记为'new_word'，常见单词的不常见含义标记为'familiar_new_meaning'。" +
            "按原文顺序不重复，无其他文字。只分析单个英文单词，不分析短语。" +
            "只对通用英语文章分析，专业内容返回'未识别到有效文段'。OCR文本："
        )
    }
    var aiAnswer by remember { mutableStateOf("") }
    val service = remember { ZhipuAIClient() }
    var hasAiAnswered by remember { mutableStateOf(false) }
    var hasTakenScreenshot by remember { mutableStateOf(false) }
    var isRetry by remember { mutableStateOf(false) }

    // 监听学习模式关闭，重新启动截图流程
    LaunchedEffect(isLearning.value) {
        if (!isLearning.value) {
            isRetry = true
        }
    }

    // 主循环：截屏→OCR→AI分析
    LaunchedEffect(Unit) {
        while (true) {
            if (!isLearning.value && isRetry) {
                isRetry = false
                hasAiAnswered = false
                hasTakenScreenshot = false
                screenContent = ""
                aiAnswer = ""

                // 截图并OCR识别
                try {
                    val ocrResult = ScreenshotHelper.takeSilentScreenshotAndRecognize().second
                    screenContent = ocrResult
                    hasTakenScreenshot = true
                    println("截图完成，内容长度: ${screenContent.length}")

                    if (screenContent.isNotEmpty()) {
                        service.askQuestionAsync(
                            question = aiPrompt + screenContent,
                            onSuccess = { answer ->
                                isRetry = false
                                aiAnswer = answer
                                AppState.setIsLearning(true)
                                hasAiAnswered = true
                            },
                            onError = {
                                hasAiAnswered = true
                                AppState.setIsLearning(false)
                                isRetry = true
                                println("AI分析失败: ${it.message}")
                            }
                        )
                    }
                } catch (e: Exception) {
                    println("截图失败: ${e.message}")
                    isRetry = true
                }
            }
            delay(1000)
        }
    }

    // 显示结果窗口
    if (isLearning.value && hasAiAnswered && aiAnswer.isNotEmpty()) {
        Window(
            onCloseRequest = {
                AppState.setIsLearning(false)
                hasAiAnswered = false
                hasTakenScreenshot = false
                screenContent = ""
                aiAnswer = ""
            },
            title = "英语生词识别结果",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            state = rememberWindowState(
                position = WindowPosition(Alignment.BottomEnd),
                size = DpSize.Unspecified
            ),
        ) {
            com.rollcall.app.ui.theme.AppTheme {
                WordListPanel(aiAnswer = aiAnswer)
            }
        }
    }
}

/**
 * 单词列表面板
 * 显示AI分析出的生词列表
 */
@Composable
private fun WordListPanel(aiAnswer: String) {
    val isLearning = AppState.isLearning.collectAsState()
    var wordList by remember { mutableStateOf<List<WordItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 解析AI回复
    LaunchedEffect(aiAnswer) {
        if (isLearning.value && aiAnswer.isNotEmpty()) {
            try {
                val processed = processAiResponse(aiAnswer)
                if (processed == null) {
                    AppState.setIsLearning(false)
                    wordList = emptyList()
                } else {
                    wordList = processed
                    if (processed.isEmpty()) AppState.setIsLearning(false)
                }
                delay(100)
                isLoading = false
            } catch (e: Exception) {
                wordList = emptyList()
                isLoading = false
                AppState.setIsLearning(false)
            }
        }
    }

    if (!isLoading && isLearning.value) {
        val colors = AppTheme.colors

        Column(
            modifier = Modifier
                .height(800.dp)
                .width(500.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 标题栏
            Box(
                modifier = Modifier.height(100.dp).fillMaxWidth()
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📚 单词学习",
                    style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                    color = colors.onPrimary
                )
            }

            // 单词列表
            Box(
                modifier = Modifier.height(600.dp).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        wordList == null -> item { Text("请稍后再试。", color = colors.textSecondary) }
                        wordList!!.isEmpty() -> item { Text("未发现生词。", color = colors.textSecondary) }
                        else -> {
                            // 表头
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (header in listOf("单词", "词性", "释义", "类型")) {
                                        Text(
                                            header,
                                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                            // 单词行
                            items(
                                count = wordList!!.size,
                                key = { "${wordList!![it].word}_${wordList!![it].type}_$it" }
                            ) { index ->
                                val word = wordList!![index]
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp)
                                        .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(word.word, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                                        overflow = TextOverflow.Ellipsis, maxLines = 1, color = colors.textPrimary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(word.type, style = TextStyle(fontSize = 20.sp),
                                        overflow = TextOverflow.Ellipsis, maxLines = 1, color = colors.textSecondary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(word.meaning, style = TextStyle(fontSize = 20.sp),
                                        overflow = TextOverflow.Ellipsis, maxLines = 1, color = colors.textPrimary)
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
                                        overflow = TextOverflow.Ellipsis, maxLines = 1
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // 底部操作栏
            Box(
                modifier = Modifier.height(100.dp).fillMaxWidth()
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { AppState.setIsLearning(false) }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "我知道了",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    } else {
        // 占位（不可见时保持窗口结构）
        Column(
            modifier = Modifier.height(800.dp).width(500.dp)
                .clip(RoundedCornerShape(32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { }
    }
}

// ==================== AI回复处理函数 ====================

/**
 * 处理AI回复，提取单词列表
 * 支持JSON对象和JSON数组两种格式
 */
private fun processAiResponse(aiResponse: String): List<WordItem>? {
    return try {
        // 清理AI回复中的无关标记
        var clean = aiResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        clean = clean.replace(Regex("^```(json)?"), "").replace(Regex("```$"), "").trim()

        if (!clean.startsWith("{") && !clean.startsWith("[")) return null

        // 对比上次结果，避免重复展示
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
            wordList.add(WordItem(
                word = obj.get("word")?.asString ?: "",
                type = obj.get("type")?.asString ?: "",
                meaning = obj.get("meaning")?.asString ?: "",
                category = obj.get("category")?.asString ?: ""
            ))
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
    } catch (e: Exception) { 0.0 }
}

/**
 * 从JSON中提取所有单词（用于相似度计算）
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
    } catch (e: Exception) { emptySet() }
}
