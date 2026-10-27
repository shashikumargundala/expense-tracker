---
title: 'Drive access token and Google API clients'
type: 'feature'
ticket: '2'
created: '2026-10-28'
status: 'built'
baseline_revision: 'b9ef2f4ff41feb2b4562cd22a3dcb19a46e715f1'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The signed-in app can't reach Google Drive or Sheets yet. Story 2.3's find-or-create needs a `drive.file` access token, an HTTP client that recovers from expired tokens, and typed Drive and Sheets calls.

**Approach:**
- **Access in `:domain`:** add `AccountGateway.getAccess()`, returning a sealed `AccessResult`: `Granted`, `NeedsUserConsent(request)` or `Failure(reason)`.
  - The consent request is an opaque `:domain` handle with no Google type in it. Story 2.3 launches it.
- **Access in `:data`:** implement it with `AuthorizationClient` and the `drive.file` scope for the signed-in account (`setAccount`). Cache the token in memory only.
- **HTTP client:** an OkHttp interceptor attaches the token. An OkHttp `Authenticator` answers a 401 with `clearToken`, a fresh `getAccess()`, and exactly one retry.
- **API services:** Retrofit Drive v3 and Sheets v4 services with Moshi codegen models, covering only the calls story 2.3 needs.

## Boundaries & Constraints

**Always:**
- **Tokens:** never stored on disk, and never logged. The same goes for the email address and the Authorization header (Logging convention). No logging interceptor sends headers or bodies.
- **Errors:** Google, network and HTTP exceptions are caught in `:data` and mapped to domain failures (Errors convention). Coroutine cancellation is rethrown.
- **Libraries:** everything goes through the catalog. Add `kotlinx-coroutines-play-services` (to await Play Tasks) and OkHttp `mockwebserver3`, both at existing version refs.
- **API coverage:**
  - Drive: `files.list` (with `q`, `orderBy`, `fields`, `pageSize`) and `files.update` (to set `appProperties`).
  - Sheets: `spreadsheets.create` and `spreadsheets.values.batchUpdate`.
  - Base URLs are `https://www.googleapis.com/drive/v3/` and `https://sheets.googleapis.com/v4/`.

**Never:**
- Finding, creating or recovering the Expense Sheet, or launching the consent prompt (stories 2.3 and 2.4).
- Any scope other than `drive.file`.
- A refresh token, client secret or OAuth redirect (AD-10).
- R8 keep rules (story 2.7).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Token granted | Authorization succeeds | `Granted`; calls carry `Authorization: Bearer <token>` | No error expected |
| Token cached | Second call, same session | No new authorize call | No error expected |
| Expired token | Server answers 401 once | clearToken, fresh token, one retry, success | No error expected |
| Still 401 | Server answers 401 twice | Exactly one retry, then a mapped auth failure | No loop |
| Consent needed | Authorization has a resolution | `NeedsUserConsent(request)` with an opaque handle | No call is sent |
| Signed out | No stored session | `Failure(NotSignedIn)` | No authorize call |

</frozen-after-approval>

## Code Map

- `domain/.../account/AccountGateway.kt`: has only `signIn()`. Add `getAccess(): AccessResult`.
- `domain/.../account/`: the `SignInResult` and `SignInFailure` pattern to copy for `AccessResult` and `AccessFailure`.
- `data/.../account/CredentialManagerAccountGateway.kt`: implements `AccountGateway`. It gets `getAccess()` too, by delegating to a new `DriveAuthorization` class, so the gateway file stays small.
- `data/.../account/RoomSessionRepository.kt`: the source of the signed-in email for `setAccount`.
- `data/.../account/AccountDataModule.kt`: the Hilt provides pattern. Add a `data/google/GoogleApiModule` providing OkHttp, the two Retrofits and Moshi.
- `data/build.gradle.kts`: add retrofit, converter-moshi, the okhttp BOM and okhttp, moshi, moshi codegen through KSP, coroutines-play-services, and in tests mockwebserver3.
- `domain/src/test/.../account/` and `data/src/test/.../account/`: the existing fakes and Robolectric setup.

## Tasks & Acceptance

