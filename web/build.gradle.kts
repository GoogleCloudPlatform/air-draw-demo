plugins {
    kotlin("js")
}

kotlin {
    js {
        browser()
        /*
        {
            dceTask {
                dceOptions.devMode = true
            }

        }
         */
        binaries.executable()
    }
    sourceSets["main"].dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(project(":common"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.9")
        implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
    }
}

/*
tasks.named<Jar>("jsJar") {
    from("build/distributions")
    into("META-INF/resources")
    dependsOn("browserDevelopmentWebpack")
}
 */
