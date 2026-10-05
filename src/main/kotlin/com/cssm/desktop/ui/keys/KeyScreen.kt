package com.cssm.desktop.ui.keys

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import java.awt.FileDialog
import java.awt.Frame
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
import com.cssm.desktop.data.KeyStore
import com.cssm.desktop.data.SshKey
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.launch

/**
 * 私钥管理页：命名保存 SSH 私钥（PEM），服务器编辑页可直接选用。
 */
@Composable
fun KeyScreen(
    store: KeyStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys by store.keys.collectAsState()
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<SshKey?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<SshKey?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
                IpadTitle(
                    text = "私钥",
                    modifier = Modifier.padding(top = 14.dp, end = 20.dp, bottom = 8.dp)
                )
            }
            if (keys.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有私钥，点右下角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 90.dp)
                ) {
                    item(key = "list") {
                        IpadCard {
                            keys.forEachIndexed { i, k ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = k.name.ifBlank { "(未命名)" },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = keyFingerprint(k.pem),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { editing = k; showEditor = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "编辑",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { confirmDelete = k }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (i < keys.size - 1) {
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
            onClick = { editing = SshKey(); showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加私钥")
        }
    }

    if (showEditor && editing != null) {
        var name by remember(editing) { mutableStateOf(editing!!.name) }
        var pem by remember(editing) { mutableStateOf(editing!!.pem) }
        var passphrase by remember(editing) { mutableStateOf(editing!!.passphrase) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing!!.id == 0L) "添加私钥" else "编辑私钥") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("名称") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dialog = FileDialog(null as Frame?, "选择私钥文件", FileDialog.LOAD)
                                dialog.isVisible = true
                                val dir = dialog.directory
                                val file = dialog.file
                                if (dir != null && file != null) {
                                    try {
                                        pem = java.io.File(dir, file).readText()
                                    } catch (_: Exception) {}
                                }
                                dialog.dispose()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("从文件选择", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pem, onValueChange = { pem = it },
                        label = { Text("私钥（PEM）") },
                        placeholder = { Text("-----BEGIN ...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphrase, onValueChange = { passphrase = it },
                        label = { Text("私钥口令（可选）") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pem.isBlank()) { showEditor = false; return@TextButton }
                    scope.launch {
                        store.save(editing!!.copy(name = name.trim().ifBlank { "未命名密钥" }, pem = pem.trim(), passphrase = passphrase))
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
            title = { Text("删除私钥") },
            text = { Text("确定删除「${confirmDelete!!.name.ifBlank { "(未命名)" }}」吗？使用该私钥的服务器需要重新配置。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.delete(confirmDelete!!.id); confirmDelete = null }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}

/** 私钥摘要：取首尾片段做标识，不暴露完整内容。 */
private fun keyFingerprint(pem: String): String {
    val compact = pem.lines().filter { !it.startsWith("-----") }.joinToString("").trim()
    if (compact.length < 16) return "无效的私钥格式"
    return compact.take(8) + "…" + compact.takeLast(8)
}
