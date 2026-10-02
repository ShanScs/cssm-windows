package com.cssm.desktop.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** SSH 本地端口转发规则：本地 localPort -> 经 serverId -> remoteHost:remotePort。 */
@Serializable
data class ForwardRule(
    val id: Long = 0L,
    val name: String = "",
    val serverId: Long = 0L,
    val localPort: Int = 8080,
    val remoteHost: String = "127.0.0.1",
    val remotePort: Int = 80,
    val updatedAt: Long = 0L
)

/** 转发规则 JSON 文件存储：~/.cssm/forwards.json。 */
class ForwardStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val file: File = File(System.getProperty("user.home"), ".cssm/forwards.json")

    private val _rules = MutableStateFlow<List<ForwardRule>>(emptyList())
    val rules: StateFlow<List<ForwardRule>> = _rules.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init { scope.launch { load() } }

    private suspend fun load() = withContext(Dispatchers.IO) {
        JsonFileStore.migrateTmp(file, "forwards.json.tmp")
        val list = try {
            if (file.exists()) json.decodeFromString(ListSerializer(ForwardRule.serializer()), file.readText())
            else emptyList()
        } catch (_: Exception) { emptyList() }
        _rules.value = list.sortedBy { it.id }
    }

    private suspend fun persist(list: List<ForwardRule>) {
        try {
            JsonFileStore.writeAtomic(
                file, "forwards.json.tmp",
                json.encodeToString(ListSerializer(ForwardRule.serializer()), list)
            )
        } catch (_: Exception) { }
        _rules.value = list.sortedBy { it.id }
    }

    suspend fun save(rule: ForwardRule): Long = mutex.withLock {
        val cur = _rules.value.toMutableList()
        val now = System.currentTimeMillis()
        return if (rule.id == 0L) {
            val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
            persist(cur + rule.copy(id = newId, updatedAt = now))
            newId
        } else {
            val idx = cur.indexOfFirst { it.id == rule.id }
            if (idx >= 0) { cur[idx] = rule.copy(updatedAt = now); persist(cur) }
            rule.id
        }
    }

    suspend fun delete(id: Long) = mutex.withLock {
        persist(_rules.value.filterNot { it.id == id })
    }
}
