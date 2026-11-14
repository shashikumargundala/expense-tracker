---
title: 'An Expense added offline reaches the Sheet'
type: 'feature'
ticket: '1'
created: '2026-11-13'
status: 'built'
baseline_revision: 'b10d4f5b17da3abb315615295bcbd618025f1447'
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

**Problem:** The app has no Expenses, no local mirror and no sync, so nothing the user records can reach the Expense Sheet. Epic 3's other stories need one path working through every layer first.

**Approach:** Tracer bullet. It adds:
- the `Expense` model
- the whole Room mirror schema, with a migration that keeps the session
- an outbox holding AD-3's seven entry types
- `AddExpenseUseCase`, writing the Expense and its outbox entry in one transaction
- Sheet tab reads and Expenses row writes on `ExpenseSheetGateway`
- a minimal `SyncEngine` that applies only `UpsertExpense`
- a `SyncCoordinator`, run when a session with a Sheet ID becomes available

The Expenses tab gets a minimal FAB, form and list with a "Waiting to sync" chip. Entries 7 and 8 replace that UI.

## Boundaries & Constraints

**Always:**
- **Dependencies:** AD-1 and AD-11. `:domain` stays pure Kotlin with no Google types. Only `:data` `sync` calls `ExpenseSheetGateway` (AD-4). Screens read Room through `:domain` repositories (AD-3).
- **Values:** amounts are `Long` paise (AD-6). Dates are minute-precision `LocalDateTime` from an injected `java.time.Clock` (AD-19). IDs are random lowercase UUIDs (AD-5), and Source is `App` for app-created Expenses.
- **Writes:** a local change and its outbox entry share one Room transaction. An entry leaves the outbox only after the API confirms the write. Rows are found by `ID` in a fresh read, never by row index, and columns are mapped by header name (AD-7).
- **Sheet values:** `Date` is written `yyyy-MM-dd HH:mm` with `USER_ENTERED`. Reads use `UNFORMATTED_VALUE` with `SERIAL_NUMBER` dates.
- **Schema:** every schema change ships a migration (epic decision, 2026-11-13). This is one version bump, 1 to 2, adding all six tables.
- **Logging:** never log amounts with descriptions, emails, tokens or Sheet IDs.

**Never:**
- No base check, delete, pull of other tabs, AD-5 ID repair, WorkManager, foreground trigger or sync status pill. Entries 2–6 and 11 own those.
- No designed form, list, money formatter or period filter. Entries 7–9 own those.
- No destructive migration fallback.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Add offline | Amount ₹120, Food, "Chai", no network | Row lists at once with "Waiting to sync"; outbox has one `UpsertExpense` (base absent) | none |
| Invalid input | Amount ≤ 0 or no Category | Save disabled; the use case returns a named failure, and nothing is written | none |
| Sync, new row | Pending entry; its ID isn't in the Sheet | Row appended with Source `App`; entry removed; chip clears | none |
| Sync, key exists | Pending entry; the ID is already a row (a retry) | That row updated in place; no duplicate | none |
| Sync offline or failing | `Network`, `Access`, `NeedsConsent` or `SheetMissing` | Entry stays in the outbox; the mirror is unchanged; the next trigger retries | No prompt, no crash |
| Mirror refresh | Sheet rows after the writes | One transaction upserts the pulled Expenses and Categories, and deletes mirror rows missing from the Sheet unless they're pending | none |
| Upgrade from 2.x | v1 database with a session | Migrates to v2; the session is kept; Home opens | none |

**Decisions (owner, 2026-11-13):**
1. Until the first successful sync fills the Categories mirror, the form can't save. On a first open after an upgrade, or right after sign-in, while offline, it shows "Categories appear after the first sync". This is accepted as a known limitation for this story.
2. The implementer's CLAUDE.md and `.github/copilot-instructions.md` updates are kept, with the "Session gate" bullet corrected to match.

</frozen-after-approval>

## Code Map

