---
title: Expense Tracker PRD
status: final
created: 2026-10-08
updated: 2026-10-08
---

# PRD: Expense Tracker
## 0. Document Purpose
This PRD defines v1 of Expense Tracker, a personal Android app, for its owner and for the downstream BMad steps (UX, architecture, spec, tickets). Vocabulary is fixed by the Glossary (§3). Features are grouped in §4 with globally numbered requirements (FR-1 to FR-23). Where a requirement realizes a user journey (UJ-1 to UJ-4), it says so. Technical notes that aren't requirements live in `addendum.md` beside this file. The existing codebase is the starting point: this PRD describes the product it should become, not a new build from scratch.

## 1. Vision
Expense Tracker is a private expense tracker for people tired of finance apps that share their data with banks and show ads. It reads bank debit SMS on the phone, so logging a UPI or card payment takes a tap instead of typing, and the user always decides what gets recorded.

All data lives in the user's own Google Sheet, with no app server in between, so the user owns their records, can open and edit them anywhere, and gets everything back on a new phone. Monthly budgets per category show at a glance how much is left before the next purchase.

## 2. Target User
Built for its owner first, then a small group of people, each with their own private data, and ready for public release on Google Play.

### 2.1 Jobs To Be Done
- Know where my money went without typing in every expense.
- Keep my financial data in my own hands, out of banks', advertisers' and app makers' reach.
- See at a glance how much is left in each budget this month, before I spend.

### 2.2 Key User Journeys

- **UJ-1. Shylock catches today's UPI payments in one glance.**
  - **Persona and context:** Shylock pays by UPI throughout the day. He doesn't want to type each expense by hand, or hand his bank data to another app.
  - **Entry state:** the app is closed. Debit SMS from his Indian bank (HDFC, ICICI, SBI, Union Bank and similar) have arrived over the day.
  - **Path:**
    1. He opens the app and goes to the **Inbox** tab.
    2. The app reads today's SMS on the device and keeps only bank debit messages, including UPI and card debits.
    3. When two SMS show the same Transaction Reference, only one Inbox Item appears. When there is no Transaction Reference, or the references differ, both appear and he decides.
    4. Each Inbox Item shows the amount, the payee or merchant, the bank and masked account, and the time. He can expand it to see the original SMS.
    5. At the end of the list he taps **Load more**, which adds the previous day. Each tap goes back one more day.
    6. He taps **Add** on an Inbox Item. The expense form opens pre-filled with the amount, the date and time, and the payee as the description. He picks a category and saves.
  - **Climax:** the item leaves Inbox, a "₹450 added to Food" confirmation appears, the expense shows in his list, and the Food budget (and any alert) updates.
  - **Resolution:** SMS he has Added or Dismissed never reappear in Inbox.
  - **Edge case:** a duplicate SMS with no Transaction Reference appears twice, and Shylock Dismisses the extra one.

- **UJ-2. Shylock sets up on day one, and again on a new phone.**
  - **Persona and context:** Shylock wants his data in his own Google account, not on someone's server.
  - **Entry state:** freshly installed app, not signed in.
  - **Path:**
    1. He signs in with Google. Sign-in is required.
    2. On his first ever sign-in, the app creates a new Expense Sheet in his Google Drive.
    3. Months later he gets a new phone, installs the app and signs in. The app finds his existing Expense Sheet and loads his expenses from it. It creates a new one only if none exists.
  - **Climax:** on the new phone, his full history appears without any import step.
  - **Resolution:** the sheet is the record of truth, and the app shows what's in it.
  - **Edge case:** if he deleted the Expense Sheet from Drive, the next sign-in creates a fresh, empty one.

- **UJ-3. Shylock logs a chai on the train, and fixes a typo on his laptop.**
  - **Persona and context:** Shylock often has no signal on his commute, and sometimes reviews his spending in the sheet on his laptop.
  - **Path:**
    1. With no signal, he adds a ₹120 expense. The app saves it on the phone and marks it as waiting to sync.
    2. Once he's back online, the app pushes it to the sheet with Source = App.
    3. That evening, on his laptop, he corrects an amount in the sheet and adds a cash expense as a new row, leaving Source blank. The app fills it in as User.
    4. Next time he opens the app, it shows the corrected amount and the new cash expense.
  - **Climax:** the app and the sheet agree, whichever side he edited.
  - **Edge case:** the sheet wins. If a row was changed in the sheet while an offline edit to the same row was waiting, the sheet's version is kept.

