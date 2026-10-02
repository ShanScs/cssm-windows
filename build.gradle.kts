import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.awt.BasicStroke
import java.awt.Color
import java.net.URI
import org.gradle.api.Task
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import javax.imageio.ImageIO

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "com.cssm.desktop"
version = "0.1.0"

// Windows 安装包图标输出位置（generateWinIcon 任务在文件末尾定义，构建时生成）
val winIconFile = layout.buildDirectory.file("winIcon/icon.ico")

// 仓库统一在 settings.gradle.kts 的 dependencyResolutionManagement 里声明，
// 这里不重复声明（否则会屏蔽 settings 的仓库）。

dependencies {
    // Compose Multiplatform 桌面 UI
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    // SSH
    implementation("com.hierynomus:sshj:0.38.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    // 终端模拟器（Swing，嵌进 Compose）
    implementation("org.jetbrains.jediterm:jediterm-core:3.64")
    implementation("org.jetbrains.jediterm:jediterm-ui:3.64")
    // JNA 已移除：改用 AWT 绝对坐标拖拽，不再需要
    // 协程 Swing 调度器 / JSON 存储
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.cssm.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Cssm"
            vendor = "Cssm"
            // 原生包版本号要求 MAJOR > 0
            // 注意：release job 用它拼 tag（v<packageVersion>），每次发版必须和 msiPackageVersion 同步递增
            packageVersion = "1.2.20"
            windows {
                menu = true
                // 安装包/快捷方式图标：构建时由 generateWinIcon 从矢量描述生成，
                // 不依赖二进制资源文件（GitHub 推送通道不支持二进制）。
                iconFile.set(winIconFile)
                // 开始菜单里建 "Cssm" 文件夹：应用和卸载入口分开放，
                // 避免快捷方式被归到名字奇怪的分组（jpackage 默认按 vendor 分组）
                menuGroup = "Cssm"
                // 固定升级 UUID：同一个应用必须永远不变。
                // 之前没配它，jpackage 每次构建都随机生成一个，
                // Windows 会把每个构建当成毫不相干的新产品并排安装——
                // 这正是开始菜单快捷方式错乱、以及同版本号必须手动卸载的根源。
                upgradeUuid = "eb72eb1c-d421-4f77-a2ba-1bd24ab9277c"
                // 每次发版递增：Windows Installer 靠它判断新旧版本做覆盖升级
                msiPackageVersion = "1.2.20"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

/**
 * Windows 安装包图标：用纯 JDK（java.awt + ImageIO）把安卓版图标
 * （深色圆角底 + 终端提示符 >_）渲染成多尺寸 ICO。
 * 不依赖任何二进制资源文件，jpackage 打包前自动执行。
 */
val generateWinIcon by tasks.registering {
    outputs.file(winIconFile)
    doLast {
        val icoFile = winIconFile.get().asFile
        icoFile.parentFile.mkdirs()
        val sizes = listOf(16, 24, 32, 48, 64, 128, 256)
        val pngs = sizes.map { size ->
            val img = BufferedImage(
                size, size, BufferedImage.TYPE_INT_ARGB
            )
            val g = img.createGraphics() as Graphics2D
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )
            val k = size / 108.0
            // 深色圆角底 #0B0E14
            g.color = Color(11, 14, 20)
            val radius = (112 * size / 512.0).toInt()
            g.fillRoundRect(0, 0, size, size, radius, radius)
            g.stroke = BasicStroke(
                (12 * k).toFloat(),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            )
            // chevron > ：M4,12 L48,54 L4,96（顶满），#7DD3FC
            g.color = Color(125, 211, 252)
            g.drawLine((4 * k).toInt(), (12 * k).toInt(), (48 * k).toInt(), (54 * k).toInt())
            g.drawLine((48 * k).toInt(), (54 * k).toInt(), (4 * k).toInt(), (96 * k).toInt())
            // underscore _ ：M52,96 L104,96（顶满），#6EE7B7
            g.color = Color(110, 231, 183)
            g.drawLine((52 * k).toInt(), (96 * k).toInt(), (104 * k).toInt(), (96 * k).toInt())
            g.dispose()
            val bos = ByteArrayOutputStream()
            ImageIO.write(img, "png", bos)
            bos.toByteArray()
        }
        DataOutputStream(FileOutputStream(icoFile)).use { out ->
            fun w16(v: Int) {
                out.writeByte(v and 0xFF)
                out.writeByte((v shr 8) and 0xFF)
            }
            fun w32(v: Int) {
                out.writeByte(v and 0xFF)
                out.writeByte((v shr 8) and 0xFF)
                out.writeByte((v shr 16) and 0xFF)
                out.writeByte((v shr 24) and 0xFF)
            }
            w16(0); w16(1); w16(sizes.size)
            var offset = 6 + 16 * sizes.size
            for (i in sizes.indices) {
                val dim = if (sizes[i] >= 256) 0 else sizes[i]
                out.writeByte(dim); out.writeByte(dim)
                out.writeByte(0); out.writeByte(0)
                w16(1); w16(32); w32(pngs[i].size); w32(offset)
                offset += pngs[i].size
            }
            for (p in pngs) out.write(p)
        }
        logger.lifecycle("Generated Windows icon: $icoFile")
    }
}
// 所有 package*Msi 任务都先生成图标
tasks.matching { it.name.contains("package") && it.name.contains("Msi", ignoreCase = true) }
    .configureEach { dependsOn(generateWinIcon) }




