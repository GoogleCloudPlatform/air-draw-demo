import org.jetbrains.kotlin.cli.jvm.main

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

android {
    defaultConfig {
        compileSdkVersion(29)
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        /*
        android().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

         */

        /*
        main {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

         */

        /*
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        */

        /*
        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

         */

        /*
        androidMain
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

         */
    }

    jvm {
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    android()

    js()
}
