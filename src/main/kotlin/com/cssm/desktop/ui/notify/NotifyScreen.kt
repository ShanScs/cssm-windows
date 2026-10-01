package com.cssm.desktop.ui.notify

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.AlertRule
import com.cssm.desktop.data.AlertStore
import com.cssm.desktop.data.Server
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 通知页：通知中心（历史记录）+ 告警规则管理（离线 / CPU 阈值）。
 */
@Composable
fun NotifyScreen(
    store: AlertStore,
    servers: List<Server>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by store.items.collectAsState()
    val rules by store.rules.collectAsState()
    val scope = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    var editing by remember { mutableStateOf<AlertRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<AlertRule?>(null) }

    fun serverName(id: Long): String = servers.firstOrNull { it.id == id }?.name ?: "未知服务器"

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 90.dp)
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                    IpadTitle(text = "通知", modifier = Modifier.padding(top = 14.dp, end = 20.dp, bottom = 8.dp))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { scope.launch { store.markAllRead() } }) { Text("全部已读") }
                    TextButton(onClick = { scope.launch { store.clearItems() } }) {
                        Text("清空", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item(key = "rules-title") {
                Text("告警规则", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            }
            item(key = "rules") {
                IpadCard {
                    if (rules.isEmpty()) {
                        Text("暂无规则，点右下角 + 添加（离线 / CPU 阈值）",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp))
                    }
                    rules.forEachIndexed { i, r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (r.type == "offline") "${serverName(r.serverId)} 离线告警"
                                    else "${serverName(r.serverId)} CPU ≥ ${r.threshold}%",
                                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (r.enabled) "已启用" else "已停用",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = r.enabled,
                                onCheckedChange = { scope.launch { store.saveRule(r.copy(enabled = it)) } }
                            )
                            IconButton(onClick = { confirmDelete = r }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (i < rules.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }

            item(key = "items-title") {
                Text("通知中心", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            }
            if (items.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("暂无通知", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item(key = "items") {
                    IpadCard {
                        items.forEachIndexed { i, n ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = if (n.read) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(n.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(n.body, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Text(dateFmt.format(Date(n.time)), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (i < items.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = AlertRule(); showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加规则")
        }
    }

    if (showEditor && editing != null) {
        var serverId by remember(editing) { mutableStateOf(editing!!.serverId) }
        var type by remember(editing) { mutableStateOf(editing!!.type) }
        var threshold by remember(editing) { mutableStateOf(editing!!.threshold.toString()) }
        var menu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("新建告警规则") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box {
                        OutlinedTextField(
                            value = servers.firstOrNull { it.id == serverId }?.name ?: "选择服务器",
                            onValueChange = { }, readOnly = true, singleLine = true,
                            label = { Text("服务器") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // 透明点击层
                        Box(
                            modifier = Modifier.matchParentSize().clickable { menu = true }
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            servers.forEach { sv ->
                                DropdownMenuItem(
                                    text = { Text(sv.name) },
                                    onClick = { serverId = sv.id; menu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("类型", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = type == "offline",
                            onClick = { type = "offline" },
                            label = { Text("离线") }
                        )
                        androidx.compose.material3.FilterChip(
                            selected = type == "cpu",
                            onClick = { type = "cpu" },
                            label = { Text("CPU 阈值") }
                        )
                    }
                    if (type == "cpu") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = threshold,
                            onValueChange = { threshold = it.filter(Char::isDigit).take(3) },
                            label = { Text("CPU 阈值（%）") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (serverId == 0L) return@TextButton
                    val th = threshold.toIntOrNull()?.coerceIn(1, 100) ?: 90
                    scope.launch {
                        store.saveRule(AlertRule(serverId = serverId, type = type, threshold = th))
                        showEditor = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    if (confirmDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除规则") },
            text = { Text("确定删除这条告警规则吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.deleteRule(confirmDelete!!.id); confirmDelete = null }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}
