---
title: 'Pull: repair IDs and write them back'
type: 'feature'
ticket: '3'
created: '2026-11-16'
status: 'built'
baseline_revision: '49c671561393507ce88f3e8a06cca04485cbcf2b'
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

**Problem:** Hand-typed Expense rows with a blank ID never reach the app. Retried appends leave duplicate rows. Rows that share an ID silently lose all but the first. A blank Source stays blank in the Sheet. An SMS-sourced Expense deleted in the Sheet could let its SMS return to the Inbox (AD-5, AD-13).

**Approach:** After the pull reads every tab and before any push, the engine applies AD-5's repairs to the Expenses tab and writes them back:
- **Blank IDs:** a valid row with a blank ID gets a new UUID.
- **Retry duplicates:** later rows with the same ID and identical mapped values are deleted.
- **Differing duplicates:** later rows with the same ID but different values get a new UUID and a cleared SMS ID.
- **Blank Source:** written back as `User`.

The engine then re-reads Expenses when rows moved. For every SMS-sourced mirror row whose Expense is gone from the Sheet, it appends that row's SMS ID to Dismissed.

## Boundaries & Constraints

**Always:**
- **AD-5 order:** the first row in sheet order keeps an ID. "Identical" compares all mapped values (Date, Amount, Category by `CategoryKey`, Description, Source and SMS ID), with a blank Source read as `User`.
- **Writes touch mapped cells only:** ID, Source and SMS ID. Unknown columns are never written.
- **Deletes run bottom-up in one `spreadsheets.batchUpdate`.** Rows are found again from the read, never from stored indexes.
- **Invalid rows** (AD-7) are never repaired, deleted or written. Their IDs count as present: they're never "vanished", and their mirror rows stay. A blank-ID row that's otherwise invalid stays untouched and counts as invalid.
- **Vanished:** an Expense ID is in the mirror and has an SMS ID, it's neither in the fresh read nor among the invalid IDs, and it has no pending outbox entry. Its SMS ID gets a Dismissed row (`Dismissed On` = now from the injected `Clock`), unless Dismissed already has that ID.
- **Failures:** any repair, delete or Dismissed write that fails, or needs consent, stops the run before pushes and the refresh, as other failures do. A rerun recomputes from a fresh read, so repairs are idempotent.

**Never:**
- No base check, no deletes of anything but retry duplicates, and no pushes beyond `UpsertExpense` (entries 4–5).
- No repair of tabs other than Expenses (AD-5 covers Expense IDs only).
- No schema change, and no UI.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Blank ID | A valid row with no ID and a blank Source | A new lowercase UUID written to ID, `User` written to Source; mirrored | none |
| Blank Source only | A keyed row with a blank Source | `User` written to its Source cell | none |
| Retry duplicate | Rows 5 and 9 have ID `x` and identical values | Row 9 deleted; one `x` in the Sheet and the mirror | none |
| Differing duplicate | Rows 5 and 9 have ID `x`; row 9's amount differs and it has SMS ID `s` | Row 9 gets a new UUID and a cleared SMS ID; both mirrored | none |
| Several deletes | Duplicates at rows 4, 7 and 12 | One request deleting 12, then 7, then 4; the rows in between survive | none |
| Vanished SMS row | The mirror has `e1` with SMS ID `s1`; the Sheet no longer has `e1` | `s1, now` appended to Dismissed and mirrored | Already in Dismissed: nothing appended |
| Invalid row | `e2` is still in the Sheet but its amount is `abc` | Not vanished, no Dismissed row, mirror row kept, counted invalid | none |
| Pending row | `e3` has an outbox entry and isn't in the Sheet | Not vanished; left for its push | none |
| Write fails | The cell write or delete fails, or needs consent | No push, no refresh; the outbox is unchanged | The next run retries from a fresh read |

</frozen-after-approval>

## Code Map

