plugins {
    kotlin("jvm")
    id("kotlinx-serialization") version KotlinVersion.CURRENT.toString()
}

dependencies {
    compile(kotlin("stdlib"))

    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
