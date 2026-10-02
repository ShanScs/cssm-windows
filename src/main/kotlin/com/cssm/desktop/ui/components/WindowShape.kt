package com.cssm.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * 窗口诊断日志：记录最小化/隐藏/显示事件，排查"拖拽后窗口消失"。
 * 日志写到 ~/.cssm/window-diag.log，用户复现后把这个文件发回来即可定位。
 */
@Composable
fun WindowScope.WindowDisappearDiagnose(state: WindowState) {
    DisposableEffect(window) {
        val listener = object : WindowAdapter() {
            override fun windowIconified(e: WindowEvent) =
                diagLog("ICONIFIED", "state.isMinimized=${state.isMinimized}")

            override fun windowDeiconified(e: WindowEvent) =
                diagLog("DEICONIFIED", "state.isMinimized=${state.isMinimized}")

            override fun windowActivated(e: WindowEvent) =
                diagLog("ACTIVATED", "")
        }
        val compListener = object : ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent) =
                diagLog("HIDDEN", "state.isMinimized=${state.isMinimized}")

            override fun componentShown(e: ComponentEvent) =
                diagLog("SHOWN", "")
        }
        window.addWindowListener(listener)
        window.addComponentListener(compListener)
        diagLog("LISTENER_ATTACHED", "startup")
        onDispose {
            window.removeWindowListener(listener)
            window.removeComponentListener(compListener)
        }
    }
}

private fun WindowScope.diagLog(event: String, extra: String) {
    try {
        val w = window
        val dir = java.io.File(System.getProperty("user.home"), ".cssm")
        dir.mkdirs()
        val log = java.io.File(dir, "window-diag.log")
        if (log.length() > 200_000) log.delete() // 防止无限增长
        val b = w.bounds
        log.appendText(
            "${java.time.LocalDateTime.now()} $event bounds=$b " +
                "showing=${w.isShowing} visible=${w.isVisible} $extra\n"
        )
    } catch (_: Exception) {
    }
}
