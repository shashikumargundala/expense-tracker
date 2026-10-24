---
type: architecture-review
lens: adversary
target: ../architecture-expense-tracker.md
reviewed: '2026-10-08'
verdict: 'NOT READY TO BUILD: 9 holes, 3 critical'
---

# Adversarial Review: Architecture Spine, Expense Tracker

## Method

For each focus area I built two units one level down, such as a use case and the sync engine, or the push step and the pull step. Each unit obeys every AD to the letter, yet the two still build incompatible behavior. Every pair below is a hole. Each hole comes with an exact AD change that closes it. I didn't edit the spine.

## Verdict

**Not ready to build.** The module and feature skeleton (AD-1, AD-2, AD-11, AD-12, AD-15 to AD-17) is sound. The sync contract (AD-3, AD-4, AD-5) isn't. It says *that* the outbox is pushed and the mirror is replaced, but not *how* a push finds its row, how a conflict is detected, what the outbox can hold, or how replacement interacts with concurrent writes. Two competent implementers would build sync engines that lose data in different ways. Holes 1 to 3 must be closed before any sync or expense story starts. Holes 4 to 6 must be closed before the inbox, category and budget stories start.

| # | Severity | Area | Short name |
| --- | --- | --- | --- |
| 1 | Critical | Outbox vs Sheet-wins (AD-3/4/5) | Push-first makes conflicts undetectable, and retries double rows |
| 2 | Critical | Mirror replacement vs UI and writers | Replacement drops in-flight writes and flashes empty state |
| 3 | Critical | Outbox scope / Dismissed tab | Outbox and conflict rule only define Expenses, so other tabs have no write path |
| 4 | High | SMS fingerprints after delete | Deleting an Added Expense resurrects its Inbox Item |
| 5 | High | Category delete and reassignment | Frozen reassign list, orphan names, budgets of deleted Categories, resurrected defaults |
| 6 | High | Time zones in dates | Instant in Room vs zone-local text in the Sheet drifts and splits months |
| 7 | Medium | Budget resolution | Duplicate (Month, Category) rows, no "remove budget", month stamped at push |
| 8 | Medium | Source handling | Source rewritten on edit, never repaired when ID is present, copied with duplicates |
| 9 | Medium | Account switch / Sheet identity | Outbox of account A pushed to account B's Sheet |

---

## Hole 1 (Critical): Push-first makes Sheet-wins undetectable, and non-idempotent appends double rows

**ADs involved:** AD-3, AD-4, AD-5, FR-9.

**Pair A (lost hand edit):**
- *Unit P, the push step.* AD-4 says "a sync run pushes the outbox". P pushes a pending in-app edit by finding row ID X and writing the outbox values over it.
- *Unit Q, the pull and conflict step.* AD-4 says "a conflict resolves to the Sheet's version, and the losing outbox entry is dropped". Q looks for conflicts between pulled rows and *unpushed* outbox entries.
- *Result:* P has already overwritten the User's laptop edit before Q runs. Q finds no unpushed entry, so it never sees a conflict. FR-9 is violated every time, and both units obeyed AD-4. The same happens to deletes: a pending in-app delete removes a row the User just corrected in the Sheet. A pending edit to a row the User deleted in the Sheet either fails or gets appended back, depending on P's author.

**Pair B (doubled Expense):**
- *Unit P* appends a create with `values.append`. The write succeeds but the response is lost to a timeout, so the outbox entry stays.
- *The next run's P* appends it again, so the Sheet now has two rows with the same UUID.
- *Unit N, the pull normalizer, applies AD-5 to the letter.* The second row gets a fresh UUID, which is written back. The Expense is now permanently doubled, it counts twice against the Budget, and nothing ever reconciles it.

**Also unspecified:** whether push addresses rows by index or by ID. Rows inserted or sorted by hand between read and write shift indexes, so an index-based write lands on the wrong Expense. Whether push writes whole rows is also open: a whole-row `update` blanks unknown columns interleaved between known ones, which breaks AD-7.

