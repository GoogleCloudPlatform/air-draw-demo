# Air Draw

[![Run on Google Cloud](https://deploy.cloud.run/button.png)](https://deploy.cloud.run)

## Local Dev

Run the server:
```
./gradlew -t :server:run
```

Visit: [http://localhost:8080](http://localhost:8080)

Run the client:

1. [Download Android Command Line Tools:](https://developer.android.com/studio)

1. [Get a Firebase google-services.json](https://support.google.com/firebase/answer/7015592?hl=en) and put it in the `android` directory

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

1. For a physical device, [setup adb](https://developer.android.com/studio/run/device)

1. Run on a device using an external server:
    ```
    ./gradlew android:installDebug -PdrawUrl=YOUR_DRAW_SERVER_URL
    ```

1. Or to run from Android Studio / IntelliJ, create a `gradle.properties` file in your root project directory containing:
    ```
   drawUrl=YOUR_DRAW_SERVER_URL
    ```

   And setup the activity to first run *Gradle-aware Make* with a task of `:android:assembleDebug`

Use GCP for Pub/Sub & Vision API:

1. Go to: https://console.cloud.google.com/apis/library/vision.googleapis.com

1. Enable the Vision API

1. Click "Create Credentials"

1. Select "Cloud Vision API" at the API you are using

1. Select "No, I’m not using them"

1. Click "What Credentials do I need"

1. Give the Service Account a name

1. Select a role, like Project Editor

1. Leave the JSON option selected and press "Continue"

1. A JSON file will be downloaded to your machine

1. Go to: https://console.cloud.google.com/cloudpubsub/topicList

1. Create a topic named `air-draw`

1. Create a subscription named `air-draw`

1. Select "Never Expire"

1. Press "Create"

1. Run the app locally connecting to Pub/Sub and the Vision API:
    ```
    GOOGLE_APPLICATION_CREDENTIALS=YOUR_CREDS.json ./gradlew -t run
    ```

Build and Deploy manually:

```
gcloud config set run/region us-central1
gcloud services enable run.googleapis.com
gcloud builds submit --tag=gcr.io/$(gcloud config get-value project)/air-draw
gcloud beta run deploy air-draw --image gcr.io/$(gcloud config get-value project)/air-draw --allow-unauthenticated --memory=512Mi
```
