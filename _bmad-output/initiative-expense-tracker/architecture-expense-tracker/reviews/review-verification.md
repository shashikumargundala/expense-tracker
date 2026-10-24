---
review: verification
target: ../architecture-expense-tracker.md
memlog: ../.memlog.md
reviewed: '2026-10-08'
lens: 'Every committed decision was web-researched or checked against reality, not asserted from training data'
verdict: 'Revise before build: the APIs hold up, but the Stack table has unverified, outdated pins and AD-10 leaves out details that decide whether reinstall rediscovery works'
---

# Verification Review: Architecture Spine, Expense Tracker

## Verdict

**Revise before build.** The API-level decisions hold up against current Google and Android documentation:

- Credential Manager for sign-in.
- `AuthorizationClient` with `drive.file`.
- Sheets REST v4 under `drive.file`.
- A WorkManager job with a network constraint.

The memlog's 2026-10-08 checks for Hilt, hilt-navigation-compose and hilt-work, Credential Manager, googleid, play-services-auth, WorkManager, Room and Navigation are all confirmed against Maven metadata today.

Three problems need fixing first:

1. The rows of the Stack table marked "Existing [ADOPTED]" (memlog line 39) were never checked. Several of them are well out of date. Two of them also clash with the newly verified libraries: Compose BOM against Navigation 2.10.2 and hilt-navigation-compose, and Kotlin 2.2.10 against Hilt 2.60.1.
2. AD-10 and the deployment notes rely on `drive.file` to find the sheet again after a reinstall. They don't state the condition that makes this work: per-file grants belong to the Google Cloud project.
3. AD-10's line "requests a new one silently when it expires" skips the `clearToken` step that the official guide documents. It also doesn't say what the background sync worker does when it needs user consent.

## Method

- Read the spine and `.memlog.md` and listed every external claim: library versions, the existence of each named technology, and API behaviour.
- Library versions: fetched `maven-metadata.xml` directly from Google Maven (`dl.google.com/android/maven2`) and Maven Central (`repo1.maven.org`) on 2026-10-08. Read the POM and Gradle module files of the libraries in question to check their transitive Compose and Kotlin requirements.
- APIs: fetched the official reference pages for Sheets v4 `spreadsheets.create` and `spreadsheets.values.batchUpdate`, Drive v3 `files.list`, the Drive and Sheets scope guides, the Android Authorization guide, the androidx.credentials release notes, the WorkManager "define work" guide and the Dagger releases page.
- Compared the spine's Stack table against `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.3.1).

## Verification ledger

### Library versions (checked 2026-10-08)

| Stack entry | Spine pin | Memlog status | Current stable (source) | Result |
| --- | --- | --- | --- | --- |
| Compose BOM | 2024.09.00 | Adopted, not verified | **2026.09.00** (Google Maven, updated 2026-09-09). BOM 2024.09.00 maps `ui` to 1.7.0; 2026.09.00 maps it to 1.12.1 | **Outdated by 2 years; conflicts with other pins (F1)** |
| OkHttp (+ logging-interceptor) | 4.10.0 | Adopted, not verified | **5.5.0** (Maven Central, 2026-08-16). Last 4.x is 4.12.0 | **Outdated (F2)** |
| Retrofit / converter-moshi | 2.12.0 | Adopted, not verified | **3.0.0** (Maven Central, 2025-05-15). 2.12.0 declares OkHttp 3.14.9; 3.0.0 declares OkHttp 4.12.0 | **Outdated (F2)** |
| Moshi | 1.15.2 | Adopted, not verified | 1.15.2 | OK (current) |
| Kotlin | 2.2.10 | Adopted, not verified | **2.4.21** (2.3.21 is the line Hilt 2.60 builds with) | **Outdated; clashes with Hilt 2.60.1 (F3)** |
| AGP | 9.1.1 | Adopted, not verified | **9.4.1** stable (9.5.0-alpha08 preview) | Outdated (F3) |
| KSP | 2.3.5 | Adopted, not verified | **2.3.12** | Outdated (F3) |
| Robolectric / Roborazzi | 4.16.1 / 1.59.0 | Not in the memlog at all | **4.17 / 1.76.0** | Outdated, never verified (F3) |
| Hilt | 2.60.1 | Verified | 2.60.1 (requires AGP 9 and Gradle 9.1+; minSdk 23; built with Kotlin 2.3.21) | OK. Wrapper is Gradle 9.3.1 |
| hilt-navigation-compose, hilt-work | 1.4.0 | Verified | 1.4.0 | OK |
| Navigation Compose | 2.10.2 | Verified | 2.10.2 (needs Compose `ui`/`runtime`/`animation` **1.10.5**) | OK, but forces Compose above the BOM (F1) |
| Room | 2.8.5 | Verified | 2.8.5. Room 3 stable is 3.0.3 (3.1.0-alpha01 preview) | OK. The memlog's Room 3 note is accurate |
| WorkManager | 2.12.0 | Verified | 2.12.0 (2026-09-23) | OK |
| Credential Manager | 1.6.0 | Verified | 1.6.0 (2026-04-08; minSdk 23; 1.7.0-alpha03 preview) | OK. minSdk 24 is compatible |
| googleid | 1.2.1 | Verified | 1.2.1 (2026-09-16) | OK |
| play-services-auth | 22.0.0 | Verified | 22.0.0. The Authorization guide uses this exact version | OK |
| Sheets API | REST v4 | Verified | v4 is current | OK |