**Proposed change: replace AD-4's Rule with:**
> Only the `sync` feature in `:data` calls `ExpenseSheetGateway`. Every outbox entry records the **base**: the row's mapped values as last pulled, or "absent" for a create. A sync run (1) reads all four tabs fresh, (2) for each outbox entry in order, locates its row **by ID in that fresh read**, never by a cached row index, and compares it with the base: if they differ, the Sheet wins and the entry is dropped; if they match, the entry is applied, (3) pulls and replaces the mirror (AD-18). Applying is idempotent: a create whose ID already exists in the fresh read becomes an update of that row, and a delete whose ID is absent is a no-op. Writes touch only the cells of mapped columns, never whole rows. An entry leaves the outbox only after the Sheets API confirms its write.

**And tighten AD-5:** "When two rows share an ID and every mapped value is equal, the later rows are deleted as retry duplicates. Otherwise the first row in sheet order keeps the ID and each later row gets a new UUID, written back."

---

## Hole 2 (Critical): Mirror replacement drops in-flight writes and flashes empty state

**ADs involved:** AD-3, AD-4, AD-15, FR-7.

**Pair A (vanishing Expense):**
- *Unit U, `AddExpenseUseCase`.* Obeying AD-3, it writes the Expense and its outbox entry in one Room transaction.
- *Unit S, the sync engine.* Obeying AD-4, it snapshots the unpushed outbox, pulls the Sheet over a 2-second network call, then replaces the mirror and re-applies the snapshot.
- *Result:* the User adds an Expense during that 2-second window. U commits after S took its snapshot, so S's replacement erases the new Expense from Room. It's still in the outbox, so it comes back on the *next* sync, but FR-7 ("visible immediately, counts toward Budgets immediately") is broken until then.

**Pair B (flicker and false alerts):**
- *Unit S* implements "replace" as `DELETE FROM expense` followed by batched inserts, which AD-4 permits.
- *Unit H, the home ViewModel.* Obeying AD-15, it maps the `Flow` to `UiState`.
- *Result:* H briefly sees zero Expenses and then a partial set. Amount Left jumps, warnings appear and vanish, and list scroll position resets. If the Categories tab read fails after the Expenses tab read succeeds, S may replace one table and not the other, leaving Expenses that point at unknown Categories.

**Also unspecified:** where the FR-7 "waiting to sync" marker comes from. One author adds a `synced` column to the Expense table, another derives the marker from the outbox, and the two disagree after a dropped conflict.

**Proposed new AD-18: Mirror replacement is atomic and diff-based.**
> The sync engine replaces the mirror only after all four tabs are read successfully. If any read fails, the mirror is left untouched. Replacement runs in **one** Room transaction that (a) reads the outbox inside that transaction, (b) computes the new mirror as Sheet contents plus every outbox entry present at that moment, and (c) applies it as a diff: insert new rows, update changed rows, delete missing rows. It never clears a table first. Local write transactions (AD-3) and replacement serialize on Room's transaction lock, so every local write is either already in the outbox when replacement reads it, or applied after replacement. "Pending Sync" is derived only from the existence of an outbox entry for that ID. No entity carries a `synced` flag.

---

## Hole 3 (Critical): Outbox and conflict rule only define Expenses, so the other tabs have no write path

**ADs involved:** AD-3, AD-4, AD-7, AD-8, AD-9, AD-13, NFR-4.

**The pair:**
- *Unit I, the inbox Dismiss use case.* Obeying AD-13, it "writes the fingerprint to the Dismissed tab". AD-4 forbids it from calling the gateway, so it writes a Room `dismissed` row plus an outbox entry (AD-3).
- *Unit S, the sync engine.* Its author read AD-4's "conflict on the same Expense" and "re-applying outbox entries" and built an outbox keyed by Expense ID with create, edit and delete operations only. The author of I wrote a Dismissal entry that S's schema can't express, or I wrote only to Room.
- *Result:* the next pull replaces the mirror from the Sheet. The Dismissed tab doesn't yet contain the fingerprint, so the Room row is wiped and the Inbox Item reappears (FR-16 broken). Budgets (AD-8, "a row is written when the User sets a Budget", offline per NFR-4) and Category create and delete (AD-9, "same outbox batch") hit the same gap. No AD names the outbox entry types, their keys, or the conflict rule for non-Expense rows.

**Also:** the Dismissed tab is append-only in intent, but nothing says so. A Dismissal entry whose fingerprint is already present needs to be a no-op, or two phones produce duplicate rows. Nothing says who reads `Dismissed On` (no one, which is fine), or its format.

