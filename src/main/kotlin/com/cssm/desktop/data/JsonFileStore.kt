package com.cssm.desktop.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * JSON 文件原子写入 + 旧版本数据丢失 bug 的一次性迁移。
 *
 * 背景：Windows 上 File.renameTo() 在目标文件已存在时直接返回 false
 * （不抛异常），旧版本第二次及以后的 persist 因此全部静默丢失——内存里
 * 数据正常，一重启就只剩第一次保存的内容。改用 Files.move(REPLACE_EXISTING)
 * 后 Windows 下覆盖写入正常。
 *
 * 迁移：旧版本每次 persist 都会把全量数据写进 tmp（只是最后 rename 失败），
 * 所以残留的 tmp 实际是最新全量数据；load 前若 tmp 比主文件新，先用它恢复，
 * 把旧版本弄丢的数据找回来（一次性）。
 */
internal object JsonFileStore {

    /** load 之前调用：用残留 tmp 恢复数据（一次性；无 tmp 或 tmp 更旧时直接返回） */
    fun migrateTmp(file: File, tmpName: String) {
        try {
            val dir = file.parentFile ?: return
            val tmp = File(dir, tmpName)
            if (!tmp.exists()) return
            if (!file.exists() || tmp.lastModified() > file.lastModified()) {
                dir.mkdirs()
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }

    /** 原子写入：先写 tmp 再 move 覆盖，Windows 下可用 */
    fun writeAtomic(file: File, tmpName: String, text: String) {
        val dir = file.parentFile
        dir?.mkdirs()
        val tmp = File(dir, tmpName)
        tmp.writeText(text)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
