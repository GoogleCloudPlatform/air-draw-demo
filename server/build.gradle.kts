import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile(project(":common"))

    compile("com.github.haifengl:smile-plot:1.5.3")
    compile("com.github.haifengl:smile-interpolation:1.5.3")
    compile("com.github.haifengl:smile-netlib:1.5.3")

    compile("io.micronaut:micronaut-runtime:1.2.6")
    compile("io.micronaut:micronaut-http-server-netty:1.2.6")
    compile("io.micronaut:micronaut-views:1.2.0")
    compile("ch.qos.logback:logback-classic:1.2.3")

    compile("com.google.cloud:google-cloud-vision:1.99.0")
    compile("com.google.cloud:google-cloud-pubsub:1.101.0")
    compile("io.netty:netty-tcnative-boringssl-static:2.0.27.Final")

    runtime("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.1")
    runtime("org.thymeleaf:thymeleaf:3.0.11.RELEASE")

    kapt("io.micronaut:micronaut-inject-java:1.2.6")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        javaParameters = true
    }
}

application {
    mainClassName = "com.jamesward.airdraw.WebAppKt"
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