**Proposed change: tighten AD-3's Rule:**
> Every screen reads data only from Room, via `:domain` repository interfaces that return `Flow`. Every local change to any tab is written to Room and appended to the outbox in the same Room transaction. Nothing else records a pending change. The outbox entry types are closed and live in `:domain` `sync`: `UpsertExpense(id)`, `DeleteExpense(id)`, `UpsertCategory(name)`, `DeleteCategory(name, reassignTo)`, `UpsertBudget(month, category)`, `AddDismissal(fingerprint)`. Each entry's key is the row key named in AD-7. The base-compare and Sheet-wins rule of AD-4 applies to every type by that key. `AddDismissal` is a no-op when the fingerprint is already present. Dismissed rows are never edited or deleted by the app, and `Dismissed On` is `yyyy-MM-dd`.

**And add to AD-7:** "Row keys: Expenses `ID`, Categories `Name`, Budgets (`Month`, `Category`), Dismissed `SMS ID`."

---

## Hole 4 (High): Deleting an Added Expense resurrects its Inbox Item

**ADs involved:** AD-13, AD-3, AD-5, FR-5, FR-16.

**The pair:**
- *Unit D, `DeleteExpenseUseCase`.* Obeying AD-3 and FR-5, it deletes the Expense, so the row and its `SMS ID` leave the Sheet.
- *Unit F, the inbox filter.* Obeying AD-13, it hides any fingerprint "present in either place", meaning an Expense's `SMS ID` or the Dismissed tab.
- *Result:* the fingerprint is now in neither place. The next time Inbox reads that day (FR-14 Load more), the SMS is offered again, breaking FR-16 ("one already Added never reappears"). The same happens when the User clears the `SMS ID` cell or deletes the row in the Sheet.

**Related pairs:**
- *Duplicate rows.* AD-5's duplicate-ID repair keeps `SMS ID` on both rows, so two Expenses claim one SMS.
- *Unstable fingerprint.* The fingerprint depends on which parser extracted a Transaction Reference. If a new bank parser (AD-12) extracts a reference where `GenericDebitParser` didn't, an already-Added SMS gets a new fingerprint, from hash to reference, and reappears.
- *Namespace collision.* A reference and a hash share one namespace, and references aren't namespaced by bank.

**Proposed change: tighten AD-13's Rule:**
> An SMS fingerprint is `ref:<bank>:<reference>` when the parser extracts a Transaction Reference, otherwise `h:<hex SHA-256 of sender, timestamp millis and body>`. Its format is frozen once released. Changing it requires keeping the old one as an alias. Inbox shows at most one Inbox Item per fingerprint and hides an SMS if **either** of its possible fingerprints (`ref:` or `h:`) is known. Adding writes the fingerprint to the Expense's `SMS ID`. Dismissing appends it to the Dismissed tab. **Deleting an Expense whose `SMS ID` is non-blank appends that fingerprint to the Dismissed tab in the same Room transaction and outbox batch.** During a pull, a fingerprint seen in an Expense row is also copied into Dismissed if it's absent there, so that hand deletes in the Sheet don't resurrect it. When AD-5 gives a duplicate row a new UUID, it also clears that row's `SMS ID`. Raw SMS text is held in memory only while Inbox is open. It is never written to Room, the Sheet, the outbox or logs.

---

## Hole 5 (High): Category delete freezes its reassign list; orphan names, budgets and defaults are unowned

**ADs involved:** AD-9, AD-3, AD-4, AD-8, FR-17.

**Pair A (frozen reassign list):**
- *Unit C, `DeleteCategoryUseCase`.* Obeying AD-9, it emits one `UpsertExpense` per Expense currently in Room with Category "Travel", all in one batch.
- *Unit S, the sync engine.* Obeying AD-4, it drops any entry whose row the User edited in the Sheet (Sheet wins), and it pulls rows the User hand-typed with "Travel" after C ran.
- *Result:* after sync, some Expenses still reference the deleted Category. Every screen that groups by Category now meets a name with no color, icon or Budget, and each author handles it differently: one hides the Expenses, one crashes on a null lookup, one shows "Uncategorized".

