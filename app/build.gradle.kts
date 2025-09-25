import java.io.ByteArrayOutputStream
import java.io.File // <-- 新增 import

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

fun getVersionCodeFromGit(): Int {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = byteOut
        }
        byteOut.toString().trim().toInt()
    } catch (e: Exception) {
        1
    }
}

android {
    namespace = "com.example.remindersapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.remindersapp"
        minSdk = 26
        targetSdk = 35
        versionCode = getVersionCodeFromGit()
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // --- 这是实现自定义文件名的最终、最简单的方案 ---
    // 这个配置会影响所有构建变体（debug 和 release）
    setProperty("archivesBaseName", "RemindersApp-v${defaultConfig.versionName}-${defaultConfig.versionCode}")

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 核心库
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose - 使用最新的 BOM 来统一管理版本
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room for database
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.35.1-alpha")

    // 测试库
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose:ui-test-junit4")
    debugImplementation("androidx.compose:ui-tooling")
    debugImplementation("androidx.compose:ui-test-manifest")
}