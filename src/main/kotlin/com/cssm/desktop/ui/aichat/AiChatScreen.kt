package com.cssm.desktop.ui.aichat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.AiChatConfig
import com.cssm.desktop.data.AiChatConfigStore
import com.cssm.desktop.ui.components.IpadTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private data class ChatMsg(val role: String, val content: String)

/**
 * AI Chat：OpenAI 兼容接口聊天，Key 在本机保存（~/.cssm/aichat.json）。
 */
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(AiChatConfig()) }
    var configLoaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        config = AiChatConfigStore.load()
        configLoaded = true
        if (config.apiKey.isBlank()) showConfig = true
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        if (config.apiKey.isBlank()) {
            showConfig = true
            error = "请先配置 API Key"
            return
        }
        input = ""
        error = null
        messages.add(ChatMsg("user", text))
        sending = true
        scope.launch {
            try {
                val reply = chatOnce(config, messages.toList())
                messages.add(ChatMsg("assistant", reply))
            } catch (e: Exception) {
                error = "请求失败：${e.message}"
            }
            sending = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
            }
            IpadTitle(text = "AI Chat", modifier = Modifier.padding(top = 14.dp, end = 8.dp, bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showConfig = !showConfig }) {
                Icon(Icons.Filled.Settings, contentDescription = "配置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
        }

        if (showConfig && configLoaded) {
            var endpoint by remember(config) { mutableStateOf(config.endpoint) }
            var apiKey by remember(config) { mutableStateOf(config.apiKey) }
            var model by remember(config) { mutableStateOf(config.model) }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = endpoint, onValueChange = { endpoint = it },
                    label = { Text("接口地址") }, singleLine = true,
                    placeholder = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it },
                    label = { Text("API Key") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model, onValueChange = { model = it },
                    label = { Text("模型") }, singleLine = true,
                    placeholder = { Text("gpt-4o-mini") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val c = AiChatConfig(
                            endpoint.trim().trimEnd('/').ifBlank { "https://api.openai.com/v1" },
                            apiKey.trim(), model.trim().ifBlank { "gpt-4o-mini" }
                        )
                        scope.launch {
                            AiChatConfigStore.save(c)
                            config = c
                            showConfig = false
                        }
                    }) { Text("保存") }
                }
            }
        }

        if (error != null) {
            Text(
                error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item(key = "hint") {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "输入下方消息开始对话\n可询问服务器运维相关问题",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            items(messages, key = { it.hashCode().toString() + it.content.take(8) }) { m ->
                val isUser = m.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        Text(
                            m.content,
                            fontSize = 14.sp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            if (sending) {
                item(key = "typing") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("输入消息…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { send() },
                enabled = !sending && input.isNotBlank()
            ) {
                Icon(
                    Icons.Filled.Send, contentDescription = "发送",
                    tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val httpClient: HttpClient by lazy {
    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
}

private val lenientJson = Json { ignoreUnknownKeys = true }

@Throws(Exception::class)
private suspend fun chatOnce(config: AiChatConfig, history: List<ChatMsg>): String =
    withContext(Dispatchers.IO) {
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        val msgsJson = history.joinToString(",") {
            """{"role":"${it.role}","content":"${esc(it.content)}"}"""
        }
        val body = """{"model":"${esc(config.model)}","messages":[$msgsJson],"stream":false}"""
        val req = HttpRequest.newBuilder()
            .uri(URI.create(config.endpoint.trimEnd('/') + "/chat/completions"))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${config.apiKey}")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw IllegalStateException("HTTP ${resp.statusCode()}：${resp.body().take(300)}")
        }
        val root = lenientJson.parseToJsonElement(resp.body()).jsonObject
        root["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?.trim()
            ?: throw IllegalStateException("解析响应失败")
    }
