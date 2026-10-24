---
title: 'Reconciliation: architecture spine vs PRD'
reviewed: '../architecture-expense-tracker.md'
against: ['../../prd-expense-tracker/prd-expense-tracker.md', '../../prd-expense-tracker/addendum.md']
date: '2026-10-08'
verdict: 'Revise before build'
---

# Reconciliation: Architecture Spine vs PRD

## Verdict

**Revise before build.** The spine covers the PRD broadly. All 23 FRs and 6 NFRs are listed in `binds`, and the module, outbox, UUID, paise, sparse-budget and parser decisions fit the PRD well. But:

- One rule **contradicts** the PRD. AD-4 pushes the outbox before it pulls, so a pending in-app edit overwrites a newer Sheet edit. That breaks FR-9 and the UJ-3 edge case.
- The addendum's explicit ask, "Architecture must define how changes are detected", is still unanswered.
- Several requirements have no home in the spine: settings persistence (FR-22, FR-23), first-use seeding (FR-17), session lifecycle (FR-1, NFR-4), validation of hand-edited rows (FR-8), and keeping Added SMS suppressed after deletion (FR-16).

Severity key: **H** = contradicts the PRD or will cause data loss or a visible requirement failure. **M** = the requirement has no home, and builders will each invent their own rule. **L** = needs clarifying, or the risk is small.

## Top findings

### G1 (H). Push-first sync overwrites Sheet edits, and there is no change detection
- **PRD:** FR-9, UJ-3 edge case ("the sheet's version is kept"), addendum "Offline queue vs. sheet-as-truth" ("Architecture must define how changes are detected"), FR-5.
- **Spine:** AD-4: "A sync run pushes the outbox, then pulls every tab… A conflict on the same Expense resolves to the Sheet's version, and the losing outbox entry is dropped."
- **Problem:** in the push step, the queued in-app edit is written over whatever the Sheet holds now. By the time the pull runs, the Sheet's newer edit is already gone, so there's nothing left for "Sheet wins" to keep. No AD defines how a conflict is detected: there's no base version, row hash or last-pulled snapshot stored with an outbox entry. Row addressing isn't specified either. If the User sorts or inserts rows in the Sheet between pull and push, an update or delete aimed at a stale row index (FR-5) hits the wrong row. The same gap leaves app-delete vs Sheet-edit, and app-edit vs Sheet-delete, undefined.
- **Fix:** amend AD-4 (or add an AD). Each update or delete outbox entry stores the row values it was based on, or their hash, from the last pull. The push step re-reads the target rows by `ID` just before writing. If the current Sheet row differs from the base, or is missing, the entry is dropped and the Sheet version kept, and the User sees a non-blocking message. Writes and deletes locate rows by `ID` at write time, never by a cached index. Spell out the policy for all four combinations: edit/edit, edit/delete, delete/edit and delete/delete.

### G2 (H). Added SMS can come back to Inbox, and reference-only fingerprints can collide
- **PRD:** FR-16 ("A Dismissed Inbox Item, or one already Added, never reappears… remembered in the Expense Sheet"), UJ-1 Resolution, FR-12.
- **Spine:** AD-13 records "Added" only as the Expense row's `SMS ID`. AD-4 replaces the mirror with the Sheet's contents.
- **Problem 1:** when the User deletes an Expense that came from an SMS, in the app (FR-5) or by deleting the row in the Sheet (FR-8), its `SMS ID` disappears with it. The fingerprint is then in neither place, so the SMS reappears in Inbox the next time that day is read. The same happens if the User clears the `SMS ID` cell by hand.
- **Problem 2:** the fingerprint is the bare Transaction Reference. Some banks' card SMS carry short auth or reference codes, which can repeat across days or banks. One Dismiss would then hide a different, real transaction forever.
- **Fix:**
  - Deleting an SMS-sourced Expense in the app also appends its fingerprint to the Dismissed tab, in the same outbox batch. You could rename the tab "Handled", or add a `Reason` column.
  - When a pull finds that a row with an `SMS ID` has disappeared, the sync engine copies that `SMS ID` to Dismissed.
  - Namespace reference-based fingerprints, for example `bank|ref|amountPaise`. This still collapses true duplicates (FR-12) but stops cross-transaction collisions. Document it in AD-13.

