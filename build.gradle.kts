buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven (url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.3.60"))
        classpath("com.android.tools.build:gradle:3.5.2")
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven (url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
