package com.cssm.desktop.ui.docker

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.DockerContainer
import com.cssm.desktop.ssh.DockerManager
import com.cssm.desktop.ssh.SshConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Docker 容器管理：一台服务器的容器列表，可启动/停止/重启。
 */
@Composable
fun DockerScreen(
    server: Server,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val conn = remember { SshConnection() }
    val docker = remember { DockerManager() }
    var containers by remember { mutableStateOf<List<DockerContainer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionTarget by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loading = true
                error = null
            }
            try {
                if (!conn.isConnected) {
                    withContext(Dispatchers.IO) { conn.connect(server, 80, 24) }
                }
                val list = withContext(Dispatchers.IO) { docker.listContainers(conn) }
                withContext(Dispatchers.Main) {
                    containers = list
                    loading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message ?: "加载失败"
                    loading = false
                }
            }
        }
    }

    fun doAction(containerId: String, action: String) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { actionTarget = containerId }
            try {
                withContext(Dispatchers.IO) {
                    when (action) {
                        "start" -> docker.start(conn, containerId)
                        "stop" -> docker.stop(conn, containerId)
                        "restart" -> docker.restart(conn, containerId)
                    }
                }
                // 刷新列表
                val list = withContext(Dispatchers.IO) { docker.listContainers(conn) }
                withContext(Dispatchers.Main) {
                    containers = list
                    actionTarget = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message
                    actionTarget = null
                }
            }
        }
    }

    LaunchedEffect(server.id) {
        load()
    }

    DisposableEffect(server.id) {
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
            Text(
                text = "${server.displayName} · 容器",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { load() }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { load() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                containers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "没有容器",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(containers, key = { it.id }) { c ->
                            DockerContainerCard(
                                container = c,
                                busy = actionTarget == c.id,
                                onStart = { doAction(c.id, "start") },
                                onStop = { doAction(c.id, "stop") },
                                onRestart = { doAction(c.id, "restart") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockerContainerCard(
    container: DockerContainer,
    busy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    val running = container.state == "running"
    val stateColor = when (container.state) {
        "running" -> Color(0xFF34C77B)
        "exited" -> Color(0xFFF87171)
        else -> Color(0xFFE8933C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = container.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = container.image,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "● ${container.state}",
                    fontSize = 12.sp,
                    color = stateColor
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = container.status,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (running) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "CPU ${container.cpuPercent.toInt()}% · 内存 ${container.memUsage} (${container.memPercent.toInt()}%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    if (!running) {
                        TextButton(onClick = onStart) { Text("启动") }
                    } else {
                        TextButton(onClick = onStop) { Text("停止") }
                    }
                    TextButton(onClick = onRestart) { Text("重启") }
                }
            }
        }
    }
}