**Pair B (orphan names and stale budgets):**
- *Hand-typed names.* The User types `food` or `Food ` in the Sheet. AD-9 says "exact name", so the pull keeps it as a separate, unknown Category.
- *Stale budgets.* Budgets rows for a deleted Category remain. If the User later re-creates "Travel", `GetEffectiveBudget` (AD-8) silently revives the old budget.

**Pair C (resurrected defaults):**
- *Unit B, the default-Category bootstrap.* It runs on first launch and writes defaults through the outbox (AD-3).
- *Unit S* pushes them into the User's existing Sheet on a new phone (FR-2).
- *Result:* Categories the User had deleted come back, and colors the User changed are overwritten.

**Proposed change: replace AD-9's Rule with:**
> An Expense and a Budget refer to their Category by name. Names are compared after trimming and case-folding. That folded form is the Category key and must be unique. There is no Category ID. Deleting a Category is a single `DeleteCategory(name, reassignTo)` outbox entry. **At push time**, the sync engine rewrites the Category of every Expense row in the fresh read that matches the name, deletes every Budgets row for that name, and then deletes the Categories row. It's exempt from the base-compare of AD-4 for the Expense rows it rewrites. During a pull, any Category name used by an Expense or Budget but missing from the Categories tab is added to the Categories tab with a default color and icon, written back. Default Categories are written only by the sync engine when it **creates** a new Expense Sheet (FR-2), never by a local bootstrap.

---

## Hole 6 (High): An instant in Room vs zone-local text in the Sheet drifts and splits months

**ADs involved:** the Dates and months convention, AD-7, AD-8, FR-18, FR-20.

**Pair A (drift):**
- *Unit M, the Sheet mapper.* Obeying the convention, it formats the Room epoch millis as `yyyy-MM-dd HH:mm` in the *current* device zone on push, and parses it in the *current* device zone on pull.
- *Unit S, the sync engine.* It replaces the mirror on every sync.
- *Result:* if the device zone changes between push and pull (travel, or a manual setting), every Expense's instant shifts by the offset on the next pull. Any later edit pushes the shifted value. Precision also changes: Room keeps milliseconds, the Sheet keeps minutes, so the base-compare in Hole 1 sees a false difference on every row unless both sides use the same precision.

**Pair B (month split):**
- *Unit G, `GetEffectiveBudget` plus the spend query.* It buckets Room instants into `YearMonth` using the device zone at query time.
- *Unit A, analytics (FR-21).* It's written by someone else and buckets with `ZoneOffset.UTC` or a SQL `strftime`.
- *Result:* an Expense at 23:30 on the 31st falls in different months on the home page and in analytics.

**Pair C (Sheets date coercion):**
- *Unit W, the writer.* It writes with `valueInputOption=USER_ENTERED`, so Sheets turns the text into a date serial.
- *Unit R, the reader.* It reads with `UNFORMATTED_VALUE` and expects a string, or reads `FORMATTED_VALUE` and gets the spreadsheet's locale format.
- *Result:* hand-typed dates show the same mismatch.

**Proposed new AD-19: An Expense's time is a wall-clock local date-time.**
> An Expense's date is a `java.time.LocalDateTime` truncated to the minute in `:domain`. Room stores it as ISO text `yyyy-MM-dd'T'HH:mm` or as epoch-minutes of that local time read as UTC. No other zone conversion happens after creation. An SMS timestamp becomes a `LocalDateTime` once, in the device zone, when the Inbox Item is built. The Sheet `Date` cell is written with `USER_ENTERED` as `yyyy-MM-dd HH:mm`. It's read with `valueRenderOption=UNFORMATTED_VALUE` and `dateTimeRenderOption=SERIAL_NUMBER`, and serial numbers are converted without a zone. Text that matches `yyyy-MM-dd[ HH:mm]` is also accepted. An Expense's Budget Month is `YearMonth.from(date)`, computed only in `:domain`. "Current Budget Month" is `YearMonth.now()` from an injected `Clock`.

---

## Hole 7 (Medium): Budget resolution has duplicate keys, no removal, and an ambiguous month stamp

**ADs involved:** AD-8, AD-3, FR-19.

