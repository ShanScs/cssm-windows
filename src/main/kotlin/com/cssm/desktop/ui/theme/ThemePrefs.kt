package com.cssm.desktop.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/** 外观偏好：存在系统 Preferences（~/.java/.userPrefs），与安卓端 DataStore 语义对齐 */
object ThemePrefs {
    private const val KEY_THEME = "theme_mode"

    private val prefs: Preferences =
        Preferences.userRoot().node("com/cssm/desktop")

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private fun load(): ThemeMode = try {
        ThemeMode.valueOf(prefs.get(KEY_THEME, ThemeMode.FOLLOW_SYSTEM.name))
    } catch (_: Exception) {
        ThemeMode.FOLLOW_SYSTEM
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        try {
            prefs.put(KEY_THEME, mode.name)
            prefs.flush()
        } catch (_: Exception) {
        }
    }
}
