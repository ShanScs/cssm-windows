package com.cssm.desktop.ssh
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringReader
import java.io.StringWriter

import com.cssm.desktop.data.ForwardRule
import com.cssm.desktop.data.Server
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.connection.channel.direct.Parameters
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.net.ServerSocket
import kotlin.coroutines.coroutineContext

/**
 * SSH 本地端口转发管理：每条规则一条 SSH 长连接 + 本地监听。
 * 启动：本地 localPort -> 经服务器 -> remoteHost:remotePort。
 * listen() 阻塞在 IO 线程，停止时关闭 ServerSocket 即可退出。
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

class ForwardManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 正在运行的规则 id 集合 */
    private val _running = MutableStateFlow<Set<Long>>(emptySet())
    val running: StateFlow<Set<Long>> = _running.asStateFlow()

    /** 规则 id -> 错误信息（启动失败时） */
    private val _errors = MutableStateFlow<Map<Long, String>>(emptyMap())
    val errors: StateFlow<Map<Long, String>> = _errors.asStateFlow()

    private data class Tunnel(
        val client: SSHClient,
        val socket: ServerSocket,
        val job: Job
    )

    private val tunnels = mutableMapOf<Long, Tunnel>()

    fun isRunning(id: Long): Boolean = synchronized(tunnels) { tunnels.containsKey(id) }

    fun start(rule: ForwardRule, server: Server) {
        synchronized(tunnels) { if (tunnels.containsKey(rule.id)) return }
        setError(rule.id, null)
        scope.launch {
            var client: SSHClient? = null
            var ss: ServerSocket? = null
            try {
                client = withContext(Dispatchers.IO) {
                    val c = SSHClient()
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
                    c
                }
                ss = ServerSocket(rule.localPort)
                val fwd = client.newLocalPortForwarder(
                    Parameters(
                        "127.0.0.1", rule.localPort,
                        rule.remoteHost.ifBlank { "127.0.0.1" }, rule.remotePort
                    ),
                    ss
                )
                synchronized(tunnels) {
                    tunnels[rule.id] = Tunnel(client, ss, coroutineContext[Job]!!)
                }
                _running.value = _running.value + rule.id
                // 阻塞直到停止（关闭 ServerSocket）
                withContext(Dispatchers.IO) { fwd.listen() }
            } catch (e: Exception) {
                if (e !is CancellationException) setError(rule.id, e.message ?: "启动失败")
            } finally {
                try { ss?.close() } catch (_: Exception) { }
                try { client?.disconnect() } catch (_: Exception) { }
                synchronized(tunnels) { tunnels.remove(rule.id) }
                _running.value = _running.value - rule.id
            }
        }
    }

    fun stop(id: Long) {
        val t = synchronized(tunnels) { tunnels.remove(id) } ?: return
        try { t.socket.close() } catch (_: Exception) { }
        t.job.cancel()
    }

    private fun setError(id: Long, msg: String?) {
        val cur = _errors.value.toMutableMap()
        if (msg == null) cur.remove(id) else cur[id] = msg
        _errors.value = cur
    }

    fun stopAll() {
        val ids = synchronized(tunnels) { tunnels.keys.toList() }
        ids.forEach { stop(it) }
    }


}
