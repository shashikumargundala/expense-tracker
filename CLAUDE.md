# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

An offline-first Android expense tracker that will sync with a Google Sheet. It's built as three Gradle modules following Clean Architecture, with Kotlin, Jetpack Compose, Material 3 and Hilt. `applicationId` and app namespace are `com.shashi.expensetracker`. Toolchain: AGP 9.4.1 (built-in Kotlin, so no `kotlin-android` plugin), Kotlin 2.4.21, KSP 2.3.12, Gradle 9.6.0, JDK 17 to run Gradle, a Java 21 toolchain for `:app` unit tests (Robolectric 4.17's SDK 36 sandbox needs it; the foojay resolver downloads it automatically), Java target 11, compileSdk 37, targetSdk 36, minSdk 24 (Compose 1.12 and Lifecycle 2.11 require compileSdk 37).

The architecture spine at `_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md` is the source of truth for design decisions (AD-1 to AD-20) and Stack versions. `.github/copilot-instructions.md` holds the same guidance. Keep the two files in sync.

## Commands

Run all commands from the repo root using the Gradle wrapper:

```bash
./gradlew assembleDebug test                      # build the debug APK and run all JVM tests in every module
./gradlew :domain:test                            # pure JVM tests for :domain
./gradlew :app:testDebugUnitTest                  # :app JVM tests (incl. Robolectric Compose tests)
./gradlew :app:testDebugUnitTest --tests "com.shashi.expensetracker.navigation.AppShellTest"     # one class
./gradlew :domain:test --tests "com.shashi.expensetracker.domain.greeting.GetGreetingUseCaseTest"     # one class
./gradlew :app:lintDebug                          # Android lint
./gradlew :domain:dependencies --configuration runtimeClasspath   # confirm :domain has no Android deps
./gradlew checkKotlinFileLength                   # fail on any .kt file over 500 lines (AD-17)
./gradlew recordRoborazziDebug                    # re-record screenshot baselines into app/src/test/screenshots/
./gradlew verifyRoborazziDebug                    # compare screenshots against the committed baselines
./gradlew compareRoborazziDebug                   # write _compare diff images without failing
./gradlew build verifyRoborazziDebug              # what CI runs: assemble, unit tests, lint, the 500-line check, screenshots
```

- **CI:** `.github/workflows/ci.yml` runs `./gradlew build verifyRoborazziDebug` on JDK 21 for every PR (`pull_request` only, so each commit runs once). It needs no secrets; release builds stay unsigned there. On failure it uploads reports, test results and Roborazzi diffs (`**/build/outputs/roborazzi/`).
- **Screenshots:** Roborazzi (Robolectric native graphics) baselines are committed in `app/src/test/screenshots/` as `<screen>_<variant>.png`; today only the shell on Home (`screenshot/ShellScreenshotTest`: Home in light, dark, 1.75x font and 600dp rail, plus Expenses at 320dp). Plain `test` only renders them. A failed verify writes `<name>_compare.png` to `app/build/outputs/roborazzi/`. Variants come only from Robolectric `@Config` (qualifiers, `+night`, `fontScale`), never test-only app code. To add a screen, follow the KDoc on `captureScreen` in `screenshot/Screenshots.kt`, then record on a passing build and check each PNG by eye. Baselines must match Linux CI rendering; if CI fails only on text anti-aliasing, re-record on Linux CI rather than raising the threshold.

