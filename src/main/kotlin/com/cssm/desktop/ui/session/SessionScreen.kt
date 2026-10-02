package com.cssm.desktop.ui.session

import androidx.compose.ui.awt.SwingPanel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.net.GeoIp
import com.cssm.desktop.ssh.OsDetector
import com.cssm.desktop.ssh.SshConnection
import com.cssm.desktop.ssh.SshTtyConnector
import com.cssm.desktop.ssh.StatsCollector
import com.cssm.desktop.ui.terminal.CssmTermSettings
import com.jediterm.terminal.ui.JediTermWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

private sealed interface ConnState {
    data object Connecting : ConnState
    data object Connected : ConnState
    data class Failed(val message: String) : ConnState
}

/**
 * SSH 会话界面：顶部工具栏 + JediTerm 终端（Swing 嵌入）。
 *
 * 工具栏：返回 / 状态点 / 服务器名 / 字号 -/+ / 清屏 / 重连。
 * 连接成功后顺手做 OS 识别与地区检测并写回 ServerStore（与手机端一致）。
 */
@Composable
fun SessionScreen(
    server: Server,
    store: ServerStore,
    onClose: () -> Unit
) {
    val ssh = remember { SshConnection() }
    var connState by remember { mutableStateOf<ConnState>(ConnState.Connecting) }
    var fontSize by remember { mutableStateOf(CssmTermSettings.loadFontSize()) }
    var widget by remember { mutableStateOf<JediTermWidget?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    // 深色模式跟随 App 主题（按背景亮度判断）
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // SSH 连接生命周期：server 切换或手动重连时重建
    LaunchedEffect(server.id, attempt) {
        connState = ConnState.Connecting
        withContext(Dispatchers.Swing) {
            try { widget?.stop() } catch (_: Exception) {}
        }
        widget = null
        val err = withContext(Dispatchers.IO) {
            try {
                ssh.connect(server, 100, 30)
                // 连接成功后：OS 识别 + 地区检测（失败静默忽略）
                try {
                    val os = OsDetector.detect(ssh)
                    val region = GeoIp.detectRegion(server.host)
                    store.updateConnectionInfo(
                        server.id, os.id, os.name,
                        region?.name ?: server.region, region?.flag ?: server.regionFlag
                    )
                } catch (_: Exception) {
                }
                try {
                    StatsCollector().collect(ssh)
                } catch (_: Exception) {
                }
                null
            } catch (e: Exception) {
                e.message?.ifBlank { null } ?: "连接失败"
            }
        }
        connState = if (err == null) ConnState.Connected else ConnState.Failed(err)
    }

    // 终端组件生命周期：连接成功后创建；字号/深浅色变化时重建（SSH 不动）
    LaunchedEffect(connState, fontSize, dark) {
        if (connState != ConnState.Connected) return@LaunchedEffect
        val settings = CssmTermSettings(fontSize, dark)
        val w = withContext(Dispatchers.Swing) {
            try { widget?.stop() } catch (_: Exception) {}
            // 100 列：比 80 宽，能容纳二维码等宽内容；JediTerm 会按面板实际尺寸自动调整
            JediTermWidget(100, 30, settings).also {
                it.setTtyConnector(SshTtyConnector(ssh))
                it.start()
            }
        }
        widget = w
        withContext(Dispatchers.Swing) { w.requestFocusInWindow() }
    }

    DisposableEffect(server.id) {
        onDispose {
            // 后台断开：SSHJ 的 disconnect 在网络异常时可能阻塞，绝不能卡 UI 线程
            val w = widget
            Thread {
                try { w?.stop() } catch (_: Exception) {}
                try { ssh.disconnect() } catch (_: Exception) {}
            }.apply { isDaemon = true; name = "ssh-disconnect" }.start()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- 工具栏 ----
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("← 返回") }
            val dotColor = when (connState) {
                ConnState.Connecting -> MaterialTheme.colorScheme.tertiary
                ConnState.Connected -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                is ConnState.Failed -> MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                server.displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val ns = (fontSize - 1).coerceIn(8f, 24f)
                fontSize = ns; CssmTermSettings.saveFontSize(ns)
            }) { Text("A-") }
            Text("${fontSize.toInt()}", style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = {
                val ns = (fontSize + 1).coerceIn(8f, 24f)
                fontSize = ns; CssmTermSettings.saveFontSize(ns)
            }) { Text("A+") }
            TextButton(onClick = {
                try { widget?.terminalTextBuffer?.clearScreenAndHistoryBuffers() } catch (_: Exception) {}
            }) { Text("清屏") }
            TextButton(onClick = { attempt++ }) { Text("重连") }
        }

        // ---- 内容区 ----
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (val st = connState) {
                ConnState.Connecting -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.size(12.dp))
                        Text("正在连接 ${server.displayTarget}…")
                    }
                }
                is ConnState.Failed -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("连接失败", style = MaterialTheme.typography.titleMedium)
                        Text(
                            st.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { attempt++ }) { Text("重试") }
                    }
                }
                ConnState.Connected -> {
                    val w = widget
                    if (w != null) {
                        key(w) {
                            // Swing 是重量级组件，无视 Compose 的圆角 clip；
                            // 底部留出圆角半径的安全区，避免方形终端盖住窗口圆角
                            SwingPanel(
                                factory = { w },
                                modifier = Modifier.fillMaxSize()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
