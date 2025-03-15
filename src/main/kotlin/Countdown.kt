import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun countdown() {
    val countDownType = Global.countDownType.collectAsState()
    var isRun by remember { mutableStateOf(false) }
    val isDownloadSuccessfully = remember { mutableStateOf(false) }
    var downloadMusic by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (!downloadMusic) {
                downloadMusic = checkAndCopyModel(
                    "http://xyc.okc.today/Countdown.zip", File("D:/Xiaoye/"), File("D:/Xiaoye/Countdown/")
                )
                if (downloadMusic) {
                    isDownloadSuccessfully.value = true
                    break
                }
                delay(3000)
            }
        }
    }

    val times = when (countDownType.value) {
        1 -> 60000L       // 1分钟
        2 -> 180000L      // 3分钟
        3 -> 300000L      // 5分钟
        4 -> 600000L      // 10分钟
        else -> 0L        // 默认情况
    }

    var timeLeft by remember { mutableStateOf(times) }

    // 每秒减少一次
    LaunchedEffect(countDownType.value) {
        while (timeLeft > 0) {
            delay(1000)  // 每秒减少
            timeLeft -= 1000
        }
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft == 3000L) {
            if (!isRun) {

                val controller = JfxComponentController()

                val musicPath = "D:/Xiaoye/Countdown/Countdown.mp3"

                controller.playMedia(musicPath)

                Runtime.getRuntime().addShutdownHook(Thread {
                    println("程序关闭，停止音乐播放")
                    controller.stopMedia()
                })

                isRun = true
            }
        }
        if (timeLeft <= 0L) {
            Global.setCountDownType(0)
            Global.setButtonState("关闭")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(10.dp),
                    text = "倒计时",
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (countDownType.value) {
                    1 -> {
                        Text(
                            text = formatTime(timeLeft),
                            fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }

                    2 -> {
                        Text(
                            text = formatTime(timeLeft),
                            fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }

                    3 -> {
                        Text(
                            text = formatTime(timeLeft),
                            fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }

                    4 -> {
                        Text(
                            text = formatTime(timeLeft),
                            fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopEnd // 设置内容对齐方式为右上角
    ) {
        IconButton(onClick = { Global.setCountDownType(0) }) {
            Icon(
                imageVector = Icons.Default.Close, // 选择图标，这里用的是关闭图标
                contentDescription = "Close"
            )
        }
    }
}

// 格式化时间：将毫秒转换为分钟：秒 格式
fun formatTime(milliseconds: Long): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
