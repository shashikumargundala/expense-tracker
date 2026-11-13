---
title: 'Settings Google account tile'
type: 'feature'
ticket: '6'
created: '2026-11-13'
status: 'built'
baseline_revision: '3763bf8dbd22224c3fccfe818fb1115eb7b77abe'
route: 'full'
route_source: 'auto'
risk: 'low'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Settings is a placeholder title, so the user can't see which Google account and Expense Sheet the app uses, or open the Sheet (EXPERIENCE.md Settings contents, items 3 and 4).

**Approach:** Under the Settings title, add the mockup's "Google account" section: one glass tile with the avatar initial, the email and "Signed in with Google", a divider, then the "Expense Sheet" row ("In your Google Drive") and the Open my Sheet text action. The data comes from `ObserveSessionUseCase`. Open my Sheet opens the Sheet in the Google Sheets app, or the Play Store listing for Sheets when that app isn't installed.

## Boundaries & Constraints

**Always:**
- The ViewModel depends only on `ObserveSessionUseCase` (AD-15). `SettingsScreen` stays stateless and gets an `onOpenSheet(sheetId)` lambda; the Route performs the launch.
- The Sheet URL is `https://docs.google.com/spreadsheets/d/<sheetId>/edit`. Never log it, the ID or the email.
- Touch targets are at least 48dp, text meets WCAG AA in light and dark, and the tile reads well at 1.75x font with long emails wrapping.
- Keep `screen_settings`; add `settings_account`, `settings_open_sheet`.

**Never:**
- No Preferences (Currency, Display name), Sign out, sync-error or needs-consent pills; epic 3 owns those (3.11–3.13).
- No `:domain` or `:data` change, and no new Google dependency in `:app`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Signed in | Session(email, sheetId) | Tile shows the initial (uppercase first letter of the email), the email, "Signed in with Google", the "Expense Sheet" row and Open my Sheet | none |
| Session loading | Not yet emitted | No tile yet; title only, no flash of wrong data | none |
| No Sheet ID | `sheetId == null` (unreachable past the gate) | Account row only; no Expense Sheet row or Open my Sheet | none |
| Open, Sheets installed | Tap Open my Sheet | `ACTION_VIEW` of the Sheet URL with package `com.google.android.apps.docs.editors.sheets` | none |
| Open, Sheets missing | No activity for that intent | Opens `market://details?id=com.google.android.apps.docs.editors.sheets` | No Play Store either: opens `https://play.google.com/store/apps/details?id=…`; if nothing can open it, nothing happens and nothing crashes |

