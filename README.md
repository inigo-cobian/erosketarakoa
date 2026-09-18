# Erosketarako

An offline shopping list app. Manage multiple named shopping lists, each with items
that carry a quantity, category, notes, and a bought/unbought state. All data is stored
locally on the device; there is no remote storage, sync, or sign-in.

The project contains the Android client:

- **`android/`** — a Kotlin + Jetpack Compose Android client (local-only, Room).

## Architecture

A single source of truth in the device's local database:

- **Client**: Compose UI + ViewModels read from Room via a repository. All reads and writes
  go through the repository; Room is the only place data lives.
- Each record carries an `updatedAt` timestamp and a soft-delete `isDeleted` tombstone.

## Requirements

- **JDK 21**
- **Android SDK** (API 35, build-tools 35.0.0)
- No global Gradle needed; the project ships a Gradle wrapper (`./gradlew`)

The project uses the JDK via `JAVA_HOME`. On this machine:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

The Android build finds the SDK via `android/local.properties` (`sdk.dir=...`).

## Android

### Build

```bash
cd android
./gradlew assembleDebug
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

### Test

```bash
cd android
./gradlew testDebugUnitTest        # JVM unit tests
./gradlew connectedDebugAndroidTest # instrumented tests (requires a device/emulator)
```

### Run

Install and launch the app on an emulator or device:

```bash
cd android
./gradlew installDebug
# then launch "Erosketarako" from the app launcher
```

### Using the app

1. Create lists and add items.
2. Everything is saved locally on the device.

## Attribution

Item icons are from [OpenMoji](https://openmoji.org), licensed under
[Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)](https://creativecommons.org/licenses/by-sa/4.0/).
The same credit is shown in the app's About screen.
