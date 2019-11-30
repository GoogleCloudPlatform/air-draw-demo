import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.google.gms.google-services") version ("4.3.3")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("com.google.android.material:material:1.2.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta3")

    implementation("androidx.core:core-ktx:1.1.0")
//    implementation("androidx.ui:ui-framework:0.1.0-SNAPSHOT")
    implementation("androidx.ui:ui-layout:0.1.0-dev02")
    implementation("androidx.ui:ui-material:0.1.0-dev02")
    implementation("androidx.ui:ui-tooling:0.1.0-dev02")

    implementation(project(":common"))

    implementation("com.google.firebase:firebase-ml-vision:24.0.1")
    implementation("com.google.firebase:firebase-ml-vision-image-label-model:19.0.0")

    implementation("org.tensorflow:tensorflow-lite:1.13.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2")

    implementation("io.micronaut:micronaut-http-client:1.2.6")

    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.1")
    api("uk.uuid.slf4j:slf4j-android:1.7.28-0")

    kapt("io.micronaut:micronaut-inject-java:1.2.6")
}

googleServices {
    disableVersionCheck = true
}

android {
    compileSdkVersion(29)
    buildToolsVersion = "28.0.3"

    defaultConfig {
        applicationId = "com.jamesward.airdraw"
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        val drawUrl: String? by project
        if (drawUrl != null) {
            manifestPlaceholders = mapOf("drawurl" to drawUrl)
        }
        else {
            // 10.0.2.2 is the IP for your machine from the Android emulator
            manifestPlaceholders = mapOf("drawurl" to "http://10.0.2.2:8080")
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

    lintOptions {
        lintOptions {
            warning("InvalidPackage")
        }
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/spring-configuration-metadata.json")
        exclude("META-INF/config-properties.adoc")
        exclude("META-INF/io.netty.versions.properties")
    }
}