### G3 (H). Session lifecycle: offline restart, sign-out and switching accounts
- **PRD:** FR-1 ("Signing out returns to the sign-in screen and removes the account's tokens"), NFR-4 ("every core action except sign-in… works without a connection"), NFR-5, Glossary "User" ("Nothing is shared between Users"), §2 (a small group of people, each with private data).
- **Spine:** AD-10 keeps access tokens in memory only and persists nothing. AD-11 `AccountGateway` has "sign out". Nothing else is said.
- **Problems:**
  - (a) After a process restart with no network, the app has no persisted record that a User is signed in. If "signed in" means a successful silent authorization, the app is stuck on the sign-in screen offline, which breaks NFR-4.
  - (b) Sign-out doesn't define what happens to the Room mirror, the outbox, scheduled WorkManager jobs, settings or SMS fingerprints. Unpushed outbox entries could be lost silently. Or the next account signed in on the same phone could see the previous User's expenses, or push that User's outbox into its own Sheet.
  - (c) Revoked access, or a silent token request that needs user consent, has no defined behavior. This matters most inside the WorkManager job, which can't show UI.
- **Fix:** add a session AD covering these rules:
  - Persist a non-secret signed-in account marker (account ID and email) so the app can start offline.
  - On sign-out, warn if the outbox isn't empty. Then cancel the sync work, clear Room (mirror, outbox, fingerprints) and per-user settings, and call `clearCredentialState`.
  - Either key the Room data to the account, or wipe it on any account change.
  - When a token needs consent, the background sync fails softly and flags "re-authorize" for the next foreground session.

### G4 (M). Hand-edited Sheet rows have no validation or parsing contract
- **PRD:** FR-8, UJ-3 step 3 (a hand-typed cash expense with Source left blank), §1 ("open and edit them anywhere"), FR-17 (unique names), FR-18.
- **Spine:** AD-7 fixes header names. AD-5 handles a blank `ID` (and sets Source = User only in that case). The Dates convention fixes `yyyy-MM-dd HH:mm`.
- **Problems:**
  - FR-8 says a blank **Source** is filled with User. AD-5 does that only when the ID is also blank, so a row with an ID but no Source is unhandled.
  - Nothing says what happens to rows people really type: `₹1,200`, `1200.5`, a Sheets date serial or a locale-formatted date, a negative or zero amount, a Category not in the Categories tab, case or whitespace variants such as `food ` vs `Food`, or duplicate `(Month, Category)` rows in Budgets.
  - The Sheets read and write modes aren't fixed (`valueRenderOption`, `valueInputOption` RAW vs USER_ENTERED), and they decide how dates and amounts round-trip.
  - The value formats for Color and Icon aren't specified.
- **Fix:** extend AD-7 with a row-parsing contract:
  - Fix the read and write value options.
  - List the accepted amount and date forms.
  - Unknown Category: auto-create it, or map it to an "Uncategorized" category.
  - Invalid rows are skipped, left untouched in the Sheet, and reported through a sync message. They're never deleted or rewritten.
  - Blank Source becomes User regardless of ID.
  - Category matching: trimmed and case-insensitive, or exact. Pick one.
  - `(Month, Category)` is unique in Budgets, and the last row wins.
  - Color is a hex string, and Icon is a key from a fixed set.

### G5 (M). Finding the Sheet and seeding defaults on first use
- **PRD:** FR-2 (reuse, no second sheet, recreate if deleted), UJ-2 edge case, FR-17 ("Default Categories exist on first use"), addendum "Finding the sheet again".
- **Spine:** AD-11 "find or create the Expense Sheet". No rule for how it's found or seeded.
- **Problems:**
  - It doesn't say how the sheet is identified: by name, by a Drive `appProperties` marker, or by a stored file ID.
  - It doesn't say what happens when several matches exist. That could follow a race between two phones on first sign-in (OQ3), or a copy the User made.
  - Trashed files: Drive still returns them and allows writing to them unless the query adds `trashed=false`. Without that filter, a sheet the User "deleted" (UJ-2 edge case) keeps being used.
  - A sheet deleted mid-session (sync gets 404) has no defined behavior: recreate it, and what happens to the mirror and outbox?
  - Default Categories have no owner. If each install seeds them locally and pushes them, a new phone duplicates them, or brings back defaults the User deleted.
- **Fix:** add these rules to AD-7 or AD-11:
  - Identify the sheet by an `appProperties` key (for example `expenseTrackerSheet=v1`), using `drive.file` with `trashed=false`.
  - If several match, pick the oldest-created one and log the rest. Never write to more than one.
  - Write the default Categories only when the Sheet is created, never on sign-in to an existing Sheet.
  - On 404 mid-session, follow the UJ-2 edge-case rule: create a fresh sheet, clear the mirror, drop the outbox, and tell the User. Also confirm that the debug and release OAuth clients share one Cloud project, because `drive.file` grants are per project.

