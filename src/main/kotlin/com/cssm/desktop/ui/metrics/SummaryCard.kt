package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadRing
import com.cssm.desktop.ui.components.IpadSubPanel
import com.cssm.desktop.ui.components.ipadBlue

/**
 * All Servers 汇总卡片（iPad 风格）：
 * 标题行 + CPU / Memory / Disk 大圆环 + Network / I/O 速率与曲线。
 */
@Composable
fun SummaryCard(
    servers: List<Server>,
    states: Map<Long, ServerMetrics>,
    modifier: Modifier = Modifier
) {
    val onlineCount = states.values.count { it.online && it.stats != null }
    val offlineCount = servers.size - onlineCount

    val onlineStats = states.values.filter { it.online && it.stats != null }.map { it.stats!! }
    val avgCpu = if (onlineStats.isNotEmpty()) onlineStats.map { it.cpuPercent }.average() else 0.0
    val usedCores = onlineStats.sumOf { it.cpuPercent / 100 * it.cpuCores }
    val totalCores = onlineStats.sumOf { it.cpuCores }
    val memUsedMb = onlineStats.sumOf { it.memUsedMb }
    val memTotalMb = onlineStats.sumOf { it.memTotalMb }
    val memPct = if (memTotalMb > 0) memUsedMb * 100.0 / memTotalMb else 0.0
    val diskUsedGb = onlineStats.sumOf { it.diskUsedGb }
    val diskTotalGb = onlineStats.sumOf { it.diskTotalGb }
    val diskPct = if (diskTotalGb > 0) diskUsedGb * 100.0 / diskTotalGb else 0.0
    val totalRx = onlineStats.sumOf { it.rxBytesPerSec }
    val totalTx = onlineStats.sumOf { it.txBytesPerSec }
    val totalIoR = onlineStats.sumOf { it.ioReadBytesPerSec }
    val totalIoW = onlineStats.sumOf { it.ioWriteBytesPerSec }

    val rxHist = aggregateHistory(states.values.map { it.rxHistory })
    val txHist = aggregateHistory(states.values.map { it.txHistory })
    val ioReadHist = aggregateHistory(states.values.map { it.ioReadHistory })
    val ioWriteHist = aggregateHistory(states.values.map { it.ioWriteHistory })

    val blue = ipadBlue()

    IpadCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(blue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = blue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Servers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text(text = "● $onlineCount Online", fontSize = 12.sp, color = Color(0xFF34C77B))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "● $offlineCount Offline", fontSize = 12.sp, color = Color(0xFFFF453A))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "● 0 Unknown", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = "${servers.size} Servers",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 三个子面板：CPU / Memory / Disk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IpadSubPanel(modifier = Modifier.weight(1f)) {
                    IpadRing(
                        label = "CPU",
                        pct = avgCpu,
                        sub = if (totalCores > 0) "%.1f / %d Cores".format(usedCores, totalCores) else "",
                        size = 76.dp,
                        stroke = 8.dp
                    )
                }
                IpadSubPanel(modifier = Modifier.weight(1f)) {
                    IpadRing(
                        label = "Memory",
                        pct = memPct,
                        sub = "${formatGb(memUsedMb / 1024.0)} / ${formatGb(memTotalMb / 1024.0)}",
                        size = 76.dp,
                        stroke = 8.dp
                    )
                }
                IpadSubPanel(modifier = Modifier.weight(1f)) {
                    IpadRing(
                        label = "Disk",
                        pct = diskPct,
                        sub = "${formatGb(diskUsedGb)} / ${formatGb(diskTotalGb)}",
                        size = 76.dp,
                        stroke = 8.dp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 网络 / I/O 子面板
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IpadSubPanel(modifier = Modifier.weight(1f)) {
                    NetDottedPanel(
                        label = "Network",
                        down = "↓ ${formatRate(totalRx)}",
                        up = "↑ ${formatRate(totalTx)}",
                        downValues = rxHist,
                        upValues = txHist
                    )
                }
                IpadSubPanel(modifier = Modifier.weight(1f)) {
                    NetDottedPanel(
                        label = "I/O",
                        down = "↓ ${formatRate(totalIoR)}",
                        up = "↑ ${formatRate(totalIoW)}",
                        downValues = ioReadHist,
                        upValues = ioWriteHist
                    )
                }
            }
        }
    }
}

/** 多台服务器的历史按索引相加（长度按最短对齐） */
private fun aggregateHistory(histories: List<List<Long>>): List<Long> {
    if (histories.isEmpty()) return emptyList()
    val minSize = histories.minOf { it.size }
    if (minSize == 0) return emptyList()
    return (0 until minSize).map { i ->
        histories.sumOf { it[it.size - minSize + i] }
    }
}
