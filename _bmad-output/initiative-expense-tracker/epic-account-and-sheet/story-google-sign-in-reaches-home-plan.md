---
title: 'Google sign-in reaches Home'
type: 'feature'
ticket: '1'
created: '2026-10-27'
status: 'built'
baseline_revision: '118c7da22a882c9b4bd0c1bfe6c0439d13698bce'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md'
  - '{project-root}/_bmad-output/initiative-expense-tracker/epic-account-and-sheet/epic-account-and-sheet.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Anyone can open the app with no account, and later epics have no signed-in user or session to build on. CAP-1 says that without a signed-in account, only Sign-in is reachable.

**Approach:** This is the tracer for epic 2: real Google sign-in through every layer.
- **Domain:** `AccountGateway` and `SessionRepository` interfaces in `:domain` `account`, plus use cases.
- **Data:** a Credential Manager "Sign in with Google" implementation, and a Room `AppDatabase` with a `session` table, in `:data`.
- **App:** a minimal Sign-in screen in `:app` `account`, made the gated start destination. Launch opens Home when a session is stored, and Sign-in otherwise.
- **Owner setup:** the owner sets up Google Cloud: a project, the Testing consent screen with test users and the `drive.file` scope, the Drive and Sheets APIs, a Web OAuth client, and the debug Android client.

## Boundaries & Constraints

**Always:**
- **Module rules:** no Google or Android type in a `:domain` signature (AD-11). Use cases return sealed results (Errors convention), ViewModels depend only on use cases (AD-15), and Hilt binds the implementations in `:data` (AD-16).
- **Credential Manager:** `GetSignInWithGoogleOption`, with the Web client ID as the server client ID.
- **What's stored:** the account ID (the ID token's `sub`, read locally because there's no server) and the email. The ID token itself is never stored. The Sheet ID column exists, empty until story 2.3.
- **Logging:** no email or token is ever logged.
- **Client ID config:** the Web client ID is set per build type through `buildConfigField`, read from the committed `gradle.properties` key `googleWebClientId`. It isn't a secret.
  - If the ID is blank, sign-in returns a named failure. It must not crash or hang.
  - CI builds with it blank.
- **Room:** `AppDatabase` version 1, with its schema exported to `data/schemas/`. Later epics add tables to it with migrations.
- **Existing tests stay green:** shell, navigation and Roborazzi tests run with a fake signed-in session, and the existing baselines stay byte-identical.

**Never:**
- Drive or Sheets access, `AuthorizationClient`, or the Expense Sheet (stories 2.2 and 2.3).
- The designed Sign-in visuals (story 2.5).
- Sign-out or account switching (epic 3, Decision 6).
- Storing a token, refresh token or client secret.
- An OAuth redirect or deep link.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Fresh install | No session | Sign-in shows; no nav, no other screen reachable | No error expected |
| Sign in | User picks an account | Session saved (ID, email); Home opens | No error expected |
| Relaunch | Session stored | Home opens directly, offline too | No error expected |
| Cancel | User dismisses the picker | Stays on Sign-in, no message | Cancellation is not a failure |
| Failure | Network down, blank client ID, or other Credential Manager error | Stays on Sign-in with a short error text | Mapped to a named `SignInResult` failure |

</frozen-after-approval>

## Code Map

- `app/src/main/kotlin/com/shashi/expensetracker/MainActivity.kt`: `ExpenseTrackerTheme { AppNavHost() }`. It needs the start destination from a small `StartViewModel`, and draws only the canvas until the session has been read.
- `app/.../navigation/AppNavHost.kt`: `AppNavHost(startDestination: String = MainGraph, …)` is the gate seam story 1.5 built. Add a `SignInGraph` route next to `MainGraph`. After sign-in, navigate to `MainGraph` and pop Sign-in.
- `app/.../navigation/Destinations.kt`: route constants live only here.
- `app/.../navigation/AppShell.kt`: the nav is already hidden outside `MainGraph`.
- `data/build.gradle.kts`: Hilt and KSP only. Add Room (runtime, ktx, the KSP compiler and the schema location), the credentials, credentials-play-services-auth and googleid libraries, `buildFeatures.buildConfig`, and the test libraries (JUnit, Robolectric, coroutines-test, room-testing).
- `data/.../greeting/`: the existing Hilt `@Binds` module pattern to copy.
- `gradle/libs.versions.toml`: already has the Room, credentials and googleid entries. Add `hilt-android-testing` at the Hilt version.
- `app/src/test/.../MainActivityTest.kt`, `navigation/AppShellTest.kt` and `screenshot/ShellScreenshotTest.kt`: these launch `MainActivity` and expect Home. Give them a signed-in session through Hilt test bindings (`@HiltAndroidTest`, plus `@TestInstallIn` or `@BindValue` replacing the session binding).
- Debug signing SHA-1 for the debug Android OAuth client: `EA:0A:C8:A2:95:A7:87:DB:D5:A2:79:EF:A2:80:AA:52:21:69:D3:B5`.

