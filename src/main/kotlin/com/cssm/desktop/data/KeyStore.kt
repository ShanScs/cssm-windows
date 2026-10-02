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

/** 命名 SSH 私钥（PEM），供服务器编辑页选用。 */
@Serializable
data class SshKey(
    val id: Long = 0L,
    val name: String = "",
    val pem: String = "",
    val passphrase: String = "",
    val updatedAt: Long = 0L
)

/** 私钥 JSON 文件存储：~/.cssm/keys.json。 */
class KeyStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val file: File = File(System.getProperty("user.home"), ".cssm/keys.json")

    private val _keys = MutableStateFlow<List<SshKey>>(emptyList())
    val keys: StateFlow<List<SshKey>> = _keys.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init { scope.launch { load() } }

    private suspend fun load() = withContext(Dispatchers.IO) {
        JsonFileStore.migrateTmp(file, "keys.json.tmp")
        val list = try {
            if (file.exists()) json.decodeFromString(ListSerializer(SshKey.serializer()), file.readText())
            else emptyList()
        } catch (_: Exception) { emptyList() }
        _keys.value = list.sortedBy { it.id }
    }

    private suspend fun persist(list: List<SshKey>) {
        try {
            JsonFileStore.writeAtomic(
                file, "keys.json.tmp",
                json.encodeToString(ListSerializer(SshKey.serializer()), list)
            )
        } catch (_: Exception) { }
        _keys.value = list.sortedBy { it.id }
    }

    suspend fun save(key: SshKey): Long = mutex.withLock {
        val cur = _keys.value.toMutableList()
        val now = System.currentTimeMillis()
        return if (key.id == 0L) {
            val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
            persist(cur + key.copy(id = newId, updatedAt = now))
            newId
        } else {
            val idx = cur.indexOfFirst { it.id == key.id }
            if (idx >= 0) { cur[idx] = key.copy(updatedAt = now); persist(cur) }
            key.id
        }
    }

    suspend fun delete(id: Long) = mutex.withLock {
        persist(_keys.value.filterNot { it.id == id })
    }
}
