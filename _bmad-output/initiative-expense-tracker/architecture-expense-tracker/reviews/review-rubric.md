---
type: architecture-review
lens: rubric
target: ../architecture-expense-tracker.md
reviewed: '2026-10-08'
verdict: 'Not ready. Strong skeleton, but the sync contract (FR-9 conflict detection, row addressing) is undecided, and the Stack is not current.'
---

# Rubric Review: Architecture Spine, Expense Tracker

**Verdict:** Not ready to bind the level below. The module, layering, identity, money, budget and SMS decisions are sound and mostly enforceable. But the spine leaves the most divergence-prone unit, the sync engine, without a decidable conflict and row-addressing rule. Its own AD-4 ordering contradicts FR-9. Session and account lifecycle is missing. Eight Stack entries are stale. The operational envelope skips applicationId, Play App Signing and backup rules.

Evidence base: the spine; PRD and addendum in `../../prd-expense-tracker/`; the code under `app/` at 26e51ad; `gradle/libs.versions.toml`; Maven Central and Google Maven metadata, checked 2026-10-08.

## Scorecard

| # | Rubric item | Result |
| --- | --- | --- |
| 1 | Fixes the real divergence points and misses none | **Fail.** Sync conflict detection, row addressing, Sheets value encoding, session lifecycle, settings storage, outbox scope and default-Category bootstrap are all open. |
| 2 | Every AD Rule is enforceable and prevents its divergence | **Partial.** AD-1, AD-5, AD-6, AD-8 and AD-17 are enforceable. AD-4's Rule does not achieve its own stated outcome. AD-2 and AD-15 name no enforcement mechanism. AD-15 and the Naming convention disagree on whether use cases are interfaces. |
| 3 | Nothing under Deferred lets two units diverge | **Partial.** "Two phones" relies on AD-4 convergence, which is not guaranteed without ID-based addressing. The Room migrations deferral can silently drop the outbox. |
| 4 | Named tech verified as current | **Fail.** 8 of 20 rows are stale, one by two years. Several libraries in use aren't listed. |
| 5 | Ratifies, not contradicts, the brownfield code | **Partial.** Every AD is `[ADOPTED]` and none is ratified. The spine reverses CLAUDE.md conventions (no DI, prefs in repository, keep the OAuth deep link, single module) without stating a migration stance. It also drops the kept conventions (version catalog, `testTag`, commented-out deps). |
| 6 | Every owned dimension is decided, deferred or open, including the operational envelope | **Partial.** CI, distribution and environments are present. Missing: applicationId and package identity, Play App Signing key for OAuth, shared debug keystore, backup rules, R8/minify, manifest permissions. |

## Findings

### F1. Critical: AD-4 cannot deliver "Sheet wins", and change detection is undecided
- **Where:** AD-4, AD-3, Structural Seed sync diagram.
- **Problem:** AD-4 orders a sync run as "push the outbox, then pull." A pending in-app edit is therefore written over the row before the engine ever sees the hand edit. The app's version wins, which is the opposite of FR-9 and of AD-4's own last sentence. The Rule never says how a "conflict on the same Expense" is detected. The addendum explicitly hands this to architecture ("Architecture must define how changes are detected"). It's also undefined what happens when the outbox entry targets a row that was deleted in the Sheet: resurrect it or drop it. Two implementers would build different engines.
- **Fix:** amend AD-3 and AD-4 as follows:
  - Each outbox entry stores the entity key, the operation and a `baseHash`. The `baseHash` is a hash of the row's mapped values at the last pull, or null for a create.
  - A sync run reads the target tabs, then for each entry compares the current row (found by ID) with `baseHash`. It writes only when they match. When they differ, or the row is gone, it drops the entry (Sheet wins) and records a `syncMessage`. After the writes, it re-reads and replaces the mirror.
  - Add a sync-engine test matrix to the Testing convention: edit vs hand-edit, edit vs hand-delete, delete vs hand-edit, create.

