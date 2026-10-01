package com.cssm.desktop.ui.forward

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import com.cssm.desktop.data.ForwardRule
import com.cssm.desktop.data.ForwardStore
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.ForwardManager
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.launch

/**
 * 转发页：SSH 本地端口转发规则管理。
 * 规则：本地 localPort -> 经服务器 -> remoteHost:remotePort。
 */
@Composable
fun ForwardScreen(
    store: ForwardStore,
    manager: ForwardManager,
    servers: List<Server>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rules by store.rules.collectAsState()
    val running by manager.running.collectAsState()
    val errors by manager.errors.collectAsState()
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<ForwardRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ForwardRule?>(null) }

    fun serverName(id: Long): String = servers.firstOrNull { it.id == id }?.name ?: "未知服务器"

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                IpadTitle(
                    text = "转发",
                    modifier = Modifier.padding(top = 14.dp, end = 20.dp, bottom = 8.dp)
                )
            }
            if (rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有转发规则，点右下角 + 新建", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 90.dp)
                ) {
                    item(key = "list") {
                        IpadCard {
                            rules.forEachIndexed { i, r ->
                                val on = r.id in running
                                val err = errors[r.id]
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = null,
                                            tint = if (on) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = r.name.ifBlank { "未命名规则" },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "127.0.0.1:${r.localPort} → ${serverName(r.serverId)} → ${r.remoteHost}:${r.remotePort}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (err != null) {
                                                Text(err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Switch(
                                            checked = on,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    val sv = servers.firstOrNull { it.id == r.serverId }
                                                    if (sv != null) manager.start(r, sv)
                                                } else manager.stop(r.id)
                                            }
                                        )
                                        IconButton(onClick = { editing = r; showEditor = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "编辑",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { confirmDelete = r }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                if (i < rules.size - 1) {
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
        }

        FloatingActionButton(
            onClick = { editing = ForwardRule(); showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建规则")
        }
    }

    if (showEditor && editing != null) {
        var name by remember(editing) { mutableStateOf(editing!!.name) }
        var serverId by remember(editing) { mutableStateOf(editing!!.serverId) }
        var localPort by remember(editing) { mutableStateOf(editing!!.localPort.toString()) }
        var remoteHost by remember(editing) { mutableStateOf(editing!!.remoteHost) }
        var remotePort by remember(editing) { mutableStateOf(editing!!.remotePort.toString()) }
        var serverMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing!!.id == 0L) "新建转发" else "编辑转发") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("名称") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = servers.firstOrNull { it.id == serverId }?.name ?: "选择服务器",
                            onValueChange = { }, readOnly = true, singleLine = true,
                            label = { Text("经由服务器") },
                            modifier = Modifier.fillMaxWidth().clickable { serverMenu = true }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = serverMenu,
                            onDismissRequest = { serverMenu = false }
                        ) {
                            servers.forEach { sv ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(sv.name) },
                                    onClick = { serverId = sv.id; serverMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localPort, onValueChange = { localPort = it.filter(Char::isDigit) },
                        label = { Text("本地端口") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteHost, onValueChange = { remoteHost = it },
                        label = { Text("远端地址（服务器视角）") }, singleLine = true,
                        placeholder = { Text("127.0.0.1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remotePort, onValueChange = { remotePort = it.filter(Char::isDigit) },
                        label = { Text("远端端口") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lp = localPort.toIntOrNull() ?: 0
                    val rp = remotePort.toIntOrNull() ?: 0
                    if (serverId == 0L || lp !in 1..65535 || rp !in 1..65535) return@TextButton
                    scope.launch {
                        store.save(editing!!.copy(
                            name = name.trim().ifBlank { "未命名规则" },
                            serverId = serverId,
                            localPort = lp,
                            remoteHost = remoteHost.trim().ifBlank { "127.0.0.1" },
                            remotePort = rp
                        ))
                        showEditor = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    if (confirmDelete != null) {
        val r = confirmDelete!!
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除规则") },
            text = { Text("确定删除「${r.name.ifBlank { "未命名规则" }}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        manager.stop(r.id)
                        store.delete(r.id)
                        confirmDelete = null
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}
