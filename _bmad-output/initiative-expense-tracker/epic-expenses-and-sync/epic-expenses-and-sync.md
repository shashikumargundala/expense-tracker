---
type: epic
title: "Expenses work offline and the Sheet stays the truth"
parent: initiative-expense-tracker
covers: [CAP-2, CAP-3, CAP-4, CAP-5, CAP-6, CAP-7, CAP-17, CAP-18, CAP-19]
after: []
assignee: ""
risk: high
---

# Expenses work offline and the Sheet stays the truth

## Description

Build the Room mirror, the outbox and the one sync engine the spine defines, then the Expenses tab on top of it, so every change works offline and the Expense Sheet stays the record of truth.

## Outcome

Shylock adds, edits and deletes Expenses anywhere, online or not, and his Sheet and app always agree; CAP-3 to CAP-7's success checks show it.

## Done when

1. The sync engine applies all seven outbox types (AD-3) across all five tabs, by row key with the base check, idempotently, including DeleteCategory's reassign push, and refreshes the mirror atomically (AD-4, AD-5, AD-7, AD-9, AD-18); its test matrix passes against the fake ExpenseSheetGateway.
2. Expenses can be added, edited (bottom-sheet form, Save disabled until a Category is picked) and deleted (swipe with confirmation, long-press menu) offline, show Pending Sync, and reach the Sheet when the network returns, even with the app closed (CAP-3, CAP-5, AD-14).
3. Rows typed or edited in the Sheet appear in the app with Source and ID filled in, and the Sheet wins a conflict (CAP-6, CAP-7).
4. The Expenses tab groups by day with search, Category chips and the shared period filter; a new phone restores Expenses and Settings (CAP-4, CAP-17 filter part, CAP-2 restore part).
5. Currency and display name are set in Settings and sync through the Settings tab; all amounts use the money formatter; sign-out warns on pending changes, then erases local data (CAP-18, CAP-19, AD-20).
6. Its features ship in a release build uploaded to the Play closed test track.

## Boundaries

Local storage, sync and the Expenses tab. Not Category or Budget management screens (epic-budgets-and-home), not SMS (epic-sms-inbox).

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, CAP-3 to CAP-7, CAP-17, CAP-18, CAP-19
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-3 to AD-7, AD-9, AD-14, AD-18, AD-19, AD-20, Testing
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Expenses, State Patterns (sync pill), Interaction Primitives; mockup key-expenses.html

## Notes

- Decision: owns the money formatter and the sync status pill (offline, error, needs-consent) used by later epics (2026-10-24).
- Decision: CAP-17 is shared — this epic builds the period filter and uses it on Expenses; epic-budgets-and-home uses it on Home (2026-10-24).
- Decision: sign-out (CAP-19) lives here because it needs the outbox and sync work (2026-10-24).
- Waits on epic-account-and-sheet because: the sync engine needs the gateways, the created Sheet, the stored session and the fake gateway.
- Decision: incepted into 15 stories, kept in one epic with one owner (user's decision, 2026-11-13).
- Decision: entry 1 is the tracer bullet: an Expense added offline reaches the Sheet through a minimal form and list, which entries 7 and 8 replace with the designed versions (user's decision, 2026-11-13).
- Decision: entry 2 (pull and atomic refresh) follows the tracer as the least certain work; pull and push are each split in two for size (2–3 and 4–5) (user's decision, 2026-11-13).
- Decision: every Room schema change ships a migration from now on, starting with entry 1's single bump that adds all six mirror tables; no destructive fallback (user's decision, 2026-11-13).
- Decision: entries wait on epic 2's 2.3 and 2.4 (Sheet gateway), 2.6 (Settings tile) and 2.7 (release sign-in), not on the whole epic (2026-11-13).
- Decision: no closing end-to-end suite; the sync test matrix lives in entries 4 and 5 and entry 14 checks the journeys on the phone (user's decision, 2026-11-13).
- Decision: entry 9's shared selection holds the Category as well as the period, so epic-budgets-and-home's Home Category rows set it to open Expenses filtered (user's decision, 2026-11-13).
- Decision: entry 12 replaces Home's greeting tracer with the display name greeting (FR-23) (user's decision, 2026-11-13).
- Source conflict: EXPERIENCE.md, Component Patterns, Expense row — "Rows with `synced = false` show 'Waiting to sync'" vs AD-3 "there is no `synced` column"; Pending Sync is an outbox entry for the row, and the build follows AD-3.
