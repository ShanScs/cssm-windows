package com.cssm.desktop.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadSectionHeader
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.ipadBlue
import com.cssm.desktop.ui.components.ipadName

/**
 * 终端页（iPad 风格）：最近会话 + 服务器列表，点击行进入 SSH 终端。
 */
@Composable
fun TerminalScreen(
    store: ServerStore,
    onOpen: (Long) -> Unit,
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp)
        ) {
            if (recent.isNotEmpty()) {
                item(key = "recent-h") {
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
                    item(key = "recent-list") {
                        ServerListCard(servers = recent, onOpen = onOpen)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            item(key = "all-h") {
                IpadSectionHeader(
                    icon = Icons.Filled.List,
                    iconTint = blue,
                    text = "服务器",
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "all-list") {
                if (filtered.isEmpty()) {
                    IpadCard {
                        Text(
                            text = "还没有服务器，去「更多 → 服务器」点击 + 添加",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    ServerListCard(servers = filtered, onOpen = onOpen)
                }
            }
        }
    }
}

/** 白卡服务器列表（行间分割线） */
@Composable
fun ServerListCard(
    servers: List<Server>,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    IpadCard(modifier = modifier) {
        servers.forEachIndexed { index, server ->
            ServerRow(server = server, onClick = { onOpen(server.id) })
            if (index < servers.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 60.dp)
                )
            }
        }
    }
}

@Composable
private fun ServerRow(server: Server, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OsBadge(osId = server.osId, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.ipadName(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = server.displayTarget,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "›",
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
