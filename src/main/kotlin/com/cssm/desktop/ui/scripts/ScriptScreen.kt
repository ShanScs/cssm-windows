package com.cssm.desktop.ui.scripts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Script
import com.cssm.desktop.data.ScriptStore
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.SshExec
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.launch

/**
 * 脚本页：管理 Shell 脚本，选择服务器执行并查看输出。
 */
@Composable
fun ScriptScreen(
    store: ScriptStore,
    servers: List<Server>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by store.scripts.collectAsState()
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<Script?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var runTarget by remember { mutableStateOf<Script?>(null) }
    var pickServerFor by remember { mutableStateOf<Script?>(null) }
    var output by remember { mutableStateOf<Pair<String, String>?>(null) } // (title, text)
    var running by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Script?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                IpadTitle(
                    text = "脚本",
                    modifier = Modifier.padding(top = 14.dp, end = 20.dp, bottom = 8.dp)
                )
            }
            if (scripts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有脚本，点右下角 + 新建", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 90.dp)
                ) {
                    item(key = "list") {
                        IpadCard {
                            scripts.forEachIndexed { i, s ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = s.name.ifBlank { "(未命名)" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { pickServerFor = s }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "运行",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { editing = s; showEditor = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "编辑",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { confirmDelete = s }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (i < scripts.size - 1) {
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
            onClick = { editing = Script(); showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建脚本")
        }
    }

    // 新建/编辑
    if (showEditor && editing != null) {
        var name by remember(editing) { mutableStateOf(editing!!.name) }
        var content by remember(editing) { mutableStateOf(editing!!.content) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing!!.id == 0L) "新建脚本" else "编辑脚本") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("名称") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        label = { Text("Shell 脚本") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank() && content.isBlank()) { showEditor = false; return@TextButton }
                    scope.launch {
                        store.save(editing!!.copy(name = name.trim(), content = content))
                        showEditor = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    // 选择服务器运行
    if (pickServerFor != null) {
        val s = pickServerFor!!
        AlertDialog(
            onDismissRequest = { pickServerFor = null },
            title = { Text("在服务器上运行") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(s.name.ifBlank { "(未命名)" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    if (servers.isEmpty()) {
                        Text("还没有服务器，请先添加服务器", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    servers.forEach { sv ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !running) {
                                    pickServerFor = null
                                    runTarget = s
                                    running = true
                                    scope.launch {
                                        val title = "${s.name.ifBlank { "脚本" }} @ ${sv.name}"
                                        try {
                                            val out = SshExec.run(sv, s.content)
                                            output = title to out
                                        } catch (e: Exception) {
                                            output = title to "执行失败：${e.message}"
                                        }
                                        running = false
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sv.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("${sv.username}@${sv.host}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = { TextButton(onClick = { pickServerFor = null }) { Text("取消") } }
        )
    }

    // 执行中
    if (running) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("执行中") },
            text = { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            } },
            confirmButton = { }
        )
    }

    // 输出
    if (output != null) {
        val (title, text) = output!!
        AlertDialog(
            onDismissRequest = { output = null },
            title = { Text(title, fontSize = 15.sp) },
            text = {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { output = null }) { Text("关闭") } }
        )
    }

    // 删除确认
    if (confirmDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除脚本") },
            text = { Text("确定删除「${confirmDelete!!.name.ifBlank { "(未命名)" }}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.delete(confirmDelete!!.id); confirmDelete = null }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}
