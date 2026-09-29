package com.cssm.desktop.ssh

import com.cssm.desktop.data.Server
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.password.PasswordUtils
import java.io.Closeable

/**
 * SFTP 文件信息。
 */
data class SftpFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val permissions: String
)

/**
 * SFTP 客户端封装：基于 sshj，一台服务器一个实例。
 */
class SftpConnection : Closeable {
    private var client: SSHClient? = null
    private var sftp: SFTPClient? = null

    fun connect(server: Server) {
        disconnect()
        val c = SSHClient()
        c.addHostKeyVerifier(PromiscuousVerifier())
        c.connect(server.host, server.port)
        if (server.password.isNotBlank()) {
            c.authPassword(server.username, PasswordUtils.createOneOff(server.password.toCharArray()))
        } else if (server.privateKey.isNotBlank()) {
            // TODO: 私钥认证（M2）
            throw UnsupportedOperationException("私钥认证暂未实现")
        } else {
            throw IllegalArgumentException("未配置认证方式")
        }
        sftp = c.newSFTPClient()
        client = c
    }

    fun isConnected(): Boolean = try {
        client?.isConnected == true && sftp != null
    } catch (_: Exception) {
        false
    }

    /**
     * 列出目录内容。
     */
    fun listDir(path: String): List<SftpFile> {
        val s = sftp ?: throw IllegalStateException("未连接")
        return s.ls(path).map { info ->
            val attrs = info.attributes
            SftpFile(
                name = info.name,
                path = info.path,
                isDirectory = attrs.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY,
                size = attrs.size,
                modifiedAt = attrs.mtime * 1000,
                permissions = attrs.permissions.toString()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override fun close() {
        disconnect()
    }

    fun disconnect() {
        try { sftp?.close() } catch (_: Exception) {}
        try { client?.disconnect() } catch (_: Exception) {}
        sftp = null
        client = null
    }
}
