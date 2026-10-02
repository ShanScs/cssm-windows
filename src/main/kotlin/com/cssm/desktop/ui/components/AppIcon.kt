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
        val strokeW = 10f * k
        val chev = Color(0xFF7DD3FC)
        val under = Color(0xFF6EE7B7)
        // chevron 放大到接近边框：M22,30 L52,54 L22,78
        drawLine(chev, Offset(22 * k, 30 * k), Offset(52 * k, 54 * k), strokeW, StrokeCap.Round)
        drawLine(chev, Offset(52 * k, 54 * k), Offset(22 * k, 78 * k), strokeW, StrokeCap.Round)
        // underscore 放大：M60,80 L92,80
        drawLine(under, Offset(60 * k, 80 * k), Offset(92 * k, 80 * k), strokeW, StrokeCap.Round)
    }
}
