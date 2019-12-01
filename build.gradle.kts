buildscript {
//    extra.apply {
//        set("kotlin_version", "1.3.60-eap-25")
//    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven (url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
//        classpath(kotlin("gradle-plugin", "1.3.60-eap-25"))
//        classpath("com.android.tools.build:gradle:4.0.0-alpha03")
        classpath(kotlin("gradle-plugin", "1.3.61"))
        classpath("com.android.tools.build:gradle:4.0.0-alpha04")
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