### API and technology claims

| Claim (where) | Checked against | Result |
| --- | --- | --- |
| Sign-in through Credential Manager plus Sign in with Google (AD-10) | androidx.credentials release page; the memlog's 2024-09 blog source | Confirmed. The library is current and is Google's supported path |
| `AuthorizationClient` with `drive.file` for Drive and Sheets (AD-10) | developer.android.com/identity/authorization | Confirmed. The guide's own example requests `DriveScopes.DRIVE_FILE`. Tokens last about 1 hour, and `authorize()` returns a token silently once consent exists. **But the guide also tells you to `clearToken` an invalid token, and a refresh token must never be stored on the device (F5)** |
| No refresh token or client secret on the device (AD-10, NFR-5) | Same guide | Confirmed as the recommended practice |
| `drive.file` is non-sensitive and the narrowest scope (NFR-3 map) | Drive "Choose scopes" guide; Sheets scopes page ("Recommended, Non-sensitive") | Confirmed |
| Sheets v4 can create and read/write the app's spreadsheet under `drive.file` (AD-7, AD-11) | Reference pages for `spreadsheets.create` and `spreadsheets.values.batchUpdate` | Confirmed. Both list `drive.file` among accepted scopes |
| Drive can find the sheet again after a reinstall under `drive.file` (PRD acceptance at line 105; AD-11 "find or create") | Drive v3 `files.list` reference (accepts `drive.file`); Drive scope guide (per-file access); Picker docs (grant is tied to the Cloud project number via `setAppId`) | **Holds only under conditions the spine doesn't state (F4)** |
| A WorkManager job constrained to a network connection (AD-14) | WorkManager "define work" guide | Confirmed: `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`, unique work, `Result.retry()` with backoff. The spine doesn't name the unique-work policy or explain how a worker without UI gets a token (F5) |
| Retrofit with a Bearer token works against Sheets v4 | Memlog line 38; REST reference | Confirmed (a plain OAuth 2.0 Bearer header) |
| detekt 1.23.8 lacks AGP 9 support; 2.0 is alpha (memlog line 38, Deferred) | Memlog only. Not re-checked in this review | Plausible. Doesn't affect any AD, since the 500-line rule uses a Gradle check |

## Findings

### F1. Compose BOM 2024.09.00 is two years old and conflicts with the verified libraries (High)

**Evidence:** Google Maven `androidx/compose/compose-bom/maven-metadata.xml` shows release **2026.09.00**. The 2024.09.00 BOM pins `androidx.compose.ui:ui` at 1.7.0. The `navigation-compose-2.10.2.module` file requires `ui`, `runtime`, `runtime-saveable` and `animation` at **1.10.5**, and the `hilt-navigation-compose-1.4.0.pom` file requires `ui`/`runtime` 1.8.2 and `lifecycle-viewmodel-compose` 2.9.1. The toml still has lifecycle at 2.8.7.

**Impact:** Gradle resolves `ui`, `runtime` and `animation` up to 1.10.5. Artifacts that only the BOM controls, such as `material3`, `foundation` and `ui-test`, stay at the 2024 versions. The build then mixes Compose versions, which defeats the BOM and is a known source of runtime `NoSuchMethodError` crashes and Roborazzi rendering drift. The memlog marks the pin "[ADOPTED]" without checking it.

**Suggested fix:** pin Compose BOM **2026.09.00** in the Stack table. Bump the lifecycle and activity-compose aliases to versions at or above what Navigation 2.10.2 and hilt-navigation-compose 1.4.0 require. Add a memlog `(version)` line recording the check.

### F2. OkHttp 4.10.0 and Retrofit 2.12.0 were never re-verified and are behind (Medium)

**Evidence:** on Maven Central, OkHttp's latest release is **5.5.0** (2026-08-16) and the last 4.x release is 4.12.0. Retrofit's latest is **3.0.0** (2025-05-15). The `retrofit-2.12.0.pom` declares OkHttp **3.14.9**, so today's build only gets OkHttp 4 because the app overrides it. The `retrofit-3.0.0.pom` declares OkHttp 4.12.0.

