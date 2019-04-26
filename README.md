# Air Draw

[![Run on Google Cloud](https://storage.googleapis.com/cloudrun/button.png)](https://console.cloud.google.com/cloudshell/editor?shellonly=true&cloudshellboostmode=true&cloudshell_image=gcr.io/cloudrun/button&cloudshell_git_repo=https://github.com/jamesward/hello-micronaut.git)

TODO: Update Git Repo

## Local Dev

Run the server:
```
./gradlew -t server:run
```

Visit: [http://localhost:8080](http://localhost:8080)

Run the client:

1. [Download Android Command Line Tools:](https://developer.android.com/studio)

1. Install the SDK:
    ```
    mkdir android-sdk
    cd android-sdk
    unzip PATH_TO_SDK_ZIP/sdk-tools-linux-VERSION.zip
    tools/bin/sdkmanager --update
    tools/bin/sdkmanager "platforms;android-28" "build-tools;28.0.3" "extras;google;m2repository" "extras;android;m2repository"
    tools/bin/sdkmanager --licenses
    ```

1. Add the following to your ~/.bashrc
    ```
    export ANDROID_SDK_ROOT=PATH_TO_SDK/android-sdk
    ```

1. Source the new profile:
    ```
    source ~/.bashrc
    ```

1. Run the build from this project's dir:
    ```
    ./gradlew android:build
    ```