### F2. High: Sheet row addressing and value encoding are not fixed
- **Where:** AD-5, AD-7, Dates convention.
- **Problems:**
  - AD-7 maps columns by header but says nothing about rows. The Sheets values API addresses by A1 range, so an update or delete must find a row index. A hand-inserted or sorted row between read and write makes position-based writes hit the wrong row. AD-5's write-back of new UUIDs and `Source = User` during pull has the same risk.
  - Today's `GoogleSheetsService` has only append, get and put. There's no delete (`batchUpdate`/`deleteDimension`).
  - The value options aren't fixed. With today's `USER_ENTERED`, `yyyy-MM-dd HH:mm` becomes a locale-dependent date serial. Hand-typed amounts like `₹1,200` read back differently under FORMATTED_VALUE and UNFORMATTED_VALUE.
- **Fix:** add a rule to AD-7:
  - Re-read the ID column immediately before every write batch. Address rows only by ID. Delete with `spreadsheets.batchUpdate` `deleteDimension`, in descending row order.
  - Write with `valueInputOption=RAW` (Date as text `yyyy-MM-dd HH:mm`, Amount as a plain decimal).
  - Read with `valueRenderOption=UNFORMATTED_VALUE` and `dateTimeRenderOption=FORMATTED_STRING`. Accept both serial and text dates on read.
  - Batch reads with `values:batchGet` to stay within the 60 reads/min/user quota.

### F3. High: account and session lifecycle, and first-run bootstrap, are undecided
- **Where:** AD-10, AD-11, AD-14, FR-1, FR-2, FR-17, NFR-4.
- **Problems:**
  - **Offline cold start:** AD-10 keeps tokens in memory only. Nothing says what persists "signed in" across a cold start, though FR-1 gates every screen and NFR-4 requires offline use.
  - **Sign-out and account switch:** nothing says what happens to the Room mirror and any unpushed outbox. Account B could see A's data, or A's pending edits could be lost or pushed to B's Sheet.
  - **Background authorization:** nothing says what the WorkManager worker does when `AuthorizationClient.authorize()` returns a result that needs a UI resolution.
  - **Expense Sheet identity:** the "find" rule isn't fixed. It could match by title, by Drive `appProperties`, or by the cached ID, and nothing says which to use when several match.
  - **Default Categories:** nothing says where they are bootstrapped. Today `checkAndBootstrapCategories()` runs on every empty DB. On a new phone, that would re-add defaults through the outbox, including Categories the user had deleted from the Sheet.
- **Fix:** add an AD, "Session and Expense Sheet lifecycle":
  - Persist the signed-in account ID and email and the spreadsheet ID locally, using DataStore owned by `account` in `:data`. This is the FR-1 gate, and it works offline.
  - Sign-out first tries to flush the outbox, then warns about unpushed changes, then wipes the Room DB.
  - The worker treats "needs resolution" as a retryable failure and surfaces a re-authorize prompt on next open.
  - Find the Sheet with Drive `files.list` filtered on `appProperties.expenseTrackerSheet=v1` and `trashed=false`. Pick the most recently modified match.
  - Default Categories are written only when a new Expense Sheet is created. They're never seeded into Room on its own.

### F4. High: the Stack table is not current
- Checked against Maven Central and Google Maven on 2026-10-08:

| Entry | Spine | Latest stable | Note |
| --- | --- | --- | --- |
| Android Gradle Plugin | 9.1.1 | 9.4.1 | stale |
| Kotlin | 2.2.10 | 2.4.21 | stale (2 minors) |
| KSP | 2.3.5 | 2.3.12 | stale |
| Compose BOM | 2024.09.00 | 2026.09.00 | **two years stale** |
| OkHttp | 4.10.0 | 5.5.0 (4.x line: 4.12.0) | stale |
| Retrofit | 2.12.0 | 3.0.0 | major available; decide |
| Robolectric | 4.16.1 | 4.17 | stale |
| Roborazzi | 1.59.0 | 1.76.0 | stale |
| Hilt 2.60.1, androidx.hilt 1.4.0, Navigation 2.10.2, Room 2.8.5, WorkManager 2.12.0, Credential Manager 1.6.0, googleid 1.2.1, play-services-auth 22.0.0, Moshi 1.15.2 | | | current |