### G6 (M). Privacy: Android Auto Backup, and leaks through saved state
- **PRD:** NFR-1 ("Data goes only to the User's own Expense Sheet… nothing to any server of its own"), §5 "No app server", UJ-2 (the new phone gets data from the Sheet).
- **Spine:** silent on backup. AD-13 says raw SMS text is "held in memory only".
- **Problems:**
  - With the default `allowBackup`, Android backs up the Room DB (expenses, outbox, fingerprints) and preferences to Google's backup service. That's a second copy outside the Expense Sheet.
  - When that backup is restored onto a new phone or a reinstall, a stale outbox replays and a stale mirror shows. That can push duplicate or out-of-date rows, which contradicts "the sheet is the record of truth" (UJ-2).
  - Separately, "memory only" can quietly break if SMS text goes into `rememberSaveable`, `SavedStateHandle` or navigation arguments. The system persists those in the saved-instance state.
- **Fix:**
  - Add a privacy rule: set `allowBackup=false`, or `dataExtractionRules` that exclude the DB and preferences, so a new phone always rebuilds from the Sheet.
  - Extend AD-13: raw SMS text is never put in saved state or navigation arguments. Pass only a fingerprint between screens and re-read the SMS when needed.
  - Record these choices for the Data safety form (NFR-3).

## Further findings

| # | Sev | PRD ref | Finding | Suggested fix |
| --- | --- | --- | --- | --- |
| F1 | M | FR-22, FR-23 | Currency and display name have no storage or home. AD-7 has no Settings tab, and no local store is named. On a new phone, Currency reverts to INR, which goes against the UJ-2 promise of "everything back". | Decide whether settings roam, for example in a `Settings` tab (`Key`, `Value`), or stay on the device in DataStore. Add the choice to AD-7 or the Capability Map. |
| F2 | M | NFR-4, FR-16 | Dismiss is a pending change, but AD-3 lists only "create, edit or delete". It doesn't say Dismiss rows go through the outbox, or that Room mirrors the Dismissed tab so Inbox works offline. | State in AD-3 that Dismiss is an outbox operation. In AD-13, say the handled-fingerprint check reads only the Room mirror. |
| F3 | L | NFR-6 | AD-13 says "any fingerprint present in either place", which reads as the Sheet. A network check in the Inbox path would break the 2-second target. NFR-6 is bound only to AD-3. | Bind NFR-6 to AD-12 and AD-13. Inbox does no network I/O, and SMS reading and parsing run off the main thread. |
| F4 | L | FR-11 | The Testing convention covers only positive parser samples. `GenericDebitParser` as a catch-all could turn promos like "₹500 will be debited… offer" into Inbox Items, which FR-11 forbids ("never"). | Require negative samples (credits, OTPs, promos, payment requests or "collect" SMS, failed transactions) for every parser and for the generic parser. |
| F5 | L | FR-18, FR-19 | AD-8 has no way to remove a Budget from month M onward, other than an amount of 0. It also leaves old Budgets rows behind for a deleted Category, and they come back if the name is reused. | Treat Amount `0` or blank as "no Budget". When a Category is deleted, also delete or ignore its Budgets rows (AD-9). |
| F6 | L | FR-20, UJ-4 Resolution | "Current Budget Month" isn't tied to a clock source. If the app stays open past midnight on the 1st, it keeps showing last month. | Derive the current `YearMonth` from an injectable clock flow that ticks at the month boundary or on resume. |
| F7 | L | §4.4 "nothing runs in the background", NFR-3 | AD-14 adds a background WorkManager sync. That doesn't contradict the PRD, since the PRD statement is about SMS, but it should be explicit for the Play SMS declaration and the Data safety form. | State that the worker never touches `SmsSource`, and that `READ_SMS` is used only while Inbox is in the foreground. |
| F8 | L | NFR-3 | Play compliance is mapped only to AD-10 and AD-12. The Data safety form, the Financial features declaration, the privacy policy and a manifest permission audit (no transitive `RECEIVE_SMS` or `INTERNET`-only SDKs) have no owner. | Add a release-checklist item, or a CI manifest-permission check, under the release-process row. |
| F9 | L | NFR-5 | NFR-5 assumes tokens are stored securely. AD-10 stores none, which is stronger and acceptable. NFR-5's "removed on sign-out" still needs explicit revoke and clear calls. | Merge into the G3 session AD. |
| F10 | L | FR-22 | "Paise", with a fixed ×100 minor unit, is INR-specific. Currencies with 0 or 3 decimals would format wrongly if the formatter assumes two decimals. | Keep storage at ×100, and make the formatter decimal-aware per currency, or limit the picker to 2-decimal currencies. |
| F11 | L | addendum "Data-model shift" | AD-4 replaces Room as the source of truth, and "v1 starts a new schema". The owner's existing local Room data from today's app would be lost on upgrade. | Decide on a one-time export of the existing local rows to the new Sheet, or record that the loss is accepted. |
| F12 | L | Dates convention, FR-4, FR-18 | The Sheet stores local time with no offset, and Room stores epoch milliseconds. A time-zone change shifts every Expense on the next pull, and can move an Expense into a different Budget Month. | Accept and document it, or treat Sheet dates as zone-less local date-times in `:domain`. |

