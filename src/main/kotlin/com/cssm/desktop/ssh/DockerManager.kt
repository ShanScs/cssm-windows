package com.cssm.desktop.ssh

import com.cssm.desktop.data.Server

/**
 * Docker 容器信息。
 */
data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val status: String,
    val state: String,  // running, exited, paused
    val cpuPercent: Double = 0.0,
    val memUsage: String = "",
    val memPercent: Double = 0.0
)

/**
 * Docker 管理：通过 SSH exec 调用 docker 命令。
 */
class DockerManager {

    /**
     * 获取容器列表（含资源占用）。
     * 需要目标机器安装 docker 且用户有权限。
     */
    fun listContainers(conn: SshConnection): List<DockerContainer> {
        // 先检查 docker 是否可用
        val version = try {
            conn.exec("docker --version", 5)
        } catch (e: Exception) {
            throw IllegalStateException("Docker 未安装或无权限: ${e.message}")
        }
        if (!version.contains("Docker version")) {
            throw IllegalStateException("Docker 未安装")
        }

        // 容器列表：ID,名称,镜像,状态
        val psFormat = "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}|{{.State}}"
        val psOutput = conn.exec("docker ps -a --format \"$psFormat\"", 10)
        
        // 资源占用（只取运行中的）
        val statsFormat = "{{.ID}}|{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}"
        val statsMap = try {
            conn.exec("docker stats --no-stream --format \"$statsFormat\"", 10)
                .lineSequence()
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) parts[0] to Triple(parts[1], parts[2], parts[3])
                    else null
                }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }

        return psOutput.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 5) return@mapNotNull null
                val id = parts[0]
                val stats = statsMap[id]
                DockerContainer(
                    id = id,
                    name = parts[1],
                    image = parts[2],
                    status = parts[3],
                    state = parts[4],
                    cpuPercent = stats?.first?.trimEnd('%')?.toDoubleOrNull() ?: 0.0,
                    memUsage = stats?.second ?: "",
                    memPercent = stats?.third?.trimEnd('%')?.toDoubleOrNull() ?: 0.0
                )
            }.toList()
    }

    fun start(conn: SshConnection, containerId: String): String {
        return conn.exec("docker start $containerId", 15)
    }

    fun stop(conn: SshConnection, containerId: String): String {
        return conn.exec("docker stop $containerId", 15)
    }

    fun restart(conn: SshConnection, containerId: String): String {
        return conn.exec("docker restart $containerId", 15)
    }
}