**Pair A (tie):**
- *Unit E, the edit-Budget use case.* It appends a Budgets row on each change, which satisfies "a row is written when the User sets or changes a Budget".
- *Unit G, `GetEffectiveBudget`.* It picks "the row with the greatest Month".
- *Result:* two edits in one month give two rows with the same Month, so G's tie-break is undefined. One author takes the first row, another the last.

**Pair B (month stamp):**
- *Unit E* edits offline on 31 October.
- *Unit S* pushes on 1 November and stamps `Month` from its own clock.
- *Result:* the change lands in November, not October.

**Pair C (no removal):** there's no way to say "no Budget from this month on". One author writes Amount 0, another deletes the row, and deleting reverts to an older month's Budget.

**Proposed change: append to AD-8's Rule:**
> (`Month`, `Category`) is the unique key of a Budgets row. Setting a Budget upserts the row for the current Budget Month, which is stamped when the User acts and carried in the outbox entry. If a pull finds duplicate keys, the last row in sheet order wins and the others are deleted on write-back. An Amount of 0 means no Budget from that month on. A Budget whose Category doesn't exist is ignored by `GetEffectiveBudget`.

---

## Hole 8 (Medium): Source is rewritten on edit, never repaired when ID is present, and copied with duplicates

**ADs involved:** AD-5, AD-7, FR-3, FR-8.

**Pair A (Source rewritten):**
- *Unit X, `EditExpenseUseCase`.* It builds a full `Expense` with Source = App, because "the app wrote it" (FR-3).
- *Unit N, the pull normalizer.* It sets Source = User only on the blank-ID path, as AD-5 says.
- *Result:* an in-app edit of a hand-typed row flips it to App.

**Pair B (Source never repaired):**
- *Hand edit.* The User types a row with an ID and a blank Source, or `app` in lowercase.
- *Unit N* leaves it alone because the ID isn't blank.
- *Result:* FR-8 ("blank Source filled in as User") goes unmet, and parsers diverge on case.

**Pair C (Source copied with duplicates):**
- *Hand copy.* The User copy-pastes an App row in the Sheet.
- *Unit N* gives the copy a new UUID, as AD-5 says, but keeps Source = App and the SMS ID.

**Proposed change: add to AD-5, or a new AD-20:**
> Source is set once, when the Expense is created: App by the app, User by the pull for a row that arrives without an app-assigned ID. Edits never change it. During a pull, a Source cell that isn't `App` or `User` after trimming and case-folding is set to `User` and written back, whatever the ID. A row that AD-5 re-IDs as a duplicate gets Source = User and a blank `SMS ID`.

---

## Hole 9 (Medium): Account switch and Sheet identity

**ADs involved:** AD-3, AD-10, AD-11, FR-1, FR-2.

**Pair A (wrong Sheet):**
- *Unit O, sign-out.* Obeying AD-10, it clears in-memory tokens.
- *Unit S, the sync engine.* After a different account signs in, S pushes the outbox left over from account A into account B's Sheet.

**Pair B (two Sheets):**
- *Unit F, the find-or-create step (AD-11).* It searches Drive by title.
- *Result:* it finds two "Expense Tracker" Sheets, for example after a reinstall race, and picks either one. Nothing says how the Sheet is identified.

**NFR-5 conflict:** NFR-5 says tokens are "stored with secure storage". AD-10 says tokens are held only in memory. The ADs pass, but the PRD's acceptance criterion would fail review.

**Proposed change: append to AD-10:**
> Room and the outbox belong to one signed-in account. Signing out warns if the outbox is non-empty, then deletes the Room database. The Expense Sheet is identified by a Drive `appProperties` marker (`expenseTrackerSheet=v1`). If more than one file matches, the most recently modified one is used and the others are left alone. AD-10 supersedes NFR-5's "stored" wording: tokens are never persisted. Record this in the PRD.

---

## Pairs considered and found closed

- *Two writers to the Sheet:* AD-4 closes it.
- *Parsers editing each other:* AD-12 closes it.
- *`Double` money in a domain calculation:* AD-6 closes it, provided the Sheet mapper rounds half-even when it parses hand-typed decimals. Worth one line in AD-6.
- *Column reorder:* AD-7's header mapping closes it. Duplicate header names in the Sheet are still unaddressed; suggest "the first matching header wins".
