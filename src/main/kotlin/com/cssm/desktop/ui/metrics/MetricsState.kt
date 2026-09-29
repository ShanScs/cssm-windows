package com.cssm.desktop.ui.metrics

import com.cssm.desktop.ssh.MonitorStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单台服务器的指标状态。
 */
data class ServerMetrics(
    val serverId: Long,
    /** null=尚未采集到数据；online=false 表示采集失败（离线） */
    val stats: MonitorStats? = null,
    val online: Boolean = true,
    val lastError: String? = null,
    /** 最近 N 次采样的下行速率历史（画曲线用） */
    val rxHistory: List<Long> = emptyList(),
    /** 最近 N 次采样的上行速率历史 */
    val txHistory: List<Long> = emptyList(),
    /** 最近 N 次采样的磁盘读速率历史 */
    val ioReadHistory: List<Long> = emptyList(),
    /** 最近 N 次采样的磁盘写速率历史 */
    val ioWriteHistory: List<Long> = emptyList()
) {
    companion object {
        const val HISTORY_SIZE = 30
    }

    /** 推入一次新采样，返回更新后的状态（历史只保留最近 N 次） */
    fun withNewSample(stats: MonitorStats): ServerMetrics {
        fun push(list: List<Long>, v: Long) = (list + v).takeLast(HISTORY_SIZE)
        return copy(
            stats = stats,
            online = true,
            lastError = null,
            rxHistory = push(rxHistory, stats.rxBytesPerSec),
            txHistory = push(txHistory, stats.txBytesPerSec),
            ioReadHistory = push(ioReadHistory, stats.ioReadBytesPerSec),
            ioWriteHistory = push(ioWriteHistory, stats.ioWriteBytesPerSec)
        )
    }
}

/**
 * 指标页的数据持有者：为每台服务器维护一条采集用 SSH 长连接，
 * 每 [intervalSec] 秒采样一次，通过 StateFlow 推送 UI。
 *
 * 由 MetricsScreen 通过 remember 创建，随页面销毁而停止。
 */
class MetricsRepository(
    private val intervalSec: Long = 3
) {
    private val _states = MutableStateFlow<Map<Long, ServerMetrics>>(emptyMap())
    val states: StateFlow<Map<Long, ServerMetrics>> = _states.asStateFlow()

    // 实际采集逻辑在 MetricsScreen 的 LaunchedEffect 里驱动，
    // 这里只做纯粹的状态容器，方便预览和测试。

    fun update(id: Long, metrics: ServerMetrics) {
        _states.value = _states.value + (id to metrics)
    }

    fun markOffline(id: Long, error: String?) {
        val cur = _states.value[id]
        _states.value = _states.value + (id to ServerMetrics(
            serverId = id,
            stats = cur?.stats,
            online = false,
            lastError = error
        ))
    }

    fun remove(id: Long) {
        _states.value = _states.value - id
    }

    fun clear() {
        _states.value = emptyMap()
    }
}
