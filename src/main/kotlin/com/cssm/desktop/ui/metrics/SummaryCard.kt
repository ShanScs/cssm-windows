package com.cssm.desktop.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server

/**
 * All Servers 汇总卡片：聚合所有在线服务器的 CPU / 内存 / 磁盘 / 网络 / IO。
 * 对标 iPad 版顶部的汇总条。
 */
@Composable
fun SummaryCard(
    servers: List<Server>,
    states: Map<Long, ServerMetrics>,
    modifier: Modifier = Modifier
) {
    val onlineCount = states.values.count { it.online && it.stats != null }
    val offlineCount = servers.size - onlineCount

    // 聚合
    val onlineStats = states.values.filter { it.online && it.stats != null }.map { it.stats!! }
    val avgCpu = if (onlineStats.isNotEmpty()) onlineStats.map { it.cpuPercent }.average() else 0.0
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

    // 汇总历史曲线（各服务器历史按索引相加）
    val rxHist = aggregateHistory(states.values.map { it.rxHistory })
    val txHist = aggregateHistory(states.values.map { it.txHistory })
    val ioHist = aggregateHistory(states.values.map { it.ioReadHistory + it.ioWriteHistory })

    val cpuColor = MaterialTheme.colorScheme.primary
    val memColor = Color(0xFFE8933C)  // iPad 版内存是橙色
    val diskColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Servers",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text(
                            text = "● $onlineCount Online",
                            fontSize = 12.sp,
                            color = Color(0xFF34C77B)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "● $offlineCount Offline",
                            fontSize = 12.sp,
                            color = Color(0xFFF87171)
                        )
                    }
                }
                Text(
                    text = "${servers.size} Servers",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // 三个大圆环
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricRing(
                    label = "CPU",
                    fraction = avgCpu / 100,
                    value = "${avgCpu.toInt()}%",
                    sub = "",
                    color = cpuColor,
                    size = 84.dp
                )
                MetricRing(
                    label = "Memory",
                    fraction = memPct / 100,
                    value = "${memPct.toInt()}%",
                    sub = "${formatGb(memUsedMb / 1024.0)} / ${formatGb(memTotalMb / 1024.0)}",
                    color = memColor,
                    size = 84.dp
                )
                MetricRing(
                    label = "Disk",
                    fraction = diskPct / 100,
                    value = "${diskPct.toInt()}%",
                    sub = "${formatGb(diskUsedGb)} / ${formatGb(diskTotalGb)}",
                    color = diskColor,
                    size = 84.dp
                )
            }

            Spacer(Modifier.height(16.dp))

            // 网络 / IO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryNetCol(
                    label = "Network",
                    down = "↓ ${formatRate(totalRx)}",
                    up = "↑ ${formatRate(totalTx)}"
                )
                Sparkline(down = rxHist, up = txHist, width = 120.dp, height = 36.dp)
                SummaryNetCol(
                    label = "I/O",
                    down = "↓ ${formatRate(totalIoR)}",
                    up = "↑ ${formatRate(totalIoW)}"
                )
                AreaChart(values = ioHist, width = 120.dp, height = 36.dp)
            }
        }
    }
}

@Composable
private fun SummaryNetCol(label: String, down: String, up: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(text = down, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = up, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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

