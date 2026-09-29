package com.cssm.desktop.ui.metrics

/** 速率格式化：B/s, K/s, MB/s, GB/s */
fun formatRate(bytesPerSec: Long): String = when {
    bytesPerSec < 0 -> "--"
    bytesPerSec < 1024 -> "$bytesPerSec B/s"
    bytesPerSec < 1024 * 1024 -> "%.1f K/s".format(bytesPerSec / 1024.0)
    bytesPerSec < 1024 * 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / 1024.0 / 1024.0)
    else -> "%.2f GB/s".format(bytesPerSec / 1024.0 / 1024.0 / 1024.0)
}

/** 容量格式化：MB, GB, TB */
fun formatGb(gb: Double): String = when {
    gb < 1 -> "%.0f MB".format(gb * 1024)
    gb < 1024 -> "%.1f GB".format(gb)
    else -> "%.2f TB".format(gb / 1024)
}
