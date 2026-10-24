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
