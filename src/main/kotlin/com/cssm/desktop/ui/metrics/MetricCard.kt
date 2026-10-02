package com.cssm.desktop.ui.metrics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.MonitorStats
import com.cssm.desktop.ui.components.FlagImage
import com.cssm.desktop.ui.components.IpadCard
import com.cssm.desktop.ui.components.IpadSubPanel
import com.cssm.desktop.ui.components.OsBadge
import com.cssm.desktop.ui.components.hoverBorder
import com.cssm.desktop.ui.theme.MonoDigits

private val NeoGreen = Color(0xFF3FB950)
private val NeoBlue = Color(0xFF58A6FF)
private val NeoOrange = Color(0xFFF0883E)
private val NeoRed = Color(0xFFFF453A)

/**
 * 单台服务器的指标卡片（NeoServer 风格）：
 * 标题行（状态点 + 地区名称 … 刷新/系统图标/国旗）→ 在线天数/价格 pill →
 * CPU/内存/硬盘/流量四行（百分比 + 进度条 + 明细）→
 * 网速/总量/到期三小面板 → 三网延迟 → 总流量/丢包+延迟柱状图。
 * compact=true 时只保留标题/pill/CPU/内存/网速单行（约一半高度）。
 * 点击卡片进入服务器信息编辑。
 */
@Composable
fun ServerMetricCard(
    server: Server,
    metrics: ServerMetrics?,
    ping: PingState,
    onOpen: () -> Unit,
    onPingRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val stats: MonitorStats? = metrics?.stats
    val online = metrics?.online != false
    val hasData = metrics != null && stats != null
    val dimmed = !online && metrics != null
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gray = MaterialTheme.colorScheme.onSurfaceVariant

    IpadCard(
        modifier = modifier
            .alpha(if (dimmed) 0.6f else 1f)
            .hoverBorder(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ---- 标题行 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotOn = online && hasData
                val dotPulse = rememberInfiniteTransition(label = "card-dot")
                val dotAlpha by dotPulse.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "card-dot-alpha"
                )
                Box(
                    modifier = Modifier.size(8.dp)
                        .alpha(if (dotOn) dotAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (dotOn) NeoGreen else NeoRed)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = cardTitle(server),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPingRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "刷新延迟",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(2.dp))
                OsBadge(osId = server.osId, size = 26.dp)
                if (server.regionFlag.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    FlagImage(flag = server.regionFlag, height = 16.dp)
                }
            }

            // ---- pill 行：在线天数 / 价格 ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                NeoPill(
                    text = when {
                        hasData -> "在线 ${stats!!.uptimeSec / 86400} 天"
                        metrics != null && !online -> "离线"
                        else -> "连接中"
                    }
                )
                if (server.price.isNotBlank()) NeoPill(text = server.price)
            }

            Spacer(Modifier.height(12.dp))

            // ---- CPU / 内存 ----
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricBar(
                    label = "CPU",
                    pctText = if (hasData) "%.1f%%".format(stats!!.cpuPercent) else "—",
                    pct = if (hasData) stats!!.cpuPercent else 0.0,
                    sub = if (hasData) "%.2f, %.2f, %.2f".format(stats!!.load1, stats!!.load5, stats!!.load15) else "—",
                    modifier = Modifier.weight(1f)
                )
                MetricBar(
                    label = "内存",
                    pctText = if (hasData) "%.1f%%".format(stats!!.memPercent) else "—",
                    pct = if (hasData) stats!!.memPercent else 0.0,
                    sub = if (hasData) "${formatMb(stats!!.memUsedMb)} / ${formatMb(stats!!.memTotalMb)}" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
            if (compact) {
                // ---- 紧凑模式：网速单行 ----
                Spacer(Modifier.height(10.dp))
                IpadSubPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp, vertical = 9.dp
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↑ ${if (hasData) formatRate(stats!!.txBytesPerSec) else "—"}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeoGreen,
                            fontFamily = MonoDigits,
                            modifier = Modifier.weight(1f), maxLines = 1
                        )
                        Text(
                            text = "↓ ${if (hasData) formatRate(stats!!.rxBytesPerSec) else "—"}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeoBlue,
                            fontFamily = MonoDigits,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!compact) {
            Spacer(Modifier.height(10.dp))

            // ---- 硬盘 / 流量 ----
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricBar(
                    label = "硬盘",
                    pctText = if (hasData) "%.1f%%".format(stats!!.diskPercent) else "—",
                    pct = if (hasData) stats!!.diskPercent else 0.0,
                    sub = if (hasData) "${formatGb(stats!!.diskUsedGb)} / ${formatGb(stats!!.diskTotalGb)}" else "—",
                    modifier = Modifier.weight(1f)
                )
                val quotaBytes = server.trafficQuotaGb * 1024L * 1024L * 1024L
                val hasQuota = quotaBytes > 0
                val txPct = if (hasData && hasQuota)
                    (stats!!.txTotalBytes * 100.0 / quotaBytes).coerceIn(0.0, 100.0) else 0.0
                MetricBar(
                    label = "流量",
                    pctText = if (hasData && hasQuota) "%.1f%%".format(txPct) else "",
                    pct = txPct,
                    pctColor = NeoGreen,
                    showBar = hasQuota,
                    sub = if (hasData) {
                        val used = formatBigBytes(stats!!.txTotalBytes)
                        if (hasQuota) "$used / ${formatGb(server.trafficQuotaGb)}" else used
                    } else "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---- 三小面板：网速 / 总量 / 到期 ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IpadSubPanel(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) {
                    MiniLine(text = "↑ ${if (hasData) formatRate(stats!!.txBytesPerSec) else "—"}", color = NeoGreen)
                    Spacer(Modifier.height(6.dp))
                    MiniLine(text = "↓ ${if (hasData) formatRate(stats!!.rxBytesPerSec) else "—"}", color = NeoBlue)
                }
                IpadSubPanel(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) {
                    MiniLine(text = "↑ ${if (hasData) formatBigBytes(stats!!.txTotalBytes) else "—"}", color = gray)
                    Spacer(Modifier.height(6.dp))
                    MiniLine(text = "↓ ${if (hasData) formatBigBytes(stats!!.rxTotalBytes) else "—"}", color = gray)
                }
                // 计费信息：没填到期/续费价就不显示，避免一排"—"
                if (server.expireAt > 0 || server.renewPrice.isNotBlank()) {
                    IpadSubPanel(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                    ) {
                        val days = remainingDays(server.expireAt)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DateRange, null, tint = gray, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (server.expireAt > 0) {
                                    if (days >= 0) "剩余 $days 天" else "已到期"
                                } else "—",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface, maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AttachMoney, null, tint = gray, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = server.renewPrice.ifBlank { "—" },
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface, maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- 三网 ----
            IpadSubPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "三网", fontSize = 12.sp, color = gray, modifier = Modifier.weight(1f))
                    CARRIERS.forEachIndexed { i, c ->
                        if (i > 0) Spacer(Modifier.width(14.dp))
                        Text(text = c.name, fontSize = 12.sp, color = gray)
                        Spacer(Modifier.width(4.dp))
                        if (c.name in ping.unreachable) {
                            Text(
                                text = "不可达",
                                fontSize = 12.sp, color = gray
                            )
                        } else {
                            Text(
                                text = ping.latencies[c.name]?.toString() ?: "—",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.color,
                                fontFamily = MonoDigits
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- 底部：总流量 / 丢包 ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IpadSubPanel(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "总流量", fontSize = 12.sp, color = gray, modifier = Modifier.weight(1f))
                        Text(
                            text = if (hasData) formatBigBytes(stats!!.txTotalBytes + stats!!.rxTotalBytes) else "—",
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = onSurface
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hasData)
                            "↑ ${formatBigBytes(stats!!.txTotalBytes)}    ↓ ${formatBigBytes(stats!!.rxTotalBytes)}"
                        else "—",
                        fontSize = 11.sp, color = gray, maxLines = 1
                    )
                }
                IpadSubPanel(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                ) {
                    // 丢包率按阈值变色：<1% 绿，<5% 橙，≥5% 红；三网全不可达则不显示
                    val allUnreachable = ping.unreachable.size >= CARRIERS.size
                    val lossColor = when {
                        allUnreachable -> onSurface
                        ping.lossPct < 1.0 -> NeoGreen
                        ping.lossPct < 5.0 -> NeoOrange
                        else -> NeoRed
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "丢包", fontSize = 12.sp, color = gray, modifier = Modifier.weight(1f))
                        Text(
                            text = if (allUnreachable) "—" else "%.1f%%".format(ping.lossPct),
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = lossColor,
                            fontFamily = MonoDigits
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    PingBars(bars = ping.bars, modifier = Modifier.fillMaxWidth())
                }
            }
            } // if (!compact)
        }
    }
}

