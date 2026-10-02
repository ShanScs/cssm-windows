package com.cssm.desktop.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 桌面端卡片 hover 高亮：鼠标悬停时显示一圈描边（不改变布局尺寸）。
 * 放在 clickable 之前调用。
 */
@Composable
fun Modifier.hoverBorder(
    shape: Shape = RoundedCornerShape(16.dp),
    hoverColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    return this
        .hoverable(interactionSource)
        .border(
            width = if (hovered) 1.5.dp else 0.dp,
            color = if (hovered) hoverColor else Color.Transparent,
            shape = shape
        )
}