**Impact:** this is the whole transport for Sheets and Drive (AD-11). OkHttp 4.10.0 dates from 2022 and has missed later TLS, HTTP/2 and security fixes. Starting a new `:data` module on Retrofit 2 also means a later migration.

**Suggested fix:** pin **Retrofit 3.0.0** with the same version of converter-moshi, plus OkHttp **4.12.0**, which is what Retrofit 3 declares. OkHttp **5.5.0** is an option if you want the current major version. Either way, keep `logging-interceptor` on the same version through `okhttp-bom`. Record the check in the memlog.

### F3. Other adopted toolchain pins are unverified, and Kotlin 2.2.10 sits below Hilt 2.60.1's Kotlin (Medium)

**Evidence:**
- On Maven Central, Kotlin stable is **2.4.21** and KSP is **2.3.12**.
- On Google Maven, AGP stable is **9.4.1**.
- Robolectric is **4.17** and Roborazzi is **1.76.0**. The spine pins 4.16.1 and 1.59.0, which don't appear in the memlog at all.
- The Dagger releases page says 2.60 "updated Kotlin to 2.3.21", and `hilt-android-2.60.1.pom` depends on `kotlin-stdlib` **2.3.21**.

**Impact:** with Kotlin 2.2.10, Hilt pulls a newer stdlib than the compiler. The compiler reads metadata only one version ahead, so this works today but is fragile, and the memlog never checked it. Pins that are a year or more old also miss Compose compiler and AGP 9 fixes.

**Suggested fix:**
- Re-verify the "Existing [ADOPTED]" row and either:
  - move to Kotlin **2.3.21 or newer** with KSP **2.3.12**, or
  - add a memlog line explaining why 2.2.10 stays.
