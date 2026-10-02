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
    private val intervalSec: Long = 1
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

/** 连续采样失败多少次才判定断线并重连：偶发抖动不闪断 UI */
private const val MAX_CONSECUTIVE_FAILURES = 5

/**
 * 单台服务器的采集循环：App 存活期间保持一条 SSH 长连接，每 1 秒采样。
 * 采样偶发失败不判离线（保留上次数据显示），连续失败 [MAX_CONSECUTIVE_FAILURES]
 * 次才标记离线并在后台重连；每次重连都取服务器最新信息（改密码后自动恢复，
 * 服务器被删则退出）。由 App 级作用域驱动，App 关闭前一直运行。
 */
internal suspend fun collectLoop(
    server: Server,
    repo: MetricsRepository,
    serverProvider: () -> Server? = { server },
) {
    val conn = SshConnection()
    val collector = StatsCollector()
    var fails = 0
    while (true) {
        // 每次(重)连都取最新信息；服务器已被删除则结束
        val srv = serverProvider() ?: break
        try {
            withContext(Dispatchers.IO) {
                conn.connect(srv, 80, 24)
            }
            // 连接成功，进入采样循环
            while (true) {
                val stats = try {
                    withContext(Dispatchers.IO) {
                        collector.collect(conn)
                    }
                } catch (_: Exception) {
                    null
                }
                if (stats != null) {
                    fails = 0
                    val cur = repo.states.value[srv.id]
                        ?: ServerMetrics(srv.id)
                    repo.update(srv.id, cur.withNewSample(stats))
                } else {
                    fails++
                    if (fails >= MAX_CONSECUTIVE_FAILURES) {
                        repo.markOffline(srv.id, "连接中断，正在重连")
                        break
                    }
                    // 偶发失败：保留上次数据继续显示，不闪断
                }
                delay(1000)
            }
        } catch (e: Exception) {
            // 连接失败（含鉴权失败）：计入失败，退避后用最新信息重试
            fails++
            if (fails >= MAX_CONSECUTIVE_FAILURES) {
                repo.markOffline(srv.id, e.message)
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
