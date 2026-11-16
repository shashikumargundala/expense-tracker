---
title: 'Pull: parse every tab and refresh the mirror atomically'
type: 'feature'
ticket: '2'
created: '2026-11-16'
status: 'built'
baseline_revision: '84c8ebc87b1e289cc1c4796ff5be4c8a3bfefb9c'
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

**Problem:** Sync pulls only Expenses and Categories, and fails outright when a tab or required header is missing. Budgets, Dismissed and Settings never reach the phone, so a new phone restores only part of the user's data (CAP-2), and hand edits in the Sheet are only partly honoured (CAP-6).

**Approach:**
- **Repair the layout first.** Every sync adds any missing tab or required header (AD-7).
- **Read all five tabs** with AD-7's parsing.
- **Refresh all five mirror tables in one Room transaction.** The transaction reads the outbox inside it and keeps rows that have a pending entry (AD-18).
- **Add missing Categories.** A Category used on an Expense but missing from the Categories tab is appended there (AD-7).
- **Report invalid rows.** Rows skipped as invalid are counted across all tabs in the sync result.

## Boundaries & Constraints

**Always:**
- **Mapping:** columns map by header name, trimmed and case-insensitive. Unknown columns and their values are never touched. Row cells are written only in mapped columns.
- **Parsing (AD-7):**
  - Amounts accept `₹`, thousands separators and decimals, and must be above zero.
  - Dates accept serial numbers or text.
  - A Budget `Month` accepts `yyyy-MM` text or a date.
  - Category names match trimmed and case-folded. An Expense or Budget whose Category matches an existing Category takes that Category's own spelling in the mirror.
  - A blank Source is `User` in the mirror.
- **Invalid rows:** a row that has its key but a missing or unparseable mapped value is skipped, left untouched in the Sheet, and counted. A row with a blank key isn't counted: entry 3 repairs blank IDs.
- **Atomic refresh:** it runs only after every tab read succeeds. It's one `withTransaction` that applies a diff, never a clear-and-refill. The mirror row is the last-pulled value of every row without a pending entry, and later edits take their outbox `base` from it.
- **No schema change:** Room stays at version 2.

**Never:**
- No ID repair, duplicate handling or Source write-back to the Sheet (entry 3).
- No base check, and no push of any type except `UpsertExpense` (entries 4–5).
- No triggers or UI (entries 6, 8, 11).
- No repository write APIs for Budgets, Dismissals or Settings (entry 5).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| New phone | Empty mirror; Sheet has rows in all five tabs | Every tab's rows land in the mirror; `skipped` = 0 | none |
| Hand-typed Expense | `₹1,200.50`, date text `8 Oct 2026 13:42`, Category ` food `, Source blank | 120050 paise, 2026-10-08T13:42, Category `Food`, Source `User` | none |
| Edited and deleted in the Sheet | A row's amount changed; another row removed; neither pending | The mirror takes the new amount and drops the removed row | none |
| Pending row | A row has an outbox entry; the Sheet differs or lacks it | The mirror row is unchanged | none |
| Saved during a run | An Expense is added after the reads, before the refresh | It survives the refresh and stays pending | none |
| Invalid rows | Amount `abc`, an unparseable date, a Budget without a Month | Skipped and counted; `Synced.skipped` = 3; the Sheet is untouched | none |
| Unknown Category | An Expense uses `Pets`, which isn't in Categories | `Pets` is appended to the Categories tab, then mirrored | A write failure fails the sync with no refresh |
| Missing tab or header | Settings tab deleted; `Source` header removed | The tab is added with its headers; the header goes into the first empty header cell; existing cells untouched | A failure fails the sync with no refresh |
| Unknown column | Expenses has an extra `Notes` column | It's never read or written; pushed rows leave it as it is | none |

</frozen-after-approval>

## Code Map

