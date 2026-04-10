package com.rollcall.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.state.AppState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class JfxComponentController {
    private lateinit var mediaPlayer: MediaPlayer

    init {
        JFXPanel()
    }

    fun playMedia(
        filePath: String,
        onFinished: (() -> Unit)? = null
    ) {
        val media = Media(File(filePath).toURI().toString())
        mediaPlayer = MediaPlayer(media).apply {
            setOnEndOfMedia {
                println("音乐播放完毕")
                onFinished?.invoke()
            }
            play()
        }
    }

    fun stopMedia() {
        mediaPlayer.stop()
    }
}

/**
 * 彩蛋界面 — 高三毕业版本
 * 黑金配色，流星粒子背景，缓慢浮现的毕业寄语
 * 传递青春一去不返的惆怅与对未来的期许
 */
@Composable
fun easterEgg() {
    var isRun by remember { mutableStateOf(false) }
    val hasBeenPlayed = remember { mutableStateOf(false) }
    val isDownloadSuccessfully = remember { mutableStateOf(false) }
    var downloadMusic by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (!downloadMusic) {
                downloadMusic = checkAndCopyModel(
                    "http://xy.wsmlbe.cn/EasterEgg2.zip", File("D:/Xiaoye/"), File("D:/Xiaoye/EasterEgg/")
                )
                if (downloadMusic) {
                    isDownloadSuccessfully.value = true
                    break
                }
                delay(3000)
            }
        }
    }

    if (isDownloadSuccessfully.value) {
        if (!isRun) {
            val controller = JfxComponentController()
            val musicPath = "D:/Xiaoye/EasterEgg/EasterEgg.mp3"
            controller.playMedia(musicPath)
            Runtime.getRuntime().addShutdownHook(Thread {
                println("程序关闭，停止音乐播放")
                controller.stopMedia()
            })
            isRun = true
        }

        LaunchedEffect(Unit) {
            delay(55000)
            hasBeenPlayed.value = true
            isRun = false
            AppState.setIsEasterEgg(false)
        }

        if (!hasBeenPlayed.value) {
            Window(
                onCloseRequest = {}, title = "毕业季",
                undecorated = true, transparent = true,
                alwaysOnTop = true, resizable = false
            ) {
                LaunchedEffect(Unit) {
                    while (isActive) {
                        window.isMinimized = false
                        window.isAlwaysOnTop = true
                        delay(1000)
                    }
                }

                LaunchedEffect(window) {
                    val screenSize =
                        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
                    window.setSize(screenSize.width, screenSize.height)
                    window.extendedState = JFrame.MAXIMIZED_BOTH
                }

                val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
                window.iconImage = icon

                // 整体淡入
                var globalFadeIn by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(300); globalFadeIn = true }
                val globalAlpha by animateFloatAsState(
                    targetValue = if (globalFadeIn) 1f else 0f,
                    animationSpec = tween(2000)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(globalAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A0A12),
                                    Color(0xFF0D0D1A),
                                    Color(0xFF111128),
                                    Color(0xFF0A0A12)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 流星粒子背景
                    MeteorParticles()

                    // 底部光晕
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x18D4A574),
                                    Color(0x08D4A574),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.85f),
                                radius = size.width * 0.4f
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.85f),
                            radius = size.width * 0.4f
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：班级标识
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(0.18f),
                            contentAlignment = Alignment.Center
                        ) {
                            classIdentity()
                        }
                        // 右侧：毕业寄语
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(0.82f),
                            contentAlignment = Alignment.Center
                        ) {
                            graduationSentences()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 流星粒子动画背景
 * 模拟夜空中缓缓坠落的星光碎片
 */
@Composable
private fun MeteorParticles() {
    data class Particle(
        val x: Float, val startY: Float, val speed: Float,
        val size: Float, val alpha: Float, val hue: Float
    )

    val particles = remember {
        List(40) {
            Particle(
                x = Random.nextFloat(),
                startY = Random.nextFloat() * -0.3f,
                speed = 0.0002f + Random.nextFloat() * 0.0004f,
                size = 1f + Random.nextFloat() * 3f,
                alpha = 0.15f + Random.nextFloat() * 0.4f,
                hue = Random.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = ((p.startY + time * p.speed * 3000f) % 1.3f)
            val px = p.x * size.width + sin((time * 10f + p.hue * 50f).toDouble()).toFloat() * 30f
            val py = y * size.height
            val color = when {
                p.hue < 0.33f -> Color(0xFFD4A574) // 暖金
                p.hue < 0.66f -> Color(0xFFE8D5B7) // 浅金
                else -> Color(0xFF8B9DC3) // 银蓝
            }
            drawCircle(
                color = color.copy(alpha = p.alpha * (1f - y.coerceIn(0f, 1f) * 0.5f)),
                radius = p.size,
                center = Offset(px, py)
            )
        }
    }
}

/**
 * 班级标识 — 毕业版
 * 竖排金色文字，呼吸式光晕，优雅缓动入场
 */
@Composable
fun classIdentity() {
    var isAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1500); isAnimation = true }

    val slideUp by animateFloatAsState(
        targetValue = if (isAnimation) 0f else 200f,
        animationSpec = tween(2500, easing = FastOutSlowInEasing)
    )
    val fadeIn by animateFloatAsState(
        targetValue = if (isAnimation) 1f else 0f,
        animationSpec = tween(3000)
    )

    // 呼吸式发光效果
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 60.dp)
            .offset(y = slideUp.dp)
            .alpha(fadeIn),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 顶部装饰线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFD4A574).copy(alpha = glowAlpha))
                        )
                    )
            )
            Spacer(Modifier.height(20.dp))

            // 班级名称 - 竖排
            "高三${AppState.CLASS}班".forEach { char ->
                Text(
                    text = char.toString(),
                    fontSize = 44.sp,
                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFD4A574).copy(alpha = glowAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            // 届次标识
            Text(
                "2026",
                fontSize = 20.sp,
                color = Color(0xFF8B9DC3).copy(alpha = 0.6f),
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(20.dp))
            // 底部装饰线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFD4A574).copy(alpha = glowAlpha), Color.Transparent)
                        )
                    )
            )
        }
    }
}

