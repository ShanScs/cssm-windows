package com.cssm.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import java.awt.GraphicsEnvironment
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
        ) {
            // 拖拽层：只覆盖按钮以外的区域，避免误触最小化/最大化/关闭
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = WinButtonWidth * 3)
                    .pointerInput(window, maximized) {
                        detectTapGestures(onDoubleTap = { toggleMaximize() })
                    }
                    .pointerInput(window, maximized) {
                        if (maximized) return@pointerInput
                        awaitEachGesture {
                            // requireUnconsumed = false：即使按下瞬间先被按钮消费，
                            // 拖拽手势依然能接管
                            val down = awaitFirstDown(requireUnconsumed = false)
                            awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            } ?: return@awaitEachGesture
                            // 优先 Windows 原生标题栏拖拽（JNA 缺失/异常时回退到 setLocation 循环）
                            if (!tryNativeCaptionDrag(window)) {
                                var prev = down.position
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val delta = change.position - prev
                                    prev = change.position
                                    change.consume()
                                    val loc = window.locationOnScreen
                                    val (nx, ny) = clampPosition(
                                        window,
                                        (loc.x + delta.x).roundToInt(),
                                        (loc.y + delta.y).roundToInt()
                                    )
                                    window.setLocation(nx, ny)
                                }
                            }
                        }
                    }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                val appIcon = remember { AppIconPainter() }
                androidx.compose.foundation.Image(
                    painter = appIcon,
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

/**
 * 把窗口位置钳制在屏幕可见范围内：至少保留 160px 宽度和标题栏可见，
 * 窗口永远不会被拖出屏幕"消失"。
 */
private fun clampPosition(window: java.awt.Window, x: Int, y: Int): Pair<Int, Int> {
    val b = window.graphicsConfiguration?.bounds
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val w = window.width.coerceAtLeast(1)
    val nx = x.coerceIn(b.x - w + 160, b.x + b.width - 160)
    val ny = y.coerceIn(b.y, b.y + b.height - 60)
    return nx to ny
}

/** 缩放时同样钳制：窗口主体永远留在屏幕内 */
private fun clampBounds(window: java.awt.Window, x: Int, y: Int, w: Int, h: Int): java.awt.Rectangle {
    val b = window.graphicsConfiguration?.bounds
        ?: GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val nw = w.coerceAtLeast(MinWinW)
    val nh = h.coerceAtLeast(MinWinH)
    val nx = x.coerceIn(b.x - nw + 160, b.x + b.width - 160)
    val ny = y.coerceIn(b.y, b.y + b.height - 60)
    return java.awt.Rectangle(nx, ny, nw, nh)
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
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    // 只有"干净"的点按才触发：按住按钮拖拽窗口时不误触
                    // （位移超过 touchSlop 说明用户在拖拽，直接吞掉这次点击）
                    if (up != null &&
                        (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                    ) {
                        onClick()
                    }
                }
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
 * 顶部不放手柄：标题栏占满顶部，顶边缩放在桌面端几乎用不到，还会和拖拽/按钮打架。
 */
@Composable
fun WindowScope.WindowResizeHandles(
    minWidth: Int = MinWinW,
    minHeight: Int = MinWinH
) {
    val thickness = 8.dp
    val corner = 14.dp
    fun edgeBounds(x: Int, y: Int, w: Int, h: Int) {
        window.bounds = clampBounds(window, x, y, w, h)
    }
    Box(Modifier.fillMaxSize()) {
        // 左右边
        ResizeEdge(
            modifier = Modifier.align(Alignment.CenterStart).width(thickness).fillMaxSize(),
            cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
        ) { dx, _ ->
            val b = window.bounds
            val w = (b.width - dx).roundToInt().coerceAtLeast(minWidth)
            edgeBounds(b.x + b.width - w, b.y, w, b.height)
        }
        ResizeEdge(
            modifier = Modifier.align(Alignment.CenterEnd).width(thickness).fillMaxSize(),
            cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
        ) { dx, _ ->
            val b = window.bounds
            edgeBounds(b.x, b.y, (b.width + dx).roundToInt().coerceAtLeast(minWidth), b.height)
        }
        // 底边
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomCenter).height(thickness).fillMaxWidth(),
            cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)
        ) { _, dy ->
            val b = window.bounds
            edgeBounds(b.x, b.y, b.width, (b.height + dy).roundToInt().coerceAtLeast(minHeight))
        }
        // 底部两角
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomStart).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)
        ) { dx, dy ->
            val b = window.bounds
            val w = (b.width - dx).roundToInt().coerceAtLeast(minWidth)
            edgeBounds(b.x + b.width - w, b.y, w, (b.height + dy).roundToInt().coerceAtLeast(minHeight))
        }
        ResizeEdge(
            modifier = Modifier.align(Alignment.BottomEnd).size(corner),
            cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
        ) { dx, dy ->
            val b = window.bounds
            edgeBounds(b.x, b.y, (b.width + dx).roundToInt().coerceAtLeast(minWidth), (b.height + dy).roundToInt().coerceAtLeast(minHeight))
        }
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