**Execution:**
- [x] `gradle/libs.versions.toml`, `data/build.gradle.kts`: add the libraries.
- [x] `domain/.../account/AccessResult.kt`, `ConsentRequest.kt` and `AccountGateway.kt`: the sealed result, an opaque `ConsentRequest` interface, and `getAccess()`. Update the domain test fakes.
- [x] `data/.../account/DriveAuthorization.kt`: `AuthorizationClient`, `drive.file`, `setAccount(Account(email, "com.google"))`, the in-memory token cache behind a `Mutex`, and `invalidate(token)` calling `clearToken`. Maps the result to `AccessResult`, with a `:data` `ConsentRequest` implementation wrapping the `PendingIntent`.
- [x] `data/.../google/AuthInterceptor.kt` and `TokenAuthenticator.kt`: attach the bearer token. On 401, retry only when the request had no prior retry (`priorResponse == null`), after invalidating and re-fetching. No logging of headers or bodies.
- [x] `data/.../google/DriveService.kt`, `SheetsService.kt` and the Moshi models (`DriveFile`, `FileList`, `Spreadsheet`, `SheetProperties`, `ValueRange` and `BatchUpdateValuesRequest`, with only the fields used), plus `GoogleApiModule`.
- [x] `data/src/test/.../google/`: MockWebServer tests covering each matrix row through the real OkHttp client with a fake token source. They assert the header, a single retry, no infinite loop, and that no captured log line contains the token. Add parsing tests for each model against a recorded JSON sample.
- [x] `data/src/test/.../account/DriveAuthorizationTest.kt`: the result mapping (granted, resolution, failure) through a testable mapping function, the cache, and invalidation.
- [x] `CLAUDE.md`, `.github/copilot-instructions.md`: the `data/google` package, how to call Drive and Sheets, and the token rules.

**Acceptance Criteria:**
- Given `./gradlew build verifyRoborazziDebug`, when it runs, then it passes, with every screenshot baseline unchanged.
- Given the new tests, when they run, then each I/O matrix row is covered by a passing test.

## Implementation Notes

- `AccessTokenSource` (`data/google`) is the seam between `DriveAuthorization` and the HTTP client; `TokenResult.Token` redacts itself in `toString()`. `AccessResult.Granted` carries no token.
- `DriveAuthorization` takes an internal `Authorizer` (the Play services calls) so the cache and invalidation are unit-tested; the `@Inject` secondary constructor builds the real one. The cache is keyed by account email, so a different account never reuses a token.
- With no token, `AuthInterceptor` (and `TokenAuthenticator` after a 401) throws `GoogleAccessDeniedException`, so nothing is sent. `googleApiCall` maps every outcome to `ApiResult` / `ApiFailure` (`NeedsUserConsent`, `Access(AccessFailure)`, `Network`, `Http(code)`, `Unexpected`); a 401 after the single retry is `Access(Unauthorized)`.
- `AccessFailure` is `NotSignedIn`, `Unauthorized`, `Unavailable`. Extra models `SpreadsheetProperties` and `Sheet` are needed for the Sheets JSON shape. The OkHttp client carries a `@GoogleApi` qualifier.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-10-28, quick)

Verdicts: medium 1, low 2. Routes: patch 3 (one partly deferred).

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `invalidate()` calls `clearToken` after releasing the Mutex, so a concurrent fetch can re-cache the rejected token and the retry 401s again | medium | patch | Real: the lock window is visible in `DriveAuthorization`, and the fakes hand out fresh tokens synchronously, so no test saw it. `clearToken` now runs under the lock; a race test was added. |
| 2 | `AccessDeniedException` shadows the default-imported `kotlin.io.AccessDeniedException` | low | patch | Real trap for story 2.3's code in another package. Renamed `GoogleAccessDeniedException`. |
| 3 | `AccessFailure.Unauthorized` is documented as a `getAccess()` outcome but only arises from `googleApiCall` | low | patch + defer | Domain KDoc corrected. The same wording in CLAUDE.md and the Copilot instructions is deferred (agent-context files). |

## Design Notes

- **Launching consent in story 2.3:** `ConsentRequest` is opaque to `:domain` and `:app`. Story 2.3 adds `AccountGateway.requestConsent(request)`, implemented in `:data` by launching the wrapped `PendingIntent` through the tracked Activity's `ActivityResultRegistry`. That's why 2.2 only has to carry the request, not launch it.
- **Single retry:** OkHttp calls the `Authenticator` again for every 401. Returning null when `response.priorResponse != null` caps it at one retry.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout. Don't commit, push or open a PR; the main session does that after review.

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest`: expected to pass, including the matrix tests.
- `./gradlew build verifyRoborazziDebug`: expected to pass.
