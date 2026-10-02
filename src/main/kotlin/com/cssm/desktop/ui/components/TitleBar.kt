package com.cssm.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import kotlin.math.roundToInt

private val TitleBarHeight = 44.dp
private val WinButtonWidth = 46.dp
private const val MinWinW = 760
private const val MinWinH = 540

/**
 * 自定义标题栏（无边框窗口用）：左侧图标+标题可拖拽，双击切换最大化；
 * 右侧 Windows 风格最小化 / 最大化 / 关闭按钮（悬停高亮，关闭悬停红色）。
 */
@Composable
fun WindowScope.CustomTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.surface
    val fg = MaterialTheme.colorScheme.onSurface
    val maximized = state.placement == WindowPlacement.Maximized
    val toggleMaximize = {
        state.placement =
            if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TitleBarHeight)
                .background(bg)
                .pointerInput(window, maximized) {
                    detectTapGestures(onDoubleTap = { toggleMaximize() })
                }
                .pointerInput(window, maximized) {
                    if (maximized) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val loc = window.locationOnScreen
                        window.setLocation(
                            (loc.x + dragAmount.x).roundToInt(),
                            (loc.y + dragAmount.y).roundToInt()
                        )
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource("icon.png"),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cssm",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg
                )
            }
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                WinButton(
                    onClick = { state.isMinimized = true },
                    glyph = { c -> MinimizeGlyph(c) }
                )
                WinButton(
                    onClick = toggleMaximize,
                    glyph = { c -> MaximizeGlyph(c, maximized) }
                )
                WinButton(
                    onClick = onCloseRequest,
                    hoverBg = Color(0xFFE81123),
                    hoverFg = Color.White,
                    glyph = { c -> CloseGlyph(c) }
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            thickness = 1.dp
        )
    }
}

@Composable
private fun WinButton(
    onClick: () -> Unit,
    hoverBg: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    hoverFg: Color = MaterialTheme.colorScheme.onSurface,
    glyph: @Composable (Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .width(WinButtonWidth)
            .height(TitleBarHeight)
            .background(if (hovered) hoverBg else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(onClick) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        glyph(if (hovered && hoverBg == Color(0xFFE81123)) hoverFg else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MinimizeGlyph(color: Color) {
    Canvas(Modifier.size(11.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

@Composable
private fun MaximizeGlyph(color: Color, maximized: Boolean) {
    Canvas(Modifier.size(11.dp)) {
        if (maximized) {
            // 还原：两个错开的方框
            val s = 1.2.dp.toPx()
            drawRect(color = color, topLeft = Offset(3.dp.toPx(), 0f),
                size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(s))
            drawRect(color = color, topLeft = Offset(0f, 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(s))
        } else {
            drawRect(
                color = color,
                size = size,
                style = Stroke(1.2.dp.toPx())
            )
        }
    }
}

@Composable
private fun CloseGlyph(color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val s = 1.2.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), s)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), s)
    }
}

/**
 * 无边框窗口的边缘拖拽缩放（8dp 隐形手柄 + 方向光标），最大化时不显示。
 */
@Composable
fun WindowScope.WindowResizeHandles(
    minWidth: Int = MinWinW,
    minHeight: Int = MinWinH
) {
    val thickness = 8.dp
    val corner = 14.dp
    Box(Modifier.fillMaxSize()) {
        // 四条边
        ResizeEdge(
            modifier = Modifier.align(Alignment.CenterStart).width(thickness).fillMaxSize(),
            cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
        ) { dx, _ -> val b = window.bounds; val w = (b.width - dx).roundToInt().coerceAtLeast(minWidth); window.setBounds(b.x + b.width - w, b.y, w, b.height) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.CenterEnd).width(thickness).fillMaxSize(),
            cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
        ) { dx, _ -> val b = window.bounds; window.setBounds(b.x, b.y, (b.width + dx).roundToInt().coerceAtLeast(minWidth), b.height) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.TopCenter).height(thickness).fillMaxWidth(),
            cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
        ) { _, dy -> val b = window.bounds; val h = (b.height - dy).roundToInt().coerceAtLeast(minHeight); window.setBounds(b.x, b.y + b.height - h, b.width, h) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomCenter).height(thickness).fillMaxWidth(),
            cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)
        ) { _, dy -> val b = window.bounds; window.setBounds(b.x, b.y, b.width, (b.height + dy).roundToInt().coerceAtLeast(minHeight)) }
        // 四个角
        ResizeEdge(
            modifier = Modifier.align(Alignment.TopStart).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)
        ) { dx, dy -> val b = window.bounds; val w = (b.width - dx).roundToInt().coerceAtLeast(minWidth); val h = (b.height - dy).roundToInt().coerceAtLeast(minHeight); window.setBounds(b.x + b.width - w, b.y + b.height - h, w, h) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.TopEnd).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)
        ) { dx, dy -> val b = window.bounds; val h = (b.height - dy).roundToInt().coerceAtLeast(minHeight); window.setBounds(b.x, b.y + b.height - h, (b.width + dx).roundToInt().coerceAtLeast(minWidth), h) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomStart).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)
        ) { dx, dy -> val b = window.bounds; val w = (b.width - dx).roundToInt().coerceAtLeast(minWidth); window.setBounds(b.x + b.width - w, b.y, w, (b.height + dy).roundToInt().coerceAtLeast(minHeight)) }
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomEnd).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
        ) { dx, dy -> val b = window.bounds; window.setBounds(b.x, b.y, (b.width + dx).roundToInt().coerceAtLeast(minWidth), (b.height + dy).roundToInt().coerceAtLeast(minHeight)) }
    }
}

@Composable
private fun WindowScope.ResizeEdge(
    modifier: Modifier,
    cursor: Cursor,
    onDrag: (dx: Float, dy: Float) -> Unit
) {
    // AwtCursor 是 internal，改用 hover 时直接设置 window.cursor
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    androidx.compose.runtime.LaunchedEffect(hovered) {
        window.cursor = if (hovered) cursor else Cursor.getDefaultCursor()
    }
    Box(
        modifier = modifier
            .hoverable(interaction)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    )
}
