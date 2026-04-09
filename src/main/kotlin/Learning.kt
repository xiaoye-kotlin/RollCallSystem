import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.lang.reflect.Method
import java.util.*
import javax.imageio.ImageIO

@Composable
fun recognizeWord() {
    val isLearning = Global.isLearning.collectAsState()
    var screenContent by remember {
        mutableStateOf("")
    }
    val aiPrompt by remember {
        mutableStateOf(
            "你是一个专业的英语词汇分析助手。工作流程：1.如果OCR文本英文单词少于5个或包含大量专业术语或是系统界面，返回'未识别到有效文段'；2.只分析英文单词（单个单词），忽略中文和英文短语；找出大一学生不认识的单个单词，包括完全生词和常见单词的不常见含义；排除基础词汇和专有名词；3.输出JSON数组格式：[{'word':'单个单词','type':'词性(n./v./adj./adv.等)','meaning':'中文释义','category':'类型(new_word/familiar_new_meaning)'}]，完全陌生的单词标记为'new_word'，常见单词的不常见含义标记为'familiar_new_meaning'。按原文顺序不重复，无其他文字。只分析单个英文单词，不分析短语。只对通用英语文章分析，专业内容返回'未识别到有效文段'。OCR文本："
        )
    }
    var aiAnswer by remember { mutableStateOf("") }
    val service = remember { ZhipuAIClient() }
    var hasAiAnswered by remember { mutableStateOf(false) }
    var hasTakenScreenshot by remember { mutableStateOf(false) }
    var isRetry by remember { mutableStateOf(false) }

    LaunchedEffect(isLearning.value) {
        if (!isLearning.value) {
            isRetry = true
        }
    }

// 监听 isLearning 变化，启动学习流程

    LaunchedEffect(Unit) {
        while (true) {

            println("hasRunRecognition. isLearning: ${isLearning.value}")

            // 只有当 isLearning 从 false 变为 true 时才启动流程
            if (!isLearning.value && isRetry) {
                isRetry = false
                hasAiAnswered = false
                hasTakenScreenshot = false
                screenContent = ""
                aiAnswer = ""

                // 截图和OCR识别
                val ocrResult = takeSilentScreenshotAndRecognize().second
                screenContent = ocrResult
                hasTakenScreenshot = true
                println("截图完成，内容长度: ${screenContent.length}")
                if (screenContent.isNotEmpty()) {
                    service.askQuestionAsync(
                        question = aiPrompt + screenContent,
                        onSuccess = { answer ->
                            isRetry = false
                            aiAnswer = answer
                            println("AI_Submit_Success, isLearning: ${isLearning.value}")
                            Global.setIsLearning(true)
                            hasAiAnswered = true
                            // 这里不需要再设置 isLearning，保持当前状态
                        },
                        onError = {
                            hasAiAnswered = true
                            Global.setIsLearning(false) // 只有出错时才设置为false
                            isRetry = true
                            println("AI_Submit_Error, isLearning: ${isLearning.value}")
                        })
                }
            }
            delay(1000)
        }

    }

// 显示结果窗口
    if (isLearning.value && hasAiAnswered && aiAnswer.isNotEmpty()) {
        Window(
            onCloseRequest = {
                // 关闭窗口时重置状态
                Global.setIsLearning(false)
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
            MaterialTheme {
                aiAnswer(aiAnswer = aiAnswer)
            }
        }
    }
    println("Debug - isLearning: ${isLearning.value}, hasTakenScreenshot: $hasTakenScreenshot, hasAiAnswered: $hasAiAnswered, aiAnswer长度: ${aiAnswer.length}")
}

@Composable
fun aiAnswer(aiAnswer: String) {
    val isLearning = Global.isLearning.collectAsState()
    var wordList by remember { mutableStateOf<List<WordItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(aiAnswer) {
        if (isLearning.value && aiAnswer.isNotEmpty() && aiAnswer.isNotBlank()) {
            try {
                // 处理AI回复内容
                val processedContent = processAiResponse(aiAnswer)

                if (processedContent == null) {
                    Global.setIsLearning(false)
                    println("AI_writeLearningFalse")
                    wordList = emptyList() // 设置为空列表而不是null
                } else {
                    wordList = processedContent
                    if (processedContent.isEmpty()) {
                        Global.setIsLearning(false)
                        println("AI_writeLearningFalse2")
                    }
                }

                delay(100)
                isLoading = false
            } catch (e: Exception) {
                wordList = emptyList()
                isLoading = false
                println("AI_writeLearningFalse3:${e.localizedMessage}")
                Global.setIsLearning(false)
            }
        }
    }

    if (!isLoading && isLearning.value) {
        Column(
            modifier = Modifier
                .height(800.dp)
                .width(500.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF87CEFA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "单词学习",
                    style = androidx.compose.ui.text.TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
                )
            }
            Box(
                modifier = Modifier
                    .height(600.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    when {
                        wordList == null -> {
                            item {
                                Text(
                                    "请稍后再试。",
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }

                        wordList!!.isEmpty() -> {
                            item {
                                Text(
                                    "未发现生词。",
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }

                        else -> {
                            // 表格标题
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "单词",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Text(
                                        "词性",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Text(
                                        "释义",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Text(
                                        "类型",
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }

                            items(
                                count = wordList!!.size,
                                key = { index ->
                                    val word = wordList!![index]
                                    "${word.word}_${word.type}_${word.meaning}_$index"
                                }
                            ) { index ->
                                val word = wordList!![index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        word.word,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        word.type,
                                        style = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        word.meaning,
                                        style = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = when (word.category) {
                                            "new_word" -> "生词"
                                            "familiar_new_meaning" -> "熟词生义"
                                            else -> word.category
                                        },
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 20.sp,
                                            color = when (word.category) {
                                                "new_word" -> Color.Red
                                                "familiar_new_meaning" -> Color.Blue
                                                else -> Color.Black
                                            }
                                        ),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF87CEFA)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        Global.setIsLearning(false)
                    }
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "我知道了",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .height(800.dp)
                .width(500.dp)
                .clip(RoundedCornerShape(32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

        }
    }
}

// 数据类
data class WordItem(
    val word: String,
    val type: String,
    val meaning: String,
    val category: String
)

// 处理AI回复的函数 - 使用Gson处理JSON
private fun processAiResponse(aiResponse: String): List<WordItem>? {
    return try {
        // 移除 <think> 标签及其内容
        var cleanResponse = aiResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()

        // 移除 JSON 数据前后的 ``` 标记
        cleanResponse = cleanResponse.replace(Regex("^```(json)?"), "").replace(Regex("```$"), "").trim()

        // 检查是否是JSON格式
        if (cleanResponse.startsWith("{") || cleanResponse.startsWith("[")) {

            val lastWords = readFromFile("D:/Xiaoye/Learning/LastWords.json")


            if (lastWords != "404" && calculateSimilarity(cleanResponse, lastWords) >= 0.2) {
                println("Similarity: ${calculateSimilarity(cleanResponse, lastWords)}")
                return null
            }

            writeToFile("D:/Xiaoye/Learning/LastWords.json", cleanResponse)

            val gson = Gson()

            if (cleanResponse.startsWith("{")) {
                // 处理对象格式
                val jsonObject = gson.fromJson(cleanResponse, JsonObject::class.java)
                val wordsArray = jsonObject.getAsJsonArray("words") ?: return emptyList()

                val wordList = mutableListOf<WordItem>()
                wordsArray.forEach { element ->
                    val wordObj = element.asJsonObject
                    wordList.add(
                        WordItem(
                            word = wordObj.get("word")?.asString ?: "",
                            type = wordObj.get("type")?.asString ?: "",
                            meaning = wordObj.get("meaning")?.asString ?: "",
                            category = wordObj.get("category")?.asString ?: ""
                        )
                    )
                }
                wordList
            } else {
                // 处理数组格式
                val wordsArray = gson.fromJson(cleanResponse, JsonArray::class.java)
                val wordList = mutableListOf<WordItem>()
                wordsArray.forEach { element ->
                    val wordObj = element.asJsonObject
                    wordList.add(
                        WordItem(
                            word = wordObj.get("word")?.asString ?: "",
                            type = wordObj.get("type")?.asString ?: "",
                            meaning = wordObj.get("meaning")?.asString ?: "",
                            category = wordObj.get("category")?.asString ?: ""
                        )
                    )
                }
                wordList
            }
        } else {
            null // 不是JSON格式
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null // 解析失败
    }
}

// 计算两个JSON字符串的相似度（基于单词集合的重叠度）
private fun calculateSimilarity(json1: String, json2: String): Double {
    return try {
        val gson = Gson()

        // 解析JSON并提取所有单词
        val words1 = extractWordsFromJson(json1, gson)
        val words2 = extractWordsFromJson(json2, gson)

        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        // 计算交集
        val intersection = words1.intersect(words2).size
        // 计算并集
        val union = words1.union(words2).size

        intersection.toDouble() / union.toDouble()
    } catch (e: Exception) {
        0.0 // 解析失败时返回0相似度
    }
}

// 从JSON中提取所有单词
private fun extractWordsFromJson(json: String, gson: Gson): Set<String> {
    return try {
        val words = mutableSetOf<String>()

        if (json.startsWith("{")) {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            val wordsArray = jsonObject.getAsJsonArray("words") ?: return emptySet()

            wordsArray.forEach { element ->
                val wordObj = element.asJsonObject
                wordObj.get("word")?.asString?.let { word ->
                    words.add(word.lowercase(Locale.getDefault()).trim())
                }
            }
        } else {
            val wordsArray = gson.fromJson(json, JsonArray::class.java)
            wordsArray.forEach { element ->
                val wordObj = element.asJsonObject
                wordObj.get("word")?.asString?.let { word ->
                    words.add(word.lowercase(Locale.getDefault()).trim())
                }
            }
        }

        words
    } catch (e: Exception) {
        emptySet()
    }
}

fun takeSilentScreenshotAndRecognize(): Pair<File, String> {
    val ocrHelper = OcrHelper()
    val screenShotDir = File("screenshots/").apply { mkdirs() }
    val outputFile = File(screenShotDir, "silent_screenshot_${System.currentTimeMillis()}.png")

    try {
        val robot = Robot()
        val screenBounds = getAllScreenBounds()

        // 尝试使用高分辨率截图（Java 9+）
        val highResImage = tryGetHighResolutionScreenshot(robot, screenBounds)
            ?: robot.createScreenCapture(screenBounds) // 回退到普通截图

        ImageIO.write(highResImage, "png", outputFile)

        println("高质量全屏截图成功: ${outputFile.absolutePath}")
        println("截图尺寸: ${highResImage.width} x ${highResImage.height}")

        // 截图完成后立即进行 OCR 识别
        println("开始 OCR 识别...")
        val ocrResult = ocrHelper.recognizeImage(outputFile)
        println("OCR 识别完成")

        return Pair(outputFile, ocrResult)
    } catch (e: Exception) {
        println("截图或识别失败: ${e.message}")
        throw e
    }
}

/**
 * 获取所有屏幕的边界（支持多显示器）
 */
private fun getAllScreenBounds(): Rectangle {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val screens = ge.screenDevices

    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0

    for (screen in screens) {
        val screenBounds = screen.defaultConfiguration.bounds
        // 找到最小X和Y
        if (screenBounds.x < minX) minX = screenBounds.x
        if (screenBounds.y < minY) minY = screenBounds.y
        // 找到最大X+width和Y+height
        val currentMaxX = screenBounds.x + screenBounds.width
        val currentMaxY = screenBounds.y + screenBounds.height
        if (currentMaxX > maxX) maxX = currentMaxX
        if (currentMaxY > maxY) maxY = currentMaxY
    }

    return Rectangle(minX, minY, maxX - minX, maxY - minY)
}

/**
 * 尝试获取高分辨率截图（Java 9+ 特性）
 */
private fun tryGetHighResolutionScreenshot(robot: Robot, screenBounds: Rectangle): BufferedImage? {
    return try {
        // 使用反射调用 createMultiResolutionScreenCapture（Java 9+）
        val method: Method = robot.javaClass.getMethod("createMultiResolutionScreenCapture", Rectangle::class.java)
        val multiResolutionImage = method.invoke(robot, screenBounds)

        // 获取分辨率变体列表
        val getVariantsMethod = multiResolutionImage.javaClass.getMethod("getResolutionVariants")
        val variants = getVariantsMethod.invoke(multiResolutionImage) as List<*>

        // 选择最高分辨率的图像
        var highestResImage: BufferedImage? = null
        var maxPixels = 0

        for (variant in variants) {
            if (variant is BufferedImage) {
                val pixels = variant.width * variant.height
                if (pixels > maxPixels) {
                    maxPixels = pixels
                    highestResImage = variant
                }
            }
        }

        highestResImage ?: variants.firstOrNull() as? BufferedImage
    } catch (e: Exception) {
        println("高分辨率截图不可用，使用普通截图: ${e.message}")
        null
    }
}