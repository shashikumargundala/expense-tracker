---
type: epic
title: "Shylock signs in and his Expense Sheet is ready"
parent: initiative-expense-tracker
covers: [CAP-1, CAP-2]
after: []
assignee: ""
risk: high
---

# Shylock signs in and his Expense Sheet is ready

## Description

Real Google sign-in replaces the simulated one, and the app finds or creates the user's Expense Sheet with every tab the spine defines, so later epics have a session and a Sheet to sync with.

## Outcome

Shylock signs in once and his Expense Sheet exists in his Drive, ready for his data; CAP-1 and CAP-2's success checks show it.

## Done when

1. Without a signed-in account only the Sign-in screen is reachable; Continue with Google uses Credential Manager and the Google-branded button, and a cancelled sign-in returns silently to signed-out (CAP-1, AD-10).
2. Access uses AuthorizationClient with `drive.file` only; tokens stay in memory, with clearToken and one retry on 401 (AD-10).
3. First sign-in creates one Expense Sheet tagged `expenseTracker=v1` with the Expenses, Categories, Budgets, Dismissed and Settings tabs and the default Categories; a later sign-in reuses it; a deleted Sheet is recreated (CAP-2 find and create part, AD-7, AD-11).
4. Signed-in account, email and Sheet ID are stored without secrets, so the app cold-starts offline (AD-20); Settings shows the Google account section.
5. Its features ship in a release build uploaded to the Play closed test track.

## Boundaries

Sign-in, session and the Expense Sheet's existence and layout. Not reading or writing rows (epic-expenses-and-sync), not sign-out (epic-expenses-and-sync).

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, CAP-1, CAP-2
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-7, AD-10, AD-11, AD-20, Operations (OAuth clients)
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Sign-in and Settings; mockups key-signin.html, key-settings.html

## Notes

- Decision: CAP-2 is shared — this epic finds, creates and recreates the Sheet; epic-expenses-and-sync restores Expenses and Settings on a new phone; epic-budgets-and-home restores Budgets (2026-10-24).
- Handoff: the debug OAuth client uses the developer's default debug key SHA-1; the release client uses the Play App Signing SHA-1 recorded by epic-platform-baseline entry 7 (2026-10-24).
- Decision: owns the fake ExpenseSheetGateway test fixture (2026-10-24).
- Source conflict: AD-9 says default Categories are written 'only when the sync engine creates a new Expense Sheet', AD-11 puts creation in the gateway — this epic writes them at Sheet creation; offer the wording fix to bmad-architecture.
- Waits on epic-platform-baseline because: it needs the module scaffold, Hilt, the shell and the final app ID the OAuth clients bind to.
