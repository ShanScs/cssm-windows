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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import com.cssm.desktop.ui.components.cardEntrance
import com.cssm.desktop.ui.components.CenteredGridContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.EmptyState
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.theme.UiPrefs

/**
 * 指标页（iPad 风格）：标题 + 地区分段 + 三列服务器卡片。
 * 采集由 App 级驱动（Main.kt），本页只负责展示。
 */
@Composable
fun MetricsScreen(
    store: ServerStore,
    repo: MetricsRepository,
    onOpen: (Long) -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by store.servers.collectAsState()
    val states by repo.states.collectAsState()
    val ping by CarrierPing.state.collectAsState()
    val compact by UiPrefs.compactMetrics.collectAsState()
    val pingScope = rememberCoroutineScope()

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
            EmptyState(
                icon = Icons.Filled.Dns,
                title = "还没有服务器",
                subtitle = "添加第一台服务器，开始实时监控它的状态",
                actionLabel = "添加服务器",
                onAction = onAddServer,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        CenteredGridContainer {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(filtered, key = { _, s -> s.id }) { index, server ->
                ServerMetricCard(
                    server = server,
                    metrics = states[server.id],
                    ping = ping,
                    onOpen = { onOpen(server.id) },
                    onPingRefresh = { CarrierPing.probeNow(pingScope) },
                    compact = compact,
                    modifier = Modifier.cardEntrance(index)
                )
            }
        }
        }
    }
}
