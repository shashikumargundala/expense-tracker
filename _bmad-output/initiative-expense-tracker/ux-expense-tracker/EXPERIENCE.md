---
name: Expense Tracker
status: final
created: 2026-10-08
updated: 2026-10-08
sources:
  - .memlog.md
  - ../prd-expense-tracker/prd-expense-tracker.md
  - ../architecture-expense-tracker/architecture-expense-tracker.md
  - mockups/key-home.html
  - mockups/key-expenses.html
  - mockups/key-inbox.html
  - mockups/key-budgets.html
  - mockups/key-signin.html
  - mockups/key-settings.html
---

# Expense Tracker: Experience Spine

## Foundation

Expense Tracker is an Android phone app built with Material 3 in Jetpack Compose. It adapts from narrow 320dp phones up to large phones, foldables and tablets, and supports dark mode and font scaling. `DESIGN.md` is the visual reference (tokens, components, shapes); this file defines structure and behavior. It's a consumer app for public Play release, so the bar is polish, accessibility and trust.

Glossary terms come from the PRD and are used verbatim: Expense, Category, Budget, Budget Month, Amount Left, Pending Sync, Inbox, Inbox Item, Add, Dismiss, Expense Sheet, Source, Currency.

Rendered references: `mockups/key-home.html`, `mockups/key-expenses.html`, `mockups/key-inbox.html`, `mockups/key-budgets.html`, `mockups/key-signin.html`, `mockups/key-settings.html`, plus the directions `mockups/direction-orbit-rings.html` and `mockups/direction-jelly-pop.html`. **The spines (this file and DESIGN.md) win over the mocks wherever they disagree.**

## Information Architecture

Sign-in is required before anything else (FR-1). After sign-in, the app has five tabs in a floating pill nav (`{components.pill-nav}`), in this order:

