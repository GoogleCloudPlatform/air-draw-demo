/*
When using the android artifact, the android project fails with:
Execution failed for task ':android:processDebugResources'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.Workers$ActionFacade
   > AAPT2 aapt2-4.1.0-6503028-linux Daemon #0: Unexpected error during link, attempting to stop daemon.
     This should not happen under normal circumstances, please file an issue if it does.
 */

plugins {
    id("com.android.application")
    kotlin("multiplatform")
}

android {
    compileSdkVersion(30)
}

kotlin {
    jvm()

    android()

    js {
        browser()
    }
}
