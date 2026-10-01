package com.cssm.desktop.ssh

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
                        val kp: KeyProvider = c.loadKeys(
                            server.privateKey,
                            server.keyPassphrase.ifEmpty { null }
                        )
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
