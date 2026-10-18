# Delivery plan

This plan delivers the app in [SPEC.md](SPEC.md) in four iterations. Each iteration ends with a debug APK from CI that can be sideloaded and tested on a real phone.

## Decisions

- Package name: `house.ouwl.draadloos`
- App label: Draadloos
- Station artwork: `radio.jpg` (supplied, used for now)
- Widget: Jetpack Glance

## Iteration 1: It plays

Goal: tap a big button, RSG plays, and it keeps playing with the screen off.

- [x] Scaffold the Gradle project (Kotlin, Compose, Media3, minSdk 26, version catalog)
- [ ] GitHub Actions workflow that builds the debug APK and uploads it as an artifact
- [x] MediaSessionService + ExoPlayer playing the redirect stream URL (ExoPlayer follows the redirect)
- [x] Fixed metadata: title "RSG", artist "Radio Sonder Grense", station artwork
- [x] Foreground service type `mediaPlayback`
- [x] Audio focus and becoming-noisy handling (pause on headphone unplug)
- [x] Compose screen: one huge "Speel"/"Stop" button connected to the service, large high-contrast text
- [x] Ask for `POST_NOTIFICATIONS` permission on Android 13+
- [ ] Starter README with build and sideload steps

Done when:

- [ ] The APK plays RSG with the screen off
- [ ] Playback continues while using the phone normally (leaving the app, browsing, other apps)
- [ ] Controls show in the pull-down and on the lock screen
- [ ] Unplugging headphones pauses playback

## Iteration 2: Live behaviour and resilience

Goal: behave like a radio, not a podcast.

- [ ] Pause stops and releases the connection; play reconnects at the live edge
- [ ] Seeking removed, so there's no seek bar anywhere
- [ ] Auto-reconnect with backoff (2s, 4s, 8s, capped at 30s), reset once the stream plays again
- [ ] Switch to the HLS URL if the AAC stream keeps failing
- [ ] Playback resumption (`MediaButtonReceiver` + `onPlaybackResumption`)

Done when:

- [ ] Pause then play is live, with no old buffered audio
- [ ] Turning airplane mode on and off recovers by itself
- [ ] RSG shows in the media area after a reboot

## Iteration 3: Now playing

Goal: show "artist – title" in the notification and on the lock screen.

- [ ] Check that ICY metadata arrives from this stream as `IcyInfo`, and see what the titles look like
- [ ] Parse ICY titles into artist and title and push them to the session without interrupting playback
- [ ] Triton SSE sideband (`_SBM`), built only if the check shows ICY doesn't arrive
- [ ] Fall back to "RSG" when there's no metadata; clear stale metadata on stop

Done when:

- [ ] Song changes appear on the lock screen within a few seconds

## Iteration 4: Widget and polish

Goal: start the radio from the home screen.

- [ ] 2x1 Glance widget with a play/stop toggle that shows the current state
- [ ] Play uses a foreground-service start; stop uses a normal service call or broadcast (Android 12+ rules)
- [ ] Widget updates whenever playback state changes
- [ ] UI checks for the user: font scaling, contrast, TalkBack labels
- [ ] Simple Afrikaans status line (e.g. "Koppel tans…" / "Speel")
- [ ] Finish the README: installing from the CI artifact, allowing unknown sources, adding the widget

Done when:

- [ ] The widget works from a cold start (app not running) on Android 12+ with no crash
