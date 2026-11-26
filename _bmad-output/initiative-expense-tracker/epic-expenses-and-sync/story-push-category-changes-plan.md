---
title: 'Push Category changes'
type: 'feature'
ticket: '5'
created: '2026-11-20'
status: 'built'
baseline_revision: '7f10768e6e43a22cb18d894ac9c2f4098a9a9ca2'
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

**Problem:** Categories can't be written locally, and `UpsertCategory` and `DeleteCategory` never reach the Sheet. Epic 4's Category editor has no API to call. A pending new Category could also be appended by sync with default colour and icon, which would then make the user's choice lose to "the Sheet wins" (deferred from 3.2).

**Approach:**
- **Repository:** add `CategoryRepository.upsert(category)` and `delete(name, reassignTo)`, each writing the mirror and its outbox entry in one transaction. The outbox gets a nullable `payload` column (Room v3) that holds `reassignTo`.
- **Push `UpsertCategory`** with the base check.
- **Push `DeleteCategory`** per AD-9: rewrite every matching Expense row to `reassignTo` without the base check, delete that Category's Budgets rows, then delete the Category row.
- **Pending Categories:** treat a Category with a pending `UpsertCategory` as known when appending missing Categories.
- **Invalid rows:** report invalid rows by key, so a push never appends a second row for one.

## Boundaries & Constraints

**Always:**
- **Keys:** Categories are keyed by `CategoryKey` (trim, ASCII fold), and every match uses it (AD-9).
- **Upsert base check:**
  - The base is the Category's `{name, color, icon}` as last pulled; null means absent. A row that differs from the base, or is gone, means the Sheet wins and the entries are dropped.
  - Base absent with no row appends.
  - Base absent with a row present writes nothing.
  - Writes touch only the Name, Color and Icon cells.
- **Delete (AD-9), in this order:** rewrite the Category cell of every Expense row whose Category matches the name in the fresh read to `reassignTo` (one batch; no base check for these rows), then delete every Budgets row for that Category, then delete the Category row if it's there. Deletes run bottom-up in one request per tab. The Category row is deleted even if it changed in the Sheet, because the user explicitly chose to delete it.
- **Local delete:** it's one transaction that reassigns the mirror's Expenses (matched by `CategoryKey`), deletes the mirror's Budgets for that Category, deletes the Category, and queues `DeleteCategory(rowKey = name, payload = reassignTo, base = the row)`. It returns false, and writes nothing, when `reassignTo` isn't another existing Category.
- **One change per key, as in 3.4:** a key's entries become one change; the last entry's type decides, and the first entry's base is the one checked. Entries leave only after the API confirms. A failure keeps the key's entries and stops the run before the refresh.
- **Schema:** the `payload` column is added by `AutoMigration(2, 3)`, with the exported `3.json` and a migration test showing v2 outbox rows survive.

**Never:**
- No Budget, Dismissal or Setting pushes or repositories (entry 16).
- No Category editor UI or use cases (epic 4). There's no rename either: epic 4 decides whether a rename is a new upsert plus a delete with reassign.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| New Category | Local `upsert(Pets, #A855F7, pets)`; no Sheet row | Appended with those three cells; the entry leaves | none |
| Upsert, unchanged | The row equals the base | Color and Icon updated in place | none |
| Upsert, changed in Sheet | The row's Color differs from the base | Nothing written; the entry is dropped; the mirror takes the Sheet's value | none |
| Upsert, deleted in Sheet | The base is present; the row is gone | Nothing written; dropped; the refresh removes the mirror row | none |
| Upsert, invalid row | The row for that key is invalid | Skipped this run; the entries are kept; nothing is appended | none |
| Pending new Category used | An Expense uses `Pets`; `UpsertCategory(Pets)` is pending | `MissingCategories` doesn't append `Pets`; the push appends it with the user's colour and icon | none |
| Delete with reassign | Delete `Fun` into `Food`; 3 Expense rows and 2 Budgets rows use `fun` | The 3 Expense Category cells become `Food`, the 2 Budgets rows are deleted, the Fun row is deleted; the mirror agrees | none |
| Delete, already gone | The Category row is absent | The Expense rewrite and Budgets cleanup still run; the entry leaves | none |
| Bad reassign | `reassignTo` is the same name, or unknown | `delete` returns false; nothing changes | none |
| Failure | The rewrite, a delete or a write fails, or needs consent | That key's entries stay; no refresh | The next run retries from a fresh read |
| Upgrade | A v2 database with outbox rows | Migrates to v3; the rows are kept with a null `payload` | none |

