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
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.awt.*
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.exitProcess

object Global {
    const val VERSION = 15
    const val CLASS = 3
    var accentColorMain = 0xFFFF5733.toInt()
    var accentColorFloating = 0xFFFF5733.toInt()
    var accentColorNamed = 0xFFFF5733.toInt()
    var isRandomColor = false
    var url = ""
    var downloadUrl = ""
    var isOpen = ""
    var timeApi = ""
    var countdownName = ""
    var countdownTime = ""

    private var studentList: List<Student> = emptyList()

    fun updateStudentListFromJson(json: String) {
        studentList = parseStudentJson(json)
    }

    var subjectList: Map<String, DailySchedule> = emptyMap()

    fun updateSubjectListFromJson(json: String) {
        println("Parsing the subjectList")
        subjectList = parseSubjectJson(json)
        println("subjectList: $subjectList")
    }

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean>
        get() = _isInternetAvailable

    fun setIsInternetAvailable(value: Boolean) {
        _isInternetAvailable.value = value
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean>
        get() = _isLoading

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    private val _buttonState = MutableStateFlow("点名")
    val buttonState: StateFlow<String>
        get() = _buttonState

    fun setButtonState(value: String) {
        _buttonState.value = value
    }

    private val _isChangeFace = MutableStateFlow(false)
    val isChangeFace: StateFlow<Boolean>
        get() = _isChangeFace

    fun setIsChangeFace(value: Boolean) {
        _isChangeFace.value = value
    }

    private val _isVoiceIdentify = MutableStateFlow(false)
    val isVoiceIdentify: StateFlow<Boolean>
        get() = _isVoiceIdentify

    fun setIsVoiceIdentify(value: Boolean) {
        _isVoiceIdentify.value = value
    }

    private val _isCountDownDayOpen = MutableStateFlow(false)
    val isCountDownDayOpen: StateFlow<Boolean>
        get() = _isCountDownDayOpen

    fun setIsCountDownDayOpen(value: Boolean) {
        _isCountDownDayOpen.value = value
    }

    private val _isTime = MutableStateFlow(false)
    val isTime: StateFlow<Boolean>
        get() = _isTime

    fun setIsTime(value: Boolean) {
        _isTime.value = value
    }

    private val _date = MutableStateFlow("无")
    val date: StateFlow<String>
        get() = _date

    fun setDate(value: String) {
        _date.value = value
    }

    private val _week = MutableStateFlow("无")
    val week: StateFlow<String>
        get() = _week

    fun setWeek(value: String) {
        _week.value = value
    }

    private val _time = MutableStateFlow("无")
    val time: StateFlow<String>
        get() = _time

    fun setTime(value: String) {
        _time.value = value
    }

    private val _luckyGuy = MutableStateFlow("无")
    val luckyGuy: StateFlow<String>
        get() = _luckyGuy

    fun setLuckyGuy(value: String) {
        _luckyGuy.value = value
    }

    private val _isLongPressed = MutableStateFlow(false)
    val isLongPressed: StateFlow<Boolean>
        get() = _isLongPressed

    fun setIsLongPressed(value: Boolean) {
        _isLongPressed.value = value
    }

    private val _isEasterEgg = MutableStateFlow(false)
    val isEasterEgg: StateFlow<Boolean>
        get() = _isEasterEgg

    fun setIsEasterEgg(value: Boolean) {
        _isEasterEgg.value = value
    }

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean>
        get() = _isDragging

    fun setIsDragging(value: Boolean) {
        _isDragging.value = value
    }

    private val _countDownType = MutableStateFlow(0)
    val countDownType: StateFlow<Int>
        get() = _countDownType

    fun setCountDownType(value: Int) {
        _countDownType.value = value
    }

    private val _isCountDownOpen = MutableStateFlow(false)
    val isCountDownOpen: StateFlow<Boolean>
        get() = _isCountDownOpen

    fun setIsCountDownOpen(value: Boolean) {
        _isCountDownOpen.value = value
    }

    private val _isWallpaper = MutableStateFlow(false)
    val isWallpaper: StateFlow<Boolean>
        get() = _isWallpaper

    fun setIsWallpaper(value: Boolean) {
        _isWallpaper.value = value
    }

    private val _isDeleteWallpaper = MutableStateFlow(false)
    val isDeleteWallpaper: StateFlow<Boolean>
        get() = _isDeleteWallpaper

    fun setIsDeleteWallpaper(value: Boolean) {
        _isDeleteWallpaper.value = value
    }


    private val recentStudents = ArrayDeque<Student>()
    private const val MAX_RECENT_STUDENTS = 30

    fun getRandomStudent(): Pair<String, String>? {
        println("Executing a random student selection!")

        if (studentList.isEmpty()) return null

        // 计算总权重
        val totalWeight = studentList.sumOf { it.probability }

        // 为选择学生创建一个临时列表，过滤掉历史记录中的学生
        val availableStudents = studentList.filter { !recentStudents.contains(it) }

        // 如果可用学生列表为空，则清空历史记录并重新尝试选择
        if (availableStudents.isEmpty()) {
            recentStudents.clear() // 清空历史记录
            return getRandomStudent() // 重新尝试选择
        }

        // 打乱可用学生的顺序，使得选择更加随机
        val shuffledAvailableStudents = availableStudents.shuffled()

        // 使用权重进行随机选择
        val randomWeight = Random.nextInt(0, totalWeight)
        var cumulativeWeight = 0
        var selectedStudent: Student? = null

        for (student in shuffledAvailableStudents) {
            cumulativeWeight += student.probability
            if (randomWeight < cumulativeWeight) {
                selectedStudent = student
                break
            }
        }

        // 如果选中的学生无效，则重新尝试选择
        while (selectedStudent == null || selectedStudent.name.isEmpty()) {
            return getRandomStudent() // 重新尝试选择
        }

        // 将选中的学生加入最近选择的记录中
        recentStudents.add(selectedStudent)
        if (recentStudents.size > MAX_RECENT_STUDENTS) {
            recentStudents.removeFirst() // 保持历史记录的最大长度
        }

        // 分离学生名字的前后部分
        val name = selectedStudent.name
        val firstPart = name.firstOrNull()?.toString() ?: ""
        val secondPart = name.drop(1)

        return Pair(firstPart, secondPart)
    }


}

fun isValidJson(jsonString: String): Boolean {
    return try {
        // 使用 Gson 尝试解析数据
        Gson().fromJson(jsonString, Any::class.java)
        true  // 如果没有抛出异常，说明是有效的 JSON
    } catch (_: JsonSyntaxException) {
        false  // 如果发生异常，说明不是有效的 JSON
    }
}

fun main() = application {
    val isInternetAvailable = Global.isInternetAvailable.collectAsState()
    val buttonState = Global.buttonState.collectAsState()
    val isLoading = Global.isLoading.collectAsState()
    val isVoiceIdentify = Global.isVoiceIdentify.collectAsState()
    val isLongPressed = Global.isLongPressed.collectAsState()
    val isEasterEgg = Global.isEasterEgg.collectAsState()
    var isRun by remember { mutableStateOf(false) }
    var mainWindowVisible by remember { mutableStateOf(true) }
    var floatingWindowVisible by remember { mutableStateOf(false) }
    var isReadyVisible by remember { mutableStateOf(false) }
    var isFirstNameVisible by remember { mutableStateOf(false) }
    var isLastNameVisible by remember { mutableStateOf(false) }
    var randomCounter by remember { mutableStateOf(0) }

    var isFirstInitialized by remember { mutableStateOf(false) }

    val selectedStudent = remember(randomCounter) {
        if (randomCounter % 2 == 0 && isFirstInitialized) {
            Global.getRandomStudent()
        } else {
            isFirstInitialized = true
            null
        }
    }

    var isOpenHtml by remember { mutableStateOf(false) }
    val targetDir = File("D:/")
    val testDir = File("D:/vosk-model-small-cn-0.22")

    var isModelExists by remember { mutableStateOf(false) }
    var jsonData by remember { mutableStateOf("无") }
    var subjectData by remember { mutableStateOf("无") }

    val isTime = Global.isTime.collectAsState()

    val isCountDownDayOpen = Global.isCountDownDayOpen.collectAsState()
    val isCountDownOpen = Global.isCountDownOpen.collectAsState()

    val week = Global.week.collectAsState()
    val time = Global.time.collectAsState()
    val date = Global.date.collectAsState()
    val luckyGuy = Global.luckyGuy.collectAsState()

    var countdownName by remember { mutableStateOf("无") }
    var countdownTime by remember { mutableStateOf("无") }
    val jsonCountDownNameFilePath = "D:/Xiaoye/CountDownName.json"
    val jsonCountDownTimeFilePath = "D:/Xiaoye/CountDownTime.json"

    var operating by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {

            while (true) {
                operating++
                if (readFromFile("D:/Xiaoye/Updating") == "The System is updating...") {
                    exitProcess(0)
                }
                writeToFile("D:/Xiaoye/Version", Global.VERSION.toString())
                writeToFile("D:/Xiaoye/Operating", operating.toString())
                delay(1000)
            }
        }
    }

