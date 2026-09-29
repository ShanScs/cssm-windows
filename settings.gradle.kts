pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    repositories {
        // JediTerm 等走代理不稳定的包：已用 curl 预下载到项目内本地仓库
        maven { url = uri("libs-repo") }
        google()
        mavenCentral()
        // JediTerm（Swing 终端模拟器）不在 Maven Central，在 JetBrains 仓库
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}
rootProject.name = "cssm-desktop"
