# RSG radio app: UI design spec

This goes with the [project spec](SPEC.md). It covers the main screen (4 states) and the home-screen widgets.
Reference screenshots are in [`design/`](../design/) (one per artboard). Where a screenshot and this file disagree, this file wins.

Units: the mockups are drawn on a 390 × 844 phone, so **1 mockup px = 1 dp**. Text sizes are **sp**.

---

## 1. Design tokens

### Colours

| Token | Hex | Use |
|---|---|---|
| `Background` | `#000000` | Screen and widget background |
| `OnBackground` | `#FFFFFF` | Primary text |
| `TextMuted` | `#D4D4D4` | Secondary status text ("Gestop", retry countdown) |
| `TextSubtle` | `#C8C8C8` | Small labels ("Nou op die lug") |
| `Accent` | `#F7D046` | Speel button, song title, warning panel, connecting outline, widget "playing" outline |
| `StopButton` | `#FFFFFF` | Stop button background |
| `OnButton` | `#000000` | Text and icons on Speel/Stop buttons and on the warning panel |
| `Live` | `#FF5A4E` | "Regstreeks" (live) dot |
| `Divider` | `#3A3A3A` | Now-playing card border |
| `PillBorder` | `#4A4A4A` | Stopped status pill border, spinner track |
| `PillFill` | `#1F1F1F` | "Regstreeks" pill background |
| `IdleIcon` | `#A8A8A8` | Small stop square in the "Gestop" pill |

Dark only. Don't follow the system light theme and don't use Material You dynamic colour. High contrast is the point of this design.

### Typography

- Typeface: **Atkinson Hyperlegible** (Regular 400, Bold 700). It was designed for low-vision readers and is under the SIL Open Font License. Bundle it in `res/font/` rather than downloading it at runtime.
- Text that adapts its size (station name, artist and title) scales down to fit. Everything else uses sp and must still work at a **1.3× system font scale**: no clipping, no overlap.

| Style | Size | Weight | Used for |
|---|---|---|---|
| `ButtonLabel` | 64sp | Bold | "Speel" / "Stop" |
| `StationLarge` | 64sp (stopped / connecting), 56sp (playing / reconnecting) | Bold | "RSG" |
| `StationSub` | 24sp | Regular | "Radio Sonder Grense" |
| `Status` | 22sp | Regular (Bold for "Regstreeks") | Status pill text |
| `NowPlayingArtist` | 28sp | Bold | Artist |
| `NowPlayingTitle` | 24sp | Regular, `Accent` colour | Song title |
| `Label` | 18sp | Regular | "Nou op die lug" |
| `WarningTitle` | 26sp | Bold | "Verbinding verloor" |
| `WarningBody` | 20sp | Regular | Warning body, retry countdown |

### Shape and spacing

- Screen padding: 24dp left and right; 72dp top (56dp in the playing / reconnecting states); 40dp bottom.
- Main button: full width, **200dp tall**, corner radius 36dp.
- Station art: 168dp square with 32dp corners (stopped / connecting); 132dp square with 28dp corners (playing / reconnecting). White background behind the art.
- Status pill: fully rounded, padding 10dp vertical × 20dp horizontal.
- Now-playing card and warning panel: corner radius 24dp, padding 20–22dp.

---

## 2. Main screen

### Layout (top to bottom)

