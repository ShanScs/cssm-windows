package com.cssm.desktop.ssh
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringReader
import java.io.StringWriter

import com.cssm.desktop.data.Server
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.PTYMode
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.InputStream
import java.io.OutputStream
import java.security.Security
import java.util.concurrent.TimeUnit

/**
 * 基于 sshj 的 SSH 连接封装：交互式 shell + 按需 exec 通道。
 * 由安卓端 SshConnection 移植（去掉 Android 依赖，BouncyCastle 在此直接安装）。
 *
 * 已知限制：
 * - 使用 PromiscuousVerifier 跳过主机密钥校验，正式版应做 known_hosts 管理。
 * - 密码/私钥由调用方传入，明文存储问题见 Server 注释。
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

class SshConnection {

    private var client: SSHClient? = null
    private var shellSession: Session? = null
    private var shell: Session.Shell? = null

    val isConnected: Boolean
        get() = try {
            client?.isConnected == true
        } catch (_: Exception) {
            false
        }

    @Throws(Exception::class)
    fun connect(server: Server, cols: Int, rows: Int) {
        installFullBouncyCastle()
        disconnect()
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
                        tmp.writeText(convertPkcs1ToPkcs8(server.privateKey.trim()).replace("\r\n", "\n").replace("\r", "\n"))
                        c.loadKeys(tmp.absolutePath, server.keyPassphrase.ifBlank { "" })
                    } finally {
                        try { tmp.delete() } catch (_: Exception) {}
                    }
                }
                c.authPublickey(server.username, kp)
            } else {
                c.authPassword(server.username, server.password)
            }
            val session = c.startSession()
            try {
                session.allocatePTY(
                    "xterm-256color",
                    cols.coerceAtLeast(20),
                    rows.coerceAtLeast(10),
                    0, 0,
                    emptyMap<PTYMode, Int>()
                )
                shell = session.startShell()
                shellSession = session
                client = c
            } catch (e: Exception) {
                try { session.close() } catch (_: Exception) {}
                throw e
            }
        } catch (e: Exception) {
            try { c.disconnect() } catch (_: Exception) {}
            throw e
        }
    }

    fun shellInput(): InputStream? =
        try { shell?.inputStream } catch (_: Exception) { null }

    fun shellOutput(): OutputStream? =
        try { shell?.outputStream } catch (_: Exception) { null }

    /** 发送 SSH window-change 请求调整远端 PTY 尺寸。 */
    fun resize(cols: Int, rows: Int) {
        val sh = shell ?: return
        try {
            sh.changeWindowDimensions(cols.coerceAtLeast(1), rows.coerceAtLeast(1), 0, 0)
        } catch (_: Exception) {
        }
    }

    @Throws(Exception::class)
    fun exec(command: String, timeoutSec: Long = 15): String {
        val c = client ?: throw IllegalStateException("SSH 未连接")
        c.startSession().use { s ->
            val cmd = s.exec(command)
            cmd.join(timeoutSec, TimeUnit.SECONDS)
            return cmd.inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun disconnect() {
        try { shellSession?.close() } catch (_: Exception) {}
        try { client?.disconnect() } catch (_: Exception) {}
        shell = null
        shellSession = null
        client = null
    }



    companion object {
        @Volatile
        private var bcInstalled = false

        @Synchronized
        fun installFullBouncyCastle() {
            if (bcInstalled) return
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            bcInstalled = true
        }
    }
}
