import Global.buttonState
import Global.subjectList
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 自定义倒置三角形形状
object TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            // 将顶点向右拉伸，比如拉伸到宽度的 3/4 处
            moveTo(size.width * 0.75f, size.height)
            // 右边顶点
            lineTo(size.width, 0f)
            // 左边顶点
            lineTo(0f, 0f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}


@Composable
fun dragWindow() {


    val displayText = buttonState.collectAsState()
    var isClick by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf(false) }

    if (isClick && displayText.value == "点名") {
        //fetchWebPage(Global.url)
        isClick = false
        Global.setButtonState("关闭")
    }

    if (isClick && displayText.value == "关闭") {
        //killProcess("msedge.exe")
        isClick = false
        Global.setButtonState("点名")
    }

// 存储协程任务，用来取消之前的协程

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        var subject = ""

        val isChangeFace = Global.isChangeFace.collectAsState()
        val isTime = Global.isTime.collectAsState()

        val week = Global.week.collectAsState()
        val time = Global.time.collectAsState()

        if (isTime.value && week.value != "无" && time.value != "无") {
            val todaySchedule = subjectList[week.value]
            if (todaySchedule != null) {
                val currentTime = time.value // 当前时间
                val nextClass = getNextClassIfDismissalTime(currentTime, todaySchedule.schedule)

                if (nextClass != null) {
                    if (nextClass != "未下课") {
                        subject = nextClass
                        Global.setIsChangeFace(true)
                        println("Next Subject is: $nextClass")
                    } else {
                        Global.setIsChangeFace(false)
                    }
                } else {
                    println("No Subject")
                }
            } else {
                println("No Subject")
            }
        } else {
            Global.setIsChangeFace(false)
        }

        var isAnimated by remember { mutableStateOf(false) }
        var isChange by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(1000)
            isAnimated = true
        }

        LaunchedEffect(isChangeFace.value) {
            isAnimated = false  // 在每次状态变化时重置动画状态
            delay(500)  // 延迟触发动画
            isAnimated = true  // 设置为 true 以触发动画
        }

        LaunchedEffect(isChangeFace.value) {
            delay(1000)
            isChange = isChangeFace.value
        }

        LaunchedEffect(longPressed) {
            if (longPressed) {
                delay(2000)
                longPressed = false
                Global.setIsLongPressed(false)
            }
        }

        AnimatedVisibility(
            visible = isAnimated,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)) + slideInVertically(
                initialOffsetY = { -80 },
                animationSpec = tween(durationMillis = 500)
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isChange) {
                    Surface(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .height(100.dp)
                            .width(300.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                                    text = "下节课是",
                                    fontSize = 35.sp,
                                )
                                Text(
                                    text = "${subject}课",
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                                    fontSize = 35.sp,
                                    style = TextStyle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFF0000), // 红色
                                                Color(0xFFFF6347), // 番茄红
                                                Color(0xFFFF0000)  // 红色
                                            )
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    modifier = Modifier.padding(end = 10.dp, top = 10.dp, bottom = 10.dp),
                                    text = "~",
                                    fontSize = 35.sp,
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .size(20.dp)
                            .clip(TriangleShape) // 将三角形形状应用于 clip
                    ) {
                        // 三角形箭头
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                var rotation by remember { mutableStateOf(0f) }
                var scale by remember { mutableStateOf(1f) }

                val animatedRotation by animateFloatAsState(
                    targetValue = rotation,
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutLinearInEasing
                    ), label = ""
                )

                val animatedScale by animateFloatAsState(
                    targetValue = scale,
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutLinearInEasing
                    ), label = ""
                )

                var isFirstRun by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                        while (true) {
                            if (isFirstRun) {

                                val randomNumber = (1..2).random()
                                if (randomNumber == 1 && buttonState.value != "关闭") {
                                    Global.setButtonState("^_^")
                                    delay(2000)
                                    if (buttonState.value != "关闭") {
                                        Global.setButtonState("点名")
                                    }
                                } else if (buttonState.value != "关闭") {
                                    scale = 1.3f
                                    rotation = 360f
                                    delay(1200)
                                    scale = 1f
                                    rotation = 0f
                                    delay(1200)
                                    Global.setButtonState("＠_＠")
                                    delay(2000)
                                    if (buttonState.value != "关闭") {
                                        Global.setButtonState("点名")
                                    }
                                }
                            } else {
                           isFirstRun = true
                        }
                            val randomDelay = (1..10000).random() + 60000
                            delay(randomDelay.toLong())
                    }
                }

                Surface(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .height(100.dp)
                        .width(100.dp)
                        .clip(CircleShape)
                        .rotate(animatedRotation)
                        .scale(animatedScale)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(Global.accentColorFloating))
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = displayText.value,
                            fontSize = 30.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}