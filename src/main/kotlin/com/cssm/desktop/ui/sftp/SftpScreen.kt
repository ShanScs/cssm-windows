package com.cssm.desktop.ui.sftp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.SftpConnection
import com.cssm.desktop.ssh.SftpFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SFTP 文件浏览器：一台服务器的文件列表，可进目录。
 */
@Composable
fun SftpScreen(
    server: Server,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val conn = remember { SftpConnection() }
    var currentPath by remember { mutableStateOf("/") }
    var files by remember { mutableStateOf<List<SftpFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(path: String) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loading = true
                error = null
            }
            try {
                if (!conn.isConnected()) {
                    withContext(Dispatchers.IO) { conn.connect(server) }
                }
                val list = withContext(Dispatchers.IO) { conn.listDir(path) }
                withContext(Dispatchers.Main) {
                    files = list
                    currentPath = path
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message ?: "连接失败"
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(server.id) {
        load("/")
    }

    // 退出时断开
    androidx.compose.runtime.DisposableEffect(server.id) {
        onDispose { conn.disconnect() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${server.displayName} · 文件",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentPath,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { load(currentPath) }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        // 面包屑：上级目录
        if (currentPath != "/") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        val parent = currentPath.trimEnd('/').substringBeforeLast('/', "/")
                        load(if (parent.isBlank()) "/" else parent)
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = "..", fontSize = 14.sp)
                }
            }
        }

        // 内容
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.TextButton(onClick = { load(currentPath) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "空目录",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(files, key = { it.path }) { file ->
                            SftpFileRow(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        load(file.path)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SftpFileRow(
    file: SftpFile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal
            )
            if (!file.isDirectory) {
                Text(
                    text = "${formatSize(file.size)} · ${formatDate(file.modifiedAt)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!file.isDirectory) {
            Text(
                text = formatSize(file.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f K".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f M".format(bytes / 1024.0 / 1024.0)
    else -> "%.2f G".format(bytes / 1024.0 / 1024.0 / 1024.0)
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
private fun formatDate(ms: Long): String = try {
    dateFormat.format(Date(ms))
} catch (_: Exception) {
    ""
}
