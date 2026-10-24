---
id: SPEC-expense-tracker
companions:
  - ../prd-expense-tracker/prd-expense-tracker.md
  - ../architecture-expense-tracker/architecture-expense-tracker.md
  - brownfield.md
  - ../ux-expense-tracker/DESIGN.md
  - ../ux-expense-tracker/EXPERIENCE.md
sources:
  - ../prd-expense-tracker/addendum.md
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability. Consult them only if you need narrative rationale or prose color this contract intentionally omits.

# Expense Tracker v1

## Why

This spec realizes a vision and solves a pain. The owner, and later a small group and public Play users, want a private Android expense tracker. Mainstream finance apps link to banks, share data and show ads. This app keeps every record in the user's own Google Sheet with no app server. It turns Indian bank debit SMS into one-tap expenses the user approves, and shows how much of each monthly budget is left. The existing AI Studio–generated app is the starting point. It has an all-time "monthly" budget, simulated sign-in and a hard-coded `$`, so v1 restructures it (`brownfield.md`). Glossary terms follow the PRD (§3) verbatim.

## Capabilities

- **CAP-1**
  - **intent:** A User must sign in with a Google account before using the app.
  - **success:** Without a signed-in account only the sign-in screen is reachable; after sign-in the home page opens.
- **CAP-2**
  - **intent:** On sign-in, the app reuses the User's Expense Sheet, or creates one when none exists or it was deleted.
  - **success:**
    - On a first sign-in, one new Expense Sheet with all tabs appears in the User's Drive.
    - On a new phone, the existing Sheet's Expenses, Budgets and Settings load with no second Sheet created.
    - After the Sheet is deleted, the next sign-in creates a fresh, empty one.
- **CAP-3**
  - **intent:** The User can add, edit and delete Expenses (amount, Category, description, date) in the app.
  - **success:**
    - The amount must be greater than zero and a Category is required.
    - Saved Expenses have Source = App.
    - Delete asks for confirmation.
    - Each change reaches the Expense Sheet on the next sync.
- **CAP-4**
  - **intent:** The User can browse Expenses newest first, search by description or Category, and filter by Category.
  - **success:** A search or filter shows exactly the matching Expenses.
- **CAP-5**
  - **intent:** Expenses saved without a connection appear immediately, count toward Budgets, and sync when the phone is back online.
  - **success:**
    - An Expense added in airplane mode shows a "waiting to sync" marker.
    - It reaches the Sheet after the connection returns, even if the app was closed.
- **CAP-6**
  - **intent:** Rows added, changed or deleted directly in the Expense Sheet appear in the app on the next sync.
  - **success:**
    - A hand-typed row with blank Source and ID gets Source = User and an ID written back.
    - Edits and deletes made in the Sheet show in the app after pull-to-refresh.
- **CAP-7**
  - **intent:** When an Expense changed in the Sheet while an in-app edit to it was Pending Sync, the Sheet's version is kept.
  - **success:** In that scenario, after sync both the Sheet and the app show the Sheet's values.
- **CAP-8**
  - **intent:** The app asks for SMS read permission the first time Inbox opens, explaining that SMS content never leaves the phone.
  - **success:** If the User refuses, Inbox explains how to grant it and every other feature still works.
- **CAP-9**
  - **intent:** Inbox lists the current day's debit SMS from Indian banks (HDFC, ICICI, SBI, Union Bank and similar), including UPI and card debits. **Load more** adds one earlier day per tap.
  - **success:**
    - Each bank's sample debit SMS yields an Inbox Item.
    - Credit, OTP and promotional samples yield none.
    - Each Load more tap adds exactly the previous day.
- **CAP-10**
  - **intent:** SMS sharing a Transaction Reference produce one Inbox Item. SMS without one, or with different references, each appear.
  - **success:** A duplicated SMS with the same reference shows once. Two SMS without references both show.
- **CAP-11**
  - **intent:** Each Inbox Item shows amount, payee or merchant, bank with masked account, and time, and expands to the original SMS.
  - **success:** All listed fields display for every bank's sample SMS.
