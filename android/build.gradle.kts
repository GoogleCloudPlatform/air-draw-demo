import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("kotlinx-serialization") version "1.3.31"
}

apply(plugin = "com.google.gms.google-services")

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation(kotlin("reflect", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.0")

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("com.google.android.material:material:1.1.0-alpha10")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta2")

    implementation("com.github.kittinunf.fuel:fuel-android:2.0.1")

    implementation("com.google.firebase:firebase-ml-vision:23.0.0")
    implementation("com.google.firebase:firebase-ml-vision-image-label-model:18.0.0")

    implementation("org.tensorflow:tensorflow-lite:1.13.1")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    aaptOptions {
        noCompress("tflite")
    }

    lintOptions {
        lintOptions {
            warning("InvalidPackage")
        }
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
    }
}
