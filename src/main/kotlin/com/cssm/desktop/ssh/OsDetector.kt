package com.cssm.desktop.ssh

import com.cssm.desktop.data.Server

/** 识别到的操作系统信息 */
data class OsInfo(
    val id: String,
    val name: String
)

/**
 * 通过 /etc/os-release 识别远端操作系统。
 * 失败时返回 unknown，调用方可安全地存入数据库。
 */
object OsDetector {

    fun detect(conn: SshConnection): OsInfo {
        return try {
            parse(conn.exec("cat /etc/os-release", timeoutSec = 8))
        } catch (_: Exception) {
            OsInfo(Server.OS_UNKNOWN, "")
        }
    }

    fun parse(out: String): OsInfo {
        var id = Server.OS_UNKNOWN
        var name = ""
        for (line in out.lineSequence()) {
            val t = line.trim()
            when {
                t.startsWith("ID=") ->
                    id = t.removePrefix("ID=").trim().trim('"')
                        .lowercase().ifBlank { Server.OS_UNKNOWN }
                t.startsWith("PRETTY_NAME=") ->
                    name = t.removePrefix("PRETTY_NAME=").trim().trim('"')
            }
        }
        return OsInfo(id, name)
    }
}
