package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.audio.AudioManager
import com.rollcall.app.drawStar
import com.rollcall.app.setWindowIcon
import com.rollcall.app.state.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 点名结果展示窗口
 * 全屏显示被点到的学生姓名，带有动画效果
 *
 * @param selectedStudent 选中的学生（姓, 名）
 * @param driveIsLongPressed 是否是长按模式（选3个学生）
 * @param isReadyVisible 提示文字是否可见
 * @param isFirstNameVisible 名字是否可见
 * @param isLastNameVisible 姓氏是否可见
 * @param isOpenHtml 是否已播放语音
 * @param onOpenHtmlChanged 语音播放状态变更回调
 * @param student1/2/3 长按模式下选中的3个学生
 * @param luckyGuyJson 幸运学生JSON
 * @param poolGuyJson 倒霉学生JSON
 */
@Composable
fun ApplicationScope.RollCallResultWindow(
    selectedStudent: Pair<String, String>?,
    driveIsLongPressed: Boolean,
    isReadyVisible: Boolean,
    isFirstNameVisible: Boolean,
    isLastNameVisible: Boolean,
    isOpenHtml: Boolean,
    onOpenHtmlChanged: (Boolean) -> Unit,
    student1: String, student2: String, student3: String,
    luckyGuyJson: String, poolGuyJson: String
) {
    val selectedFullName = selectedStudent?.let { it.first + it.second }.orEmpty()
    val studentNamePool = remember(selectedFullName) {
        AppState.getStudentNamePool(includeName = selectedFullName, limit = 42)
    }
    val revealFlash = remember(selectedFullName) { Animatable(0f) }
    val slotStageText = remember(driveIsLongPressed, isReadyVisible, isLastNameVisible, isFirstNameVisible) {
        when {
            driveIsLongPressed -> "连抽进行中"
            isFirstNameVisible -> "结果已锁定"
            isLastNameVisible -> "最后收束中"
            isReadyVisible -> "滚轴锁定中"
            else -> "准备抽取"
        }
    }

    // 随机选择提示语
    val tips = if (!driveIsLongPressed) {
        listOf("我选好了！", "你猜猜是谁？", "就你了！", "幸运儿是",
            "掌声有请", "你是全班最靓的仔", "让我挑选一个学霸...", "我看好你！")
    } else {
        listOf("我选好了！", "你猜猜是谁？", "就你们了！", "幸运儿是",
            "掌声有请", "你们是全班最靓的仔", "让我挑选一些学霸...", "我看好你们！")
    }
    val randomTips = remember(selectedFullName, driveIsLongPressed) { tips.random() }

    // 解析幸运/倒霉学生列表
    val luckyGuyList = parseSafeJsonList(luckyGuyJson)
    val poolGuyList = parseSafeJsonList(poolGuyJson)

    Window(
        onCloseRequest = ::exitApplication,
        title = "点名系统",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false
    ) {
        setWindowIcon()

        // 保持窗口置顶
        LaunchedEffect(Unit) {
            while (isActive) {
                window.isMinimized = false
                window.isAlwaysOnTop = true
                kotlinx.coroutines.delay(1000)
            }
        }

        // 全屏显示
        LaunchedEffect(window) {
            val screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            window.setSize(screenSize.width, screenSize.height)
            window.extendedState = JFrame.MAXIMIZED_BOTH
        }

        // 播放语音
        LaunchedEffect(isOpenHtml) {
            if (!isOpenHtml) {
                onOpenHtmlChanged(true)
                val audioName = if (!driveIsLongPressed) {
                    selectedStudent?.let { it.first + it.second } ?: ""
                } else {
                    "$student1,$student2,$student3"
                }
                if (audioName.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        AudioManager.playNameAudio(
                            audioName, AppState.url,
                            AppState.isInternetAvailable.value
                        )
                    }
                }
            }
        }

        // 结果展示区域
        val baseAccent = Color(AppState.accentColorNamed)
        val infiniteTransition = rememberInfiniteTransition()
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val floatingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse
            )
        )

        LaunchedEffect(selectedFullName, isFirstNameVisible, driveIsLongPressed) {
            if (!driveIsLongPressed && isFirstNameVisible && selectedFullName.isNotBlank()) {
                revealFlash.snapTo(0.34f)
                revealFlash.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                )
            } else if (!isFirstNameVisible) {
                revealFlash.snapTo(0f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    resultBackgroundBrush(baseAccent)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.28f,
                    center = center.copy(x = size.width * 0.18f, y = size.height * 0.16f)
                )
                drawCircle(
                    color = Color(0xFFFFF1C7).copy(alpha = 0.08f),
                    radius = size.minDimension * 0.24f,
                    center = center.copy(x = size.width * 0.86f, y = size.height * 0.18f)
                )
                drawCircle(
                    color = Color(0xFFBEE3FF).copy(alpha = 0.09f),
                    radius = size.minDimension * 0.22f,
                    center = center.copy(x = size.width * 0.76f, y = size.height * 0.82f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A).copy(alpha = 0.22f))
            )

            if (!driveIsLongPressed) {
                AnimatedVisibility(
                    visible = isFirstNameVisible,
                    enter = scaleIn(initialScale = 0.1f, animationSpec = tween(700)) +
                            fadeIn(tween(700)) + expandVertically(tween(500)) +
                            slideInHorizontally(tween(500))
                ) {
                    selectedStudent?.let {
                        when {
                            luckyGuyList.contains(it.second) -> {
                                Canvas(Modifier.fillMaxSize().alpha(0.24f)) {
                                    drawStar(size = 1600f, center = center, rotationAngle = angle)
                                }
                            }
                            poolGuyList.contains(it.second) -> {
                                Text(
                                    "🎱",
                                    fontSize = 600.sp,
                                    modifier = Modifier.rotate(25f),
                                    color = Color.White.copy(alpha = 0.16f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp, vertical = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedVisibility(
                    visible = isReadyVisible,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(1000))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        ResultFloatingTag(
                            title = "本轮提示",
                            value = randomTips,
                            alpha = floatingAlpha
                        )
                        ResultFloatingTag(
                            title = if (driveIsLongPressed) "模式" else "状态",
                            value = if (driveIsLongPressed) "三人连抽" else resolveSingleStudentStatus(selectedStudent, luckyGuyList, poolGuyList),
                            alpha = floatingAlpha
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ResultGlassPanel(
                        modifier = Modifier
                            .widthIn(max = 1500.dp),
                        padding = PaddingValues(horizontal = 40.dp, vertical = 34.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedContent(
                                targetState = slotStageText,
                                transitionSpec = {
                                    fadeIn(tween(180)) + slideInVertically(tween(240)) togetherWith
                                        fadeOut(tween(140)) + slideOutVertically(tween(180))
                                }
                            ) { stageText ->
                                Text(
                                    text = stageText,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.94f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = headlineShadowStyle(28.sp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))

                            if (!driveIsLongPressed) {
                                SlotMachineDisplay(
                                    selectedFullName = selectedFullName,
                                    studentNamePool = studentNamePool,
                                    isReadyVisible = isReadyVisible,
                                    isFirstNameVisible = isFirstNameVisible,
                                    isLastNameVisible = isLastNameVisible
                                )
                            } else {
                                ThreeStudentsDisplay(
                                    visible = isFirstNameVisible,
                                    s1 = student1,
                                    s2 = student2,
                                    s3 = student3
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ResultMetaCard(
                                    title = "语音播报",
                                    value = if (isOpenHtml) "已触发" else "准备中"
                                )
                                ResultMetaCard(
                                    title = "网络",
                                    value = if (AppState.isInternetAvailable.value) "在线" else "离线"
                                )
                                ResultMetaCard(
                                    title = "来源",
                                    value = if (driveIsLongPressed) "连抽模式" else "随机点名"
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }

            if (revealFlash.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = revealFlash.value))
                )
            }
        }
    }
}

@Composable
private fun SlotMachineDisplay(
    selectedFullName: String,
    studentNamePool: List<String>,
    isReadyVisible: Boolean,
    isFirstNameVisible: Boolean,
    isLastNameVisible: Boolean,
    speed: SlotMachineSpeed = SlotMachineSpeed.NORMAL,
    titleText: String = "本轮锁定",
    resultFontSize: Int = 92,
    subtitleOverride: String? = null
) {
    val targetChars = remember(selectedFullName) { normalizeTargetChars(selectedFullName.ifBlank { "下一位" }) }
    val visibleReelCount = remember(selectedFullName) {
        if (selectedFullName.trim().length >= 3) 3 else 2
    }
    val reelAccents = listOf(
        Color(0xFFFFCB74),
        Color(0xFF86C8FF),
        Color(0xFF83E0B2)
    )
    val finalScale by animateFloatAsState(
        targetValue = if (isFirstNameVisible) 1f else 0.95f,
        animationSpec = tween(420, easing = FastOutSlowInEasing)
    )
    val subtitleText = remember(isReadyVisible, isLastNameVisible, isFirstNameVisible, visibleReelCount) {
        subtitleOverride ?: when {
            isFirstNameVisible -> "三轴已收束，结果已揭晓"
            isLastNameVisible -> "还差最后一轴"
            isReadyVisible -> "正在按名单字符滚动${visibleReelCount}个滚轴"
            else -> "等待开始"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResultStagePill(
                title = titleText,
                subtitle = subtitleText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RevealInfoChip(label = "轴数", value = visibleReelCount.toString())
                RevealInfoChip(
                    label = "速度",
                    value = if (speed == SlotMachineSpeed.FAST) "FAST" else "NORMAL"
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isFirstNameVisible) "命中目标已固定在中线" else "滚轴会按名单字符继续减速收束",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(visibleReelCount) { reelIndex ->
                val reelSpec = remember(selectedFullName, studentNamePool, reelIndex, targetChars) {
                    buildCharSlotReelSpec(
                        targetChar = targetChars[reelIndex],
                        pool = studentNamePool,
                        reelIndex = reelIndex,
                        speed = speed
                    )
                }
                SlotReel(
                    spec = reelSpec,
                    accent = reelAccents[reelIndex],
                    shouldSpin = isReadyVisible,
                    highlightLocked = when (reelIndex) {
                        0 -> isLastNameVisible
                        else -> isFirstNameVisible
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        AnimatedVisibility(
            visible = isFirstNameVisible,
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.88f, animationSpec = tween(420)),
            exit = fadeOut(tween(140))
        ) {
            LockResultCard(
                modifier = Modifier
                    .scale(finalScale)
                    .fillMaxWidth(),
                title = titleText,
                value = selectedFullName.ifBlank { "未选中" },
                valueFontSize = resultFontSize.sp
            )
        }
    }
}

@Composable
private fun SlotReel(
    spec: SlotReelSpec,
    accent: Color,
    shouldSpin: Boolean,
    highlightLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val itemHeight = 76.dp
    val visibleRows = 5
    val centerRowIndex = visibleRows / 2
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val finalOffset = (spec.finalIndex - centerRowIndex) * itemHeightPx
    val scrollState = rememberScrollState()
    val settleGlow by animateFloatAsState(
        targetValue = if (highlightLocked) 1f else 0.58f,
        animationSpec = tween(320, easing = FastOutSlowInEasing)
    )
    val pulseTransition = rememberInfiniteTransition()
    val lockedPulse by pulseTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(880, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val glowScale by animateFloatAsState(
        targetValue = if (highlightLocked) 1.02f else 0.985f,
        animationSpec = tween(320, easing = FastOutSlowInEasing)
    )
    val frameAlpha by animateFloatAsState(
        targetValue = when {
            highlightLocked -> 0.26f
            shouldSpin -> 0.18f
            else -> 0.12f
        },
        animationSpec = tween(260, easing = FastOutSlowInEasing)
    )
    val centerGlowAlpha by animateFloatAsState(
        targetValue = when {
            highlightLocked -> 0.22f
            shouldSpin -> 0.15f
            else -> 0.10f
        },
        animationSpec = tween(260, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(shouldSpin, spec, itemHeightPx) {
        scrollState.scrollTo(0)
        if (shouldSpin) {
            delay(spec.startDelayMillis.toLong())
            scrollState.animateScrollTo(
                value = (finalOffset + itemHeightPx * spec.overshootFactor).roundToInt().coerceAtLeast(0),
                animationSpec = tween(
                    durationMillis = (spec.durationMillis * 0.76f).roundToInt(),
                    easing = FastOutSlowInEasing
                )
            )
            scrollState.animateScrollTo(
                value = finalOffset.roundToInt().coerceAtLeast(0),
                animationSpec = tween(
                    durationMillis = (spec.durationMillis * 0.24f).roundToInt().coerceAtLeast(160),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleRows)
            .scale(glowScale)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = frameAlpha))
            .border(
                width = if (highlightLocked) 2.dp else 1.dp,
                color = accent.copy(alpha = if (highlightLocked) 0.8f + lockedPulse * 0.18f else 0.38f),
                shape = RoundedCornerShape(30.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(itemHeight + 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    accent.copy(
                        alpha = centerGlowAlpha + settleGlow * 0.08f + if (highlightLocked) lockedPulse * 0.05f else 0f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState, enabled = false)
        ) {
            spec.items.forEach { label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (label == spec.targetName) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.84f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = displayShadowStyle(48.sp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(accent.copy(alpha = if (highlightLocked) 0.24f else 0.14f))
                .border(
                    width = if (highlightLocked) 3.dp else 1.dp,
                    color = accent.copy(alpha = if (highlightLocked) 0.82f + lockedPulse * 0.16f else 0.55f),
                    shape = RoundedCornerShape(22.dp)
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1828).copy(alpha = 0.92f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0F1828).copy(alpha = 0.92f))
                    )
                )
        )
    }
}

private data class SlotReelSpec(
    val items: List<String>,
    val finalIndex: Int,
    val targetName: String,
    val durationMillis: Int,
    val startDelayMillis: Int,
    val overshootFactor: Float
)

private enum class SlotMachineSpeed {
    NORMAL,
    FAST
}

private fun buildCharSlotReelSpec(
    targetChar: String,
    pool: List<String>,
    reelIndex: Int,
    speed: SlotMachineSpeed
): SlotReelSpec {
    val safeTarget = targetChar.ifBlank { "·" }
    val reelPool = buildReelCharacterPool(pool, reelIndex, safeTarget)
    val random = Random(safeTarget.hashCode() + reelIndex * 97 + reelPool.size * 13)
    val items = mutableListOf<String>()
    val visibleRows = 5
    val headPadding = reelPool.shuffled(random).take(visibleRows)

    items += headPadding
    repeat(7) {
        items += reelPool.shuffled(random).take(minOf(10, reelPool.size))
    }
    repeat(4 + reelIndex) {
        items += reelPool[random.nextInt(reelPool.size)]
    }

    val finalIndex = items.size
    items += safeTarget
    items += reelPool.shuffled(random).take(visibleRows)

    while (items.size < 96) {
        items += reelPool.shuffled(random).take(minOf(12, reelPool.size))
    }

    return SlotReelSpec(
        items = items,
        finalIndex = finalIndex,
        targetName = safeTarget,
        durationMillis = slotTimingProfile(speed).durationFor(reelIndex),
        startDelayMillis = slotTimingProfile(speed).delayFor(reelIndex),
        overshootFactor = slotTimingProfile(speed).overshootFor(reelIndex)
    )
}

private data class SlotTimingProfile(
    val durations: List<Int>,
    val delays: List<Int>,
    val overshoots: List<Float>
) {
    fun durationFor(index: Int): Int = durations.getOrElse(index) { durations.last() }
    fun delayFor(index: Int): Int = delays.getOrElse(index) { delays.last() }
    fun overshootFor(index: Int): Float = overshoots.getOrElse(index) { overshoots.last() }
}

private fun slotTimingProfile(speed: SlotMachineSpeed): SlotTimingProfile = when (speed) {
    SlotMachineSpeed.NORMAL -> SlotTimingProfile(
        durations = listOf(1380, 1720, 2140),
        delays = listOf(0, 120, 240),
        overshoots = listOf(0.24f, 0.32f, 0.2f)
    )

    SlotMachineSpeed.FAST -> SlotTimingProfile(
        durations = listOf(820, 980, 1160),
        delays = listOf(0, 70, 140),
        overshoots = listOf(0.18f, 0.24f, 0.16f)
    )
}

private fun normalizeTargetChars(name: String): List<String> {
    val chars = name.trim().toList().map { it.toString() }
    return when {
        chars.size >= 3 -> chars.take(3)
        chars.size == 2 -> listOf(chars[0], chars[1], "·")
        chars.size == 1 -> listOf(chars[0], "·", "·")
        else -> listOf("下", "一", "位")
    }
}

private fun buildReelCharacterPool(
    names: List<String>,
    reelIndex: Int,
    targetChar: String
): List<String> {
    val normalizedNames = names
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val byPosition = normalizedNames.mapNotNull { raw ->
        val chars = raw.toList().map { it.toString() }
        when {
            reelIndex < chars.size -> chars[reelIndex]
            else -> null
        }
    }

    val pool = (byPosition + listOf(targetChar))
        .filter { it.isNotBlank() }

    return if (pool.isNotEmpty()) pool else listOf("张", "王", "李", "赵", targetChar)
}

private fun resultBackgroundBrush(baseAccent: Color): Brush {
    return Brush.linearGradient(
        colors = listOf(
            baseAccent.copy(alpha = 0.94f),
            baseAccent.copy(
                red = 0.12f + baseAccent.red * 0.6f,
                green = 0.16f + baseAccent.green * 0.55f,
                blue = 0.25f + baseAccent.blue * 0.5f
            ),
            Color(0xFF101727)
        )
    )
}

@Composable
private fun ResultFloatingTag(
    title: String,
    value: String,
    alpha: Float
) {
    ResultGlassPanel(
        modifier = Modifier
            .widthIn(min = 176.dp),
        alpha = alpha,
        cornerRadius = 24.dp,
        padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ResultMetaCard(
    title: String,
    value: String
) {
    ResultGlassPanel(
        modifier = Modifier
            .widthIn(min = 120.dp),
        alpha = 0.12f,
        cornerRadius = 18.dp,
        padding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.72f))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun resolveSingleStudentStatus(
    student: Pair<String, String>?,
    luckyGuyList: List<String>,
    poolGuyList: List<String>
): String {
    val name = student?.second ?: return "等待揭晓"
    return when {
        luckyGuyList.contains(name) -> "幸运位"
        poolGuyList.contains(name) -> "焦点位"
        else -> "已锁定"
    }
}

/**
 * 三人点名显示（长按模式）
 */
@Composable
private fun ThreeStudentsDisplay(
    visible: Boolean,
    s1: String, s2: String, s3: String
) {
    val names = remember(s1, s2, s3) { listOf(s1, s2, s3).filter { it.isNotBlank() } }
    val studentNamePool = remember(names) {
        AppState.getStudentNamePool(includeName = names.firstOrNull().orEmpty(), limit = 42)
    }
    var activeIndex by remember(visible, names) { mutableStateOf(0) }
    var midLocked by remember(visible, names) { mutableStateOf(false) }
    var fullyLocked by remember(visible, names) { mutableStateOf(false) }
    var completedCount by remember(visible, names) { mutableStateOf(0) }
    var showFinalReveal by remember(visible, names) { mutableStateOf(false) }

    LaunchedEffect(visible, names) {
        activeIndex = 0
        midLocked = false
        fullyLocked = false
        completedCount = 0
        showFinalReveal = false

        if (!visible || names.isEmpty()) return@LaunchedEffect

        names.forEachIndexed { index, _ ->
            activeIndex = index
            midLocked = false
            fullyLocked = false
            delay(220)
            midLocked = true
            delay(if (index == 0) 980 else 900)
            fullyLocked = true
            completedCount = index + 1
            if (index != names.lastIndex) delay(340)
        }
        delay(180)
        showFinalReveal = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.1f, animationSpec = tween(520)) +
                fadeIn(tween(420)) + expandVertically(tween(360)),
        exit = fadeOut(tween(180))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResultStagePill(
                    title = if (showFinalReveal) "连抽完成" else "连抽进度",
                    subtitle = if (showFinalReveal) {
                        "3 位结果已全部揭晓"
                    } else {
                        "${activeIndex.coerceAtMost((names.size - 1).coerceAtLeast(0)) + 1}/${names.size.coerceAtLeast(1)}"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    names.forEachIndexed { index, _ ->
                        val dotScale by animateFloatAsState(
                            targetValue = when {
                                completedCount > index -> 1.1f
                                activeIndex == index && !showFinalReveal -> 1f
                                else -> 0.86f
                            },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        )
                        Box(
                            modifier = Modifier
                                .scale(dotScale)
                                .size(if (completedCount > index) 14.dp else 11.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completedCount > index -> Color.White.copy(alpha = 0.92f)
                                        activeIndex == index && !showFinalReveal -> Color.White.copy(alpha = 0.6f)
                                        else -> Color.White.copy(alpha = 0.2f)
                                    }
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (names.isNotEmpty()) {
                AnimatedContent(
                    targetState = showFinalReveal,
                    transitionSpec = {
                        fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) togetherWith fadeOut(tween(160))
                    }
                ) { isFinal ->
                    if (isFinal) {
                        FinalTripleReveal(names = names)
                    } else {
                        AnimatedContent(
                            targetState = activeIndex,
                            transitionSpec = {
                                fadeIn(tween(180)) + slideInHorizontally(
                                    initialOffsetX = { it / 12 },
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ) togetherWith fadeOut(tween(140)) + slideOutHorizontally(
                                    targetOffsetX = { -it / 14 },
                                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                                )
                            }
                        ) { index ->
                SlotMachineDisplay(
                    selectedFullName = names[index],
                    studentNamePool = studentNamePool,
                    isReadyVisible = visible,
                    isFirstNameVisible = fullyLocked,
                    isLastNameVisible = midLocked,
                    speed = SlotMachineSpeed.FAST,
                    titleText = "第 ${index + 1} 位",
                    resultFontSize = 64,
                    subtitleOverride = when {
                        fullyLocked -> "已锁定，准备下一位"
                        midLocked -> "最后一轴正在减速"
                        else -> "快速抽取中"
                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                names.forEachIndexed { index, name ->
                    val isDone = completedCount > index
                    val isCurrent = activeIndex == index && visible && !isDone
                    val cardAlpha by animateFloatAsState(
                        targetValue = when {
                            isDone -> 0.2f
                            isCurrent -> 0.16f
                            else -> 0.08f
                        },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                    val borderAlpha by animateFloatAsState(
                        targetValue = when {
                            isDone -> 0.78f
                            isCurrent -> 0.46f
                            else -> 0.18f
                        },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                    val cardScale by animateFloatAsState(
                        targetValue = when {
                            isDone -> 1f
                            isCurrent -> 1.02f
                            else -> 0.98f
                        },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .scale(cardScale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = cardAlpha))
                            .border(
                                width = if (isDone) 2.dp else 1.dp,
                                color = Color.White.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .heightIn(min = 116.dp)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "第${index + 1}位",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.66f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = when {
                                isDone || isCurrent -> name
                                else -> "等待中"
                            },
                            fontSize = if (isDone || isCurrent) 32.sp else 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDone || isCurrent) Color.White else Color.White.copy(alpha = 0.48f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        if (isDone) {
                            RevealStatusBadge(
                                text = "已锁定",
                                accent = Color.White.copy(alpha = 0.9f)
                            )
                        } else if (isCurrent) {
                            Spacer(Modifier.height(6.dp))
                            RevealStatusBadge(
                                text = "抽取中",
                                accent = Color.White.copy(alpha = 0.72f)
                            )
                        } else {
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalTripleReveal(names: List<String>) {
    var revealCount by remember(names) { mutableStateOf(0) }

    LaunchedEffect(names) {
        revealCount = 0
        names.forEachIndexed { index, _ ->
            delay(if (index == 0) 120L else 220L)
            revealCount = index + 1
        }
    }

    val accentPalette = listOf(
        Color(0xFFFFD27A),
        Color(0xFF8DD2FF),
        Color(0xFF90E7B6)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(36.dp))
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResultStagePill(
                title = "本轮连抽结果",
                subtitle = "三位同学已全部锁定"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RevealInfoChip(label = "人数", value = names.size.toString())
                RevealInfoChip(label = "状态", value = "LOCKED")
            }
        }
        Spacer(Modifier.height(18.dp))
        names.forEachIndexed { index, name ->
            AnimatedVisibility(
                visible = revealCount > index,
                enter = fadeIn(tween(220)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ) + slideInVertically(
                    initialOffsetY = { it / 10 },
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(accentPalette[index % accentPalette.size].copy(alpha = 0.14f))
                        .border(
                            1.dp,
                            accentPalette[index % accentPalette.size].copy(alpha = 0.34f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "第${index + 1}位",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "LOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPalette[index % accentPalette.size]
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (index == 0) "优先锁定" else if (index == 1) "继续命中" else "最终揭晓",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.66f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = name,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (index != names.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ResultStagePill(
    title: String,
    subtitle: String
) {
    ResultGlassPanel(
        modifier = Modifier
            .widthIn(min = 180.dp),
        alpha = 0.14f,
        cornerRadius = 20.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.94f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RevealInfoChip(
    label: String,
    value: String
) {
    ResultGlassPanel(
        modifier = Modifier
            .widthIn(min = 74.dp),
        alpha = 0.12f,
        cornerRadius = 16.dp,
        padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RevealStatusBadge(
    text: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
private fun ResultGlassPanel(
    modifier: Modifier = Modifier,
    alpha: Float = 0.14f,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = alpha))
            .border(
                1.dp,
                Color.White.copy(alpha = (alpha + 0.12f).coerceAtMost(0.42f)),
                RoundedCornerShape(cornerRadius)
            )
            .padding(padding),
        content = content
    )
}

@Composable
private fun LockResultCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueFontSize: androidx.compose.ui.unit.TextUnit
) {
    ResultGlassPanel(
        modifier = modifier,
        alpha = 0.16f,
        cornerRadius = 34.dp,
        padding = PaddingValues(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = valueFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                style = displayShadowStyle(valueFontSize)
            )
        }
    }
}

private fun headlineShadowStyle(fontSize: androidx.compose.ui.unit.TextUnit): TextStyle =
    TextStyle(
        fontSize = fontSize,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.38f),
            offset = Offset(0f, 3f),
            blurRadius = 10f
        )
    )

private fun displayShadowStyle(fontSize: androidx.compose.ui.unit.TextUnit): TextStyle =
    TextStyle(
        fontSize = fontSize,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.42f),
            offset = Offset(0f, 4f),
            blurRadius = 12f
        )
    )

/**
 * 安全解析JSON字符串列表
 */
private fun parseSafeJsonList(json: String): List<String> {
    return try {
        if (json.isNotBlank() && json.trim().startsWith("[")) {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } else emptyList()
    } catch (e: Exception) { emptyList() }
}