- `data/.../google/SheetTabs.kt` -- `readTab` L123–151 drops blank rows at L139, numbers rows `index + 2` at L142, drops `Unkeyed` silently at L147 and counts `Invalid` at L143–146. Add an Expenses read that also returns the unkeyed valid rows (with row number and parsed values), whether each row's Source cell was blank, and the IDs of invalid rows. `writeExpense` L40–58 shows how cells are written (`headerColumns` L106–115, per-cell `ValueRange`, `USER_ENTERED`). `appendRows` L77–97 is the generic append to reuse for Dismissed.
- `data/.../google/ExpenseRowMapping.kt` -- L28 returns `Unkeyed` before the other fields are checked; parse the rest so a blank-ID row can be valid or invalid. L39 maps blank Source to `User`; expose the blank as well. `DismissalRowMapping.kt` (20 lines) needs `cells()`; use `SheetValues.asText` L158 and `formatDate` L103.
- `data/.../google/SheetsModels.kt` -- `SpreadsheetRequest` L26 has only `addSheet`. Add `deleteDimension` (`DimensionRange(sheetId, "ROWS", startIndex 0-based, endIndex exclusive)`). `SheetProperties(sheetId, title)` L19 already exists. `SheetLayoutRepair.kt` L19–23 and `TITLES` L55: request `sheets.properties(sheetId,title)` so the Expenses tab's numeric ID is known, or fetch it where the delete happens.
- `domain/.../account/ExpenseSheetGateway.kt`, `ExpenseSheetTabResult.kt` -- add the richer Expenses read, `writeExpenseRepairs(sheetId, fixes)` (row, ID, Source `User`, clear SMS ID), `deleteExpenseRows(sheetId, rows)` and `appendDismissals(sheetId, dismissals)`. Fix the stale `Rows.skipped` KDoc at L13–14, which still says a blank ID is counted.
- `data/.../sync/SyncEngine.kt` -- the order is check (L61–67), repair (L68), reads (L69–73), push loop (L75–98, `rowsById` L139–143). Insert the ID repair after the reads (a pure, unit-tested `ExpenseIdRepair`), then re-read Expenses after any delete or new ID. Then detect vanished rows and append Dismissed rows (include them in the Dismissals passed to the refresh), then push. Inject `Clock`, provided at `data/.../expense/ExpenseModule.kt:20`, and update the constructor calls at `SyncEngineTest.kt:58–59` and `SyncCoordinatorTest.kt:63`.
- `data/.../sync/MirrorRefresh.kt` -- keep mirror Expenses whose ID is in the invalid set. Today `diff` deletes them; `distinctBy` is at L121.
- `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` (146 lines; `expenses` L33, `write()` L127–138, rows `index + 2`) -- model raw rows (a blank ID, a blank Source, invalid rows), cell fixes, deletes that shift rows, and Dismissed appends, with recording and failure knobs. Also update `EnsureExpenseSheetUseCaseTest.kt` L85–95 (`error("not used")`) and `SheetsExpenseSheetGateway.kt` L75–91.
- Tests: `SyncEngineTest.kt` (353 lines) and `SheetTabsTest.kt` (338) are near the 500-line limit. Put the engine cases in a new `ExpenseIdRepairTest` (a pure function) plus `SyncEngineRepairTest`, and the request bodies in a new `SheetTabsRepairTest`.

## Tasks & Acceptance

**Execution:**
- [x] `:domain` -- the gateway additions, the result types, and the KDoc fix -- the contract
- [x] `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` and the other implementers -- the raw-row model, fixes, deletes, Dismissed -- engine tests
- [x] `data/.../google/` -- the richer Expenses read, the `deleteDimension` model, the sheet-ID lookup, `writeExpenseRepairs` (one `batchUpdateValues` writing only ID, Source and SMS ID cells), `deleteExpenseRows` (one `batchUpdate`, descending), `appendDismissals` -- the Sheet side
- [x] `data/.../sync/ExpenseIdRepair.kt` -- pure: from the read rows, compute the fixes and deletes per AD-5 -- the rules
- [x] `data/.../sync/SyncEngine.kt`, `MirrorRefresh.kt` -- the order, the re-read, vanished detection, the Dismissed append, invalid IDs kept -- the matrix
- [x] Tests: `ExpenseIdRepairTest` (blank, identical, differing, mixed groups, first-in-order), `SheetTabsRepairTest` (exact bodies: the cell ranges, `deleteDimension` indexes in descending order, the Dismissed append), `SyncEngineRepairTest` (every matrix row, including the failure and re-run cases) -- the matrix

