package com.cssm.desktop.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * 安卓版 App 图标（终端提示符 >_）：运行时直接绘制，无需图片资源文件。
 * 用于窗口图标（任务栏/标题栏）。
 */
class AppIconPainter : Painter() {
    override val intrinsicSize: Size get() = Size(512f, 512f)

    override fun DrawScope.onDraw() {
        // 对应安卓 vector 的 108 viewport
        val k = size.width / 108f
        drawRoundRect(
            color = Color(0xFF0B0E14),
            cornerRadius = CornerRadius(112f * size.width / 512f, 112f * size.width / 512f)
        )
        val strokeW = 11f * k
        val chev = Color(0xFF7DD3FC)
        val under = Color(0xFF6EE7B7)
        // chevron 顶到边框：M10,22 L46,54 L10,86
        drawLine(chev, Offset(10 * k, 22 * k), Offset(46 * k, 54 * k), strokeW, StrokeCap.Round)
        drawLine(chev, Offset(46 * k, 54 * k), Offset(10 * k, 86 * k), strokeW, StrokeCap.Round)
        // underscore 顶到边框：M54,88 L98,88
        drawLine(under, Offset(54 * k, 88 * k), Offset(98 * k, 88 * k), strokeW, StrokeCap.Round)
    }
}
