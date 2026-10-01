package com.cssm.desktop.ui.containers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.ipadName

/**
 * 容器页（iPad 风格）：容器服务器卡片网格。
 * 点击卡片或按钮进入该服务器的 Docker 容器管理。
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
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filtered, key = { it.id }) { server ->
                ContainerServerCard(
                    server = server,
                    onDocker = { onDocker(server.id) },
                    onSession = { onSession(server.id) },
                    onSftp = { onSftp(server.id) }
                )
            }
        }
    }
}

@Composable
private fun ContainerServerCard(
    server: Server,
    onDocker: () -> Unit,
    onSession: () -> Unit,
    onSftp: () -> Unit
) {
    IpadCard(modifier = Modifier.clickable(onClick = onDocker)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                OsBadge(osId = server.osId, size = 34.dp)
                Spacer(Modifier.width(10.dp))
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
                        text = "Docker",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // 运行中 / 停止 / 统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    icon = Icons.Filled.PlayArrow,
                    label = "运行中",
                    onClick = onDocker,
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    icon = Icons.Filled.Stop,
                    label = "停止",
                    onClick = onDocker,
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    icon = Icons.Filled.BarChart,
                    label = "统计",
                    onClick = onDocker,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // 小按钮：终端 / 文件
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniButton(icon = Icons.Filled.Terminal, onClick = onSession)
                MiniButton(icon = Icons.Filled.Folder, onClick = onSftp)
            }
        }
    }
}

@Composable
private fun StatusButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MiniButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp, 32.dp)
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
