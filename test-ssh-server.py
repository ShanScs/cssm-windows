#!/usr/bin/env python3
"""Cssm 桌面版本地测试 SSH 服务器。

监听 127.0.0.1:2222，账号 testuser / testpass123。
- shell 请求：pty + /bin/bash 真实交互 shell（支持 resize）
- exec 请求：直接执行命令并回传输出（给 StatsCollector/OsDetector 用）
仅用于本地联调，不要对外暴露。
"""
import asyncio
import fcntl
import os
import pty
import signal
import struct
import termios

import asyncssh


class TestServer(asyncssh.SSHServer):
    def password_auth_supported(self):
        return True

    def validate_password(self, username, password):
        return username == "testuser" and password == "testpass123"


def set_winsize(fd, rows, cols):
    try:
        fcntl.ioctl(fd, termios.TIOCSWINSZ, struct.pack("HHHH", rows, cols, 0, 0))
    except OSError:
        pass


async def pump_fd_to_process(fd, process, done):
    import codecs
    decoder = codecs.getincrementaldecoder("utf-8")(errors="replace")
    loop = asyncio.get_event_loop()
    try:
        while not done.is_set():
            try:
                data = await loop.run_in_executor(None, os.read, fd, 65536)
            except OSError:
                break
            if not data:
                break
            try:
                text = decoder.decode(data)
                if text:
                    process.stdout.write(text)
            except (BrokenPipeError, asyncssh.BreakReceived):
                break
    finally:
        done.set()


async def pump_process_to_fd(process, fd, done):
    try:
        while not done.is_set():
            try:
                data = await process.stdin.read(65536)
            except (asyncssh.BreakReceived, asyncio.IncompleteReadError):
                break
            if data is None:
                break
            try:
                os.write(fd, data.encode("utf-8") if isinstance(data, str) else data)
            except OSError:
                break
    finally:
        done.set()


async def handle_client(process: asyncssh.SSHServerProcess):
    # exec 请求：直接跑命令
    if process.command:
        proc = await asyncio.create_subprocess_shell(
            process.command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
        out, _ = await proc.communicate()
        if out:
            process.stdout.write(out.decode("utf-8", errors="replace"))
        process.exit(proc.returncode or 0)
        return

    # shell 请求：pty + bash
    term = process.get_terminal_type() or "xterm-256color"
    size = process.get_terminal_size() or (80, 24)
    cols, rows = size[0], size[1]

    pid, fd = pty.fork()
    if pid == 0:
        os.environ["TERM"] = term
        set_winsize(1, rows, cols)
        os.execv("/bin/bash", ["/bin/bash", "-i"])
        os._exit(1)

    set_winsize(fd, rows, cols)

    def on_resize(channel, width, height, pixwidth, pixheight):
        set_winsize(fd, height, width)
        try:
            os.kill(pid, signal.SIGWINCH)
        except ProcessLookupError:
            pass

    process.channel.set_terminal_size_changed_handler(on_resize)

    done = asyncio.Event()
    t1 = asyncio.ensure_future(pump_fd_to_process(fd, process, done))
    t2 = asyncio.ensure_future(pump_process_to_fd(process, fd, done))
    await done.wait()
    for t in (t1, t2):
        t.cancel()
    try:
        os.close(fd)
    except OSError:
        pass
    try:
        _, status = os.waitpid(pid, os.WNOHANG)
    except ChildProcessError:
        pass
    process.exit(0)


async def main():
    await asyncssh.listen(
        "127.0.0.1", 2222,
        server_factory=TestServer,
        process_factory=handle_client,
        server_host_keys=["/tmp/cssm-test-ssh-key"],
    )
    print("SSH test server on 127.0.0.1:2222 (testuser/testpass123)", flush=True)
    await asyncio.get_event_loop().create_future()


if __name__ == "__main__":
    if not os.path.exists("/tmp/cssm-test-ssh-key"):
        import subprocess
        subprocess.run(
            ["ssh-keygen", "-t", "ed25519", "-f", "/tmp/cssm-test-ssh-key", "-N", ""],
            check=True, capture_output=True,
        )
    asyncio.run(main())