- **CAP-12**
  - **intent:** Add on an Inbox Item opens the expense form pre-filled with amount, date and time, and payee as the description. The User picks the Category and saves.
  - **success:** After saving, the item leaves Inbox, a confirmation such as "₹450 added to Food" appears, the Expense lists, and the Category's Amount Left updates.
- **CAP-13**
  - **intent:** An SMS that was Added or Dismissed never appears in Inbox again, including after its Expense is deleted and on a new phone.
  - **success:** Re-reading the same day on the same or a new phone shows none of those SMS.
- **CAP-14**
  - **intent:** The User can create and delete Categories (unique name, color, icon). Default Categories exist when a new Expense Sheet is created.
  - **success:**
    - Duplicate names are rejected.
    - Deleting a Category with Expenses requires choosing where they move, and none are left pointing at the deleted Category.
- **CAP-15**
  - **intent:** Each Category can have a monthly Budget. Spending counts only the current Budget Month, the new month starts from the previous month's Budgets, and the User can change a Budget at any time from the current month onward.
  - **success:**
    - Spending resets on the 1st.
    - A Budget changed in October applies to October and later months while September keeps its value.
    - Budgets survive a reinstall.
- **CAP-16**
  - **intent:** The home page lists each Category with its Budget and Amount Left for the current month, flagging a warning at 80% or more and exceeded at 100% or more.
  - **success:** With a ₹6,000 Budget, the home page shows ₹900 left after ₹5,100 spent with a warning, and exceeded with the overage after ₹6,100 spent.
- **CAP-17**
  - **intent:** The User can see spending split across Categories, with amounts and percentages, for the current month (default), a previous month, or a custom date range. The Expenses list uses the same period filter.
  - **success:**
    - The amounts sum to the period's total and the percentages sum to 100.
    - A custom range shows spending only, with no Budget, Amount Left or alerts.
- **CAP-18**
  - **intent:** The User can set the Currency (INR default) and display name in Settings, and both follow the User to a new phone.
  - **success:**
    - Changing the Currency changes only the symbol and format of every amount.
    - Settings are restored after a reinstall.
- **CAP-19**
  - **intent:** The User can sign out. If changes haven't synced, the app warns first, then erases all local data.
  - **success:**
    - With an Expense still Pending Sync, sign-out shows a warning.
    - After sign-out, no Expense data remains on the device.

## Constraints

- No app server. The Expense Sheet in the User's own Drive is the record of truth, and nothing is shared between Users.
- Raw SMS text never leaves the phone's memory, and no third-party analytics, crash or ad SDKs send personal data.
- Google access is the `drive.file` scope only. v1 must pass Play review, including the SMS-based money management permission declaration, the Data safety form and the Financial features declaration.
- Every core action except sign-in works offline.
- v1 SMS detection covers Indian banks and debits only.
- Inbox shows a day's items within about 2 seconds on a mid-range phone.
- The architecture spine (AD-1 to AD-20) binds every implementation choice: Clean Architecture in three Gradle modules, Hilt, MVVM, the outbox sync contract, and a 500-line file limit.
- Android only, minSdk 24, target SDK 36. The app ID is `com.shashi.expensetracker` and is permanent after the first Play release.

## Non-goals

- Bank linking, or sharing data with banks or aggregators.
- Ads, or any paid tier. The app is free.
- Creating an Expense from an SMS without the User tapping Add.
- Shared or family budgets.
- v1: banks outside India, tracking credited amounts, payee-to-Category suggestions, "Analyze my finances" (v2), income tracking, savings goals, investments, bill reminders, receipt scanning, iPhone or web versions.
- Importing Google Sheets the app didn't create.

## Success signal

- The owner records every expense for a full calendar month using only this app, with no other expense app or notes, and the app and the Expense Sheet agree at month end.

## Assumptions

- Deleting an Expense that came from an SMS treats that SMS as Dismissed. The spine decides this, and it's consistent with the PRD's "once removed, don't add it back".
- Currency and display name are stored in the Expense Sheet's Settings tab. The spine decides this; the PRD left storage open.

## Open Questions

- Deferred from the PRD: where the v2 LLM runs (OQ1), handling undetected debit SMS (OQ2), two phones on one Sheet (OQ3), the lead time for Google OAuth verification (OQ4), and the final product name (OQ5).
