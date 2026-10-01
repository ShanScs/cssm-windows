import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "com.cssm.desktop"
version = "0.1.0"

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
            packageVersion = "1.1.3"
            windows {
                menu = true
                // 开始菜单里建 "Cssm" 文件夹：应用和卸载入口分开放，
                // 避免快捷方式被归到名字奇怪的分组（jpackage 默认按 vendor 分组）
                menuGroup = "Cssm"
                // 固定升级 UUID：同一个应用必须永远不变。
                // 之前没配它，jpackage 每次构建都随机生成一个，
                // Windows 会把每个构建当成毫不相干的新产品并排安装——
                // 这正是开始菜单快捷方式错乱、以及同版本号必须手动卸载的根源。
                upgradeUuid = "eb72eb1c-d421-4f77-a2ba-1bd24ab9277c"
                // 每次发版递增：Windows Installer 靠它判断新旧版本做覆盖升级
                msiPackageVersion = "1.1.3"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
