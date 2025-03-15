import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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

    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            println("isDownloading is touched")
            withContext(Dispatchers.IO) {
                while (!isModelExists) {
                    isModelExists = checkAndCopyModel(Global.downloadUrl, targetDir, testDir) // 检测模型文件是否存在
                }
                println("isModelExists is touched")
                while (jsonData == "无") {
                    jsonData = getNameList() // 获取名单列表
                    delay(3000)
                }
                while (subjectData == "无") {
                    subjectData = getSubjectList() // 获取课表
                    delay(3000)
                }
                println("Data is touched")
                Global.updateStudentListFromJson(jsonData)
                Global.updateSubjectListFromJson(subjectData)
                Global.setIsInternetAvailable(true)
                countdownName = Global.countdownName
                countdownTime = Global.countdownTime
                if (countdown <= 0 && jsonData != "无") {
                    writeToFile(jsonNameListFilePath, jsonData)
                }
                if (countdown <= 0 && subjectData != "无") {
                    writeToFile(jsonSubjectListFilePath, subjectData)
                    Global.setIsLoading(false)
                    isDownloading = false
                }
                if (countdown <= 0 && countdownName != "无") {
                    writeToFile(jsonCountDownNameFilePath, countdownName)
                }
                if (countdown <= 0 && countdownTime != "无") {
                    writeToFile(jsonCountDownTimeFilePath, countdownTime)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isLoading.value) {
            println("Countdown: $countdown")
            countdown--
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (!isDownloading) {
            withContext(Dispatchers.IO) {
                println("start to get data")
                Global.isOpen = getIsOpen() // 获取程序开关
                Global.url = getUrl() // 获取全局域名前缀
                Global.setIsVoiceIdentify(getIsVoiceIdentifyOpen().toBooleanStrictOrNull() == true) // 获取是否开启语音识别
                Global.downloadUrl = getDownloadUrl() // 获取模型下载链接
                Global.timeApi = getTimeApi() // 获取时间接口
                Global.countdownName = getCountDownName() // 获取倒计时名字
                Global.countdownTime = getCountDownTime() // 获取倒计时时间
                println("Student read result: ${readFromFile(jsonNameListFilePath)}")
                println("Subject read result: ${readFromFile(jsonSubjectListFilePath)}")
                println("isInternetAvailable: ${isInternetAvailable.value}")
                if (!isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404" && readFromFile(
                        jsonSubjectListFilePath
                    ) != "404"
                ) {
                    println("has been readData")
                    jsonData = readFromFile(jsonNameListFilePath)
                    subjectData = readFromFile(jsonSubjectListFilePath)
                    countdownName = readFromFile(jsonCountDownNameFilePath)
                    countdownTime = readFromFile(jsonCountDownTimeFilePath)
                    if (countdownName != "无") {
                        Global.countdownName = countdownName
                    }
                    if (countdownTime != "无") {
                        Global.countdownTime = countdownTime
                    }
                    if (jsonData != "无") {
                        Global.updateStudentListFromJson(jsonData)
                        println("Student Data has been written")
                    }
                    if (subjectData != "无") {
                        Global.updateSubjectListFromJson(subjectData)
                        println("Subject Data has been written")
                    }
                } else if (!isInternetAvailable() && readFromFile(jsonNameListFilePath) != "404") {
                    jsonData = readFromFile(jsonNameListFilePath)
                    Global.updateStudentListFromJson(jsonData)
                    println("Student Data has been written")
                }
                tips = "Loading..."// addToStartup("RollCallSystem", "RollCallSystem.exe") // 添加开机自启动
            }
            delay(2000)
        }
    }

    LaunchedEffect(countdown) {
        println("Global.url: ${Global.url}, Global.timeApi: ${Global.timeApi}, Global.isOpen: ${Global.isOpen}, isModelExists: $isModelExists, Global.downloadUrl: ${Global.downloadUrl}, isVoiceIdentify: ${isVoiceIdentify.value}")
        if (countdown < 0 && (Global.url != "" && Global.url.contains("http") && Global.timeApi != "" && Global.timeApi.contains("http") && Global.downloadUrl != "" && Global.downloadUrl.contains("http") && Global.isOpen == "true"))
        {
            isDownloading = true
        } else if (!isInternetAvailable()) {
            if (readFromFile(jsonNameListFilePath) != "404" || readFromFile(jsonSubjectListFilePath) != "404") {
                tips = "当前无网络连接或无法上网，即将进入离线模式"
                delay(900)
                Global.setIsLoading(false)
            } else {
                tips = "联网后将自动运行，5秒后最小化程序。"
            }
            Global.setIsInternetAvailable(false)
        }
    }

    // 动态壁纸函数
    videoWallpaper("D:\\Xiaoye\\wallpaper.mp4")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (Global.isOpen == "false") {
                Text(
                    modifier = Modifier
                        .padding(20.dp),
                    text = "Error",
                    fontSize = 30.sp
                )
            } else if (!isInternetAvailable.value) {
                Text(
                    modifier = Modifier
                        .padding(20.dp),
                    text = "No Wifi",
                    fontSize = 30.sp
                )
            } else if (countdown <= 0) {
                Text(
                    modifier = Modifier
                        .padding(20.dp),
                    text = "启动中...",
                    fontSize = 30.sp
                )
            } else {
                Text(
                    modifier = Modifier
                        .padding(20.dp),
                    text = "${countdown}s",
                    fontSize = 30.sp
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

                if (Global.isOpen == "false") {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(20.dp),
                            color = Color.Red,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(10.dp),
                                text = "程序已停止使用",
                                fontSize = 40.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (tips.contains("Loading...")) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(16.dp)
                                clip = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(20.dp),
                            color = Color.Green,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(10.dp),
                                text = tips,
                                fontSize = 40.sp,
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
                        Surface(
                            modifier = Modifier
                                .padding(20.dp),
                            color = Color.Red,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(10.dp),
                                text = tips,
                                fontSize = 40.sp,
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
