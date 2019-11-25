plugins {
    //id("kotlin2js")
    id("kotlin-platform-js")
}

dependencies {
    compile(kotlin("stdlib-js"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
    kotlinOptions {
        metaInfo = false
        //noStdlib = false
        //sourceMap = true
        //moduleKind = "umd" //"commonjs" //"amd" //"umd"
        //main = "call"
        outputFile = "$buildDir/classes/kotlin/main/${project.name}.js"
    }
}

task<Copy>("assembleJsLib") {
    configurations.compile.get().resolve().forEach { file ->
        from(zipTree(file.absolutePath)) {
            includeEmptyDirs = false
            include { fileTreeElement ->
                val path = fileTreeElement.path
                (path.endsWith(".js") || path.endsWith(".js.map")) && (path.startsWith("META-INF/resources/") || !path.startsWith("META-INF/"))
            }
        }
    }
    from(tasks.withType<ProcessResources>().map { it.destinationDir })
    into("$buildDir/classes/kotlin/main")

    dependsOn("classes")
}

tasks {
    jar {
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
