/*
When using the android artifact, the android project fails with:
Execution failed for task ':android:processDebugResources'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.Workers$ActionFacade
   > AAPT2 aapt2-4.1.0-6503028-linux Daemon #0: Unexpected error during link, attempting to stop daemon.
     This should not happen under normal circumstances, please file an issue if it does.
 */

plugins {
    //id("com.android.application")// version "4.1.0"
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

/*
android {
    compileSdkVersion(30)
    //buildToolsVersion = "30.0.2"
}
 */

kotlin {
    // note: you can't have both jvm & android
    jvm()
    /*
    {
        withJava()
    }
     */

    //android()

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
