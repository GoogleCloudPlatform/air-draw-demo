plugins {
    kotlin("jvm")
}

dependencies {
    compile(kotlin("stdlib"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
