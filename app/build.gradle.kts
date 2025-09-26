import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose - 使用 BOM 统一管理版本
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // --- 新增：实现扩展图标库 ---
    implementation(libs.androidx.compose.material.icons.extended)

    // Room for database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore for settings
    implementation(libs.androidx.datastore.preferences)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Permissions
    implementation(libs.accompanist.permissions)

    // 测试库
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}