- **UJ-4. Shylock sees how much Food money is left before ordering dinner.**
  - **Persona and context:** Shylock sets a ₹6,000 monthly Food budget and checks it before spending.
  - **Entry state:** it's the 24th, and he has spent ₹5,100 on Food this month.
  - **Path:**
    1. He opens the app. The home page lists each category with its budget and the amount left this month: Food shows ₹900 left of ₹6,000.
    2. At 80% or more of the budget, Food is flagged as a warning, and at 100% or more as exceeded, showing how much over.
    3. He decides to raise Food to ₹7,000 for this month, so he opens the Budgets tab and edits Food's Budget there.
  - **Climax:** he knows his remaining Food money at a glance, without doing any math.
  - **Resolution:** on the 1st of next month, spending resets to zero and every category starts with a copy of this month's budgets, so Food starts at ₹7,000.
  - **Edge case:** changing a budget affects this month and carries forward; past months keep the budget they had.

## 3. Glossary

- **User**: the one person who owns an app install and its Expense Sheet. Nothing is shared between Users.
- **Expense Sheet**: the Google Sheets spreadsheet in the User's own Google Drive that holds all their data. It is the record of truth. One per User.
- **Expense**: one spending record with an amount, a Category, a description, a date and time, and a Source. Stored as one row in the Expense Sheet.
- **Source**: a field of each Expense: **App** when the app created it, **User** when the User typed it directly into the Expense Sheet.
- **Category**: a label that groups Expenses (for example Food), with a color and an icon.
- **Budget**: the spending limit for one Category in one Budget Month. A Category has at most one Budget per Budget Month.
- **Budget Month**: a calendar month, from the 1st to the last day. Spending against a Budget counts only Expenses dated in that month.
- **Amount Left**: a Category's Budget minus its spending in the current Budget Month.
- **Pending Sync**: the state of an Expense saved on the phone that hasn't reached the Expense Sheet yet.
- **Inbox**: the tab listing Inbox Items for the User to review.
- **Inbox Item**: a bank debit SMS that the app detected and hasn't yet been Added or Dismissed.
- **Transaction Reference**: the reference or transaction number in a bank SMS, such as a UPI ref, when one is present.
- **Add**: turning an Inbox Item into an Expense through the pre-filled expense form.
- **Dismiss**: permanently discarding an Inbox Item without creating an Expense.
- **Currency**: the single currency the User's amounts are shown in. INR by default.

## 4. Features

### 4.1 Sign-in and Expense Sheet

**Description:** Every User signs in with Google before using the app. The app keeps each User's data in their own Expense Sheet and runs no server of its own. Realizes UJ-2.

#### FR-1: Required Google sign-in
A User must sign in with a Google account before reaching any other screen. Realizes UJ-2.
- Without a signed-in account, only the sign-in screen is reachable.
- Signing out returns to the sign-in screen and removes the account's tokens from the device.

#### FR-2: Create or reuse the Expense Sheet
On sign-in, the app reuses the User's existing Expense Sheet, or creates one if none exists. Realizes UJ-2.
- First ever sign-in: a new Expense Sheet appears in the User's Drive, containing the Expense and Budget columns and the Source column.
- Sign-in on a new phone or after a reinstall: the existing Expense Sheet is found and its Expenses and Budgets are shown, and no second sheet is created.
- If the User deleted the Expense Sheet, the next sign-in creates a fresh, empty one.

### 4.2 Expenses

**Description:** The User records and reviews spending. Expenses come from manual entry, from Inbox, or straight from the Expense Sheet.

#### FR-3: Add an Expense manually
The User can add an Expense with an amount, a Category, a description and a date. Realizes UJ-3.
- The amount must be greater than zero and a Category is required.
- A saved Expense has Source = App.

#### FR-4: Browse, search and filter Expenses
The User can see Expenses newest first, search by description or Category, and filter by Category.

#### FR-5: Delete an Expense
The User can delete an Expense, and its row is removed from the Expense Sheet.
- The app asks for confirmation before deleting.

#### FR-6: Edit an Expense
The User can edit an Expense's amount, Category, description and date in the app, and the change is written to the Expense Sheet. Realizes UJ-3.

### 4.3 Expense Sheet sync and offline

