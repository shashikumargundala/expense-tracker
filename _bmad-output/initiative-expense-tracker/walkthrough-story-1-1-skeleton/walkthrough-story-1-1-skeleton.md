# Walkthrough: story 1.1, three-module skeleton (PR #2, 0e5eefe)

Current block: 4

Paused 2026-10-25: the user moved on to building the rest of epic 1. Resume at block 4.

- [x] **Block 1: Intent** (done)
- [x] **Block 2: Broad strokes** (done)
- [x] **Block 3: Build and toolchain** (done)
- [ ] **Block 4: Domain use case** (current, paused, not accepted)
- [ ] **Block 5: Data stub and Hilt binding** (unvisited)
- [ ] **Block 6: App entry, ViewModel and screen** (unvisited)
- [ ] **Block 7: Tests** (unvisited)
- [ ] **Block 8: What was deleted** (unvisited)
- [ ] **Block 9: Periphery** (unvisited)

## Block 1: Intent

Source: verbatim from the plan's frozen Intent section ([plan](../epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md)).

> **Problem:** The repo is an AI Studio single-module app (`com.example`) on an old toolchain with no DI. Every later epic needs the three-module Clean Architecture from the spine (AD-1, AD-2, AD-16), so it has to exist before anything else.
>
> **Approach:** Delete the old app code. Stand up `:domain` (pure Kotlin/JVM), `:data` (Android library) and `:app` on the spine's Stack versions with Hilt. A greeting goes from a `:domain` use case through a `:data` stub gateway to a single Compose screen, which proves the wiring end to end. Docs are updated to match.

## Block 2: Broad strokes

The old app is replaced by three modules wired by Hilt. A tracer greeting runs through all three. Open these in order:

