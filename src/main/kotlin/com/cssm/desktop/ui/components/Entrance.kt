package com.cssm.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 卡片入场动画：淡入 + 轻微上浮，按 index 错开延迟。
 * 在 Lazy 网格的 item 里配合 key 使用，每个 item 只播放一次。
 */
@Composable
fun Modifier.cardEntrance(index: Int): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val delay = (index * 70).coerceAtMost(420)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 340, delayMillis = delay),
        label = "entrance-alpha"
    )
    val dy by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = tween(durationMillis = 340, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "entrance-dy"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = dy
    }
}