- **Debug signing:** debug builds use AGP's default debug keystore (`~/.android/debug.keystore`, created on the first debug build). No keystore lives in the repo.
- **Secrets:** there is no Secrets Gradle plugin and no `.env`. Nothing secret is committed.
- **Web client ID:** `googleWebClientId` in the committed `gradle.properties` is the Web OAuth client ID that Credential Manager uses as the server client ID. It isn't a secret. `data/build.gradle.kts` exposes it per build type as `BuildConfig.GOOGLE_WEB_CLIENT_ID`. When it's blank (the default, and in CI), the app still builds and sign-in returns `SignInFailure.NotConfigured` instead of opening the picker. `docs/GOOGLE_CLOUD_SETUP.md` has the owner's Google Cloud steps (project, Testing consent screen with test users and `drive.file`, Drive and Sheets APIs, Web client, debug Android client).
- **Release builds:** R8 minification and resource shrinking are on. Signing reads `KEYSTORE_PATH` (absolute, or relative to the repo root), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` from the environment. If any is missing, `assembleRelease` produces an unsigned APK; it never falls back to the debug key. The "Release builds will be unsigned" warning is logged at configuration time, so it prints on any build (including `assembleDebug` and `test`) whenever Gradle configures the build without those variables, and a configuration-cache hit hides it. Keep rules for Moshi and Retrofit models are in `app/proguard-rules.pro`.
- **Sharing an APK:** until the Play release (story 1.7), shared builds come from `scripts/build-release-apk.sh`, which builds a signed release into `dist/`. The release key lives outside the repo at `~/.android-keys/expense-tracker-release.jks`. `docs/RELEASE.md` covers the key, the backup, `versionCode` bumps and sharing.
- **Manifest:** `allowBackup=false` and `data_extraction_rules.xml` exclude everything from cloud backup and device transfer (the Sheet is the backup). `GET_ACCOUNTS` is declared with `tools:node="remove"` so no dependency can add it.

## Architecture

Dependencies point inward only (AD-1):

- **`:domain`** (pure Kotlin/JVM, `com.shashi.expensetracker.domain.*`): entities, use cases and `*Gateway` / `*Repository` interfaces. Its only dependencies are `javax.inject` and `kotlinx-coroutines-core`. No Android, Room, Retrofit or Google dependency is allowed.
- **`:data`** (Android library, `com.shashi.expensetracker.data.*`): implementations of the domain interfaces plus the Hilt modules that bind them. Depends on `:domain`. Its Robolectric tests run on the same Java 21 toolchain as `:app` (`./gradlew :data:testDebugUnitTest`).
- **`:app`** (Android application, `com.shashi.expensetracker.*`): the `@HiltAndroidApp` `ExpenseTrackerApp`, the `@AndroidEntryPoint` `MainActivity`, Compose screens and `@HiltViewModel`s. Depends on `:domain` and `:data`, but app code imports only `:domain` types; `:data` is there only so Hilt can install its bindings.

Hilt wires every dependency (AD-16) via KSP. Inside each module, code is grouped by feature package.

### Account and session

- **`:domain` `account`:** `Session` (`accountId`, `email`, `sheetId: String?`), `SessionRepository` (`observe(): Flow<Session?>`, `save(): Boolean`; `:data` catches Room errors, so an unreadable session emits null and a failed save returns false), `AccountGateway` (`signIn(): SignInResult`, `getAccess(): AccessResult`), the sealed `SignInResult` (`Success(accountId, email)`, `Cancelled`, `Failure(SignInFailure)`), the sealed `AccessResult` (`Granted`, `NeedsUserConsent(ConsentRequest)`, `Failure(AccessFailure)` with `NotSignedIn`, `Unauthorized` or `Unavailable`; `ConsentRequest` is an opaque handle with no Google type), `SignInUseCase` (stores the session only on success) and `ObserveSessionUseCase`.
- **`:data` `account`:**
  - `CredentialManagerAccountGateway` uses `GetSignInWithGoogleOption` with the Web client ID. `GoogleCredentialMapping` reads `sub` (the account ID) and `email` from the ID token's payload locally and maps cancel to `Cancelled` and errors to named failures. The ID token is never stored, and no email or token is logged.
  - The gateway shows the picker on the activity that `ResumedActivityTracker` holds weakly. `AccountDataModule` binds the tracker into a `Set<Application.ActivityLifecycleCallbacks>`, which `ExpenseTrackerApp` registers at startup.
  - Room: `AppDatabase` (`expense_tracker.db`, version 1) has the single-row `session` table (`SessionEntity`, `SessionDao`, `RoomSessionRepository`). The schema is exported to `data/schemas/` and committed. Later epics add tables to this database with a version bump and a migration.
  - `AccountModule` binds `SessionRepository` and `AccountGateway`. `AccountDataModule` provides the database, DAOs, the `@GoogleWebClientId` string and the tracker.
  - `DriveAuthorization` (`@Singleton`, also the `AccessTokenSource`) backs `getAccess()`: `AuthorizationClient` with the `drive.file` scope only, `setAccount(Account(email, "com.google"))` for the stored session's email, no UI. A resolution becomes `NeedsUserConsent(PendingIntentConsentRequest)` (story 2.3 launches it); no session is `Failure(NotSignedIn)` with no authorize call. The token is cached in memory per account behind a `Mutex`; `invalidate(token)` drops it and calls `clearToken`. `DriveAuthorizationMapping` is the pure, tested mapping.
- **`:data` `google` (Drive and Sheets REST):**
  - `GoogleApiModule` provides Moshi, the `@GoogleApi` `OkHttpClient` and the `DriveService` (Drive v3, `https://www.googleapis.com/drive/v3/`: `listFiles`, `updateFile`) and `SheetsService` (Sheets v4, `https://sheets.googleapis.com/v4/`: `createSpreadsheet`, `batchUpdateValues`) Retrofit services. Models (`DriveFile`, `FileList`, `Spreadsheet`, `SpreadsheetProperties`, `Sheet`, `SheetProperties`, `ValueRange`, `BatchUpdateValuesRequest`) use Moshi codegen (`@JsonClass(generateAdapter = true)`) and declare only the fields used; add a field when a story needs it.
  - `AuthInterceptor` attaches `Authorization: Bearer <token>`; with no token it throws `GoogleAccessDeniedException` and nothing is sent. `TokenAuthenticator` answers a 401 with `invalidate`, a fresh token and exactly one retry (`priorResponse != null` returns null, so a second 401 surfaces).
  - **To call Drive or Sheets**, inject the service and wrap each call in `googleApiCall { drive.listFiles(...) }`. It returns `ApiResult.Success(value)` or `ApiResult.Failure(ApiFailure)`: `NeedsUserConsent(request)`, `Access(AccessFailure)` (a 401 after the retry is `Access(Unauthorized)`), `Network`, `Http(code)` or `Unexpected`. It rethrows cancellation; map `ApiFailure` to the feature's domain failure in `:data`.
  - **Token rules:** tokens live in memory only, never on disk, in Room, in logs or in `toString()` (`TokenResult.Token` redacts itself). Never log emails, URLs, headers or bodies, and never add an OkHttp logging interceptor. No refresh token, client secret or OAuth redirect exists (AD-10).
  - Tests: `GoogleApiClientTest` runs the real client against `mockwebserver3` with `FakeTokenSource`; `GoogleApiModelsTest` parses recorded samples in `data/src/test/resources/google/`.
