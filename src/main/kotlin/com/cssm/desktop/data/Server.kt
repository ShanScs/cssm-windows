package com.cssm.desktop.data

import kotlinx.serialization.Serializable

/**
 * 服务器连接信息（桌面版）。
 *
 * 与安卓端字段对齐；updatedAt 预留给未来的账号同步（多端合并用）。
 * 注意：密码/私钥目前明文存在本地 JSON，正式版应迁移到系统钥匙串。
 */
@Serializable
data class Server(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String = AUTH_PASSWORD,
    val password: String = "",
    val privateKey: String = "",
    val keyPassphrase: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val region: String = "",
    val regionFlag: String = "",
    val lastConnectedAt: Long = 0L,
    val osId: String = OS_UNKNOWN,
    val osName: String = "",
    /** 列表排序位：越小越靠前；用户在终端页排序后写入 */
    val sortOrder: Long = 0,
    /** 数据更新时间：预留给账号同步做合并 */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val AUTH_PASSWORD = "password"
        const val AUTH_KEY = "key"
        const val OS_UNKNOWN = "unknown"
    }

    val displayTarget: String
        get() = "$username@$host:$port"

    /** 带地区国旗的展示名，如「🇺🇸 美国 · 洛杉矶」；未设置地区时退化为名称；地区与名称互相包含时去重 */
    val displayName: String
        get() = if (region.isNotBlank()) {
            val flag = regionFlag.ifBlank { "" }
            val r = region.trim()
            val n = name.trim()
            val core = when {
                n.isEmpty() || n == r || r.startsWith(n) -> r
                n.startsWith(r) -> n
                else -> "$r · $n"
            }
            "$flag $core".trim()
        } else {
            name
        }

    val authTypeLabel: String
        get() = if (authType == AUTH_KEY) "密钥" else "密码"
}
