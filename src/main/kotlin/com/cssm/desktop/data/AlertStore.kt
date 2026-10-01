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

/** 告警规则：服务器离线 / CPU 使用率超阈值。 */
@Serializable
data class AlertRule(
    val id: Long = 0L,
    val serverId: Long = 0L,
    /** "offline" | "cpu" */
    val type: String = "offline",
    /** cpu 阈值（百分比），offline 类型忽略 */
    val threshold: Int = 90,
    val enabled: Boolean = true,
    val updatedAt: Long = 0L
)

/** 通知记录（持久化，方便打开通知中心回看）。 */
@Serializable
data class NotifyItem(
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val time: Long = 0L,
    val read: Boolean = false
)

/** 告警规则 + 通知记录存储：~/.cssm/alerts.json / notifications.json。 */
class AlertStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val dir: File = File(System.getProperty("user.home"), ".cssm")
    private val ruleFile: File = File(dir, "alerts.json")
    private val notifyFile: File = File(dir, "notifications.json")

    private val _rules = MutableStateFlow<List<AlertRule>>(emptyList())
    val rules: StateFlow<List<AlertRule>> = _rules.asStateFlow()

    private val _items = MutableStateFlow<List<NotifyItem>>(emptyList())
    val items: StateFlow<List<NotifyItem>> = _items.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        scope.launch {
            loadRules(); loadItems()
        }
    }

    private suspend fun loadRules() = withContext(Dispatchers.IO) {
        val list = try {
            if (ruleFile.exists()) json.decodeFromString(ListSerializer(AlertRule.serializer()), ruleFile.readText())
            else emptyList()
        } catch (_: Exception) { emptyList() }
        _rules.value = list.sortedBy { it.id }
    }

    private suspend fun loadItems() = withContext(Dispatchers.IO) {
        val list = try {
            if (notifyFile.exists()) json.decodeFromString(ListSerializer(NotifyItem.serializer()), notifyFile.readText())
            else emptyList()
        } catch (_: Exception) { emptyList() }
        _items.value = list.sortedByDescending { it.time }.take(200)
    }

    private suspend fun persistRules(list: List<AlertRule>) {
        try {
            dir.mkdirs()
            val tmp = File(dir, "alerts.json.tmp")
            tmp.writeText(json.encodeToString(ListSerializer(AlertRule.serializer()), list))
            tmp.renameTo(ruleFile)
        } catch (_: Exception) { }
        _rules.value = list.sortedBy { it.id }
    }

    private suspend fun persistItems(list: List<NotifyItem>) {
        try {
            dir.mkdirs()
            val tmp = File(dir, "notifications.json.tmp")
            tmp.writeText(json.encodeToString(ListSerializer(NotifyItem.serializer()), list))
            tmp.renameTo(notifyFile)
        } catch (_: Exception) { }
        _items.value = list.sortedByDescending { it.time }.take(200)
    }

    suspend fun saveRule(rule: AlertRule): Long = mutex.withLock {
        val cur = _rules.value.toMutableList()
        val now = System.currentTimeMillis()
        return if (rule.id == 0L) {
            val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
            persistRules(cur + rule.copy(id = newId, updatedAt = now))
            newId
        } else {
            val idx = cur.indexOfFirst { it.id == rule.id }
            if (idx >= 0) { cur[idx] = rule.copy(updatedAt = now); persistRules(cur) }
            rule.id
        }
    }

    suspend fun deleteRule(id: Long) = mutex.withLock {
        persistRules(_rules.value.filterNot { it.id == id })
    }

    suspend fun push(title: String, body: String) = mutex.withLock {
        val cur = _items.value.toMutableList()
        val newId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
        persistItems(listOf(NotifyItem(id = newId, title = title, body = body, time = System.currentTimeMillis())) + cur)
    }

    suspend fun markAllRead() = mutex.withLock {
        persistItems(_items.value.map { it.copy(read = true) })
    }

    suspend fun clearItems() = mutex.withLock { persistItems(emptyList()) }
}
