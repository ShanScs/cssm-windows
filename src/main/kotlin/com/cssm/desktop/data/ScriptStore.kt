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

/** 可在服务器上执行的 Shell 脚本。 */
@Serializable
data class Script(
    val id: Long = 0L,
    val name: String = "",
    val content: String = "",
    val sortOrder: Int = 0,
    val updatedAt: Long = 0L
)

/** 脚本 JSON 文件存储：~/.cssm/scripts.json。 */
class ScriptStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val file: File = File(System.getProperty("user.home"), ".cssm/scripts.json")

    private val _scripts = MutableStateFlow<List<Script>>(emptyList())
    val scripts: StateFlow<List<Script>> = _scripts.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init { scope.launch { load() } }

    private suspend fun load() = withContext(Dispatchers.IO) {
        JsonFileStore.migrateTmp(file, "scripts.json.tmp")
        val list = try {
            if (file.exists()) json.decodeFromString(ListSerializer(Script.serializer()), file.readText())
            else emptyList()
        } catch (_: Exception) { emptyList() }
        _scripts.value = list.sortedBy { it.sortOrder }
    }

    private suspend fun persist(list: List<Script>) {
        try {
            JsonFileStore.writeAtomic(
                file, "scripts.json.tmp",
                json.encodeToString(ListSerializer(Script.serializer()), list)
            )
        } catch (_: Exception) { }
        _scripts.value = list.sortedBy { it.sortOrder }
    }

    suspend fun save(script: Script): Long = mutex.withLock {
        val cur = _scripts.value.toMutableList()
        val now = System.currentTimeMillis()
        return if (script.id == 0L) {
            val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
            val maxOrder = cur.maxOfOrNull { it.sortOrder } ?: -1
            persist(cur + script.copy(id = newId, sortOrder = maxOrder + 1, updatedAt = now))
            newId
        } else {
            val idx = cur.indexOfFirst { it.id == script.id }
            if (idx >= 0) { cur[idx] = script.copy(updatedAt = now); persist(cur) }
            script.id
        }
    }

    suspend fun delete(id: Long) = mutex.withLock {
        persist(_scripts.value.filterNot { it.id == id })
    }
}
