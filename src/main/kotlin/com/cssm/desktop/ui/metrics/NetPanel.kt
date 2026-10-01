package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 子面板里的 Network / I/O 内容（iPad 风格）：
 * 标题 + ↓/↑ 速率 + 点状历史曲线。窄面板用 stacked 把曲线放下方整宽。
 */
@Composable
fun NetDottedPanel(
    label: String,
    down: String,
    up: String,
    downValues: List<Long>,
    upValues: List<Long>,
    sparkWidth: Dp = 110.dp,
    sparkHeight: Dp = 34.dp,
    stacked: Boolean = false,
    /** 总量行，如总流量；为空不显示 */
    footer: String = ""
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = label, fontSize = 12.sp, color = variant)
        Spacer(Modifier.height(6.dp))
        if (stacked) {
            Text(text = down, fontSize = 12.sp, color = Color(0xFF34C77B), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(text = up, fontSize = 12.sp, color = Color(0xFF0EA5E9), maxLines = 1)
            if (footer.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = footer,
                    fontSize = 10.sp,
                    color = variant.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(6.dp))
            Sparkline(
                down = downValues,
                up = upValues,
                width = sparkWidth,
                height = sparkHeight,
                dotted = true
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = down, fontSize = 12.sp, color = Color(0xFF34C77B), maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text(text = up, fontSize = 12.sp, color = Color(0xFF0EA5E9), maxLines = 1)
                    if (footer.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = footer,
                            fontSize = 10.sp,
                            color = variant.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Sparkline(
                    down = downValues,
                    up = upValues,
                    width = sparkWidth,
                    height = sparkHeight,
                    dotted = true
                )
            }
        }
    }
}

/** OS 名前的小状态点：在线绿 / 无数据琥珀 / 离线红（iPad 卡片同款琥珀点） */
@Composable
fun StatusDot(online: Boolean, hasData: Boolean) {
    val color = when {
        !online -> Color(0xFFFF453A)
        !hasData -> Color(0xFFD6985D)
        else -> Color(0xFF34C77B)
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}
