package com.rollcall.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.rollcall.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * 随机座位生成器
 * 打乱学生座位并以教室布局形式展示
 */
@Composable
fun SeatMapScreen(onClose: () -> Unit) {
    // 模拟学生列表（从AppState获取或使用占位）
    val studentNames = remember {
        mutableStateListOf<String>().apply {
            // 尝试从统计数据获取学生名称
            try {
                val jsonData = com.rollcall.app.util.FileHelper.readFromFile("D:/Xiaoye/StatisticalData.json")
                if (jsonData != "404") {
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
                    val data: Map<String, Int> = com.google.gson.Gson().fromJson(jsonData, type) ?: emptyMap()
                    addAll(data.keys.toList())
                }
            } catch (_: Exception) {}
            // 如果没有数据，使用占位
            if (isEmpty()) {
                for (i in 1..40) add("学生$i")
            }
        }
    }

    var seats by remember { mutableStateOf(studentNames.shuffled()) }
    var columns by remember { mutableStateOf(6) }
    var isShuffling by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = onClose,
        title = "随机座位表",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(620.dp, 700.dp)
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
                        Text("💺", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("随机座位表", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "关闭", tint = colors.textSecondary)
                    }
                }

                // 操作栏
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 列数控制
                    Text("每行", fontSize = 14.sp, color = colors.textSecondary)
                    listOf(4, 5, 6, 7, 8).forEach { col ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (col == columns) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (col == columns) colors.primary else colors.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { columns = col }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$col",
                                fontSize = 14.sp,
                                fontWeight = if (col == columns) FontWeight.Bold else FontWeight.Normal,
                                color = if (col == columns) colors.primary else colors.textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // 打乱按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accent.copy(alpha = 0.12f))
                            .clickable {
                                isShuffling = true
                                seats = studentNames.shuffled()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "🔀 重新排座",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent
                        )
                    }
                }

                LaunchedEffect(isShuffling) {
                    if (isShuffling) { delay(300); isShuffling = false }
                }

                Spacer(Modifier.height(12.dp))

                // 讲台
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎤 讲台", fontSize = 16.sp, color = colors.textHint, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(16.dp))

                // 座位网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(seats.size) { index ->
                        SeatCard(
                            name = seats[index],
                            index = index + 1,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatCard(
    name: String,
    index: Int,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 座号
        Text(
            "$index",
            fontSize = 10.sp,
            color = colors.textHint,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        // 姓名
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
