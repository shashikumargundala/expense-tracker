# Expense Tracker

An offline-first Android expense tracker that syncs with your own Google Sheet. It's built with Kotlin, Jetpack Compose, Material 3 and Hilt, in three Gradle modules: `:domain`, `:data` and `:app`.

## Run locally

**Prerequisites:**
- A recent [Android Studio](https://developer.android.com/studio) that supports AGP 9.4.1.
- JDK 17 or newer to run Gradle.
- Unit tests run on a Java 21 toolchain, which Gradle downloads if it's missing.

1. Open the project directory in Android Studio, or run commands from the repo root.
2. Build and test: `./gradlew assembleDebug test`. The first build downloads Gradle 9.6.0, the dependencies, Android SDK platform 37 and, if needed, JDK 21, so it can take a while.
3. Install on a connected device or emulator: `./gradlew :app:installDebug`

Debug builds are signed with your default Android debug key (`~/.android/debug.keystore`). No `.env` file or keystore in the repo is needed.

See `CLAUDE.md` for the conventions, and the architecture spine in `_bmad-output/initiative-expense-tracker/architecture-expense-tracker/` for the design decisions.