**Description:** The Expense Sheet is the record of truth. The app works offline and catches up when it's back online. Realizes UJ-2, UJ-3.

#### FR-7: Offline saves
An Expense added without a connection is saved on the phone as Pending Sync and pushed to the Expense Sheet once the phone is back online. Realizes UJ-3.
- Pending Sync Expenses are visible in the list immediately, and count toward Budgets immediately.
- A Pending Sync Expense carries a small "waiting to sync" marker.

#### FR-8: Expense Sheet edits show up in the app
Rows added, changed or deleted directly in the Expense Sheet appear in the app the next time it syncs. Realizes UJ-3.
- A row the User added with a blank Source is filled in with Source = User.
- The app syncs when it opens, when it returns to the foreground, and when the User pulls down to refresh.

#### FR-9: The Expense Sheet wins conflicts
When an Expense is changed in the Expense Sheet while an in-app edit to it is still Pending Sync, the Expense Sheet's version is kept. Realizes UJ-3.

### 4.4 SMS Inbox

**Description:** The app reads bank debit SMS on the phone and offers each one for the User to Add as an Expense. Nothing is added automatically, and nothing runs in the background. Realizes UJ-1.

#### FR-10: SMS permission
The app asks for SMS read permission the first time the User opens Inbox, explaining what is read and that SMS content never leaves the phone, except as Expenses the User Adds. Realizes UJ-1.
- If the User refuses, Inbox explains how to grant the permission, and the rest of the app works normally.

#### FR-11: Detect debit SMS
When Inbox opens, the app reads the current day's SMS and keeps only debit transaction messages from Indian banks (HDFC, ICICI, SBI, Union Bank and similar), including UPI payments and card purchases. Realizes UJ-1.
- Credit messages, OTPs and promotional SMS never become Inbox Items.

#### FR-12: Ignore duplicate SMS
When two SMS share the same Transaction Reference, only one Inbox Item appears. SMS with no Transaction Reference, or with different references, each appear. Realizes UJ-1.

#### FR-13: Inbox Item contents
Each Inbox Item shows the amount, the payee or merchant, the bank and masked account number, and the time. Tapping it reveals the original SMS text. Realizes UJ-1.

#### FR-14: Load more days
At the end of the list, a **Load more** button reads the previous day's SMS and adds its Inbox Items. Each tap goes back one more day. Realizes UJ-1.

#### FR-15: Add from Inbox
Tapping **Add** opens the expense form pre-filled with the SMS amount, date and time, and payee as the description, with the Category left empty. On save, the Expense is created, the Inbox Item disappears, and a confirmation such as "₹450 added to Food" appears. Realizes UJ-1.

#### FR-16: Dismiss is permanent
A Dismissed Inbox Item, or one already Added, never reappears, even when the same day is read again. Realizes UJ-1.
- Added and Dismissed SMS are remembered in the Expense Sheet, so a new phone doesn't offer them again.

**Feature-specific NFR:** see NFR-1 (raw SMS text never leaves the phone).

### 4.5 Categories and Budgets

**Description:** The User organizes spending into Categories, each with a monthly Budget, and sees at a glance what's left. Realizes UJ-4.

#### FR-17: Manage Categories
The User can create and delete Categories, each with a name, color and icon. Default Categories exist on first use.
- Category names are unique.
- Deleting a Category with Expenses requires choosing where its Expenses go.

#### FR-18: Monthly Budgets
Each Category can have a Budget per Budget Month. Spending against it counts only that month's Expenses. Realizes UJ-4.
- On the 1st of a month, each Category's Budget for the new month is a copy of the previous month's Budget.
- Budgets are stored in the Expense Sheet.

#### FR-19: Edit a Budget anytime
The User can change a Category's Budget at any time. The change applies to the current Budget Month and is copied forward into later months. Past months keep their own Budget. Realizes UJ-4.

#### FR-20: Amount Left on the home page
The home page lists each Category with its Budget and Amount Left for the current Budget Month. Realizes UJ-4.
- At 80% or more of the Budget the Category shows a warning, and at 100% or more it shows as exceeded, with the amount over.

### 4.6 Spending analytics

#### FR-21: Category breakdown
The home page shows how spending splits across Categories, with amounts and percentages, for a selected period: the current Budget Month (default), a previous month, or a custom date range. Realizes UJ-4.
- For a month period, each Category also shows its Budget, Amount Left and alerts (FR-20).
- For a custom date range, each Category shows spending only, with no Budget, Amount Left or alerts.
- The Expenses list (FR-4) offers the same period filter.

