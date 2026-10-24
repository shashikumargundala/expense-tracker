---
type: initiative
title: "Expense Tracker v1: private, SMS-assisted expense tracking in your own Google Sheet"
parent: none
covers: [CAP-1, CAP-2, CAP-3, CAP-4, CAP-5, CAP-6, CAP-7, CAP-8, CAP-9, CAP-10, CAP-11, CAP-12, CAP-13, CAP-14, CAP-15, CAP-16, CAP-17, CAP-18, CAP-19]
after: []
assignee: ""
risk: high
---

# Expense Tracker v1

## Description

Rebuild the AI Studio–generated app into the v1 the spec describes: Google sign-in, every record in the user's own Expense Sheet with offline sync, monthly Budgets with Amount Left on Home, and an Inbox that turns Indian bank debit SMS into one-tap Expenses. The spec owns the capabilities, constraints and non-goals; the architecture spine and the UX spines bind how it is built.

## Outcome

The owner, then a small group, tracks every expense privately in their own Google Sheet; the spec's success signal (a full month logged with only this app, app and Sheet agreeing) shows it worked.

## Done when

1. The owner logs every expense for a full calendar month using only this app, and the app and the Expense Sheet agree at month end.
2. CAP-1 to CAP-19 are live in a release build on the Play closed test track.
3. The release passes Play review, including the SMS-based money management declaration, Data safety and Financial features forms.
4. On the release build, no raw SMS text leaves the phone and no third-party SDK sends personal data.

## Boundaries

Capability groups from the spec, one owner (the developer with agent lanes). The Android app only: no server, no bank linking, nothing from the spec's Non-goals or v2.

- Touch point: Google Cloud console — OAuth clients for debug and release in one project; owner: epic-account-and-sheet
- Touch point: Google Drive and Sheets — the user's Expense Sheet over REST; owner: epic-account-and-sheet (find, create), epic-expenses-and-sync (read, write)
- Touch point: Play Console — closed track and app signing, owner: epic-platform-baseline; Data safety, Financial features and SMS permission declarations, owner: epic-sms-inbox
- Touch point: GitHub Actions — CI; owner: epic-platform-baseline

Tracer path: sign in, the Expense Sheet is created, an Expense added offline reaches the Sheet, and Home shows that Category's Amount Left (epics 1 to 4).

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, sections Capabilities, Constraints, Non-goals
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-1 to AD-20 (cross-epic decisions: AD-3 outbox types, AD-4 and AD-18 sync, AD-5 identity, AD-6 money, AD-7 Sheet layout, AD-11 gateways, AD-13 fingerprints, AD-19 dates)
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md and _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md
- brownfield — _bmad-output/initiative-expense-tracker/spec-expense-tracker/brownfield.md

## Notes

- Decision: three closed-track releases instead of sprints — R1 = epics 1 to 3 (usable tracker), R2 = epic 4 (Budgets and Home), R3 = epic 5 (SMS Inbox) (user's decision, 2026-10-24).
- Decision: the tracer path above is the first milestone; the SMS Inbox comes last (user's decision, 2026-10-24).
- Decision: epic-sms-inbox owns the final Data safety, Financial features and SMS permission declarations and passing Play review, since data practices are complete only then (user's decision, 2026-10-24).
- Decision: shared setup owners — screenshot test harness: epic-platform-baseline; fake ExpenseSheetGateway: epic-account-and-sheet; money formatter and sync status pill: epic-expenses-and-sync (2026-10-24).
- Unknown: real bank debit SMS samples (HDFC, ICICI, SBI, Union Bank) are not available yet; epic-sms-inbox opens with collecting them.
