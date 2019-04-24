import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    id("com.google.cloud.tools.jib") version "1.0.2"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.30"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile("com.github.haifengl:smile-plot:1.5.2")
    compile("com.github.haifengl:smile-interpolation:1.5.2")
    compile("com.github.haifengl:smile-netlib:1.5.2")

    compile("io.micronaut:micronaut-runtime:1.1.0")
    compile("io.micronaut:micronaut-http-client:1.1.0")
    compile("io.micronaut:micronaut-http-server-netty:1.1.0")
    compile("io.micronaut:micronaut-views:1.1.0")
    compile("ch.qos.logback:logback-classic:1.2.3")

    compile("com.google.cloud:google-cloud-vision:1.70.0")

    runtime("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7")
    runtime("org.thymeleaf:thymeleaf:3.0.11.RELEASE")

    kapt("io.micronaut:micronaut-inject-java:1.1.0")
    kapt("io.micronaut:micronaut-validation:1.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

configure<AllOpenExtension> {
    annotation("io.micronaut.aop.Around")
}

tasks.all {
    when(this) {
        is JavaForkOptions -> {
            jvmArgs("-noverify")
            jvmArgs("-XX:TieredStopAtLevel=1")
        }
    }
}

application {
    mainClassName = "com.jamesward.airdraw.WebAppKt"
}

jib {
    val projectId: String? by project
    val repoName: String? by project

    to.image = "gcr.io/$projectId/$repoName"
    container {
        mainClass = application.mainClassName
        ports = listOf("8080")
    }
}
