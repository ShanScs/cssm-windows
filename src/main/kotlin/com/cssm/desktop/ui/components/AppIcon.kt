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
        val strokeW = 12f * k
        val chev = Color(0xFF7DD3FC)
        val under = Color(0xFF6EE7B7)
        // 图形顶满深色方块：chevron M4,12 L48,54 L4,96；underscore M52,96 L104,96
        drawLine(chev, Offset(4 * k, 12 * k), Offset(48 * k, 54 * k), strokeW, StrokeCap.Round)
        drawLine(chev, Offset(48 * k, 54 * k), Offset(4 * k, 96 * k), strokeW, StrokeCap.Round)
        drawLine(under, Offset(52 * k, 96 * k), Offset(104 * k, 96 * k), strokeW, StrokeCap.Round)
    }
}
