package com.cssm.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.AppIconPainter
import com.cssm.desktop.ui.components.CustomTitleBar
import com.cssm.desktop.ui.components.WindowDisappearDiagnose
import com.cssm.desktop.ui.components.WindowResizeHandles
import com.cssm.desktop.ui.components.ipadBlue
import com.cssm.desktop.ui.containers.ContainersScreen
import com.cssm.desktop.ui.files.FilesScreen
import com.cssm.desktop.ui.metrics.MetricsRepository
import com.cssm.desktop.ui.metrics.MetricsScreen
import com.cssm.desktop.ui.metrics.CarrierPing
import com.cssm.desktop.ui.metrics.collectLoop
import com.cssm.desktop.ui.more.MoreScreen
import com.cssm.desktop.ui.servers.EditScreen
import com.cssm.desktop.ui.docker.DockerScreen
import com.cssm.desktop.ui.sftp.SftpScreen
import com.cssm.desktop.ui.servers.ServersScreen
import com.cssm.desktop.ui.session.SessionScreen
import com.cssm.desktop.data.AlertStore
import com.cssm.desktop.data.ForwardStore
import com.cssm.desktop.data.KeyStore
import com.cssm.desktop.data.ScriptStore
import com.cssm.desktop.ssh.ForwardManager
import com.cssm.desktop.ui.aichat.AiChatScreen
import com.cssm.desktop.ui.files.LocalFilesScreen
import com.cssm.desktop.ui.forward.ForwardScreen
import com.cssm.desktop.ui.keys.KeyScreen
import com.cssm.desktop.ui.notify.NotifyEngine
import com.cssm.desktop.ui.notify.NotifyScreen
import com.cssm.desktop.ui.scripts.ScriptScreen
import com.cssm.desktop.ui.search.SearchScreen
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
    // 设置页（原"更多"）的二级页
    data object ServerManage : Route
    data object Scripts : Route
    data object Keys : Route
    data object AiChat : Route
    data object Forward : Route
    data object Notify : Route
    data object Search : Route
    data object LocalFiles : Route
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
                    // 重连时取服务器最新信息（改密码/改主机后自动用新配置恢复）
                    collectLoop(server, metricsRepo) {
                        store.servers.value.firstOrNull { it.id == server.id }
                    }
                }
            }
        }
        metricsRepo.states.value.keys.filter { it !in ids }.forEach { metricsRepo.remove(it) }
    }

    // 功能数据存储（App 级单例）
    val scriptStore = remember { ScriptStore() }
    val keyStore = remember { KeyStore() }
    val forwardStore = remember { ForwardStore() }
    val alertStore = remember { AlertStore() }
    val forwardManager = remember { ForwardManager() }

    // 告警引擎：App 存活期间每 30 秒检查一次
    LaunchedEffect(Unit) {
        NotifyEngine.start(appScope, alertStore, store, metricsRepo)
        CarrierPing.start(appScope)
    }

    fun nav(target: Route, ret: Route? = null) {
        if (ret != null) returnRoute = ret
        route = target
    }

    CssmTheme(themeMode = themeMode) {
        val windowState = rememberWindowState(width = 1100.dp, height = 720.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cssm",
            state = windowState,
            undecorated = true,
            transparent = true,
            icon = remember { AppIconPainter() }
        ) {
            WindowDisappearDiagnose(windowState)
            val maximized = windowState.placement == WindowPlacement.Maximized
            val corner = if (maximized) 0.dp else 8.dp
            // 透明窗口 + Compose 抗锯齿圆角背景（替代 AWT shape 硬裁剪，避免圆角毛边）；
            // 最大化时直角
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(corner))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CustomTitleBar(
                        state = windowState,
                        onCloseRequest = ::exitApplication
                    )
                    // Surface 提供默认内容色（LocalContentColor），深色下普通 Text 自动用浅色
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background
                    ) {
            val r = route
            if (r is Route.Session || r is Route.Sftp ||
                r is Route.Docker || r is Route.ServerManage || r is Route.Scripts ||
                r is Route.Keys || r is Route.AiChat || r is Route.Forward ||
                r is Route.Notify || r is Route.Search || r is Route.LocalFiles
            ) {
                FullContent(
                    route = r,
                    store = store,
                    scriptStore = scriptStore,
                    keyStore = keyStore,
                    forwardStore = forwardStore,
                    alertStore = alertStore,
                    forwardManager = forwardManager,
                    onBack = { route = returnRoute },
                    navEdit = { id, ret -> nav(Route.Edit(id), ret) },
                    navSession = { id, ret -> nav(Route.Session(id), ret) }
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
                        onOpenSettings = { route = Route.More }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Box(Modifier.weight(1f)) {
                        // Route.Edit 时背景显示返回目标的 tab 内容
                        val tabR = if (r is Route.Edit) returnRoute else r
                        when (tabR) {
                            Route.Metrics -> MetricsScreen(
                                store = store,
                                repo = metricsRepo,
                                onOpen = { id -> nav(Route.Edit(id), r) },
                                onAddServer = { nav(Route.Edit(0), r) }
                            )
                            Route.Terminal -> TerminalScreen(
                                store = store,
                                onOpen = { id -> nav(Route.Session(id), r) },
                                onAddServer = { nav(Route.Edit(0), r) }
                            )
                            Route.Files -> FilesScreen(
                                store = store,
                                onOpenSftp = { id -> nav(Route.Sftp(id), r) },
                                onOpenLocal = { nav(Route.LocalFiles, r) },
                                onAddServer = { nav(Route.Edit(0), r) }
                            )
                            Route.Containers -> ContainersScreen(
                                store = store,
                                onDocker = { id -> nav(Route.Docker(id), r) },
                                onSession = { id -> nav(Route.Session(id), r) },
                                onSftp = { id -> nav(Route.Sftp(id), r) },
                                onAddServer = { nav(Route.Edit(0), r) }
                            )
                            Route.More -> MoreScreen(
                                onServers = { nav(Route.ServerManage, r) },
                                onScripts = { nav(Route.Scripts, r) },
                                onKeys = { nav(Route.Keys, r) },
                                onAiChat = { nav(Route.AiChat, r) },
                                onForward = { nav(Route.Forward, r) },
                                onNotify = { nav(Route.Notify, r) },
                                onSearch = { nav(Route.Search, r) }
                            )
                            else -> Unit
                        }
                        // 添加/编辑服务器弹 Dialog，背景透出当前 tab
                        if (r is Route.Edit) {
                            EditScreen(
                                id = r.id, store = store, keyStore = keyStore,
                                onDone = { route = returnRoute },
                                onCancel = { route = returnRoute }
                            )
                        }
                    }
                }
            }
            }
                } // Column
                    } // 圆角内容 Box
                    if (!maximized) {
                        WindowResizeHandles()
                    }
                } // 透明根 Box
            }
    }
}