- **Also missing from the table:**
  - Lifecycle (2.8.7 in the catalog, stale), activity-compose, kotlinx-coroutines and Material 3.
  - The Kotlin JVM plugin needed for the pure-Kotlin `:domain`. AGP 9's built-in Kotlin covers only Android modules.
  - A DataStore decision, if F3 is adopted.
  - The Secrets Gradle Plugin's fate, since the spine says nothing in `.env` is required.
- The spine also silently upgrades Room (2.7.0 to 2.8.5) and Navigation (2.8.9 to 2.10.2) from the catalog. That's fine, but say so.
- **Fix:** bump the stale rows. Choose OkHttp 5.x with Retrofit 3.0.0, or pin OkHttp 4.12.0 and say why. Add the missing rows. State that all versions live in `gradle/libs.versions.toml` (the existing convention).

### F5. High: operational envelope gaps
- **Where:** Structural Seed deployment diagram and its Environments line, Config convention.
- **Problems:**
  - **Package identity:** the app still has `applicationId = com.aistudio.expensetracker.ktzqm`, namespace `com.example` and `rootProject.name = "My Application"`. Android OAuth clients are bound to package name and SHA-1, so changing the applicationId after creating clients, or after the first Play upload, is costly. The spine doesn't fix it. PRD OQ5 (product name) is pending but doesn't block choosing a package.
  - **Play App Signing:** "tied to their signing key" is ambiguous. Play-installed builds are signed by the Play app signing key, not the upload key (`my-upload-key.jks`). A release OAuth client registered against the upload key fails sign-in for every tester.
  - **Debug keystore:** `debug.keystore` isn't in the repo. A debug OAuth client is tied to one SHA-1, so every developer machine, and CI, needs the same debug keystore or its own client.
  - **Backup:** `allowBackup="true"`, and `data_extraction_rules.xml` is the untouched template, so the Room DB gets backed up by default. A restored mirror and outbox on a new phone would replay stale outbox entries against the Sheet. That breaks UJ-2's "the sheet is the record of truth".
  - **Release build:** `isMinifyEnabled = false`. There's no R8 decision for release.
  - **Manifest permissions:** the manifest declares `GET_ACCOUNTS`, which isn't needed under Credential Manager and hurts the Play review (NFR-3). `READ_SMS` isn't declared yet.
- **Fix:** add an "Operations" block to the spine:
  - Set the final applicationId and namespace now.
  - Register the release OAuth client with the Play App Signing SHA-1.
  - Commit a shared debug keystore, or document per-developer clients.
  - Set `allowBackup="false"` or exclude the `databases` domain in both backup XMLs.
  - Enable R8 for release, with keep rules for Moshi, Retrofit and Credential Manager.
  - Remove `GET_ACCOUNTS` and declare `READ_SMS`.
  - Keep the GitHub Actions job list as is.

### F6. Medium: outbox scope is ambiguous
- **Problem:** AD-3 says "a local create, edit or delete" without naming entities. AD-9 implies Category edits ride the outbox ("same outbox batch"). AD-13 writes Dismissed fingerprints to the Sheet but doesn't say whether that goes through the outbox. AD-8 Budget writes are unspecified.
- **Fix:** state that the outbox covers Expenses, Categories, Budgets and Dismissed. Give the entry shape: entity type, key, op, payload and `baseHash` (from F1). Say that entries are pushed in insertion order, and that consecutive entries for the same key are collapsed.

### F7. Medium: settings persistence is undecided (FR-22, FR-23)
- **Problem:** Currency and display name have a feature package (`settings`) but no storage decision. If they live in device prefs, a new phone loses them (UJ-2). If they live in the Sheet, AD-7 needs a tab.
- **Fix:** decide one. Either add a `Settings` tab (`Key`, `Value`) to AD-7, or state that they're device-local in DataStore and that a new phone resets them.

### F8. Medium: use-case shape contradicts itself; AD-2 and AD-15 lack enforcement
- **Problem:** AD-15 says ViewModels depend on "use-case interfaces". The Naming convention says use cases are `VerbNounUseCase` with an `operator fun invoke`, which implies concrete classes. Implementers will split. AD-2 (feature isolation) and AD-15 (ViewModels depend only on `:domain`) name no check, unlike AD-1 (Gradle) and AD-17 (Gradle task).
- **Fix:** pick one. Either use concrete use-case classes, with tests faking the repositories and gateways beneath them, or interfaces plus an `Impl`. Add Konsist (or ArchUnit) tests to CI asserting:
  - `:app` imports no `:data` types outside Hilt modules.
  - Feature packages import other features only through their `:domain` API.
  - ViewModel constructors take only `:domain` types.

