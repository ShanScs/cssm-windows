package com.cssm.desktop.ui.metrics

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 三网延迟探测（NeoServer 指标卡片「三网」行用）。
 * 从本机经 TCP 建连测三大运营商代表节点的延迟；丢包率按最近 60 次尝试统计。
 * 结果是本机网络视角，所有服务器卡片共用同一份探测结果。
 */
data class Carrier(val name: String, val host: String, val color: Color)

val CARRIERS = listOf(
    Carrier("电信", "202.96.128.86", Color(0xFF3FB950)),   // 江苏电信 DNS
    Carrier("联通", "221.5.88.88", Color(0xFF58A6FF)),     // 联通 DNS
    Carrier("移动", "211.136.17.107", Color(0xFFF0883E)),  // 移动 DNS
)

data class PingState(
    val latencies: Map<String, Int?> = emptyMap(), // 运营商名 -> 延迟毫秒（null=超时）
    val lossPct: Double = 0.0,                    // 最近 60 次尝试的丢包率
    val bars: List<Float?> = emptyList(),          // 最近 18 轮平均延迟（null=整轮失败）
)

object CarrierPing {
    private const val PORT = 53
    private const val TIMEOUT_MS = 2500
    private const val INTERVAL_MS = 60_000L
    private const val MAX_ATTEMPTS = 60
    private const val MAX_BARS = 18

    private val _state = MutableStateFlow(PingState())
    val state: StateFlow<PingState> = _state.asStateFlow()

    private var job: Job? = null
    private val attempts = ArrayDeque<Boolean>() // true=成功
    private val bars = ArrayDeque<Float?>()

    fun start(scope: CoroutineScope) {
        if (job != null) return
        scope.launch(Dispatchers.IO) { probe() }
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(INTERVAL_MS)
                probe()
            }
        }
    }

    fun probeNow(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { probe() }
    }

    private suspend fun probe() = coroutineScope {
        val results = CARRIERS.map { c ->
            async(Dispatchers.IO) { c.name to tcpPing(c.host, PORT, TIMEOUT_MS) }
        }.awaitAll().toMap()

        synchronized(this@CarrierPing) {
            results.values.forEach { ms ->
                attempts.addLast(ms != null)
                if (attempts.size > MAX_ATTEMPTS) attempts.removeFirst()
            }
            val ok = results.values.filterNotNull()
            bars.addLast(if (ok.isEmpty()) null else ok.average().toFloat())
            if (bars.size > MAX_BARS) bars.removeFirst()
            val failed = attempts.count { !it }
            _state.value = PingState(
                latencies = results,
                lossPct = if (attempts.isEmpty()) 0.0 else failed * 100.0 / attempts.size,
                bars = bars.toList()
            )
        }
    }

    private fun tcpPing(host: String, port: Int, timeoutMs: Int): Int? {
        return try {
            val t0 = System.nanoTime()
            Socket().use { s -> s.connect(InetSocketAddress(host, port), timeoutMs) }
            ((System.nanoTime() - t0) / 1_000_000).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            null
        }
    }
}
