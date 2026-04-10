package com.rollcall.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

private data class NoiseReading(
    val normalizedLevel: Float,
    val estimatedDb: Int,
    val statusText: String,
    val errorText: String? = null
)

@Composable
fun NoiseMeterScreen(onClose: () -> Unit) {
    var noiseLevel by remember { mutableStateOf(0f) }
    var estimatedDb by remember { mutableStateOf(0) }
    var peakLevel by remember { mutableStateOf(0f) }
    var peakDb by remember { mutableStateOf(0) }
    var history by remember { mutableStateOf(List(36) { 0f }) }
    var statusMessage by remember { mutableStateOf("正在连接麦克风...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        captureMicrophoneNoise(
            onReading = { reading ->
                noiseLevel = reading.normalizedLevel
                estimatedDb = reading.estimatedDb
                peakLevel = maxOf(peakLevel * 0.985f, reading.normalizedLevel)
                peakDb = maxOf((peakDb * 0.99f).toInt(), reading.estimatedDb)
                history = (history.drop(1) + reading.normalizedLevel)
                statusMessage = reading.statusText
                errorMessage = reading.errorText
            }
        )
    }

    Window(
        onCloseRequest = onClose,
        title = "噪音检测",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(420.dp, 560.dp)
        )
    ) {
        AppTheme {
            val colors = AppTheme.colors

            val levelColor = when {
                noiseLevel < 0.22f -> colors.success
                noiseLevel < 0.5f -> colors.warning
                else -> colors.error
            }
            val levelText = when {
                errorMessage != null -> "未检测到麦克风"
                noiseLevel < 0.12f -> "非常安静"
                noiseLevel < 0.25f -> "安静"
                noiseLevel < 0.45f -> "正常"
                noiseLevel < 0.65f -> "有点吵"
                noiseLevel < 0.82f -> "很吵"
                else -> "太吵了"
            }
            val animatedLevel by animateFloatAsState(
                targetValue = noiseLevel.coerceIn(0f, 1f),
                animationSpec = tween(160)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.gradient1,
                                colors.gradient2,
                                Color.White.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .border(1.dp, colors.cardBorder.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("噪音检测", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text(
                                text = if (errorMessage == null) "实时麦克风采样" else "麦克风不可用",
                                fontSize = 12.sp,
                                color = colors.textHint
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "关闭", tint = colors.textSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.76f))
                        .border(1.dp, colors.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = errorMessage ?: "当前为真实麦克风输入强度；显示的是相对 dB 估算值，不是校准后的物理分贝。",
                        fontSize = 12.sp,
                        color = if (errorMessage == null) colors.textHint else colors.error,
                        lineHeight = 18.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(190.dp)) {
                        val strokeWidth = 16f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        drawArc(
                            color = Color.Gray.copy(alpha = 0.16f),
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = levelColor,
                            startAngle = 150f,
                            sweepAngle = 240f * animatedLevel,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = estimatedDb.toString(),
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                        Text("dB", fontSize = 14.sp, color = colors.textHint)
                        Spacer(Modifier.height(6.dp))
                        Text(statusMessage, fontSize = 12.sp, color = colors.textHint)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(levelColor.copy(alpha = 0.12f))
                        .border(1.dp, levelColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(levelText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = levelColor)
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.76f))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / history.size
                        history.forEachIndexed { index, level ->
                            val barHeight = level.coerceIn(0.02f, 1f) * size.height
                            val barColor = when {
                                level < 0.22f -> Color(0xFF2ECC71)
                                level < 0.5f -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.78f),
                                topLeft = Offset(index * barWidth + 1.5f, size.height - barHeight),
                                size = Size(barWidth - 3f, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoiseMetricCard(
                        title = "当前",
                        value = "${estimatedDb} dB",
                        subtitle = levelText,
                        accentColor = levelColor,
                        modifier = Modifier.weight(1f)
                    )
                    NoiseMetricCard(
                        title = "峰值",
                        value = "${peakDb} dB",
                        subtitle = "本次采样",
                        accentColor = colors.accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoiseMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(title, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4E5B57))
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF8A8298))
    }
}

private suspend fun captureMicrophoneNoise(onReading: suspend (NoiseReading) -> Unit) {
    withContext(Dispatchers.IO) {
        val format = AudioFormat(44_100f, 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)

        if (!AudioSystem.isLineSupported(info)) {
            onReading(
                NoiseReading(
                    normalizedLevel = 0f,
                    estimatedDb = 0,
                    statusText = "麦克风不可用",
                    errorText = "当前系统没有可用的录音输入设备，或 Java 无法访问麦克风。"
                )
            )
            return@withContext
        }

        var line: TargetDataLine? = null
        try {
            line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()

            val buffer = ByteArray(4096)
            var smoothedLevel = 0f

            while (isActive) {
                val bytesRead = line.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) {
                    delay(60)
                    continue
                }

                val rms = calculateRms(buffer, bytesRead)
                val dbfs = if (rms > 0.0) 20.0 * log10(rms / Short.MAX_VALUE) else -90.0
                val normalizedLevel = (((dbfs + 60.0) / 60.0).toFloat()).coerceIn(0f, 1f)
                smoothedLevel = smoothedLevel * 0.65f + normalizedLevel * 0.35f
                val estimatedDb = (smoothedLevel * 90f).toInt().coerceIn(0, 90)

                onReading(
                    NoiseReading(
                        normalizedLevel = smoothedLevel,
                        estimatedDb = estimatedDb,
                        statusText = String.format(Locale.US, "实时采样 %.1f dBFS", dbfs.coerceAtLeast(-90.0))
                    )
                )

                delay(80)
            }
        } catch (e: Exception) {
            onReading(
                NoiseReading(
                    normalizedLevel = 0f,
                    estimatedDb = 0,
                    statusText = "麦克风启动失败",
                    errorText = "无法读取麦克风：${e.message ?: "未知错误"}"
                )
            )
        } finally {
            try {
                line?.stop()
                line?.close()
            } catch (_: Exception) {
            }
        }
    }
}

private fun calculateRms(buffer: ByteArray, bytesRead: Int): Double {
    if (bytesRead < 2) return 0.0

    var sum = 0.0
    var sampleCount = 0
    var index = 0
    while (index + 1 < bytesRead) {
        val low = buffer[index].toInt() and 0xFF
        val high = buffer[index + 1].toInt()
        val sample = ((high shl 8) or low).toShort().toInt()
        sum += sample.toDouble() * sample.toDouble()
        sampleCount++
        index += 2
    }

    if (sampleCount == 0) return 0.0
    return sqrt(sum / sampleCount).let { if (it.isNaN()) 0.0 else abs(it) }
}
