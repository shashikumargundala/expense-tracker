---
title: 'Find or create the Expense Sheet at sign-in'
type: 'feature'
ticket: '3'
created: '2026-10-31'
status: 'built'
baseline_revision: 'facae2399ba853c5ea4d29579ab6a08e55ce69e8'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Sign-in stores a session with no Sheet ID, so the app has no Expense Sheet to sync with, and the gate opens Home for any stored session.

**Approach:** Add `ExpenseSheetGateway` (find, create) in `:domain` with a Drive/Sheets implementation in `:data`, and `EnsureExpenseSheetUseCase`, which sign-in runs after Google sign-in, launching the consent prompt when needed. It saves the Sheet ID in the session, and the gate opens Home only for a session that has one.

## Boundaries & Constraints

**Always:**
- No Google type in a `:domain` signature (AD-11); Drive and Sheets calls go through `googleApiCall`.
- Find is `files.list` on `appProperties has { key='expenseTracker' and value='v1' }` with `trashed=false`; the oldest `createdTime` wins.
- Create writes AD-7's five tabs, headers and the five default Categories first, and sets the marker last, so a half-built file is never found.
- Tokens, emails and URLs are never logged.
- Use cases return a sealed result and never throw.

**Never:**
- Reading or writing Expense rows, sign-out, and the missing-Sheet recovery (2.4).
- Changing the Room schema (`session.sheet_id` already exists).
- Account switching.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| First sign-in | No tagged Sheet in Drive | Creates one Sheet with five tabs, headers and Categories, marks it, saves its ID, opens Home | none |
| Re-sign-in | One or more tagged Sheets | Reuses the oldest, saves its ID | none |
| Consent needed | `getAccess()` is `NeedsUserConsent` | Consent prompt in the foreground, then continues | Declined: stays on Sign-in, no message, nothing saved |
| Network failure | Find or create fails | Stays on Sign-in with "Couldn't sign in. Check your connection and try again." | Nothing saved |
| Partial create | Create fails before the marker | No tagged file exists, so the next attempt creates afresh | Failure result |
| Old session | Stored session without a Sheet ID | Gate shows Sign-in | none |

**Decisions (owner, 2026-11-01):**
1. **Consent declined** is silent, like cancel: the user stays on Sign-in with no message.
2. **Sheet title** is "Expense Tracker".
3. **Session timing:** the session is saved only once the Sheet ID is known, so `SignInUseCase` no longer saves right after Google sign-in, and a failed setup leaves nothing stored.

</frozen-after-approval>

## Code Map

- `domain/.../account/` -- `Session`, `SessionRepository`, `AccountGateway`, `AccessResult`, `ConsentRequest`, `SignInUseCase` (saves a session with no `sheetId`); add `ExpenseSheetGateway`, `ExpenseSheetResult`, `EnsureExpenseSheetUseCase`, and `AccountGateway.requestConsent(request)`.
- `domain/build.gradle.kts` -- plain `kotlin.jvm`, no test fixtures; add `java-test-fixtures` for the fake gateway.
- `data/.../google/` -- `DriveService.listFiles` (no `spaces`; `FileList` has no `nextPageToken`), `updateFile` (sets `appProperties`), `SheetsService.createSpreadsheet`, `batchUpdateValues`; add `ExpenseSheetLayout` (tabs, headers, Categories) and `SheetsExpenseSheetGateway`.
- `data/.../account/` -- `CredentialManagerAccountGateway`, `ResumedActivityTracker.current`, `PendingIntentConsentRequest`, `AccountModule` (bindings), `RoomSessionRepository.save` (writes `sheetId`).
- `app/.../account/` -- `SignInViewModel`, `SignInUiState`, `SignInScreen`; `StartViewModel` (gate: `session?.sheetId != null`).
- `app/src/test/.../account/FakeAccountModule.kt` -- `SignedInTestSession` has no `sheetId`, so it needs one; bind a fake `ExpenseSheetGateway`.

## Tasks & Acceptance

