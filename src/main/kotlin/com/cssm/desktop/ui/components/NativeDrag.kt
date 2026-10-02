package com.cssm.desktop.ui.components

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import java.awt.Window

private const val WM_NCLBUTTONDOWN = 0x00A1
private const val HTCAPTION = 0x02

/**
 * Windows 原生标题栏拖拽：通过 WM_NCLBUTTONDOWN + HTCAPTION 把整个拖拽交给系统，
 * 获得原生行为（Aero Snap、多显示器、DPI 缩放、任务栏边缘吸附都由系统处理）。
 *
 * 这比自己用 setLocation 循环跟随鼠标可靠得多：自己循环会有坐标系换算、
 * 事件时序等问题，在某些机器上会导致窗口"消失"。
 *
 * 必须在非 EDT 线程调用（SendMessage 会进入模态拖拽循环直到松开鼠标）。
 * 非 Windows 平台或 JNA 加载失败时返回 false，调用方回退到 setLocation 循环。
 */
fun nativeCaptionDrag(window: Window): Boolean {
    if (!System.getProperty("os.name", "").startsWith("Windows", ignoreCase = true)) return false
    return try {
        val hwnd = WinDef.HWND(Native.getComponentPointer(window))
        User32.INSTANCE.SendMessage(
            hwnd,
            WM_NCLBUTTONDOWN,
            WinDef.WPARAM(HTCAPTION.toLong()),
            WinDef.LPARAM(0)
        )
        true
    } catch (_: Throwable) {
        false
    }
}

/** 在后台线程启动原生拖拽（避免阻塞调用线程的消息循环） */
fun startNativeCaptionDrag(window: Window) {
    Thread({ nativeCaptionDrag(window) }, "caption-drag").apply {
        isDaemon = true
        start()
    }
}
