plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    id("com.google.cloud.tools.jib") version "1.7.0"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile(project(":common"))

    compile("com.github.haifengl:smile-plot:1.5.2")
    compile("com.github.haifengl:smile-interpolation:1.5.3")
    compile("com.github.haifengl:smile-netlib:1.5.3")

    compile("io.micronaut:micronaut-runtime:1.2.5")
    compile("io.micronaut:micronaut-http-client:1.2.5")
    compile("io.micronaut:micronaut-http-server-netty:1.2.5")
    compile("io.micronaut:micronaut-views:1.2.0")
    compile("ch.qos.logback:logback-classic:1.2.3")

    compile("com.google.cloud:google-cloud-vision:1.97.0")
    compile("com.google.cloud:google-cloud-pubsub:1.97.0")
    compile("io.netty:netty-tcnative-boringssl-static:2.0.20.Final")

    runtime("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
    runtime("org.thymeleaf:thymeleaf:3.0.11.RELEASE")

    kapt("io.micronaut:micronaut-inject-java:1.2.5")
    kapt("io.micronaut:micronaut-validation:1.2.5")
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

jib {
    container {
        mainClass = application.mainClassName
    }
}
