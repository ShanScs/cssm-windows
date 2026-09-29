package com.cssm.desktop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.metrics.MetricsScreen
import com.cssm.desktop.ui.servers.EditScreen
import com.cssm.desktop.ui.docker.DockerScreen
import com.cssm.desktop.ui.sftp.SftpScreen
import com.cssm.desktop.ui.servers.ServersScreen
import com.cssm.desktop.ui.session.SessionScreen
import com.cssm.desktop.ui.settings.SettingsScreen
import com.cssm.desktop.ui.theme.CssmTheme
import com.cssm.desktop.ui.theme.ThemePrefs

sealed interface Route {
    data object Metrics : Route
    data object Servers : Route
    data class Edit(val id: Long) : Route
    data class Session(val id: Long) : Route
    data class Sftp(val id: Long) : Route
    data class Docker(val id: Long) : Route
    data object Settings : Route
}

fun main() = application {
    val store = remember { ServerStore() }
    var route by remember { mutableStateOf<Route>(Route.Metrics) }
    val themeMode by ThemePrefs.mode.collectAsState()

    CssmTheme(themeMode = themeMode) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cssm",
            state = rememberWindowState(width = 1100.dp, height = 720.dp)
        ) {
            val r = route
            if (r is Route.Session || r is Route.Edit || r is Route.Sftp || r is Route.Docker) {
                // 会话 / 编辑：全窗口内容，自带返回
                FullContent(route = r, store = store, onBack = { route = Route.Metrics })
            } else {
                Column(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    TopNavBar(
                        selected = r,
                        onSelect = { route = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Box(Modifier.weight(1f)) {
                        when (r) {
                            Route.Metrics -> MetricsScreen(
                                store = store,
                                onOpen = { route = Route.Session(it) }
                            )
                            Route.Servers -> ServersScreen(
                                store = store,
                                onAdd = { route = Route.Edit(0) },
                                onOpen = { route = Route.Session(it) },
                                onEdit = { route = Route.Edit(it) },
                                onSftp = { route = Route.Sftp(it) },
                                onDocker = { route = Route.Docker(it) }
                            )
                            Route.Settings -> SettingsScreen()
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullContent(route: Route, store: ServerStore, onBack: () -> Unit) {    when (route) {
        is Route.Session -> {
            val servers by store.servers.collectAsState()
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                SessionScreen(server = server, store = store, onClose = onBack)
            } else {
                onBack()
            }
        }
        is Route.Sftp -> {
            val servers by store.servers.collectAsState()
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                SftpScreen(server = server, onBack = onBack)
            } else {
                onBack()
            }
        }
        is Route.Docker -> {
            val servers by store.servers.collectAsState()
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                DockerScreen(server = server, onBack = onBack)
            } else {
                onBack()
            }
        }
        is Route.Edit -> EditScreen(
            id = route.id, store = store,
            onDone = onBack, onCancel = onBack
        )
        else -> Unit
    }
}

/**
 * 顶部导航栏：标题在左，页面 tab 居中胶囊。
 * 参考 iPad 端顶部 tab 的交互形式，视觉按 Cssm 自己的风格做。
 */
@Composable
private fun TopNavBar(
    selected: Route,
    onSelect: (Route) -> Unit
) {
    val tabs = listOf(
        "指标" to Route.Metrics,
        "服务器" to Route.Servers,
        "设置" to Route.Settings
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cssm",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEach { (label, route) ->
                val isSelected = selected == route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.background
                            else Color.Transparent
                        )
                        .clickable { onSelect(route) }
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 右侧占位，保持 tab 居中
        Spacer(Modifier.weight(1f))
    }
}