</frozen-after-approval>

## Code Map

- `data/.../sync/OutboxEntity.kt` L47–54 (`id`, `type`, `row_key`, `base`, `created_at`) -- add `payload: String?`. `data/.../account/AppDatabase.kt` L37–39 -- version 3, add `AutoMigration(2, 3)`. The schemas are in `data/schemas/…AppDatabase/` (`1.json`, `2.json`). `data/src/test/.../account/MigrationTest.kt` (59 lines, one v1→v2 test) -- add a v2→v3 test.
- `domain/.../category/CategoryRepository.kt` (`observe` only), and `data/.../category/RoomCategoryRepository.kt` L16 -- add `upsert` and `delete(name, reassignTo)`, following `RoomExpenseRepository.add` L46–59 (`withTransaction`, false on exception). `CategoryDao` has `observe`, `getAll`, `upsertAll` and `deleteByNames`; add a single get. `ExpenseDao` needs a bulk `reassignCategory`; `BudgetDao` needs a delete by Category.
- `data/.../sync/ExpenseBase.kt` -- the model for a new `CategoryBase` (`encode`, `decode`, `matches` by `CategoryKey`).
- `data/.../sync/ExpensePush.kt` (135 lines; grouping L60–88, pure `change` L100–122) -- the model for a new `CategoryPush.kt`. Don't change `ExpensePush` beyond what the engine order needs.
- `data/.../sync/SyncEngine.kt` (177 lines) -- the Expense push is at L92–97, followed by an Expenses re-read, vanished SMS IDs L101, `MissingCategories.find` L113 and the refresh L115. Run `CategoryPush` after the Expense push, re-reading Expenses, Categories and Budgets when it changed anything. That has to happen before the vanished-SMS step and `MissingCategories`.
- `data/.../sync/MissingCategories.kt` L15–22 -- `known` is built from the Sheet only; add the pending `UpsertCategory` keys.
- `data/.../sync/MirrorRefresh.kt` L82 (pending Categories skipped) -- also keep mirror Categories whose key is invalid in the Sheet.
- `data/.../google/SheetTabs.kt` -- `readTab` L197–225 drops invalid rows and only counts them; also return their keys (a generic `invalidKeys` on `TabReadResult.Rows`, for every tab). `appendCategories` is L72. `deleteExpenseRows` L113–132 finds the tab ID with `TAB_IDS` and deletes bottom-up; generalise it into `deleteRows(sheetId, tab, rows)`. `writeExpenseRepairs` L87 is the model for a batch `writeExpenseCategories(rows, name)`. Add `writeCategory(sheetId, category, existingRow)`.
- `domain/.../account/ExpenseSheetGateway.kt` -- the new methods; `deleteExpenseRows` may delegate to the generic delete.
- `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` (230 lines) -- add Category write and delete, Budgets delete, Expense Category rewrites, invalid keys, recording and failure knobs.
- Tests: `ExpensePushTest.kt` is at 402 lines, so put the new cases in `CategoryPushTest.kt`, `CategoryBaseTest.kt`, `RoomCategoryRepositoryTest.kt` and `MigrationTest.kt`.

## Tasks & Acceptance

**Execution:**
- [x] `data/.../sync/OutboxEntity.kt`, `AppDatabase.kt`, `data/schemas/…/3.json`, `MigrationTest.kt` -- the `payload` column, v3 and its migration test -- schema
- [x] `:domain` -- `CategoryRepository.upsert` and `delete`, the gateway additions, `TabReadResult.Rows.invalidKeys` -- the contract
- [x] `data/.../category/`, `expense/`, `budget/` DAOs and `RoomCategoryRepository` -- the transactional local writes -- the local side
- [x] `data/.../google/` -- `writeCategory`, `writeExpenseCategories`, generic `deleteRows`, and the invalid keys from `readTab` -- the Sheet side
- [x] `data/.../sync/CategoryBase.kt`, `CategoryPush.kt`, `SyncEngine.kt`, `MissingCategories.kt`, `MirrorRefresh.kt` -- the push, the order, the pending-known fix, invalid keys kept -- AD-9
- [x] Fake gateway and the other implementers -- the knobs -- tests
- [x] Tests: `CategoryBaseTest`, `CategoryPushTest` (every matrix row), `RoomCategoryRepositoryTest` (a transaction and the bad reassign), the `MigrationTest` v2→v3, and `SheetTabs` request bodies for the new writes and deletes -- the matrix

