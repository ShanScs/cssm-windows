package com.cssm.desktop.ssh

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import java.io.IOException
import java.io.InputStreamReader

/**
 * JediTerm 与 sshj shell 流之间的桥接。
 *
 * - read：阻塞读取远端字节，经 InputStreamReader 按 UTF-8 解码为 char
 *   （Reader 内部处理跨 read 调用的多字节字符截断）。
 * - resize：JediTerm 在面板尺寸变化时回调，转成 SSH window-change 发给远端。
 */
class SshTtyConnector(
    private val ssh: SshConnection,
    private val onResize: (cols: Int, rows: Int) -> Unit = { c, r -> ssh.resize(c, r) }
) : TtyConnector {

    private val input = requireNotNull(ssh.shellInput()) { "SSH shell 未建立" }
    private val output = requireNotNull(ssh.shellOutput()) { "SSH shell 未建立" }
    private val reader = InputStreamReader(input, Charsets.UTF_8)

    @Volatile
    private var closed = false

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        if (closed) return -1
        return try {
            val n = reader.read(buf, offset, length)
            if (n == -1) { closed = true }
            n
        } catch (_: IOException) {
            closed = true
            -1
        }
    }

    override fun write(bytes: ByteArray) {
        if (closed) return
        try {
            output.write(bytes)
            output.flush()
        } catch (_: IOException) {
            closed = true
        }
    }

    override fun write(string: String) = write(string.toByteArray(Charsets.UTF_8))

    override fun isConnected(): Boolean = !closed && ssh.isConnected

    override fun waitFor(): Int = 0

    override fun ready(): Boolean =
        try {
            input.available() > 0
        } catch (_: Exception) {
            false
        }

    override fun resize(termSize: TermSize) {
        try {
            onResize(termSize.columns, termSize.rows)
        } catch (_: Exception) {
        }
    }

    override fun getName(): String = "Cssm SSH"

    override fun close() {
        closed = true
    }
}
