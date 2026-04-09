package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import java.io.File
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.state.AppState

fun Modifier.neumorphism(): Modifier = this
    .clip(RoundedCornerShape(32.dp))
    .background(Color(0xFF1A1A1A)) // 比背景亮一点
    .drawBehind {
        val radius = 32.dp.toPx()

        // 右下暗影
        drawRoundRect(
            color = Color(0xFF000000),
            topLeft = Offset(6f, 6f),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            alpha = 0.8f
        )

        // 左上高光
        drawRoundRect(
            color = Color(0xFF2A2A2A),
            topLeft = Offset(-6f, -6f),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            alpha = 0.9f
        )
    }

fun Modifier.innerNeumorphism(): Modifier = this
    .clip(CircleShape)
    .background(Color(0xFF0E0E0E))
    .drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1C1C1C),
                    Color.Black
                )
            )
        )
    }


@Composable
fun alarmClock() {
    val isAlarmClock = AppState.isAlarmClock.collectAsState()
    var isMusicClock by remember { mutableStateOf(false) }
    val remainingSeconds by countdownTimer(totalMinutes = 45)
    val timeText = formatTimeForAlarm(remainingSeconds)

    if (isAlarmClock.value) {
        LaunchedEffect(Unit) {
            while (!isMusicClock) {
                withContext(Dispatchers.IO) {
                    isMusicClock = checkAndCopyModel(
                        "http://xy.wsmlbe.cn/myBeauty.zip", File("D:/Xiaoye"), File("D:/Xiaoye/Music/")
                    )
                }
                delay(15000)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0E0E0E) // 深黑但不死黑，更高级
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(500.dp)
                    .neumorphism()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .innerNeumorphism(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.padding(20.dp),
                            painter = painterResource("images/alarmClock.png"),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = "距离起床还有",
                        fontSize = 36.sp,
                        color = Color(0xFFAAAAAA)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    flipTimeText(
                        timeText = timeText,
                        fontSize = 104.sp
                    )

                }
            }

            Spacer(modifier = Modifier.height(48.dp))


            Box(
                modifier = Modifier
                    .innerNeumorphism(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = "请勿关闭屏幕",
                    fontSize = 30.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
fun countdownTimer(
    totalMinutes: Int
): State<Long> {
    val isAlarmClock = AppState.isAlarmClock.collectAsState()
    val totalSeconds = totalMinutes * 60L
    val remaining = remember { mutableStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (remaining.value > 0) {
            delay(1000)
            remaining.value--
        }
        if (remaining.value == 0L) {
            val controller = JfxComponentController()
            val musicPath = "D:/Xiaoye/Music/myBeauty.mp3"

            launch {
                snapshotFlow { isAlarmClock.value }
                    .collect { isOn ->
                        if (!isOn) {
                            println("闹钟被关闭，立即停止音乐")
                            controller.stopMedia()
                            cancel() // 停止监听
                        }
                    }
            }

            controller.playMedia(musicPath) {
                AppState.setIsAlarmHasBeenHeard(true)
            }

            Runtime.getRuntime().addShutdownHook(Thread {
                println("程序关闭，停止音乐播放")
                controller.stopMedia()
            })
        }
    }

    return remaining
}

@Composable
fun flipDigit(
    digit: Char,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Bold
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            (slideInVertically { it } + fadeIn())
                .togetherWith(slideOutVertically { -it } + fadeOut())
        },
        label = "FlipDigit"
    ) { target ->
        Text(
            text = target.toString(),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White
        )
    }
}

@Composable
fun staticChar(
    char: Char,
    fontSize: TextUnit
) {
    Text(
        text = char.toString(),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
fun flipTimeText(
    timeText: String, // 格式固定：MM:SS
    fontSize: TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeText.forEach { char ->
            if (char.isDigit()) {
                flipDigit(
                    digit = char,
                    fontSize = fontSize
                )
            } else {
                staticChar(
                    char = char,
                    fontSize = fontSize
                )
            }
        }
    }
}


fun formatTimeForAlarm(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
