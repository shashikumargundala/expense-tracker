# Copilot instructions

## Project overview

This is a single-module Android application (`:app`) named Expense Tracker. It uses Kotlin, Jetpack Compose, Material 3, Java 11 source compatibility, and an Android API 36 compile/target SDK.

The runtime flow is:

- `MainActivity` creates the Room database, repository, and `ExpenseViewModel`, then chooses between `LoginScreen` and `MainAppContent`.
- `ExpenseViewModel` owns UI state and user actions. It exposes Room `Flow`s as lifecycle-aware `StateFlow`s and performs repository work in `viewModelScope`.
- `ExpenseRepository` is the data boundary. It reads and writes `ExpenseEntity` and `CategoryEntity` through Room, stores session/sync settings in `SharedPreferences`, bootstraps default categories, and optionally synchronizes rows with Google Sheets.
- `AppDatabase`, `ExpenseDao`, and `CategoryDao` provide the local offline-first store. Expenses are marked with `synced`; unsynced local rows are pushed when Sheets is configured.
- `GoogleSheetsService` and `RetrofitClient` wrap the Sheets REST API. OAuth authorization returns to `MainActivity` through the `com.example.expensetracker://oauth2callback` deep link.
- Most UI composables currently live in `app/src/main/java/com/example/MainActivity.kt`; theme definitions are under `com.example.ui.theme`.

Keep new persistence and synchronization behavior in the data/repository layer rather than directly in composables. Preserve the offline-first behavior: local database writes should succeed independently of a failed Sheets request.

## Build, test, and lint

Run commands from the repository root with the Gradle wrapper:

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Run all local JVM unit tests
./gradlew :app:testDebugUnitTest

# Run one test class
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"

# Run one test method
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest.addition_isCorrect"

# Run Robolectric/Compose screenshot tests individually
./gradlew :app:testDebugUnitTest --tests "com.example.GreetingScreenshotTest"

# Run Android instrumentation tests on a connected device or emulator
./gradlew :app:connectedDebugAndroidTest

# Run Android lint
./gradlew :app:lintDebug
```

Local JVM tests are in `app/src/test`; device tests are in `app/src/androidTest`. `GreetingScreenshotTest` uses Robolectric, Compose test rules, and Roborazzi and writes its image to `app/src/test/screenshots/greeting.png`.

For local setup, Android Studio is the expected environment. The Secrets Gradle Plugin reads `.env` and falls back to `.env.example`; create `.env` with the required `GEMINI_API_KEY` when following the README setup. Do not commit `.env`, OAuth credentials, tokens, or keystores. The debug build is configured to use the repository's `debug.keystore`; the README notes removing that signing override when importing/running the generated project if the keystore is unavailable.

## Repository-specific conventions

- Use Room entities/DAOs for expense and category data. `CategoryEntity.name` is the primary key, so category names must be treated as unique.
- Expose observable data from DAOs as `Flow<List<...>>`; convert it to UI state in `ExpenseViewModel` with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
- Keep blocking database and network work off the main thread. Repository methods use `withContext(Dispatchers.IO)` and callers launch them from `viewModelScope`.
- New expenses start with `synced = false`; only mark them synced after a successful Sheets write. Sheets failures are reported through the existing `syncMessage`/logging flow while retaining local data.
- Persist session configuration through the repository's `SharedPreferences` properties rather than reading preferences directly from composables or the activity.
- OAuth callback handling belongs in `MainActivity.handleIntent`; keep the existing custom scheme and host when changing authentication.
- Compose screens use `testTag` values for interaction tests. Preserve existing tags when changing controls, and add tags for new user-critical actions.
- UI actions generally mutate `ExpenseViewModel` state and let Room flows recompose the screen. Avoid duplicating database state in composables.
- Budget analytics are derived from the expense flow: category spend is grouped by category name, and alerts are emitted at 80% (`WARNING`) and 100% (`CRITICAL`) of a positive category budget.
- Dependencies are managed through `gradle/libs.versions.toml` and referenced with version-catalog aliases in `app/build.gradle.kts`; add or update aliases there instead of hard-coding versions in module build logic.
