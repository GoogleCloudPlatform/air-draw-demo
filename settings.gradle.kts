rootProject.name = "air-draw"

// when running the root stage task, ignore the android subproject
if (startParameter.taskRequests.find { it.args.contains("stage") } == null) {
    include("android", "server")
} else {
    include("server")
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}
