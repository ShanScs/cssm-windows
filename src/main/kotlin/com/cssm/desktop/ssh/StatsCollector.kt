package com.cssm.desktop.ssh

/**
 * 服务器监控数据快照。
 *
 * v0.2 新增：网络累计流量（rxTotalBytes/txTotalBytes）、磁盘 I/O 速率与累计。
 * 新增字段均带默认值，老调用方不受影响。
 */
data class MonitorStats(
    val cpuPercent: Double,
    val cpuCores: Int = 0,
    val memPercent: Double,
    val memUsedMb: Long,
    val memTotalMb: Long,
    val diskPercent: Double,
    val diskUsedGb: Double,
    val diskTotalGb: Double,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val rxTotalBytes: Long = 0,
    val txTotalBytes: Long = 0,
    val ioReadBytesPerSec: Long = 0,
    val ioWriteBytesPerSec: Long = 0,
    val ioReadTotalBytes: Long = 0,
    val ioWriteTotalBytes: Long = 0,
    val load1: Double,
    val load5: Double,
    val load15: Double,
    val uptimeText: String,
    val sampledAt: Long = System.currentTimeMillis()
)

/**
 * 每 N 秒通过 exec 通道采集一次 /proc 数据并计算差值型指标。
 * 线程安全说明：同一实例只在单条采集协程里使用。
 */
class StatsCollector {

    private var prevCpuIdle: Long = 0
    private var prevCpuTotal: Long = 0
    private var prevRx: Long = 0
    private var prevTx: Long = 0
    private var prevIoR: Long = 0
    private var prevIoW: Long = 0
    private var prevAt: Long = 0
    private var hasPrev: Boolean = false

    fun collect(conn: SshConnection): MonitorStats? {
        val raw = try {
            conn.exec(STATS_COMMAND)
        } catch (_: Exception) {
            return null
        }
        return parse(raw)
    }

