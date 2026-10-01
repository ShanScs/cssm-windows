package com.cssm.desktop.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.cssm.desktop.ui.components.FlagImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cssm.desktop.data.REGIONS
import com.cssm.desktop.data.Region
import com.cssm.desktop.data.Server
import com.cssm.desktop.data.ServerStore
import com.cssm.desktop.data.findRegion
import com.cssm.desktop.net.GeoIp
import kotlinx.coroutines.launch

/**
 * 新增 / 编辑服务器（桌面版 M1）。
 * 保存时若未选地区，尝试自动识别（ip-api），失败则静默跳过。
 */
@Composable
fun EditScreen(
    id: Long,
    store: ServerStore,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(id == 0L) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("root") }
    var authType by remember { mutableStateOf(Server.AUTH_PASSWORD) }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var keyPassphrase by remember { mutableStateOf("") }
    var region by remember { mutableStateOf<Region?>(null) }
    var regionMenu by remember { mutableStateOf(false) }
    var detecting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id != 0L) {
            store.getById(id)?.let { s ->
                name = s.name; host = s.host; port = s.port.toString()
                username = s.username; authType = s.authType
                password = s.password; privateKey = s.privateKey
                keyPassphrase = s.keyPassphrase
                region = findRegion(s.region)
            }
            loaded = true
        }
    }

    if (!loaded) {
        Column(
            Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { CircularProgressIndicator() }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (id == 0L) "添加服务器" else "编辑服务器",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onCancel) { Text("取消") }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp, 8.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Field("名称", name, { name = it }, placeholder = "我的服务器")
            Field("主机", host, { host = it }, placeholder = "192.168.1.10 或 example.com")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("端口", port, { port = it.filter { c -> c.isDigit() }.take(5) },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number)
                Field("用户名", username, { username = it },
                    modifier = Modifier.weight(2f), placeholder = "root")
            }

            // 认证方式
            Text("认证方式", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { authType = Server.AUTH_PASSWORD },
                    enabled = authType != Server.AUTH_PASSWORD
                ) { Text("密码") }
                OutlinedButton(
                    onClick = { authType = Server.AUTH_KEY },
                    enabled = authType != Server.AUTH_KEY
                ) { Text("密钥") }
            }
            if (authType == Server.AUTH_PASSWORD) {
                Field("密码", password, { password = it }, secret = true)
            } else {
                Field("私钥（PEM）", privateKey, { privateKey = it },
                    singleLine = false, minLines = 4, placeholder = "-----BEGIN ...")
                Field("私钥口令（可选）", keyPassphrase, { keyPassphrase = it }, secret = true)
            }

            // 地区
            Text("地区", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { regionMenu = true }) {
                    val sel = region
                    if (sel == null) {
                        Text("未设置")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FlagImage(flag = sel.flag, height = 12.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(sel.name)
                        }
                    }
                }
                DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                    DropdownMenuItem(text = { Text("未设置") }, onClick = {
                        region = null; regionMenu = false
                    })
                    REGIONS.forEach { r ->
                        DropdownMenuItem(text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FlagImage(flag = r.flag, height = 12.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(r.name)
                            }
                        }, onClick = {
                            region = r; regionMenu = false
                        })
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (host.isBlank() || detecting) return@TextButton
                        detecting = true
                        scope.launch {
                            GeoIp.detectRegion(host)?.let { region = it }
                            detecting = false
                        }
                    }
                ) { Text(if (detecting) "识别中…" else "自动识别") }
            }

            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val h = host.trim()
                    val p = port.toIntOrNull() ?: 0
                    error = when {
                        h.isEmpty() -> "请填写主机"
                        p !in 1..65535 -> "端口不合法"
                        username.isBlank() -> "请填写用户名"
                        authType == Server.AUTH_PASSWORD && password.isEmpty() -> "请填写密码"
                        authType == Server.AUTH_KEY && privateKey.isBlank() -> "请填写私钥"
                        else -> ""
                    }
                    if (error.isNotEmpty() || saving) return@Button
                    saving = true
                    scope.launch {
                        try {
                            var r = region
                            if (r == null) {
                                r = GeoIp.detectRegion(h)
                            }
                            val s = Server(
                                id = id,
                                name = name.trim().ifBlank { h },
                                host = h, port = p, username = username.trim(),
                                authType = authType, password = password,
                                privateKey = privateKey, keyPassphrase = keyPassphrase,
                                region = r?.name ?: "", regionFlag = r?.flag ?: ""
                            )
                            // 保留编辑前的系统/排序/会话信息
                            val old = if (id != 0L) store.getById(id) else null
                            val merged = if (old != null) s.copy(
                                createdAt = old.createdAt,
                                lastConnectedAt = old.lastConnectedAt,
                                osId = old.osId, osName = old.osName,
                                sortOrder = old.sortOrder
                            ) else s
                            store.save(merged)
                            onDone()
                        } catch (e: Exception) {
                            error = e.message ?: "保存失败"
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) { Text(if (saving) "保存中…" else "保存") }

            if (id != 0L) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            store.getById(id)?.let { store.delete(it) }
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除服务器") }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    secret: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}
