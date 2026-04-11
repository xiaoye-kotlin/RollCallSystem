package com.rollcall.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rollcall.app.ui.theme.AppTheme

/**
 * 现代化玻璃拟态卡片组件
 * 带有柔和阴影和半透明效果，支持深浅主题
 *
 * @param modifier 修饰符
 * @param backgroundColor 背景颜色（默认使用主题卡片色）
 * @param borderColor 边框颜色（默认使用主题卡片边框色）
 * @param cornerRadius 圆角大小
 * @param content 卡片内容
 */
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colors.cardBackground,
    borderColor: Color = AppTheme.colors.cardBorder,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shadowColor = AppTheme.colors.shadowLight

    Box(
        modifier = modifier
            .drawBehind {
                val radius = cornerRadius.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(2f, 4f),
                    size = size.copy(width = size.width, height = size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

/**
 * 状态提示条组件
 * 用于显示加载进度、错误信息等
 * 支持成功/错误/警告三种状态
 *
 * @param text 提示文字
 * @param isLoading 是否为加载状态
 */
@Composable
fun StatusBanner(
    text: String,
    isLoading: Boolean = true
) {
    val colors = AppTheme.colors
    val backgroundColor = if (isLoading) {
        colors.success.copy(alpha = 0.85f)
    } else {
        colors.error.copy(alpha = 0.85f)
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
 * 现代化圆角按钮，支持主题色
 */
@Composable
fun CountdownOptionButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colors.surfaceVariant
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.textPrimary
        )
    }
}

/**
 * 渐变进度指示器
 * 使用微光动画效果，用于加载页面
 */
@Composable
fun ShimmerProgressBar(
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    val colors = AppTheme.colors
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.surfaceVariant)
    ) {
        // 实际进度条
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.primary,
                            colors.accent,
                            colors.primary
                        )
                    )
                )
        )
        // 微光效果
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .offset(x = shimmerOffset.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.shimmer,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 图标标签按钮
 * 用于更多功能面板等场景
 */
@Composable
fun IconLabelButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    ModernCard(
        modifier = modifier
            .height(90.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 图标圆圈
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }
    }
}

/**
 * 分节标题组件
 * 用于功能面板等区域的分组标题
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.colors.textHint,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
