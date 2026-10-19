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

## Release build

A release APK has to be signed with the project's own key. The key lives in a keystore (`draadloos-release.jks`) that is git-ignored and must never be committed. Back it up together with its passwords: without them, installed copies can't be updated.

**Locally.** Put the keystore in the repo root (or point `DRAADLOOS_KEYSTORE` at it) and add the passwords to `~/.gradle/gradle.properties`, outside the repo:

```properties
DRAADLOOS_KEYSTORE_PASSWORD=...
DRAADLOOS_KEY_PASSWORD=...
```

`DRAADLOOS_KEY_ALIAS` defaults to `draadloos`, and `DRAADLOOS_KEY_PASSWORD` defaults to the keystore password. Then run:

```sh
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. Without the password, Gradle still builds, but the APK is unsigned and can't be installed. Debug builds are signed with a different key, so uninstall the debug app before installing a release APK over it.

**Publishing a version.** Releases are built by GitHub Actions:

1. Raise `versionCode` and set `versionName` (for example `1.0.0`) in `app/build.gradle.kts`, and merge that to `main`.
2. Tag the commit and push the tag:

   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```

The `Release` workflow fails if the tag doesn't match `versionName`. Otherwise it builds the signed APK and attaches `draadloos-1.0.0.apk` to a GitHub Release.

It needs these repository secrets (Settings → Secrets and variables → Actions): `DRAADLOOS_KEYSTORE_BASE64` (from `base64 -i draadloos-release.jks | pbcopy`), `DRAADLOOS_KEYSTORE_PASSWORD`, `DRAADLOOS_KEY_ALIAS` and `DRAADLOOS_KEY_PASSWORD`.

## Tests

```sh
./gradlew testDebugUnitTest koverVerifyDebug
```

The tests run on the JVM with Robolectric, so no phone or emulator is needed. `koverVerifyDebug` fails if line or branch coverage drops below the minimums in `app/build.gradle.kts`. For a coverage report, run `./gradlew koverHtmlReportDebug` and open `app/build/reports/kover/htmlDebug/index.html`.

CI runs the same command as the `test` check on every pull request.

## Install on a phone (sideload)

You need an Android 8.0 (API 26) phone or newer.

### 1. Get the APK

For a released version, download `draadloos-<version>.apk` from the repository's **Releases** page and go to step 2. Otherwise build it yourself (see [Build](#build)), or take a debug build from CI, which needs no developer tools:

1. Open the repository on GitHub, go to **Actions**, and open the latest **CI** run on `main` (or on the pull request you want to try).
2. Under **Artifacts** at the bottom of the run page, download **`draadloos-debug-apk`**. It is a zip file containing `app-debug.apk`. You need to be signed in to GitHub to download artifacts.
3. Unzip it. On the phone, a file manager or the Files app can open the zip.

### 2. Install it

**Without a computer connection:** copy `app-debug.apk` to the phone (for example by email or Google Drive) and open it from the phone's Files app. Android blocks apps from outside the Play Store until you allow it for the app you opened the file with, so:

- When Android says "For your security, your phone is not allowed to install unknown apps from this source", tap **Settings** and turn on **Allow from this source** for that app (Files, Drive, Chrome and so on).
- Go back and tap **Install**. If Play Protect warns about an unknown developer, choose **Install anyway**.
- You can turn the permission off again afterwards (Settings → Apps → Special app access → Install unknown apps).

**With a USB cable and `adb`:** turn on USB debugging on the phone (Settings → About phone → tap "Build number" seven times, then Developer options → USB debugging), connect it, and run:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Android 13 and newer, allow notifications when the app first opens so the radio controls show in the notification shade and on the lock screen.

## Add the widget

There are two home-screen widgets, a wide one (4×1) and a small one (1×1). Each has one round button that plays or stops RSG and always shows the real state: yellow ▶ when stopped, white ■ when playing or connecting. The wide one also shows what is on air.

1. Touch and hold an empty spot on the home screen and choose **Widgets**.
2. Find **Draadloos** and pick **RSG (breed)** or **RSG (klein)**.
3. Touch and hold the widget, drag it to a spot on the home screen and let go.

Tap the round button to play or stop. Tap anywhere else on the widget to open the app. The widget works even if the app isn't open.
