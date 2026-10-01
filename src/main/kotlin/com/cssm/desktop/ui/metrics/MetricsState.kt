package com.cssm.desktop.ui.metrics

import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.MonitorStats
import com.cssm.desktop.ssh.SshConnection
import com.cssm.desktop.ssh.StatsCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

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
 * 由 App 入口创建并驱动采集（Main.kt），App 存活期间一直运行，
 * 切换 tab 不会中断；彻底关闭 App 时随协程作用域结束。
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

/**
 * 单台服务器的采集循环：保持 SSH 长连接，每 3 秒采样。
 * 断线后等待 5 秒重连。由 App 级作用域驱动，App 关闭前一直运行。
 */
internal suspend fun collectLoop(
    server: Server,
    repo: MetricsRepository
) {
    val conn = SshConnection()
    val collector = StatsCollector()
    while (true) {
        try {
            withContext(Dispatchers.IO) {
                conn.connect(server, 80, 24)
            }
            // 连接成功，进入采样循环
            while (true) {
                try {
                    val stats = withContext(Dispatchers.IO) {
                        collector.collect(conn)
                    }
                    if (stats != null) {
                        val cur = repo.states.value[server.id]
                            ?: ServerMetrics(server.id)
                        repo.update(server.id, cur.withNewSample(stats))
                    }
                } catch (e: Exception) {
                    // 单次采样失败，标记离线并重连
                    repo.markOffline(server.id, e.message)
                    break
                }
                delay(3000)
            }
        } catch (e: Exception) {
            // 连接失败
            if (repo.states.value[server.id]?.stats == null) {
                repo.markOffline(server.id, e.message)
            } else {
                // 有历史数据时保持显示，仅标记离线
                val cur = repo.states.value[server.id]
                if (cur?.online != false) {
                    repo.update(server.id, cur!!.copy(online = false, lastError = e.message))
                }
            }
        }
        try {
            withContext(Dispatchers.IO) { conn.disconnect() }
        } catch (_: Exception) {
        }
        delay(5000)
        if (!currentCoroutineContext().isActive) break
    }
}