**Acceptance Criteria:**
- Given a hand-typed row in the owner's Sheet with no ID and no Source, when the app syncs, then the row gets a UUID and Source `User` in the Sheet and appears in the app.
- Given an Expense row copied below itself in the Sheet, so two identical rows share an ID, when the app syncs, then the copy is deleted from the Sheet, and the app and the Sheet show it once.

## Implementation Notes

- `readExpenses` now returns `TabReadResult<ExpenseRowRead>`, including blank-ID and invalid rows. Fixes are written before deletes, because they use row numbers from the same read. Deletes look up the Expenses tab's numeric ID with `spreadsheets.get` (`sheets.properties(sheetId,title)`). Vanished-row detection reads the mirror before the outbox, so an Expense saved mid-run counts as pending.
- CLAUDE.md and the Copilot instructions don't describe the repair step yet (deferred to the epic 3 sweep, 3.15).

- Lint crash on the first full build after the review patches: the intermittent `:app:lintAnalyzeDebugUnitTest` K2 crash on `ExpenseTrackerAppTest.kt`, stack captured in `deferred-work.md`. It passed after `--rerun-tasks`, and the next full `build verifyRoborazziDebug` passed. It isn't caused by this story's code.
- Device check (2026-11-16, S23, debug build, the owner's Sheet):
  - The owner added an Expenses row with no ID and no Source (16 Nov 18:00, 50, Fun) and pasted a copy of the Chai row below it.
  - After one sync, the new row got ID `0019c0e3-08c1-45f7-b985-9116d3f5fedf` and Source `User` in the Sheet and the mirror.
  - The pasted Chai copy was deleted, leaving one Chai row.
  - `manual-1`'s blank Source was written back as `User`.
  - The outbox is empty, and there were no Google API errors. Both acceptance criteria pass.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-16, quick): 0 high, 2 medium, 1 low

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Identical later copies that differ from the first row each get a new UUID instead of being deleted | medium | patch | Real: `plan()` compares each later row only with the first row of its ID, so A/B/B leaves two permanent B Expenses, against AD-5. The fix deletes any row equal to an earlier row in the group, then re-IDs the remaining distinct rows after the first. |
| 2 | An invalid first row lets a later valid row keep the shared ID, and a pending push for that ID writes or appends another row with it | medium | patch | Real: invalid rows are skipped before grouping. The fix keeps invalid rows in the group order (they stay untouched), re-IDs later valid rows, and skips, while keeping its entry, an `UpsertExpense` whose ID exists only as an invalid row. |
| 3 | An SMS ID on an invalid Dismissed row (unreadable date) isn't seen, so it's appended again | low | reject | Real but unlikely: it needs a hand-mangled Dismissed date plus a vanished row. It happens once (the next read sees the valid copy) and is harmless (Dismissed is keyed by SMS ID), and the fix means reworking the Dismissed read. |

## Design Notes

- **Read shape:** each Expenses row read carries `row`, a nullable `id`, the parsed `Expense` (or invalid), and `sourceBlank`. `ExpenseIdRepair` groups the valid rows by ID in sheet order. Keeping the repair pure is what makes AD-5's rules testable without the network.
- **Why re-read:** deletes shift every row below them, and the push loop and the refresh need exact row numbers, so after any delete or new ID the engine reads Expenses again rather than adjusting the numbers by hand.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks:**
- The two acceptance criteria on the S23 (debug build) against the owner's Sheet, with the mirror read through `run-as` and sqlite.