- `data/.../account/AppDatabase.kt:10` -- `@Database(entities = [SessionEntity], version = 1, exportSchema = true)`. Keep it in this package: the schema dir `data/schemas/com.shashi.expensetracker.data.account.AppDatabase/` holds `1.json`. Add the six entities, version 2, and `autoMigrations = [AutoMigration(1, 2)]` (tables are only added).
- `data/build.gradle.kts:36` -- the KSP `room.schemaLocation`, and `room-testing` at L67. Add `sourceSets["test"].assets.srcDir("$projectDir/schemas")` for `MigrationTestHelper`, and `testImplementation(testFixtures(project(":domain")))`.
- `data/.../account/AccountDataModule.kt:29-32` -- the DB and DAO providers; add the new DAOs. `RoomSessionRepository.kt` is the error-catching pattern: `.catch` emits empty or null, writes return a result, and `CancellationException` is rethrown.
- `data/.../google/SheetsService.kt` -- has `createSpreadsheet` and `batchUpdateValues`. Add `getValues` (`GET spreadsheets/{id}/values/{range}` with `valueRenderOption`, `dateTimeRenderOption`) and `appendValues` (`POST …/values/{range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`). `SheetsModels.kt` `ValueRange` is the response shape; there's a sample at `data/src/test/resources/google/sheets-value-range.json`.
- `data/.../google/ExpenseSheetLayout.kt` -- tab names and headers (L11–25). Reuse them; don't duplicate.
- `data/.../google/SheetsExpenseSheetGateway.kt` (72 lines) and `ApiFailure.toResult()` L64 -- put the new reads and writes and the row mapping in separate files (`ExpenseRowMapping.kt`) to stay under 500 lines.
- `domain/.../account/ExpenseSheetGateway.kt`, `ExpenseSheetResult.kt` -- add `readExpenses(sheetId)`, `readCategories(sheetId)` and `writeExpense(sheetId, expense, existingRow: Int?)` with their own sealed results (rows carry their 1-based sheet row). Failures reuse `ExpenseSheetFailure`, `NeedsConsent` and `SheetMissing`.
- `domain/src/testFixtures/.../FakeExpenseSheetGateway.kt` -- add an in-memory Expenses and Categories table with safe defaults: empty reads and successful writes. Every Hilt app test will now sync against it.
- `app/.../StartViewModel.kt:34-38` -- reads the session once. Also request a sync each time the session first has a Sheet ID: on cold start, and after the first sign-in.
- `app/.../expense/ExpensesScreen.kt` (L25 Route, L42 a placeholder tagged `screen_expenses`), `ExpensesViewModel.kt`, `ExpensesUiState.kt` -- replace with the minimal list, FAB and form. Sheets pass `scrimColor = MaterialTheme.colorScheme.scrim`.
- `app/src/test/.../screenshot/ShellScreenshotTest.kt:47-53` -- the `shell_expenses_w320` baseline changes once the tab has a FAB; re-record it.
- `app/build.gradle.kts`, `data/build.gradle.kts`, `gradle/libs.versions.toml` -- minSdk 24 needs `isCoreLibraryDesugaringEnabled` and `coreLibraryDesugaring(libs.desugar.jdk.libs)` for `java.time`. Add the catalog entry.

## Tasks & Acceptance

**Execution:**
- [x] `gradle/libs.versions.toml`, `app/` and `data/build.gradle.kts` -- desugaring, test assets for schemas, data testFixtures -- java.time on API 24
- [x] `domain/.../expense/` -- `Expense`, `ExpenseSource` (`App`, `User`), `ExpenseRepository` (`observe(): Flow<List<ExpenseListItem>>` with `pending`, `add(expense): Boolean`), `AddExpenseUseCase` (validates, then sets the UUID, `App` and `now` truncated to the minute) -- AD-3, AD-5, AD-6, AD-19
- [x] `domain/.../category/` -- `Category(name, color, icon)`, `CategoryRepository.observe()` -- the form's picker
- [x] `domain/.../sync/` -- `SyncState(running, pendingCount)`, a `SyncController` interface (`request()`, `observeState()`), `RequestSyncUseCase`, `ObserveSyncStateUseCase` -- entries 6 and 8 extend these
- [x] `domain/.../account/ExpenseSheetGateway.kt`, the results, the fake -- tab reads and the Expenses row write -- the contract
- [x] `data/.../expense/`, `category/`, `sync/` -- entities for the six tables; `OutboxEntity(id, type, rowKey, base?, createdAt)` with all seven types; DAOs; Room repositories (`add` writes the Expense and its outbox entry in `withTransaction`); converters for `LocalDateTime` and enums; Hilt modules binding the repositories, `SyncController`, and a `@Singleton` `Clock.systemDefaultZone()` -- the mirror
- [x] `data/.../google/` -- `getValues` and `appendValues`, `ExpenseRowMapping` (header-named columns; paise to and from decimal rupees; serial or text date to `LocalDateTime`; written `yyyy-MM-dd HH:mm`), and the gateway methods -- AD-7
- [x] `data/.../sync/SyncEngine.kt`, `SyncCoordinator.kt` -- per the matrix; the coordinator runs one sync at a time behind a `Mutex` and drops a request while one is running -- tracer sync
- [x] `app/.../StartViewModel.kt` -- the sync request -- "on app open"
- [x] `app/.../expense/` -- a minimal list (amount, Category, description, date, a `expense_pending` chip on rows), an FAB `expenses_add`, and a minimal form (`expense_form_amount`, Category chips `expense_form_category`, `expense_form_description`, `expense_form_save`, disabled until valid; date is now) -- the visible tracer
- [x] Tests:
  - `:domain` `AddExpenseUseCaseTest`
  - `:data` `MigrationTest` (v1 with a session, migrated to v2, session intact)
  - Room repository and transaction tests
  - `ExpenseRowMappingTest`
  - `SheetsExpenseSheetGateway` reads and writes against mockwebserver3, asserting the ranges, render options and `USER_ENTERED`
  - `SyncEngineTest` and `SyncCoordinatorTest` against `FakeExpenseSheetGateway`
  - `:app` `ExpensesScreenTest` (add, then the row with its chip), and the shell baseline re-recorded

  -- the matrix