### F9. Medium: Deferred items that can cause divergence
- **Two phones:** convergence holds only once F1 and F2 are adopted. Without ID-based addressing, two phones' position-based writes corrupt rows. Two phones signing in for the first time at the same moment can also create two Expense Sheets, which FR-2 forbids. Add a note that "find or create" re-checks after create, and that the newer duplicate is trashed if both exist.
- **Room migrations:** "v1 starts a new schema; migrations from first public release." The closed-test track already has real users. Any destructive fallback silently drops unpushed outbox entries. Turn on `exportSchema = true` now. Rule that destructive fallback is allowed only for mirror tables, and never for the outbox, or require a migration for every bump once on the closed track.

### F10. Medium: the brownfield stance is unstated
- **Problems:**
  - Every AD is `[ADOPTED]`. Nothing marks what's ratified from the code: Room, Compose and Material 3, Retrofit and Moshi, Robolectric and Roborazzi, `testTag`s, the version catalog, and keeping unused deps commented out.
  - The spine reverses several CLAUDE.md and `.github/copilot-instructions.md` rules: "no DI framework", prefs exposed as repository properties, "keep the OAuth scheme and host unchanged", and the single `:app` module. These files will keep steering agents the old way unless they're updated.
  - Existing Sheets created by today's code (`Sheet1`, headers `Local ID, Timestamp, Amount ($), …`, int IDs) have no migration stance.
  - The 80% and 100% alert thresholds in today's `ExpenseViewModel` are kept by FR-20 but aren't recorded as a ratified rule.
- **Fix:** mark the kept decisions `[RATIFIED]` and cite file paths. Add a "Migration from 26e51ad" note saying the legacy `Sheet1` layout is ignored, or imported once. Make a follow-up action to update CLAUDE.md and the copilot instructions when the spine is accepted.

### F11. Low: money minor units assume INR
- **Problem:** AD-6 fixes "paise" while FR-22 allows other Currencies. Formatting-only changes are fine. But the Sheet mapping divides by 100, so currencies with 0 or 3 minor digits would display wrongly.
- **Fix:** say "minor units, always 2 decimal places in v1 regardless of Currency." Alternatively, restrict the FR-22 choices to 2-decimal currencies.

### F12. Low: fingerprint stability
- **Problems:**
  - If a parser's Transaction Reference extraction changes, for example after a bug fix, the stored fingerprints no longer match, and handled SMS reappear.
  - Reference-based and hash-based fingerprints share one namespace.
- **Fix:** prefix the fingerprints (`ref:<bank>:<ref>` and `h:<sha256>`). Add a parser-test rule that extraction of the reference stays stable for the sample corpus.

### F13. Low: hand-typed unknown Category
- **Problem:** the spine doesn't say what a pull does with an Expense whose Category isn't on the Categories tab. Today's code auto-creates it with grey colour and no budget.
- **Fix:** ratify the current behaviour in AD-9: auto-add the Category to the Categories tab with default colour and icon.

## What is solid
- AD-1 (pure `:domain`), AD-5 (UUID IDs, with a correct diagnosis of today's `toIntOrNull() ?: 1`), AD-6 (Long paise), AD-8 (sparse budgets matching FR-18 and FR-19 with no scheduler), AD-12 (parser registry) and AD-13 (raw SMS never persisted) are crisp, enforceable and well aimed.
- AD-10 correctly retires the custom-scheme redirect and on-device client secret, as the addendum anticipated, and `drive.file` with app-created Sheets satisfies FR-2 and NFR-2.
- AD-17's 500-line Gradle check is concrete and directly addresses the 1,940-line `MainActivity.kt`.
- The Capability map covers FR-1 to FR-23 and NFR-1 and NFR-3. NFR-6 (Inbox within 2 seconds) and NFR-5 (covered implicitly by AD-10's memory-only tokens) should be listed explicitly.