| Surface | Reached from | What it delivers |
|---|---|---|
| Sign-in (`mockups/key-signin.html`) | Cold start when signed out; after Sign out | The brand and one "Continue with Google" button (Google's official Sign in with Google button), then the Signing in state while the Expense Sheet is found or created (FR-1, FR-2; see State Patterns). The only reachable screen while signed out. |
| **Home** (hero) | Default tab after sign-in | Spending analytics for the selected period: a hero chart of all Categories on top, then the Category list with each Category's ring and spend, plus Amount Left and Budget for month periods. Also the Inbox hint, the sync pill and the FAB. |
| **Expenses** | Nav; tapping a Category on Home | Every Expense grouped by day, with search, the same period filter as Home, and Category filter chips. Tap a row to edit; swipe to delete. FAB. |
| **Inbox** | Nav (badge shows the count); the Home Inbox hint | Inbox Items from bank debit SMS, each with Add, an expandable original SMS, swipe to Dismiss, and Load more. No FAB. |
| **Budgets** | Nav | Every Category with its monthly Budget and this month's ring. Tap one to edit it; "Add category" creates one. The only place to edit Budgets and Categories. No FAB. |
| **Settings** (`mockups/key-settings.html`) | Nav | Currency, display name, Google account and Expense Sheet, Open my Sheet, Sign out. No FAB. See Settings contents. |
| Expense form (sheet) | FAB (Home, Expenses); tapping an Expense row; Inbox **Add** | One form for adding and editing an Expense: Amount, Date, Time, Description, Category. From Inbox Add, it opens pre-filled from the SMS. |
| Category editor (sheet) | Tapping a Category in Budgets; "Add category" | Monthly budget, name, color, icon, Save, Delete category. |

- **FAB**: shown only on Home and Expenses. On Inbox, adding goes through each Inbox Item's Add button, and on Budgets through the "Add category" tile.
- **Inbox Add** opens the same expense form as the FAB. The expense form is a bottom sheet everywhere: from the FAB, from a row edit, and from Inbox Add.
- **Overlays** go one level deep: a sheet or a dialog, never a sheet on a sheet. The exception is a confirmation dialog raised from a sheet (Delete category).

## Settings contents

Settings has no PRD journey. It exists for FR-22, FR-23, the account and Expense Sheet visibility from UJ-2, and sign-out from FR-1. Layout and copy are as rendered in `mockups/key-settings.html`: two sections, then Sign out.

**Preferences**

1. **Currency**: a dropdown, showing "₹ INR · Indian rupee" by default. Help line: "Changes the symbol only. Amounts are not converted." Changing it changes only the symbol and formatting (FR-22). The list offers only currencies with two decimal places (architecture).
2. **Display name**: a text field. Help line: "Shown in your greeting on Home." It sets the name in the greeting ("Good evening, Shylock") (FR-23).

**Google account**

3. **Account and Expense Sheet**: one tile shows the signed-in email ("Signed in with Google") and the Expense Sheet in use ("In your Google Drive"). The tile also shows the sync-error and needs-consent pills, with the same copy and tap behavior as the header sync pill (see State Patterns).
4. **Open my Sheet**: a text action in the same tile. It opens the Expense Sheet in the Google Sheets app or the browser [ASSUMPTION: the target app].

**Sign out**

5. **Sign out**: a neutral pill, last on the screen. With nothing Pending Sync, it signs out at once. If any changes are still Pending Sync, a warning dialog comes first: title "Sign out?", body "1 change hasn't reached your Google Sheet. Signing out deletes it from this phone.", with Cancel and a red **Sign out**. Cancel keeps everything. Signing out cancels sync, erases all local data and returns to Sign-in (AD-20, NFR-5).

## Voice and Tone

The visuals are playful; the words are calm and plain. Microcopy states what happened or what to do, in short sentences, with numbers in the user's Currency. There are no jokes, no emoji and no exclamation marks. The only lighter touch allowed is a two-word nudge on a status chip ("slow down").

| Do | Don't |
|---|---|
| "₹450 added to Food" | "Nice! Expense saved 🎉" |
| "Shopping: ₹450 over budget" / "₹450 over of ₹4,000" | "Uh oh, you blew your Shopping budget!" |
| "1 waiting to sync" / "Waiting to sync" | "Sync pending (error code 0)" |
| "Delete ₹2,300 Amazon?" + "This also removes it from your Google Sheet." | "Are you sure? This cannot be undone!!" |
| "Spending only. Budgets are monthly, so a custom range has no Amount Left." | "Budget unavailable" |
| "3 new bank SMS to review" | "You've got mail!" |
| "Applies from October onward" | "Budget will be updated for current and subsequent periods" |
| "Private: lives in your own Google Sheet" | "We take your privacy seriously" |
| "85% · slow down" | "Danger zone" |

Other copy from the mocks: "Good evening, Shylock", "Tap one to see its expenses", "Tap a Category to change its budget, name, color or icon.", "Search expenses", "Load more", "Show original SMS" / "Hide original SMS". Settings, sync and dialog copy sits with its surface or state.

## Component Patterns

These are behavioral rules only. Visual specs live in `DESIGN.md` Components.

| Component | Where | Behavioral rules |
|---|---|---|
| Period filter (`{components.period-filter}`) | Home, Expenses | Three choices: **This month** (default), **Previous months** (opens a month list: Sep 2026, Aug 2026 …), **Custom range** (opens a date range picker). The selected segment shows the chosen range. One shared selection: changing it on Home also applies on Expenses, and the reverse. It's kept for the session and resets to "This month" at the next launch. |
| Hero chart (`{components.hero-chart}`) | Home | Covers all Categories for the period. Month periods show the inner Budget ring and Amount Left. A custom range shows spend only. One merged TalkBack label (see Accessibility Floor). |
| Category row (`{components.category-row}`) | Home | The whole row is one touch target. Tapping switches to the Expenses tab, filtered to that Category and the selected period. Month periods show spend, Amount Left, Budget and state. A custom range shows spend and share of spending only. A Category with no Budget shows its share of spending and a "No budget" label. |
| Inbox hint | Home | Shows the count of new Inbox Items and the banks ("From HDFC, SBI and ICICI · today"). Tap to open Inbox. Hidden when there are no Inbox Items [ASSUMPTION]. |
| Sync pill (`{components.sync-pill}`) | Home, Expenses headers | Shows the number of Pending Sync Expenses ("1 waiting to sync"). Hidden when nothing is pending [ASSUMPTION]. Also carries the offline, sync-error and needs-consent states (see State Patterns). |
| FAB (`{components.fab}`) | Home, Expenses | Opens the empty expense form. Its TalkBack label is "Add expense". |
| Search field | Expenses | Matches description or Category name (FR-4) and combines with the period and Category filters. |
| Category filter chips (`{components.filter-chip}`) | Expenses | Single choice: "All" or one Category. Set automatically when the User arrives from a Category row on Home. |
| Day header | Expenses | "Today", "Yesterday", then "Tue, 6 Oct" style, following the real calendar. Shows the day total. Stays pinned while that day's rows scroll. |
| Expense row (`{components.expense-row}`) | Expenses | Tap to edit in the expense form. Swipe left to delete; a confirmation dialog follows. Rows with `synced = false` show "Waiting to sync" until the Expense Sheet write succeeds. |
| Inbox Item (`{components.inbox-item}`) | Inbox | A visible **Add** button opens the pre-filled expense form. "Show original SMS" toggles the masked SMS text inline. Swipe left to Dismiss. Shows amount, payee, bank and masked account, and time (FR-13). |
| Load more | End of the Inbox list | Each tap reads one more previous day and appends its Inbox Items under a new day header ("Yesterday", …). Shows progress while reading (target about 2 seconds, NFR-6). |
| Expense form (sheet) | FAB, row tap, Inbox Add | Amount must be above zero and a Category is required (FR-3). **Save** stays disabled until a Category is picked. When opened from Inbox Add, the form pre-fills Amount, Date, Time and Description (payee) and leaves Category empty and outlined, with a prompt. On save, the sheet closes and a snackbar confirms. |
| Category editor (sheet) | Budgets | Edits the monthly Budget (applies to this Budget Month and later ones; past months keep theirs, FR-19), the name (must be unique), the color (eight swatches) and the icon. **Delete category** opens a confirmation that requires choosing where the Category's Expenses go (FR-17). "Add category" opens the same editor empty, with the next unused color and a default icon. |
| Delete dialog (`{components.dialog}`) | Expenses, Category editor | Cancel returns the row or editor unchanged. Delete is the filled red action. The delete-Expense dialog reads "Delete ₹2,300 Amazon?" / "This also removes it from your Google Sheet." / Cancel, Delete. The delete-Category dialog reads "Move its N expenses to:", then a Category picker, then Delete. |
| Snackbar (`{components.snackbar}`) | Above the nav | Confirms saves ("₹450 added to Food"). After a Dismiss it shows "Dismissed · Undo" for 5 seconds. Announced by TalkBack. |
| Pill nav (`{components.pill-nav}`) | All tabs | Five items. The active item shows its label, and inactive items show icons with TalkBack labels. The Inbox badge shows the Inbox Item count and updates live. At 600dp and wider, a navigation rail replaces it with the same behavior. |

## State Patterns

Mock references: month and custom-range states in `mockups/key-home.html`; Pending Sync and the delete dialog in `mockups/key-expenses.html`; the pre-filled sheet and saved snackbar in `mockups/key-inbox.html`; warning and exceeded in `mockups/key-budgets.html`; the signing-in state in `mockups/key-signin.html`; sync error, needs consent and the sign-out warning in `mockups/key-settings.html`. States without a mock are written here only.

| State | Surface | Treatment |
|---|---|---|
| Loading (first sync after sign-in) | Home, Expenses, Budgets | Glass placeholder shapes in the layout's own form: a ring and rows. Never flash an empty state while the first sync runs. After that, screens read from the local mirror and update in place without a spinner (AD-18). [ASSUMPTION: placeholder form; the log doesn't specify loading visuals.] |
| Loading (Inbox read, Load more) | Inbox | Placeholder Inbox Items, or a progress indicator in place of Load more, for up to about 2 seconds. |
| Empty | Each list surface | A simple line-art illustration in Jelly Pop tints (bundled with the app), one plain line and **one** action. Copy: Home "No expenses this month yet." with action **Add expense**; Expenses (with filters active) "No expenses match." with action **Clear filters**; Inbox "No bank SMS to review today." with action **Load more**; Budgets (no Categories) "No Categories yet." with action **Add category**. |
| Pending Sync | Home and Expenses headers; Expense rows | Header pill "1 waiting to sync" plus a row chip "Waiting to sync". Pending Sync Expenses count toward Budgets immediately (FR-7). The chip clears only after a successful Expense Sheet write. |
| Offline | Everywhere | Every core action keeps working (NFR-4). There's no blocking banner. The sync pill reads "Offline · 1 waiting to sync" when something is pending. Saves never fail for lack of a connection. |
| Sync error | Sync pill; Settings (Google account row) | The pill reads "Couldn't sync. Tap to retry.", and a tap starts a sync. Local data stays and is never discarded. Rows skipped as invalid in the Expense Sheet are counted in the message. |
| Needs consent (re-auth) | Sync pill; Settings (Google account row) | When Google access needs the User's consent again, background work stops and the next foreground sync shows the Google consent prompt (AD-10). Until it's granted, the pill reads "Sign-in needed to sync" and a tap reopens the prompt. |
| SMS permission: first ask | Inbox, first open | Before the system dialog, a short explanation: what is read (bank debit SMS on this phone), and that SMS text never leaves the phone except as Expenses the User Adds (FR-10). It's a full-surface rationale with one **Allow SMS access** action. |
| SMS permission denied | Inbox | An illustration and plain steps to grant the permission, with one action, **Open settings**. The rest of the app works normally. The Home Inbox hint is hidden [ASSUMPTION]. |
| Warning (80% or more of Budget) | Home and Budgets Category rows; hero | Ring arc in `{colors.warning}`, an amber outline and glow, and a chip with the percentage and words ("85% · slow down"). Month periods only. |
| Exceeded (100% or more of Budget) | Home and Budgets Category rows | The ring closes in `{colors.exceeded}`, and a second red lap draws the overspend ("Shopping: ₹450 over budget"; row "₹450 over of ₹4,000"). A red outline and glow plus an "Exceeded" chip. Month periods only. |
| Far over (above 200% of Budget) | Home and Budgets Category rows | The second lap stays full and the label reads "₹X over". |
| No Budget | Home and Budgets Category rows | The ring shows the Category's share of spending, with a "No budget" label and no warning or exceeded styling. |
| Custom range | Home, Expenses | Spend only: the chart and rows show spending and share per Category. No Budget, no Amount Left, no inner Budget ring, no warning or exceeded styling. The footer explains why. |
| Previous month | Home, Expenses | Shows that Budget Month's spend against the Budget it had then, with the same warning and exceeded rules. |
| Delete confirmation | Expenses | See Component Patterns, Delete dialog. |
| Delete Category confirmation | Category editor | See Component Patterns, Delete dialog. |
| Signing in | Sign-in | After "Continue with Google", the button is replaced by a progress ring and "Setting up your Expense Sheet…" while the Expense Sheet is found or created. TalkBack announces the text. |
| Sign-in cancelled | Sign-in | If the User cancels or dismisses the Google sign-in, the screen returns to the signed-out state with the "Continue with Google" button. There's no separate error screen or message. |
| Saved | After any expense save | The confirming snackbar shows (see Component Patterns, Snackbar). From Inbox, the Inbox Item leaves the list and the badge count drops. |
| Sign-out with pending changes | Settings | A "Sign out?" warning dialog before sign-out (AD-20). See Settings contents. |

