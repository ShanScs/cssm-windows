package com.cssm.desktop.ui.containers

import com.cssm.desktop.data.Server
import com.cssm.desktop.ssh.SshConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 容器卡片用的 Docker 轻量探测（iPad 版卡片会逐台探测显示 Connecting/计数）。
 * 一次 SSH exec 拿齐：容器状态列表、镜像数、网络数、数据卷数。
 */
sealed interface DockerCardStatus {
    data object Connecting : DockerCardStatus
    data class Ready(
        val running: Int,
        val stopped: Int,
        val images: Int,
        val networks: Int,
        val volumes: Int
    ) : DockerCardStatus {
        val total: Int get() = running + stopped
    }
    data class Failed(val msg: String) : DockerCardStatus
}

private const val PROBE_CMD =
    // ps 的 stderr 保留（2>&1）：daemon 未运行 / 无权限时的报错要能被识别出来
    "printf 'S\\n'; docker ps -a --format '{{.State}}' 2>&1; " +
        "printf 'I\\n'; docker images -q 2>/dev/null; " +
        "printf 'N\\n'; docker network ls -q 2>/dev/null; " +
        "printf 'V\\n'; docker volume ls -q 2>/dev/null; printf 'E\\n'"

suspend fun probeDocker(server: Server): DockerCardStatus {
    val conn = SshConnection()
    return try {
        withTimeout(30_000) {
            withContext(Dispatchers.IO) {
                conn.connect(server, 80, 24)
                // 先确认装没装 docker，避免把“没装”误报成其他错误
                val which = conn.exec("command -v docker 2>/dev/null || echo NO_DOCKER", 10).trim()
                if (which.isEmpty() || which == "NO_DOCKER") {
                    return@withContext DockerCardStatus.Failed("未安装 Docker")
                }
                val out = conn.exec(PROBE_CMD, 20)
                val errLine = out.lineSequence().map { it.trim() }.firstOrNull {
                    it.contains("Cannot connect to the Docker daemon", ignoreCase = true) ||
                        it.contains("Is the docker daemon running", ignoreCase = true) ||
                        it.contains("permission denied", ignoreCase = true)
                }
                if (errLine != null) {
                    val reason = if (errLine.contains("permission denied", ignoreCase = true))
                        "无权限访问 Docker（需加入 docker 组或使用 sudo）"
                    else "Docker daemon 未运行"
                    return@withContext DockerCardStatus.Failed(reason)
                }
                parseProbe(out)
            }
        }
    } catch (e: Exception) {
        DockerCardStatus.Failed(e.message ?: "连接失败")
    } finally {
        try {
            conn.disconnect()
        } catch (_: Exception) {
        }
    }
}

private fun parseProbe(output: String): DockerCardStatus {
    val sections = mutableMapOf<Char, MutableList<String>>()
    var cur: Char? = null
    for (line in output.lineSequence()) {
        val t = line.trim()
        if (t.length == 1 && t[0] in "SINV") {
            if (t == "E") break
            cur = t[0]
            sections.getOrPut(cur) { mutableListOf() }
        } else if (cur != null && t.isNotEmpty()) {
            sections[cur]!!.add(t)
        }
    }
    val networks = sections['N']?.size ?: 0
    // 正常 daemon 至少有 bridge/host/none 三个网络；一个都没有说明 docker 不可用
    if (networks == 0) return DockerCardStatus.Failed("Docker 不可用")
    val states = sections['S'].orEmpty()
    val running = states.count { it.contains("running", ignoreCase = true) }
    return DockerCardStatus.Ready(
        running = running,
        stopped = states.size - running,
        images = sections['I']?.size ?: 0,
        networks = networks,
        volumes = sections['V']?.size ?: 0
    )
}
