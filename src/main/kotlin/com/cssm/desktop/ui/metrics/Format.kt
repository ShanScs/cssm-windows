package com.cssm.desktop.ui.metrics

/** 速率格式化：B/s, KB/s, MB/s, GB/s */
fun formatRate(bytesPerSec: Long): String = when {
    bytesPerSec < 0 -> "--"
    bytesPerSec < 1024 -> "$bytesPerSec B/s"
    bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024.0)
    bytesPerSec < 1024 * 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / 1024.0 / 1024.0)
    else -> "%.2f GB/s".format(bytesPerSec / 1024.0 / 1024.0 / 1024.0)
}

/** 容量格式化：MB, GB, TB */
fun formatGb(gb: Double): String = when {
    gb < 1 -> "%.0f MB".format(gb * 1024)
    gb < 1024 -> "%.1f GB".format(gb)
    else -> "%.2f TB".format(gb / 1024)
}

/** 总量格式化（iPad 紧凑风格）：B, KB, MB, GB */
fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "--"
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    else -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
}