## Tasks & Acceptance

**Execution:**
- [x] `gradle/libs.versions.toml`, `data/build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`: add the libraries, Room's KSP schema argument, the `googleWebClientId` property (blank) and the `GOOGLE_WEB_CLIENT_ID` build config field, plus Hilt testing in `:app`.
- [x] `domain/src/main/kotlin/.../domain/account/`:
  - `Session` (`accountId`, `email`, `sheetId: String?`);
  - `SessionRepository` (`observe`, `save`);
  - `AccountGateway` (`signIn(): SignInResult`);
  - a `SignInResult` sealed type: `Success(accountId, email)`, `Cancelled`, `Failure(reason)`;
  - `SignInUseCase`, which saves the session on success;
  - `ObserveSessionUseCase`.
- [x] `domain/src/test/.../account/`: JVM tests for the use cases with fakes. They cover success saving the session, cancel and failure saving nothing, and the observe flow.
- [x] `data/src/main/kotlin/.../data/account/`:
  - `AppDatabase`, the `SessionEntity` and `SessionDao`, and the Room `SessionRepository`;
  - `CredentialManagerAccountGateway` (see Design Notes for the Activity context);
  - `AccountModule`, which binds them.
- [x] `data/src/test/.../account/`: Robolectric tests for the Room repository (in-memory) and for the gateway's mapping of `sub` from the ID token payload, cancellation and errors, through a mapping function testable without Credential Manager.
- [x] `app/src/main/kotlin/.../account/`: `SignInScreen` (minimal: title, a Continue with Google button `signin_continue_google`, a progress state `signin_progress`, and the error text `signin_error`), `SignInViewModel` and `SignInUiState`.
- [x] `app/.../StartViewModel.kt`, `MainActivity.kt`, `navigation/AppNavHost.kt`, `navigation/Destinations.kt`: gate the start, and navigate to `MainGraph` after sign-in with Sign-in popped.
- [x] `app/src/test/...`: the shell, Main and screenshot tests use a fake signed-in session. Add a `SignInGateTest`: no session shows Sign-in with no nav, a successful sign-in shows Home, cancel stays put, and failure shows `signin_error`.
- [x] `docs/GOOGLE_CLOUD_SETUP.md`: the owner's click-by-click steps, and where to paste the Web client ID (`gradle.properties`). The release client comes in story 2.7.
- [x] `CLAUDE.md`, `.github/copilot-instructions.md`: the account package, the session gate, the Room database, and the Web client ID property.

**Acceptance Criteria:**
- Given the owner's Google Cloud setup and the Web client ID in `gradle.properties`, when they install the debug build on their phone and tap Continue with Google, then they pick their account and Home opens. Relaunching opens Home without signing in again. This is a manual check.
- Given `./gradlew build verifyRoborazziDebug`, when it runs, then it passes with every existing baseline unchanged.

## Implementation Notes