## Interaction Primitives

- **Tap a row to edit.** An Expense row opens the expense form in edit mode. A Budgets Category opens the Category editor.
- **Swipe left to delete (Expenses).** Swiping past the threshold opens the delete confirmation instead of deleting at once. Cancel slides the row back. Deleting an SMS-sourced Expense keeps that SMS out of Inbox (AD-13).
- **Swipe left to Dismiss (Inbox).** No confirmation. A snackbar shows "Dismissed · Undo" for 5 seconds; once the snackbar is gone, the Dismissed Inbox Item never comes back (FR-16). The swipe background is muted, not red, so it reads differently from Delete.
- **Long-press menu as an alternative to swipe.** Long-pressing an Expense row opens a menu with Edit and Delete; long-pressing an Inbox Item opens a menu with Add and Dismiss. The same actions are offered as TalkBack custom actions on the row.
- **Pull to refresh.** It starts a sync (FR-8) on Home, Expenses and Budgets. On Inbox it re-reads today's SMS.
- **Load more (Inbox)** and the **Period filter** (Home and Expenses): see Component Patterns.
- **Tap a Home Category** to open that Category's Expenses for the selected period: the app switches to the Expenses tab with that Category's chip and the period already applied.
- **Banned:** creating an Expense from SMS without a tap on Add; editing Budgets from Home; auto-playing tours or carousels.

