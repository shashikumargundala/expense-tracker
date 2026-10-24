# Brownfield notes: Expense Tracker

The v1 build restructures the existing single-module `:app` (commit `26e51ad`) into the three-module layout of spine AD-1. It doesn't extend the current code in place.

## Start fresh

- There's no migration of the current Room database (`expense_tracker_db` v1) or of the legacy Google Sheet layout (`Sheet1`, columns `Local ID, Timestamp, Amount ($), Category, Description`).
- The current OAuth flow (custom-scheme deep link `com.example.expensetracker://oauth2callback`, client ID and secret in SharedPreferences) is removed, not adapted (AD-10).

## Gaps in today's code that v1 must close

| Today | Where | Closed by |
| --- | --- | --- |
| Budgets add up all-time spending under a "Monthly Budget Alert" label | `ExpenseViewModel.categorySpendMetrics`; `CategoryEntity.budgetLimit` has no month | CAP-15, CAP-16, AD-8 |
| Google sign-in is simulated with a dummy token | `ExpenseViewModel.loginWithGoogle` | CAP-1, AD-10 |
| Currency is hard-coded to `$` | `MainActivity.kt` | CAP-18 |
| No in-app edit of an Expense | — | CAP-3 |
| Unparseable Sheet IDs fall back to `1`, so they overwrite Expense 1 | `ExpenseRepository.pullDataFromSheets` | AD-5 |
| Amounts are `Double` | `ExpenseEntity.amount` | AD-6 |
| Almost all UI is in one 1,940-line file | `MainActivity.kt` | AD-17 |
| Manual dependency wiring | `MainActivity.onCreate` | AD-16 |

## Project setup changes

- **App ID and namespace:** the placeholder `applicationId` `com.aistudio.expensetracker.ktzqm` and namespace `com.example` are replaced by `com.shashi.expensetracker`.
- **Manifest:** `GET_ACCOUNTS` is removed, `READ_SMS` is added, and `allowBackup` is set to `false`.
- **Debug signing:** the debug build references a missing `debug.keystore`. Debug signing follows the spine's Operations section.
- **Toolchain:** versions in `gradle/libs.versions.toml` are upgraded to the spine's Stack table.
- **Keep:** existing `testTag`s on interactive controls stay, and new user-critical actions get tags.
