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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rollcall.app.ui.theme.AppTheme

/**
 * 更多功能面板 - 现代化倒计时选择
 * 使用主题色，带图标和精致的视觉层次
 */
@Composable
fun moreFunction() {
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
            verticalArrangement = Arrangement.Center
        ) {
            // 标题区域
            Text(
                text = "⏱",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "倒计时",
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "选择时长开始计时",
                fontSize = 14.sp,
                color = colors.textHint,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 倒计时选项
            val options = listOf(
                Triple("1", "分钟", colors.primary),
                Triple("3", "分钟", colors.accent),
                Triple("5", "分钟", colors.success),
                Triple("10", "分钟", colors.warning)
            )

            options.forEach { (value, unit, accentColor) ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .height(70.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 数字圆圈
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
        }
    }
}