## Motion

Motion is lively and springy, with a sense of orbit:

- Rings sweep to their value on first show and when data changes (about 900 ms, spring settle). A second lap draws after the first ring closes.
- Cards and rows bounce in with a stagger (about 40 ms apart) on first show.
- The FAB and primary buttons squash slightly on press (scale about 0.92, spring back).
- The aurora drifts slowly behind the content.
- Sheets and dialogs spring in, and swiped rows spring back when cancelled.
- The expanded original SMS animates open and closed.

**Reduce motion.** When the system's "Remove animations" setting is on, all of this motion is off: rings, cards and sheets appear at their final state, and the aurora is still. Every function works identically, and nothing depends on an animation finishing.

## Accessibility Floor

- **Font scaling to at least 1.75x** reflows layouts instead of truncating; amounts and Category names are never cut off. The reflow rules are in DESIGN.md Layout & Spacing.
- **TalkBack labels.** Rings, the chart and amounts have merged, spoken labels, as in the mocks:
  - Hero: "Spending by category, October 2026: Food 35%, Shopping 31%, Bills 26%, Transport 4%, Fun 3%. 74% of budget used."
  - Category row: "Food, ₹5,100 spent, ₹900 left of ₹6,000. Opens Food expenses." / "Shopping, ₹4,450 spent, ₹450 over ₹4,000 budget. Opens Shopping expenses."
  - Custom range: "Food, ₹3,250 spent, 5 to 20 October. Opens Food expenses."
  - Budgets row: "Food, budget ₹6,000 a month. Edit."
  - Inbox: "Add ₹450 Café Coffee Day as expense"; the hint reads "3 new bank SMS to review. Open Inbox"; the nav badge reads "3 new".
  - Warning and exceeded states are spoken in the label, not only shown.
