package com.cssm.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 卡片网格的居中容器：宽屏下把内容宽度钳在 [maxWidth] 内居中，
 * 卡片不再被横向拉伸成宽条；窗口较窄时占满可用宽度。
 */
@Composable
fun CenteredGridContainer(
    maxWidth: Dp = 1280.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            content()
        }
    }
}
