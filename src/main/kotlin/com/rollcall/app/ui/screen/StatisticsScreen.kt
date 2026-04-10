package com.rollcall.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper

/**
 * 点名统计面板
 * 展示每个学生的被点名次数，按次数降序排列
 * 带有可视化条形图和排名指示器
 *
 * @param onClose 关闭面板回调
 */
@Composable
fun StatisticsPanel(
    onClose: () -> Unit
) {
    val statsData = remember { loadStatistics() }
    val maxCount = statsData.maxOfOrNull { it.second } ?: 1
    val totalCalls = statsData.sumOf { it.second }

    Window(
        onCloseRequest = onClose,
        title = "点名统计",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(520.dp, 700.dp)
        ),
    ) {
        com.rollcall.app.ui.theme.AppTheme {
            val themeColors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(themeColors.gradient1, themeColors.gradient2)
                        )
                    )
            ) {
                // 标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "📊 点名统计",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "共 ${statsData.size} 位同学 · 总计 $totalCalls 次点名",
                            fontSize = 14.sp,
                            color = themeColors.textHint
                        )
                    }
                    // 关闭按钮
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = themeColors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 统计列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (statsData.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无点名记录",
                                    fontSize = 18.sp,
                                    color = themeColors.textHint
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = statsData,
                            key = { index, (name, _) -> "${name}_$index" }
                        ) { index, (name, count) ->
                            StatisticsRow(
                                rank = index + 1,
                                name = name,
                                count = count,
                                maxCount = maxCount,
                                colors = themeColors
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统计行组件
 * 每个学生一行，包含排名、姓名、次数和可视化进度条
 */
@Composable
private fun StatisticsRow(
    rank: Int,
    name: String,
    count: Int,
    maxCount: Int,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    val progress = count.toFloat() / maxCount.coerceAtLeast(1)

    // 前三名使用特殊颜色
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // 金
        2 -> Color(0xFFC0C0C0) // 银
        3 -> Color(0xFFCD7F32) // 铜
        else -> colors.textHint
    }

    val barColor = when (rank) {
        1 -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        2 -> Brush.horizontalGradient(listOf(Color(0xFFC0C0C0), Color(0xFF808080)))
        3 -> Brush.horizontalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8B4513)))
        else -> Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.7f), colors.primary))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )
        }

        Spacer(Modifier.width(12.dp))

        // 姓名和进度条
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${count}次",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }
    }
}

/**
 * 从本地文件加载点名统计数据
 * @return 按点名次数降序排列的 (姓名, 次数) 列表
 */
private fun loadStatistics(): List<Pair<String, Int>> {
    return try {
        val jsonData = FileHelper.readFromFile("D:/Xiaoye/StatisticalData.json")
        if (jsonData == "404") return emptyList()

        val type = object : TypeToken<Map<String, Int>>() {}.type
        val data: Map<String, Int> = Gson().fromJson(jsonData, type) ?: emptyMap()
        data.entries
            .map { it.key to it.value }
            .sortedByDescending { it.second }
    } catch (_: Exception) {
        emptyList()
    }
}