- **Touch targets** are at least 48dp, including segmented filter options, chips, color swatches, icon buttons, the Add button and nav items, even where the visual is smaller.
- **Contrast** meets WCAG AA for every text and background pair, in light, dark and the glass fallback. The ratios and the rule for text on the gradient are in DESIGN.md Colors (Contrast) and Elevation & Depth.
- **Never color alone.** Warning and exceeded always carry words or numbers ("85% · slow down", "Exceeded", "₹450 over") and the exceeded ring adds a second lap. Pending Sync carries text and an icon.
- **Focus order** follows reading order: header, filters, chart, list, then nav. The swipe actions are also available from the long-press menu and as custom actions.
- Interactive controls keep `testTag`s, as named in the mocks (for example `add_expense_fab`, `period_this_month`, `home_category_food`, `inbox_add_ccd`, `delete_confirm`).
  - Sign-in: `signin_continue_google` (the Google button) and `signin_progress` (the setting-up status).
  - Settings: `settings_currency`, `settings_display_name`, `settings_open_sheet`, `settings_sign_out`, `settings_sync_retry` (sync-error pill), `settings_sync_consent` (needs-consent pill).
  - Sign-out dialog: `signout_cancel`, `signout_confirm`.

## Responsive & Platform

- **Narrow phones (320dp)** at 1.75x font: single column with the reflow rules above. This is the hardest case and was rendered in `mockups/key-home.html`.
- **Standard and large phones (360 to 430dp):** single column, as in the key screens.
- **Foldables and tablets (600dp and wider):** keep the five-tab model and a readable content width. A navigation rail replaces the pill nav, and content is laid out in two columns. The period filter and chart stay at the top.
- **Platform:** Android back closes the top sheet or dialog first, then returns to Home from other tabs [ASSUMPTION]. Edge-to-edge, with the floating nav above the gesture bar. The theme follows the system light or dark setting, with no in-app toggle; Material You is off.

## Key Flows

### UJ-1: Shylock catches today's UPI payments in one glance

Surfaces: Home (Inbox hint), Inbox, expense form sheet. Mock: `mockups/key-inbox.html`.

1. Shylock opens the app. Home shows "3 new bank SMS to review" and the Inbox badge shows 3.
2. He taps the hint, or the Inbox tab. On the first-ever open, the SMS permission explanation appears and he allows access.
3. Inbox reads today's SMS on the phone and lists bank debits only, with one Inbox Item per Transaction Reference.
4. Each Inbox Item shows amount, payee, bank and masked account, and time. He taps "Show original SMS" to check one.
5. At the end of the list he taps **Load more**, and yesterday's Inbox Items appear.
6. He taps **Add** on ₹450 Café Coffee Day. The expense form sheet opens pre-filled with ₹450, 8 Oct 2026, 1:42 PM and "Café Coffee Day", with Category empty and outlined.
7. He picks Food, and Save becomes enabled. He taps Save.
8. **Climax:** the sheet closes, the Inbox Item leaves Inbox, the snackbar reads "₹450 added to Food", the badge drops to 2, and Food's ring on Home moves (with any alert updating).

Edge case: an SMS with no Transaction Reference appears twice. He swipes the extra one left to Dismiss it, with no confirmation, and it never reappears. If SMS permission is denied, Inbox shows how to grant it and every other tab works normally.

### UJ-2: Shylock sets up on day one, and again on a new phone

Surfaces: Sign-in, Home, Settings. Mocks: `mockups/key-signin.html`, `mockups/key-settings.html`.

