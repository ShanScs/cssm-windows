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
    val normalized = pem.trim().replace("\r\n", "\n").replace("\r", "\n")
    if (!normalized.contains("BEGIN RSA PRIVATE KEY")) return normalized
    // 方法1: BouncyCastle PEMParser
    try {
        val parser = PEMParser(StringReader(normalized))
        val obj = parser.readObject()
        parser.close()
        val info = when (obj) {
            is PEMKeyPair -> obj.privateKeyInfo
            is org.bouncycastle.asn1.pkcs.PrivateKeyInfo -> obj
            else -> null
        }
        if (info != null) {
            val sw = StringWriter()
            val pw = JcaPEMWriter(sw)
            pw.writeObject(info)
            pw.close()
            val result = sw.toString()
            if (result.contains("BEGIN PRIVATE KEY")) return result
        }
    } catch (_: Exception) {}
    // 方法2: 手动构造 PKCS#8 (RSA)
    try {
        val b64 = normalized.lines()
            .filter { !it.startsWith("-----") && it.isNotBlank() }
            .joinToString("")
        val pkcs1 = java.util.Base64.getDecoder().decode(b64)
        // PKCS#8 头: SEQUENCE { INTEGER 0, SEQUENCE { OID rsaEncryption, NULL }, OCTET STRING { pkcs1 } }
        val rsaOid = byteArrayOf(0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)
        val algId = byteArrayOf(0x30, 0x0D) + rsaOid + byteArrayOf(0x05, 0x00)
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val octet = byteArrayOf(0x04) + encodeLen(pkcs1.size) + pkcs1
        val body = version + algId + octet
        val der = byteArrayOf(0x30) + encodeLen(body.size) + body
        val out = "-----BEGIN PRIVATE KEY-----\n" +
            java.util.Base64.getEncoder().encodeToString(der).chunked(64).joinToString("\n") +
            "\n-----END PRIVATE KEY-----\n"
        return out
    } catch (_: Exception) {}
    return normalized
}

/** DER 长度编码 */
private fun encodeLen(len: Int): ByteArray {
    return if (len < 128) byteArrayOf(len.toByte())
    else {
        val bytes = mutableListOf<Byte>()
        var v = len
        while (v > 0) { bytes.add(0, (v and 0xFF).toByte()); v = v shr 8 }
        byteArrayOf((0x80 or bytes.size).toByte()) + bytes.toByteArray()
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
                // 注意：不能立即删除，sshj 是懒加载，认证时才读文件
                val tmpKeyFile = java.io.File.createTempFile("cssm_key_", ".pem")
                tmpKeyFile.deleteOnExit()
                tmpKeyFile.writeText(convertPkcs1ToPkcs8(server.privateKey.trim()).replace("\r\n", "\n").replace("\r", "\n"))
                val kp: KeyProvider = c.loadKeys(tmpKeyFile.absolutePath, server.keyPassphrase.ifBlank { "" })
                    c.authPublickey(server.username, kp)
                try { tmpKeyFile.delete() } catch (_: Exception) {}
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
