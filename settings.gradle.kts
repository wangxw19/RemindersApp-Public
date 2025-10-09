// settings.gradle.kts

pluginManagement {
    repositories {
        // 优先使用官方源
        google()
        mavenCentral()
        gradlePluginPortal()
        // 如果官方源无法访问，再使用镜像
        // maven { url = uri("https://maven.aliyun.com/repository/public") }
        // maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 优先使用官方源
        google()
        mavenCentral()
        // 如果官方源无法访问，再使用镜像
        // maven { url = uri("https://maven.aliyun.com/repository/public") }
        // maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "RemindersApp"
include(":app")