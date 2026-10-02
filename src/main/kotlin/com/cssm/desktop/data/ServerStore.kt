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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 服务器 JSON 文件存储：~/.cssm/servers.json。
 *
 * 语义对齐安卓端 ServerRepository：
 * - 列表按 sortOrder 升序
 * - save：id==0 则新增（自增 id），否则整行更新
 * - persistOrder：按给定 id 顺序重写 sortOrder
 * - clearRecentSession：只清 lastConnectedAt（删会话记录，不删服务器）
 */
class ServerStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val file: File = File(System.getProperty("user.home"), ".cssm/servers.json")

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        scope.launch { load() }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        // 旧版本 Windows renameTo bug：用残留 tmp 恢复丢失的数据（一次性）
        JsonFileStore.migrateTmp(file, "servers.json.tmp")
        val list = try {
            if (file.exists()) json.decodeFromString(
                ListSerializer(Server.serializer()), file.readText()
            ) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        _servers.value = list.sortedBy { it.sortOrder }
    }

    private suspend fun persist(list: List<Server>) {
        try {
            JsonFileStore.writeAtomic(
                file, "servers.json.tmp",
                json.encodeToString(ListSerializer(Server.serializer()), list)
            )
        } catch (_: Exception) {
        }
        _servers.value = list.sortedBy { it.sortOrder }
    }

    suspend fun getById(id: Long): Server? = _servers.value.firstOrNull { it.id == id }

    /** id==0 新增（分配自增 id 与末尾 sortOrder），否则整行更新；返回 id */
    suspend fun save(server: Server): Long = mutex.withLock {
        val cur = _servers.value.toMutableList()
        val now = System.currentTimeMillis()
        return if (server.id == 0L) {
            val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
            val maxOrder = cur.maxOfOrNull { it.sortOrder } ?: -1
            val fresh = server.copy(id = newId, sortOrder = maxOrder + 1, updatedAt = now)
            persist(cur + fresh)
            newId
        } else {
            val idx = cur.indexOfFirst { it.id == server.id }
            if (idx >= 0) {
                cur[idx] = server.copy(updatedAt = now)
                persist(cur)
            }
            server.id
        }
    }

    suspend fun delete(server: Server) = mutex.withLock {
        persist(_servers.value.filterNot { it.id == server.id })
    }

    suspend fun updateConnectionInfo(id: Long, osId: String, osName: String, region: String, regionFlag: String) =
        mutex.withLock {
            val cur = _servers.value.toMutableList()
            val idx = cur.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val s = cur[idx]
                cur[idx] = s.copy(
                    osId = osId, osName = osName,
                    region = region, regionFlag = regionFlag,
                    lastConnectedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                persist(cur)
            }
        }

    suspend fun persistOrder(ids: List<Long>) = mutex.withLock {
        val cur = _servers.value.toMutableList()
        val orderMap = ids.withIndex().associate { (i, id) -> id to i.toLong() }
        for (i in cur.indices) {
            val o = orderMap[cur[i].id]
            if (o != null) cur[i] = cur[i].copy(sortOrder = o, updatedAt = System.currentTimeMillis())
        }
        persist(cur)
    }

    suspend fun clearRecentSession(id: Long) = mutex.withLock {
        val cur = _servers.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == id }
        if (idx >= 0) {
            cur[idx] = cur[idx].copy(lastConnectedAt = 0L, updatedAt = System.currentTimeMillis())
            persist(cur)
        }
    }
}