    var lastTrueTimestamp = 0L

    LaunchedEffect(isInternetAvailable.value) {
        withContext(Dispatchers.IO) {
            while (isInternetAvailable.value) {
                if (Global.url != "No Wifi" && Global.url.contains("http")) {
                    Global.setIsVoiceIdentify(getIsVoiceIdentifyOpen().toBoolean()) // 获取是否开启语音识别
                    Global.setIsTime(getIsTimeOpen().toBoolean()) // 获取是否开启时间提醒
                    Global.setIsCountDownDayOpen(getCountDownDaySwitch().toBoolean()) // 获取是否开启倒数日
                    Global.setIsCountDownOpen(getCountDownSwitch().toBoolean()) // 获取是否开启倒计时
                    Global.setIsWallpaper(getWallpaperSwitch().toBoolean()) // 获取是否开启动态壁纸
                    Global.setIsDeleteWallpaper(getDeleteWallpaperSwitch().toBoolean()) // 获取是否删除动态壁纸
                    val currentTimestamp = System.currentTimeMillis()

                    if (currentTimestamp - lastTrueTimestamp > 100000) {
                        if (getEasterEggSwitch().toBooleanStrictOrNull() == true) {
                            Global.setIsEasterEgg(true)
                            lastTrueTimestamp = currentTimestamp
                        } else {
                            Global.setIsEasterEgg(false)
                        }
                    }

                    if (getLuckyGuy().contains("|")) {
                        try {
                            // 将字符串按 `|` 分隔
                            val items = getLuckyGuy().split("|")

                            // 使用 Gson 将拆分后的数据转换为 JSON
                            val gson = Gson()
                            val luckyguyJson = gson.toJson(items)

                            Global.setLuckyGuy(luckyguyJson)

                            // 写入文件
                            writeToFile("D:/Xiaoye/LuckyGuy.json", luckyguyJson)

                        } catch (e: Exception) {
                            // 如果出现异常，打印错误信息并跳过
                            println("转换失败：${e.message}")
                        }
                    }
                    if (isTime.value && Global.timeApi != "" && Global.timeApi.contains("http")) {
                        parseTimeJsonResponse(getTimeData()).weekday.let {
                            Global.setWeek(it)
                        }
                        parseTimeJsonResponse(getTimeData()).date.let {
                            it.substring(11, 16).let { timeString ->
                                Global.setTime(timeString)
                            }
                        }
                        println(date.value)
                        println(week.value)
                        println(time.value)
                    }

                    if (isCountDownDayOpen.value && Global.timeApi != "" && Global.timeApi.contains("http")) {
                        parseTimeJsonResponse(getTimeData()).date.let { rawDate ->
                            rawDate.split(" ").getOrNull(0)?.let { dateString ->
                                Global.setDate(dateString)
                            }
                        }
                    }
                }

                delay(3000)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (Global.url != "" && Global.url.contains("http")) {
                println("has accessed the 'getOnline' function")
                getOnline()
            }
            delay(10000)
        }
    }

    LaunchedEffect(isInternetAvailable.value) {
        withContext(Dispatchers.IO) {
            while (isInternetAvailable.value) {
                println("second to get data")
                Global.url = getUrl() // 获取全局域名前缀
                Global.timeApi = getTimeApi() // 获取时间接口
                Global.countdownName = getCountDownName() // 获取倒计时名字
                Global.countdownTime = getCountDownTime() // 获取倒计时时间
                Global.setIsVoiceIdentify(getIsVoiceIdentifyOpen().toBooleanStrictOrNull() == true) // 获取是否开启语音识别
                Global.setIsTime(getIsTimeOpen().toBooleanStrictOrNull() == true) // 获取是否开启时间提醒
                Global.downloadUrl = getDownloadUrl() // 获取模型下载链接
                Global.isOpen = getIsOpen() // 获取程序开关
                if (Global.url != "No Wifi" && Global.url.contains("http")) {
                    countdownName = readFromFile(jsonCountDownNameFilePath)
                    countdownTime = readFromFile(jsonCountDownTimeFilePath)
                    if (countdownName != "无") {
                        Global.countdownName = countdownName
                    }
                    if (countdownTime != "无") {
                        Global.countdownTime = countdownTime
                    }
                    jsonData = getNameList() // 获取名单列表
                    subjectData = getSubjectList() // 获取课表
                    if (jsonData != "无") {
                        Global.updateStudentListFromJson(jsonData)
                    }
                    if (subjectData != "无") {
                        Global.updateSubjectListFromJson(subjectData)
                    }
                }
                if (Global.downloadUrl != "No Wifi" && Global.downloadUrl.contains("http")) {
                    isModelExists = checkAndCopyModel(Global.downloadUrl, targetDir, testDir) // 检测模型文件是否存在
                    break
                }

                checkAndCopyModel("http://xyc.okc.today/LAVF.zip", File("D:/Xiaoye"), File("D:/Xiaoye/LAVF"))

                if (readFromFile("D:/Xiaoye/LuckyGuy.json") != "404") {
                    if (isValidJson(readFromFile("D:/Xiaoye/LuckyGuy.json"))) {
                        Global.setLuckyGuy(readFromFile("D:/Xiaoye/LuckyGuy.json"))
                    }
                }

                delay(3000)
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        isRun = true
    }

    LaunchedEffect(isLoading.value) {
        if (!isLoading.value) {
            mainWindowVisible = false
            floatingWindowVisible = true
        }
    }

    var isLongPressedAnimation by remember { mutableStateOf(false) }
    val driveIsLongPressed = remember { mutableStateOf(false) }
    var isFirstDriveIsLongPressed by remember { mutableStateOf(true) }
    var isGetStudent by remember { mutableStateOf(false) }

    LaunchedEffect(isLongPressed.value) {
        if (isLongPressed.value) {
            isLongPressedAnimation = true
            println("Has executed the 'driveIsLongPressed.value = true' LaunchedEffect!")
            delay(1000)
            driveIsLongPressed.value = true
            isLongPressedAnimation = false
        }
    }

    LaunchedEffect(driveIsLongPressed.value) {
        if (driveIsLongPressed.value && !isFirstDriveIsLongPressed) {
            println("Has executed the 'driveIsLongPressed' LaunchedEffect!")
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

    LaunchedEffect(buttonState.value) {
        randomCounter += 1
        if (floatingWindowVisible && buttonState.value == "关闭") {
            isReadyVisible = true
            delay(500)
            isLastNameVisible = true
            isOpenHtml = false
            delay(1500)
            isFirstNameVisible = true
            delay(2000)
            floatingWindowVisible = true
            Global.setButtonState("点名")
        } else {
            isReadyVisible = false
            isLastNameVisible = false
            isFirstNameVisible = false
        }
    }


    var result1 by remember { mutableStateOf("") }

    LaunchedEffect(isVoiceIdentify.value) {
        if (floatingWindowVisible && isVoiceIdentify.value) {
            // 启动协程异步获取语音识别结果
            voice { result ->
                result1 = result
                println("Result: $result")
                if (result != "没有可用的识别结果" && result != "") {
                    if (Regex("点.*名").containsMatchIn(result) ||
                        Regex("抽.*人").containsMatchIn(result) ||
                        Regex("选.*人").containsMatchIn(result) ||
                        Regex("选.*同学").containsMatchIn(result) ||
                        Regex("抽.*同学").containsMatchIn(result) ||
                        Regex("叫.*人").containsMatchIn(result) ||
                        Regex("叫.*同学").containsMatchIn(result) ||
                        Regex("挑.*人").containsMatchIn(result) ||
                        Regex("挑.*同学").containsMatchIn(result) ||
                        Regex("随便.*点").containsMatchIn(result) ||
                        Regex("随便.*抽").containsMatchIn(result) ||
                        Regex("随机.*选").containsMatchIn(result) ||
                        Regex("随机.*抽").containsMatchIn(result) ||
                        Regex("请.*发言").containsMatchIn(result) ||
                        Regex("叫.*回答").containsMatchIn(result) ||
                        Regex("点.*同学").containsMatchIn(result)
                    ) {

                        floatingWindowVisible = true
                        Global.setButtonState("关闭")
                    }/* else {
                    sendRequestAsync(result) { response ->
                        if (response != null && response == "true") {
                            floatingWindowVisible = true
                            Global.setButtonState("关闭")
                        }
                    }
                }*/
                }
            }
        }
    }


    if (!Global.isRandomColor) {
        // 颜色组合列表
        val colorSchemes = listOf(
            // 启动页面深色背景-悬浮窗浅色背景-点名页面明亮背景
            Triple(0xFF37474F.toInt(), 0xFFCFD8DC.toInt(), 0xFFFFF59D.toInt()), // 组合1: 深灰 - 浅蓝灰 - 亮黄
            Triple(0xFF263238.toInt(), 0xFFB0BEC5.toInt(), 0xFFFFCC80.toInt()), // 组合2: 深蓝灰 - 浅灰 - 浅橙
            Triple(0xFF004D40.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFF176.toInt()), // 组合3: 墨绿 - 浅绿 - 浅黄
            Triple(0xFF1B5E20.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt()), // 组合4: 深绿 - 浅绿 - 浅黄
            Triple(0xFF3E2723.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFA726.toInt()), // 组合5: 深褐 - 浅褐 - 亮橙

            // 启动页面深色背景-悬浮窗浅色背景-点名页面明亮背景
            Triple(0xFF01579B.toInt(), 0xFFBBDEFB.toInt(), 0xFFFFAB91.toInt()), // 组合6: 深蓝 - 浅蓝 - 浅红
            Triple(0xFF4E342E.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFCDD2.toInt()), // 组合7: 棕色 - 浅灰褐 - 粉色
            Triple(0xFF1565C0.toInt(), 0xFFB3E5FC.toInt(), 0xFFFF8A65.toInt()), // 组合8: 深蓝 - 浅蓝 - 浅橙
            Triple(0xFF283593.toInt(), 0xFFC5CAE9.toInt(), 0xFFFFCCBC.toInt()), // 组合9: 深蓝 - 浅蓝 - 橙粉
            Triple(0xFF006064.toInt(), 0xFFB2EBF2.toInt(), 0xFFFFE082.toInt()), // 组合10: 深青 - 浅青 - 浅黄

            // 启动页面中性背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF5D4037.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFECB3.toInt()), // 组合11: 棕色 - 浅褐 - 柔黄
            Triple(0xFF3F51B5.toInt(), 0xFFE8EAF6.toInt(), 0xFFFFAB91.toInt()), // 组合12: 蓝色 - 浅蓝 - 橙红
            Triple(0xFF303F9F.toInt(), 0xFFE3F2FD.toInt(), 0xFFFFF59D.toInt()), // 组合13: 深蓝 - 浅蓝 - 亮黄
            Triple(0xFF1976D2.toInt(), 0xFFBBDEFB.toInt(), 0xFFFFCDD2.toInt()), // 组合14: 蓝色 - 浅蓝 - 浅粉
            Triple(0xFF0288D1.toInt(), 0xFFB3E5FC.toInt(), 0xFFFF8A65.toInt()), // 组合15: 蓝色 - 浅蓝 - 浅橙

            // 启动页面中性背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF512DA8.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFF176.toInt()), // 组合16: 紫色 - 浅紫 - 浅黄
            Triple(0xFF673AB7.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFECB3.toInt()), // 组合17: 紫色 - 浅粉紫 - 柔黄
            Triple(0xFF7B1FA2.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFCC80.toInt()), // 组合18: 紫红 - 浅粉 - 浅橙
            Triple(0xFF880E4F.toInt(), 0xFFFCE4EC.toInt(), 0xFFFFAB91.toInt()), // 组合19: 玫红 - 浅粉 - 橙红
            Triple(0xFFAD1457.toInt(), 0xFFF48FB1.toInt(), 0xFFFFF59D.toInt()), // 组合20: 深玫红 - 粉红 - 浅黄

            // 启动页面深色背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF0D47A1.toInt(), 0xFF82B1FF.toInt(), 0xFFFFF176.toInt()), // 组合21: 深蓝 - 浅蓝 - 浅黄
            Triple(0xFF01579B.toInt(), 0xFF4FC3F7.toInt(), 0xFFFFF59D.toInt()), // 组合22: 深青蓝 - 浅青 - 浅黄
            Triple(0xFF00695C.toInt(), 0xFF4DB6AC.toInt(), 0xFFFFEB3B.toInt()), // 组合23: 墨绿 - 浅绿 - 亮黄
            Triple(0xFF1B5E20.toInt(), 0xFF66BB6A.toInt(), 0xFFFFCC80.toInt()), // 组合24: 深绿 - 浅绿 - 浅橙
            Triple(0xFF33691E.toInt(), 0xFF9CCC65.toInt(), 0xFFFFAB91.toInt()), // 组合25: 深橄榄绿 - 浅黄绿 - 浅红

            // 启动页面中性背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF827717.toInt(), 0xFFDCE775.toInt(), 0xFFFFF176.toInt()), // 组合26: 橄榄 - 浅黄绿 - 浅黄
            Triple(0xFFF57F17.toInt(), 0xFFFFF59D.toInt(), 0xFFFFA726.toInt()), // 组合27: 橙黄 - 浅黄 - 橙色
            Triple(0xFFBF360C.toInt(), 0xFFFFCCBC.toInt(), 0xFFFF8A65.toInt()), // 组合28: 深橙 - 浅橙粉 - 浅橙
            Triple(0xFF4A148C.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCCBC.toInt()), // 组合29: 紫色 - 浅紫 - 橙粉
            Triple(0xFF006064.toInt(), 0xFFB2EBF2.toInt(), 0xFFFFA726.toInt()), // 组合30: 墨青 - 浅青 - 橙色

            // 启动页面深色背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFFD84315.toInt(), 0xFFFFCCBC.toInt(), 0xFFFFAB91.toInt()), // 组合31: 深橙红 - 浅橙粉 - 浅红
            Triple(0xFFBF360C.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFAB91.toInt()), // 组合32: 深橙 - 浅黄 - 浅红
            Triple(0xFF1A237E.toInt(), 0xFFC5CAE9.toInt(), 0xFFFFF176.toInt()), // 组合33: 深蓝 - 浅蓝 - 浅黄
            Triple(0xFF311B92.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFECB3.toInt()), // 组合34: 深紫 - 浅紫 - 柔黄
            Triple(0xFF004D40.toInt(), 0xFF80CBC4.toInt(), 0xFFFFCC80.toInt()), // 组合35: 深绿 - 浅绿 - 浅橙

            // 启动页面中性背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF880E4F.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFEB3B.toInt()), // 组合36: 玫红 - 浅粉 - 亮黄
            Triple(0xFFAD1457.toInt(), 0xFFF48FB1.toInt(), 0xFFFFA726.toInt()), // 组合37: 深玫红 - 粉红 - 橙色
            Triple(0xFF4A148C.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFAB91.toInt()), // 组合38: 深紫 - 浅紫 - 浅红
            Triple(0xFFD81B60.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFF59D.toInt()), // 组合39: 深玫红 - 浅粉 - 浅黄
            Triple(0xFF8E24AA.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCC80.toInt()), // 组合40: 紫红 - 浅紫 - 浅橙

            // 启动页面深色背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF4E342E.toInt(), 0xFFD7CCC8.toInt(), 0xFFFFA726.toInt()), // 组合41: 棕色 - 浅灰褐 - 亮橙
            Triple(0xFF01579B.toInt(), 0xFFB3E5FC.toInt(), 0xFFFFF176.toInt()), // 组合42: 深青 - 浅蓝 - 浅黄
            Triple(0xFF2E7D32.toInt(), 0xFFA5D6A7.toInt(), 0xFFFFEB3B.toInt()), // 组合43: 绿 - 浅绿 - 亮黄
            Triple(0xFF558B2F.toInt(), 0xFFDCEDC8.toInt(), 0xFFFFF9C4.toInt()), // 组合44: 橄榄绿 - 浅黄绿 - 浅黄
            Triple(0xFF6A1B9A.toInt(), 0xFFE1BEE7.toInt(), 0xFFFFCC80.toInt()), // 组合45: 深紫 - 浅紫 - 浅橙

            // 启动页面中性背景-悬浮窗浅色背景-点名页面亮色背景
            Triple(0xFF880E4F.toInt(), 0xFFF8BBD0.toInt(), 0xFFFFA726.toInt()), // 组合46: 玫红 - 浅粉 - 橙色
            Triple(0xFFC2185B.toInt(), 0xFFF48FB1.toInt(), 0xFFFF8A65.toInt()), // 组合47: 深玫红 - 粉红 - 浅橙
            Triple(0xFF009688.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFF176.toInt()), // 组合48: 青 - 浅青 - 浅黄
            Triple(0xFF00796B.toInt(), 0xFFB2DFDB.toInt(), 0xFFFFCCBC.toInt()), // 组合49: 墨绿 - 浅青 - 橙粉
            Triple(0xFFD32F2F.toInt(), 0xFFFFCDD2.toInt(), 0xFFFFA726.toInt())  // 组合50: 深红 - 浅红 - 亮橙
        )

        val randomIndex = Random.nextInt(colorSchemes.size) // 随机选择索引
        val selectedScheme = colorSchemes[randomIndex]      // 选择颜色组合

        Global.accentColorMain = selectedScheme.first     // 启动页面背景色
        Global.accentColorFloating = selectedScheme.second // 悬浮窗颜色
        Global.accentColorNamed = selectedScheme.third    // 点名界面背景色

        Global.isRandomColor = true
    }

    if (isLongPressed.value) {

        Window(
            onCloseRequest = ::exitApplication,
            title = "点名程序",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(400.dp, 400.dp)
            ),
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false
        ) {

            val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
            val awtWindow = this.window

            awtWindow.iconImage = icon  // 设置窗口的图标

            AnimatedVisibility(
                visible = isLongPressedAnimation,
                enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 500
                    )
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 300
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("images/pane.png"),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .size(400.dp)
                    )
                    Text(
                        "3",
                        fontSize = 300.sp,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            }

        }

    }

    // 彩蛋
    if (isEasterEgg.value) {
        easterEgg()
    }

    if (mainWindowVisible && !isEasterEgg.value) {

        Window(
            onCloseRequest = ::exitApplication,
            title = "点名程序",
            state = WindowState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(1000.dp, 700.dp)
            ),
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
        ) {

            LaunchedEffect(isLoading.value) {
                if (!isLoading.value) {
                    window.extendedState = Frame.ICONIFIED
                }
            }

            val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
            val awtWindow = this.window

            awtWindow.iconImage = icon  // 设置窗口的图标

            AnimatedVisibility(
                visible = isRun,
                enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 500
                    )
                ),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 300
                    )
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            shape = RoundedCornerShape(16.dp)
                            clip = true
                        },
                    color = Color(Global.accentColorMain)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        title()
                    }
                }
            }
        }
    }

    val isChangeFace = Global.isChangeFace.collectAsState()

    var isRunCountdownDay by remember { mutableStateOf(false) }

    if (floatingWindowVisible && !isEasterEgg.value) {

        if (date.value != "无" && isCountDownDayOpen.value && Global.countdownName != "无" && Global.countdownTime != "无") {

            LaunchedEffect(Unit) {
                delay(500)
                isRunCountdownDay = true
            }


            Window(
                onCloseRequest = { },
                title = "倒数日",
                state = WindowState(
                    position = WindowPosition.Aligned(Alignment.TopCenter),
                    size = DpSize(400.dp, 50.dp)
                ),
                undecorated = true,
                transparent = true,
                alwaysOnTop = true,
                resizable = false
            ) {
                val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
                val awtWindow = this.window

                awtWindow.iconImage = icon  // 设置窗口的图标

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
                    visible = isRunCountdownDay,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 300
                        )
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                                alpha = 0.7f
                            },
                        color = Color(Global.accentColorFloating)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            countdownDay()
                        }
                    }
                }
            }
        } else {
            isRunCountdownDay = false
        }

        val countDownType = Global.countDownType.collectAsState()
        var isRunCountdown by remember { mutableStateOf(false) }

        if (isCountDownOpen.value && countDownType.value != 0) {

            LaunchedEffect(Unit) {
                delay(500)
                isRunCountdown = true
            }


            Window(
                onCloseRequest = { },
                title = "倒计时",
                state = WindowState(
                    position = WindowPosition.Aligned(Alignment.Center),
                    size = DpSize(500.dp, 300.dp)
                ),
                undecorated = true,
                transparent = true,
                alwaysOnTop = true,
                resizable = false
            ) {
                val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
                val awtWindow = this.window

                awtWindow.iconImage = icon  // 设置窗口的图标

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
                    visible = isRunCountdown,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 300
                        )
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            },
                        color = Color(Global.accentColorFloating)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            countdown()
                        }
                    }
                }
            }
        } else {
            isRunCountdown = false
        }


        val isDragging = Global.isDragging.collectAsState()

        if (isCountDownOpen.value && countDownType.value == 0 && isDragging.value) {

            LaunchedEffect(Unit) {
                delay(500)
                isRunCountdown = true
            }


            Window(
                onCloseRequest = { },
                title = "更多选项",
                state = WindowState(
                    position = WindowPosition.Aligned(Alignment.CenterEnd),
                    size = DpSize(300.dp, 600.dp)
                ),
                undecorated = true,
                transparent = true,
                alwaysOnTop = true,
                resizable = false
            ) {
                val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
                val awtWindow = this.window

                awtWindow.iconImage = icon  // 设置窗口的图标

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
                    visible = isRunCountdown,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 500
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = 300
                        )
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                                alpha = 0.7f
                            },
                        color = Color(Global.accentColorFloating)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            moreFunction()
                        }
                    }
                }
            }
        } else {
            isRunCountdown = false
        }

        fun getScreenLimits(): Pair<Int, Int> {
            // 获取屏幕的尺寸
            val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize

            // 获取屏幕的宽度和高度
            val screenWidth = screenSize.width
            val screenHeight = screenSize.height

            return Pair(screenWidth, screenHeight)
        }

        fun checkWindowPosition(location: Point) {
            if (isCountDownOpen.value && countDownType.value == 0) {

                val (screenWidth, screenHeight) = getScreenLimits()

                println("xLimitMax: $screenWidth, yLimitMax: $screenHeight, deltaX: ${screenWidth * 0.7518} - ${screenWidth * 0.6528} ,deltaY: ${screenHeight * 0.2188} - ${screenHeight * 0.2954}")

                when {
                    location.x.toDouble() in screenWidth * 0.7518..screenWidth * 0.9569 && location.y.toDouble() in screenHeight * 0.2188..screenHeight * 0.2954 -> {
                        Global.setCountDownType(1)
                        println("CountDownType: 1")
                    }

                    location.x.toDouble() in screenWidth * 0.7518..screenWidth * 0.9569 && location.y.toDouble() in screenHeight * 0.3063..screenHeight * 0.4595 -> {
                        Global.setCountDownType(2)
                        println("CountDownType: 2")
                    }

                    location.x.toDouble() in screenWidth * 0.7518..screenWidth * 0.9569 && location.y.toDouble() in screenHeight * 0.4923..screenHeight * 0.6017 -> {
                        Global.setCountDownType(3)
                        println("CountDownType: 3")
                    }

                    location.x.toDouble() in screenWidth * 0.7518..screenWidth * 0.9569 && location.y.toDouble() in screenHeight * 0.6017..screenHeight * 0.7330 -> {
                        Global.setCountDownType(4)
                        println("CountDownType: 4")
                    }

                    else -> {
                        Global.setCountDownType(0)
                        println("CountDownType: 0")
                    }
                }
            }
        }

        Window(
            onCloseRequest = { },
            title = "点名系统",
            state = if (isChangeFace.value) {
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
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false
        ) {
            val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
            val awtWindow = this.window

            awtWindow.iconImage = icon  // 设置窗口的图标

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    while (true) {
                        window.isMinimized = false
                        awtWindow.isAlwaysOnTop = true
                        println("isDragging: ${isDragging.value}")
                        delay(1000)
                    }
                }
            }

            val window = remember { this.window }

            var isDraggingBox by remember { mutableStateOf(false) }
            rememberCoroutineScope()  // 使用 compose 的 CoroutineScope
            var lastMouseLocation by remember { mutableStateOf<Pair<Int, Int>?>(null) }
            var isLongPressedBox by remember { mutableStateOf(false) }  // 长按状态
            var pressJob by remember { mutableStateOf<Job?>(null) }



            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            ) {
                val dragThreshold = 5  // 拖动阈值
                val clickThreshold = 200L  // 点击和拖动的时间差（200ms作为点击和拖动的分界）

                LaunchedEffect(Unit) {
                    var pressTime: Long = 0  // 用来记录按下的时间
                    var isClick = false  // 判断是否是点击操作

                    window.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mousePressed(e: java.awt.event.MouseEvent?) {
                            pressTime = System.currentTimeMillis()  // 记录按下时间
                            isClick = true  // 假设是点击操作

                            // 记录鼠标按下时的位置
                            lastMouseLocation = e?.locationOnScreen?.let { Pair(it.x, it.y) }
                            isDraggingBox = true
                            Global.setIsDragging(true)

                            // 启动协程来检测长按
                            pressJob = CoroutineScope(Dispatchers.IO).launch {
                                delay(1000)  // 等待1秒来判断是否长按
                                if (isDraggingBox && isClick) {  // 如果拖动状态未改变，且是点击操作
                                    isLongPressedBox = true
                                    Global.setIsLongPressed(true)  // 设置长按状态
                                    println("Long press detected!")
                                }
                            }
                        }

                        override fun mouseReleased(e: java.awt.event.MouseEvent?) {
                            val currentTime = System.currentTimeMillis()
                            val clickDuration = currentTime - pressTime

                            // 如果按下和释放的时间差小于点击阈值，认为是点击
                            if (clickDuration < clickThreshold) {
                                if (isClick) {
                                    Global.setButtonState("关闭")
                                    Global.setIsLongPressed(false)  // 确保长按标志被清除
                                    pressJob?.cancel()  // 取消长按协程
                                }
                            }

                            // 鼠标释放时停止拖动
                            isDraggingBox = false
                            lastMouseLocation = null
                            Global.setIsDragging(false)

                            // 取消长按检测协程
                            pressJob?.cancel()

                            // 使窗口在鼠标释放时停留在最终位置
                            e?.locationOnScreen?.let { mouseLocation ->
                                val currentLocation = window.location
                                val newLocation = currentLocation.apply {
                                    x = mouseLocation.x - window.width / 2  // 根据鼠标位置计算新位置
                                    y = mouseLocation.y - window.height / 2  // 根据鼠标位置计算新位置
                                }

                                SwingUtilities.invokeLater {
                                    window.location = newLocation
                                }

                                println("Window moved to: $newLocation")
                                checkWindowPosition(newLocation)
                            }
                        }
                    })

                    window.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
                        override fun mouseDragged(e: java.awt.event.MouseEvent?) {
                            if (isDraggingBox) {
                                // 计算鼠标拖动的偏移量
                                lastMouseLocation?.let { lastLocation ->
                                    val deltaX = e?.locationOnScreen?.x?.minus(lastLocation.first) ?: 0
                                    val deltaY = e?.locationOnScreen?.y?.minus(lastLocation.second) ?: 0

                                    // 判断是否超过了拖动阈值
                                    if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                                        // 如果超出阈值，认为是拖动而非点击，取消长按检测
                                        pressJob?.cancel()
                                        isLongPressedBox = false
                                        Global.setIsLongPressed(false)

                                        // 更新窗口的位置
                                        val currentLocation = window.location
                                        val newLocation = currentLocation.apply {
                                            x += deltaX
                                            y += deltaY
                                        }

                                        SwingUtilities.invokeLater {
                                            window.location = newLocation
                                        }

                                        // 更新上次鼠标的位置
                                        lastMouseLocation = e?.locationOnScreen?.let { Pair(it.x, it.y) }
                                        println("Window moved to: $newLocation")
                                    }
                                }
                            }
                        }
                    })
                }
                // 动态壁纸函数
                videoWallpaper()
                // 点名悬浮窗
                dragWindow()
            }
        }
    }

    var student1 by remember { mutableStateOf("某某某") }
    var student2 by remember { mutableStateOf("某某某") }
    var student3 by remember { mutableStateOf("某某某") }

    if (driveIsLongPressed.value && !isGetStudent) {
        isGetStudent = true
        Global.getRandomStudent()?.let { student1 = it.first + it.second }
        Global.getRandomStudent()?.let { student2 = it.first + it.second }
        Global.getRandomStudent()?.let { student3 = it.first + it.second }
    }

    if (buttonState.value == "关闭" || driveIsLongPressed.value) {

        Global.setCountDownType(0)

        val tips = if (!driveIsLongPressed.value) {
            listOf(
                "我选好了！",
                "你猜猜是谁？",
                "就你了！",
                "幸运儿是",
                "掌声有请",
                "你是全班最靓的仔",
                "让我挑选一个学霸...",
                "我看好你！"
            )
        } else {
            listOf(
                "我选好了！",
                "你猜猜是谁？",
                "就你们了！",
                "幸运儿是",
                "掌声有请",
                "你们是全班最靓的仔",
                "让我挑选一些学霸...",
                "我看好你们！"
            )
        }
        val randomTips = tips.random()

        Window(
            onCloseRequest = ::exitApplication,
            title = "点名系统",
            undecorated = true,  //无边框
            transparent = true,  //透明窗口
            alwaysOnTop = true,  //窗口置顶
            resizable = false
        ) {

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    while (true) {
                        window.isMinimized = false
                        window.isAlwaysOnTop = true
                        delay(1000)
                    }
                }
            }


            LaunchedEffect(window) {
                val screenSize =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
                window.setSize(screenSize.width, screenSize.height)
                window.extendedState = JFrame.MAXIMIZED_BOTH
            }

            LaunchedEffect(isOpenHtml) {
                if (!isOpenHtml && Global.url.contains("http")) {
                    isOpenHtml = true
                    val audioUrl = if (!driveIsLongPressed.value) {
                        selectedStudent?.let { student ->
                            if (student.second == "服马超·王显福") {
                                "${Global.url}/voice.php?text=国服马，超王显福"
                            } else {
                                "${Global.url}/voice.php?text=${student.first}${student.second}"
                            }
                        } ?: ""
                    } else {
                        "${Global.url}/voice.php?text=$student1,$student2,$student3"
                    }
                    if (audioUrl.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            playAudio(audioUrl)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(Global.accentColorNamed)),
                contentAlignment = Alignment.Center
            ) {

                val luckyGuyList: List<String> =
                    Gson().fromJson(luckyGuy.value, object : TypeToken<List<String>>() {}.type)

                val angle by animateFloatAsState(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 3000, delayMillis = 500),
                        repeatMode = RepeatMode.Restart
                    )
                )

                if (!driveIsLongPressed.value) {
                    AnimatedVisibility(
                        visible = isFirstNameVisible,
                        enter = scaleIn(
                            initialScale = 0.1f,
                            animationSpec = tween(durationMillis = 700)
                        ) + fadeIn(animationSpec = tween(durationMillis = 700)) +
                                expandVertically(animationSpec = tween(500)) +
                                slideInHorizontally(animationSpec = tween(500))
                    ) {
                        selectedStudent?.let {
                            if (luckyGuyList.contains(it.second)) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    drawStar(size = 1600f, center = center, rotationAngle = angle)
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(
                        visible = isReadyVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = 1000
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (!driveIsLongPressed.value) {
                                Text(
                                    randomTips,
                                    fontSize = 100.sp,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(start = 200.dp, top = 100.dp)
                                )
                            } else {
                                Text(
                                    randomTips,
                                    fontSize = 100.sp,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(start = 200.dp, top = 50.dp)
                                )
                            }
                        }
                    }

                    if (!driveIsLongPressed.value) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 100.dp)
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = isLastNameVisible,
                                enter = scaleIn(
                                    initialScale = 0.1f,
                                    animationSpec = tween(durationMillis = 700)
                                ) + fadeIn(animationSpec = tween(durationMillis = 700)) +
                                        expandVertically(animationSpec = tween(500)) +
                                        slideInHorizontally(animationSpec = tween(500))
                            ) {
                                // 左侧部分（姓氏）
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(), // 始终占据固定空间
                                    contentAlignment = Alignment.Center
                                ) {

                                    Text(
                                        selectedStudent?.first ?: "",
                                        fontSize = 300.sp,
                                        color = Color.Red,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = isFirstNameVisible,
                                enter = scaleIn(
                                    initialScale = 0.1f,
                                    animationSpec = tween(durationMillis = 700)
                                ) + fadeIn(animationSpec = tween(durationMillis = 700)) +
                                        expandVertically(animationSpec = tween(500)) +
                                        slideInHorizontally(animationSpec = tween(500))
                            ) {
                                // 右侧部分（名字）
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(), // 始终占据固定空间
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        selectedStudent?.second ?: "某某",
                                        fontSize = 300.sp,
                                        color = Color.Red,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        AnimatedVisibility(
                            visible = isFirstNameVisible,
                            enter = scaleIn(
                                initialScale = 0.1f,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700)) +
                                    expandVertically(animationSpec = tween(500)) +
                                    slideInHorizontally(animationSpec = tween(500))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    student1,
                                    fontSize = 150.sp,
                                    color = Color.Red,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = 25.dp)
                                )
                                Text(
                                    student2,
                                    fontSize = 150.sp,
                                    color = Color.Red,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = 25.dp)
                                )
                                Text(
                                    student3,
                                    fontSize = 150.sp,
                                    color = Color.Red,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = 100.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 绘制五角星
fun DrawScope.drawStar(size: Float, center: Offset, rotationAngle: Float) {
    val path = Path()

    val numPoints = 5
    val radius = size / 2
    val angleStep = 2 * PI / numPoints

    for (i in 0 until numPoints) {
        // 外顶点
        val outerAngle = i * angleStep
        val outerX = (center.x + radius * cos(outerAngle)).toFloat()  // 转换为 Float
        val outerY = (center.y + radius * sin(outerAngle)).toFloat()  // 转换为 Float

        if (i == 0) {
            path.moveTo(outerX, outerY)
        } else {
            path.lineTo(outerX, outerY)
        }

        // 内顶点
        val innerAngle = outerAngle + angleStep / 2
        val innerX = (center.x + radius / 2 * cos(innerAngle)).toFloat()  // 转换为 Float
        val innerY = (center.y + radius / 2 * sin(innerAngle)).toFloat()  // 转换为 Float
        path.lineTo(innerX, innerY)
    }
    path.close()

    // 旋转并绘制
    rotate(rotationAngle, pivot = center) {
        drawPath(path = path, color = Color.Yellow)
    }
}

var mediaPlayer: MediaPlayer? = null

fun playAudio(url: String) {
    val isInternetAvailable = Global.isInternetAvailable.value
    val voiceDir = File("D:/Xiaoye/Voice/")
    if (!voiceDir.exists()) voiceDir.mkdirs()

    val fileName = url.hashCode().toString() + ".mp3"
    val localFile = File(voiceDir, fileName)

    if (localFile.exists()) {
        playLocalAudio(localFile.absolutePath)
    } else if (isInternetAvailable) {
        downloadAudio(url, localFile) { success ->
            if (success) playLocalAudio(localFile.absolutePath)
            else println("音频下载失败: $url")
        }
    }
}

fun playLocalAudio(filePath: String) {
    Platform.runLater {
        mediaPlayer?.dispose()
        val media = Media(File(filePath).toURI().toString())
        mediaPlayer = MediaPlayer(media).apply {
            play()
            setOnEndOfMedia {
                dispose()
                mediaPlayer = null
            }
        }
    }
}

fun downloadAudio(url: String, file: File, onComplete: (Boolean) -> Unit) {
    try {
        URI(url).toURL().openStream().use { input ->
            Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        onComplete(true)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(false)
    }
}


/*fun killProcess(processName: String) {
    println("execute kill process")
    try {
        val processBuilder = ProcessBuilder("taskkill", "/F", "/IM", processName)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        // 读取命令执行的输出
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line)
        }

        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}*/
