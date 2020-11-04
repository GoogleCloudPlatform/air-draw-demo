plugins {
    id("com.android.application") version "4.0.0" apply false
    kotlin("android") version "1.4.10" apply false
    kotlin("jvm") version "1.4.10" apply false
    kotlin("js") version "1.4.10" apply false
    kotlin("multiplatform") version "1.4.10" apply false
    kotlin("plugin.allopen") version "1.4.10" apply false
    kotlin("plugin.serialization") version "1.4.10" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
}
