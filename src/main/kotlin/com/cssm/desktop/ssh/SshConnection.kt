package com.cssm.desktop.ssh

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
                // 密钥内容写临时文件再加载（sshj 的 loadKeys 只认文件路径）
                val kp: KeyProvider = c.loadKeyFromString(server.privateKey, server.keyPassphrase)
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