/** 标题：地区 + 名称（去重，空格分隔） */
private fun cardTitle(s: Server): String {
    val r = s.region.trim()
    val n = s.name.trim().ifBlank { s.host }
    return when {
        r.isEmpty() -> n
        r.startsWith(n) -> r
        n.startsWith(r) -> n
        else -> "$r $n"
    }
}

/** 到期剩余天数（按自然日） */
private fun remainingDays(expireAt: Long): Long {
    if (expireAt <= 0) return Long.MIN_VALUE
    return (expireAt - System.currentTimeMillis()) / 86_400_000L
}

/** 灰色小 pill，如「在线 61 天」「$10.99 / 年」 */
@Composable
private fun NeoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 百分比行：label 左灰字 / 百分比右白字 + 进度条 + 明细灰字；无配额时不显示百分比和进度条 */
@Composable
private fun RowScope.MetricBar(
    label: String,
    pctText: String,
    pct: Double,
    sub: String,
    modifier: Modifier = Modifier,
    pctColor: Color = MaterialTheme.colorScheme.onSurface,
    showBar: Boolean = true
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (showBar) {
                Text(
                    text = pctText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = pctColor
                )
            }
        }
        if (showBar) {
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeoGreen)
                )
            }
            Spacer(Modifier.height(5.dp))
        } else {
            Spacer(Modifier.height(5.dp))
        }
        Text(
            text = sub,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = MonoDigits,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 小面板里的一行：箭头 + 数值 */
@Composable
private fun MiniLine(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        fontFamily = MonoDigits,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** 丢包面板里的延迟柱状图：绿=正常（高度∝延迟），红=整轮超时 */
@Composable
private fun PingBars(bars: List<Float?>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(26.dp)) {
        val n = bars.size
        if (n == 0) return@Canvas
        val gap = 3.dp.toPx()
        val bw = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        val maxV = (bars.filterNotNull().maxOrNull() ?: 1f).coerceAtLeast(1f)
        bars.forEachIndexed { i, v ->
            val h = if (v == null) size.height
            else (v / maxV * size.height * 0.8f + size.height * 0.12f).coerceAtMost(size.height)
            val x = i * (bw + gap)
            drawRoundRect(
                color = if (v == null) NeoRed else NeoGreen,
                topLeft = Offset(x, size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
        }
    }
}

private fun formatMb(mb: Long): String =
    if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.1f MB".format(mb.toDouble())

private fun formatBigBytes(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1024) "%.2f TB".format(gb / 1024.0) else "%.1f GB".format(gb)
}
