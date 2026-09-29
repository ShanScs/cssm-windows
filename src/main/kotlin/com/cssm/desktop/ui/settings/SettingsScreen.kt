package com.cssm.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cssm.desktop.ui.theme.ThemeMode
import com.cssm.desktop.ui.theme.ThemePrefs

/** 设置页（桌面版 M1）：外观主题 + 关于 */
@Composable
fun SettingsScreen() {
    val mode by ThemePrefs.mode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 4.dp)
        )
        Column(
            modifier = Modifier.padding(20.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption("跟随系统", ThemeMode.FOLLOW_SYSTEM, mode)
                ThemeOption("深色", ThemeMode.DARK, mode)
                ThemeOption("浅色", ThemeMode.LIGHT, mode)
            }
            Spacer(Modifier.height(8.dp))
            Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Cssm 桌面版 0.1.0\n服务器管理 · SSH 终端",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeOption(label: String, value: ThemeMode, current: ThemeMode) {
    OutlinedButton(
        onClick = { ThemePrefs.setMode(value) },
        enabled = value != current
    ) { Text(label) }
}
