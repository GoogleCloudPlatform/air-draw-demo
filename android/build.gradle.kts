import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application") version "3.4.0"
    kotlin("android") version "1.3.30"
    kotlin("android.extensions") version "1.3.30"
    kotlin("kapt") version "1.3.30"
    id("kotlinx-serialization") version "1.3.30"
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    google()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation(kotlin("reflect", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.0")

    implementation("com.android.support:appcompat-v7:28.0.0")
    implementation("com.android.support:design:28.0.0")

    implementation("com.github.kittinunf.fuel:fuel-android:2.0.1")
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
