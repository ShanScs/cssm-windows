package com.cssm.desktop.ui.notify

import com.cssm.desktop.data.AlertStore
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.metrics.MetricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * 告警引擎：App 级后台协程，每 30 秒检查一次告警规则。
 * 触发时写入通知中心 + Windows 右下角托盘气泡。
 */
object NotifyEngine {

    fun start(
        scope: CoroutineScope,
        alertStore: AlertStore,
        serverStore: ServerStore,
        metricsRepo: MetricsRepository
    ) {
        scope.launch {
            val offlineFired = mutableSetOf<Long>()
            val cpuFiredAt = mutableMapOf<Long, Long>()
            while (isActive) {
                try {
                    val rules = alertStore.rules.value.filter { it.enabled }
                    if (rules.isNotEmpty()) {
                        val states = metricsRepo.states.value
                        val servers = serverStore.servers.value.associateBy { it.id }
                        for (rule in rules) {
                            val st = states[rule.serverId] ?: continue
                            val server = servers[rule.serverId] ?: continue
                            when (rule.type) {
                                "offline" -> {
                                    // 曾经采集到数据、现在掉线 -> 告警；恢复后清标记
                                    if (st.stats != null && !st.online) {
                                        if (offlineFired.add(rule.serverId)) {
                                            fire(alertStore, "服务器离线", "${server.name} 连接已断开")
                                        }
                                    } else if (st.online) {
                                        offlineFired.remove(rule.serverId)
                                    }
                                }
                                "cpu" -> {
                                    val cpu = st.stats?.cpuPercent ?: 0.0
                                    if (st.online && cpu >= rule.threshold) {
                                        val last = cpuFiredAt[rule.id] ?: 0L
                                        if (System.currentTimeMillis() - last > 60 * 60 * 1000) {
                                            cpuFiredAt[rule.id] = System.currentTimeMillis()
                                            fire(
                                                alertStore, "CPU 告警",
                                                "${server.name} CPU 使用率 ${cpu.toInt()}%（阈值 ${rule.threshold}%）"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }
                delay(30_000)
            }
        }
    }

    private suspend fun fire(alertStore: AlertStore, title: String, body: String) {
        alertStore.push(title, body)
        trayNotify(title, body)
    }

    private var trayIcon: TrayIcon? = null

    private fun trayNotify(title: String, body: String) {
        try {
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()
            var icon = trayIcon
            if (icon == null) {
                val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
                val g = img.createGraphics()
                g.color = java.awt.Color(0x00, 0x7A, 0xFF)
                g.fillOval(0, 0, 16, 16)
                g.dispose()
                icon = TrayIcon(img, "Cssm")
                icon.isImageAutoSize = true
                tray.add(icon)
                trayIcon = icon
            }
            icon.displayMessage(title, body, TrayIcon.MessageType.WARNING)
        } catch (_: Exception) { }
    }
}
