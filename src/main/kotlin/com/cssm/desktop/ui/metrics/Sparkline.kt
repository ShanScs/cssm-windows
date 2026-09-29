package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 迷你历史曲线（sparkline）：网络 / 磁盘 IO 用。
 * 双线：下行（蓝色）+ 上行（绿色），iPad 版同款信息密度。
 */
@Composable
fun Sparkline(
    down: List<Long>,
    up: List<Long>,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 28.dp,
    downColor: Color = Color(0xFF5B8DEF),
    upColor: Color = Color(0xFF34C77B)
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        // 背景圆角（简化为透明底，iPad 版有浅灰底）
        val all = (down + up)
        val max = (all.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()

        fun pathFor(values: List<Long>): Path {
            val path = Path()
            if (values.isEmpty()) return path
            val n = values.size
            // 只有一个点时画在最右
            val stepX = if (n > 1) w / (n - 1) else 0f
            values.forEachIndexed { i, v ->
                val x = if (n > 1) i * stepX else w
                val y = h - (v.toFloat() / max) * (h - 4.dp.toPx()) - 2.dp.toPx()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        val stroke = 1.5.dp.toPx()
        drawPath(pathFor(down), color = downColor, style = Stroke(stroke))
        drawPath(pathFor(up), color = upColor, style = Stroke(stroke))
    }
}

/**
 * 单线面积图：I/O 用（iPad 版底部小面积图）。
 */
@Composable
fun AreaChart(
    values: List<Long>,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 28.dp,
    lineColor: Color = Color(0xFF5B8DEF),
    fillColor: Color = Color(0xFF5B8DEF).copy(alpha = 0.15f)
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val n = values.size
        val stepX = if (n > 1) w / (n - 1) else 0f

        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = if (n > 1) i * stepX else w
            val y = h - (v.toFloat() / max) * (h - 4.dp.toPx()) - 2.dp.toPx()
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        // 面积填充
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(fillPath, color = fillColor)
        drawPath(linePath, color = lineColor, style = Stroke(1.5.dp.toPx()))
    }
}
