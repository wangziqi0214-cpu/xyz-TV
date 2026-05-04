package com.ultrazg.xyztv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 极简人像头图标：圆形头部 + 半圆肩膀轮廓
 */
@Composable
fun MinimalPersonIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()

        // 头部（圆形）
        val headRadius = w * 0.22f
        val headCenterX = w * 0.5f
        val headCenterY = w * 0.28f
        drawCircle(color = color, radius = headRadius, center = Offset(headCenterX, headCenterY))

        // 肩膀（半圆弧，下方居中）
        val shoulderPath = Path().apply {
            moveTo(w * 0.14f, w * 0.88f)
            cubicTo(
                w * 0.14f, w * 0.52f,
                w * 0.86f, w * 0.52f,
                w * 0.86f, w * 0.88f
            )
            close()
        }
        drawPath(shoulderPath, color)
    }
}
