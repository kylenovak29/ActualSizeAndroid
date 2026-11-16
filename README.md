# Actual Size — Android WebView wrapper (Version 4)

This is an **Android Studio source project**, not a precompiled APK. It embeds the **unchanged Version 4 HTML** in `app/src/main/assets/www/index.html`.

## What it does

- Runs the existing photo viewer offline, with its calibration and both measuring lines.
- Lets you choose a photo through Android's system file picker (no storage permissions).
- Keeps calibration preferences in the application's private WebView storage.
- Uses AndroidX `WebViewAssetLoader` to serve the local HTML on a secure HTTPS-like origin.
- Disallows direct file URL access and has **no INTERNET permission**.
- Android Back closes the actual-size or card-calibration overlay before leaving the app.
- Screen stays awake normally; no special background services or tracking.

## Build in Android Studio (recommended)

1. Install [Android Studio](https://developer.android.com/studio) on a computer, if necessary.
2. Unzip `ActualSizeAndroid.zip`. In Android Studio, choose **Open** and select the **ActualSizeAndroid** folder (not the `app` subfolder).
3. Allow Android Studio to download the specified **Gradle 8.13**, **Android Gradle Plugin 8.13.2**, and **Android SDK Platform 36** (if prompted). Use JDK 17 or later.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)** (menu wording may differ between Android Studio releases).
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk` within the extracted project. To send it to your phone, connect via USB and press **Run**, or copy the APK to your Android phone and open it. Android may ask you to authorize installation from the app that opened the APK.

**Gradle wrapper note:** This package contains the Gradle wrapper *settings*, but not the generated Gradle wrapper JAR/scripts because the build environment that created the package did not have Gradle. Android Studio may use its configured Gradle installation to sync. If it insists on a full wrapper, in a terminal with Gradle 8.13 installed run `gradle wrapper --gradle-version 8.13`, then reopen/sync the project. Alternatively, create a new Android Studio **Empty Views Activity** Java project and replace its app module files with this project's files.

### Alternative: GitHub Actions build (no Android Studio needed)

An optional `.github/workflows/build-apk.yml` is included. Upload this project's contents to a GitHub repository, open its **Actions → Build Android APK** workflow, choose **Run workflow**, and download the **actual-size-debug-apk** build artifact after completion. The artifact ZIP contains `app-debug.apk`. This sends only the app source (not any of your photographs) to GitHub for compilation. Requires a GitHub account and appropriate Actions permission. This workflow has not been run in this environment.

### Command-line build (once Gradle 8.13 and Android SDK Platform 36 are installed)

```bash
gradle :app:assembleDebug
```

Or, after generating the wrapper, `./gradlew :app:assembleDebug` (on Windows `gradlew.bat :app:assembleDebug`).

## Editing the HTML

Replace `app/src/main/assets/www/index.html` with the next standalone HTML version, then rebuild. The Android shell shouldn't need changes unless browser/native functionality changes.

## Android settings

- Package/application ID: `org.actualsize.viewer`
- Java source language, minimum Android version: Android 8.0 / API 26
- `compileSdk 36`, `targetSdk 35`
- Java 17 or newer required for compiling
- App icon: included in resources

## Calibration

The WebView may report a different CSS-pixel/device-pixel ratio than Chrome. Verify with the credit-card reference on your **first APK launch**. The calibration stored in the Chrome HTML file is separate from this app's calibration. Changing display scaling settings may require recalibration.

## Known limitations

- The included project has not been compiled or installed in this environment; please treat the first build/phone run as a validation step. Build-time dependencies need to download once on your computer.
- Portrait/landscape rotation retains the WebView so selected photos should not be lost solely to rotation.
- A photo with perspective distortion can only be at a single approximate physical scale unless measured objects are coplanar with the reference.
- Photos are not persisted by the app. Calibration preferences are persisted locally.

## Android implementation reference

- `MainActivity.java` is the only native source file. There is **no JavaScript bridge**.
- The `WebChromeClient.onShowFileChooser` hook implements `<input type=file>`.
- The bundled file is exposed at `https://appassets.androidplatform.net/assets/www/index.html` via `WebViewAssetLoader` (no network required).
