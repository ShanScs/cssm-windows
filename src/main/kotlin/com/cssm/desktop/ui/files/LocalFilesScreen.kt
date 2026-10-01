package com.cssm.desktop.ui.files

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.ui.components.IpadCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LocalEntry(
    val file: File,
    val isDir: Boolean,
    val size: Long,
    val modified: Long
)

/**
 * 本地文件浏览器（Windows）：浏览本地磁盘目录，双击文件用系统默认应用打开。
 */
@Composable
fun LocalFilesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var current by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var refreshTick by remember { mutableStateOf(0) }
    var entries by remember { mutableStateOf<List<LocalEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<File>() }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    suspend fun load(dir: File) {
        loading = true
        error = null
        val list = withContext(Dispatchers.IO) {
            try {
                val files = dir.listFiles()?.toList() ?: emptyList()
                files.map {
                    LocalEntry(it, it.isDirectory, if (it.isDirectory) 0 else it.length(), it.lastModified())
                }.sortedWith(compareBy({ !it.isDir }, { it.file.name.lowercase() }))
            } catch (e: Exception) {
                error = e.message ?: "读取失败"
                emptyList()
            }
        }
        entries = list
        loading = false
    }

    LaunchedEffect(current, refreshTick) { load(current) }

    fun openDir(dir: File) {
        history.add(current)
        current = dir
    }

    fun goBack() {
        if (history.isNotEmpty()) current = history.removeLast()
        else onBack()
    }

    fun openFile(f: File) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f)
        } catch (e: Exception) {
            error = "无法打开：${e.message}"
        }
    }

    // 磁盘根目录快捷入口
    val roots = remember { File.listRoots().toList() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { goBack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { current = File(System.getProperty("user.home")) ; history.clear() }) {
                Icon(Icons.Filled.Home, contentDescription = "主目录", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = current.absolutePath,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = { refreshTick++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 磁盘快捷栏
        if (roots.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                roots.forEach { r ->
                    androidx.compose.material3.FilterChip(
                        selected = current.absolutePath == r.absolutePath,
                        onClick = { history.clear(); current = r },
                        label = { Text(r.absolutePath.trimEnd(File.separatorChar).ifEmpty { r.absolutePath }) }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("空文件夹", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    item(key = "list") {
                        IpadCard {
                            entries.forEachIndexed { i, e ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (e.isDir) openDir(e.file) else openFile(e.file)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (e.isDir) Icons.Filled.Folder else Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = if (e.isDir) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = e.file.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateFmt.format(Date(e.modified)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!e.isDir) {
                                        Text(
                                            text = formatSize(e.size),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (i < entries.size - 1) {
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
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