- **Session gate (`:app`):** `StartViewModel` reads the stored session once at launch (`Loading`, `SignedIn` or `SignedOut`). `MainActivity` draws only the background while it's `Loading`, then starts `AppNavHost` at `MainGraph` when signed in or at `SignInGraph` otherwise, so launch opens Home offline. `account/SignInScreen` is minimal until story 2.5 (tags `screen_signin`, `signin_continue_google`, `signin_progress`, `signin_error`). Cancel stays silent. After sign-in, `AppNavHost` navigates to `MainGraph` and pops `SignInGraph`.
- **Tests:** app tests that launch `MainActivity` are `@HiltAndroidTest` with `@Config(application = HiltTestApplication::class)` and a `HiltAndroidRule` at rule order 0. `account/FakeAccountModule` (`@TestInstallIn`, replacing `AccountModule`) gives every such test a signed-in `FakeSessionRepository` and a `FakeAccountGateway`. `account/SignInGateTest` clears the session and launches with `createEmptyComposeRule` plus `ActivityScenario`. The one exception is `ExpenseTrackerAppTest`, which runs on the real `ExpenseTrackerApp` and Hilt graph. It checks that launch with no session opens Sign-in and that the app registered `ResumedActivityTracker`.

The greeting tracer still runs from `GetGreetingUseCase` (`:domain`) through `StubGreetingGateway` (`:data`, bound by `GreetingModule`). In `:app`, `HomeViewModel` and `HomeScreen` now show it.

### App shell and navigation (`:app`)

