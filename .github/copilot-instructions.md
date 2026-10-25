# Copilot instructions

<!-- Keep this file in sync with CLAUDE.md. -->

## Project

An offline-first Android expense tracker that will sync with a Google Sheet. It's built as three Gradle modules following Clean Architecture, with Kotlin, Jetpack Compose, Material 3 and Hilt. `applicationId` and app namespace are `com.shashi.expensetracker`. Toolchain: AGP 9.4.1 (built-in Kotlin, so no `kotlin-android` plugin), Kotlin 2.4.21, KSP 2.3.12, Gradle 9.6.0, JDK 17, Java target 11, compileSdk 37, targetSdk 36, minSdk 24 (Compose 1.12 and Lifecycle 2.11 require compileSdk 37).

The architecture spine at `_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md` is the source of truth for design decisions (AD-1 to AD-20) and Stack versions. `.github/copilot-instructions.md` holds the same guidance. Keep the two files in sync.

## Commands

Run all commands from the repo root using the Gradle wrapper:

```bash
./gradlew assembleDebug test                      # build the debug APK and run all JVM tests in every module
./gradlew :domain:test                            # pure JVM tests for :domain
./gradlew :app:testDebugUnitTest                  # :app JVM tests (incl. Robolectric Compose tests)
./gradlew :app:testDebugUnitTest --tests "com.shashi.expensetracker.greeting.GreetingViewModelTest"   # one class
./gradlew :domain:test --tests "com.shashi.expensetracker.domain.greeting.GetGreetingUseCaseTest"     # one class
./gradlew :app:lintDebug                          # Android lint
./gradlew :domain:dependencies --configuration runtimeClasspath   # confirm :domain has no Android deps
```

- **Debug signing:** debug builds use AGP's default debug keystore (`~/.android/debug.keystore`, created on the first debug build). No keystore lives in the repo.
- **Secrets:** there is no Secrets Gradle plugin and no `.env`. Nothing secret is committed.
- **Release builds:** R8 minification and resource shrinking are on. Signing reads `KEYSTORE_PATH` (absolute, or relative to the repo root), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` from the environment. If any is missing, `assembleRelease` produces an unsigned APK and logs a warning; it never falls back to the debug key. Keep rules for Moshi and Retrofit models are in `app/proguard-rules.pro`.
- **Manifest:** `allowBackup=false` and `data_extraction_rules.xml` exclude everything from cloud backup and device transfer (the Sheet is the backup). `GET_ACCOUNTS` is declared with `tools:node="remove"` so no dependency can add it.

## Architecture

Dependencies point inward only (AD-1):

- **`:domain`** (pure Kotlin/JVM, `com.shashi.expensetracker.domain.*`): entities, use cases and `*Gateway` / `*Repository` interfaces. Its only dependencies are `javax.inject` and `kotlinx-coroutines-core`. No Android, Room, Retrofit or Google dependency is allowed.
- **`:data`** (Android library, `com.shashi.expensetracker.data.*`): implementations of the domain interfaces plus the Hilt modules that bind them. Depends on `:domain`.
- **`:app`** (Android application, `com.shashi.expensetracker.*`): the `@HiltAndroidApp` `ExpenseTrackerApp`, the `@AndroidEntryPoint` `MainActivity`, Compose screens and `@HiltViewModel`s. Depends on `:domain` and `:data`, but app code imports only `:domain` types; `:data` is there only so Hilt can install its bindings.

Hilt wires every dependency (AD-16) via KSP. Inside each module, code is grouped by feature package.

The current code is a tracer: `greeting` goes from `GetGreetingUseCase` (`:domain`) through `StubGreetingGateway` (`:data`, bound by `GreetingModule`) to `GreetingViewModel` and `GreetingScreen` (`:app`). Story 1.5 replaces it with the Home screen.

## Conventions

- Use cases are `VerbNounUseCase` classes with one public `operator fun invoke`. They return a sealed result (success or a named failure) and never throw to callers.
- Outside systems are behind `*Gateway` interfaces, stored data behind `*Repository` interfaces, both defined in `:domain` and implemented in `:data`.
- Each screen has one ViewModel exposing one immutable `<Screen>UiState` as a `StateFlow`. ViewModels depend only on `:domain` use cases and turn failures into `UiState` messages.
- Interactive and key UI elements use `testTag` (for example `greeting_text`). Keep existing tags and add tags to new user-critical actions.
- No `.kt` file may exceed 500 lines (AD-17).
- Dependencies and plugins go through `gradle/libs.versions.toml` aliases. Don't hard-code versions in module build files. The catalog already holds the Stack libraries later epics need.
- Source lives in `src/main/kotlin` and `src/test/kotlin`.
