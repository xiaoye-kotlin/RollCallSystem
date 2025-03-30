import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel

class JfxComponentController {
    val panel: JPanel = JPanel()
    private lateinit var mediaPlayer: MediaPlayer

    init {
        // 初始化 JavaFX
        JFXPanel() // 必须初始化 JavaFX 环境
    }

    fun playMedia(filePath: String) {
        val media = Media(File(filePath).toURI().toString()) // 本地文件路径
        mediaPlayer = MediaPlayer(media)
        mediaPlayer.play()
    }

    fun stopMedia() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
        }
    }
}

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
                    "http://xyc.okc.today/EasterEgg2.zip", File("D:/Xiaoye/"), File("D:/Xiaoye/EasterEgg/")
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

        // 控制全屏消失
        LaunchedEffect(Unit) {
            delay(50000)
            hasBeenPlayed.value = true
            isRun = false
            Global.setIsEasterEgg(false)
        }


        if (!hasBeenPlayed.value) {
            Window(
                onCloseRequest = {}, title = "点名系统", undecorated = true,  //无边框
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

                val icon = ImageIO.read(javaClass.getResourceAsStream("/images/callTheRoll.png"))
                val awtWindow = this.window

                awtWindow.iconImage = icon  // 设置窗口的图标

                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(0.15f),
                            contentAlignment = Alignment.Center
                        ) {
                            classIdentity()
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(0.85f),
                            contentAlignment = Alignment.Center
                        ) {
                            animatedSentences()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun classIdentity() {
    var isAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1000)
        isAnimation = true
    }
    // 使用 animateFloatAsState 来平滑动画效果
    val animatedOffsets = animateFloatAsState(
        targetValue = if (isAnimation) 0f else 300f,
        animationSpec = tween(durationMillis = 1500)
    )

    val animatedScales =
        animateFloatAsState(
            targetValue = if (isAnimation) 1f else 0.5f,
            animationSpec = tween(durationMillis = 1500)
        )

    val animatedAlphas =
        animateFloatAsState(
            targetValue = if (isAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1500)
        )

    val offsetX by animateFloatAsState(
        targetValue = if (Math.random() > 0.5) 60f else -60f, // 增大偏移量
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = FastOutSlowInEasing), // 慢一点的动画速度
            repeatMode = RepeatMode.Reverse
        )
    )

    val offsetY by animateFloatAsState(
        targetValue = if (Math.random() > 0.5) 60f else -60f, // 增大偏移量
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = FastOutSlowInEasing), // 慢一点的动画速度
            repeatMode = RepeatMode.Reverse
        )
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 100.dp)
            .offset(y = animatedOffsets.value.dp)
            .graphicsLayer(
                scaleX = animatedScales.value,
                scaleY = animatedScales.value
            )
            .alpha(animatedAlphas.value), // 应用渐变效果
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // 使文本可滚动
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            "高2026届${Global.CLASS}班".forEach { char ->
                Text(
                    text = char.toString(),  // 每个字符单独显示
                    fontSize = 50.sp, // 字体大小
                    style = TextStyle(fontWeight = FontWeight.Bold), // 设置粗体和斜体
                    color = Color.White,
                    modifier = if (isAnimation) Modifier.offset(x = offsetX.dp, y = offsetY.dp) else Modifier
                )
            }
        }
    }
}

@Composable
fun animatedSentences() {
    // 追踪每个句子的显示状态
    val sentences =
        remember { mutableStateListOf(false, false, false, false, false, false, false, false, false, false) }

    // 每个句子的延迟时间
    val delayTimes = listOf(8500L, 3500L, 4500L, 3500L, 3500L, 3000L, 2500L, 3000L, 3000L, 3200L)

    // 使用 animateFloatAsState 来平滑动画效果
    val animatedOffsets = sentences.map { sentence ->
        animateFloatAsState(
            targetValue = if (sentence) 0f else 300f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val animatedScales = sentences.map { sentence ->
        animateFloatAsState(
            targetValue = if (sentence) 1f else 0.5f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val animatedAlphas = sentences.map { sentence ->
        animateFloatAsState(
            targetValue = if (sentence) 1f else 0f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    // 控制句子出现的顺序
    LaunchedEffect(Unit) {
        for (i in sentences.indices) {
            delay(delayTimes[i]) // 根据延迟时间来控制每个句子的动画
            sentences[i] = true // 触发每个句子的动画
        }
    }

    // 获取屏幕尺寸来动态调整字体大小
    val screenHeight = Toolkit.getDefaultToolkit().screenSize.height

    // 根据屏幕高度来计算字体大小
    val fontSize = (screenHeight / 13).coerceAtMost(300) // 确保字体大小不会超过最大值

    // 显示句子并应用动画效果
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
    ) {
        val texts = listOf(
            "窗外有风景，手里有课本 🌳📚",
            "眼里有梦想，心中有未来 👀💭🌟",
            "一学期的时间晃眼而过 ⏳💨",
            "我们即将步入高二下学期 📅🎓",
            "希望全体同学们学业有成 📚🎯",
            "老师们身体健康 👩‍🏫💪",
            "最后，祝大家寒假快乐！ ❄️🎉",
            "记得好好完成作业 📝✔️",
            "那我们就相约，各自努力 🤝💪",
            "于2月顶峰相见！ 🏔️⏳"
        )
        // 遍历句子并在可见时应用动画
        sentences.forEachIndexed { index, isVisible ->
            if (isVisible) {
                Box(
                    modifier = Modifier
                        .offset(y = animatedOffsets[index].value.dp)
                        .graphicsLayer(
                            scaleX = animatedScales[index].value,
                            scaleY = animatedScales[index].value
                        )
                        .alpha(animatedAlphas[index].value), // 应用渐变效果
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (index == 9) {
                        Text(
                            texts[index],  // 使用对应的文本
                            fontSize = fontSize.sp, // 动态字体大小
                            style = TextStyle(fontWeight = FontWeight.Bold), // 设置斜体
                            color = Color.Red
                        )
                    } else {
                        Text(
                            texts[index],  // 使用对应的文本
                            fontSize = fontSize.sp, // 动态字体大小
                            style = TextStyle(fontWeight = FontWeight.Bold), // 设置加粗
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}