# Project spec: RSG radio app

Build a minimal Android app (Kotlin, Jetpack Compose, minSdk 26, Media3) that plays one live radio station: RSG (SABC Afrikaans radio).

## Stream

https://playerservices.streamtheworld.com/api/livestream-redirect/RSGAAC.aac

(Triton/StreamTheWorld redirect → HE-AAC v2 48kbps. Let ExoPlayer follow the redirect; don't hardcode the numbered server host. HLS fallback: `.../RSGAAC.m3u8`)

## Requirements

- MediaSessionService + ExoPlayer, so playback shows in the Android pull-down media controls and lock screen, and keeps playing with the screen off. Title "RSG", artist "Radio Sonder Grense", station artwork.
- Live stream behaviour: no seek bar; pause = stop and release the connection; resume = reconnect to live (not to buffered audio).
- Playback resumption (MediaButtonReceiver + onPlaybackResumption) so RSG appears in the media area after reboot.
- Audio focus + becoming-noisy handling (pause on headphone unplug).
- Auto-reconnect with backoff on network errors.
- UI built for an older, non-technical user: one huge play/stop button, large text, high contrast, Afrikaans labels ("Speel" / "Stop").
- A 1x1 or 2x1 home-screen widget with a play/stop toggle that reflects current state. Mind Android 12+ foreground-service start rules from widget clicks: only use startForegroundService when starting playback.
- Foreground service type mediaPlayback, POST_NOTIFICATIONS permission request on 13+.
- Show "now playing" (artist – title) in the notification and lock screen. Try ICY metadata first (the mount has Shoutcast v1 metadata enabled; ExoPlayer exposes it as IcyInfo via Player.Listener.onMetadata). Fall back to Triton's SSE sideband (_SBM suffix) only if ICY doesn't come through. Keep "RSG" as the fallback title when no metadata is available.
- README covering the build and sideloading the APK; plus a GitHub Actions workflow that builds a debug APK as an artifact.
