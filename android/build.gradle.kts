import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.android.support:appcompat-v7:28.0.0")
    implementation("com.android.support:design:28.0.0")

    implementation("com.github.kittinunf.fuel:fuel-android:2.0.1")

    implementation(project(":common"))
}

android {
    compileSdkVersion(28)
    buildToolsVersion = "28.0.3"

    defaultConfig {
        applicationId = "com.jamesward.airdraw"
        minSdkVersion(23)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"

        val drawUrl: String? by project
        if (drawUrl != null) {
            val usesCleartextTraffic = if (drawUrl!!.startsWith("https")) "false" else "true"
            manifestPlaceholders = mapOf("usesCleartextTraffic" to usesCleartextTraffic)
            resValue("string", "draw_url", drawUrl!!)
        }
        else {
            manifestPlaceholders = mapOf("usesCleartextTraffic" to "true")
            resValue("string", "draw_url", "http://10.0.2.2:8080/draw")
        }
    }

    sourceSets["main"].java.srcDir("src/main/kotlin")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        this as KotlinJvmOptions
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
    }
}