**Execution:**
- [ ] `domain/.../account/ExpenseSheetGateway.kt`, `ExpenseSheetResult.kt` -- find-or-create interface and sealed result -- AD-11
- [ ] `domain/.../account/EnsureExpenseSheetUseCase.kt`, `SignInUseCase.kt` -- `getAccess()`, `requestConsent`, gateway, then save the whole session once with its `sheetId`; `SignInUseCase` stops saving on its own -- one place for the sequence, decision 3
- [ ] `domain/.../account/AccountGateway.kt` -- add `requestConsent(request)`; update fakes and callers -- consent design from 2.2
- [ ] `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt`, `domain/build.gradle.kts` -- shared fake -- epic 3 reuse
- [ ] `data/.../google/ExpenseSheetLayout.kt`, `SheetsExpenseSheetGateway.kt`, `data/.../account/AccountModule.kt` -- implement find, create and marker-last; bind it -- the contract
- [ ] `data/.../account/CredentialManagerAccountGateway.kt` -- launch the consent `PendingIntent` through the tracked Activity -- foreground prompt
- [ ] `app/.../account/SignInViewModel.kt`, `SignInUiState.kt`, `SignInScreen.kt` -- run the use case after sign-in; "Setting up your Expense Sheet…" progress; failure line
- [ ] `app/.../StartViewModel.kt` -- `SignedIn` only with a `sheetId`
- [ ] Tests -- use case, layout and gateway against `mockwebserver3`, `SignInGateTest`, `FakeAccountModule` with a Sheet ID -- cover the matrix

**Acceptance Criteria:**
- Given a first sign-in with consent granted, when it finishes, then exactly one tagged Sheet exists with five tabs, headers and Categories, and Home opens.
- Given app data was cleared, when the owner signs in again, then the same Sheet is reused.
- Given the Sheet was deleted and app data cleared, when the owner signs in, then a fresh Sheet is created.

## Implementation Notes

- Decision 3 (save the session only once the Sheet ID is known) meant `DriveAuthorization` could no longer read the email from a stored session during sign-in, so it gained an in-memory `useAccount(email)`, set by `signIn()`. It has no unit test, and the sign-out work (epic 3) should clear it.
- "Setting up your Expense Sheet…" shows for the whole sign-in attempt, including while the account picker is open, because `SignInUseCase` reports no phases.
- Not checked on a device: the consent `PendingIntent` launch and the Drive `appProperties` marker need the S23 check.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-01, quick)

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `requestConsent` never returns if the activity is destroyed during the prompt | medium | patch | Real: the callback is registered on the activity's registry with no lifecycle owner, so a destroyed activity never resumes the continuation, and the ViewModel's scope outlives it, leaving the spinner up. The implementer's note called this a silent decline, which was wrong. Fix: resume with false on ON_DESTROY. |
| 2 | `DriveAuthorization.signedInEmail` is never cleared | low | reject | `signIn()` overwrites it on every success and a cold start has it null, so no reachable path reads a stale value while there's no account switching. Sign-out belongs to epic 3; the KDoc already says "in progress". |
| 3 | A granted-but-still-`NeedsConsent` retry is reported as `ConsentDeclined` | medium | patch | Real: `toOutcome()` maps every `NeedsConsent` to `ConsentDeclined`, so the user sees silence after accepting. Fix: a second `NeedsConsent` after a granted prompt becomes `Failure(Access)`. |
| 4 | The test "find needing consent asks once and retries" covers only the declined branch | medium | patch | Real: it asserts `findCalls == 1` and never exercises grant-then-retry, so the `withConsent` retry was unverified. Replaced with three tests. |
| 5 | CLAUDE.md and the Copilot instructions don't describe `ExpenseSheetGateway`, `EnsureExpenseSheetUseCase`, `requestConsent`, the Sheet-ID gate or `testFixtures` | medium | defer | Real, but the fix edits agent-context files. Recorded in `deferred-work.md`. |
| 6 | `testFixturesImplementation(coroutines)` is redundant | low | patch | Real: main's `api` coroutines dependency already reaches the fixtures; the fix is a deletion. |
| 7 | Import order in `AccountModule.kt` and `CredentialManagerAccountGateway.kt` | low | patch | Real and cosmetic; a direct correction. |

## Design Notes

Category rows (light colours, DESIGN.md): Food `#F59E0B` restaurant; Transport `#10B981` directions_car; Shopping `#E11D48` shopping_bag; Bills `#3B82F6` receipt_long; Fun `#A855F7` celebration. AD-7 headers: Expenses `ID, Date, Amount, Category, Description, Source, SMS ID`; Categories `Name, Color, Icon`; Budgets `Month, Category, Amount`; Dismissed `SMS ID, Dismissed On`; Settings `Key, Value`. The date column uses `USER_ENTERED`.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks (if no CLI):**
- On the S23, signing in as a test user creates one tagged Sheet in the owner's Drive.