- `domain/.../account/ExpenseSheetGateway.kt` L38–49 (`readExpenses`, `readCategories`, `writeExpense`) and `ExpenseSheetTabResult.kt` (`TabReadResult.Rows(rows, skipped)`, `RowWriteResult`) -- add `repairLayout(sheetId)`, `readBudgets`, `readDismissals`, `readSettings` and `appendCategories(sheetId, categories)`, reusing the result types. Also update the KDoc, which says a missing header fails.
- `domain/.../{budget,inbox,settings}/` -- new `Budget(month: YearMonth, category, amountPaise)`, `Dismissal(smsId, dismissedOn: LocalDateTime)` and `Setting(key, value)`. Add a shared `CategoryKey` helper (trim plus `lowercase(Locale.ROOT)`) in `domain/.../category/` and use it in `MirrorRefresh` L52.
- Implementers to update: `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` (gets five in-memory tabs, `repairCalls`, appended Categories, and safe defaults), `domain/src/test/.../EnsureExpenseSheetUseCaseTest.kt` L80–91 (`error("not used")` stubs) and `data/src/test/.../sync/SyncCoordinatorTest.kt` L42 (`GatedGateway by sheet`).
- `data/.../google/SheetTabs.kt` -- `readTab` at L80–102 fails on a missing header at L92 and counts `skipped` at L93–101. Generalise the per-tab readers.
- `data/.../google/ExpenseRowMapping.kt` (201 lines) -- header mapping at L46/56/60, amounts at L122, dates at L143, Source at L79. Split it into per-tab files (`BudgetRowMapping.kt` and so on) that share the column and value helpers, to stay under 500 lines.
- `data/.../google/ExpenseSheetLayout.kt` L11–39 -- tabs and `headers`. Add constants for the Budgets, Dismissed and Settings headers.
- `data/.../google/SheetsService.kt`, `SheetsModels.kt` -- add `getSpreadsheet(id, fields = "sheets.properties.title")`, reusing `Spreadsheet`/`Sheet`/`SheetProperties`, and `batchUpdate(id, BatchUpdateSpreadsheetRequest)` with `AddSheetRequest`. Headers are written with the existing `batchUpdateValues`.
- `data/.../sync/SyncEngine.kt` L55–99 -- the run is: check, repair, read the five tabs, push `UpsertExpense`, re-read Expenses, append missing Categories, then refresh. `SyncResult.Synced(pushed, skipped)` counts all tabs.
- `data/.../sync/MirrorRefresh.kt` (67 lines; `withTransaction` at L32, `outbox.rowKeys(types)` at L39/53) -- extend it to Budgets, Dismissals and Settings. Add `OutboxType` groups (`OutboxEntity.kt` L25–28) for `UpsertBudget`, `AddDismissal` and `UpsertSetting`.
- `data/.../budget/BudgetEntity.kt`, `inbox/DismissalEntity.kt`, `settings/SettingEntity.kt` -- add DAOs (`getAll`, `upsert`, `delete`), register them in `AppDatabase` (schema unchanged) and provide them in the Hilt module.
- Tests: `data/src/test/.../sync/SyncEngineTest.kt` (246 lines; `newEngine()` L45; refresh cases L173, L203; uses `TestDatabase.kt`) -- move the refresh cases into a new `MirrorRefreshTest`. `SheetTabsTest.kt` L93 (missing header fails) becomes "header added". `ExpenseRowMappingTest.kt`, and the samples in `data/src/test/resources/google/`.

## Tasks & Acceptance

**Execution:**
- [x] `:domain` -- `Budget`, `Dismissal`, `Setting`, `CategoryKey`; the gateway additions and their KDoc -- the contract
- [x] `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` and the other test implementers -- five tabs, repair tracking, appended Categories -- engine tests
- [x] `data/.../google/SheetsService.kt`, `SheetsModels.kt` -- `spreadsheets.get` (titles) and `batchUpdate` `addSheet` -- tab repair
- [x] `data/.../google/` per-tab mappings, `SheetTabs`, gateway -- AD-7 parsing for all five tabs, `repairLayout` (add missing tabs with their headers, and missing headers in the first empty row-1 cells), `appendCategories` (Name; Color is the first of DESIGN.md's eight swatches at L322, light hex, that no existing Category uses, or else Food's `#F59E0B`; Icon `category`) -- matrix
- [x] `data/.../{budget,inbox,settings}/` DAOs, `AppDatabase`, Hilt -- mirror tables
- [x] `data/.../sync/SyncEngine.kt`, `MirrorRefresh.kt`, `OutboxEntity.kt` -- the run order and the five-table refresh -- AD-18
- [x] Tests:
  - mapping tests per tab: the hand-typed, invalid and case-fold rows
  - `SheetTabsTest`: the repair requests and bodies, and the `addSheet` and header writes
  - `MirrorRefreshTest`: edit, delete, pending and the five tables
  - `SyncEngineTest`: new phone, saved during a run (gate the gateway between the reads and the refresh), unknown Category, failure means no refresh, and the `skipped` total

  -- the matrix

