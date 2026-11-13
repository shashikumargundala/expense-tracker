---
title: 'Missing-Sheet recovery and offline cold start'
type: 'feature'
ticket: '4'
created: '2026-11-13'
status: 'built'
baseline_revision: '5f259d71eb713609936c42ecf02c970c0399afc4'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 1
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** If the stored Expense Sheet is deleted or trashed during a session, nothing notices, and epic 3's sync has no way to recover (AD-20 "Missing Sheet"). There's also no check that a signed-in cold start works with no network.

**Approach:** Add `ExpenseSheetResult.SheetMissing` and an `ExpenseSheetGateway.check(sheetId)` that returns it on a Drive 404 or a trashed file. Add `RecoverExpenseSheetUseCase`, which epic 3's sync calls. It checks the stored Sheet; when that Sheet is missing, it runs `EnsureExpenseSheetUseCase`'s find-or-create for the stored account and stores the new Sheet ID. Confirm that the stored session opens Home offline.

## Boundaries & Constraints

**Always:**
- `:domain` has no Google types (AD-11). Drive calls go through `googleApiCall`. Nothing logs IDs, emails or tokens.
- Recovery reuses `EnsureExpenseSheetUseCase` for consent, find-or-create and the save; it doesn't copy that logic.
- Use cases return sealed results and never throw (except cancellation).

**Never:**
- No sync engine, sync triggers or caller of recovery in `:app`; epic 3 entries 1 and 6 wire it in.
- No new tabs, columns or Room schema change.
- No UI change.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Sheet present | Session with sheetId; `check` is Ready | `Ready(sameId)`; no find, create or save | none |
| Sheet deleted | `check` → SheetMissing; `find` NotFound | One `create`; session saved with the new ID; `Ready(newId)` | none |
| Sheet trashed, another tagged | `check` → SheetMissing; `find` Ready(other) | Session saved with `other`; no create | none |
| No session | Session null or sheetId null | `Failure(Access)`; gateway not called | none |
| Check fails | `check` → Network / Access / Unexpected | The same failure; session unchanged | none |
| Consent needed | `check` → NeedsConsent | Asks consent once, retries `check`; declined → `ConsentDeclined` | Nothing saved |
| Gateway check | Drive `files.get` 404, or `trashed: true` | `SheetMissing` | Other HTTP codes → `Failure(Unexpected)` |

</frozen-after-approval>

## Code Map

