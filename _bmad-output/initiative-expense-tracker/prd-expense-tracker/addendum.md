# Addendum: Expense Tracker PRD

## Technical notes (for architecture)

- **Data-model shift.** Today's code is Room-first: the local Room DB is primary and Google Sheets is an optional push/pull sync (`ExpenseRepository.syncLocalToSheets`, `pullDataFromSheets`). On 2026-10-08 the User decided that the Expense Sheet is the record of truth, with no app server. Architecture must decide the local role (cache vs. offline queue).
- **Offline queue vs. sheet-as-truth.** Offline adds are queued locally and pushed on reconnect. Decided: the sheet's version wins a conflict (FR-9). Architecture must define how changes are detected.
- **Finding the sheet again.** "Reuse the existing file on later sign-ins" fits the `drive.file` scope: an app can see files it created itself. This narrow scope also eases Google's OAuth verification (see the research in `.memlog.md` beside this file).
- **OAuth redirect.** The current app returns from Google OAuth through the custom-scheme deep link `com.example.expensetracker://oauth2callback` and stores a client secret on the device. Google disables custom URI scheme redirects for new Android OAuth clients and recommends Google Identity Services `AuthorizationClient`. Expect to replace this flow before public release.
- **SMS permission.** On-demand reading needs only `READ_SMS`, not `RECEIVE_SMS`, since nothing runs in the background.

## Gaps between today's code and this PRD (hand to bmad-build)

Found while reading the code on 2026-10-07 and 2026-10-08. Each becomes a fix once the PRD is final.
- **Budgets aren't monthly.** `CategoryEntity.budgetLimit` is a single limit with no month, and `ExpenseViewModel.categorySpendMetrics` sums all-time spend, although the UI says "Monthly Budget Alert". Fixed by FR-18 and FR-19.
- **Google sign-in is simulated.** `ExpenseViewModel.loginWithGoogle` stores a dummy token. Replaced by real sign-in (FR-1).
- **Currency is hard-coded to `$`** in `MainActivity.kt`. Replaced by FR-22 (INR default).
- **No in-app edit of an Expense.** Added by FR-6.

## Market context (research, 2026-10-08)

- The closest competitor is Tiller ($99 a year, bank feeds into Google Sheets). Offline Android trackers such as Bluecoins and Monefy use one-time Pro purchases. Android apps that write to Sheets are basic open-source tools. That leaves room for an offline-first app with two-way Sheets sync, budgets and charts.
- Users repeatedly ask for privacy (no bank linking), free export, recurring expenses, multi-currency and faster entry, and push back on subscriptions.