1. [settings.gradle.kts:27](../../../settings.gradle.kts#L27): the three modules.
2. [GetGreetingUseCase.kt:8](../../../domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/GetGreetingUseCase.kt#L8): the pure-Kotlin use case returning a sealed result.
3. [GreetingModule.kt:12](../../../data/src/main/kotlin/com/shashi/expensetracker/data/greeting/GreetingModule.kt#L12): `:data` binds the stub gateway to the domain interface.
4. [MainActivity.kt:16](../../../app/src/main/kotlin/com/shashi/expensetracker/MainActivity.kt#L16): the Hilt activity gets the ViewModel and renders the screen.
5. [MainActivityTest.kt:16](../../../app/src/test/kotlin/com/shashi/expensetracker/MainActivityTest.kt#L16): the end-to-end proof, launching through the real Hilt app.

## Block 3: Build and toolchain

Every version is in the catalog at the spine's Stack pins, and each module applies only the plugins it needs. AGP 9's built-in Kotlin means there's no `kotlin-android` plugin. Two deviations: compileSdk is 37 (Compose 1.12 and Lifecycle 2.11 need it), and `:app`'s test JVM runs on Java 21 for Robolectric.

- [gradle/libs.versions.toml:1](../../../gradle/libs.versions.toml#L1): the Stack pins and the supporting libraries.
- [gradle-wrapper.properties](../../../gradle/wrapper/gradle-wrapper.properties): Gradle 9.6.0.
- [domain/build.gradle.kts:4](../../../domain/build.gradle.kts#L4): the `kotlin.jvm` plugin only; its dependencies are just `javax.inject` and coroutines ([line 14](../../../domain/build.gradle.kts#L14)).
- [data/build.gradle.kts:9](../../../data/build.gradle.kts#L9): the Android library with Hilt and KSP, compileSdk 37.
- [app/build.gradle.kts:37](../../../app/build.gradle.kts#L37): depends on `:domain` and on `:data` (for the Hilt bindings only).
- [app/build.gradle.kts:66](../../../app/build.gradle.kts#L66): Java 21 test launcher plus `--add-opens`/`--add-exports`.
- [robolectric.properties](../../../app/src/test/resources/robolectric.properties): tests pinned to SDK 36.

## Block 4: Domain use case

`GetGreetingUseCase` asks the `GreetingGateway` for a name and returns `Success` or `NameUnavailable`. It never throws to callers, except that it rethrows coroutine cancellation. This is the template later use cases follow (`VerbNounUseCase`, one `invoke`, a sealed result).

- [GetGreetingUseCase.kt:8](../../../domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/GetGreetingUseCase.kt#L8): `invoke`; cancellation is rethrown at [line 12](../../../domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/GetGreetingUseCase.kt#L12).
- [GreetingGateway.kt](../../../domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/GreetingGateway.kt): the outside-source interface.
- [GreetingResult.kt](../../../domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/GreetingResult.kt): the sealed result.

## Block 5: Data stub and Hilt binding

`:data` provides a stub gateway that returns a constant name. A Hilt `@Binds` module installs it as the `GreetingGateway` implementation in the singleton component.

- [StubGreetingGateway.kt](../../../data/src/main/kotlin/com/shashi/expensetracker/data/greeting/StubGreetingGateway.kt): returns "Expense Tracker".
- [GreetingModule.kt:10](../../../data/src/main/kotlin/com/shashi/expensetracker/data/greeting/GreetingModule.kt#L10): `@InstallIn(SingletonComponent::class)` with a `@Binds` method.

## Block 6: App entry, ViewModel and screen

The manifest names the `@HiltAndroidApp` application. The `@AndroidEntryPoint` activity gets a `@HiltViewModel` that calls the use case once at init and maps the result into `GreetingUiState`. The screen shows a spinner until the message arrives.

- [AndroidManifest.xml:8](../../../app/src/main/AndroidManifest.xml#L8): the Hilt application and one launcher activity; the only permission is `INTERNET`.
- [ExpenseTrackerApp.kt](../../../app/src/main/kotlin/com/shashi/expensetracker/ExpenseTrackerApp.kt): `@HiltAndroidApp`.
- [MainActivity.kt:20](../../../app/src/main/kotlin/com/shashi/expensetracker/MainActivity.kt#L20): `setContent` collects the state with lifecycle awareness.
- [GreetingViewModel.kt:21](../../../app/src/main/kotlin/com/shashi/expensetracker/greeting/GreetingViewModel.kt#L21): init launch; a failure maps to `FALLBACK_MESSAGE`.
- [GreetingScreen.kt:20](../../../app/src/main/kotlin/com/shashi/expensetracker/greeting/GreetingScreen.kt#L20): the loading and greeting branches, tagged `greeting_loading` and `greeting_text`.
- [GreetingUiState.kt](../../../app/src/main/kotlin/com/shashi/expensetracker/greeting/GreetingUiState.kt): the state and fallback text.

## Block 7: Tests

Each layer is tested on its own with fakes, plus one Robolectric test that launches the real activity through Hilt.

- [GetGreetingUseCaseTest.kt](../../../domain/src/test/kotlin/com/shashi/expensetracker/domain/greeting/GetGreetingUseCaseTest.kt): success, gateway throwing, blank name, cancellation rethrown.
- [GreetingViewModelTest.kt](../../../app/src/test/kotlin/com/shashi/expensetracker/greeting/GreetingViewModelTest.kt): greeting and fallback states.
- [GreetingScreenTest.kt:17](../../../app/src/test/kotlin/com/shashi/expensetracker/greeting/GreetingScreenTest.kt#L17): the stateless screen, run under a plain Application.
- [MainActivityTest.kt:20](../../../app/src/test/kotlin/com/shashi/expensetracker/MainActivityTest.kt#L20): waits for `greeting_text` and expects "Hello, Expense Tracker!".

## Block 8: What was deleted

All of `app/src/main/java/com/example/**`, the old tests and screenshot, the backup-rule template XMLs, `.env.example`, `firebase-applet-config.json`, `metadata.json` and the Secrets plugin. The old code stays at commit `26e51ad`. Kept on purpose: the launcher mipmaps, [finance_app_logo_1779726000241.png](../../../app/src/main/res/drawable/finance_app_logo_1779726000241.png) (used by the launcher foreground), [proguard-rules.pro](../../../app/proguard-rules.pro) and [gradle.properties](../../../gradle.properties).

## Block 9: Periphery

- [CLAUDE.md](../../../CLAUDE.md): rewritten for three modules.
- [copilot-instructions.md](../../../.github/copilot-instructions.md): mirrors CLAUDE.md.
- [README.md](../../../README.md): run instructions and prerequisites.
- [.gitignore](../../../.gitignore): `build/` for every module, plus `_bmad/render/`.
- [Architecture spine, Stack changes](../architecture-expense-tracker/architecture-expense-tracker.md#L212): dated notes for compileSdk 37 and the Robolectric constraint.
- [themes.xml](../../../app/src/main/res/values/themes.xml): style renamed to `Theme.ExpenseTracker`.
- [deferred-work.md](../deferred-work.md): five deferred review items.
