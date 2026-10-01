package com.cssm.desktop.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.RegionSegment
import com.cssm.desktop.ui.components.ipadBlue
import com.cssm.desktop.ui.components.ipadName
import kotlinx.coroutines.launch

/**
 * 文件页（iPad 风格）：本地 + 服务器列表，点击服务器进入 SFTP。
 */
@Composable
fun FilesScreen(
    store: ServerStore,
    onOpenSftp: (Long) -> Unit,
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp)
            ) {
                item(key = "list") {
                    IpadCard {
                        // 本地
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { snackbar.showSnackbar("本地文件即将推出") }
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = blue,
                                modifier = Modifier.width(38.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "本地",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "›",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        // 服务器
                        filtered.forEach { server ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 60.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSftp(server.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OsBadge(osId = server.osId, size = 38.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = server.ipadName(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "›",
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
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
