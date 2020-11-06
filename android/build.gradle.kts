plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

val composeVersion = "1.0.0-alpha03"

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":common"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.compose.foundation:foundation-layout:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")

    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime:2.1.1")
    implementation("io.micronaut:micronaut-http-client:2.1.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    implementation("uk.uuid.slf4j:slf4j-android:1.7.28-0")

    kapt("io.micronaut:micronaut-inject-java:2.1.2")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        applicationId = "com.jamesward.airdraw"
        minSdkVersion(23)
        targetSdkVersion(30)
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
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        useIR = true
    }

    packagingOptions {
        exclude("META-INF/main.kotlin_module")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/config-properties.adoc")
        exclude("META-INF/io.netty.versions.properties")
        exclude("META-INF/spring-configuration-metadata.json")
    }

    lintOptions {
        isAbortOnError = false
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerVersion = org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION.toString()
        kotlinCompilerExtensionVersion = composeVersion
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xallow-jvm-ir-dependencies", "-Xskip-prerelease-check")
    }
}
