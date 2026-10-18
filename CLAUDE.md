# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

A minimal Android app (Draadloos, package `house.ouwl.draadloos`) that plays one live radio station, RSG (SABC Afrikaans radio). `docs/SPEC.md` is the source of truth for requirements. `docs/PLAN.md` tracks delivery in iterations: tick its checkbox in the same commit that completes an item, and tick "Done when" boxes only after testing on a real phone.

Stack: Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSessionService), minSdk 26, targetSdk 36, compileSdk 37. AGP 9 uses its built-in Kotlin support, so there is no separate `kotlin-android` plugin. Versions live in `gradle/libs.versions.toml`.

## Commands

```sh
./gradlew assembleDebug   # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew lint
./gradlew testDebugUnitTest koverVerifyDebug   # all tests + coverage gate (what CI's `test` check runs)
./gradlew testDebugUnitTest --tests "house.ouwl.draadloos.playback.RadioControlsTest"   # one class
./gradlew koverHtmlReportDebug   # report at app/build/reports/kover/htmlDebug/index.html
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run `./gradlew` as the entire Bash command, with no `cd`, `&&` or env prefix. It is excluded from the Claude Code sandbox only in that form, and `JAVA_HOME` comes from the local settings. CI (`.github/workflows/build.yml`) runs a `build` job (debug APK artifact) and a `test` job (tests + coverage gate) on pushes to `main` and on PRs.

## Tests

- Tests are JVM unit tests under `app/src/test`, run with Robolectric (`@RunWith(AndroidJUnit4::class)`), with one test class per source file in the same package. There are no instrumented (`androidTest`) tests.
- New code comes with tests. `koverVerify` enforces minimum line and branch coverage (set in `app/build.gradle.kts`). The minimums only go up; never lower them to get a PR through.
- `playback/FakeRadioPlayer` is a `SimpleBasePlayer` whose state tests set directly. Use it for anything that takes a `Player`.
- `MainActivityTest` binds a real in-process `PlaybackService` with `ShadowApplication.setComponentNameAndServiceForBindService`. Tests that don't need playback must call `declareComponentUnbindable` instead, because Robolectric's default fake binding crashes Media3.
- Use the `v2` Compose test rules (`androidx.compose.ui.test.junit4.v2`); the old ones are deprecated.

## Layout

- `playback/PlaybackService.kt` owns the ExoPlayer and the MediaSession. Everything else talks to it through a `MediaController`.
- `playback/Station.kt` holds the stream URL and the station's `MediaItem` and metadata.
- `MainActivity.kt` connects a `MediaController` in `onStart` and releases it in `onStop`. `ui/RadioScreen.kt` is the single screen.

## Constraints that span components

These rules come from the spec. They are easy to break when you only look at one file.

- **Stream URL**: use the Triton/StreamTheWorld redirect `https://playerservices.streamtheworld.com/api/livestream-redirect/RSGAAC.aac` and let ExoPlayer follow it. Never hardcode the numbered server host it redirects to. The HLS fallback is the same path with `.m3u8`.
- **Live semantics**: there is no seek bar. Pause means stop and release the connection. Resume reconnects to the live edge, not to buffered audio. This affects the player service, the Compose UI and the widget.
- **Single source of playback state**: the MediaSessionService owns the ExoPlayer. The notification, lock screen, in-app UI and home-screen widget all show the session state. Playback resumption (MediaButtonReceiver + `onPlaybackResumption`) must keep RSG in the system media area after a reboot.
- **Widget + foreground-service rules (Android 12+)**: call `startForegroundService` from a widget click only when starting playback. Send stop/toggle-off a different way.
- **Metadata**: the default title is "RSG" and the default artist is "Radio Sonder Grense". Get "now playing" from ICY metadata first (`IcyInfo` via `Player.Listener.onMetadata`). Use Triton's SSE sideband (`_SBM` suffix) only if ICY doesn't come through. When no metadata is available, fall back to "RSG".
- **Resilience**: handle audio focus and becoming-noisy (pause on headphone unplug), and auto-reconnect with backoff on network errors.
- **Manifest**: the service uses foreground service type `mediaPlayback`. Request `POST_NOTIFICATIONS` at runtime on API 33+.

## UI audience

The UI is for an older, non-technical user. It has one very large play/stop button, large text and high contrast. Labels are in Afrikaans: "Speel" / "Stop". Keep it that simple and don't add settings, lists or extra screens.

`docs/DESIGN.md` is the visual spec for the main screen (four states) and the 1x1 and 4x1 widgets: colour tokens, Atkinson Hyperlegible (bundled in `res/font/`, licence in `licenses/`), sizes and state mapping. Reference screenshots are in `design/`. Where a screenshot and DESIGN.md disagree, DESIGN.md wins.

## Deliverables beyond the app

- A README covering the build and sideloading the APK.
- A GitHub Actions workflow that builds a debug APK and uploads it as an artifact.
