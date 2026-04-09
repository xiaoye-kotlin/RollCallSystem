package com.rollcall.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 现代化卡片组件
 * 带有柔和阴影效果，适合学校大屏幕显示
 *
 * @param modifier 修饰符
 * @param backgroundColor 背景颜色
 * @param cornerRadius 圆角大小
 * @param content 卡片内容
 */
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                // 柔和阴影效果
                val radius = cornerRadius.toPx()
                drawRoundRect(
                    color = Color(0x1A000000),
                    topLeft = Offset(2f, 4f),
                    size = size.copy(width = size.width, height = size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        content = content
    )
}

/**
 * 状态提示条组件
 * 用于显示加载进度、错误信息等
 *
 * @param text 提示文字
 * @param isLoading 是否为加载状态（绿色），否则为错误状态（红色）
 */
@Composable
fun StatusBanner(
    text: String,
    isLoading: Boolean = true
) {
    val backgroundColor = if (isLoading) {
        Color(0xFF4CAF50).copy(alpha = 0.85f)
    } else {
        Color(0xFFE74C3C).copy(alpha = 0.85f)
    }

    Box(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 倒计时选项按钮
 * 现代化圆角按钮样式
 */
@Composable
fun CountdownOptionButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF0F2F5)
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2D2D3F)
        )
    }
}
