package com.cssm.desktop.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadMoreRow
import com.cssm.desktop.ui.theme.UiPrefs
import com.cssm.desktop.ui.components.IpadTitle
import com.cssm.desktop.ui.theme.ThemeMode
import com.cssm.desktop.ui.theme.ThemePrefs

/**
 * 设置页（原"更多"）：脚本 / 服务器 / 私钥 / AI Chat / 转发 / 通知 / 搜索，
 * 下方直接放外观主题与关于（独立设置页已删除）。
 */
@Composable
fun MoreScreen(
    onServers: () -> Unit,
    onScripts: () -> Unit,
    onKeys: () -> Unit,
    onAiChat: () -> Unit,
    onForward: () -> Unit,
    onNotify: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode by ThemePrefs.mode.collectAsState()
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxSize()) {
        IpadTitle(
            text = "设置",
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
                        onClick = onScripts
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
                        onClick = onKeys
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                    IpadMoreRow(
                        icon = Icons.Filled.ChatBubble,
                        iconBg = Color(0xFF007AFF),
                        label = "AI Chat",
                        onClick = onAiChat
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                    IpadMoreRow(
                        icon = Icons.Filled.Share,
                        iconBg = Color(0xFFAF52DE),
                        label = "转发",
                        onClick = onForward
                    )
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 62.dp))
                    IpadMoreRow(
                        icon = Icons.Filled.Notifications,
                        iconBg = Color(0xFF34C77B),
                        label = "通知",
                        onClick = onNotify
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
                        subtitle = "搜索服务器名称、主机、用户名",
                        onClick = onSearch
                    )
                }
            }
            item(key = "sp2") { Spacer(Modifier.height(16.dp)) }
            item(key = "appearance") {
                IpadCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeOption("跟随系统", ThemeMode.FOLLOW_SYSTEM, mode)
                            ThemeOption("深色", ThemeMode.DARK, mode)
                            ThemeOption("浅色", ThemeMode.LIGHT, mode)
                        }
                        val compact by UiPrefs.compactMetrics.collectAsState()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "指标卡紧凑模式",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "只显示标题、CPU、内存和网速",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = compact,
                                onCheckedChange = { UiPrefs.setCompactMetrics(it) }
                            )
                        }
                    }
                }
            }
            item(key = "sp3") { Spacer(Modifier.height(16.dp)) }
            item(key = "about") {
                IpadCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Cssm 桌面版 1.2.7\n服务器管理 · SSH 终端",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(label: String, value: ThemeMode, current: ThemeMode) {
    OutlinedButton(
        onClick = { ThemePrefs.setMode(value) },
        enabled = value != current
    ) { Text(label) }
}
