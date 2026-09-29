package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.MonitorStats
import com.cssm.desktop.ui.components.OsBadge
import kotlin.math.min

/**
 * 指标圆环：CPU / 内存 / 磁盘用。
 */
@Composable
fun MetricRing(
    label: String,
    fraction: Double,
    value: String,
    sub: String,
    color: Color,
    size: Dp = 64.dp
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = 7.dp.toPx()
                // 底环
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                // 进度环
                val f = min(1.0, fraction.coerceIn(0.0, 1.0)).toFloat()
                if (f > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = sub,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 单台服务器的指标卡片。
 * 参考 NeoServer iPad 的卡片信息密度，按 Cssm 自己的视觉语言实现。
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

    // 指标配色跟随主题
    val cpuColor = MaterialTheme.colorScheme.primary
    val memColor = MaterialTheme.colorScheme.tertiary
    val diskColor = MaterialTheme.colorScheme.secondary
    val offlineGray = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .alpha(if (!online && metrics != null) 0.6f else 1f)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：系统图标 + 国旗 + 名称 ... 运行时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                OsBadge(osId = server.osId, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val flag = server.regionFlag.ifBlank { "" }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (server.region.isNotBlank()) {
                            Text(
                                text = server.region,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = if (flag.isNotBlank()) "${server.displayName} $flag" else server.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val osName = server.osName.ifBlank {
                        when (server.osId) {
                            "ubuntu" -> "Ubuntu"
                            "debian" -> "Debian"
                            "centos", "rhel", "rocky", "almalinux" -> server.osId.replaceFirstChar { it.uppercase() }
                            "freebsd" -> "FreeBSD"
                            else -> ""
                        }
                    }
                    if (!online && metrics != null) {
                        Text(
                            text = "● 离线",
                            fontSize = 12.sp,
                            color = Color(0xFFF87171)
                        )
                    } else {
                        val dot = if (online) "● " else "● "
                        val dotColor = if (online) Color(0xFF34C77B) else Color(0xFFE8933C)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = dot, fontSize = 10.sp, color = dotColor)
                            if (osName.isNotBlank()) {
                                Text(
                                    text = osName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (hasData) {
                        Text(
                            text = "Uptime ${stats!!.uptimeText}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "›",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 指标区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (hasData) {
                    val s = stats!!
                    val m = metrics!!
                    MetricRing(
                        label = "CPU",
                        fraction = s.cpuPercent / 100,
                        value = "${s.cpuPercent.toInt()}%",
                        sub = "",
                        color = cpuColor
                    )
                    MetricRing(
                        label = "Memory",
                        fraction = s.memPercent / 100,
                        value = "${s.memPercent.toInt()}%",
                        sub = formatGb(s.memUsedMb / 1024.0),
                        color = memColor
                    )
                    MetricRing(
                        label = "Disk",
                        fraction = s.diskPercent / 100,
                        value = "${s.diskPercent.toInt()}%",
                        sub = formatGb(s.diskUsedGb),
                        color = diskColor
                    )
                    NetStatCol(
                        label = "Network",
                        down = "↓ ${formatRate(s.rxBytesPerSec)}",
                        up = "↑ ${formatRate(s.txBytesPerSec)}",
                        color = cpuColor,
                        sparkDown = m.rxHistory,
                        sparkUp = m.txHistory
                    )
                    NetStatCol(
                        label = "I/O",
                        down = "↓ ${formatRate(s.ioReadBytesPerSec)}",
                        up = "↑ ${formatRate(s.ioWriteBytesPerSec)}",
                        color = diskColor,
                        sparkDown = m.ioReadHistory,
                        sparkUp = m.ioWriteHistory,
                        areaChart = true
                    )
                } else {
                    MetricRing(label = "CPU", fraction = 0.0, value = "--", sub = "", color = offlineGray)
                    MetricRing(label = "Memory", fraction = 0.0, value = "--", sub = "", color = offlineGray)
                    MetricRing(label = "Disk", fraction = 0.0, value = "--", sub = "", color = offlineGray)
                    NetStatCol(label = "Network", down = "↓ --", up = "↑ --", color = offlineGray)
                    NetStatCol(label = "I/O", down = "↓ --", up = "↑ --", color = offlineGray)
                }
            }
        }
    }
}

@Composable
private fun NetStatCol(
    label: String,
    up: String,
    down: String,
    color: Color,
    sparkDown: List<Long> = emptyList(),
    sparkUp: List<Long> = emptyList(),
    areaChart: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(text = down, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = up, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        if (areaChart) {
            AreaChart(values = sparkDown + sparkUp, width = 64.dp, height = 24.dp)
        } else {
            Sparkline(down = sparkDown, up = sparkUp, width = 64.dp, height = 24.dp)
        }
    }
}
