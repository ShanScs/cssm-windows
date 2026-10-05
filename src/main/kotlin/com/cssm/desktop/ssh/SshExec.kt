package com.cssm.desktop.ssh

import com.cssm.desktop.data.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.util.concurrent.TimeUnit

/**
 * 一次性 SSH 命令执行：建连 -> exec -> 断开，用于脚本页等非终端场景。
 */
object SshExec {

    @Throws(Exception::class)
    suspend fun run(server: Server, command: String, timeoutSec: Long = 60): String =
        withContext(Dispatchers.IO) {
            val c = SSHClient()
            try {
                c.addHostKeyVerifier(PromiscuousVerifier())
                c.connectTimeout = 15_000
                c.connect(server.host, server.port)
                if (server.authType == Server.AUTH_KEY && server.privateKey.isNotBlank()) {
                    // 从字符串内容加载密钥（loadKeys 只认文件路径）
                    val kp: KeyProvider = loadKeyFromString(c, server.privateKey, server.keyPassphrase)
                    c.authPublickey(server.username, kp)
                } else {
                    c.authPassword(server.username, server.password)
                }
                c.startSession().use { s ->
                    val cmd = s.exec(command)
                    cmd.join(timeoutSec, TimeUnit.SECONDS)
                    val out = cmd.inputStream.readBytes().toString(Charsets.UTF_8)
                    val err = cmd.errorStream.readBytes().toString(Charsets.UTF_8)
                    (out + if (err.isNotBlank()) "\n[stderr]\n$err" else "").ifBlank { "(无输出)" }
                }
            } finally {
                try { c.disconnect() } catch (_: Exception) { }
            }
        }

    /** 从 PEM 字符串加载密钥，兼容 OpenSSH / PKCS#1 / PKCS#8 格式。 */
    private fun loadKeyFromString(c: SSHClient, pem: String, passphrase: String): KeyProvider {
        val pw = passphrase.toCharArray()
        val trimmed = pem.trim()
        return try {
            OpenSSHKeyFile().apply { init(StringReader(trimmed), pw) }
        } catch (_: Exception) {
            PKCS8KeyFile().apply { init(StringReader(trimmed), pw) }
        }
    }
}
