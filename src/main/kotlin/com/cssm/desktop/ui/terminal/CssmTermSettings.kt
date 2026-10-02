package com.cssm.desktop.ui.terminal

import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Font
import java.util.prefs.Preferences

/**
 * JediTerm 外观设置：等宽字体 + 可调字号（8~24，默认 13），与手机端终端字号范围对齐。
 * 字号持久化在系统 Preferences。
 * 深色模式下终端用深色底（#0B0E14）浅色字，跟随 App 主题。
 */
class CssmTermSettings(fontSize: Float, private val dark: Boolean = false) : DefaultSettingsProvider() {

    private val size = fontSize.coerceIn(8f, 24f)

    override fun getTerminalFont(): Font =
        Font(Font.MONOSPACED, Font.PLAIN, size.toInt())

    override fun getTerminalFontSize(): Float = size

    override fun getDefaultBackground(): TerminalColor =
        if (dark) TerminalColor(0x0B, 0x0E, 0x14) else TerminalColor.WHITE

    override fun getDefaultForeground(): TerminalColor =
        if (dark) TerminalColor(0xE6, 0xED, 0xF3) else TerminalColor.BLACK

    /**
     * 默认文本样式：普通文本（无 ANSI 颜色码）的单元格用这个渲染。
     * 只重写 getDefaultBackground 的话，文字格子还是默认样式的白底，
     * 深色下会出现"白字块"（Tony 截图里的白底黑字）。
     */
    override fun getDefaultStyle(): TextStyle =
        TextStyle(getDefaultForeground(), getDefaultBackground())

    companion object {
        private const val KEY_FONT_SIZE = "term_font_size"
        private val prefs: Preferences =
            Preferences.userRoot().node("com/cssm/desktop")

        fun loadFontSize(): Float =
            try {
                prefs.getFloat(KEY_FONT_SIZE, 13f).coerceIn(8f, 24f)
            } catch (_: Exception) {
                13f
            }

        fun saveFontSize(size: Float) {
            try {
                prefs.putFloat(KEY_FONT_SIZE, size.coerceIn(8f, 24f))
                prefs.flush()
            } catch (_: Exception) {
            }
        }
    }
}