**Acceptance Criteria:**
- Given the owner's S23 is in airplane mode, when they add ₹120 "Chai" in Food, then it lists at once with "Waiting to sync". When they reconnect and reopen the app, the row is in the Sheet with Source `App` and today's date, and the chip clears.
- Given the S23 has the 2.x build signed in, when this build is installed over it, then Home opens without signing in again.

## Implementation Notes

- Choices beyond the plan's wording: `writeExpense` reads `Expenses!1:1` itself before each write; text cells are written with a leading `'` so `USER_ENTERED` keeps them as text; an append that doesn't report its row re-reads before the next entry for that ID; an entry with no local Expense is removed; a tab missing a required header fails its read, so the mirror isn't refreshed from it; the budget, dismissal and setting entities live in `data/budget`, `data/inbox` and `data/settings` with no DAOs yet; `shellExpensesW320` waits for "No expenses yet" before capturing.
- Desugaring: `desugar_jdk_libs` 2.1.5 is in `:app` and `:data`. AGP 9 rejects `sourceSets["test"]`, so the schema assets are added through `androidComponents.onVariants { hostTests … addStaticSourceDirectory }`.
- Latent theme crash (existing): `Type.kt` gives `bodyLarge.lineHeight` in em and `bodySmall` in sp, so an M3 `OutlinedTextField` with a `label` throws "Cannot perform operation for Em and Sp". The minimal form uses placeholders instead. Entry 7's designed form needs labels, so fix the theme there (for example `bodyLarge` line height `19.5.sp`, which renders the same).
- Review patch fallout: the trashed-Sheet `check()` at the start of every sync made 2.4's `storedSessionColdStartsToHomeWithNoNetworkCalls` fail, because launch now syncs (AD-14 app open). It became `storedSessionColdStartsToHomeOfflineWithoutSignInOrSheetSetup`: the fake's `check` returns `Network`, and the test asserts Home opens with no sign-in, access, find or create calls. Its intent, that launch doesn't depend on the network, is kept.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-13, quick): 0 high, 2 medium, 4 low, 1 false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | No Expense can be added offline before the first successful sync, because only `MirrorRefresh` fills Categories | medium | intent_gap → resolved | Real: nothing seeds the `categories` table. The owner accepted it as a known limitation (decision 1), so there's no code change. Recorded in `deferred-work.md`. |
| 2 | `:data` `sync`, `expense` and `category` use each other's DAOs, entities and mappers, against AD-2 | low | defer | Real, and forced by the single Expense-plus-outbox transaction (AD-3). It needs a spine note, like the `home` exception. Recorded for `bmad-architecture`. |
| 3 | A trashed Sheet still accepts `values.append` and `batchUpdate`, so pending Expenses land in the trash and leave the outbox | medium | patch | Real: `SheetTabs` maps only 404 to `SheetMissing`, and the engine never calls `check()`, which knows about `trashed`. The fix calls `check(sheetId)` first and stops on anything but `Ready`. |
| 4 | Returning from recents doesn't sync, so AC 1 passes only after a fresh start | false | reject | The plan triggers only on app open (cold start or the first Sheet ID), and the foreground trigger is entry 6. The S23 check force-stops before reopening. |
| 5 | `expense_form_category` is on the `FlowRow`, not on each chip | low | patch | Real: the plan tags the chips. The fix puts the tag on every Category chip. |
| 6 | CLAUDE.md's "Session gate" bullet still says `StartViewModel` reads the session once | low | patch | Real: it contradicts the new sync bullet. The owner kept the docs edits (decision 2), so the bullet is fixed in both files. |
| 7 | `SyncCoordinator` drops a request made during a run instead of coalescing it (AD-14), with no record of the departure | low | patch | Real and as planned. The fix is a KDoc and docs note that entry 6 coalesces. |

## Design Notes

- **Outbox `base`:** a nullable JSON text of the row's mapped values as last pulled; null means absent. Entry 1 writes only absent. Entry 2 fills real bases and entry 4 checks them.
- **Pending:** an Expense is pending when an outbox entry with its `rowKey` exists. There's no `synced` column (AD-3 wins over EXPERIENCE.md's `synced = false`, per the source conflict in the epic Notes).
- **Writing a row:** the engine reads the header row and writes cells in header order. Unknown columns are left as they are; append covers only the mapped columns.

## Verification

**Commands:**
- `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest` -- expected: pass
- `./gradlew recordRoborazziDebug --tests '*ShellScreenshotTest'`, then `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks:**
- On the S23, the two acceptance criteria above. The debug build must replace the release build, which means uninstalling it first. The 2.x upgrade check then needs a 2.x debug build installed first, so it runs as: install `master`'s debug build, sign in, then install this branch's build over it.
