package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ssh.SshConnection
import com.cssm.desktop.ssh.StatsCollector
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.RegionSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 指标页（iPad 风格）：标题 + 地区分段 + 三列服务器卡片。
 * 每台服务器一条采集用 SSH 长连接，每 3 秒采样一次。
 */
@Composable
fun MetricsScreen(
    store: ServerStore,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by store.servers.collectAsState()
    val repo = remember { MetricsRepository() }
    val states by repo.states.collectAsState()
    val scope = rememberCoroutineScope()

    // 为每台服务器启动/停止采集协程
    DisposableEffect(servers) {
        val jobs = mutableMapOf<Long, Job>()
        for (server in servers) {
            if (jobs.containsKey(server.id)) continue
            jobs[server.id] = scope.launch(Dispatchers.IO) {
                collectLoop(server, repo)
            }
        }
        onDispose {
            jobs.values.forEach { it.cancel() }
        }
    }

    // 服务器被删除时清理状态
    LaunchedEffect(servers) {
        val ids = servers.map { it.id }.toSet()
        states.keys.filter { it !in ids }.forEach { repo.remove(it) }
    }

    val regions = remember(servers) {
        servers.map { it.region.ifBlank { "未知" } }.distinct()
            .map { region ->
                val flag = servers.firstOrNull {
                    (it.region.ifBlank { "未知" }) == region
                }?.regionFlag.orEmpty()
                region to flag
            }
    }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    val filtered = remember(servers, selectedRegion) {
        if (selectedRegion == null) servers
        else servers.filter { (it.region.ifBlank { "未知" }) == selectedRegion }
    }

    Column(modifier = modifier.fillMaxSize()) {
        IpadTitle(
            text = "指标",
            modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 8.dp)
        )
        if (servers.isNotEmpty()) {
            RegionSegment(
                regions = regions,
                selected = selectedRegion,
                onSelect = { selectedRegion = it },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        if (servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有服务器，去「更多 → 服务器」点击 + 添加",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filtered, key = { it.id }) { server ->
                ServerMetricCard(
                    server = server,
                    metrics = states[server.id],
                    onOpen = { onOpen(server.id) }
                )
            }
        }
    }
}

/**
 * 单台服务器的采集循环：保持 SSH 长连接，每 3 秒采样。
 * 断线后等待 5 秒重连。
 */
private suspend fun collectLoop(
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