**Acceptance Criteria:**
- Given the owner's Sheet has a hand-typed Expense row with any ID filled in and Source left blank, an edited amount on an existing row, and a typed Budgets row, when the S23's app data is cleared and the owner signs in again, then the Expenses tab shows the edited amount and the hand-typed row, which the mirror holds with Source `User`, and the mirror holds the Budget. Blank IDs are entry 3's.
- Given the Settings tab is deleted in the Sheet, when the app next syncs, then the Settings tab is back with `Key` and `Value` headers, and no other tab's data changed.

## Implementation Notes

- Choices beyond the plan's wording:
  - A row is left out without counting only when its whole key is blank; a Budget with neither Month nor Category is unkeyed, while a Budget missing only its Month is counted invalid.
  - A blank Setting `Value` reads as empty, and a Category's blank `Color` or `Icon` is accepted.
  - A header still missing after repair fails that read with `Unexpected`.
  - `MissingCategories` picks the new Category's colour before `appendCategories`.
- Cost: the layout repair runs on every sync, adding about six Sheets calls (one title list plus five header reads), so a sync is now about 12 calls or more.

- Device check (2026-11-16, S23, debug build, the owner's Sheet):
  - After clearing app data and signing in again, the first sync mirrored the edited Chai (13000 paise), the typed Budget (2026-11, Food, 600000) and all five Categories.
  - The hand-typed row first had a blank ID, so it was left out as planned (entry 3 repairs blank IDs). With ID `manual-1` it mirrored as 2026-11-16T10:00, 120050 paise, `Food`, Source `User`.
  - Settled review row 6: with the Settings tab renamed to `settings`, the sync succeeded and no second tab was added, so Sheets resolves A1 sheet names case-insensitively.
  - After the owner deleted the tab, the next sync re-added Settings (the owner confirmed it's back), and the Expenses and Budgets data were unchanged.
  - Both acceptance criteria pass.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-16, quick): 0 high, 2 medium, 1 low, 2 false, 1 maybe-false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | The device acceptance criteria aren't evidenced | false | reject | They're the plan's manual checks, run before commit as in story 3.1 (`5d628be`). Not a code defect. |
| 2 | `CategoryKey` (`lowercase(Locale.ROOT)`) folds differently from Room's ASCII-only `NOCASE`, so non-ASCII names duplicate and never converge | medium | patch | Real: `Café` versus `CAFÉ` diff as one key while SQLite inserts a second row. The fix folds only A–Z, and there are tests. |
| 3 | `MissingCategories` ignores pending `UpsertCategory` entries, so a user's new Category could be appended with defaults and then lose to "the Sheet wins" | medium | defer | Real but not reachable: nothing writes `UpsertCategory` until entry 5. Recorded in `deferred-work.md` for entry 5. |
| 4 | The canonical Category spelling makes the mirror differ from the Sheet value it calls its base | low | patch + defer | Real. The fix rewords `MirrorRefresh`'s KDoc; entry 4's base check must compare Category by `CategoryKey`, recorded for entry 4. |
| 5 | A blank Category `Color`/`Icon` or Setting `Value` isn't counted invalid | false | reject | Intended: a hand-added Category with only a Name must not vanish (screens apply defaults), and a blank Setting value is valid (the gateway KDoc). |
| 6 | Repair matches tab titles case-insensitively, but ranges use canonical titles, so this works only if A1 ranges resolve case-insensitively | false | reject | Settled on the S23: a tab renamed `settings` resolved for the `Settings` ranges, the sync succeeded and no tab was added. |

## Design Notes

- **Where the base lives:** the mirror row is the base. Every row without a pending entry is exactly its last-pulled mapped value. Entry 7's edits and entry 10's deletes copy the row into the outbox `base` when they write the first entry for a key. A later edit of a pending key keeps the first entry's base. This is why the refresh must never overwrite a pending row, and why no schema change is needed.
- **Repair order:** list the tab titles, `addSheet` each missing tab, then write each missing header. A header write targets only cells that are empty in row 1, so it can't shift existing columns.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks:**
- The two acceptance criteria on the S23 (debug build) against the owner's Sheet. The mirror is checked through `run-as` and sqlite, because Budgets and Settings have no screen yet.
