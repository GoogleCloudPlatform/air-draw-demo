plugins {
    //id("com.android.application") version "4.1.0"
    kotlin("multiplatform")
    //id("com.android.library") version "4.1.0"
    //id("kotlin-android-extensions")
    kotlin("plugin.serialization")
    //kotlin("android")
    //id("com.android.library") version "4.1.0"
    //kotlin("android")
    //id("kotlin-android-extensions")
}

/*
dependencies {
    //implementation(kotlin("stdlib"))

    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
}
 */

/*
commonMain {
    dependencies {
        // Works as common dependency as well as the platform one
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    }
}
 */

/*
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
 */

/*
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
}
 */

kotlin {
    // note: you can't have both jvm & android
    jvm {
        withJava()
    }

    js {
        browser {
            //binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
                //implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0.1")
            }
        }
    }
}