**Decisions (owner, 2026-11-13):**
1. Open my Sheet targets the Google Sheets app only, and falls back to the Play Store listing for Sheets (resolves EXPERIENCE.md's "[ASSUMPTION: the target app]").

</frozen-after-approval>

## Code Map

- `app/.../settings/SettingsScreen.kt` -- `SettingsRoute(contentPadding, viewModel)` L25; placeholder `SettingsScreen(uiState, modifier, contentPadding)` L33–52: a Box tagged `screen_settings` with a `titleLarge` `R.string.tab_settings` heading, plus a `@Suppress("UNUSED_PARAMETER")` at L31 and a preview at L56. Replace the body with a `LazyColumn` (title, section header, tile) that keeps the padding.
- `app/.../settings/SettingsUiState.kt:7` -- `SettingsUiState(isPlaceholder)`; replace it with the account fields (for example `email`, `sheetId`, `loaded`).
- `app/.../settings/SettingsViewModel.kt:11` -- an empty `@Inject` constructor; inject `ObserveSessionUseCase` and map it into a `StateFlow`, following `home/HomeViewModel`.
- `domain/.../account/ObserveSessionUseCase.kt:11` and `Session.kt:8` -- `Flow<Session?>` with `email` and `sheetId`. Don't change them.
- `app/.../navigation/AppNavHost.kt:93` -- `SettingsRoute(contentPadding = padding.value)`, unchanged.
- `app/.../ui/theme/Glass.kt:43` -- `Modifier.glassSurface(shape, navStyle = false)`; `Shape.kt:14` `Radius.lg` 24dp; `Type.kt` (`titleLarge` for the screen title, `titleMedium` for the section title, `titleSmall` for item titles, `bodySmall` for meta). The indicator colour is `colorScheme.secondaryContainer`, and `navigation/components/NavItemStyle.kt:51–53` `navActiveBrush()` (gradientStart to #DB2777) is the avatar gradient's reference, drawn at 135°.
- `_bmad-output/initiative-expense-tracker/ux-expense-tracker/mockups/key-settings.html` -- State 1, the Google account tile: markup L123, CSS L103–117, tokens L52–55. The avatar is 2.6em with a white bold initial; the email is weight 650 with `overflow-wrap:anywhere`; the divider is inset 3.35em; the `.orb` sheet icon is primary on the indicator; `.tlink` is min-height 3.2em with an external-link icon. DESIGN.md L162–167 `glass-tile`, L413–417 the Settings spec.
- `app/src/test/.../screenshot/Screenshots.kt` L10–29 (`captureScreen` KDoc), `ShellScreenshotTest.kt` L47–53 (tap `nav_expenses`, wait for `screen_expenses`, capture); `account/FakeAccountModule.kt:23` `SignedInTestSession` (`tester@example.com`, `test-sheet`).

## Tasks & Acceptance

**Execution:**
- [x] `app/.../settings/SettingsUiState.kt`, `SettingsViewModel.kt` -- account state from `ObserveSessionUseCase` -- data
- [x] `app/.../settings/components/GoogleAccountTile.kt` -- the glass tile (avatar initial in the 135° brand gradient, email, "Signed in with Google", inset divider, sheet orb, "Expense Sheet", "In your Google Drive", Open my Sheet with an external-link icon, `settings_open_sheet`), merged rows for TalkBack -- the tile
- [x] `app/.../settings/SettingsScreen.kt`, `strings.xml` -- the `LazyColumn`, the "Google account" section header and the tile; `onOpenSheet` -- the screen
- [x] `app/.../settings/OpenSheet.kt` -- `openSheetInSheetsApp(context, sheetId)` per the matrix: catch `ActivityNotFoundException` at each step -- decision 1
- [x] `app/src/main/res/drawable/ic_sheet.xml`, `ic_open_in_new.xml` -- Material Symbols vectors -- icons
- [x] `app/src/test/.../settings/SettingsViewModelTest.kt` -- signed in, loading, no Sheet ID -- matrix
- [x] `app/src/test/.../settings/SettingsScreenTest.kt` -- the tile shows the email and initial, hides the Sheet row with no ID, and Open my Sheet calls `onOpenSheet("test-sheet")` -- matrix
- [x] `app/src/test/.../settings/OpenSheetTest.kt` -- Robolectric: the started intent's action, data and package; with no Sheets activity the next started intent is the `market://` one; nothing throws when nothing resolves -- matrix
- [x] `app/src/test/.../screenshot/SettingsScreenshotTest.kt` and baselines `settings_light`, `settings_dark` and `settings_font175` (w320, 1.75x), recorded with `recordRoborazziDebug --tests` -- verify line

**Acceptance Criteria:**
- Given the signed-in owner on the S23 with Google Sheets installed, when they tap Open my Sheet, then their Expense Sheet opens in the Sheets app.
- Given TalkBack is on, when it reaches the tile, then it reads the account as one item (email, "Signed in with Google") and Open my Sheet as a button.

## Implementation Notes

- `ActiveNavGradientEnd` (#DB2777) in `navigation/components/NavItemStyle.kt` became `internal` so the avatar gradient reuses it.
- Review pass 1 patches: the badge and icon sizes follow the font scale (sp to dp), `SecurityException` is caught at each fallback step, the Play-website test asserts nothing else started, and the formatting is fixed. `settings_font175` was re-recorded.
- Device check (2026-11-13, S23): the owner checked Settings and Open my Sheet and approved.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-13, quick): 0 high, 0 medium, 5 low, 2 false

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | CLAUDE.md and the Copilot instructions still call Settings a placeholder, and miss the new tags and screenshots | low | defer | Real; the fix edits agent-context files. Recorded in `deferred-work.md`. |
| 2 | `AppShellTest` wasn't changed, against CLAUDE.md's "add a tab screen" step 4 | false | reject | `AppShellTest` already taps `nav_settings` and asserts `screen_settings`; the new screen has its own tests. |
| 3 | The avatar, orb and icons are fixed dp, so they don't scale with font the way the mockup's em sizes do | low | patch | Real: visible in `settings_font175.png`. The fix sizes them from the font size; baselines re-recorded. |
| 4 | Only `ActivityNotFoundException` is caught in `tryStartActivity` | low | patch | Real: a `SecurityException` from an unexported handler would crash, against the matrix's "nothing crashes". The fix adds one catch. |
| 5 | `FLAG_ACTIVITY_NEW_TASK` on every intent | false | reject | Launching another app in its own task is standard and harmless; no bad outcome. |
| 6 | The Play-website fallback test doesn't assert that nothing else started | low | patch | Real; the fix adds the same `assertNull` the other fallback tests have. |
| 7 | Import order and two over-long lines | low | patch | Real and cosmetic; a direct correction. |

## Design Notes

Each step of the fallback is a plain `startActivity` in a `try`/`catch`, so no `<queries>` manifest entry is needed. Robolectric records started intents but by default never throws `ActivityNotFoundException`. For the fallback tests, call `shadowOf(application).checkActivities(true)` and register only the activities each case should resolve (for example with `shadowOf(packageManager).addResolveInfoForIntent`). The test can then assert each step without a real Sheets app.

## Verification

**Commands:**
- `./gradlew :app:testDebugUnitTest` -- expected: pass
- `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks (if no CLI):**
- Open each new `settings_*.png`: the tile matches the mockup's State 1, and nothing is clipped at 320dp/1.75x.
- On the S23: Settings shows the email; Open my Sheet opens the Sheet in the Sheets app.
