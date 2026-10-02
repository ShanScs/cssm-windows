package com.cssm.desktop.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.FolderResourceCard
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.EmptyState
import com.cssm.desktop.ui.components.ServerResourceCard
import com.cssm.desktop.ui.components.cardEntrance
import com.cssm.desktop.ui.components.CenteredGridContainer
import com.cssm.desktop.ui.components.ipadBlue
import kotlinx.coroutines.launch

/**
 * 文件页（资源库卡片风格）：本地文件夹卡 + 服务器卡片网格，点击服务器进入 SFTP。
 */
@Composable
fun FilesScreen(
    store: ServerStore,
    onOpenSftp: (Long) -> Unit,
    onOpenLocal: () -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by store.servers.collectAsState()
    val blue = ipadBlue()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val regions = remember(servers) {
        servers.map { it.region.ifBlank { "未知" } }.distinct()
            .map { region ->
                val flag = servers.firstOrNull {
                    (it.region.ifBlank { "未知" }) == region
                }?.regionFlag.orEmpty()
                region to flag
            }
    }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    val filtered = remember(servers, selectedRegion) {
        if (selectedRegion == null) servers
        else servers.filter { (it.region.ifBlank { "未知" }) == selectedRegion }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            IpadTitle(
                text = "文件",
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 8.dp)
            )
            if (servers.isNotEmpty()) {
                RegionSegment(
                    regions = regions,
                    selected = selectedRegion,
                    onSelect = { selectedRegion = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            CenteredGridContainer {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "local") {
                    FolderResourceCard(
                        title = "本地",
                        subtitle = "本地文件",
                        icon = Icons.Filled.Folder,
                        iconTint = Color(0xFFF5A623),
                        onClick = onOpenLocal
                    )
                }
                itemsIndexed(filtered, key = { _, s -> s.id }) { index, server ->
                    ServerResourceCard(
                        server = server,
                        onClick = { onOpenSftp(server.id) },
                        modifier = Modifier.cardEntrance(index)
                    )
                }
                if (filtered.isEmpty()) {
                    item(span = { GridItemSpan(3) }, key = "hint") {
                        EmptyState(
                            icon = Icons.Filled.Folder,
                            title = "还没有服务器",
                            subtitle = "添加服务器后，通过 SFTP 管理它的文件",
                            actionLabel = "添加服务器",
                            onAction = onAddServer
                        )
                    }
                }
            }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}