**Acceptance Criteria:**
- Given `delete("Fun", reassignTo = "Food")` with Fun's Expenses and Budgets in the Sheet, when sync runs against the fake gateway, then no Sheet Expense row says Fun, Fun's Budgets and Category rows are gone, the mirror matches, and the outbox is empty.
- Given the S23 with the 3.4 debug build, when this build is installed over it, then the app opens on Home, still signed in, with the database at version 3.

## Implementation Notes

- Choices beyond the plan's wording: a delete into a Category that's deleted in the same run follows it to its own target, with cycles guarded. A `DeleteCategory` with a blank or missing payload is dropped (the Sheet wins). `SyncResult.pushed` now adds Expense and Category pushes, each confirmed Category delete counting as 1. The mirror reassign uses SQLite `trim(…) COLLATE NOCASE`, which trims spaces only.
- CLAUDE.md and the Copilot instructions were updated for DB v3, the payload, and the Category repository and push.
- Device check (2026-11-26, S23, debug build): installed over the 3.4 debug build, the DB went from `user_version` 2 to 3. The session and Sheet ID were kept (still signed in), with 3 Expenses and 5 Categories, an empty outbox, and the new `payload` column. There was no crash or migration error in logcat. Acceptance criterion 2 passes.

- Review patches: `ExpenseRowRead.category` exposes the raw Category cell for invalid rows too, so a delete rewrites every matching Expense row. `TabReadResult.Rows.invalidRows` carries invalid rows' numbers, so a delete removes every matching Budgets row. `BudgetRowMapping.key` now keys any row with a Category, using the Month's raw text when it doesn't parse. KDoc says the invalid-Category paths are forward-compatible only. Both docs files fixed with identical edits.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-26, quick): 0 high, 2 medium, 4 low, 1 false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | The delete rewrites only valid Expense rows, so an invalid row saying `Fun` keeps a deleted Category | medium | patch | Real: `expense == null` for invalid rows hides their Category, against the plan's "every Expense row" and AD-9. The fix exposes each row's raw Category and rewrites that one cell for any matching row. |
| 2 | Only valid Budgets rows of the deleted Category are deleted | medium | patch | Real: `invalidKeys` carries no row numbers. The fix returns invalid rows with their row numbers and deletes every matching Budgets row. |
| 3 | `CategoryRowMapping` never yields `Invalid`, so the invalid-Category paths are reachable only through the fake | low | patch | Real: it's harmless but misleading. KDoc notes now say these paths are forward-compatible. |
| 4 | A local delete made mid-run lets the refresh restore Fun on Expenses and Budgets until the next sync | low | defer | Real but brief: the next sync corrects it. The fix needs the coordinator to defer the refresh after concurrent writes. Recorded for entry 6, which owns the sync triggers and coalescing. |
| 5 | Delete Fun→Food, delete Food→Pets, then re-create Food leaves the Sheet's Expenses on Food while the mirror had Pets | low | reject | Real but needs three offline Category operations in sequence. The next sync converges the Sheet and the mirror, and the fix would mean tracking the whole reassign history. |
| 6 | CLAUDE.md and the Copilot instructions contradict themselves on what `SyncEngine` pushes, and the gateway list is stale | low | patch | Real: the implementer edited these files, so they're fixed in place with identical edits in both. |
| 7 | The on-device acceptance criterion isn't evidenced | false | reject | It was run: see the device check in Implementation Notes. |

## Design Notes

- **Why the delete skips the base check:** AD-9 skips it for the reassigned Expense rows. For the Category row, deleting is the user's explicit choice, and keeping a row nothing points to would leave Expenses orphaned. Budgets rows for the Category go with it.
- **Generic invalid keys:** returning invalid keys from every tab now, rather than only Categories, lets entry 16 reuse them for Budgets and Settings without changing the reader again.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs). If lint fails with the known K2 crash on `ExpenseTrackerAppTest.kt`, re-run `:app:lintAnalyzeDebugUnitTest --rerun-tasks`.

**Manual checks:**
- On the S23: install over the 3.4 debug build; Home opens, still signed in; `run-as` and sqlite show `user_version` 3, and the outbox has a `payload` column.
