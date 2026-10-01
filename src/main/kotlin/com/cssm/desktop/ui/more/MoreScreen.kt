package com.cssm.desktop.ui.more

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadMoreRow
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.launch

/**
 * 更多页（iPad 风格）：脚本 / 服务器 / 私钥 / 设置 / AI Chat / 转发 / 通知 / 搜索。
 * 尚未实现的功能点击后提示「即将推出」。
 */
@Composable
fun MoreScreen(
    onServers: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun soon() = scope.launch { snackbar.showSnackbar("即将推出") }

    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            IpadTitle(
                text = "更多",
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp)
            ) {
                item(key = "main") {
                    IpadCard {
                        IpadMoreRow(
                            icon = Icons.Filled.Terminal,
                            iconBg = Color(0xFF34C77B),
                            label = "脚本",
                            onClick = { soon() }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.Dns,
                            iconBg = Color(0xFF007AFF),
                            label = "服务器",
                            onClick = onServers
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.Key,
                            iconBg = Color(0xFFAF52DE),
                            label = "私钥",
                            onClick = { soon() }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.Settings,
                            iconBg = Color(0xFF34C77B),
                            label = "设置",
                            onClick = onSettings
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.ChatBubble,
                            iconBg = Color(0xFF007AFF),
                            label = "AI Chat",
                            onClick = { soon() }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.Share,
                            iconBg = Color(0xFFAF52DE),
                            label = "转发",
                            onClick = { soon() }
                        )
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                        IpadMoreRow(
                            icon = Icons.Filled.Notifications,
                            iconBg = Color(0xFF34C77B),
                            label = "通知",
                            onClick = { soon() }
                        )
                    }
                }
                item(key = "sp") { Spacer(Modifier.height(16.dp)) }
                item(key = "search") {
                    IpadCard {
                        IpadMoreRow(
                            icon = Icons.Filled.Search,
                            iconBg = Color(0xFF007AFF),
                            label = "搜索",
                            subtitle = "探索批量管理和用工具方式，克隆并用 NeoServer,",
                            onClick = { soon() }
                        )
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
