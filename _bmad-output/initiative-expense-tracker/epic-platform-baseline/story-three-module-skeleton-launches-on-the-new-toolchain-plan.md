---
title: 'Three-module skeleton launches on the new toolchain'
type: 'refactor'
ticket: '1'
created: '2026-10-24'
status: 'built'
baseline_revision: '004756010beaf50ba71ff78c50681e08e075e364'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'thorough'
review_source: 'pinned'
lenses_ran: ['blind-hunter', 'edge-case-hunter', 'verification-gap', 'intent-alignment']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The repo is an AI Studio single-module app (`com.example`) on an old toolchain with no DI. Every later epic needs the three-module Clean Architecture from the spine (AD-1, AD-2, AD-16), so it has to exist before anything else.

**Approach:** Delete the old app code. Stand up `:domain` (pure Kotlin/JVM), `:data` (Android library) and `:app` on the spine's Stack versions with Hilt. A greeting goes from a `:domain` use case through a `:data` stub gateway to a single Compose screen, which proves the wiring end to end. Docs are updated to match.

## Boundaries & Constraints

**Always:**
- Use the Stack versions from the spine. Every version lives in `gradle/libs.versions.toml`, and the catalog includes the Stack rows later epics need.
- Choose the Gradle wrapper version from AGP 9.4.1's documented minimum.
- Dependencies point inward: `:data` → `:domain` and `:app` → `:domain` + `:data`. `:app` code imports only `:domain` types; `:data` is there only for the Hilt bindings.
- `:domain` depends only on `javax.inject` and coroutines core. It has no Android or Google dependency.
- Use KSP for Hilt. Use AGP 9's built-in Kotlin, so don't apply the `kotlin-android` plugin.
- Use `applicationId` and app namespace `com.shashi.expensetracker`, with Java target 11, minSdk 24, targetSdk 36 and compileSdk 37.
- Decision (2026-10-25): compileSdk is 37, not 36, because Compose BOM 2026.09.00 and Lifecycle 2.11.0 require it; targetSdk stays 36.
- The debug build uses AGP's default debug signing (`~/.android/debug.keystore`).
- Follow the naming rules: the use case is `VerbNounUseCase` with one `operator fun invoke`, the outside source is a `*Gateway` interface in `:domain` with its implementation in `:data`, and screen state is `<Screen>UiState` in a `StateFlow`.
- Every `.kt` file stays at 500 lines or fewer.