/**
 * 毕业寄语动画 — 高三毕业版
 * 逐句缓慢浮现，金色文字，最后一句特殊高亮
 * 传递青春不再、珍重前行的情感
 */
@Composable
fun graduationSentences() {
    // 毕业主题寄语
    val texts = listOf(
        "三年前的秋天，我们在这里相遇",
        "教室里的笑声、走廊上的奔跑",
        "那些为梦想拼搏的日日夜夜",
        "都变成了此刻最珍贵的回忆",
        "",  // 空行作为分隔
        "黑板上的公式终会被擦去",
        "但刻在心底的名字永远不会褪色",
        "",
        "愿你历尽千帆，归来仍是少年",
        "高三${AppState.CLASS}班的故事，未完待续……"
    )

    val sentences = remember { mutableStateListOf(*Array(texts.size) { false }) }
    val delayTimes = listOf(4000L, 4000L, 4000L, 4500L, 500L, 4000L, 4500L, 500L, 4500L, 5000L)

    LaunchedEffect(Unit) {
        for (i in sentences.indices) {
            delay(delayTimes[i])
            sentences[i] = true
        }
    }

    val screenHeight = Toolkit.getDefaultToolkit().screenSize.height
    val baseFontSize = (screenHeight / 18).coerceIn(30, 120)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 60.dp)
    ) {
        sentences.forEachIndexed { index, isVisible ->
            if (texts[index].isEmpty()) {
                // 空行分隔
                if (isVisible) Spacer(Modifier.height(20.dp))
                return@forEachIndexed
            }

            val slideUp by animateFloatAsState(
                targetValue = if (isVisible) 0f else 60f,
                animationSpec = tween(2000, easing = FastOutSlowInEasing)
            )
            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(2500)
            )
            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.9f,
                animationSpec = tween(2000)
            )

            val isLast = index == texts.size - 1
            val isFarewell = index == texts.size - 3 // "愿你历尽千帆"

            if (isVisible) {
                Box(
                    modifier = Modifier
                        .offset(y = slideUp.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                        .alpha(alpha)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = texts[index],
                        fontSize = when {
                            isLast -> (baseFontSize * 1.15f).sp
                            isFarewell -> (baseFontSize * 1.05f).sp
                            else -> baseFontSize.sp
                        },
                        style = TextStyle(
                            fontWeight = if (isLast || isFarewell) FontWeight.Bold else FontWeight.Normal,
                            brush = when {
                                isLast -> Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500),
                                        Color(0xFFFF6347),
                                        Color(0xFFFFD700)
                                    )
                                )
                                isFarewell -> Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFD4A574),
                                        Color(0xFFE8D5B7),
                                        Color(0xFFD4A574)
                                    )
                                )
                                else -> null
                            }
                        ),
                        color = if (!isLast && !isFarewell) {
                            Color(0xFFE8E0D4).copy(alpha = 0.9f)
                        } else Color.Unspecified,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}