- `domain/.../account/ExpenseSheetResult.kt` -- sealed result; add `SheetMissing` (from `check` and, later, epic 3's reads/writes).
- `domain/.../account/ExpenseSheetGateway.kt` -- `find`, `create`; add `check(sheetId)`. KDoc lists the results each call returns.
- `domain/.../account/EnsureExpenseSheetUseCase.kt` -- `invoke(accountId, email)`. Its private `withConsent(call)` (ask once, retry once, a still-needed consent after a grant is `Failure(Access)`) and `toOutcome()` are the only copy of that logic: make both `internal` and have `RecoverExpenseSheetUseCase` call them through its injected `EnsureExpenseSheetUseCase` (`ensure.withConsent { sheets.check(id) }`, then `toOutcome()` for the non-Ready, non-SheetMissing results). `toOutcome` maps `SheetMissing` to `Failure(Unexpected)` (find and create never return it). Its class KDoc says "After Google sign-in"; reword it to cover recovery during a session too.
- `domain/.../account/SessionRepository.kt` -- `observe().first()` reads the stored session.
- `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` -- add `checkResult` (null means `Ready(sheetId)`), `checkCalls` and `checkedIds`.
- `domain/src/test/.../EnsureExpenseSheetUseCaseTest.kt:68` -- private `ConsentingGateway` implements the interface; add `check`.
- `domain/src/test/.../AccountFakes.kt` -- `FakeSessionRepository`, `FakeAccountGateway` (consent scripting); reuse.
- `data/.../google/DriveService.kt` -- add `getFile(fileId, fields)` (`GET files/{fileId}`).
- `data/.../google/DriveModels.kt` -- `DriveFile`; add `trashed: Boolean?`.
- `data/.../google/SheetsExpenseSheetGateway.kt` -- implement `check`; reuse `ApiFailure.toResult()` but map `Http(404)` to `SheetMissing` first.
- `data/src/test/.../google/SheetsExpenseSheetGatewayTest.kt` -- mockwebserver3 harness; add cases.
- `app/.../StartViewModel.kt` -- the cold start reads Room only (`observeSession().first()`), so no change. `MainActivityTest` already opens Home from the fake signed-in session; nothing asserts the launch makes no gateway call.
- `app/src/test/.../account/SignInGateTest.kt`, `FakeAccountModule.kt` -- the Hilt-injected `FakeExpenseSheetGateway` and the fake account gateway let a test count calls.

## Tasks & Acceptance

**Execution:**
- [x] `domain/.../account/ExpenseSheetResult.kt`, `ExpenseSheetGateway.kt` -- add `SheetMissing` and `check(sheetId)` with KDoc -- the hook epic 3 calls
- [x] `domain/.../account/RecoverExpenseSheetUseCase.kt` -- new use case per the matrix, returning `EnsureExpenseSheetResult` (Ready, ConsentDeclined, Failure) so epic 3 handles one result type; consent and outcome mapping come only from `EnsureExpenseSheetUseCase`'s internal `withConsent` and `toOutcome`, with no copy -- AD-20, the no-copy rule
- [x] `domain/.../account/EnsureExpenseSheetUseCase.kt` -- handle `SheetMissing` in `toOutcome`; make `withConsent` and `toOutcome` internal; reword the class KDoc -- exhaustive `when`, one copy
- [x] `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt`, `EnsureExpenseSheetUseCaseTest.kt` -- fakes implement `check` -- compile and script
- [x] `domain/src/test/.../RecoverExpenseSheetUseCaseTest.kt` -- one test per matrix row above the gateway row, plus: consent granted on `check` whose retry is `SheetMissing` runs find-or-create and stores the new ID; the deleted-Sheet test also asserts `findCalls == 1` and `checkedIds` -- verify line
- [x] `data/.../google/DriveService.kt`, `DriveModels.kt`, `SheetsExpenseSheetGateway.kt` -- `files.get` with `fields=id,trashed`; 404 or trashed → `SheetMissing` -- real detection
- [x] `data/src/test/.../SheetsExpenseSheetGatewayTest.kt` -- present, trashed, 404, 500 and network cases; assert the path and `fields` -- the contract
- [x] `data/src/test/resources/google/drive-file-get.json`, `GoogleApiModelsTest.kt` -- a recorded `files.get` sample with `trashed` parses -- models convention
- [x] `app/src/test/.../account/SignInGateTest.kt` -- a stored session with a Sheet ID launches to Home, then `composeRule.waitForIdle()` before asserting zero sign-in, access, find, create and check calls -- offline cold start

**Acceptance Criteria:**
- Given a signed-in session whose Sheet was deleted in Drive, when `RecoverExpenseSheetUseCase` runs online, then exactly one new tagged Sheet exists and the stored session holds its ID.
- Given a signed-in session, when the app cold-starts in airplane mode, then Home opens with no Sign-in screen and no error.

## Implementation Notes

- Device check (2026-11-13, S23, debug build of 9c651c0): with airplane mode on, a force-stopped app cold-started to Home in 555 ms with no Sign-in screen, no error and no crash in logcat. Recovery itself isn't reachable from the app until epic 3, so its Drive behaviour is covered only by the mockwebserver3 tests.

## Plan Change Log

### Loop 1 (2026-11-13, bad_plan)

- **Trigger:** review finding 1. `RecoverExpenseSheetUseCase.checkWithConsent` was a line-for-line copy of `EnsureExpenseSheetUseCase.withConsent`, and its result `when` rebuilt `toOutcome`, against the Always rule "doesn't copy that logic". The Code Map called both helpers private without saying how recovery should reuse them.
- **Amended:** Code Map (the `EnsureExpenseSheetUseCase` line) and Tasks. `withConsent` and `toOutcome` become `internal` and recovery calls them through its injected `EnsureExpenseSheetUseCase`. Also amended: the stale class KDoc, a test for consent granted on `check` followed by `SheetMissing`, `findCalls`/`checkedIds` assertions, and `waitForIdle()` in the cold-start test.
- **Known-bad state avoided:** two copies of the consent and outcome rules that drift apart.
- **KEEP:** everything else in the first attempt matched the plan and passed: `SheetMissing` and its KDoc; `check(sheetId)` and its KDoc; `DriveService.getFile`; `DriveFile.trashed`; `SheetsExpenseSheetGateway.check` (404 or trashed → `SheetMissing`, else `toResult()`); the fake's `checkResult`/`checkCalls`/`checkedIds`; `ConsentingGateway.check`; the five gateway tests; `drive-file-get.json` and `fetchedFileParsesTrashed`; `FakeAccountGateway.accessCalls`; and the 10 recovery tests. Reuse them as they are. The first attempt's diff is at `/tmp/story-2.4-attempt1.diff` for reference.

## Review Triage Log

### Pass 1 (2026-11-13, quick): 0 high, 2 medium, 3 low, 2 false, 1 maybe-false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `checkWithConsent` and the result `when` copy `EnsureExpenseSheetUseCase`'s `withConsent` and `toOutcome` | medium | bad_plan | Real: same calls, retry and mapping, against the Always rule "doesn't copy that logic"; a later change to the consent rule in one copy wouldn't reach recovery. Loop 1 amends the Code Map. |
| 2 | Recovery's save can write the old session back after sign-out | maybe-false | defer | Only if story 3.13's sign-out erases without cancelling and joining a running recovery first. Room's suspend `save` checks cancellation, so a cancelled-and-joined job can't save. Settled when 3.13 is built. Rated medium if true (unverified). |
| 3 | CLAUDE.md and the Copilot instructions don't list `getFile`, `trashed`, `check` or `RecoverExpenseSheetUseCase` | low | defer | Real; the fix edits agent-context files. |
| 4a | The cold-start test asserts call counts right after Home appears | low | patch | Real: no idle wait, so a call started just after the first Home frame could slip past; the fix is one `waitForIdle()`. Carried into loop 1's tasks. |
| 4b | The cold-start test doesn't simulate being offline | false | reject | The gateways are Hilt fakes, so zero calls means no network use; the airplane-mode check is the plan's manual S23 step. |
| 4c | Nothing asserts "no error" on Home | false | reject | Home has no error UI to show; `signin_error` exists only on Sign-in, and the test asserts Sign-in is absent. |
| 5 | `EnsureExpenseSheetUseCase`'s class KDoc still says "After Google sign-in" | low | patch | Real; recovery now calls it during a session. Carried into loop 1's Code Map. |
| 6 | No test for consent granted on `check` followed by `SheetMissing`; the deleted test doesn't assert `findCalls` | medium | patch | Real: the only path where consent runs on `check` and then again inside ensure is untested. Carried into loop 1's tasks. |

### Pass 2 (2026-11-13, quick, after loop 1): 0 high, 0 medium, 2 low

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | CLAUDE.md and the Copilot instructions don't list `getFile`, `trashed`, `check`, `SheetMissing` or `RecoverExpenseSheetUseCase` | low | defer | carried: pass 1 row 3; the fix edits agent-context files. Recorded in `deferred-work.md`. |
| 2 | `missing sheet with a failing save is StorageFailed` asserts only the result | low | patch | Real: unlike the other failure tests, it doesn't check that the stored session is unchanged or which path ran. Patched with the session, `checkedIds` and `findCalls` assertions. |

Pass 1 row 2 (the sign-out race) is recorded in `deferred-work.md` as an unverified medium for story 3.13.

## Design Notes

`check` is the only producer of `SheetMissing` until epic 3 adds tab reads. Epic 3's sync calls `RecoverExpenseSheetUseCase` when a read returns `SheetMissing`, or before a run. With no resumed activity, `AccountGateway.requestConsent` returns false, so background recovery reports `ConsentDeclined` without prompting (AD-10).

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks (if no CLI):**
- On the S23, signed in, with airplane mode on: force-stop and launch; Home opens.
