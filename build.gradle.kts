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
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.cssm.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Cssm"
            // 原生包版本号要求 MAJOR > 0
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
