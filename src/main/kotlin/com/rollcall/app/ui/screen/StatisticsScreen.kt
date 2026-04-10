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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper

/**
 * 增强版点名统计面板
 * 包含概要统计卡片、排名榜和可视化图表
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
    val avgCalls = if (statsData.isNotEmpty()) totalCalls.toFloat() / statsData.size else 0f
    val aboveAvg = statsData.count { it.second > avgCalls }

    Window(
        onCloseRequest = onClose,
        title = "点名统计",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(580.dp, 780.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 概要统计卡片
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        icon = "🏆",
                        label = "最多",
                        value = if (statsData.isNotEmpty()) "${statsData[0].second}次" else "-",
                        subLabel = if (statsData.isNotEmpty()) statsData[0].first else "",
                        accentColor = Color(0xFFFFD700),
                        colors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        icon = "📈",
                        label = "平均",
                        value = "%.1f次".format(avgCalls),
                        subLabel = "${aboveAvg}人超均",
                        accentColor = themeColors.primary,
                        colors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        icon = "📉",
                        label = "最少",
                        value = if (statsData.isNotEmpty()) "${statsData.last().second}次" else "-",
                        subLabel = if (statsData.isNotEmpty()) statsData.last().first else "",
                        accentColor = themeColors.accent,
                        colors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 分布柱状图（顶部前10名缩略图）
                if (statsData.size >= 3) {
                    MiniBarChart(
                        data = statsData.take(10),
                        maxCount = maxCount,
                        colors = themeColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📭", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "暂无点名记录",
                                        fontSize = 18.sp,
                                        color = themeColors.textHint
                                    )
                                }
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
                                avgCount = avgCalls,
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
 * 概要统计卡片
 */
@Composable
private fun StatSummaryCard(
    icon: String,
    label: String,
    value: String,
    subLabel: String,
    accentColor: Color,
    colors: com.rollcall.app.ui.theme.AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = colors.textHint, fontWeight = FontWeight.Medium)
        Text(
            value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (subLabel.isNotEmpty()) {
            Text(
                subLabel, fontSize = 11.sp, color = accentColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 迷你柱状图（前10名）
 */
@Composable
private fun MiniBarChart(
    data: List<Pair<String, Int>>,
    maxCount: Int,
    colors: com.rollcall.app.ui.theme.AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            "📊 点名频次分布（前${data.size}名）",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, (name, count) ->
                val fraction = count.toFloat() / maxCount.coerceAtLeast(1)
                val barColor = when (index) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> colors.primary.copy(alpha = 0.6f + index * 0.02f)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        "$count",
                        fontSize = 9.sp,
                        color = colors.textHint,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((fraction * 55).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (name.length > 2) name.take(1) + "…" else name,
                        fontSize = 8.sp,
                        color = colors.textHint,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 增强统计行组件
 * 包含排名、姓名、次数、可视化进度条和高于/低于平均标记
 */
@Composable
private fun StatisticsRow(
    rank: Int,
    name: String,
    count: Int,
    maxCount: Int,
    avgCount: Float,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    val progress = count.toFloat() / maxCount.coerceAtLeast(1)
    val isAboveAvg = count > avgCount

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

    val rankIcon = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
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
            if (rankIcon.isNotEmpty()) {
                Text(rankIcon, fontSize = 18.sp)
            } else {
                Text(
                    text = "$rank",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 姓名和进度条
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 高于平均指示
                    if (isAboveAvg && rank > 3) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.success.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("↑", fontSize = 10.sp, color = colors.success)
                        }
                    }
                }
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