**Never:**
- Don't migrate old code, the Room DB or OAuth. Deletion is fine because the old code stays in git at `26e51ad`.
- No Secrets Gradle plugin and no `.env` handling.
- Don't do story 1.2's work: no R8, no release signing, no `allowBackup` changes. The fresh manifest simply leaves out `GET_ACCOUNTS` and `READ_SMS`.
- Don't do the CI or line-check task (1.3), the DESIGN.md theme (1.4), navigation or tabs (1.5), or the Roborazzi plugin or baselines (1.6).
- Don't edit `.idea/` files.
- Decision (2026-10-24): delete `firebase-applet-config.json`, `metadata.json` and the AI Studio banner and link in the README. AI Studio will no longer regenerate or sync this project.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Greeting loads | The stub gateway returns a name | The screen shows the greeting text, and `testTag("greeting_text")` is present | No error expected |
| Gateway fails | A fake gateway throws or returns a failure | The use case returns its named failure result, and the UiState shows a fallback message | Sealed result; nothing is thrown to the UI |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/example/**`, `app/src/test/**`, `app/src/androidTest/**`, `app/src/test/screenshots/greeting.png`: old code. Delete all of it.
- `app/src/main/res/`: keep the launcher mipmaps and drawables and `values/strings.xml` (`app_name`). Delete `finance_app_logo_*.png`, `xml/backup_rules.xml` and `xml/data_extraction_rules.xml`. Rename the `Theme.MyApplication` style to `Theme.ExpenseTracker`.
- `app/src/main/AndroidManifest.xml`: rewrite it. Keep `INTERNET` for later Sheets work. Include the Hilt `Application` and one launcher `MainActivity`, with no OAuth intent filter.
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`: rewrite them. Today these are AGP 9.1.1, Kotlin 2.2.10, Gradle 9.3.1 and the Secrets plugin. Keep the foojay plugin and the repository blocks in `settings.gradle.kts`. Rename `rootProject.name` to `ExpenseTracker`.
- `gradle.properties`: keep it as is.
- `app/proguard-rules.pro`: keep it untouched (story 1.2 owns it).
- `.env.example`, `README.md` steps 4–5, `CLAUDE.md`, `.github/copilot-instructions.md`: these describe the old app and the Secrets setup. Rewrite or delete them.
- `.gitignore`: add `_bmad/render/`.
- Local environment: JDK 17 is on PATH, the Android SDK has platform 36.1 and build-tools 37.0.0. There's no AVD and no attached device, and `~/.android/debug.keystore` doesn't exist yet (AGP creates it on the first debug build).

## Tasks & Acceptance

**Execution:**
- [ ] `gradle/libs.versions.toml`, `gradle/wrapper/*`: replace the catalog with the Stack versions plus JUnit, coroutines, `javax.inject`, activity-compose and the Robolectric and Compose test libraries at current stable versions. Bump the wrapper to the minimum Gradle that AGP 9.4.1 supports.
- [ ] `settings.gradle.kts`, `build.gradle.kts`: include `:domain`, `:data` and `:app`, and declare the plugins with `apply false`.
- [ ] `domain/build.gradle.kts`, `domain/src/main/kotlin/com/shashi/expensetracker/domain/greeting/{GreetingGateway,GetGreetingUseCase,GreetingResult}.kt`: create the JVM module and the use case that returns a sealed result.
- [ ] `domain/src/test/kotlin/.../GetGreetingUseCaseTest.kt`: add JVM tests for both matrix rows, using a fake gateway.
- [ ] `data/build.gradle.kts`, `data/src/main/kotlin/com/shashi/expensetracker/data/greeting/{StubGreetingGateway,GreetingModule}.kt`: create the Android library (namespace `com.shashi.expensetracker.data`) with Hilt, a stub gateway, and a `@Binds` module.
- [ ] `app/build.gradle.kts`, `app/src/main/kotlin/com/shashi/expensetracker/{ExpenseTrackerApp,MainActivity}.kt`, `.../greeting/{GreetingScreen,GreetingViewModel,GreetingUiState}.kt`: create the `@HiltAndroidApp` application, the `@AndroidEntryPoint` activity that hosts `GreetingScreen` in a plain `MaterialTheme`, and a `@HiltViewModel` that calls the use case.
- [ ] `app/src/test/kotlin/.../greeting/{GreetingViewModelTest,GreetingScreenTest}.kt`: add a ViewModel test with a fake gateway and a Robolectric Compose test that finds `greeting_text`.
- [ ] Old files listed in the Code Map: delete them.
- [ ] `CLAUDE.md`, `.github/copilot-instructions.md`, `README.md`, `.gitignore`: describe the three modules, the commands, Hilt, default debug signing and the absence of a Secrets plugin, and keep the two docs in sync.

**Acceptance Criteria:**
- Given a clean checkout and JDK 17, when you run `./gradlew assembleDebug test`, then the build passes without a `debug.keystore` or `.env` in the repo.
- Given `./gradlew :domain:dependencies --configuration runtimeClasspath`, when you read the output, then it lists no `androidx`, `com.android` or `com.google` artifacts.
- Given the debug APK, when you inspect its manifest, then the package is `com.shashi.expensetracker` and there's no `GET_ACCOUNTS` permission.
- Given the debug app installed on a device, when you launch it, then the greeting appears. This is a human check.

## Implementation Notes

- The implementation subagent stalled twice: the first build spent more than 20 minutes downloading the new toolchain. The main session finished the README rewrite and the verification.
- Gradle wrapper set to 9.6.0.
- **compileSdk 37** (user decision, 2026-10-25): Compose BOM 2026.09.00 (Compose 1.12.1) and Lifecycle 2.11.0 fail `checkDebugAarMetadata` below API 37. targetSdk stays 36. AGP installed `platforms/android-37.0` automatically. The spine's Stack row, `CLAUDE.md` and the Copilot instructions are updated to match.
- **Plan error fixed:** `finance_app_logo_1779726000241.png` is kept. `drawable/ic_launcher_foreground.xml` references it, so deleting it breaks resource linking.
- **Robolectric 4.17** has no API 37 image. `app/src/test/resources/robolectric.properties` pins `sdk=36`. Its SDK 36 sandbox needs Java 21, so `app/build.gradle.kts` runs only the `Test` JVM on a Java 21 toolchain, with `--add-opens java.base/java.io` and `--add-exports java.base/jdk.internal.access`. Gradle itself still runs on JDK 17, and bytecode targets 11. Story 1.3's CI must provide JDK 21 or let foojay download it.
- `GreetingScreenTest` uses the non-deprecated `junit4.v2.createComposeRule`.
- Review patches (2026-10-25): added `MainActivityTest` (a Robolectric launch through the real Hilt app) and a cancellation test, put the spine's Stack changes in dated notes, and added README prerequisites. Re-verified: `./gradlew assembleDebug test :app:lintDebug` passes with 9 tests and 0 failures. Lint reports 0 errors and 12 warnings: the template `colors.xml` (1.4 replaces it), icon-format warnings on the existing logo, and `OldTargetApi`.
- First verification: `./gradlew assembleDebug test` passed with 7 tests and 0 failures. The `:domain` runtime classpath is `kotlin-stdlib`, `javax.inject` and `coroutines-core`. `aapt2` reports package `com.shashi.expensetracker`, compileSdk 37, targetSdk 36 and only the `INTERNET` permission.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-10-25, thorough: blind-hunter, edge-case-hunter, verification-gap, intent-alignment)

Verdicts: high 0, medium 4, low 7, false 13, maybe-false 0. Routes: 5 patch entries (rows 1–3 grouped, 4, 7 partly, 10, 11), 5 defer entries (5, 6, 7 partly, 8, 9), 13 rejected (all false; rows 12–13 are also plan edits).

| # | Lens | Finding | Verdict | Route | Evidence / action |
|---|------|---------|---------|-------|-------------------|
| 1 | verification-gap | No test launches `MainActivity` through the real Hilt app | medium | patch | Pre-verified: the tests use a plain Application or hand-built ViewModels. Added `app/src/test/kotlin/com/shashi/expensetracker/MainActivityTest.kt`, which runs on the manifest's `ExpenseTrackerApp` and asserts `greeting_text` = "Hello, Expense Tracker!". It passes. |
| 2 | blind-hunter | No automated Hilt launch check | medium | patch (grouped with 1) | Same root cause as 1. |
| 3 | intent-alignment | "Proves the wiring end to end" is backed only by compilation and a manual check | medium | patch (grouped with 1) | Same root cause as 1. |
| 4 | verification-gap | No test checks that the use case rethrows `CancellationException` | low | patch | Pre-verified. Added `rethrows cancellation instead of returning NameUnavailable` to `GetGreetingUseCaseTest`. It passes. |
| 5 | verification-gap | No test checks that a padded name is trimmed in the greeting | low | defer | Pre-verified. The filed disposition is defer: the tracer package is removed in 1.5 and the stub never pads. |
| 6 | blind-hunter, edge-case-hunter | Firebase API key `AIzaSyCN…` for project `silent-charge-85xj8` is still in git history | medium | defer | Real: `git show 26e51ad:firebase-applet-config.json`. It predates this change. Rotating or restricting it is the owner's job in the Google Cloud console. |
| 7 | blind-hunter, edge-case-hunter, intent-alignment | Docs and the acceptance criterion say JDK 17, but `:app` tests need a Java 21 toolchain | low | patch (README, spine) + defer (CLAUDE.md, Copilot) | Real: `app/build.gradle.kts` uses a Java 21 test launcher, and foojay downloads it automatically, as it did here. README and spine notes updated. The agent-context files are deferred under the triage rule. |
| 8 | blind-hunter | foojay plugin version is hard-coded, which conflicts with the "all versions in the catalog" rule | low | defer | Real but pre-existing (`settings.gradle.kts` at baseline). The settings `plugins {}` block can't use the catalog. Documenting the exception means editing CLAUDE.md. |
| 9 | blind-hunter | `.github/copilot-instructions.md` tells readers to keep itself in sync | low | defer | Real (line 9). The fix edits an agent-context file. |
| 10 | blind-hunter | Spine Stack row has the rationale crammed into a table cell, and the Robolectric constraint isn't recorded | low | patch | The rationale moved to a dated "Changes since 2026-10-08" note under the table, together with the Robolectric/Java 21 constraint. |
| 11 | blind-hunter | README is missing the Studio, SDK 37 and first-download prerequisites | low | patch | Added a prerequisites list and a note about the first build. |
| 12 | blind-hunter | Plan's change log and checkboxes not updated; `ticket: '1'` | false | reject | The fix is editing this plan. `ticket` is find's `id` (1) by rule. |
| 13 | blind-hunter | Verification claims `:data` tests | false | reject | The fix is editing this plan. `:data`'s binding is now exercised by `MainActivityTest`. |
| 14 | blind-hunter, intent-alignment | Nothing enforces that `:app` imports only `:domain` | false | reject | No `:app` file imports `data.*`. The spine defers architecture tests ("Until then the Gradle module boundaries carry AD-1"). |
| 15 | blind-hunter | Deleting the backup rules changed backup behaviour | false | reject | Both baseline XMLs were AI Studio templates with every rule commented out (`git show 0047560:app/src/main/res/xml/…`), so backup behaviour is unchanged. Lint now runs: 0 errors. |
| 16 | blind-hunter | `kotlinx-coroutines-core` should be `api` in `:domain` | false | reject | No `:domain` public API exposes a coroutines type today; `GreetingGateway` uses only `suspend`, which is stdlib. |
| 17 | blind-hunter | No edge-to-edge handling at targetSdk 36 | false | reject | `GreetingScreen` centres its only content, so system bars hide nothing. Edge-to-edge belongs to 1.5. The `.value` vs `by` point is style with no harm. |
| 18 | edge-case-hunter | An `Error` such as `NotImplementedError` escapes the use case | false | reject | No gateway can throw an `Error` (the stub returns a constant). Catching `Throwable` would also swallow OOM; failing loudly is correct. |
| 19 | edge-case-hunter | An internal `withTimeout` `CancellationException` leaves the spinner stuck | false | reject | No gateway uses `withTimeout`, so this state can't be reached. |
| 20 | edge-case-hunter | The ViewModel loading state has no exit if the use case throws | false | reject | The use case only rethrows cancellation, which happens when `viewModelScope` is cancelled because the ViewModel was cleared. |
| 21 | edge-case-hunter | `allowBackup=true` lets later tokens and the DB be backed up | false | reject | No tokens or DB exist in this change. Story 1.2 sets `allowBackup=false` before any of them arrive. |
| 22 | edge-case-hunter | `finance_app_logo` kept, against the plan, and orphaned | false | reject | `drawable/ic_launcher_foreground.xml` references it, so deleting it broke resource linking. Lint does not flag it as unused. The deviation is recorded in Implementation Notes. |
| 23 | intent-alignment | The diff edits the spine instead of holding to it (compileSdk 37) | false | reject | The user approved compileSdk 37 on 2026-10-25, and it is recorded in the frozen block. |
| 24 | intent-alignment | `greeting` package is outside AD-2's feature list | false | reject | It's a deliberate tracer (Design Notes) that 1.5 removes. |

## Design Notes

- **Package roots:** `com.shashi.expensetracker.domain.*` and `com.shashi.expensetracker.data.*`. `:app` uses `com.shashi.expensetracker.*` directly, which matches its namespace. Source goes in `src/*/kotlin`.
- **The `greeting` package is a tracer placeholder.** It's outside AD-2's feature list. Story 1.5 replaces it with the Home screen and deletes the package. Keep it small.

## Verification

**Commands:**
- `./gradlew assembleDebug test`: expected BUILD SUCCESSFUL, with the domain, data and app tests all passing.
- `./gradlew :domain:dependencies --configuration runtimeClasspath`: expected to list no Android artifacts.
- `$ANDROID_HOME/build-tools/37.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk`: expected `package: name='com.shashi.expensetracker'` and no `GET_ACCOUNTS`.

**Manual checks:**
- Install the debug APK on the user's device and confirm the greeting shows.