### 4.7 Profile and settings

#### FR-22: Currency
The User can choose the Currency in profile settings. INR is the default. All amounts display in the chosen Currency.
- Changing the Currency changes only the symbol and formatting; there's no conversion of existing amounts.

#### FR-23: Display name
The User can edit the name shown in the app.

## 5. Non-Goals (Explicit)

- **No bank linking.** The app never connects to bank accounts or shares data with banks or aggregators. Bank data enters only through SMS the User chooses to Add.
- **No ads, ever.** The app is free.
- **No app server.** Data lives only on the phone and in the User's own Expense Sheet.
- **No sharing.** Every account is personal. There are no shared or family budgets.
- **Not automatic.** No Expense is ever created from an SMS without the User tapping Add.

## 6. MVP Scope

### 6.1 In Scope (v1)
- All of §4: sign-in and Expense Sheet, Expenses, sheet sync and offline, SMS Inbox, Categories and Budgets, spending analytics, profile and settings (FR-1 to FR-23).
- SMS detection for Indian banks only (HDFC, ICICI, SBI, Union Bank and similar), debits only, including UPI and card.
- Android only.

### 6.2 Out of Scope for MVP
- **Banks outside India**: v2.
- **Analyze my finances**: v2. For a chosen date range, the app reviews every Expense, shows where the User spent most and where to cut back. A small LLM combines the User's Budgets with a short questionnaire: spending priorities, income, plans for next year and the next 5 years, marital status and similar. It must stay consistent with the privacy-first positioning; see Open Question 1.
- **Tracking credited amounts**: later, as a user setting.
- **Payee-to-Category suggestions**: later.
- **Not yet (no version planned)**: income tracking, savings goals, investments, bill reminders, receipt scanning, iPhone and web versions.

## 7. Success Metrics

**Primary**
- **SM-1: One full month, one app.** The owner records every expense for a full calendar month using only this app, with no other expense app or manual notes. Validates FR-3, FR-7, FR-11, FR-15, FR-20.

**Counter-metrics (do not optimize)**
- **SM-C1: Installs and growth.** The app is free, has no ads and targets a small group, so install counts are not a goal. Chasing them would pull toward features that conflict with privacy-first. Counterbalances SM-1.

## 8. Cross-Cutting Non-Functional Requirements

- **NFR-1 Privacy:** raw SMS text never leaves the phone. Data goes only to the User's own Expense Sheet, and the app sends nothing to any server of its own. No third-party analytics, crash-reporting or advertising SDKs that send personal data.
- **NFR-2 Minimum Google access:** the app requests only the Google access it needs to work with its own Expense Sheet, which also keeps Google's OAuth verification as light as possible for public release.
- **NFR-3 Play Store compliance:** v1 must pass Google Play review, including the SMS permission declaration under the "SMS-based money management" exception (with a demo video, and SMS tracking prominent in the store listing), the Data safety form, and the Financial features declaration.
- **NFR-4 Offline-first:** every core action except sign-in (adding, editing, deleting, browsing, Budgets, Inbox) works without a connection and syncs later.
- **NFR-5 Credential safety:** no Google token, refresh token or client secret is stored on the device. Short-lived access tokens are held in memory only, and sign-out erases all local data.
- **NFR-6 Responsiveness:** opening Inbox or tapping Load more shows that day's Inbox Items within about 2 seconds on a mid-range phone.

## 9. Open Questions
1. **v2 LLM placement:** does "Analyze my finances" run on the phone or in the cloud, and what data may leave the device? (Owner: product. Revisit at v2 planning.)
2. **Unrecognized bank SMS:** when a real debit SMS isn't detected (a new bank or format), how does the User report it or add it? (Revisit after a month of real use.)
3. **Two phones at once:** can one User run the app on two phones signed in to the same Expense Sheet, and is anything special needed? (Architecture.)
4. **Google OAuth verification:** the public release needs Google's app verification for Sheets and Drive access. Start it early, since it can take weeks. (Owner: product. Before public release.)
5. **Product name:** "Expense Tracker" is a working title. (Owner: product. Before public release.)

## 10. Assumptions Index
None open. Every assumption made while drafting was confirmed by the owner on 2026-10-08.
