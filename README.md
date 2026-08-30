# UltimateTracker

UltimateTracker is a personal movie, TV series, and anime tracker built with Kotlin and Jetpack Compose.

## Features

- Four watch statuses with quick status changes directly from collection cards.
- Built-in and custom media types.
- Genre and keyword tags with combined filtering.
- Local persistence with Room.
- English and Russian in-app language selection; English is the default.
- TMDB-powered catalog search, title suggestions, and automatic cover lookup.
- System Photo Picker for choosing a local cover.

## TMDB setup

Online discovery uses the TMDB API. Create an API Read Access Token in your TMDB account, then add it to your untracked `local.properties` file:

```properties
TMDB_READ_ACCESS_TOKEN=your_token_here
```

Never commit `local.properties` or your token. The rest of the app remains functional when the token is missing.

This product uses the TMDB API but is not endorsed or certified by TMDB.

## Run

1. Open this folder in a current Android Studio version.
2. Wait for Gradle Sync and install Android SDK 36 if prompted.
3. Create an emulator or connect a device running Android 6.0 (API 23) or newer.
4. Run the `app` configuration.

Build and test from the Android Studio terminal:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

The project has one `app` module and uses a lightweight MVVM architecture. Compose screens send events to a ViewModel, the ViewModel exposes `StateFlow`, the repository coordinates Room, and Room stores the collection in a local SQLite database. Online search is isolated in a small TMDB client and does not affect offline storage.