## Coverage check (FR and NFR)

| Req | Housed by | Status |
| --- | --- | --- |
| FR-1 | AD-10, AD-11 | Partial: session and sign-out lifecycle (G3) |
| FR-2 | AD-7, AD-10, AD-11 | Partial: how the sheet is found, trash, 404 (G5) |
| FR-3 | AD-3, AD-5, AD-6 | OK. Put validation in a use case, not the UI. |
| FR-4 | AD-3 | OK |
| FR-5 | AD-3, AD-4 | Partial: row addressing and delete conflicts (G1) |
| FR-6 | AD-3, AD-4 | OK, subject to G1 |
| FR-7 | AD-3, AD-14 | OK. The marker comes from the outbox. |
| FR-8 | AD-4, AD-5, AD-14 | Partial: hand-edit parsing, blank Source (G4) |
| FR-9 | AD-4 | **Contradicted** (G1) |
| FR-10 | AD-12 (on-demand) | OK. Permission UX belongs to UX. |
| FR-11 | AD-12 | OK. Add negative tests (F4). |
| FR-12 | AD-13 | Partial: fingerprint collisions (G2) |
| FR-13 | AD-12, AD-13 | OK |
| FR-14 | AD-12 | OK |
| FR-15 | AD-13, AD-5 | OK |
| FR-16 | AD-13, AD-7 | **Broken on delete** (G2), outbox for Dismiss (F2) |
| FR-17 | AD-9 | Partial: default seeding (G5), name matching (G4) |
| FR-18 | AD-8 | OK, minor gaps (F5) |
| FR-19 | AD-8 | OK |
| FR-20 | AD-8, AD-6 | OK, minor gap (F6) |
| FR-21 | AD-3, AD-6 | OK |
| FR-22 | AD-6 (format only) | **Unhoused storage** (F1, F10) |
| FR-23 | none beyond the `settings` package | **Unhoused** (F1) |
| NFR-1 | AD-13, Logging, Deferred: Observability | Partial: Auto Backup, saved state (G6) |
| NFR-2 | AD-10 | OK |
| NFR-3 | AD-10, AD-12 | Partial: release obligations without an owner (F7, F8) |
| NFR-4 | AD-3, AD-14 | Partial: offline restart (G3), offline Dismiss (F2) |
| NFR-5 | AD-10 | OK, stronger than asked. Sign-out clearing is in G3. |
| NFR-6 | AD-3 | Partial: Inbox path not bound (F3) |

## Journey edge cases

| Journey | Edge case | Status |
| --- | --- | --- |
| UJ-1 | Duplicate SMS with no reference appears twice, and the User Dismisses one | OK: the hash includes the timestamp (AD-13) |
| UJ-1 | Added or Dismissed SMS never reappear | Broken when an Added Expense is deleted (G2) |
| UJ-2 | Deleted Sheet means the next sign-in creates a fresh one | Partial: trashed files, deletion mid-session (G5) |
| UJ-2 | New phone shows full history with no import | OK (AD-4 pull), provided Auto Backup doesn't restore stale state (G6) |
| UJ-3 | Offline add, then push with Source = App | OK (AD-3, AD-14) |
| UJ-3 | Blank Source in the Sheet becomes User | Partial: only when the ID is blank (G4) |
| UJ-3 | Sheet wins a conflict with a pending offline edit | **Contradicted** (G1) |
| UJ-4 | Month rollover copies Budgets | OK (AD-8, sparse resolution) |
| UJ-4 | Past months keep their Budget | OK (AD-8) |
