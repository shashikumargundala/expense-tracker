---
type: epic
title: "Bank debit SMS become one-tap Expenses"
parent: initiative-expense-tracker
covers: [CAP-8, CAP-9, CAP-10, CAP-11, CAP-12, CAP-13]
after: []
assignee: ""
risk: high
---

# Bank debit SMS become one-tap Expenses

## Description

The Inbox tab reads the day's bank debit SMS on the phone and offers each as a one-tap Expense, never uploading SMS text, then the app passes Play review.

## Outcome

Shylock records his UPI and card debits without typing; CAP-9 to CAP-13's success checks and an approved Play review show it.

## Done when

1. The SMS permission is asked on first Inbox open with its explanation; refusing leaves the rest of the app working (CAP-8).
2. Per-bank parsers (HDFC, ICICI, SBI, Union Bank) and the generic fallback pass tests on masked real samples, including credit, OTP and promotional negatives; Inbox shows today and Load more adds one day, within about 2 seconds per day (CAP-9, AD-12, NFR-6).
3. Duplicates by Transaction Reference show once; each Inbox Item shows its fields and expands to the SMS; Add opens the pre-filled form and updates Home's Amount Left (CAP-10 to CAP-12).
4. Added, Dismissed (swipe, with Undo) and deleted-Expense SMS never reappear, on this or a new phone; no raw SMS text reaches Room, the Sheet, logs or saved state (CAP-13, AD-13).
5. The SMS permission declaration with demo video, Data safety and Financial features forms are submitted and the release passes Play review.

## Boundaries

SMS reading, parsing, the Inbox tab and Play review. Not credited amounts, other countries or category suggestions (spec Non-goals).

## References

- spec — _bmad-output/initiative-expense-tracker/spec-expense-tracker/spec-expense-tracker.md, CAP-8 to CAP-13, Constraints (Play review)
- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-12, AD-13, AD-19
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Inbox, State Patterns (permission denied); mockup key-inbox.html

## Notes

- Decision: declares `READ_SMS` in the manifest here, with the Play SMS declaration (user's decision, 2026-10-24).
- Decision: owns the final Play review submissions (2026-10-24).
- Unknown: no real bank debit SMS samples yet; the epic opens with a hitl story to collect and mask them before any parser story.
- Waits on epic-expenses-and-sync because: it needs the expense form sheet, AddDismissal and SMS ID handling in sync.
- Waits on epic-budgets-and-home because: CAP-12's check needs Home's Amount Left.
