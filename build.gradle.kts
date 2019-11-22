buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.2")
        classpath(kotlin("gradle-plugin", "1.3.60"))
        classpath("com.github.ben-manes:gradle-versions-plugin:0.27.0")
    }
}

apply(plugin = "com.github.ben-manes.versions")

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }
}
