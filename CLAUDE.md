# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This repo holds the plan for a minimal Android app that plays one live radio station, RSG (SABC Afrikaans radio). The full spec is in `docs/SPEC.md`, and it is the source of truth for requirements. No Android project exists yet. When one is scaffolded, update this file with the real build, lint and test commands.

Stack: Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSessionService), minSdk 26.

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

## Deliverables beyond the app

- A README covering the build and sideloading the APK.
- A GitHub Actions workflow that builds a debug APK and uploads it as an artifact.
