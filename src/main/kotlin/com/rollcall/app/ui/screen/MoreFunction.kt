package com.rollcall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rollcall.app.ui.theme.AppTheme

const val DROP_TARGET_NONE = 0
const val DROP_TARGET_QUICK_TOOLS = 100
const val DROP_TARGET_OCR = 101

@Composable
fun moreFunction(isCountDownEnabled: Boolean, currentDropTarget: Int) {
    val colors = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "🧰",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "拖拽投放",
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "拖到对应区域后松手执行",
                fontSize = 14.sp,
                color = colors.textHint,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            ToolDropZone(colors, currentDropTarget)

            Spacer(modifier = Modifier.height(14.dp))
            OcrDropZone(colors, currentDropTarget)

            if (isCountDownEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⏱",
                    fontSize = 36.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "倒计时",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "拖到对应时长区域后松手",
                    fontSize = 14.sp,
                    color = colors.textHint,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val options = listOf(
                    CountdownOption("1", "分钟", colors.primary, 1),
                    CountdownOption("3", "分钟", colors.accent, 2),
                    CountdownOption("5", "分钟", colors.success, 3),
                    CountdownOption("10", "分钟", colors.warning, 4)
                )

                options.forEach { option ->
                    DropZoneCard(
                        value = option.value,
                        unit = option.unit,
                        accentColor = option.accentColor,
                        colors = colors,
                        targetType = option.targetType,
                        currentDropTarget = currentDropTarget
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "倒计时已在点击面板中开启",
                        fontSize = 14.sp,
                        color = colors.textHint
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolDropZone(
    colors: com.rollcall.app.ui.theme.AppColors,
    currentDropTarget: Int
) {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .height(86.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (currentDropTarget == DROP_TARGET_QUICK_TOOLS) {
                    colors.primary.copy(alpha = 0.18f)
                } else {
                    colors.cardBackground
                }
            )
            .border(
                2.dp,
                if (currentDropTarget == DROP_TARGET_QUICK_TOOLS) {
                    colors.primary
                } else {
                    colors.primary.copy(alpha = 0.35f)
                },
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌘",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "快捷工具",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "统计 / 分组 / 抽题 / 噪音 / 倒计时",
                    fontSize = 13.sp,
                    color = colors.textHint
                )
            }
        }
    }
}

@Composable
private fun OcrDropZone(
    colors: com.rollcall.app.ui.theme.AppColors,
    currentDropTarget: Int
) {
    val isActive = currentDropTarget == DROP_TARGET_OCR
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .height(108.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) {
                    Color(0xFF8E44AD).copy(alpha = 0.18f)
                } else {
                    colors.cardBackground
                }
            )
            .border(
                2.dp,
                if (isActive) Color(0xFF8E44AD) else Color(0xFF8E44AD).copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E44AD).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OCR",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF8E44AD)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "生词识别",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "拖到这里松手，直接开始截图分析",
                    fontSize = 13.sp,
                    color = colors.textHint
                )
            }
        }
    }
}

@Composable
private fun DropZoneCard(
    value: String,
    unit: String,
    accentColor: Color,
    colors: com.rollcall.app.ui.theme.AppColors,
    targetType: Int,
    currentDropTarget: Int
) {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .height(70.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (currentDropTarget == targetType) {
                    accentColor.copy(alpha = 0.16f)
                } else {
                    colors.cardBackground
                }
            )
            .border(
                2.dp,
                if (currentDropTarget == targetType) {
                    accentColor
                } else {
                    accentColor.copy(alpha = 0.3f)
                },
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$value $unit",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }
    }
}

private data class CountdownOption(
    val value: String,
    val unit: String,
    val accentColor: Color,
    val targetType: Int
)