```
┌──────────────────────────────┐
│                              │
│   ┌──────────────┐           │  ← vertically centred in the space above the button
│   │ station art  │           │
│   └──────────────┘           │
│            RSG               │
│    Radio Sonder Grense       │
│      ( status pill )         │
│   [ now playing card ]       │  ← playing only
│   [ warning panel ]          │  ← reconnecting only
│                              │
│ ┌──────────────────────────┐ │
│ │       ▶  Speel           │ │  ← 200dp, pinned to the bottom (thumb reach)
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

- Portrait only.
- One `Column`: a content area with `weight(1f)`, centred, then the button.
- The button is the ONLY interactive element. There are no menus, settings, volume slider or seek bar.

### State mapping

The UI listens to the `MediaController` (`Player.Listener`) plus the service's own reconnect state.

| UI state | When | Status pill | Extra content | Button |
|---|---|---|---|---|
| **Gestop** (stopped) | Not playing, no reconnect pending (`STATE_IDLE` / `!playWhenReady`) | Grey square + "Gestop", `PillBorder` outline | — | **Speel**, `Accent` background, black ▶ icon |
| **Koppel tans** (connecting) | `playWhenReady && STATE_BUFFERING` | Spinner + "Koppel aan RSG…", `Accent` outline | — | **Stop**, white background, black ■ icon |
| **Speel** (playing) | `isPlaying == true` | Pulsing `Live` dot + **"Regstreeks"**, `PillFill` background | Now-playing card | **Stop** |
| **Verbinding verloor** (connection lost) | Network error, auto-reconnect backoff in progress | Replaced by the warning panel; below it a spinner + "Probeer weer oor N sekondes…" | Warning panel | **Stop**. Cancels the retry and goes to Gestop |

Rules:

- The button toggles between exactly two labels. If the user has asked for audio (playing, connecting or reconnecting), it shows **Stop**. Otherwise it shows **Speel**.
- Speel and Stop differ in colour (yellow / white), icon (▶ / ■) and word all at once. Never rely on colour alone.
- Until the app has connected to the playback service (a moment after launch), the button is dimmed (`Accent` at 40%) and does nothing, so it doesn't look pressable when it isn't.
- Pressing Stop stops and releases the stream (per the project spec), and the UI goes straight to Gestop.
- Reconnecting: dim the station art to 55% opacity. Count the retry down live, once a second.
- After buffering, briefly show Koppel tans (debounce about 300ms so it doesn't flicker).

### Now-playing card (playing state)

- 2dp `Divider` border, centred text, three lines:
  - "Nou op die lug" (`Label`, `TextSubtle`)
  - Artist (`NowPlayingArtist`, white, at most 2 lines, ellipsis)
  - Title (`NowPlayingTitle`, `Accent`, at most 2 lines, ellipsis)
- Parse the ICY `StreamTitle` as "Artist - Title". If there's no ` - `, show the whole string as the title and hide the artist line.
- **No metadata** (talk shows, news): hide the card entirely rather than show "RSG" twice. The notification and lock screen still fall back to the title "RSG", as the project spec says.

### Warning panel (reconnecting)

- `Accent` background with black text: a "wifi off" icon (36dp) on the left.
  - "Verbinding verloor" (`WarningTitle`)
  - "Moenie bekommer nie, ons probeer self weer." (`WarningBody`)
- Below the panel: the spinner + "Probeer weer oor N sekondes…" in `TextMuted`.

### Motion

- Spinner: rotates once a second.
- Live dot: fades its opacity 1 → 0.35 → 1 over 1.6s.
- Moving between the stopped / connecting layout and the playing / connection-lost layout eases the station art (size and corner radius), the "RSG" text size and the top padding over 200ms, instead of jumping.
- If the system animator duration scale is 0 (animations turned off), show the spinner and the live dot static, and change sizes at once.

### Accessibility

- Button content description: "Speel RSG" / "Stop RSG". Announce state changes (`liveRegion = Polite` on the status pill).
- Touch target: the whole 200dp button. Give it haptic feedback on press.
- Contrast: every text/background pair above passes WCAG AA (most pass AAA).

---

## 3. Home-screen widgets

Two sizes, both black with 28dp corners (or the system widget radius on Android 12+). They're shown in the mockup on a dark wallpaper.

### 4×1 widget

```
┌───────────────────────────────────────┐
│ [art]  ● RSG                    ( ■ ) │
│        Kunstenaar – Liedjie           │
└───────────────────────────────────────┘
```

- Left: station art, 56dp square with 14dp corners, white background.
- Middle (fills the remaining width, `min-width 0`):
  - "RSG", 26sp bold. A 10dp `Live` dot in front of it while playing.
  - Below it, 17sp, one line, ellipsis:
    - Stopped: "Gestop" in `TextMuted`.
    - Playing: "Artist – Title" in `Accent`. With no metadata, show "Regstreeks".
- Right: a circular toggle, 64dp in the mockup (never under 48dp).
  - Stopped: `Accent` circle with a black ▶.
  - Playing / connecting: white circle with a black ■.
- Playing: add a 3dp `Accent` inner border around the whole widget.
- **The text column must never sit under the button.** Give the button a fixed size and let the text truncate.
- The mockup is drawn at 300 × 112. On a device, fit it to the cell size Android gives you (`targetCellWidth=4`, `targetCellHeight=1`; at 300dp wide the mockup is about four launcher cells, so the old "2×1" label was wrong). If the cell height is small, shrink the art and the button together, keeping the button at 48dp or more.

### 1×1 widget

- Centred circular toggle (64dp in the mockup, at least 48dp), same colours and icons as the 4×1.
- Below it: "RSG", 16sp bold. A small 8dp `Live` dot in front while playing.
- Playing: the 3dp `Accent` inner border.

### Behaviour

- Tapping the toggle sends play/stop to the service. Use `startForegroundService` only when starting playback (Android 12+ rules, as in the project spec).
- Tapping anywhere else on the widget opens the app.
- The widget always shows the real state. Update it from the service on every play, stop or reconnect change and every metadata change. While connecting or reconnecting, the toggle shows ■ (the user can always stop).
- Content descriptions: "Speel RSG" / "Stop RSG".

---

## 4. Not covered by this design

- **Notification and lock screen:** use the standard Media3 media notification (title, artist, station art from the project spec). No custom layout.
- **Notification permission prompt (Android 13+):** the system dialog. If you add a screen that explains it first, use the same black/yellow style with an Afrikaans heading and a 64sp-style yellow button.

## 5. Assets

- `station_art.png`: the radio illustration. The copy in this handoff is cropped from a screenshot at about 240px. Replace it with the original artwork if you have it, ideally as a vector drawable or at 512px+, for the launcher icon, notification art and widget.
- Launcher icon: the radio on the white background, using an adaptive icon foreground and background.