    private fun parse(raw: String): MonitorStats? {
        return try {
            val sections = mutableMapOf<String, StringBuilder>()
            var current: StringBuilder? = null
            for (line in raw.lineSequence()) {
                val t = line.trim()
                if (t.startsWith("==") && t.endsWith("==")) {
                    current = StringBuilder()
                    sections[t.removePrefix("==").removeSuffix("==")] = current
                } else {
                    current?.appendLine(line)
                }
            }
            val now = System.currentTimeMillis()

            // ---- CPU ----
            val cpuFields = sections["CPU"]?.toString()?.trim()
                ?.split(Regex("\\s+"))?.drop(1)?.mapNotNull { it.toLongOrNull() }
                ?: return null
            val idleAll = (cpuFields.getOrNull(3) ?: 0L) + (cpuFields.getOrNull(4) ?: 0L)
            val total = cpuFields.sum()
            val cpuPercent = if (hasPrev && total > prevCpuTotal) {
                val dIdle = idleAll - prevCpuIdle
                val dTotal = total - prevCpuTotal
                ((1.0 - dIdle.toDouble() / dTotal) * 100).coerceIn(0.0, 100.0)
            } else 0.0

            // ---- MEM ----
            var memTotalKb = 0L
            var memAvailKb = 0L
            sections["MEM"]?.toString()?.lineSequence()?.forEach { l ->
                val parts = l.trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    when (parts[0].trimEnd(':')) {
                        "MemTotal" -> memTotalKb = parts[1].toLongOrNull() ?: 0L
                        "MemAvailable" -> memAvailKb = parts[1].toLongOrNull() ?: 0L
                    }
                }
            }
            val memTotalMb = memTotalKb / 1024
            val memUsedMb = (memTotalKb - memAvailKb) / 1024
            val memPercent = if (memTotalKb > 0)
                ((memTotalKb - memAvailKb).toDouble() / memTotalKb * 100) else 0.0

            // ---- DISK (df -k /) ----
            var diskPercent = 0.0
            var diskUsedGb = 0.0
            var diskTotalGb = 0.0
            sections["DISK"]?.toString()?.trim()?.split(Regex("\\s+"))?.let { p ->
                if (p.size >= 6) {
                    val totalKb = p[1].toLongOrNull() ?: 0L
                    val usedKb = p[2].toLongOrNull() ?: 0L
                    diskTotalGb = totalKb / 1024.0 / 1024.0
                    diskUsedGb = usedKb / 1024.0 / 1024.0
                    diskPercent = if (totalKb > 0) usedKb.toDouble() / totalKb * 100 else 0.0
                }
            }

            // ---- NET ----
            var rx = 0L
            var tx = 0L
            sections["NET"]?.toString()?.lineSequence()?.forEach { l ->
                val t = l.trim()
                if (':' !in t || t.startsWith("Inter-") || t.startsWith(" face")) return@forEach
                val name = t.substringBefore(':').trim()
                if (name == "lo") return@forEach
                val f = t.substringAfter(':').trim().split(Regex("\\s+"))
                rx += f.getOrNull(0)?.toLongOrNull() ?: 0L
                tx += f.getOrNull(8)?.toLongOrNull() ?: 0L
            }
            var rxRate = 0L
            var txRate = 0L
            var ioReadRate = 0L
            var ioWriteRate = 0L

            // ---- IO (/proc/diskstats) ----
            var ioR = 0L
            var ioW = 0L
            sections["IO"]?.toString()?.lineSequence()?.forEach { l ->
                // major minor name reads_completed reads_merged sectors_read ... writes_merged sectors_written
                val p = l.trim().split(Regex("\\s+"))
                if (p.size >= 14) {
                    val name = p[2]
                    if (name.startsWith("loop") || name.startsWith("ram")) return@forEach
                    ioR += (p[5].toLongOrNull() ?: 0L) * 512
                    ioW += (p[9].toLongOrNull() ?: 0L) * 512
                }
            }

            if (hasPrev && prevAt > 0) {
                val dt = (now - prevAt) / 1000.0
                if (dt > 0) {
                    rxRate = ((rx - prevRx) / dt).toLong().coerceAtLeast(0)
                    txRate = ((tx - prevTx) / dt).toLong().coerceAtLeast(0)
                    ioReadRate = ((ioR - prevIoR) / dt).toLong().coerceAtLeast(0)
                    ioWriteRate = ((ioW - prevIoW) / dt).toLong().coerceAtLeast(0)
                }
            }

            // ---- LOAD ----
            val loadParts = sections["LOAD"]?.toString()?.trim()?.split(Regex("\\s+"))
            val load1 = loadParts?.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val load5 = loadParts?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val load15 = loadParts?.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            // ---- UPTIME ----
            val upSec = sections["UPTIME"]?.toString()?.trim()
                ?.split(Regex("\\s+"))?.getOrNull(0)?.toDoubleOrNull()?.toLong() ?: 0L
            val uptimeText = formatUptime(upSec)

            prevCpuIdle = idleAll
            prevCpuTotal = total
            prevRx = rx
            prevTx = tx
            prevIoR = ioR
            prevIoW = ioW
            prevAt = now
            hasPrev = true

            val cpuCores = sections["CORE"]?.toString()?.trim()?.toIntOrNull() ?: 0

            MonitorStats(
                cpuPercent = cpuPercent,
                cpuCores = cpuCores,
                memPercent = memPercent.coerceIn(0.0, 100.0),
                memUsedMb = memUsedMb.coerceAtLeast(0),
                memTotalMb = memTotalMb.coerceAtLeast(0),
                diskPercent = diskPercent.coerceIn(0.0, 100.0),
                diskUsedGb = diskUsedGb,
                diskTotalGb = diskTotalGb,
                rxBytesPerSec = rxRate,
                txBytesPerSec = txRate,
                rxTotalBytes = rx,
                txTotalBytes = tx,
                ioReadBytesPerSec = ioReadRate,
                ioWriteBytesPerSec = ioWriteRate,
                ioReadTotalBytes = ioR,
                ioWriteTotalBytes = ioW,
                load1 = load1, load5 = load5, load15 = load15,
                uptimeText = uptimeText,
                sampledAt = now
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun formatUptime(totalSec: Long): String {
        val d = totalSec / 86400
        val h = (totalSec % 86400) / 3600
        val m = (totalSec % 3600) / 60
        return buildString {
            if (d > 0) append("${d}天 ")
            if (h > 0 || d > 0) append("${h}小时 ")
            append("${m}分")
        }.trim()
    }

    companion object {
        private const val STATS_COMMAND =
            "printf '==CPU==\\n'; head -n 1 /proc/stat; " +
            "printf '==MEM==\\n'; grep -E '^(MemTotal|MemAvailable):' /proc/meminfo; " +
            "printf '==DISK==\\n'; df -k / | tail -n 1; " +
            "printf '==NET==\\n'; cat /proc/net/dev; " +
            "printf '==IO==\\n'; cat /proc/diskstats; " +
            "printf '==CORE==\\n'; nproc 2>/dev/null || echo 0; " +
            "printf '==LOAD==\\n'; cat /proc/loadavg; " +
            "printf '==UPTIME==\\n'; cat /proc/uptime"
    }
}
