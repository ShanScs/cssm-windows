package com.cssm.desktop.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.EmptyState
import com.cssm.desktop.ui.components.osDisplayName
import kotlinx.coroutines.launch

/**
 * 服务器列表（桌面版 M1）：
 * 「最近会话」+「服务器」两个分区；点击行进入 SSH 终端。
 * 右上角「管理」进入编辑模式：⊖ 露出删除，↑↓ 调整顺序（桌面用鼠标，暂不做触屏拖拽）。
 */
@Composable
fun ServersScreen(
    store: ServerStore,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onSftp: (Long) -> Unit,
    onDocker: (Long) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val servers by store.servers.collectAsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    val recent = remember(servers) {
        servers.filter { it.lastConnectedAt > 0 }
            .sortedByDescending { it.lastConnectedAt }
            .take(8)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text("服务器", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    editing = !editing
                    pendingDeleteId = null
                }) { Text(if (editing) "完成" else "管理") }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onAdd) { Text("+ 添加") }
            }
        }

        if (servers.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Dns,
                title = "还没有服务器",
                subtitle = "增加第一台服务器，开始管理",
                actionLabel = "添加服务器",
                onAction = onAdd,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recent.isNotEmpty() && !editing) {
                item(key = "recent-h") { SectionTitle("最近会话") }
                items(recent, key = { "r-${it.id}" }) { s ->
                    ServerRow(
                        server = s,
                        onOpen = { onOpen(s.id) },
                        trailing = {
                            TextButton(onClick = {
                                scope.launch { store.clearRecentSession(s.id) }
                            }) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    )
                }
            }
            item(key = "all-h") {
                SectionTitle(if (editing) "服务器（编辑模式）" else "服务器")
            }
            items(servers, key = { it.id }) { s ->
                if (editing) {
                    val idx = servers.indexOfFirst { it.id == s.id }
                    EditRow(
                        server = s,
                        revealed = pendingDeleteId == s.id,
                        onMinus = { pendingDeleteId = if (pendingDeleteId == s.id) null else s.id },
                        onDelete = {
                            pendingDeleteId = null
                            scope.launch { store.delete(s) }
                        },
                        onMoveUp = {
                            if (idx > 0) scope.launch {
                                val ids = servers.map { it.id }.toMutableList()
                                ids.add(idx - 1, ids.removeAt(idx))
                                store.persistOrder(ids)
                            }
                        },
                        onMoveDown = {
                            if (idx in 0 until servers.lastIndex) scope.launch {
                                val ids = servers.map { it.id }.toMutableList()
                                ids.add(idx + 1, ids.removeAt(idx))
                                store.persistOrder(ids)
                            }
                        }
                    )
                } else {
                    ServerRow(server = s, onOpen = { onOpen(s.id) }, onEdit = { onEdit(s.id) }, onSftp = { onSftp(s.id) }, onDocker = { onDocker(s.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun ServerRow(
    server: Server,
    onOpen: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSftp: (() -> Unit)? = null,
    onDocker: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            OsBadge(osId = server.osId, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    server.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val os = osDisplayName(server.osId, server.osName)
                Text(
                    buildString {
                        append(server.displayTarget)
                        if (os.isNotBlank()) append(" · $os")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (onEdit != null) {
                TextButton(onClick = onEdit) { Text("编辑") }
            }
            if (onSftp != null) {
                TextButton(onClick = onSftp) { Text("文件") }
            }
            if (onDocker != null) {
                TextButton(onClick = onDocker) { Text("容器") }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun EditRow(
    server: Server,
    revealed: Boolean,
    onMinus: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (revealed) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .width(76.dp)
                    .background(Color(0xFFF85149), RoundedCornerShape(14.dp))
                    .clickable { onDelete() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("删除", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth()
                .offset(x = if (revealed) (-76).dp else 0.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp)
                        .background(Color(0xFFF85149), CircleShape)
                        .clickable(onClick = onMinus),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.width(11.dp).height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                }
                Spacer(Modifier.width(12.dp))
                OsBadge(osId = server.osId, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        server.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        server.displayTarget,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Column {
                    TextButton(onClick = onMoveUp) { Text("↑") }
                    TextButton(onClick = onMoveDown) { Text("↓") }
                }
            }
        }
    }
}
