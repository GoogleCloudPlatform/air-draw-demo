plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen")
    id("com.google.cloud.tools.jib") version "2.6.0"
}

dependencies {
    implementation(project(":common"))
    //implementation(project(":web"))
    //runtimeOnly(files("../web/build/libs/web.jar"))

    implementation("com.github.haifengl:smile-plot:1.5.2")
    implementation("com.github.haifengl:smile-interpolation:1.5.3")
    implementation("com.github.haifengl:smile-netlib:1.5.3")

    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime:2.1.1")
    implementation("io.micronaut:micronaut-runtime:2.1.2")
    implementation("io.micronaut:micronaut-http-server-netty:2.1.2")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.google.cloud:google-cloud-vision:1.100.6")
    implementation("com.google.cloud:google-cloud-pubsub:1.108.7")
    implementation("com.google.cloud:google-cloud-core:1.93.10")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.20.Final")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    kapt("io.micronaut:micronaut-inject-java:2.1.2")
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
    mainClass.set("com.jamesward.airdraw.WebAppKt")
}

allOpen {
    annotation("io.micronaut.aop.Around")
}

kapt {
    arguments {
        arg("micronaut.processing.incremental", true)
        arg("micronaut.processing.annotations", "com.jamesward.airdraw.*")
    }
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-XX:TieredStopAtLevel=1", "-Dcom.sun.management.jmxremote")

    if (gradle.startParameter.isContinuous) {
        systemProperties = mapOf(
                "micronaut.io.watch.restart" to "true",
                "micronaut.io.watch.enabled" to "true",
                "micronaut.io.watch.paths" to "src/main"
        )
    }
}

/*
tasks {
    classes {
        dependsOn(":web:jsJar")
    }
}
 */
