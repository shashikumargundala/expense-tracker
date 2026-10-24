---
type: epic
title: "Shylock sees what's left before he spends"
parent: initiative-expense-tracker
covers: [CAP-2, CAP-14, CAP-15, CAP-16, CAP-17]
after: []
assignee: ""
risk: medium
---

# Shylock sees what's left before he spends

## Description

Categories and monthly Budgets are managed in the Budgets tab, and Home shows each Category's spending and Amount Left for a chosen period.

## Outcome

Shylock knows what is left in every Category before he spends; CAP-15 and CAP-16's success checks (₹900 left of ₹6,000 with a warning) show it.

## Done when

1. The Budgets tab lists Categories with their monthly Budget; Add category, edit (budget, name, color, icon) and delete with reassigning its Expenses work, and names are unique (CAP-14, AD-9).
2. Budgets are written only on change and resolve to the latest month on or before the one shown; spending resets on the 1st; Budgets restore on a new phone (CAP-15, CAP-2 Budgets part, AD-8).
3. Home shows the ring chart and Category list with Amount Left, warning at 80% and exceeded at 100%, with the period filter; a custom range shows spending only (CAP-16, CAP-17).
4. Tapping a Category on Home opens the Expenses tab filtered to it and the period; amounts and percentages sum correctly.
5. Its features ship in a release build uploaded to the Play closed test track.

## Boundaries

Categories, Budgets and Home. Not the sync engine (epic-expenses-and-sync), not Inbox.

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, CAP-14 to CAP-17
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-6, AD-8, AD-9, AD-19
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Home, Budgets; mockups key-home.html, key-budgets.html

## Notes

- Waits on epic-expenses-and-sync because: it needs the sync engine's Category and Budget types, the period filter, the Expenses filter entry point and the money formatter.
