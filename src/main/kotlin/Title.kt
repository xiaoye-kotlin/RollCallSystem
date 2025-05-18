import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

fun createDirectory(directoryPath: String) {
    val path: Path = Paths.get(directoryPath)
    if (!Files.exists(path)) {
        Files.createDirectories(path)
    }
}

fun writeToFile(filePath: String, content: String) {
    val directoryPath = File(filePath).parent
    if (directoryPath != null) {
        createDirectory(directoryPath)
    }

    val file = File(filePath)
    file.writeText(content)
}

fun readFromFile(filePath: String): String {
    val file = File(filePath)
    return if (file.exists()) {
        file.readText()
    } else {
        "404"
    }
}

fun deleteFileOrDirectory(filePath: String) {
    val file = File(filePath)

    if (file.exists()) {
        if (file.isDirectory) {
            // 如果是目录，递归删除目录中的所有文件
            file.listFiles()?.forEach { deleteFileOrDirectory(it.absolutePath) }
        }
        file.delete() // 删除文件或目录
    } else {
        println("404")
    }
}

@Composable
fun title() {
    /*
    核心数据：
    1、全局域名（用于所有url访问的前段部分） - Global.url
    2、文件下载域名（用于下载语音转文字模型包） - Global.downloadUrl
    3、程序远程开关（用于远控程序） - Global.isOpen
    4、检测是否存在模型文件包 - isModelExists
    5、获取名单列表（用于点名的名单列表） -  jsonData
    */
    var countdown by remember { mutableStateOf(5) }
    var tips by remember { mutableStateOf("") }
    val isLoading = Global.isLoading.collectAsState()
    var jsonData by remember { mutableStateOf("无") }
    var subjectData by remember { mutableStateOf("无") }
    var luckyGuyData by remember { mutableStateOf("无") }
    var countdownName by remember { mutableStateOf("无") }
    var countdownTime by remember { mutableStateOf("无") }
    val isInternetAvailable = Global.isInternetAvailable.collectAsState()
    var isModelExists by remember { mutableStateOf(false) }
    val isVoiceIdentify = Global.isVoiceIdentify.collectAsState()

    val targetDir = File("D:/")
    val testDir = File("D:/vosk-model-small-cn-0.22")

    val jsonNameListFilePath = "D:/Xiaoye/NameList.json"
    val jsonSubjectListFilePath = "D:/Xiaoye/SubjectList.json"
    val jsonCountDownNameFilePath = "D:/Xiaoye/CountDownName.json"
    val jsonCountDownTimeFilePath = "D:/Xiaoye/CountDownTime.json"
    val jsonLuckyGuyFilePath = "D:/Xiaoye/LuckyGuy.json"

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
                    isModelExists = checkAndCopyModel(Global.downloadUrl, targetDir, testDir)
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

                Global.updateStudentListFromJson(jsonData)
                Global.updateSubjectListFromJson(subjectData)
                countdownName = Global.countdownName
                countdownTime = Global.countdownTime
                if (readFromFile("D:/Xiaoye/LuckyGuy.json") != "404") {
                    if (isValidJson(readFromFile("D:/Xiaoye/LuckyGuy.json"))) {
                        Global.setLuckyGuy(readFromFile("D:/Xiaoye/LuckyGuy.json"))
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
                    Global.setIsLoading(false)
                }
            }
        }
    }

    val isOpen = Global.isOpen.collectAsState()

    LaunchedEffect(Unit) {
        while (!isDownloading && isInternetAvailable()) {
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

                if (isInternetAvailable()) {
                    deleteFileOrDirectory("D:/Xiaoye/CountDownName.json")
                    deleteFileOrDirectory("D:/Xiaoye/CountDownTime.json")
                    Global.setIsOpen(getIsOpen().toBoolean())
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
                    Global.url = getUrl()

                    if (tips == "加载必要数据(1/10)") {
                        tips = "加载必要数据(2/10)"
                    }
                    Global.setIsVoiceIdentify(getIsVoiceIdentifyOpen().toBooleanStrictOrNull() == true)

                    if (tips == "加载必要数据(2/10)") {
                        tips = "加载必要数据(3/10)"
                    }
                    Global.downloadUrl = getDownloadUrl()

                    if (tips == "加载必要数据(3/10)") {
                        tips = "加载必要数据(4/10)"
                    }
                    Global.timeApi = getTimeApi()

                    if (tips == "加载必要数据(4/10)") {
                        tips = "加载必要数据(5/10)"
                    }
                    Global.countdownName = getCountDownName()

                    if (tips == "加载必要数据(5/10)") {
                        tips = "加载必要数据(6/10)"
                    }
                    Global.countdownTime = getCountDownTime()

                    if (tips == "加载必要数据(6/10)") {
                        tips = "加载必要数据(7/10)"
                    }
                    println("Student read result: ${readFromFile(jsonNameListFilePath)}")
                    println("Subject read result: ${readFromFile(jsonSubjectListFilePath)}")
                    println("isInternetAvailable: ${isInternetAvailable.value}")
                }

                // 其他逻辑保持不变...
                if (!isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404" &&
                    readFromFile(jsonSubjectListFilePath) != "404" &&
                    readFromFile(jsonLuckyGuyFilePath) != "404"
                ) {
                    println("has been readData")
                    jsonData = readFromFile(jsonNameListFilePath)
                    subjectData = readFromFile(jsonSubjectListFilePath)
                    luckyGuyData = readFromFile(jsonLuckyGuyFilePath)
                    if (jsonData != "无") {
                        Global.updateStudentListFromJson(jsonData)
                        println("Student Data has been written")
                    }
                    if (subjectData != "无") {
                        Global.updateSubjectListFromJson(subjectData)
                        println("Subject Data has been written")
                    }
                    if (luckyGuyData != "无") {
                        Global.setLuckyGuy(luckyGuyData)
                        println("luckyGuy Data has been written")
                    }
                } else if (!isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404") {
                    jsonData = readFromFile(jsonNameListFilePath)
                    if (jsonData != "无") {
                        Global.updateStudentListFromJson(jsonData)
                        println("Student Data has been written")
                    }
                }
            }
            delay(5000)
            if (!isDownloading && tips != "数据不完整,重新尝试...") {
                tips = "数据不完整,重新尝试..."
            }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (isInternetAvailable.value) {
            if ((Global.url != "" && Global.url.contains("http") && Global.timeApi != "" && Global.timeApi.contains(
                    "http"
                ) && Global.downloadUrl != "" && Global.downloadUrl.contains("http") && isOpen.value)
            ) {
                isDownloading = true
            }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        println("Global.url: ${Global.url}, Global.timeApi: ${Global.timeApi}, Global.isOpen: ${Global.isOpen}, isModelExists: $isModelExists, Global.downloadUrl: ${Global.downloadUrl}, isVoiceIdentify: ${isVoiceIdentify.value}")
        if (!isInternetAvailable()) {
            Global.setIsInternetAvailable(false)
            if (readFromFile(jsonNameListFilePath) != "404" || readFromFile(jsonSubjectListFilePath) != "404") {
                tips = "当前无网络连接，即将进入离线模式"
                delay(900)
                Global.setIsLoading(false)
            } else {
                tips = "联网后将自动运行，5秒后最小化程序。"
                delay(4900)
                Global.setIsLoading(false)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                modifier = Modifier,
                onClick = { Global.setIsMinimize(true) }) {
                Icon(
                    modifier = Modifier
                        .size(36.dp),
                    painter = painterResource("/images/minimize.png"),
                    contentDescription = "Minimize"
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MaterialTheme {
                val titleImage: Painter = painterResource("images/welcome.png")
                val iconImage: Painter = painterResource("images/callTheRoll.png")

                Image(
                    painter = iconImage, contentDescription = null,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (tips.contains("加载") || tips.contains("整理")) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF7CFC00).copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                                .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tips,
                                fontSize = 30.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                } else if (tips.isNotEmpty() && tips.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                                .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tips,
                                fontSize = 30.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(133.dp))
                }

                Image(painter = titleImage, contentDescription = "Title")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                modifier = Modifier,
                text = "By Compose Desktop",
                fontSize = 20.sp
            )
        }

    }
}
