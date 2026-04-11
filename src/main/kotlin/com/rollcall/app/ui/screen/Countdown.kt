package com.rollcall.app.ui.screen

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
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.network.NetworkHelper.getResourcePackageUrl
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.isActive

@Composable
fun countdown() {
    val countDownType = AppState.countDownType.collectAsState()
    val isDownloadSuccessfully = remember { mutableStateOf(false) }
    var downloadMusic by remember { mutableStateOf(false) }
    val controller = remember { JfxComponentController() }
    var hasPlayedWarning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            controller.stopMedia()
        }
    }

    LaunchedEffect(countDownType.value) {
        if (countDownType.value != 0 && !downloadMusic) {
            withContext(Dispatchers.IO) {
                while (!downloadMusic && isActive) {
                    downloadMusic = checkAndCopyModel(
                        getResourcePackageUrl("Countdown.zip"), File("D:/Xiaoye/"), File("D:/Xiaoye/Countdown/")
                    )
                    if (downloadMusic) {
                        isDownloadSuccessfully.value = true
                        break
                    }
                    delay(3000)
                }
            }
        }
    }

    LaunchedEffect(countDownType.value) {
        hasPlayedWarning = false
        controller.stopMedia()
    }

    var timeLeft by remember { mutableStateOf(0L) }

    LaunchedEffect(countDownType.value) {
        val activeType = countDownType.value
        val totalDuration = countdownDurationMillis(activeType)
        timeLeft = totalDuration

        if (activeType == 0 || totalDuration <= 0L) {
            return@LaunchedEffect
        }

        while (isActive && AppState.countDownType.value == activeType && timeLeft > 0L) {
            delay(1000)
            timeLeft = (timeLeft - 1000L).coerceAtLeast(0L)
        }
    }

    LaunchedEffect(timeLeft) {
        if (countDownType.value != 0 && timeLeft == 3000L && !hasPlayedWarning) {
            hasPlayedWarning = true
            if (isDownloadSuccessfully.value) {
                val musicPath = "D:/Xiaoye/Countdown/Countdown.mp3"
                controller.playMedia(musicPath)
            }
        }
        if (countDownType.value != 0 && timeLeft <= 0L) {
            controller.stopMedia()
            AppState.setCountDownType(0)
            AppState.setButtonState("关闭")
        }
    }

    val countdownColors = AppTheme.colors

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
                    text = "⏱ 倒计时",
                    fontSize = 50.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    color = countdownColors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (countDownType.value) {
                    1, 2, 3, 4 -> {
                        Text(
                            text = formatTime(timeLeft),
                            fontSize = 100.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = if (timeLeft <= 3000L) countdownColors.error else countdownColors.accent
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
        IconButton(onClick = { AppState.setCountDownType(0) }) {
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

private fun countdownDurationMillis(type: Int): Long {
    return when (type) {
        1 -> 60_000L
        2 -> 180_000L
        3 -> 300_000L
        4 -> 600_000L
        else -> 0L
    }
}
