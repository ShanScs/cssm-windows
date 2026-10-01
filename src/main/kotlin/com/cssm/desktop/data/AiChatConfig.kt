package com.cssm.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** AI Chat 配置：OpenAI 兼容接口。 */
@Serializable
data class AiChatConfig(
    val endpoint: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini"
)

object AiChatConfigStore {
    private val file: File = File(System.getProperty("user.home"), ".cssm/aichat.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun load(): AiChatConfig = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) json.decodeFromString(AiChatConfig.serializer(), file.readText())
            else AiChatConfig()
        } catch (_: Exception) { AiChatConfig() }
    }

    suspend fun save(config: AiChatConfig) = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(AiChatConfig.serializer(), config))
        } catch (_: Exception) { }
    }
}
