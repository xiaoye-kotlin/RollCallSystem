package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.state.AppState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * 随机抽题器
 * 教师可自定义题库，随机抽取题目展示
 * 支持多种题目类型
 */
@Composable
fun RandomQuizScreen(onClose: () -> Unit) {
    // 内置示例题目
    val questions = remember {
        mutableStateListOf(
            QuizQuestion("1 + 1 = ?", "数学", listOf("1", "2", "3", "4"), 1),
            QuizQuestion("水的化学式是？", "化学", listOf("H₂O", "CO₂", "NaCl", "O₂"), 0),
            QuizQuestion("中国的首都是？", "地理", listOf("上海", "北京", "广州", "深圳"), 1),
            QuizQuestion("「春眠不觉晓」的下一句是？", "语文", listOf("花落知多少", "处处闻啼鸟", "夜来风雨声", "春风吹又生"), 1),
            QuizQuestion("光的传播速度约为？", "物理", listOf("3×10⁶ m/s", "3×10⁸ m/s", "3×10⁵ m/s", "3×10⁷ m/s"), 1),
            QuizQuestion("英语中 'apple' 的复数形式是？", "英语", listOf("apples", "apple's", "applees", "appless"), 0),
            QuizQuestion("DNA的全称是？", "生物", listOf("脱氧核糖核酸", "核糖核酸", "氨基酸", "脂肪酸"), 0),
            QuizQuestion("第一次世界大战开始于哪一年？", "历史", listOf("1912年", "1914年", "1918年", "1939年"), 1)
        )
    }
    val subjects = remember(questions) { listOf("全部") + questions.map { it.subject }.distinct() }
    var selectedSubject by remember { mutableStateOf("全部") }
    val availableQuestions = remember(questions, selectedSubject) {
        if (selectedSubject == "全部") questions.toList() else questions.filter { it.subject == selectedSubject }
    }

    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var selectedAnswer by remember { mutableStateOf(-1) }
    var showAnswer by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    val correctAnswerText = currentQuestion?.options?.getOrNull(currentQuestion?.correctIndex ?: -1).orEmpty()

    fun drawRandomQuestion() {
        if (availableQuestions.isEmpty() || isAnimating) return
        isAnimating = true
        showAnswer = false
        selectedAnswer = -1
        currentQuestion = availableQuestions.random()
    }

    LaunchedEffect(selectedSubject) {
        currentQuestion = null
        selectedAnswer = -1
        showAnswer = false
    }

    Window(
        onCloseRequest = onClose,
        title = "随机抽题",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(620.dp, 720.dp)
        )
    ) {
        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(colors.gradient1, colors.gradient2)))
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("随机抽题", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text(
                                "当前题库 ${availableQuestions.size} 题${if (selectedSubject == "全部") "" else " · $selectedSubject"}",
                                fontSize = 12.sp,
                                color = colors.textHint
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "关闭", tint = colors.textSecondary)
                    }
                }

                subjects.chunked(4).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = if (rowIndex == subjects.chunked(4).lastIndex) 14.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { subject ->
                            SubjectChip(
                                label = subject,
                                selected = selectedSubject == subject,
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedSubject = subject
                            }
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 抽题按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(colors.primary, colors.primary.copy(alpha = 0.8f))
                            )
                        )
                        .clickable(enabled = availableQuestions.isNotEmpty()) { drawRandomQuestion() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when {
                            availableQuestions.isEmpty() -> "当前科目暂无题目"
                            currentQuestion == null -> "🎲 点击抽题"
                            else -> "🔄 换一题"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }

                // 动画完成回调
                LaunchedEffect(isAnimating) {
                    if (isAnimating) { delay(300); isAnimating = false }
                }

                Spacer(Modifier.height(16.dp))

                // 题目显示区域
                if (currentQuestion != null) {
                    val q = currentQuestion!!

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        // 科目标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(q.subject, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                        }

                        Spacer(Modifier.height(12.dp))

                        // 题目文字
                        Text(
                            q.question,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 选项
                        q.options.forEachIndexed { i, option ->
                            val isCorrect = i == q.correctIndex
                            val isSelected = i == selectedAnswer
                            val optionBorder = when {
                                showAnswer && isCorrect -> colors.success
                                showAnswer && isSelected && !isCorrect -> colors.error
                                isSelected -> colors.primary
                                else -> colors.cardBorder
                            }
                            val optionBg = when {
                                showAnswer && isCorrect -> colors.success.copy(alpha = 0.08f)
                                showAnswer && isSelected && !isCorrect -> colors.error.copy(alpha = 0.08f)
                                isSelected -> colors.primary.copy(alpha = 0.06f)
                                else -> colors.cardBackground
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(optionBg)
                                    .border(1.dp, optionBorder, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !showAnswer) {
                                        selectedAnswer = i
                                        showAnswer = true
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 选项字母
                                val letter = ('A' + i).toString()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(optionBorder.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        letter, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = optionBorder
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    option,
                                    fontSize = 17.sp,
                                    color = colors.textPrimary
                                )
                                // 正确/错误指示
                                if (showAnswer) {
                                    Spacer(Modifier.weight(1f))
                                    if (isCorrect) {
                                        Text("✅", fontSize = 18.sp)
                                    } else if (isSelected) {
                                        Text("❌", fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuizActionButton(
                                label = if (showAnswer) "再抽一题" else "跳过本题",
                                accent = colors.primary,
                                modifier = Modifier.weight(1f)
                            ) {
                                drawRandomQuestion()
                            }
                            QuizActionButton(
                                label = if (showAnswer) "答案：$correctAnswerText" else "等待作答",
                                accent = if (showAnswer) colors.success else colors.textHint,
                                enabled = showAnswer,
                                modifier = Modifier.weight(1f)
                            ) {}
                        }
                    }
                } else {
                    // 无题目时的占位
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 60.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (availableQuestions.isEmpty()) "当前筛选下暂无题目" else "点击上方按钮随机抽取题目",
                                fontSize = 16.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectChip(
    label: String,
    selected: Boolean,
    colors: com.rollcall.app.ui.theme.AppColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.cardBackground)
            .border(
                1.dp,
                if (selected) colors.primary.copy(alpha = 0.35f) else colors.cardBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.primary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuizActionButton(
    label: String,
    accent: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.3f else 0.16f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) accent else accent.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

data class QuizQuestion(
    val question: String,
    val subject: String,
    val options: List<String>,
    val correctIndex: Int
)
