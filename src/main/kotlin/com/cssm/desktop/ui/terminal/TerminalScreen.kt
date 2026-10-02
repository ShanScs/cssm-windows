package com.cssm.desktop.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadSectionHeader
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.EmptyState
import com.cssm.desktop.ui.components.ServerResourceCard
import com.cssm.desktop.ui.components.cardEntrance
import com.cssm.desktop.ui.components.ipadBlue

/**
 * 终端页（资源库卡片风格）：最近会话 + 服务器，淡彩渐变卡片网格，点击进入 SSH 终端。
 */
@Composable
fun TerminalScreen(
    store: ServerStore,
    onOpen: (Long) -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by store.servers.collectAsState()
    val blue = ipadBlue()

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
    val recent = remember(filtered) {
        filtered.filter { it.lastConnectedAt > 0 }
            .sortedByDescending { it.lastConnectedAt }
            .take(8)
    }
    var recentExpanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        IpadTitle(
            text = "终端",
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recent.isNotEmpty()) {
                item(span = { GridItemSpan(3) }, key = "recent-h") {
                    IpadSectionHeader(
                        icon = Icons.Filled.Schedule,
                        iconTint = Color(0xFF34C77B),
                        text = "最近会话",
                        textColor = blue,
                        expanded = recentExpanded,
                        onToggle = { recentExpanded = !recentExpanded }
                    )
                }
                if (recentExpanded) {
                    itemsIndexed(recent, key = { _, s -> "recent-${s.id}" }) { index, server ->
                        ServerResourceCard(
                            server = server,
                            onClick = { onOpen(server.id) },
                            isRecentSession = true,
                            modifier = Modifier.cardEntrance(index)
                        )
                    }
                    item(span = { GridItemSpan(3) }, key = "recent-gap") {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            item(span = { GridItemSpan(3) }, key = "all-h") {
                IpadSectionHeader(
                    icon = Icons.Filled.List,
                    iconTint = blue,
                    text = "服务器",
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(3) }, key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Terminal,
                        title = "还没有服务器",
                        subtitle = "添加服务器后，一点即连 SSH 终端",
                        actionLabel = "添加服务器",
                        onAction = onAddServer
                    )
                }
            } else {
                itemsIndexed(filtered, key = { _, s -> s.id }) { index, server ->
                    ServerResourceCard(
                        server = server,
                        onClick = { onOpen(server.id) },
                        modifier = Modifier.cardEntrance(recent.size + index)
                    )
                }
            }
        }
    }
}
