buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.31")
        classpath("com.android.tools.build:gradle:3.4.0")
        classpath("com.google.gms:google-services:4.3.2")
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
}

tasks.create("stage") {
    dependsOn("server:shadowJar")
}
