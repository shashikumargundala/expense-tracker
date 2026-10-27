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
5. Its features ship in a signed release APK built with story 1.9's process; the Play closed-track upload follows story 1.7 once the Play account is verified.

## Boundaries

Sign-in, session and the Expense Sheet's existence and layout. Not reading or writing rows (epic-expenses-and-sync), not sign-out (epic-expenses-and-sync).

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, CAP-1, CAP-2
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-7, AD-10, AD-11, AD-20, Operations (OAuth clients)
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Sign-in and Settings; mockups key-signin.html, key-settings.html

## Notes

- Decision: CAP-2 is shared — this epic finds, creates and recreates the Sheet; epic-expenses-and-sync restores Expenses and Settings on a new phone; epic-budgets-and-home restores Budgets (2026-10-24).
- Handoff: the debug OAuth client uses the developer's default debug key SHA-1; the release client uses the Play App Signing SHA-1 recorded by epic-platform-baseline entry 7 (2026-10-24).
- Decision: the release OAuth client uses story 1.9's release key SHA-1 `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`, which Play App Signing will reuse in 1.7 (user's decision, 2026-10-27; replaces the Play App Signing SHA-1 handoff above).
- Decision: owns the fake ExpenseSheetGateway test fixture (2026-10-24).
- Source conflict: AD-9 says default Categories are written 'only when the sync engine creates a new Expense Sheet', AD-11 puts creation in the gateway — this epic writes them at Sheet creation; offer the wording fix to bmad-architecture.
- Waits on epic-platform-baseline because: it needs the module scaffold, Hilt, the shell and the final app ID the OAuth clients bind to.
- Decision: Done when 5 ships a signed release APK (story 1.9); the Play upload waits for story 1.7 (user's decision, 2026-10-27).
- Decision: default Categories are Food, Transport, Shopping, Bills and Fun, with DESIGN.md's category colours and the Material Symbols icons restaurant, directions_car, shopping_bag, receipt_long and celebration (2026-10-27).
- Decision: one Google Cloud project with a Testing-mode consent screen and test users, the Drive and Sheets APIs enabled, one Web OAuth client (the server client ID Credential Manager needs) and Android debug and release clients; client IDs aren't secrets (2026-10-27).
- Decision: find-or-create runs as part of sign-in, behind "Setting up your Expense Sheet…" (2026-10-27).
- Source conflict: AD-4 — "only the `sync` feature in `:data` calls `ExpenseSheetGateway`" vs this epic running find-or-create at sign-in (EXPERIENCE.md's "Setting up your Expense Sheet…"); offer the wording fix to bmad-architecture together with the AD-9 one.
- Decision: a sign-in network or Sheet-creation failure shows the inline line "Couldn't sign in. Check your connection and try again."; cancel stays silent (2026-10-27).
- Decision: the session (account ID, email, Sheet ID) is stored in a Room `session` table (2026-10-27).
- Decision: no account switching in v1; a different account signs in only after sign-out, which epic-expenses-and-sync owns (2026-10-27).
- Decision: the tracer bullet is entry 1, Google sign-in to Home on the owner's phone. Entry 2, Drive access with the consent and 401 handling, is the least certain and follows it (2026-10-27).
- Decision: the data lane (2, then 3, then 4) runs first. Entries 4, 5 and 6 fan out after 3, because 3 changes the sign-in flow that 5 dresses and fills the Sheet ID that 4 and 6 read. Entry 7 checks the whole epic on the release APK, so it waits on 4, 5 and 6 (2026-10-27).
- Decision: entry 1's gate reads the stored session at launch, so relaunching opens Home. Entry 4 adds missing-Sheet recovery and confirms offline cold start (2026-10-27).
- Note: if story 1.7 lets Play App Signing generate its own key instead of reusing story 1.9's, 1.7 must register a second release OAuth client with the Play SHA-1. Offer the Operations wording fix to bmad-architecture with the AD-4 and AD-9 ones.
