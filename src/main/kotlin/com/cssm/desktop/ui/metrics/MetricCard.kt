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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.MonitorStats
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadRing
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.ipadName
import com.cssm.desktop.ui.components.osDisplayName
import kotlin.math.roundToInt

/**
 * 单台服务器的指标卡片（iPad 风格）：
 * 标题行 = 系统图标 + 地区国旗名称 + OS / 右侧 Uptime + 箭头；
 * 主体 = CPU / Mem / 磁盘小圆环 + 网络 / I/O 四行数值。
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
    val hasData = stats != null
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
                    Text(
                        text = server.ipadName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    if (!online && metrics != null) {
                        Text(
                            text = "● 离线",
                            fontSize = 12.sp,
                            color = Color(0xFFFF453A)
                        )
                    } else {
                        val os = osDisplayName(server.osId, server.osName)
                        if (os.isNotBlank()) {
                            Text(
                                text = os,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

            // 指标区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (hasData) {
                    val s = stats!!
                    val usedCores = if (s.cpuCores > 0) {
                        "${(s.cpuPercent / 100 * s.cpuCores).roundToInt()} C"
                    } else ""
                    IpadRing(
                        label = "CPU",
                        pct = s.cpuPercent,
                        sub = usedCores,
                        size = 48.dp
                    )
                    IpadRing(
                        label = "Mem",
                        pct = s.memPercent,
                        sub = formatGb(s.memUsedMb / 1024.0),
                        size = 48.dp
                    )
                    IpadRing(
                        label = "磁盘",
                        pct = s.diskPercent,
                        sub = formatGb(s.diskUsedGb),
                        size = 48.dp
                    )
                    NetStatCol(
                        label = "网络",
                        upRate = formatBytes(s.txBytesPerSec),
                        upTotal = formatBytes(s.txTotalBytes),
                        downRate = formatBytes(s.rxBytesPerSec),
                        downTotal = formatBytes(s.rxTotalBytes)
                    )
                    NetStatCol(
                        label = "I/O",
                        upRate = formatBytes(s.ioWriteBytesPerSec),
                        upTotal = formatBytes(s.ioWriteTotalBytes),
                        downRate = formatBytes(s.ioReadBytesPerSec),
                        downTotal = formatBytes(s.ioReadTotalBytes)
                    )
                } else {
                    IpadRing(label = "CPU", pct = 0.0, sub = "", size = 48.dp, valueText = "--")
                    IpadRing(label = "Mem", pct = 0.0, sub = "", size = 48.dp, valueText = "--")
                    IpadRing(label = "磁盘", pct = 0.0, sub = "", size = 48.dp, valueText = "--")
                    NetStatCol(label = "网络", upRate = "--", upTotal = "", downRate = "--", downTotal = "")
                    NetStatCol(label = "I/O", upRate = "--", upTotal = "", downRate = "--", downTotal = "")
                }
            }
        }
    }
}

/** 网络 / I/O 四行数值列：↑速率 / ↑累计 / ↓速率 / ↓累计 */
@Composable
private fun NetStatCol(
    label: String,
    upRate: String,
    upTotal: String,
    downRate: String,
    downTotal: String
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = onVariant)
        Spacer(Modifier.height(4.dp))
        Text(text = "↑$upRate", fontSize = 11.sp, color = onSurface, maxLines = 1)
        if (upTotal.isNotEmpty()) {
            Text(text = upTotal, fontSize = 10.sp, color = onVariant, maxLines = 1)
        }
        Text(text = "↓$downRate", fontSize = 11.sp, color = onSurface, maxLines = 1)
        if (downTotal.isNotEmpty()) {
            Text(text = downTotal, fontSize = 10.sp, color = onVariant, maxLines = 1)
        }
    }
}
