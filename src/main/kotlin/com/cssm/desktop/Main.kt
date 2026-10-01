package com.cssm.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cssm.desktop.ui.components.ipadBlue
import com.cssm.desktop.ui.containers.ContainersScreen
import com.cssm.desktop.ui.files.FilesScreen
import com.cssm.desktop.ui.metrics.MetricsRepository
import com.cssm.desktop.ui.metrics.MetricsScreen
import com.cssm.desktop.ui.metrics.collectLoop
import com.cssm.desktop.ui.more.MoreScreen
import com.cssm.desktop.ui.servers.EditScreen
import com.cssm.desktop.ui.docker.DockerScreen
import com.cssm.desktop.ui.sftp.SftpScreen
import com.cssm.desktop.ui.servers.ServersScreen
import com.cssm.desktop.ui.session.SessionScreen
import com.cssm.desktop.ui.settings.SettingsScreen
import com.cssm.desktop.ui.terminal.TerminalScreen
import com.cssm.desktop.ui.theme.CssmTheme
import com.cssm.desktop.ui.theme.ThemePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface Route {
    // 一级 tab（顶部居中分段导航，对标 iPad 版）
    data object Metrics : Route
    data object Terminal : Route
    data object Files : Route
    data object Containers : Route
    data object More : Route
    // 更多页的二级页
    data object ServerManage : Route
    data object Settings : Route
    // 全窗口页
    data class Edit(val id: Long) : Route
    data class Session(val id: Long) : Route
    data class Sftp(val id: Long) : Route
    data class Docker(val id: Long) : Route
}

fun main() = application {
    val store = remember { ServerStore() }
    var route by remember { mutableStateOf<Route>(Route.Metrics) }
    // 进入全窗口页之前的返回目标
    var returnRoute by remember { mutableStateOf<Route>(Route.Metrics) }
    val themeMode by ThemePrefs.mode.collectAsState()

    // 全局指标采集：App 存活期间一直跑，切 tab / 进二级页不中断，
    // 彻底关闭 App 时随作用域结束
    val metricsRepo = remember { MetricsRepository() }
    val appScope = rememberCoroutineScope()
    val servers by store.servers.collectAsState()
    val collectJobs = remember { mutableMapOf<Long, Job>() }
    LaunchedEffect(servers) {
        val ids = servers.map { it.id }.toSet()
        collectJobs.keys.filter { it !in ids }.forEach { collectJobs.remove(it)?.cancel() }
        for (server in servers) {
            if (!collectJobs.containsKey(server.id)) {
                collectJobs[server.id] = appScope.launch(Dispatchers.IO) {
                    collectLoop(server, metricsRepo)
                }
            }
        }
        metricsRepo.states.value.keys.filter { it !in ids }.forEach { metricsRepo.remove(it) }
    }

    fun nav(target: Route, ret: Route? = null) {
        if (ret != null) returnRoute = ret
        route = target
    }

    CssmTheme(themeMode = themeMode) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cssm",
            state = rememberWindowState(width = 1100.dp, height = 720.dp)
        ) {
            val r = route
            if (r is Route.Session || r is Route.Edit || r is Route.Sftp ||
                r is Route.Docker || r is Route.ServerManage || r is Route.Settings
            ) {
                FullContent(
                    route = r,
                    store = store,
                    onBack = { route = returnRoute },
                    navEdit = { id, ret -> nav(Route.Edit(id), ret) }
                )
            } else {
                Column(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    TopNavBar(
                        selected = r,
                        onSelect = { route = it },
                        onAddServer = { nav(Route.Edit(0), r) },
                        onOpenSettings = { nav(Route.Settings, r) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Box(Modifier.weight(1f)) {
                        when (r) {
                            Route.Metrics -> MetricsScreen(
                                store = store,
                                repo = metricsRepo,
                                onOpen = { id -> nav(Route.Session(id), r) }
                            )
                            Route.Terminal -> TerminalScreen(
                                store = store,
                                onOpen = { id -> nav(Route.Session(id), r) }
                            )
                            Route.Files -> FilesScreen(
                                store = store,
                                onOpenSftp = { id -> nav(Route.Sftp(id), r) }
                            )
                            Route.Containers -> ContainersScreen(
                                store = store,
                                onDocker = { id -> nav(Route.Docker(id), r) },
                                onSession = { id -> nav(Route.Session(id), r) },
                                onSftp = { id -> nav(Route.Sftp(id), r) }
                            )
                            Route.More -> MoreScreen(
                                onServers = { nav(Route.ServerManage, r) },
                                onSettings = { nav(Route.Settings, r) }
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullContent(
    route: Route,
    store: ServerStore,
    onBack: () -> Unit,
    navEdit: (Long, Route) -> Unit
) {
    val servers by store.servers.collectAsState()
    when (route) {
        is Route.Session -> {
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                SessionScreen(server = server, store = store, onClose = onBack)
            } else {
                onBack()
            }
        }
        is Route.Sftp -> {
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                SftpScreen(server = server, onBack = onBack)
            } else {
                onBack()
            }
        }
        is Route.Docker -> {
            val server = servers.firstOrNull { it.id == route.id }
            if (server != null) {
                DockerScreen(server = server, onBack = onBack)
            } else {
                onBack()
            }
        }
        is Route.ServerManage -> ServersScreen(
            store = store,
            onAdd = { navEdit(0, route) },
            onOpen = { /* 在管理页点行不进终端，保持管理语义 */ },
            onEdit = { navEdit(it, route) },
            onSftp = { },
            onDocker = { },
            onBack = onBack
        )
        is Route.Settings -> SettingsScreen(onBack = onBack)
        is Route.Edit -> EditScreen(
            id = route.id, store = store,
            onDone = onBack, onCancel = onBack
        )
        else -> Unit
    }
}

/**
 * 顶部导航栏：分段 tab 居中（指标 / 终端 / 文件 / 容器 / 更多），
 * 对标 iPad 版顶部；右侧按页面放操作按钮。
 */
@Composable
private fun TopNavBar(
    selected: Route,
    onSelect: (Route) -> Unit,
    onAddServer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tabs = listOf(
        "指标" to Route.Metrics,
        "终端" to Route.Terminal,
        "文件" to Route.Files,
        "容器" to Route.Containers,
        "更多" to Route.More
    )
    val blue = ipadBlue()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左占位，保证 tab 居中
        Box(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEach { (label, r) ->
                val isSelected = selected == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface
                            else Color.Transparent
                        )
                        .clickable { onSelect(r) }
                        .padding(horizontal = 22.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) blue
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 右侧操作
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (selected) {
                Route.Metrics, Route.Files -> IconButton(onClick = onAddServer) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "添加服务器",
                        tint = blue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Route.Terminal -> IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> Unit
            }
        }
    }
}
