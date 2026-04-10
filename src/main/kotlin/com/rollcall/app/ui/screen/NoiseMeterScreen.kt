package com.rollcall.app.ui.screen

import androidx.compose.animation.core.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * 课堂噪音检测器
 * 模拟麦克风采样显示教室音量水平
 * 提供直观的颜色反馈，帮助维持课堂纪律
 */
@Composable
fun NoiseMeterScreen(onClose: () -> Unit) {
    var noiseLevel by remember { mutableStateOf(0.3f) }
    var peakLevel by remember { mutableStateOf(0.3f) }
    var history by remember { mutableStateOf(List(30) { 0.2f }) }

    // 模拟噪音检测（真实项目中接入麦克风API）
    LaunchedEffect(Unit) {
        while (isActive) {
            val newLevel = (noiseLevel + (Random.nextFloat() - 0.5f) * 0.15f).coerceIn(0.05f, 1f)
            noiseLevel = newLevel
            if (newLevel > peakLevel) peakLevel = newLevel
            history = (history.drop(1) + newLevel)
            delay(300)
        }
    }

    // 峰值衰减
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            peakLevel = (peakLevel - 0.05f).coerceAtLeast(noiseLevel)
        }
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
            size = DpSize(400.dp, 520.dp)
        )
    ) {
        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors

            val levelColor = when {
                noiseLevel < 0.3f -> colors.success
                noiseLevel < 0.6f -> colors.warning
                else -> colors.error
            }
            val levelText = when {
                noiseLevel < 0.2f -> "🤫 非常安静"
                noiseLevel < 0.35f -> "😊 安静"
                noiseLevel < 0.5f -> "🙂 正常"
                noiseLevel < 0.7f -> "😐 有点吵"
                noiseLevel < 0.85f -> "😟 很吵"
                else -> "🤯 太吵了！"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(colors.gradient1, colors.gradient2)))
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("噪音检测", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "关闭", tint = colors.textSecondary)
                    }
                }

                // 圆弧表盘
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedLevel by animateFloatAsState(
                        targetValue = noiseLevel,
                        animationSpec = tween(250)
                    )

                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 16f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // 背景弧
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.15f),
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 进度弧
                        val progressColor = when {
                            animatedLevel < 0.3f -> Color(0xFF2ECC71)
                            animatedLevel < 0.6f -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }
                        drawArc(
                            color = progressColor,
                            startAngle = 150f,
                            sweepAngle = 240f * animatedLevel,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // 中间文字
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(noiseLevel * 100).toInt()}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                        Text("dB", fontSize = 14.sp, color = colors.textHint)
                    }
                }

                // 状态标签
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(levelColor.copy(alpha = 0.12f))
                        .border(1.dp, levelColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        levelText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = levelColor
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 波形图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / history.size
                        history.forEachIndexed { i, level ->
                            val barHeight = level * size.height
                            val barColor = when {
                                level < 0.3f -> Color(0xFF2ECC71)
                                level < 0.6f -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.7f),
                                topLeft = Offset(i * barWidth + 2, size.height - barHeight),
                                size = Size(barWidth - 4, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 峰值信息
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("当前", fontSize = 12.sp, color = colors.textHint)
                        Text(
                            "${(noiseLevel * 100).toInt()} dB",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("峰值", fontSize = 12.sp, color = colors.textHint)
                        Text(
                            "${(peakLevel * 100).toInt()} dB",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }
}