- Bump AGP to **9.4.1** (Gradle 9.3.1 already meets Hilt's Gradle 9.1+ requirement), and Robolectric and Roborazzi to **4.17 / 1.76.0**.
- Label any pin you keep as "kept deliberately, checked 2026-10-08".

### F4. Finding the sheet again under `drive.file` works only under conditions the spine doesn't state (High)

**Evidence:**
- The Sheets `spreadsheets.create` and `values.batchUpdate` reference pages, and the Drive `files.list` reference page, all accept `drive.file`. Creating, reading and writing the app's own sheet is confirmed.
- The Drive scope guide describes `drive.file` as per-file access to files "you open with an app" or create with it.
- The Picker docs require `setAppId(<Cloud project number>)` to register the per-file grant. That shows the grant belongs to the **Cloud project**, not the device or the install.
- `files.list` "returns all files by default, including trashed files."
- The PRD (line 105) requires that a reinstall or a new phone finds the existing sheet and creates no second one.

**Gaps in the spine:**
- (a) The Environments section says debug and release each get their own OAuth client. If those clients sit in different Cloud projects, a sheet created by a debug build is invisible to the release build, and testers get a second sheet.
- (b) A sheet the user made by hand, or one created by today's app (which uses a user-supplied client ID, so a different project), is invisible under `drive.file`. Android has no native Drive Picker to grant access to it.
- (c) AD-11's "find or create" doesn't say how the file is found:
  - by name, which breaks if the user renames it;
  - whether trashed files are excluded;
  - what happens when more than one file matches.

**Suggested fix:** add a rule to AD-10 or AD-11, plus a deployment note:

- Every OAuth client (debug and release Android clients) lives in **one Google Cloud project**.
- On create, the app tags the file with a Drive `appProperties` marker, for example `expenseTracker=v1`.
- Discovery uses `files.list` with `q = "mimeType='application/vnd.google-apps.spreadsheet' and appProperties has { key='expenseTracker' and value='v1' } and trashed=false"`.
- If several files match, the oldest by `createdTime` wins.
- Sheets made outside the app aren't supported in v1, and this goes in Deferred.

Record a memlog `(version)` line for the scope check.

### F5. AD-10's token handling is incomplete, especially for the WorkManager job (Medium)

**Evidence:** developer.android.com/identity/authorization says:

- Tokens last about 1 hour.
- A repeat `authorize()` returns a token silently if consent still stands.
- An invalid token should be cleared with `AuthorizationClient.clearToken(ClearTokenRequest…)`.
- When consent is needed, `authorize()` returns `hasResolution()` with a `PendingIntent` that only an Activity can launch.
- `revokeAccess()` revokes **all** scopes.

The WorkManager guide documents `Result.retry()`, backoff and unique work.

**Impact:**
- The phrase "requests a new one silently when it expires" can loop. If a Sheets call returns 401 and the app calls `authorize()` again without clearing the cached token, it may get the same rejected token back.
- The AD-14 worker runs with no UI, so it can't resolve consent and has no defined behaviour when it needs to.
- When the phone has several Google accounts, the request should target the account that signed in through Credential Manager.

**Suggested fix:** extend AD-10 and AD-14:

- `AccountGateway.getAccess()` targets the signed-in account with `AuthorizationRequest.Builder.setAccount(...)`.
- On an HTTP 401 the gateway calls `clearToken`, then calls `authorize()` once more.
- If the result needs a resolution while running in the worker, the gateway returns the domain failure `NeedsUserConsent`. The worker then returns `Result.failure()` and the app prompts on the next foreground sync.
- Sign-out uses `revokeAccess` only as an explicit "disconnect", because it revokes every scope.
- The worker is enqueued as unique work with `NetworkType.CONNECTED`, `ExistingWorkPolicy.KEEP`, and exponential backoff.

## Smaller notes (not counted as findings)

- **Memlog wording:** line 38 says "Sheets REST v4 via Retrofit + Bearer token still supported" but names no source URL. Add the reference URL so the check can be audited.
- **Unsourced detekt claim:** the memlog's claim that "detekt 1.23.8 lacks AGP 9/Kotlin 2.2 support" has no source. It doesn't affect any AD, because AD-17 uses a Gradle check.

## Sources

- Google Maven metadata, fetched 2026-10-08:
  - [compose-bom](https://dl.google.com/android/maven2/androidx/compose/compose-bom/maven-metadata.xml)
  - [credentials](https://dl.google.com/android/maven2/androidx/credentials/credentials/maven-metadata.xml)
  - [play-services-auth](https://dl.google.com/android/maven2/com/google/android/gms/play-services-auth/maven-metadata.xml)
  - [work-runtime-ktx](https://dl.google.com/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml)
  - [googleid](https://dl.google.com/android/maven2/com/google/android/libraries/identity/googleid/googleid/maven-metadata.xml)
  - [navigation-compose 2.10.2 module](https://dl.google.com/android/maven2/androidx/navigation/navigation-compose/2.10.2/navigation-compose-2.10.2.module)
  - [hilt-navigation-compose 1.4.0 POM](https://dl.google.com/android/maven2/androidx/hilt/hilt-navigation-compose/1.4.0/hilt-navigation-compose-1.4.0.pom)
  - [AGP](https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml)
  - [room-runtime](https://dl.google.com/android/maven2/androidx/room/room-runtime/maven-metadata.xml)
  - [room3-runtime](https://dl.google.com/android/maven2/androidx/room3/room3-runtime/maven-metadata.xml)
- Maven Central metadata, fetched 2026-10-08:
  - [okhttp](https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml)
  - [retrofit](https://repo1.maven.org/maven2/com/squareup/retrofit2/retrofit/maven-metadata.xml)
  - [retrofit 2.12.0 POM](https://repo1.maven.org/maven2/com/squareup/retrofit2/retrofit/2.12.0/retrofit-2.12.0.pom)
  - [retrofit 3.0.0 POM](https://repo1.maven.org/maven2/com/squareup/retrofit2/retrofit/3.0.0/retrofit-3.0.0.pom)
  - [moshi](https://repo1.maven.org/maven2/com/squareup/moshi/moshi/maven-metadata.xml)
  - [kotlin-stdlib](https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml)
  - [KSP plugin](https://repo1.maven.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml)
  - [hilt-android](https://repo1.maven.org/maven2/com/google/dagger/hilt-android/maven-metadata.xml)
  - [robolectric](https://repo1.maven.org/maven2/org/robolectric/robolectric/maven-metadata.xml)
  - [roborazzi](https://repo1.maven.org/maven2/io/github/takahirom/roborazzi/roborazzi/maven-metadata.xml)
- Release notes:
  - [Dagger releases](https://github.com/google/dagger/releases)
  - [androidx.credentials releases](https://developer.android.com/jetpack/androidx/releases/credentials)
- Android guides:
  - [Android Authorization guide (AuthorizationClient)](https://developer.android.com/identity/authorization)
  - [WorkManager: define work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- Google Workspace API reference and guides:
  - [Sheets spreadsheets.create scopes](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/create)
  - [Sheets values.batchUpdate scopes](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets.values/batchUpdate)
  - [Sheets API scopes](https://developers.google.com/workspace/sheets/api/scopes)
  - [Drive files.list](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list)
  - [Drive: choose scopes](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)
  - [Drive Picker, setAppId with the Cloud project number](https://developers.google.com/workspace/drive/picker/guides/web-picker)
