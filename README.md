# Draadloos

A minimal Android app that plays one live radio station: RSG (Radio Sonder Grense). It has one big "Speel"/"Stop" button and keeps playing with the screen off or while you use other apps.

See [docs/SPEC.md](docs/SPEC.md) for the requirements and [docs/PLAN.md](docs/PLAN.md) for progress.

## Build

You need:

- JDK 17 or newer (the one bundled with Android Studio works)
- The Android SDK with platform 37 installed, either through `ANDROID_HOME` or `sdk.dir` in `local.properties`

Build the debug APK:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Every push to `main` and every pull request also builds the APK on GitHub Actions. Open the workflow run and download the `draadloos-debug-apk` artifact (a zip containing the APK).

## Install on a phone (sideload)

**With a USB cable and `adb`:** turn on USB debugging on the phone (Settings → About phone → tap "Build number" seven times, then Developer options → USB debugging), connect it, and run:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Without a computer connection:** copy the APK to the phone (for example by email or Google Drive), open it from the phone's Files app, and allow your file manager to "install unknown apps" when Android asks.

On Android 13 and newer, allow notifications when the app first opens so the radio controls show in the notification shade.
