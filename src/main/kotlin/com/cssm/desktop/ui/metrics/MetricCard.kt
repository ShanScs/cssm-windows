package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.MonitorStats
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadRing
import com.cssm.desktop.ui.components.IpadSubPanel
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.ServerNameWithFlag
import com.cssm.desktop.ui.components.osDisplayName
import kotlin.math.roundToInt

/**
 * 单台服务器的指标卡片（iPad 风格）：
 * 标题行 = 系统图标 + 地区国旗名称 + 状态点/OS / 右侧 Uptime + 箭头；
 * 主体 = 上排 CPU / Memory / Disk，下排实时网速 / 总用量 / I/O 等高三卡。
 */
@Composable
fun ServerMetricCard(
    server: Server,
    metrics: ServerMetrics?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats: MonitorStats? = metrics?.stats
    val online = metrics?.online != false
    val m = metrics
    val hasData = m != null && stats != null
    val dimmed = !online && metrics != null

    IpadCard(
        modifier = modifier
            .alpha(if (dimmed) 0.6f else 1f)
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                OsBadge(osId = server.osId, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ServerNameWithFlag(
                        server = server,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    // 第二行：状态点 + 系统名（iPad 风格；未知系统显示占位文案）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(online = online, hasData = hasData)
                        Spacer(Modifier.width(4.dp))
                        val os = osDisplayName(server.osId, server.osName).ifBlank { "未知系统" }
                        Text(
                            text = os,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Uptime",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "›",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 上排：CPU / Memory / Disk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasData) {
                    val s = stats!!
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(
                            label = "CPU",
                            pct = s.cpuPercent,
                            sub = if (s.cpuCores > 0) {
                                "${(s.cpuPercent / 100 * s.cpuCores).roundToInt()} C"
                            } else "",
                            size = 44.dp
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(
                            label = "Memory",
                            pct = s.memPercent,
                            sub = formatGb(s.memUsedMb / 1024.0),
                            size = 44.dp
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(
                            label = "Disk",
                            pct = s.diskPercent,
                            sub = formatGb(s.diskUsedGb),
                            size = 44.dp
                        )
                    }
                } else {
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(label = "CPU", pct = 0.0, sub = "", size = 44.dp, valueText = "—")
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(label = "Memory", pct = 0.0, sub = "", size = 44.dp, valueText = "—")
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        IpadRing(label = "Disk", pct = 0.0, sub = "", size = 44.dp, valueText = "—")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 下排：实时网速 / 总用量 / I/O，各按内容自然高度，顶部对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasData) {
                    val s = stats!!
                    val mm = m!!
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "实时网速",
                            down = "↓${formatRate(s.rxBytesPerSec)}",
                            up = "↑${formatRate(s.txBytesPerSec)}",
                            downValues = mm.rxHistory,
                            upValues = mm.txHistory,
                            sparkWidth = 56.dp,
                            sparkHeight = 20.dp,
                            stacked = true
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "总用量",
                            down = "↓${formatBytes(s.rxTotalBytes)}",
                            up = "↑${formatBytes(s.txTotalBytes)}",
                            downValues = emptyList(),
                            upValues = emptyList(),
                            showSpark = false,
                            stacked = true
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "I/O",
                            down = "↓${formatRate(s.ioReadBytesPerSec)}",
                            up = "↑${formatRate(s.ioWriteBytesPerSec)}",
                            downValues = mm.ioReadHistory,
                            upValues = mm.ioWriteHistory,
                            sparkWidth = 56.dp,
                            sparkHeight = 20.dp,
                            stacked = true
                        )
                    }
                } else {
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "实时网速",
                            down = "↓—",
                            up = "↑—",
                            downValues = emptyList(),
                            upValues = emptyList(),
                            stacked = true
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "总用量",
                            down = "↓—",
                            up = "↑—",
                            downValues = emptyList(),
                            upValues = emptyList(),
                            showSpark = false,
                            stacked = true
                        )
                    }
                    IpadSubPanel(modifier = Modifier.weight(1f)) {
                        NetDottedPanel(
                            label = "I/O",
                            down = "↓—",
                            up = "↑—",
                            downValues = emptyList(),
                            upValues = emptyList(),
                            stacked = true
                        )
                    }
                }
            }
        }
    }
}
