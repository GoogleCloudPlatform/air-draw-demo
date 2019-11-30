import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

/*
sourceSets {
    main {
        resources {
            srcDir(project(":web").file("build/classes/kotlin/main"))
        }
    }
}
 */

dependencies {
    implementation(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(project(":common"))

    //api(project(":web"))
    api(files("../web/build/libs/web.jar"))

    implementation("com.github.haifengl:smile-plot:1.5.3")
    implementation("com.github.haifengl:smile-interpolation:1.5.3")
    implementation("com.github.haifengl:smile-netlib:1.5.3")

    implementation("io.micronaut:micronaut-runtime:1.2.6")
    implementation("io.micronaut:micronaut-http-server-netty:1.2.6")
    implementation("io.micronaut:micronaut-views:1.2.0")
    api("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.google.cloud:google-cloud-vision:1.99.0")
    implementation("com.google.cloud:google-cloud-pubsub:1.101.0")
    api("io.netty:netty-tcnative-boringssl-static:2.0.27.Final")

    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.1")
    api("org.thymeleaf:thymeleaf:3.0.11.RELEASE")

    kapt("io.micronaut:micronaut-inject-java:1.2.6")
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask> {
    dependsOn(":web:JsJar")
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
