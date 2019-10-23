buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.1")
        classpath(kotlin("gradle-plugin", "1.3.50"))
        if (file("google-services.json").exists()) {
            classpath("com.google.gms:google-services:4.3.2")
        }
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
