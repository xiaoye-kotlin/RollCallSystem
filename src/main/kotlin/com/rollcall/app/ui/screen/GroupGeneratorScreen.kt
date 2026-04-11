package com.rollcall.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
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
import com.rollcall.app.data.model.parseStudentJson
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper

/**
 * 随机分组面板
 * 将全班同学随机分成若干小组
 * 支持自定义组数和重新分组
 *
 * @param onClose 关闭面板回调
 */
@Composable
fun GroupGeneratorPanel(
    onClose: () -> Unit
) {
    // 加载学生列表
    val allStudents = remember { loadStudentNames() }
    val groupOptions = remember(allStudents) { buildGroupOptions(allStudents.size) }
    var groupCount by remember(groupOptions) {
        mutableStateOf(defaultGroupCount(groupOptions))
    }
    var groups by remember { mutableStateOf(generateGroups(allStudents, groupCount)) }
    var refreshCounter by remember { mutableStateOf(0) }
    val groupSizeSummary = remember(groups) { summarizeGroupSizes(groups) }

    LaunchedEffect(allStudents, groupCount, refreshCounter) {
        groups = generateGroups(allStudents, groupCount)
    }

    // 每组使用不同的强调色
    val groupColors = listOf(
        Color(0xFF5B8DEF), // 蓝
        Color(0xFFFF7E6B), // 珊瑚
        Color(0xFF2ECC71), // 绿
        Color(0xFFFBBF24), // 琥珀
        Color(0xFF9B59B6), // 紫
        Color(0xFFE74C3C), // 红
        Color(0xFF1ABC9C), // 青
        Color(0xFFF39C12), // 橙
    )

    Window(
        onCloseRequest = onClose,
        title = "随机分组",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(580.dp, 700.dp)
        ),
    ) {
        com.rollcall.app.ui.theme.AppTheme {
            val colors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(colors.gradient1, colors.gradient2)
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
                            "🎲 随机分组",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "共 ${allStudents.size} 位同学 · $groupCount 个小组 · $groupSizeSummary",
                            fontSize = 14.sp,
                            color = colors.textHint
                        )
                    }

                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 重新分组
                        IconButton(onClick = {
                            refreshCounter++
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重新分组",
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        // 关闭
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 组数选择
                groupOptions.chunked(5).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = if (rowIndex == 0) 4.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { n ->
                            val isSelected = n == groupCount
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) colors.primary.copy(alpha = 0.15f)
                                        else colors.cardBackground
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primary.copy(alpha = 0.4f)
                                        else colors.cardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = allStudents.isNotEmpty()) {
                                        if (groupCount != n) {
                                            groupCount = n
                                            refreshCounter++
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${n}组",
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.textSecondary
                                )
                            }
                        }
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (rowIndex != groupOptions.chunked(5).lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 分组列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (allStudents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无学生数据",
                                    fontSize = 18.sp,
                                    color = colors.textHint
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = groups,
                            key = { index, _ -> "group_${index}_$refreshCounter" }
                        ) { index, group ->
                            GroupCard(
                                groupIndex = index + 1,
                                members = group,
                                accentColor = groupColors[index % groupColors.size],
                                colors = colors
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 小组卡片组件
 */
@Composable
private fun GroupCard(
    groupIndex: Int,
    members: List<String>,
    accentColor: Color,
    colors: com.rollcall.app.ui.theme.AppColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // 组标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$groupIndex",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "第 $groupIndex 组",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "(${members.size}人)",
                fontSize = 14.sp,
                color = colors.textHint
            )
        }

        Spacer(Modifier.height(10.dp))

        // 成员列表 - 使用流式布局
        val rowSize = 4
        val rows = members.chunked(rowSize)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { name ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            name,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 生成随机分组
 */
private fun generateGroups(students: List<String>, groupCount: Int): List<List<String>> {
    if (students.isEmpty() || groupCount <= 0) return emptyList()
    val shuffled = students.shuffled()
    val groups = MutableList(groupCount) { mutableListOf<String>() }
    shuffled.forEachIndexed { index, name ->
        groups[index % groupCount].add(name)
    }
    return groups
}

private fun buildGroupOptions(studentCount: Int): List<Int> {
    if (studentCount <= 1) return listOf(1)
    return (2..studentCount.coerceAtMost(12)).toList()
}

private fun summarizeGroupSizes(groups: List<List<String>>): String {
    if (groups.isEmpty()) return "暂无分组"

    val sizes = groups.map { it.size }
    val min = sizes.minOrNull() ?: 0
    val max = sizes.maxOrNull() ?: 0
    return if (min == max) {
        "每组${min}人"
    } else {
        "每组${min}-${max}人"
    }
}

/**
 * 从本地文件加载学生姓名列表
 */
private fun loadStudentNames(): List<String> {
    return try {
        val jsonData = FileHelper.readFromFile("D:/Xiaoye/NameList.json")
        if (jsonData == "404") return emptyList()
        val students = parseStudentJson(jsonData)
        students.filter { it.name.isNotEmpty() }.map { it.name }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun defaultGroupCount(groupOptions: List<Int>): Int {
    return groupOptions.firstOrNull { it >= 4 }
        ?: groupOptions.lastOrNull()
        ?: 1
}
