plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 版の番号は GitHub Actions の実行番号から自動で決める（共通ルール）。
// 手で番号を付けていた頃（0.7.0・versionCode 21）より大きくするため、gradle.properties の versionOffset を使う
val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull() ?: 1
val versionOffset = (project.findProperty("versionOffset") as String?)?.toIntOrNull() ?: 0
val versionMajor = (project.findProperty("versionMajor") as String?)?.toIntOrNull() ?: 1
val appVersionName = "$versionMajor.0.$buildNumber"

// GitHub Actions がリリースのタグ（v＋versionName）を作るのに使う
tasks.register("printVersionName") { doLast { println(appVersionName) } }

android {
    namespace = "app.jwhabits"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.jwhabits"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber + versionOffset
        // GitHub のリリースのタグは「v」＋ versionName（アプリの更新は versionName をくらべる）
        versionName = appVersionName
    }

    // 署名：前から使っている Mac の debug 鍵をリポジトリに置いて使う（共通ルール。鍵が変わると上書きで更新できない）
    signingConfigs {
        create("release") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // 縮小（R8）は Room・Glance・serialization の確認が要るので、今はオフのまま
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit)
}
