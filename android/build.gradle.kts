import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("com.google.gms.google-services") version ("4.3.2")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("com.google.android.material:material:1.2.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta3")

    implementation("com.github.kittinunf.fuel:fuel-android:2.0.1")

    implementation(project(":common"))
    implementation("com.google.firebase:firebase-ml-vision:24.0.0")
    implementation("com.google.firebase:firebase-ml-vision-image-label-model:19.0.0")

    implementation("org.tensorflow:tensorflow-lite:1.13.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.1.1")
}

googleServices {
    disableVersionCheck = true
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
            resValue("string", "draw_url", "http://10.0.2.2:8080")
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

    aaptOptions {
        noCompress("tflite")
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
    }
}
