---
title: 'Refactor sweep (epic 2)'
type: 'refactor'
ticket: '8'
created: '2026-11-13'
status: 'built'
baseline_revision: '0e7966ab7c88eeb796001cbff62930776eb9f89f'
route: 'oneshot'
route_source: 'auto'
risk: 'low'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 2's plans and reviews left open items in `_bmad-output/initiative-expense-tracker/deferred-work.md`. Most are stale agent-context docs. There's also a missing unit test, and a lint crash that keeps recurring.

**Approach:** Resolve each item below, or record why it stays open. Then add a "Resolved in story 2.8" section to `deferred-work.md` with one line per item, as story 1.8 did. Cleanup only, with no behaviour change.

1. **CLAUDE.md and `.github/copilot-instructions.md`.** The two files stay identical. Fix the six stale spots:
   - **Screenshots note:** list Home, Sign-in and Settings (deferred-work line 57).
   - **`AccessResult`:** `getAccess()` returns only `Granted`, `NeedsUserConsent` or `Failure(NotSignedIn | Unavailable)`. `Unauthorized` arises only as `ApiFailure.Access` after the one retry (line 60).
   - **Web client ID and Sharing an APK bullets:** add the release Android client (GOOGLE_CLOUD_SETUP step 8), and drop the ambiguous "until the Play release (story 1.7)" (line 63).
   - **Story 2.3:** `ExpenseSheetGateway` (find, create) and `EnsureExpenseSheetUseCase`; `AccountGateway.requestConsent`; `SignInUseCase` storing the session only once the Sheet ID is known; the gate opening Home only for a session with a Sheet ID; the `:domain` `testFixtures` source set with `FakeExpenseSheetGateway` (line 66).
   - **Story 2.4:** `DriveService.getFile`, `DriveFile.trashed`, `ExpenseSheetGateway.check`, `SheetMissing`, and `RecoverExpenseSheetUseCase`, which reuses `EnsureExpenseSheetUseCase`'s internal `withConsent`/`toOutcome` and is the hook epic 3's sync calls (line 72).
   - **Story 2.6:** Settings is no longer a placeholder (the Google account tile, and `settings/OpenSheet.kt` opening the Sheets app with Play Store fallbacks); the tags gain `settings_account` and `settings_open_sheet`; the screenshots gain `SettingsScreenshotTest` (line 75).
2. **`DriveAuthorization.useAccount`** (story 2.3 noted it has no unit test). Add a test that the email set by `useAccount` is the account `getAccess()` authorizes, ahead of the stored session's email. Use the existing `DriveAuthorizationMapping`/fake seams; no production change.
3. **The lint crash.** "Unexpected failure during lint analysis" in `app/src/test/.../ExpenseTrackerAppTest.kt` hit the first full build of stories 2.4 and 2.6, then passed on re-run. Try to reproduce it with a clean `:app:lintDebug`. If a cause is found, fix it with a minimal change; otherwise record the symptom and how to re-run it.
4. **Story 3.13's sign-out ordering** (line 69). Record it as carried to epic 3 entry 13; there's no code change here.

</frozen-after-approval>

## Implementation Notes

The oneshot route fits: the change is docs, one test, and an investigation, at about 100 lines.

- **Docs:** CLAUDE.md and `.github/copilot-instructions.md` got the same eleven text changes, so they still differ only in their three header lines. `SignInUseCase`'s old "stores the session only on success" is gone; the new Expense Sheet bullets describe the store-once flow.
- **Test:** `DriveAuthorizationTest` gains `useAccountAuthorizesThatEmailAheadOfTheStoredSession` and `useAccountWorksBeforeAnySessionIsStored`. There is no production change.
- **Lint crash:** not reproduced. Two `:app:lintDebug --rerun-tasks` runs passed. It's recorded in `deferred-work.md` with how to capture it next time.
- **Sign-out ordering:** carried to epic 3 entry 13 in the "Resolved in story 2.8" section of `deferred-work.md`.

## Verification

**Commands:**
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)
- `diff CLAUDE.md .github/copilot-instructions.md` -- expected: no difference beyond the lines that already differed before this story (check against the baseline)

## Review Triage Log

### Pass 1 (2026-11-13, quick): 0 high, 0 medium, 3 low, 1 false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | The "Tests" bullet still says `FakeAccountModule` gives a "signed-in" session and a `FakeAccountGateway` only | low | patch | Real: it now also binds `FakeExpenseSheetGateway`, and the gate needs a Sheet ID. Both files now name `SignedInTestSession`, the scripting fields and the fake gateway, and say a session without a Sheet ID lands on Sign-in. |
| 2 | The open lint-crash entry sits under "Resolved in story 2.8" | low | patch | Real: it read as resolved. Moved it above the section and added a "recorded" line inside. |
| 3 | The Screenshots bullet has no separator before "Sign-in" | low | patch | Real: a missing comma; added it in both files. |
| 4 | The `.idea` changes could be swept into the commit | false | reject | The commit stages named paths only; `.idea` is never added. |