@Composable
private fun FullContent(
    route: Route,
    store: ServerStore,
    scriptStore: ScriptStore,
    keyStore: KeyStore,
    forwardStore: ForwardStore,
    alertStore: AlertStore,
    forwardManager: ForwardManager,
    onBack: () -> Unit,
    navEdit: (Long, Route) -> Unit,
    navSession: (Long, Route) -> Unit
) {
    val servers by store.servers.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
        // 设置页子项：弹 Dialog 小卡片，背景保留设置页
        is Route.ServerManage, is Route.Scripts, is Route.Keys, is Route.AiChat,
        is Route.Forward, is Route.Notify, is Route.Search -> {
            // 背景：设置页（不可交互，被 Dialog 挡住）
            MoreScreen(
                onServers = {}, onScripts = {}, onKeys = {}, onAiChat = {},
                onForward = {}, onNotify = {}, onSearch = {}
            )
            Dialog(onDismissRequest = onBack) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    when (route) {
                        is Route.ServerManage -> ServersScreen(
                            store = store,
                            onAdd = { navEdit(0, route) },
                            onOpen = { },
                            onEdit = { navEdit(it, route) },
                            onSftp = { },
                            onDocker = { },
                            onBack = onBack
                        )
                        is Route.Scripts -> ScriptScreen(
                            store = scriptStore, servers = servers, onBack = onBack
                        )
                        is Route.Keys -> KeyScreen(
                            store = keyStore, onBack = onBack
                        )
                        is Route.AiChat -> AiChatScreen(onBack = onBack)
                        is Route.Forward -> ForwardScreen(
                            store = forwardStore, manager = forwardManager,
                            servers = servers, onBack = onBack
                        )
                        is Route.Notify -> NotifyScreen(
                            store = alertStore, servers = servers, onBack = onBack
                        )
                        is Route.Search -> SearchScreen(
                            servers = servers,
                            onOpenSession = { id -> navSession(id, route) },
                            onBack = onBack
                        )
                        else -> Unit
                    }
                }
            }
        }
        is Route.LocalFiles -> LocalFilesScreen(onBack = onBack)
        else -> Unit
    }
    }
}

/**
 * 顶部导航栏：分段 tab 居中（指标 / 终端 / 文件 / 容器 / 设置），
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
        Triple("指标", Route.Metrics, Icons.Filled.Speed),
        Triple("终端", Route.Terminal, Icons.Filled.Terminal),
        Triple("文件", Route.Files, Icons.Filled.Folder),
        Triple("容器", Route.Containers, Icons.Filled.Inventory2),
        Triple("设置", Route.More, Icons.Filled.Settings)
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
            tabs.forEach { (label, r, icon) ->
                val isSelected = selected == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface
                            else Color.Transparent
                        )
                        .clickable { onSelect(r) }
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) blue
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
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
