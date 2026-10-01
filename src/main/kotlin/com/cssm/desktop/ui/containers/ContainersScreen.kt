package com.cssm.desktop.ui.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadSubPanel
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.ServerNameWithFlag
import kotlinx.coroutines.launch

/**
 * 容器页（iPad 风格）：容器服务器卡片网格。
 * 每张卡片逐台探测 Docker：Connecting → 计数（运行中/停止/总计/镜像/网络/卷）。
 */
@Composable
fun ContainersScreen(
    store: ServerStore,
    onDocker: (Long) -> Unit,
    onSession: (Long) -> Unit,
    onSftp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by store.servers.collectAsState()

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

    // 逐台 Docker 探测
    val probeStates = remember { mutableStateMapOf<Long, DockerCardStatus>() }
    LaunchedEffect(servers) {
        probeStates.keys.retainAll(servers.map { it.id }.toSet())
        servers.forEach { s ->
            if (probeStates[s.id] == null) {
                probeStates[s.id] = DockerCardStatus.Connecting
                launch { probeStates[s.id] = probeDocker(s) }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        IpadTitle(
            text = "容器服务器",
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

        if (filtered.isEmpty()) {
            IpadCard(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "还没有服务器，去「更多 → 服务器」点击 + 添加",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filtered, key = { it.id }) { server ->
                ContainerServerCard(
                    server = server,
                    status = probeStates[server.id] ?: DockerCardStatus.Connecting,
                    onDocker = { onDocker(server.id) }
                )
            }
        }
    }
}

@Composable
private fun ContainerServerCard(
    server: Server,
    status: DockerCardStatus,
    onDocker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = status as? DockerCardStatus.Ready
    val variant = MaterialTheme.colorScheme.onSurfaceVariant

    IpadCard(modifier = modifier.clickable(onClick = onDocker)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                OsBadge(osId = server.osId, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ServerNameWithFlag(
                        server = server,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (dotColor, stateText, textColor) = when (status) {
                            is DockerCardStatus.Connecting ->
                                Triple(Color(0xFFD6985D), "Connecting", Color(0xFFD6985D))
                            is DockerCardStatus.Ready ->
                                Triple(Color(0xFF34C77B), "Connected", Color(0xFF34C77B))
                            is DockerCardStatus.Failed ->
                                Triple(Color(0xFFFF453A), "Failed", Color(0xFFFF453A))
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = stateText, fontSize = 12.sp, color = textColor)
                        Text(text = " · Docker", fontSize = 12.sp, color = variant)
                    }
                }
                Text(
                    text = ready?.total?.toString() ?: "—",
                    fontSize = 14.sp,
                    color = variant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = variant.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 运行中 / 停止 / 总计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DockerStatPanel(
                    icon = Icons.Filled.PlayArrow,
                    label = "运行中",
                    value = ready?.running?.toString() ?: "—",
                    modifier = Modifier.weight(1f)
                )
                DockerStatPanel(
                    icon = Icons.Filled.Stop,
                    label = "停止",
                    value = ready?.stopped?.toString() ?: "—",
                    modifier = Modifier.weight(1f)
                )
                DockerStatPanel(
                    icon = Icons.Filled.PieChart,
                    label = "总计",
                    value = ready?.total?.toString() ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 镜像 / 网络 / 卷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockerMiniStat(
                    icon = Icons.Filled.Image,
                    value = ready?.images?.toString() ?: "—"
                )
                DockerMiniStat(
                    icon = Icons.Filled.DeviceHub,
                    value = ready?.networks?.toString() ?: "—"
                )
                DockerMiniStat(
                    icon = Icons.Filled.Storage,
                    value = ready?.volumes?.toString() ?: "—"
                )
            }
        }
    }
}

@Composable
private fun DockerStatPanel(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    IpadSubPanel(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = variant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = variant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DockerMiniStat(icon: ImageVector, value: String) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = variant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = value, fontSize = 12.sp, color = variant)
    }
}