- `MainActivity` is the navigation host only. It calls `enableEdgeToEdge()`, then draws `ExpenseTrackerTheme { AppNavHost(startDestination) }`, with the start destination picked by `StartViewModel` (the session gate above).
- `navigation/Destinations.kt` holds every route string. The `TopLevelDestination` enum lists the five tabs in order (Home, Expenses, Inbox, Budgets, Settings), each with a route, label, icon (`res/drawable/ic_nav_*.xml`) and nav testTag. `MainGraph` is the nested tab graph's route. `SignInGraph` and `SignInDestination` are the sign-in graph and screen.
- `navigation/AppNavHost.kt`: a root `NavHost` holding the nested `SignInGraph` and `MainGraph` (the five tabs, Home first). `AppNavHost(startDestination, inboxBadgeCount)` is the sign-in gate: `MainActivity` passes `MainGraph` or `SignInGraph`. Tabs cross-fade, and reduce motion turns the fade off.
- `navigation/AppShell.kt`: the aurora background, plus `PillNav` (the floating glass pill) below 600dp or `AppNavRail` at 600dp and wider. The nav shows only while a `MainGraph` destination is on screen.
  - It owns tab navigation: `popUpTo(home) { saveState = true }`, `launchSingleTop` and `restoreState`. Back from any other tab returns to Home, and Back on Home goes to the system.
  - It passes each screen a `contentPadding`: `safeDrawing` insets, widened to clear the pill or the rail.
- `navigation/components/`: `NavBadge`, the Inbox badge (hidden at 0, read as "N new"), and the shared nav item style. The active item's gradient ends at #DB2777 so its white label (`ExtendedColors.onGradient`) passes AA, and sizes snap when reduce motion is on. The active item, pill and rail, shows its icon only above 1.3x font or when its measured label doesn't fit (`showActiveNavLabel`); TalkBack still reads the label.
- Screen packages: `account` (Sign-in, outside the tabs), `home`, `expense`, `inbox`, `budget`, `settings`. Each has `<Tab>Screen`, `<Tab>ViewModel` and `<Tab>UiState`. Every tab except Home is a placeholder that shows only its title. AD-2 allows `home` as an `:app`-only package that composes other features.
- Screens never see the `NavController`. Each gets a `contentPadding` and, later, action lambdas. `<Tab>Route` collects the ViewModel (`hiltViewModel()`) and calls the stateless `<Tab>Screen`.

**To add a tab screen** (or fill in a placeholder):
1. Add or replace `<Tab>Screen`, `<Tab>ViewModel` and `<Tab>UiState` in the feature package. The screen applies `contentPadding` and tags its root `screen_<tab>`.
2. For a new top-level tab, add an entry to `TopLevelDestination` (route, `R.string` label, `ic_nav_*` vector, `nav_<tab>` tag) and a `composable(route)` in `AppNavHost`'s `mainGraph`.
3. For a screen outside the tabs, add its own graph next to `MainGraph` in `AppNavHost`, as `SignInGraph` does. The nav hides there automatically.
4. Cover it in `navigation/AppShellTest`.

Tags kept by the shell: `nav_home`, `nav_expenses`, `nav_inbox`, `nav_budgets`, `nav_settings`, `nav_inbox_badge`, `pill_nav`, `nav_rail`, `screen_home` … `screen_settings`, `greeting_text`, `greeting_loading`, plus Sign-in's `screen_signin`, `signin_continue_google`, `signin_progress` and `signin_error`. FABs, snackbars and real screen content come per screen in later epics.

## Conventions

- Use cases are `VerbNounUseCase` classes with one public `operator fun invoke`. They return a sealed result (success or a named failure) and never throw to callers.
- Outside systems are behind `*Gateway` interfaces, stored data behind `*Repository` interfaces, both defined in `:domain` and implemented in `:data`.
- Each screen has one ViewModel exposing one immutable `<Screen>UiState` as a `StateFlow`. ViewModels depend only on `:domain` use cases and turn failures into `UiState` messages.
- Interactive and key UI elements use `testTag` (for example `nav_home`, `screen_home`, `greeting_text`). Keep existing tags and add tags to new user-critical actions.
- No `.kt` file may exceed 500 lines (AD-17). `checkKotlinFileLength` in the root `build.gradle.kts` enforces it as part of `check`.
- Dependencies and plugins go through `gradle/libs.versions.toml` aliases. Don't hard-code versions in module build files. The one exception is the `foojay-resolver-convention` plugin, pinned in `settings.gradle.kts`, because the settings `plugins {}` block can't use catalog aliases. The catalog already holds the Stack libraries later epics need.
- Source lives in `src/main/kotlin` and `src/test/kotlin`.
- Bottom sheets pass `scrimColor = MaterialTheme.colorScheme.scrim`, so the scrim token's alpha (45% light, 65% dark) applies. M3's default replaces it with 32%.
