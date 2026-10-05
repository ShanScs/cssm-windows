package com.cssm.desktop.ssh
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringReader
import java.io.StringWriter

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



/** PKCS#1 (BEGIN RSA PRIVATE KEY) 转 PKCS#8，sshj 才能读 */
private fun convertPkcs1ToPkcs8(pem: String): String {
    if (!pem.contains("BEGIN RSA PRIVATE KEY")) return pem
    return try {
        val parser = PEMParser(StringReader(pem))
        val obj = parser.readObject()
        parser.close()
        val keyPair = obj as PEMKeyPair
        val info = keyPair.privateKeyInfo
        val sw = StringWriter()
        val pw = JcaPEMWriter(sw)
        pw.writeObject(info)
        pw.close()
        sw.toString()
    } catch (_: Exception) {
        pem
    }
}

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
                // sshj 的 loadKeys 只认文件路径，密钥内容先写临时文件
                val kp: KeyProvider = run {
                    val tmp = java.io.File.createTempFile("cssm_key_", ".pem")
                    try {
                        tmp.writeText(convertPkcs1ToPkcs8(server.privateKey.trim()))
                        c.loadKeys(tmp.absolutePath, server.keyPassphrase.ifBlank { "" })
                    } finally {
                        try { tmp.delete() } catch (_: Exception) {}
                    }
                }
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


}
