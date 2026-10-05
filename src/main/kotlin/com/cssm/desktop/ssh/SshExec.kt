package com.cssm.desktop.ssh

import com.cssm.desktop.data.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
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
                // 密钥内容写临时文件再加载（sshj 的 loadKeys 只认文件路径）
                val kp: KeyProvider = c.loadKeyFromString(server.privateKey, server.keyPassphrase)
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

    /** 密钥内容规范化后加载：PKCS#1 老格式 RSA 用 BouncyCastle 转 PKCS#8 再给 sshj。 */
    private fun SSHClient.loadKeyFromString(pem: String, passphrase: String): KeyProvider {
        val normalized = normalizePem(pem.trim())
        val tmp = java.io.File.createTempFile("cssm_key_", ".pem")
        try {
            tmp.writeText(normalized)
            return loadKeys(tmp.absolutePath, passphrase.ifBlank { "" })
        } finally {
            try { tmp.delete() } catch (_: Exception) {}
        }
    }

    /** 把 PKCS#1 (BEGIN RSA PRIVATE KEY) 转成 PKCS#8 (BEGIN PRIVATE KEY)，sshj 更稳。 */
    private fun normalizePem(pem: String): String {
        if (!pem.contains("BEGIN RSA PRIVATE KEY")) return pem
        return try {
            val parser = org.bouncycastle.openssl.PEMParser(java.io.StringReader(pem))
            val obj = parser.readObject()
            parser.close()
            val keyPair = (obj as org.bouncycastle.openssl.PEMKeyPair)
            val info = keyPair.privateKeyInfo // 已是 PKCS#8，直接写出
            val writer = java.io.StringWriter()
            val pemWriter = org.bouncycastle.openssl.jcajce.JcaPEMWriter(writer)
            pemWriter.writeObject(info)
            pemWriter.close()
            writer.toString()
        } catch (_: Exception) {
            pem // 转失败就用原文
        }
    }
}
