// settings.gradle.kts

pluginManagement {
    repositories {
        // --- 关键改动 ---
        // 1. 将阿里云镜像源放在最前面，作为首选仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 2. 保留官方源作为备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // --- 关键改动 ---
        // 1. 同样，将阿里云镜像源放在最前面
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 2. 保留官方源作为备用
        google()
        mavenCentral()
    }
}

rootProject.name = "RemindersApp"
include(":app")