- **Activity tracking:** `ResumedActivityTracker` (`:data`) keeps a weak reference to the resumed Activity. A self-registering tracker would be created lazily, after `MainActivity` had already resumed, so it would miss the first resume. Instead, `AccountDataModule` binds it `@IntoSet` into `Set<Application.ActivityLifecycleCallbacks>`, and `ExpenseTrackerApp` registers that set in `onCreate`. That set is an Android type, not a `:data` type, so app code still imports no `:data` types.
- **Modules:** `AccountModule` holds only the `SessionRepository` and `AccountGateway` binds, which tests replace with `FakeAccountModule`. `AccountDataModule` provides `AppDatabase` (`expense_tracker.db`), `SessionDao`, the `@GoogleWebClientId` string and the tracker set entry. It stays installed in tests, so the multibound set always has a contribution.
- **`AppDatabase` location:** it sits in `data/account/`, as the Code Map says. When a second feature adds tables, consider moving it to a shared `data/db` package.
- **`SignInResult.Failure(reason)`:** `reason` is the enum `SignInFailure` with these values:
  - `NotConfigured`
  - `NoActivity`
  - `NoGoogleAccount`
  - `InvalidCredential`
  - `Unavailable`
  - `StorageFailed`

  The UI shows "Sign-in isn't set up in this build." for `NotConfigured` and the epic's line "Couldn't sign in. Check your connection and try again." for everything else.
- **Session row:** the `session` table is a single row with `id = 0`, and `save` upserts it. `sheet_id` is nullable and empty until 2.3.
- **Gate:** `StartViewModel` reads `ObserveSessionUseCase().first()` once. Later session changes don't move the start destination; Sign-in navigates to `MainGraph` itself, with `popUpTo(SignInGraph) { inclusive = true }`.
- **Domain:** `kotlinx-coroutines-core` changed from `implementation` to `api` in `:domain`, because `SessionRepository` exposes `Flow`.
- **Storage errors (review fix):** `RoomSessionRepository` catches Room errors and logs only the exception class. `observe()` emits null, so an unreadable session behaves as signed out. `save()` returns `Boolean`, and `SignInUseCase` maps `false` to `StorageFailed`.
- **Real-app test (review fix):** `ExpenseTrackerAppTest` reads the tracker from the real app's injected `activityCallbacks` set. A Hilt EntryPoint declared in test sources isn't part of the component generated for the main `ExpenseTrackerApp`, so it can't be used there.
- **R8:** `credentials-play-services-auth` ships its own consumer keep rule, so no rule was added to `app/proguard-rules.pro`. `./gradlew build` minified the release without errors.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-10-27, quick)

Verdicts: medium 3, low 1. Routes: patch 4.

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | A Room read error in `observe()` crashes launch, with the gate stuck on Loading | medium | patch | Real: there's no catch anywhere between the DAO and `StartViewModel`'s `viewModelScope.launch`. `:data` now catches the error, logs only the class, and emits null, so the user is treated as signed out. |
| 2 | `SessionRepository.save` lets Room errors escape `:data`; only `SignInUseCase`'s blanket catch maps them | medium | patch | Real: this breaks the Errors convention, and story 2.3's Sheet ID write would hit raw exceptions. `save` now returns a domain result from `:data`. |
| 3 | `ObserveSessionUseCase` returns a raw Flow, not a sealed result | low | patch | With fix 1 it can't throw. The KDoc now documents the observer exception. |
| 4 | No test runs the real `ExpenseTrackerApp` start-up any more, so the callback registration is untested | medium | patch | Real: every activity test uses `HiltTestApplication`. Added a real-app test that asserts the tracker holds the launched activity. |

## Design Notes

- **Activity context:** Credential Manager needs an Activity to show its account picker, but `AccountGateway` can't take one, because there are no Android types in `:domain`. The `:data` implementation tracks the current resumed Activity itself, registering `ActivityLifecycleCallbacks` on the injected `Application` and keeping only a weak reference. If no Activity is resumed, it returns `Failure`.
- **Account ID:** the ID token is a JWT. Base64-decode its payload and read `sub` (and `email`). There's no signature check, because the token comes straight from the platform and no server trusts it.
- **First frame:** `StartViewModel` exposes `Loading`, `SignedIn` or `SignedOut`. `MainActivity` draws only the canvas while it's `Loading` (a Room read takes milliseconds), so neither Sign-in nor Home flashes.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout. Don't commit, push or open a PR; the main session does that after review.

**Commands:**
- `./gradlew build verifyRoborazziDebug`: expected to pass; with `googleWebClientId` blank, sign-in fails with a named failure, not a crash.
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`: expected to pass, including the new account tests.

**Manual checks (main session with the owner, after setup):**
- On the S23 with the debug build: fresh install, then Sign-in, then pick the account, then Home. Relaunch goes straight to Home. Cancelling the picker stays on Sign-in.
