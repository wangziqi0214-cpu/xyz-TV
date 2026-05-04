package com.ultrazg.xyztv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一视觉面积的侧边栏图标集
 * 所有图标在相同的 size 画布内绘制，视觉重量一致
 */

/** 首页 — 简化房屋：三角屋顶 + 矩形房身 */
@Composable
fun HomeIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        // 屋顶三角
        val roof = Path().apply {
            moveTo(w * 0.5f, w * 0.08f)
            lineTo(w * 0.92f, w * 0.44f)
            lineTo(w * 0.08f, w * 0.44f)
            close()
        }
        drawPath(roof, color)
        // 房身矩形
        drawRect(color, topLeft = Offset(w * 0.2f, w * 0.44f), size = Size(w * 0.6f, w * 0.48f))
    }
}

/** 搜索 — 放大镜：圆环 + 斜把手 */
@Composable
fun SearchIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val stroke = w * 0.1f
        // 圆环
        drawCircle(color = color, radius = w * 0.3f, center = Offset(w * 0.38f, w * 0.38f), style = Stroke(width = stroke))
        // 把手
        val handle = Path().apply {
            moveTo(w * 0.6f, w * 0.6f)
            lineTo(w * 0.9f, w * 0.9f)
        }
        drawPath(handle, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** 订阅 — 层叠方块 */
@Composable
fun SubscribeIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val s = w * 0.38f
        // 后层（偏移）
        drawRect(color, topLeft = Offset(w * 0.32f, w * 0.12f), size = Size(s, s))
        // 前层
        drawRect(color, topLeft = Offset(w * 0.18f, w * 0.3f), size = Size(s, s))
    }
}

/** 分类 — 四宫格 */
@Composable
fun CategoryIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val gap = w * 0.08f
        val cellW = (w - gap * 3f) / 2f
        // 四个圆角方块
        for (row in 0..1) {
            for (col in 0..1) {
                val left = gap + col * (cellW + gap)
                val top = gap + row * (cellW + gap)
                drawRect(color, topLeft = Offset(left, top), size = Size(cellW, cellW))
            }
        }
    }
}

/** 历史 — 时钟：圆环 + 时针分针 */
@Composable
fun HistoryIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val stroke = w * 0.09f
        val cx = w * 0.5f
        val cy = w * 0.5f
        val r = w * 0.38f
        // 圆环
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
        // 时针
        val hour = Path().apply { moveTo(cx, cy); lineTo(cx, cy - r * 0.5f) }
        drawPath(hour, color, style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round))
        // 分针
        val minute = Path().apply { moveTo(cx, cy); lineTo(cx + r * 0.45f, cy - r * 0.15f) }
        drawPath(minute, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** 设置 — 齿轮：外圈齿 + 中心圆 */
@Composable
fun SettingsIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val cx = w * 0.5f
        val cy = w * 0.5f
        val outerR = w * 0.4f
        val innerR = w * 0.26f
        val teeth = 6
        val toothW = w * 0.1f

        // 齿轮外圈（齿 + 环）
        val gear = Path().apply {
            for (i in 0 until teeth) {
                val angle = Math.toRadians((i * 360.0 / teeth) - 90.0)
                val nextAngle = Math.toRadians(((i + 0.5) * 360.0 / teeth) - 90.0)
                val midAngle = Math.toRadians(((i + 1.0) * 360.0 / teeth) - 90.0)

                val cosA = kotlin.math.cos(angle).toFloat()
                val sinA = kotlin.math.sin(angle).toFloat()
                val cosN = kotlin.math.cos(nextAngle).toFloat()
                val sinN = kotlin.math.sin(nextAngle).toFloat()
                val cosM = kotlin.math.cos(midAngle).toFloat()
                val sinM = kotlin.math.sin(midAngle).toFloat()

                // 齿顶
                val tipR = outerR + toothW * 0.5f
                if (i == 0) moveTo(cx + outerR * cosA, cy + outerR * sinA)
                lineTo(cx + tipR * cosA, cy + tipR * sinA)
                lineTo(cx + tipR * cosN, cy + tipR * sinN)
                lineTo(cx + outerR * cosN, cy + outerR * sinN)
                // 内弧到下一齿
                lineTo(cx + outerR * cosM, cy + outerR * sinM)
            }
            close()
        }
        drawPath(gear, color)
        // 中心圆孔
        drawCircle(color = color, radius = innerR, center = Offset(cx, cy), style = Stroke(width = w * 0.07f))
    }
}

/** 人像 — 圆形头部 + 半圆肩膀（视觉居中版本） */
@Composable
fun PersonIcon(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color.Unspecified) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        // 头部
        val headRadius = w * 0.2f
        val headCenterX = w * 0.5f
        val headCenterY = w * 0.3f
        drawCircle(color = color, radius = headRadius, center = Offset(headCenterX, headCenterY))
        // 肩膀
        val shoulder = Path().apply {
            moveTo(w * 0.12f, w * 0.92f)
            cubicTo(w * 0.12f, w * 0.52f, w * 0.88f, w * 0.52f, w * 0.88f, w * 0.92f)
            close()
        }
        drawPath(shoulder, color)
    }
}