1. Fresh install: only the Sign-in screen is reachable. He taps **Continue with Google**.
2. First-ever sign-in: "Setting up your Expense Sheet…" shows while the app creates his Expense Sheet in Drive, then Home shows loading placeholders.
3. Home appears with the default Categories and an empty-state prompt to add his first Expense.
4. Months later, on a new phone, he installs the app and signs in.
5. The app finds his existing Expense Sheet and runs the first sync, with placeholders and no empty flash.
6. **Climax:** Home and Expenses show his full history, with no import step.

Afterward, Settings shows which Google account and Expense Sheet are in use, and Open my Sheet opens it.

Edge case: if he cancels the Google sign-in, he's back on the Sign-in screen with no error, and can try again. If he has deleted the Expense Sheet from Drive, the next sign-in creates a fresh, empty one.

### UJ-3: Shylock logs a chai on the train, and fixes a typo on his laptop

Surfaces: Home or Expenses (FAB), expense form sheet, Expenses. Mock: `mockups/key-expenses.html`.

1. With no signal, he taps the FAB and adds ₹120 Chai under Food. Save works offline.
2. The snackbar confirms. The row appears under "Today" with "Waiting to sync", the header pill reads "1 waiting to sync", and Food's ring already counts it.
3. Back online, the app syncs on its own. The chip and the pill clear once the Expense Sheet write succeeds (Source = App).
4. That evening, on his laptop, he corrects an amount in the Expense Sheet and adds a cash Expense with Source left blank.
5. Next time he opens the app, or pulls to refresh, it syncs.
6. **Climax:** Expenses shows the corrected amount and the new cash Expense. The app and the Expense Sheet agree, whichever side he edited.

Edge case: the Expense Sheet wins. If a row changed in the Expense Sheet while his offline edit to it was pending, the Expense Sheet's version is shown. If sync fails, the pill says so and offers retry, and nothing local is lost.

### UJ-4: Shylock sees how much Food money is left before ordering dinner

Surfaces: Home, Budgets, Category editor sheet. Mocks: `mockups/key-home.html`, `mockups/key-budgets.html`.

1. It's the 24th. He opens the app to Home on "This month".
2. The hero shows ₹14,500 spent, 74% of the ₹19,500 Budget, and "₹5,000 left".
3. The Food row glows amber at 85%: "₹900 left of ₹6,000", with the chip "85% · slow down". Shopping shows its red second lap and "₹450 over of ₹4,000".
4. **Climax:** he knows his remaining Food money at a glance, without doing any math.
5. He taps Food and sees this month's Food Expenses (Expenses tab, Food chip applied).
6. He decides to raise Food's Budget. He goes to **Budgets**, taps Food, sets ₹7,000 ("Applies from October onward") and saves. Home's Food ring updates.

Resolution: on the 1st of next month spending resets and every Budget is copied forward, so Food starts at ₹7,000. Past months keep their own Budget.

## Language

English only in v1. Amounts and dates follow the chosen Currency and the device's calendar ("Tue, 6 Oct", "8 Oct 2026, 1:42 PM"). The UI uses US spelling ("color").

## Coverage Check

- **Needs to surfaces:** sign-in and Expense Sheet (FR-1, FR-2) → Sign-in, Settings. Expenses add, browse, search, filter, edit, delete (FR-3 to FR-6) → expense form, Expenses. Sync and offline (FR-7 to FR-9) → sync pill, row chip, pull to refresh. SMS Inbox (FR-10 to FR-16) → Inbox, expense form. Categories and Budgets (FR-17 to FR-19) → Budgets, Category editor. Amount Left (FR-20) → Home. Analytics (FR-21, extended with previous months and custom range) → Home hero chart and period filter. Currency and display name (FR-22, FR-23) → Settings. Dark mode, large text and screen sizes → DESIGN.md, Accessibility Floor, Responsive & Platform.
- **Surfaces to flows:** Home → UJ-1 (hint), UJ-4. Expenses → UJ-3, UJ-4. Inbox → UJ-1. Budgets → UJ-4. Sign-in → UJ-2. Settings → no journey; covered by Settings contents.
- **Gaps:** Sign-in and Settings are mocked (`mockups/key-signin.html`, `mockups/key-settings.html`), including the Settings sync-error and needs-consent states. Empty, loading, offline and SMS permission states, and the header-pill versions of sync error and needs consent, have no mock (their copy is decided). The larger-screen layout is decided but unrendered.
