# Droneside

The [FPVTrackside](https://fpvtrackside.com) Companion App — a native Android app for FPV drone racing enthusiasts.

## Overview

Droneside gives you mobile access to FPV drone racing data from the [FPVTrackside](https://fpvtrackside.com) platform: events, rounds, race results, lap-by-lap timing and pilot performance.

## Features

- **Home club dashboard** — star a club to see its upcoming and recent events on the Home tab
- **Event discovery** — upcoming, recent (last 30 days) and previous events
- **Club directory** — searchable list of clubs with location and event history
- **Event details** — rounds, registered pilots and a fastest-lap leaderboard
- **Race results** — podium, start/end timing, holeshot/best/average lap stats, lap sparklines and full lap tables
- **Pilot profiles** — per-round results, total points and a sparkline of every lap flown at the event

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3), Navigation Compose
- **Networking**: Retrofit + OkHttp + Gson
- **Images**: Coil
- **Icons**: Font Awesome 4.7 (bundled font)
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 35

## Project Structure

```
app/src/main/kotlin/com/jamiecarl/droneside/
├── MainActivity.kt
├── data/
│   ├── model/          # API data classes + image URL resolution
│   ├── network/        # Retrofit API interface and client
│   ├── repository/     # Repository + per-event in-memory cache
│   └── storage/        # Home club preference
├── ui/
│   ├── components/     # Event card, lap sparkline, lap table, stat tiles, icons
│   ├── navigation/     # Routes, NavHost, top bar and bottom tabs
│   ├── screens/        # One package per screen (screen + ViewModel)
│   └── theme/          # Dark `ds-*` palette and theme
└── util/               # Time/date formatting, result sorting, lap matching
```

## API Integration

The app uses the FPVTrackside public API at `https://fpvtrackside.com/api/public/` (events, clubs, channels, rounds, pilots, races, race summaries and raw lap data). Relative image paths are resolved against `https://fpvtrackside.com`.

## Development

### Prerequisites
- JDK 17+
- Android SDK with platform 35 (set `sdk.dir` in `local.properties` or `ANDROID_HOME`)

### Build and run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

Debug builds use the application ID `com.jamiecarl.droneside.debug`, so they install alongside the release app.

### Release build
```bash
DRONESIDE_KEYSTORE_PASSWORD=... DRONESIDE_KEY_ALIAS=... DRONESIDE_KEY_PASSWORD=... ./gradlew assembleRelease
```

The release build is signed with `release-key.keystore` in the project root (not committed).

## Version History

### v1.0.7
- Rewritten as a native Kotlin/Jetpack Compose Android app (previously NativeScript + Vue)

### v1.0.6 and earlier
- NativeScript Vue app (see git history)

## License

Private project for FPV drone racing community.

## Contact

For questions or support, please contact [dev@funkynerd.com](mailto:dev@funkynerd.com).
