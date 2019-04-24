import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.30"
    id("com.github.johnrengelman.shadow") version "4.0.4"
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

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
