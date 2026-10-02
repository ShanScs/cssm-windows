package com.cssm.desktop.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/** 界面偏好：与 ThemePrefs 同一 Preferences 节点 */
object UiPrefs {
    private const val KEY_COMPACT_METRICS = "compact_metrics"

    private val prefs: Preferences =
        Preferences.userRoot().node("com/cssm/desktop")

    private val _compactMetrics = MutableStateFlow(loadCompact())
    val compactMetrics: StateFlow<Boolean> = _compactMetrics.asStateFlow()

    private fun loadCompact(): Boolean = try {
        prefs.getBoolean(KEY_COMPACT_METRICS, false)
    } catch (_: Exception) {
        false
    }

    fun setCompactMetrics(v: Boolean) {
        _compactMetrics.value = v
        try {
            prefs.putBoolean(KEY_COMPACT_METRICS, v)
            prefs.flush()
        } catch (_: Exception) {
        }
    }
}
