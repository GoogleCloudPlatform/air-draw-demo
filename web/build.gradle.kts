plugins {
    //id("kotlin2js")
    kotlin("js")
    //id("kotlin-platform-js").version("1.3.61")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.6.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.14.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
    kotlinOptions {
        metaInfo = false
        //noStdlib = false
        sourceMap = true
        //moduleKind = "umd" //"commonjs" //"amd" //"umd"
        //main = "call"
        outputFile = "$buildDir/classes/kotlin/main/${project.name}.js"
    }
}

task<Copy>("assembleJsLib") {
    from(provider {
        configurations["runtimeClasspath"].map {
            zipTree(it).matching {
                include { fileTreeElement ->
                    val path = fileTreeElement.path
                    (path.endsWith(".js") || path.endsWith(".js.map")) && (path.startsWith("META-INF/resources/") || !path.startsWith("META-INF/"))
                }
            }
        }
    })

    into("$buildDir/classes/kotlin/main")

    dependsOn("mainClasses")
}

tasks {
    JsJar {
        into("META-INF/resources")
        dependsOn("assembleJsLib")
    }
}

/*
val kotlinPlatformType = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)


configurations {
    create("myConfiguration") {
        attributes {
            attribute(kotlinPlatformType, "jvm")
        }
    }
}

 */

/*
kotlinPlatformType

dependencies.attributesSchema {
    // registers this attribute to the attributes schema
    attribute(myAttribute)
    attribute(myUsage)
}

 */

/*
kotlin {
    targets {
        fromPreset(presets.jvm, 'jvm')
        fromPreset(presets.js, 'js')
    }
}
*/
