package com.ultrazg.xyztv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.tvFocusable(
    scale: Float = 1.06f,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scaleAnimated by animateFloatAsState(
        targetValue = if (isFocused && enabled) scale else 1f,
        label = "tv_focus_scale"
    )

    this
        .scale(scaleAnimated)
        .focusable(interactionSource = interactionSource, enabled = enabled)
}

fun Modifier.tvFocusableWithShadow(
    scale: Float = 1.06f,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scaleAnimated by animateFloatAsState(
        targetValue = if (isFocused && enabled) scale else 1f,
        label = "tv_focus_shadow_scale"
    )

    this
        .graphicsLayer {
            scaleX = scaleAnimated
            scaleY = scaleAnimated
            shadowElevation = if (isFocused && enabled) 12f else 0f
        }
        .focusable(interactionSource = interactionSource, enabled = enabled)
}
