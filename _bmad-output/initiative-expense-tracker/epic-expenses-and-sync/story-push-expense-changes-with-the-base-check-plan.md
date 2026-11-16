---
title: 'Push Expense changes with the base check'
type: 'feature'
ticket: '4'
created: '2026-11-16'
status: 'built'
baseline_revision: 'b34fdc3f0843a4e58179f9cd00c303e6f993663e'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: ['{project-root}/_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The push applies `UpsertExpense` without checking whether the Sheet row changed since the last pull, and it ignores `DeleteExpense`. A pending in-app edit can overwrite a newer hand edit, and deletes never reach the Sheet (AD-4, CAP-3, CAP-7).

**Approach:** Complete AD-4 for Expenses:
- **Find the row:** each key's pending entries are found by ID in the fresh read and compared with their `base`. This is the mapped values as last pulled, or absent.
- **The Sheet wins:** if the row differs from the base, the entry is dropped.
- **Otherwise apply idempotently:** an `UpsertExpense` writes the mirror's current values in place, or appends them. A `DeleteExpense` deletes the row, and a missing row means nothing to do.
- **Confirm before removing:** an entry leaves the outbox only after the API confirms the write.

This story also adds the base codec that entries 7 (edit) and 10 (delete) use to record a row's base.

## Boundaries & Constraints

**Always:**
- **Comparing with the base:** a row matches its base when Date, Amount, Category (by `CategoryKey`), Description, Source (a blank cell reads as `User`) and SMS ID are all equal. Unmapped columns are ignored.
- **Base absent:** if the row is absent too, it's a create, so append. If the row is present, it equals the entry's own values (a retry that already landed) or differs (changed in the Sheet); either way the entry is dropped and nothing is written.
- **One effective change per key:** the entries for a key are applied in outbox order as one change. The last entry's type decides (Upsert or Delete). The first entry's base is the one checked. All of them leave the outbox together after success.
- **Writes and deletes:** writes touch only mapped cells and never Source on an update. All deletes go in one bottom-up `deleteExpenseRows` after the writes. The engine re-reads Expenses before the refresh when rows moved.
- **Failures:** a failed, refused or consent-needing write or delete keeps that key's entries and stops the run before the refresh, as today. A rerun recomputes from a fresh read.

**Never:**
- No Category, Budget, Dismissal or Setting pushes (entry 5).
- No Edit or Delete use cases and no UI (entries 7 and 10). They will record `base` through this story's codec.
- No schema change.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Upsert, unchanged | The row equals the base | The row is updated in place to the mirror values; the entry leaves | none |
| Upsert, changed in Sheet | The row's amount differs from the base | Nothing written; the entry is dropped; the mirror takes the Sheet's value (CAP-7) | none |
| Upsert, deleted in Sheet | The base is present; the row is gone | Nothing written; the entry is dropped; the refresh removes the mirror row | none |
| Create | The base is absent; the row is absent | Appended with Source `App`; the entry leaves | none |
| Create retried | The base is absent; the row is present with the same values | Nothing written; the entry leaves; still one row | none |
| Delete, unchanged | The row equals the base | The row is deleted from the Sheet; the entry leaves | none |
| Delete, changed in Sheet | The row differs from the base | Not deleted; the entry is dropped; the mirror gets the row back | none |
| Delete, already gone | The row is absent | Nothing to do; the entry leaves | none |
| Hand-typed row | The base was pulled from a blank-Source row; the Sheet still has a blank Source | Counts as equal; the update leaves Source as it is | none |
| Duplicated | 3.3's repair left one row for the ID | The change applies to that one row; no second row appears | none |
| Several entries for a key | Upsert(base B) then Upsert, or Upsert then Delete | One change, checked against B; all the entries leave together | none |
| Write fails | `Network` or a consent need on a write or delete | That key's entries stay; no refresh | The next run retries |

</frozen-after-approval>

## Code Map

- `data/.../sync/SyncEngine.kt` (209 lines) -- the push loop is at L99–121: it skips anything but `UpsertExpense` (L100), reads `local = expenses.get(rowKey)` (L101; a missing one is deleted at L102–104), checks `stale` (L107–112), skips invalid rows (L115), calls `writeExpense` (L116), removes the entry (L118) and tracks rows (L121). `rowsById` is L187 and `invalidIds` L194. Replace the loop with a per-key apply: group `outbox.getAll()` by `rowKey` for `OutboxType.expenseTypes`, keeping the outbox order, then do the writes, then one `deleteExpenseRows`. It will pass 500 lines, so move the push into a new `ExpensePush.kt`.
- `data/.../sync/OutboxEntity.kt` -- `OutboxType.expenseTypes` (`UpsertExpense`, `DeleteExpense`), and `base: String?` (JSON; null means absent). `OutboxDao` has `getAll` (ordered by id), `delete(id)` and `rowKeys(types)`.
- `data/.../expense/RoomExpenseRepository.kt` L46–55 -- `add` writes `base = null`. Don't change it here.
- New `data/.../sync/ExpenseBase.kt` -- a Moshi codec for `{date, amountPaise, category, description, source, smsId}` (`encode(Expense)`, `decode(String?)`), plus `matches(sheetRow: Expense, base)` using `CategoryKey`. Entries 7 and 10 call `encode`.
- `domain/.../account/ExpenseSheetGateway.kt` -- `writeExpense(sheetId, expense, existingRow)` (update in place without Source, or append) and `deleteExpenseRows(sheetId, rows)` (from 3.3) already exist; no gateway change is expected. `ExpenseRowRead` gives `id`, `expense` (null if invalid) and `sourceBlank`.
- `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` -- already models raw rows, writes, deletes and failure knobs; add only what the new cases need.
- Tests: `SyncEngineTest.kt` (354 lines) and `SyncEngineRepairTest.kt` (280) are near the limit. Add a new `ExpensePushTest.kt` for the matrix and `ExpenseBaseTest.kt` for the codec and comparison. The 3.1 test "a retry whose ID is already a row updates that row and never duplicates" now expects no write when the values are equal; update it, don't delete it.

## Tasks & Acceptance

**Execution:**
- [x] `data/.../sync/ExpenseBase.kt` -- the codec and `matches`, with Category by `CategoryKey` and a blank Source as `User` -- the base
- [x] `data/.../sync/ExpensePush.kt`, `SyncEngine.kt` -- the per-key effective change, the base check, writes then bottom-up deletes, removal after confirmation, and the re-read before the refresh -- AD-4
- [x] `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` -- any knobs the new cases need -- tests
- [x] Tests: `ExpenseBaseTest` (the round trip, Category case and spacing, blank Source); `ExpensePushTest` (every matrix row, with outbox entries inserted directly with a base); update the 3.1 retry test -- the matrix

**Acceptance Criteria:**
- Given an `UpsertExpense` and a `DeleteExpense` for different keys, each with a base equal to its Sheet row, when sync runs against the fake gateway, then the first row is updated, the second is deleted, and the outbox is empty.
- Given an Expense whose Sheet row was edited after the base was taken, when sync runs, then the Sheet's value is in both the Sheet and the mirror, and the entry is gone.

## Implementation Notes

- Choices beyond the plan's wording: a base that can't be read counts as changed in the Sheet (the Sheet wins), rather than staying queued and failing every sync. An upsert with no mirror row is dropped without a write, as before. `pushed` counts confirmed writes and deletes per key. The 3.1 test "an append that doesn't report its row" became "two adds for one ID are one append with the latest values", which the one-change-per-key rule requires.
- The first full build hit the known K2 lint crash on `ExpenseTrackerAppTest.kt`; it passed after `:app:lintAnalyzeDebugUnitTest --rerun-tasks`.

- Review patch: the vanished-SMS Dismissed step now runs after the push and its re-read, so it sees only the entries still pending. As a result, a failed Dismissed append comes after the push's writes are confirmed (AD-4). The refresh is skipped, and the next run, with the mirror row still present, appends the SMS ID. `SyncEngineRepairTest`'s failure test was renamed and adjusted to match.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-16, quick): 0 high, 1 medium, 2 low

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | A pending upsert whose SMS-sourced row was deleted in the Sheet never gets its SMS ID into Dismissed | medium | patch | Real and new in this story. `vanishedDismissals` runs before the push and skips pending IDs, then the push drops the entry (row gone) and the refresh deletes the mirror row, so nothing is left to detect (AD-13). The fix computes the vanished set after the push; there's a test. |
| 2 | `Outcome.Done.rowsChanged` is never read | low | patch | Real dead code; it's removed. |
| 3 | A base-absent create whose append landed unconfirmed, then a delete: the delete is dropped and the row returns | low | reject | Real but rare: it needs a lost API reply plus an offline delete. It follows the frozen base-absent rule, and a fix would need extra stored state; the user can delete again. |

## Design Notes

- **Why one change per key:** entry 7 keeps the first base for later edits of a pending key. Applying each entry separately would make the second edit always fail the check against the row the first edit just wrote. Treating a key's entries as one change keeps AD-4's rule while letting several offline edits through.
- **Why no device check:** no app path writes `DeleteExpense` or a non-null base until entries 7 and 10, so the S23 can't exercise this story. The engine is verified against the fake gateway, as the spine's Testing convention says.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs). If lint fails with the known K2 crash on `ExpenseTrackerAppTest.kt`, re-run `:app:lintAnalyzeDebugUnitTest --rerun-tasks` (`deferred-work.md`).
